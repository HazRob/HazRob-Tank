package com.servoc.app.report;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.TextUtils;
import com.servoc.app.hardware.HazRobHardwareProfile;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Authentic, single-operation V4 Arabic PDF. No synthetic sensor values or time series. */
public final class TankInstantReportPdf {
    private TankInstantReportPdf(){}
    private static final int DARK=0xFF182334,MUTED=0xFF52606F;
    private static Paint paint(int n,boolean bold,int color){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setTextSize(n);
        p.setTypeface(android.graphics.Typeface.create("sans-serif",bold?1:0));return p;
    }
    private static void rtl(Canvas canvas,String text,float baseline,int size,boolean bold){
        TextPaint p=new TextPaint(paint(size,bold,bold?DARK:MUTED));
        String s=text==null?"":text;
        StaticLayout l=StaticLayout.Builder.obtain(s,0,s.length(),p,525)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(1).setEllipsize(TextUtils.TruncateAt.END).build();
        canvas.save();canvas.translate(35,baseline-l.getLineBaseline(0));l.draw(canvas);canvas.restore();
    }
    private static String fmt(double v,int digits){
        return Double.isFinite(v)?String.format(Locale.US,"%."+digits+"f",v):"غير متاح";
    }
    private static String utc(long t){
        SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'",Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date(t));
    }
    private static String data(JSONObject o,String key,int digits){
        return o==null?"غير متاح":fmt(o.optDouble(key,Double.NaN),digits);
    }
    public static void write(OutputStream out,JSONObject telemetry,JSONObject chemcore,String id,long timestamp)throws Exception{
        if(out==null||telemetry==null||chemcore==null||id==null||id.isEmpty())
            throw new IllegalArgumentException("Completed immutable measurement required");
        PdfDocument doc=new PdfDocument();
        try{
            PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,1).create());
            Canvas c=page.getCanvas();c.drawColor(Color.WHITE);int y=45;
            rtl(c,"HazRob Tank — تقرير القياسات الآنية",y,18,true);y+=27;
            rtl(c,"العملية: "+id+"     •     "+utc(timestamp),y,11,false);y+=22;
            rtl(c,"لقطة ختامية فعلية لعملية قياس واحدة — لا تمثل دراسة تعرض زمني",y,10,false);y+=23;
            c.drawLine(35,y,560,y,paint(1,false,0xFFD1D7DE));y+=24;
            JSONObject mq=telemetry.optJSONObject("mq");
            rtl(c,"قياسات الحساسات — Rs وR0 بوحدة kΩ",y,13,true);y+=20;
            for(HazRobHardwareProfile.Channel channel:HazRobHardwareProfile.MQ){
                JSONObject q=mq==null?null:mq.optJSONObject(channel.key);
                boolean ok=q!=null&&q.optBoolean("ok",q.optBoolean("valid",false));
                String text=channel.key.toUpperCase(Locale.US)+"  •  "+
                    (ok?"Rs "+data(q,"rs",3)+" | R0 "+data(q,"r0",3)+" | Rs/R0 "+data(q,"ratio",3):
                        "قراءة غير صالحة أو غير متاحة")+
                    (q==null?"":" • "+q.optString("r0_source",""));
                rtl(c,text,y,10,false);y+=21;
            }
            y+=8;
            JSONObject env=telemetry.optJSONObject("env");
            rtl(c,"القياسات الفيزيائية المحفوظة",y,13,true);y+=19;
            rtl(c,"الحرارة "+data(env,"temperature",2)+" °C   |   الرطوبة "+data(env,"humidity",2)+" %   |   الضغط "+data(env,"pressure",2)+" hPa",y,10,false);y+=30;
            rtl(c,"تحليل ChemCore — تقديرات استدلالية وليست إثبات هوية",y,13,true);y+=20;
            rtl(c,"قنوات MQ المستخدمة: "+chemcore.optInt("observed_channels",0)+
                "  |  المنهج: "+chemcore.optString("model_version","فحص بصمات ومنحنيات"),y,10,false);y+=19;
            JSONArray candidates=chemcore.optJSONArray("candidates");
            if(candidates==null||candidates.length()==0){rtl(c,"لم تتوفر مرشحات قابلة للحساب من المدخلات المتاحة",y,11,false);y+=22;}
            else{
                for(int i=0;i<Math.min(8,candidates.length());i++){
                    JSONObject q=candidates.optJSONObject(i);if(q==null)continue;
                    double ppm=q.optDouble("screening_fit_ppm",Double.NaN);
                    rtl(c,"#"+(i+1)+" "+q.optString("label",q.optString("name","—"))+
                        " • مطابقة "+data(q,"candidate_score_pct",1)+"%"+
                        (Double.isFinite(ppm)?" • "+fmt(ppm,2)+" ppm تقديري":""),y,10,false);
                    y+=20;
                }
            }
            y+=15;
            rtl(c,"قيم PPM إن وُجدت تقديرية؛ لا تصلح وحدها لإثبات الامتثال المهني.",y,10,false);y+=17;
            rtl(c,"المصدر: قراءات محفوظة داخل العملية الحالية، دون اختراع منحنيات زمنية.",y,10,false);
            doc.finishPage(page);
            doc.writeTo(out);
        }finally{doc.close();}
    }
}
