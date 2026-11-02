package com.hazrob.tank.foodfreshness.meat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import com.servoc.app.report.V7ReportLayoutKit;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Beef-only independent food research PDF (R06 V7).
 * This is a presentation layer. It never computes microbial count, edibility,
 * regulatory limits, source-to-Tank calibration or scientific model outcomes.
 * A one-off Tank scan is not the 36-hour experimental study.
 */
public final class V7BeefResearchPdfWriter {
    private V7BeefResearchPdfWriter(){}
    private static final float L=V7ReportLayoutKit.mm(5);
    private static final float R=V7ReportLayoutKit.A4_PORTRAIT_W-L;
    private static final float W=R-L;
    private static final String[] CLASSES=BeefResearchCatalog.GRADE_CODES;
    private static final String[] CLASS_AR={"ممتازة","جيدة","مقبولة","فاسدة"};
    // Source TS5 research confusion (actual-study row vs predicted-class col).
    // Exact values from research/food_freshness/meat/
    // wijaya2018_5series_7mq_class_prototypes.json holdout_results.confusion;
    // these 2160 records are NOT measurements of the Tank's food sample.
    private static final int[][] SOURCE_TS5_CONFUSION={
        {43,317,0,0},
        {0,540,0,0},
        {0,420,0,0},
        {0,115,359,366}
    };

    private static void text(Canvas c,String s,float x,float y,float w,float h,
                             boolean rtl,boolean bold){
        V7ReportLayoutKit.text(c,s,new RectF(x,y,x+w,y+h),rtl,bold,12,
            V7ReportLayoutKit.INK);
    }
    private static String val(Double n,int decimals){
        return n==null||!Double.isFinite(n)?"-":
            String.format(Locale.US,"%."+decimals+"f",n);
    }
    private static String value(double n,int decimals){
        return Double.isFinite(n)?String.format(Locale.US,"%."+decimals+"f",n):"-";
    }
    private static RectF rect(float x,float y,float w,float h){
        return new RectF(x,y,x+w,y+h);
    }
    private static void section(Canvas c,String ar,String en,float y){
        V7ReportLayoutKit.tableHeader(c,rect(L,y,W,42),ar+" / "+en,true);
    }
    private static void cell(Canvas c,String content,RectF r,boolean rtl){
        V7ReportLayoutKit.tableValue(c,r,content,rtl);
    }
    private static void pageChrome(Canvas c,Context ctx,int number,
                                   String captureTimestampUtc){
        V7ReportLayoutKit.drawHeader(c,ctx,false,
            "تقرير القياس البحثي للحوم البقرية","Beef Direct Measurement Research",
            V7ReportLayoutKit.missing(captureTimestampUtc));
        V7ReportLayoutKit.drawFooter(c,false,number,3);
    }
    private static void pageOne(Canvas c,Context ctx,
                                BeefResearchEngine.Result result,
                                Map<String,Double> channels,int samples,
                                long ms,String id,double t,double rh,String captureTimestampUtc){
        pageChrome(c,ctx,1,captureTimestampUtc);
        section(c,"بيانات العينة والفحص","Sample & Acquisition",80);
        text(c,"لحم بقري — دراسة بصمات بحثية",L+8,128,W-16,24,true,true);
        String scan=(ms>=0?value(ms/1000.0,1)+" s":"-");
        text(c,"Report ID: "+V7ReportLayoutKit.missing(id),L+8,156,W-16,30,false,false);
        text(c,"Scan: "+scan+"  |  Frames: "+(samples>=0?Integer.toString(samples):"-")+
            "  |  MQ: "+result.validChannels+"/7",L+8,190,W-16,34,false,false);
        text(c,"T: "+value(t,1)+" °C    |    RH: "+value(rh,1)+" %",L+8,225,W-16,25,false,false);
        section(c,"قراءات الحساسات السبعة","Observed MQ resistances",264);
        float rowY=310;
        for(String name:BeefResearchCatalog.SENSOR_ORDER){
            V7ReportLayoutKit.tableValue(c,rect(L,rowY,120,38),name,false);
            Double read=channels==null?null:channels.get(name);
            V7ReportLayoutKit.tableValue(c,rect(L+120,rowY,170,38),val(read,4),false);
            V7ReportLayoutKit.tableValue(c,rect(L+290,rowY,W-290,38),
                "Tank Rs (device units)",false);
            rowY+=38;
        }
        section(c,"مقارنة فئات الدراسة الأربع","Four research class labels",594);
        float y=642;
        for(int i=0;i<CLASSES.length;i++){
            BeefResearchEngine.Match m=null;
            for(BeefResearchEngine.Match candidate:result.rankedClasses)
                if(CLASSES[i].equals(candidate.classCode)){m=candidate;break;}
            String score=m==null?"-":value(m.descriptiveSimilarity,1)+"/100";
            V7ReportLayoutKit.tableValue(c,rect(L,y,165,29),
                CLASS_AR[i]+" / "+CLASSES[i],true);
            V7ReportLayoutKit.tableValue(c,rect(L+165,y,125,29),score,false);
            V7ReportLayoutKit.tableValue(c,rect(L+290,y,W-290,29),
                m==null?"-":"distance "+value(m.distance,3),false);
            y+=29;
        }
        text(c,"Research similarity is NOT a microbial TVC result or food-safety verdict.",
            L+8,764,W-16,21,false,true);
    }
    private static void measuredBeefCurve(Canvas c,BeefResearchEngine.Result result,
                                         Map<String,Double> observed){
        if(result.rankedClasses.isEmpty()||observed==null)return;
        String leading=result.rankedClasses.get(0).classCode;
        double[] reference=null;
        for(BeefResearchCatalog.Template template:BeefResearchCatalog.classes())
            if(template.label.equals(leading)){reference=template.logShape();break;}
        if(reference==null)return;
        double[] x=new double[7],y=new double[7];
        boolean[] active=new boolean[7];double avg=0,refAvg=0;int n=0;
        for(int i=0;i<7;i++){
            Double raw=observed.get(BeefResearchCatalog.SENSOR_ORDER.get(i));
            if(raw==null||raw<=0||!Double.isFinite(raw))continue;
            x[i]=Math.log10(raw);y[i]=reference[i];avg+=x[i];refAvg+=y[i];
            active[i]=true;n++;
        }
        if(n<2)return;
        avg/=n;refAvg/=n;
        double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
        for(int i=0;i<7;i++)if(active[i]){
            x[i]-=avg;y[i]-=refAvg;
            min=Math.min(min,Math.min(x[i],y[i]));
            max=Math.max(max,Math.max(x[i],y[i]));
        }
        float left=L+50,right=R-48,top=162,bottom=266;
        double span=Math.max(0.08,max-min);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setStrokeWidth(3f);
        for(int series=0;series<2;series++){
            p.setColor(series==0?0xFFE86820:0xFF246EAA);
            float px=Float.NaN,py=Float.NaN;
            for(int i=0;i<7;i++){
                if(!active[i]){px=Float.NaN;continue;}
                float cx=left+(right-left)*i/6f;
                float cy=bottom-(bottom-top)*(float)(((series==0?x[i]:y[i])-min)/span);
                if(Float.isFinite(px))c.drawLine(px,py,cx,cy,p);
                c.drawCircle(cx,cy,3f,p);px=cx;py=cy;
            }
        }
        text(c,"Measured Tank Rs (orange) vs study "+leading+" prototype (blue)",
             L+12,273,W-24,24,false,false);
    }
    /** Plot collected 180s real MQ telemetry only: no synthetic or literature time series. */
    private static void actualTimeTrace(Canvas c,Map<String,List<double[]>> traces){
        if(traces==null||traces.isEmpty()){
            text(c,"Live time series was not retained for this export.",
                L+22,390,W-44,34,false,false);
            return;
        }
        double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
        java.util.Map<String,Double> initial=new java.util.LinkedHashMap<>();
        for(String sensor:BeefResearchCatalog.SENSOR_ORDER){
            List<double[]> points=traces.get(sensor);
            if(points==null)continue;
            for(double[] point:points){
                if(point==null||point.length<2||!Double.isFinite(point[0])||
                   !Double.isFinite(point[1])||point[1]<=0)continue;
                if(!initial.containsKey(sensor))initial.put(sensor,point[1]);
                double relative=Math.log10(point[1]/initial.get(sensor));
                if(Double.isFinite(relative)){min=Math.min(min,relative);max=Math.max(max,relative);}
            }
        }
        if(!Double.isFinite(min))return;
        double span=Math.max(0.05,max-min);
        int[] colors={0xFF286DB1,0xFFCF6231,0xFF298A52,0xFF7D4FAA,
                      0xFFB73A61,0xFF177D87,0xFF87582A};
        float left=L+52,right=R-38,top=382,bottom=490;
        Paint pen=new Paint(Paint.ANTI_ALIAS_FLAG);pen.setStrokeWidth(2f);
        for(int i=0;i<BeefResearchCatalog.SENSOR_ORDER.size();i++){
            String sensor=BeefResearchCatalog.SENSOR_ORDER.get(i);
            List<double[]> data=traces.get(sensor);
            if(data==null||data.size()<2||!initial.containsKey(sensor))continue;
            pen.setColor(colors[i]);float px=Float.NaN,py=Float.NaN;
            for(double[] point:data){
                if(point==null||point.length<2||!Double.isFinite(point[0])||
                   !Double.isFinite(point[1])||point[1]<=0){px=Float.NaN;continue;}
                double q=Math.log10(point[1]/initial.get(sensor));
                float x=left+(right-left)*(float)Math.max(0,Math.min(1,point[0]/180.0));
                float y=bottom-(bottom-top)*(float)((q-min)/span);
                if(Float.isFinite(px))c.drawLine(px,py,x,y,pen);
                px=x;py=y;
            }
            text(c,sensor,L+17+i*(W-34)/7,494,(W-34)/7,18,false,false);
        }
    }
    private static void pageTwo(Canvas c,Context ctx,BeefResearchEngine.Result result,
                                Map<String,Double> measured,Map<String,List<double[]>> trace,
                                String captureTimestampUtc){
        pageChrome(c,ctx,2,captureTimestampUtc);
        V7ReportLayoutKit.blankChart(c,rect(L,82,W,211),
            "البصمة المقاسة مقابل المرجع / Seven-MQ source comparison",
            "MQ2–MQ135", "Shape");
        measuredBeefCurve(c,result,measured);
        V7ReportLayoutKit.blankChart(c,rect(L,306,W,211),
            "الاستجابة الزمنية للحساسات / Actual 180s Tank MQ response",
            "Capture time (seconds)","log10 Rs / initial Rs");
        actualTimeTrace(c,trace);
        BeefGaussianResearchEngine.Result gaussian=
            BeefGaussianResearchEngine.analyze(measured);
        text(c,"Secondary Gaussian study model: "+
            (gaussian.ranked.isEmpty()?gaussian.status:gaussian.ranked.get(0).classCode)+
            " (source only, no microbiological measurement)",L+14,518,W-28,22,false,false);
        // Render a chart ONLY from actual precomputed result values.
        // No synthetic confidence, no thresholding, no food quality verdict.
        float top=530;
        V7ReportLayoutKit.blankChart(c,rect(L,top,W,243),
            "مؤشر التشابه مع الفئات / Study class similarity",
            "Descriptive similarity index","Class");
        float base=top+48;
        for(int i=0;i<CLASSES.length;i++){
            BeefResearchEngine.Match match=null;
            for(BeefResearchEngine.Match q:result.rankedClasses)
                if(CLASSES[i].equals(q.classCode)){match=q;break;}
            float y=base+i*36;
            text(c,CLASSES[i],L+19,y,93,28,false,false);
            if(match==null||!Double.isFinite(match.descriptiveSimilarity)) {
                text(c,"-",L+130,y,50,28,false,false);
                continue;
            }
            float index=(float)Math.max(0,Math.min(100,match.descriptiveSimilarity));
            Paint bar=new Paint(Paint.ANTI_ALIAS_FLAG);
            bar.setColor(V7ReportLayoutKit.INK);
            bar.setStyle(Paint.Style.FILL);
            float barStart=L+138,barSpan=315;
            c.drawRect(barStart,y+6,barStart+barSpan*index/100f,y+18,bar);
            text(c,value(match.descriptiveSimilarity,1),L+460,y,65,26,false,true);
        }
    }
    private static void pageThree(Canvas c,Context ctx,String captureTimestampUtc){
        pageChrome(c,ctx,3,captureTimestampUtc);
        section(c,"التحقق البحثي المستقل","Source-study TS5 validation",82);
        text(c,"SOURCE STUDY ONLY — five beef cuts, TS1–TS4 train / TS5 holdout",
            L+6,132,W-12,50,false,true);
        text(c,"TS5 observed class (rows) × predicted class (columns)",
            L+6,176,W-12,43,false,false);
        float top=226,left=L+4,lab=124,side=(W-lab-8)/4f;
        V7ReportLayoutKit.tableHeader(c,rect(left,top,lab,44),
            "Actual / Predicted",false);
        for(int j=0;j<4;j++)
            V7ReportLayoutKit.tableHeader(c,
                rect(left+lab+j*side,top,side,44),CLASSES[j],false);
        for(int i=0;i<4;i++){
            float y=top+44+i*53;
            V7ReportLayoutKit.tableHeader(c,rect(left,y,lab,53),CLASSES[i],false);
            for(int j=0;j<4;j++)
                cell(c,Integer.toString(SOURCE_TS5_CONFUSION[i][j]),
                    rect(left+lab+j*side,y,side,53),false);
        }
        text(c,"Study holdout accuracy: "+
            value(BeefResearchCatalog.HELDOUT_ACCURACY*100,1)+
            "%   |   Balanced accuracy: "+
            value(BeefResearchCatalog.HELDOUT_BALANCED_ACCURACY*100,1)+"%",
            L+6,497,W-12,46,false,true);
        V7ReportLayoutKit.blankChart(c,rect(L,546,W,133),
            "البيانات الزمنية للدراسة الأصلية / External 36h study",
            "Study time (hours) — not Tank scan","MQ / TVC");
        text(c,"Tank scan: 180 s ≠ source study: 36 h. No actual Tank TVC or edibility clearance.",
            L+6,690,W-12,40,false,false);
        text(c,"Dataset DOI: "+BeefResearchCatalog.STUDY_DOI,
            L+6,734,W-12,24,false,false);
        text(c,"Study article DOI: 10.1016/j.dib.2018.11.091",
            L+6,759,W-12,24,false,false);
    }

    public static void write(Context ctx,OutputStream out,BeefResearchEngine.Result result,
                             Map<String,Double> readings,int sampleCount,long ms,
                             String id,double temperatureC,double humidityPct)throws Exception{
        write(ctx,out,result,readings,sampleCount,ms,id,temperatureC,humidityPct,"-",null);
    }
    /** Timestamp is the archived specimen acquisition time, not export time. */
    public static void write(Context ctx,OutputStream out,BeefResearchEngine.Result result,
                             Map<String,Double> readings,int sampleCount,long ms,
                             String id,double temperatureC,double humidityPct,
                             String captureTimestampUtc)throws Exception{
        write(ctx,out,result,readings,sampleCount,ms,id,temperatureC,humidityPct,
            captureTimestampUtc,null);
    }
    public static void write(Context ctx,OutputStream out,BeefResearchEngine.Result result,
                             Map<String,Double> readings,int sampleCount,long ms,
                             String id,double temperatureC,double humidityPct,
                             Map<String,List<double[]>> trace)throws Exception{
        write(ctx,out,result,readings,sampleCount,ms,id,temperatureC,humidityPct,"-",trace);
    }
    public static void write(Context ctx,OutputStream out,BeefResearchEngine.Result result,
                             Map<String,Double> readings,int sampleCount,long ms,
                             String id,double temperatureC,double humidityPct,
                             String captureTimestampUtc,
                             Map<String,List<double[]>> trace)throws Exception{
        if(ctx==null||out==null||result==null)
            throw new IllegalArgumentException("Frozen beef study result required");
        PdfDocument pdf=new PdfDocument();
        try {
            for(int i=1;i<=3;i++){
                PdfDocument.Page p=V7ReportLayoutKit.open(pdf,i,false);
                Canvas c=p.getCanvas();c.drawColor(Color.WHITE);
                if(i==1)pageOne(c,ctx,result,readings,sampleCount,ms,id,
                    temperatureC,humidityPct,captureTimestampUtc);
                else if(i==2)pageTwo(c,ctx,result,readings,trace,captureTimestampUtc);
                else pageThree(c,ctx,captureTimestampUtc);
                pdf.finishPage(p);
            }
            pdf.writeTo(out);
        }finally{pdf.close();}
    }
}
