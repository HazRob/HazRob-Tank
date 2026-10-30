package com.servoc.app.report;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import com.servoc.app.R;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * V7 R01 PDF — presentation of immutable ChemCore V6 engine results.
 *
 * No second gas/R0/GHS/ppm/exposure approval or decision is performed here.
 * Scientific statuses, available ratios and source hypotheses are forwarded
 * unchanged from Engine JSON. The report only lays out source measurements
 * and engine data. This class NEVER invents time series or fills missing R0.
 *
 * PDF layout: A4 portrait 595x842 points; white; 5 mm side margins;
 * black header with owner logo at right / 15pt amber bilingual title centered /
 * actual captured date in white at left; gray footer.
 */
public final class TankInstantV7PdfWriter {
    private TankInstantV7PdfWriter(){}
    private static volatile Context application;
    public static void bindContext(Context context) {
        if(context!=null)application=context.getApplicationContext();
    }
    private static final int W=595,H=842;
    private static final float M=14.2f,RIGHT=W-M;
    private static final int INK=0xFF111111,AMBER=0xFFFFBE00,GRAY=0xFFE9E9E9,
        MEDIUM=0xFF6B6B6B,BLACK=0xFF000000,WHITE=Color.WHITE;
    private static final String[] MQ={"MQ2","MQ3","MQ4","MQ5","MQ7","MQ8","MQ9","MQ135"};

    private static Paint p(float pt,boolean bold,int color) {
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
        p.setTextSize(pt);p.setColor(color);return p;
    }
    private static TextPaint tp(float pt,boolean bold,int color){
        return new TextPaint(p(pt,bold,color));
    }
    private static float mm(float x){return x*72f/25.4f;}
    private static void rect(Canvas c,float x,float y,float w,float h,int color){
        Paint fill=p(12,false,color);c.drawRect(x,y,x+w,y+h,fill);
    }
    private static void border(Canvas c,float x,float y,float w,float h){
        Paint pen=p(12,false,BLACK);pen.setStyle(Paint.Style.STROKE);
        pen.setStrokeWidth(.65f);c.drawRect(x,y,x+w,y+h,pen);
    }
    private static void line(Canvas c,float x,float y,float x2,float y2,int color,float width){
        Paint a=p(12,false,color);a.setStrokeWidth(width);c.drawLine(x,y,x2,y2,a);
    }
    /** RTL shaping, one or more lines, 1.15 line spacing, no paragraph spacing. */
    private static void txt(Canvas c,String value,float x,float y,float width,
                float pt,boolean bold,int color,Layout.Alignment alignment,
                boolean rtl,int maxLines){
        String s=value==null?"—":value;
        TextPaint t=tp(pt,bold,color);
        StaticLayout lay=StaticLayout.Builder.obtain(s,0,s.length(),t,Math.max(1,(int)width))
            .setTextDirection(rtl?TextDirectionHeuristics.RTL:TextDirectionHeuristics.LTR)
            .setAlignment(alignment).setIncludePad(false).setLineSpacing(0,1.15f)
            .setMaxLines(maxLines).build();
        c.save();c.translate(x,y);lay.draw(c);c.restore();
    }
    private static void center(Canvas c,String value,float x,float y,float w,float pt,boolean bold,int color,boolean rtl){
        txt(c,value,x,y,w,pt,bold,color,Layout.Alignment.ALIGN_CENTER,rtl,2);
    }
    private static void right(Canvas c,String value,float x,float y,float w,float pt,boolean bold,int color){
        txt(c,value,x,y,w,pt,bold,color,Layout.Alignment.ALIGN_NORMAL,true,2);
    }
    private static void left(Canvas c,String value,float x,float y,float w,float pt,boolean bold,int color){
        txt(c,value,x,y,w,pt,bold,color,Layout.Alignment.ALIGN_NORMAL,false,2);
    }
    private static String format(double d,int n){
        return Double.isFinite(d)?String.format(Locale.US,"%."+n+"f",d):"—";
    }
    private static String number(JSONObject o,String key,int n){
        return o==null?"—":format(o.optDouble(key,Double.NaN),n);
    }
    private static String utc(long time){
        SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd\nHH:mm:ss 'UTC'",Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date(time));
    }
    private static void drawOfficialLogo(Canvas c,float x,float y,float size){
        Context app=application;
        if(app==null)app=com.servoc.app.chemical.GhsPictograms.applicationContext();
        if(app!=null){
            try{
                Drawable logo=app.getDrawable(R.drawable.hazrob_official_report_mark);
                if(logo!=null){
                    logo.setBounds((int)x,(int)y,(int)(x+size),(int)(y+size));
                    logo.draw(c);return;
                }
            }catch(Exception ignored){}
        }
        // Graphic fallback for test-only renderers without application Context:
        // preserve owner-supplied hexagon/crosshair motif; normal app uses
        // the actual resource above. Never substitute old Tank vehicle icon.
        Path hex=new Path();float cx=x+size/2f,cy=y+size/2f;
        for(int i=0;i<6;i++){
            double a=Math.PI*i/3-Math.PI/2;
            float px=(float)(cx+size*.45*Math.cos(a));
            float py=(float)(cy+size*.45*Math.sin(a));
            if(i==0)hex.moveTo(px,py);else hex.lineTo(px,py);
        }
        hex.close();
        Paint gold=p(12,false,AMBER);gold.setStyle(Paint.Style.STROKE);
        gold.setStrokeWidth(Math.max(3,size*.09f));c.drawPath(hex,gold);
        Paint white=p(12,false,WHITE);white.setStyle(Paint.Style.STROKE);
        white.setStrokeWidth(Math.max(2,size*.04f));
        for(float r:new float[]{.27f,.18f,.09f})c.drawCircle(cx,cy,size*r,white);
        c.drawLine(cx-size*.30f,cy,cx+size*.30f,cy,white);
        c.drawLine(cx,cy-size*.30f,cx,cy+size*.30f,white);
    }
    private static void header(Canvas c,String timestamp){
        rect(c,M,M,W-2*M,69,BLACK);
        drawOfficialLogo(c,RIGHT-59,M+8,52);
        // Three independent columns avoid BiDi corruption of captured times.
        left(c,utc(timestampAsLong(timestamp)),M+5,M+10,134,12,false,WHITE);
        center(c,"تقرير التحليل الآني للغازات",M+145,M+16,267,15,true,AMBER,true);
        center(c,"Instant Gas Analysis Report",M+145,M+40,267,15,true,AMBER,false);
    }
    private static long timestampAsLong(String v){
        try{return Long.parseLong(v);}catch(Exception e){return 0L;}
    }
    private static void footer(Canvas c,int page,int count){
        rect(c,M,799,W-2*M,29,GRAY);
        left(c,"Haz",RIGHT-91,803,60,14,true,BLACK);
        left(c,"Rob",RIGHT-56,803,45,14,true,AMBER);
        left(c,"صفحة "+page+" من "+count,M+9,802,178,14,true,BLACK);
    }
    private static void tag(Canvas c,float x,float y,float w,String heading,String data){
        rect(c,x,y,w,57,0xFFF5F5F5);border(c,x,y,w,57);
        right(c,heading,x+mm(2),y+6,w-mm(4),12,true,INK);
        center(c,data,x+mm(2),y+29,w-mm(4),12,true,INK,false);
    }
    private static Map<String,JSONObject> channelLedger(JSONObject result){
        Map<String,JSONObject> map=new HashMap<>();
        JSONArray rows=result.optJSONArray("original_mq_channel_evidence");
        if(rows!=null)for(int i=0;i<rows.length();i++){
            JSONObject row=rows.optJSONObject(i);
            if(row!=null)map.put(row.optString("sensor_model","").toUpperCase(Locale.US),row);
        }
        return map;
    }
    private static void table(Canvas c,JSONObject telemetry,Map<String,JSONObject> evidence,float x,float y,float width){
        right(c,"قراءات الحساسات / MQ readings",x,y-22,width,12,true,INK);
        final float h=36,hh=39;
        final float[] widths={81,66,61,61,width-269}; // left to right: state,ratio,r0,rs,sensor
        float xx=x;
        String[] names={"الحالة / State","Rs/R0","R0","Rs","MQ"};
        for(int j=0;j<5;j++){
            rect(c,xx,y,widths[j],hh,AMBER);border(c,xx,y,widths[j],hh);
            center(c,names[j],xx+mm(2),y+9,widths[j]-mm(4),12,true,BLACK,j==0);
            xx+=widths[j];
        }
        JSONObject raw=telemetry.optJSONObject("mq");
        for(int i=0;i<MQ.length;i++){
            String code=MQ[i];
            JSONObject src=raw==null?null:raw.optJSONObject(code.toLowerCase(Locale.US));
            JSONObject engine=evidence.get(code);
            String status=engine==null?"—":engine.optBoolean("r0_clean_air_device_evidence_qualified",false)?"موثق / Q":"بحثي / R";
            String ratio=number(engine,"rs_r0_ratio_research",3);
            String[] cells={status,ratio,number(src,"r0",2),number(src,"rs",2),code};
            float yy=y+hh+i*h;xx=x;
            for(int j=0;j<5;j++){
                rect(c,xx,yy,widths[j],h,i%2==0?WHITE:0xFFF2F2F2);
                border(c,xx,yy,widths[j],h);
                center(c,cells[j],xx+mm(2),yy+8,widths[j]-mm(4),12,j==4,INK,false);
                xx+=widths[j];
            }
        }
    }
    private static void matrix(Canvas c,JSONObject telemetry,Map<String,JSONObject> engine,float x,float y,float width){
        right(c,"مصفوفة الأدلة / Evidence matrix",x,y-22,width,12,true,INK);
        final float hh=35,h=36,labelWidth=65;
        final float cw=(width-labelWidth)/4f;
        String[] headers={"Rs","R0","OK","Ratio"};
        float xx=x;
        for(int i=0;i<5;i++){
            float w=i==0?labelWidth:cw;
            rect(c,xx,y,w,hh,AMBER);border(c,xx,y,w,hh);
            center(c,i==0?"MQ":headers[i-1],xx+1,y+9,w-2,12,true,BLACK,false);
            xx+=w;
        }
        JSONObject mq=telemetry.optJSONObject("mq");
        for(int i=0;i<MQ.length;i++){
            String key=MQ[i];
            JSONObject raw=mq==null?null:mq.optJSONObject(key.toLowerCase(Locale.US));
            JSONObject e=engine.get(key);
            boolean[] flags={raw!=null&&Double.isFinite(raw.optDouble("rs",Double.NaN)),
                raw!=null&&Double.isFinite(raw.optDouble("r0",Double.NaN)),
                e!=null&&e.optBoolean("r0_clean_air_device_evidence_qualified",false),
                e!=null&&e.opt("rs_r0_ratio_research") instanceof Number};
            float yy=y+hh+i*h;
            rect(c,x,yy,labelWidth,h,i%2==0?WHITE:0xFFF1F1F1);border(c,x,yy,labelWidth,h);
            center(c,key,x+1,yy+8,labelWidth-2,12,true,INK,false);
            for(int j=0;j<4;j++){
                float cx=x+labelWidth+j*cw;
                int color=flags[j]?0xFF424242:0xFFDEDEDE;
                // Indicator is presentation of source availability/engine status,
                // not another analytical eligibility decision.
                rect(c,cx,yy,cw,h,i%2==0?WHITE:0xFFF4F4F4);
                rect(c,cx+5,yy+7,cw-10,h-14,color);
                border(c,cx,yy,cw,h);
            }
        }
    }
    private static void radar(Canvas c,Map<String,JSONObject> evidence,float x,float y,float width,float height){
        right(c,"البصمة البحثية / MQ fingerprint",x,y,width,12,true,INK);
        float cx=x+width*.52f,cy=y+104f,r=48f;
        Paint grid=p(12,false,0xFF959595);grid.setStyle(Paint.Style.STROKE);grid.setStrokeWidth(.6f);
        Paint spoke=p(12,false,0xFFB4B4B4);spoke.setStrokeWidth(.7f);
        float max=1;
        int count=0;
        double[] vals=new double[MQ.length];boolean[] present=new boolean[MQ.length];
        for(int i=0;i<MQ.length;i++){
            JSONObject q=evidence.get(MQ[i]);
            double v=q==null?Double.NaN:q.optDouble("rs_r0_ratio_research",Double.NaN);
            if(Double.isFinite(v)&&v>=0){
                present[i]=true;vals[i]=v;count++;max=Math.max(max,(float)v);
            }
        }
        max=(float)Math.ceil(max*2f)/2f;
        for(int ring=1;ring<=3;ring++){
            Path path=new Path();
            for(int i=0;i<MQ.length;i++){
                double theta=-Math.PI/2+i*2*Math.PI/MQ.length;
                float rr=r*ring/3;
                float xx=cx+(float)Math.cos(theta)*rr, yy=cy+(float)Math.sin(theta)*rr;
                if(i==0)path.moveTo(xx,yy);else path.lineTo(xx,yy);
            }
            path.close();c.drawPath(path,grid);
        }
        for(int i=0;i<MQ.length;i++){
            double a=-Math.PI/2+i*2*Math.PI/MQ.length;
            float ox=cx+(float)Math.cos(a)*r,oy=cy+(float)Math.sin(a)*r;
            c.drawLine(cx,cy,ox,oy,spoke);
            float lx=cx+(float)Math.cos(a)*(r+27)-25,ly=cy+(float)Math.sin(a)*(r+27)-8;
            center(c,MQ[i],lx,ly,50,12,false,INK,false);
        }
        if(count>=3){
            Path polygon=new Path();
            boolean begun=false;
            // Do not connect through missing values; the engine ledger is the source.
            if(count==MQ.length){
                for(int i=0;i<MQ.length;i++){
                    double a=-Math.PI/2+i*2*Math.PI/MQ.length;
                    float xx=cx+(float)Math.cos(a)*r*(float)(vals[i]/max);
                    float yy=cy+(float)Math.sin(a)*r*(float)(vals[i]/max);
                    if(i==0)polygon.moveTo(xx,yy);else polygon.lineTo(xx,yy);
                    begun=true;
                }
                if(begun){
                    polygon.close();
                    Paint fill=p(12,false,0xFF3B5360);fill.setAlpha(55);c.drawPath(polygon,fill);
                    Paint edge=p(12,true,0xFF232323);edge.setStyle(Paint.Style.STROKE);
                    edge.setStrokeWidth(2f);c.drawPath(polygon,edge);
                }
            }else{
                for(int i=0;i<MQ.length;i++)if(present[i]){
                    double a=-Math.PI/2+i*2*Math.PI/MQ.length;
                    float xx=cx+(float)Math.cos(a)*r*(float)(vals[i]/max);
                    float yy=cy+(float)Math.sin(a)*r*(float)(vals[i]/max);
                    Paint point=p(12,true,INK);
                    c.drawCircle(xx,yy,3,point);
                }
            }
        }else{
            center(c,"No ratio series",x,y+170,width,12,false,MEDIUM,false);
        }
        left(c,"Rs/R0 • بحثي / Research only",x+8,y+188,width-16,12,false,INK);
    }
    private static void decision(Canvas c,JSONObject engine,float x,float y,float w){
        right(c,"قرار ChemCore / Decision",x,y,w,12,true,INK);
        float yy=y+28;
        JSONObject gate=engine.optJSONObject("baseline_response_gate");
        String state=gate==null?"—":gate.optString("state","—");
        String identity=engine.optString("open_set_status","UNKNOWN");
        String[] keys={"النتيجة / Result","مرجع R0","قنوات MQ","النسب / Ratios","حالة PPM"};
        String[] vals={identity,gate==null?"—":""+gate.optInt("accepted_device_r0_channels",0),
            ""+engine.optInt("observed_mq_channels",0),
            ""+engine.optInt("ratio_ready_mq_channels",0),
            engine.optBoolean("reportable_ppm",false)?"Engine: YES":"Engine: NO"};
        for(int i=0;i<keys.length;i++){
            float ry=yy+i*28;
            rect(c,x,ry,w,27,i%2==0?0xFFF5F5F5:WHITE);border(c,x,ry,w,27);
            right(c,keys[i],x+mm(2),ry+6,w*.37f,12,false,INK);
            right(c,vals[i],x+w*.39f,ry+5,w*.57f,12,true,INK);
        }
        right(c,"البوابة / Gate: "+state,x+mm(2),yy+146,w-mm(4),12,false,INK);
    }
    private static void mainPage(PdfDocument doc,JSONObject telemetry,JSONObject engine,
                                  String id,long timestamp,int total){
        PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(W,H,1).create());
        Canvas c=page.getCanvas();c.drawColor(WHITE);
        header(c,Long.toString(timestamp));footer(c,1,total);
        float full=W-2*M;
        right(c,"العملية / Operation: "+id,M,87,full,12,true,INK);
        float gap=7,cardW=(full-2*gap)/3f;
        tag(c,M,113,cardW,"الهوية / Identity",engine.optString("open_set_status","UNKNOWN"));
        tag(c,M+cardW+gap,113,cardW,"الاستجابة / MQ",""+engine.optInt("observed_mq_channels",0)+" channels");
        tag(c,M+2*(cardW+gap),113,cardW,"التركيز / PPM",
            engine.optBoolean("reportable_ppm",false)?"Engine: reportable":"Not reportable");
        Map<String,JSONObject> ledger=channelLedger(engine);
        float tableW=331;
        table(c,telemetry,ledger,RIGHT-tableW,200,tableW);
        matrix(c,telemetry,ledger,M,200,full-tableW-8);
        radar(c,ledger,M,552,263,207);
        decision(c,engine,M+270,552,full-270);
        right(c,"القيم والاستنتاجات من المحرك • لا يعيد PDF تقييم الأهلية أو الغاز",M,760,full,12,false,INK);
        left(c,"Engine-supplied evidence • no second analytic gate",M,778,full,12,false,INK);
        doc.finishPage(page);
    }
    private static void detailPage(PdfDocument doc,JSONArray records,int start,int n,int pageNo,int total,long captured){
        PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(W,H,pageNo).create());
        Canvas c=page.getCanvas();c.drawColor(WHITE);header(c,Long.toString(captured));footer(c,pageNo,total);
        right(c,"سجل فرضيات المصادر / Source hypotheses",M,93,W-2*M,14,true,INK);
        right(c,"مخرجات ChemCore البحثية كما وردت دون اعتماد هوية أو ppm",M,118,W-2*M,12,false,INK);
        float y=150,w=W-2*M;
        rect(c,M,y,w,32,AMBER);border(c,M,y,w,32);
        right(c,"المادة / Substance   •   Sensor   •   Reference ID",M+mm(2),y+6,w-mm(4),12,true,BLACK);
        for(int i=0;i<n;i++){
            JSONObject o=records.optJSONObject(start+i);
            if(o==null)continue;
            float rowY=y+32+i*37;
            rect(c,M,rowY,w,37,i%2==0?WHITE:0xFFF1F1F1);border(c,M,rowY,w,37);
            right(c,"#"+(start+i+1)+" "+o.optString("gas_or_class","—")+
                "  /  "+o.optString("sensor_model","—"),M+mm(2),rowY+3,w-mm(4),12,true,INK);
            left(c,"Ref: "+o.optString("source_curve_id","—"),M+mm(2),rowY+20,w-mm(4),12,false,INK);
        }
        doc.finishPage(page);
    }
    public static void write(OutputStream out,JSONObject telemetry,JSONObject engine,
                             String operationId,long measurementTimeUtcMs)throws Exception {
        if(out==null||telemetry==null||engine==null||operationId==null)
            throw new IllegalArgumentException("Immutable captured measurement required");
        JSONArray sources=engine.optJSONArray("source_curve_hypotheses");
        int count=sources==null?0:sources.length();
        final int perPage=15;
        int pages=1+(count+perPage-1)/perPage;
        PdfDocument doc=new PdfDocument();
        try{
            mainPage(doc,telemetry,engine,operationId,measurementTimeUtcMs,pages);
            for(int i=0;i<count;i+=perPage)
                detailPage(doc,sources,i,Math.min(perPage,count-i),
                    2+i/perPage,pages,measurementTimeUtcMs);
            doc.writeTo(out);
        }finally{doc.close();}
    }
}
