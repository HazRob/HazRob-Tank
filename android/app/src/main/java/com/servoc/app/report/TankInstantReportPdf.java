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
    /**
     * Dedicated V6 gas-only ONE-SHOT PDF. Unlike the V5 source format this
     * intentionally contains NO ranked posterior, synthetic confidence,
     * exposure exceedance, certified ppm or hazard-class claim. UNKNOWN is
     * always the field identity while Tank physical G3 is absent.
     */
    private static void writeResearchV6(OutputStream stream,JSONObject telemetry,
                            JSONObject v6,String id,long timestamp)throws Exception{
        PdfDocument doc=new PdfDocument();
        try{
            PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,1).create());
            Canvas canvas=page.getCanvas();
            canvas.drawColor(Color.WHITE);
            int y=46;
            rtl(canvas,"HazRob Tank — تقرير غازات ChemCore V6 البحثي",y,17,true);y+=29;
            rtl(canvas,"العملية: "+id+" • "+utc(timestamp),y,10,false);y+=21;
            rtl(canvas,"نتيجة مؤقتة للقراءة الختامية فقط — ليست قياس تعرض زمني",y,10,false);y+=23;
            rtl(canvas,"الهوية: UNKNOWN — لم يتم اعتماد أي غاز على Tank G3",y,13,true);y+=23;
            rtl(canvas,"تركيز ppm معتمد: غير متاح • لا يمكن إصدار حكم تجاوز حدود مهنية",y,10,false);y+=23;
            canvas.drawLine(35,y,560,y,paint(1,false,0xFFBEC7D1));y+=26;
            rtl(canvas,"سجل القراءات الأصلية (بوحدة استقبال ESP32؛ kΩ عند توثيقها)",y,12,true);y+=23;
            JSONObject mq=telemetry.optJSONObject("mq");
            for(HazRobHardwareProfile.Channel channel:HazRobHardwareProfile.MQ){
                JSONObject s=mq==null?null:mq.optJSONObject(channel.key);
                boolean eligible=s!=null&&s.optBoolean("ok",s.optBoolean("valid",false))&&
                    (!s.has("powered")||s.optBoolean("powered",false))&&
                    Double.isFinite(s.optDouble("rs",Double.NaN))&&
                    s.optDouble("rs",Double.NaN)>0;
                String row=channel.key.toUpperCase(Locale.US)+"  •  "+
                    (eligible?"Rs "+data(s,"rs",3)+
                        "  •  R0 "+data(s,"r0",3)+
                        "  •  المصدر "+s.optString("r0_source","غير موثق"):
                        "قراءة غائبة أو غير صالحة، لم تدخل التحليل");
                rtl(canvas,row,y,9,false);y+=19;
            }
            y+=9;
            rtl(canvas,"جودة بيانات V6 المستقلة",y,12,true);y+=23;
            rtl(canvas,"قنوات Rs محفوظة "+v6.optInt("observed_mq_channels",0)+
                " • بلا R0 "+v6.optInt("raw_only_mq_channels",0)+
                " • نسبة صالحة "+v6.optInt("ratio_ready_mq_channels",0),y,10,false);y+=21;
            rtl(canvas,"الامتناع عن الهوية: "+v6.optString("open_set_status",
                    "UNKNOWN_WITHOUT_PHYSICAL_TANK_G3"),y,9,false);y+=22;
            JSONArray source=v6.optJSONArray("source_curve_hypotheses");
            int total=source==null?0:source.length();
            rtl(canvas,"فرضيات منحنيات المصادر — غير مثبتة ميدانيًا ("+total+")",y,12,true);y+=20;
            if(total==0){
                rtl(canvas,"لا توجد منحنيات مؤهلة للحساب من القيم الأصلية المتاحة",y,10,false);
                y+=20;
            }else{
                for(int i=0;i<Math.min(total,7)&&y<733;i++){
                    JSONObject q=source.optJSONObject(i);
                    if(q==null)continue;
                    String row="#"+(i+1)+" "+q.optString("gas_or_class","—")+
                        " • "+q.optString("sensor_model","—")+
                        " • المصدر "+q.optString("source_curve_id","—");
                    rtl(canvas,row,y,9,false);y+=18;
                }
                if(total>7&&y<748){
                    rtl(canvas,"بقية فرضيات المصادر محفوظة رقمياً ("+(total-7)+")",y,9,false);
                    y+=18;
                }
            }
            if(y<770)y=770;
            rtl(canvas,"تحذير: لا تُستخدم فرضيات المنحنيات لإطلاق إنذار أو حساب ppm مهني.",y,9,true);
            y+=19;
            rtl(canvas,"بيانات الأغذية ومحرك الغازات منفصلان؛ لا يوجد دمج تلقائي.",y,9,false);
            doc.finishPage(page);
            doc.writeTo(stream);
        }finally{
            doc.close();
        }
    }

    public static void write(OutputStream out,JSONObject telemetry,JSONObject chemcore,String id,long timestamp)throws Exception{
        if(out==null||telemetry==null||chemcore==null||id==null||id.isEmpty())
            throw new IllegalArgumentException("Completed immutable measurement required");
        if("hazrob-tank-v6-android-telemetry-bridge/1".equals(
                chemcore.optString("schema",""))){
            writeResearchV6(out,telemetry,chemcore,id,timestamp);
            return;
        }
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
            JSONObject stable=chemcore.optJSONObject("active_stable_reference_models");
            if(stable!=null){
                rtl(c,"Stable: "+stable.optString("version","—")+" • "+stable.optString("status","—")+
                    " • منحنيات مستخدمة "+stable.optInt("models_imported",0),y,9,false);y+=17;
            }
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
            // Raw limits are published reference facts and are available without M4 calibration.
            // Do not calculate regulatory exceedance or use estimated PPM as a certified measurement.
            JSONObject inventory=chemcore.optJSONObject("exposure_reference_inventory");
            JSONArray refCandidates=inventory==null?null:inventory.optJSONArray("candidates");
            if(refCandidates!=null&&refCandidates.length()>0&&y<700){
                rtl(c,"حدود التعرض المرجعية للمواد المرشحة (لا تعني تحقق الرصد)",y,12,true);y+=21;
                for(int i=0;i<Math.min(2,refCandidates.length())&&y<750;i++){
                    JSONObject candidate=refCandidates.optJSONObject(i);if(candidate==null)continue;
                    rtl(c,"مرشح #"+candidate.optInt("candidate_rank",i+1)+" • "+candidate.optString("label","—"),y,10,true);y+=17;
                    JSONArray limits=candidate.optJSONArray("limits");
                    if(limits==null||limits.length()==0){rtl(c,"لا توجد حدود موثقة محليًا لهذه المادة",y,9,false);y+=15;continue;}
                    for(int j=0;j<Math.min(3,limits.length())&&y<750;j++){
                        JSONObject limit=limits.optJSONObject(j);if(limit==null)continue;
                        String v=data(limit,"value",3),unit=limit.optString("unit","");
                        rtl(c,limit.optString("authority","")+" "+limit.optString("limit_type","")+
                            " • "+v+" "+unit+" • "+limit.optString("source_id",""),y,9,false);y+=15;
                    }
                }
                y+=9;
            }
            rtl(c,"قيم PPM إن وُجدت تقديرية؛ لا تصلح وحدها لإثبات الامتثال المهني.",y,10,false);y+=17;
            rtl(c,"المصدر: قراءات محفوظة داخل العملية الحالية، دون اختراع منحنيات زمنية.",y,10,false);
            doc.finishPage(page);
            JSONObject warningReport=chemcore.optJSONObject("screening_exposure_warnings");
            JSONArray warnings=warningReport==null?null:warningReport.optJSONArray("warnings");
            if(warnings!=null&&warnings.length()>0){
                PdfDocument.Page second=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,2).create());
                Canvas w=second.getCanvas();w.drawColor(Color.WHITE);int yy=47;
                rtl(w,"HazRob Tank — تحذيرات مبكرة تقديرية",yy,17,true);yy+=29;
                rtl(w,"مقارَنات مشروطة بمرشحي ChemCore، وليست قياسًا معتمدًا أو إثبات تجاوز قانوني",yy,10,false);yy+=26;
                for(int i=0;i<warnings.length()&&yy<720;i++){
                    JSONObject item=warnings.optJSONObject(i);if(item==null)continue;
                    String role=item.optString("reference_role","OTHER"),name=item.optString("label","—");
                    rtl(w,"⚠ "+name+" • "+role+" • "+item.optString("authority",""),yy,12,true);yy+=19;
                    rtl(w,"PPM تقديري "+data(item,"estimated_ppm",2)+"  /  حد "+data(item,"reference_value",2)+" "+item.optString("reference_unit",""),yy,10,false);yy+=19;
                    if("LFL".equals(role)){
                        rtl(w,"🔥 "+data(item,"percent_lfl",1)+"% من حد الاشتعال LFL • مقارنة فورية",yy,10,false);yy+=19;
                    }else if("TWA".equals(role)||"STEL".equals(role)){
                        rtl(w,"لو استمر هذا التركيز "+data(item,"reference_duration_min",0)+" دقيقة فقد يتجاوز متوسط التعرض المرجعي",yy,10,false);yy+=19;
                        rtl(w,"هذه ليست نتيجة متوسط TWA/STEL مقاس خلال كامل المدة",yy,9,false);yy+=17;
                    }else{
                        rtl(w,"مقارنة مرجعية لحظية تستلزم تأكيد هوية الغاز والتركيز",yy,10,false);yy+=19;
                    }
                    yy+=15;
                }
                if(warnings.length()>8)rtl(w,"توجد تحذيرات إضافية؛ راجع شاشة ChemCore.",742,10,false);
                rtl(w,"الاستنتاج مشروط بعدم اليقين وتداخل الغازات؛ غياب التحذير لا يعني أمان الجو.",789,10,false);
                doc.finishPage(second);
            }
            doc.writeTo(out);
        }finally{doc.close();}
    }
}
