package com.servoc.app.report;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.TextUtils;
import android.graphics.pdf.PdfDocument;
import com.servoc.app.measurements.PhysicalCatalog;
import org.json.JSONObject;
import java.io.OutputStream;
import java.util.Locale;

/** One-page operator snapshot. Only actually transmitted values are reported. */
public final class TankPhysicalSnapshotPdf {
    private TankPhysicalSnapshotPdf(){}
    private static boolean good(double n){return Double.isFinite(n);}
    private static String value(double n,int digits){return good(n)?String.format(Locale.US,"%."+digits+"f",n):"—";}
    private static String sample(JSONObject object,String key,int digits){
        return object==null?"—":value(object.optDouble(key,Double.NaN),digits);
    }
    private static TextPaint text(int size,boolean bold,int color){
        TextPaint p=new TextPaint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setTextSize(size);
        p.setTypeface(android.graphics.Typeface.create("sans-serif",bold?1:0));
        return p;
    }
    /**
     * Android StaticLayout handles Arabic joining, bidi runs and their font fallback.
     * Raw Canvas.drawText(...) of mixed Arabic/Latin runs is NOT our PDF RTL engine.
     */
    private static void rtl(Canvas c,String s,int baselineY,TextPaint p){
        rtlAt(c,s,baselineY,p,35,525);
    }
    private static void rtlField(Canvas c,String s,int baselineY,TextPaint p){
        rtlAt(c,s,baselineY,p,240,320);
    }
    private static void rtlAt(Canvas c,String s,int baselineY,TextPaint p,int left,int width){
        String value=s==null?"":s;
        StaticLayout paragraph=StaticLayout.Builder.obtain(value,0,value.length(),p,width)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .setMaxLines(1)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build();
        c.save();
        c.translate(left,baselineY-p.getTextSize());
        paragraph.draw(c);
        c.restore();
    }
    private static void left(Canvas c,String s,int y,TextPaint p){
        p.setTextAlign(Paint.Align.LEFT);c.drawText(s,35,y,p);
    }

    public static void write(OutputStream out,JSONObject snapshot,String timestampUtc)throws Exception{
        if(out==null||snapshot==null)throw new IllegalArgumentException("Frozen physical snapshot required");
        JSONObject env=snapshot.optJSONObject("env"),sound=snapshot.optJSONObject("sound"),light=snapshot.optJSONObject("ldr");
        double temp=env==null?Double.NaN:env.optDouble("temperature",Double.NaN);
        double hum=env==null?Double.NaN:env.optDouble("humidity",Double.NaN);
        double press=env==null?Double.NaN:env.optDouble("pressure",Double.NaN);
        boolean envOk=env!=null&&env.optBoolean("ok",false);
        PdfDocument doc=new PdfDocument();
        try{
            PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,1).create());
            Canvas c=page.getCanvas();c.drawColor(Color.WHITE);
            TextPaint title=text(19,true,0xFF15202B),body=text(11,false,0xFF303A44),strong=text(12,true,0xFF15202B),muted=text(9,false,0xFF5A6571);
            rtl(c,"HazRob Tank V3 | تقرير القياسات الفيزيائية",52,title);
            rtl(c,"لقطة قياس فورية • البيانات التي كانت متاحة وقت طلب التقرير",75,body);
            rtl(c,"UTC: "+timestampUtc,93,muted);
            Paint line=new Paint();line.setColor(0xFFD8DEE4);line.setStrokeWidth(1);c.drawLine(35,104,560,104,line);
            int y=132;
            for(int i=0;i<PhysicalCatalog.COUNT;i++){
                if(i==9||i==10||i==11)continue; // independent PM subtab and report
                String reading="—",source="غير مركب / غير معاير";
                switch(i){
                    case 0: if(envOk&&good(temp)){reading=value(temp,2);source="ENV • حرارة مقاسة";}break;
                    case 1: if(envOk&&good(hum)){reading=value(hum,2);source="ENV • رطوبة مقاسة";}break;
                    case 2: if(envOk&&good(press)){reading=value(press,2);source="ENV • ضغط مقاس";}break;
                    case 3: if(light!=null&&light.optBoolean("installed",false))source="LDR: حد رقمي فقط، لا lux";break;
                    case 4: if(light!=null&&light.optBoolean("installed",false))source="LDR: لا قياس lm";break;
                    case 5:
                        if(sound!=null&&sound.optBoolean("calibrated_dba",false)&&good(sound.optDouble("dba",Double.NaN))){
                            reading=value(sound.optDouble("dba",Double.NaN),1)+" dBA";source="مقياس معاير، عتبة تنبيه لحظية 90 dBA";
                        }else if(sound!=null&&good(sound.optDouble("db_spl",Double.NaN))){
                            reading=value(sound.optDouble("db_spl",Double.NaN),1);source="dB SPL تقديري، غير معاير";
                        }else source="MAX4466: لا قراءة dB معايرة";
                        break;
                    case 14:
                        if(envOk&&good(press)&&press>0){reading=value(44330.0*(1.0-Math.pow(press/1013.25,0.1903)),1);source="مشتق من الضغط، ليس مقياس ارتفاع مستقل";}
                        break;
                    default:break;
                }
                rtlField(c,PhysicalCatalog.NAMES_AR[i]+"  ("+PhysicalCatalog.UNITS[i]+")",y,strong);
                left(c,reading,y,body);
                rtlField(c,source,y+14,muted);
                c.drawLine(35,y+20,560,y+20,line);y+=40;
            }
            y+=5;
            rtl(c,"قراءات ميكروفون تشخيصية (فولت)",y,strong);y+=18;
            left(c,sample(sound,"rms_v",4)+" V",y,body);rtlField(c,"MAX4466 RMS",y,body);y+=22;
            left(c,sample(sound,"p2p_v",4)+" V",y,body);rtlField(c,"MAX4466 Peak-to-Peak",y,body);y+=25;
            rtl(c,"المصدر: "+(envOk?"BME/BMP، قيم لحظية":"ENV غير متاح أو غير صالح"),y,muted);
            rtl(c,"هذا التقرير لا يثبت الامتثال المهني أو التعرض التراكمي.",810,muted);
            doc.finishPage(page);
            doc.writeTo(out);
        }finally{doc.close();}
    }
}
