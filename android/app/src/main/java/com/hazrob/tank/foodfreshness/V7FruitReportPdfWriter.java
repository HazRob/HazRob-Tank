package com.hazrob.tank.foodfreshness;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import com.servoc.app.report.V7ReportLayoutKit;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * R05 native food-headspace document layout, intentionally not yet activated
 * as production until real licensed fruit photos are packaged and visual QA
 * passes. Does not call the gas ChemCore or classify food safety.
 */
public final class V7FruitReportPdfWriter {
    private V7FruitReportPdfWriter(){}
    private static final float MM=V7ReportLayoutKit.mm(1);
    private static final float L=5*MM;
    private static final float W=200*MM;
    private static final float[] COL={25,56,31,28,26,34};
    private static final String[] FAMILIES={
        "banana","blueberry","grape","green apple","kiwi","mandarin",
        "pear","red apple","strawberry","tomato","applebanana",
        "applebananamandarin","applebananatomato","applemandarin",
        "appletomato","bananamandarin","tomatobanana","tomatomandarin"
    };
    private static final String[] AR={
        "موز","توت أزرق","عنب","تفاح أخضر","كيوي","يوسفي",
        "كمثرى","تفاح أحمر","فراولة","طماطم","تفاح وموز",
        "تفاح وموز ويوسفي","تفاح وموز وطماطم","تفاح ويوسفي",
        "تفاح وطماطم","موز ويوسفي","طماطم وموز","طماطم ويوسفي"
    };
    private static final String[] HEAD={
        "صورة / Photo","الصنف / Family","يوم الدراسة / Reference",
        "التشابه / Index","MQ overlap","دليل / Source"
    };

    private static String fmt(double d,int p){
        return Double.isFinite(d)?String.format(Locale.US,"%."+p+"f",d):"-";
    }
    private static void text(Canvas c,String s,RectF r,boolean rtl,boolean bold){
        V7ReportLayoutKit.text(c,s,r,rtl,bold,12,V7ReportLayoutKit.INK);
    }
    private static void frame(Canvas c,RectF r,int color,float width){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);
        c.drawRect(r,p);
    }
    private static String imageAssetName(String family){
        return "fruit_photos/"+family.replace(" ","_")+".webp";
    }
    /**
     * The packaged asset is a reference picture of a food family, not a
     * snapshot of an actual Tank sample. There is no emoji/photo fallback
     * masquerading as photographic evidence.
     */
    private static boolean image(Context ctx,Canvas c,String family,RectF r){
        if(ctx==null)return false;
        String name=imageAssetName(family);
        try(InputStream stream=ctx.getAssets().open(name)){
            Bitmap bitmap=BitmapFactory.decodeStream(stream);
            if(bitmap==null)return false;
            Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
            float ratio=Math.min(r.width()/bitmap.getWidth(),r.height()/bitmap.getHeight());
            float w=bitmap.getWidth()*ratio,h=bitmap.getHeight()*ratio;
            RectF dest=new RectF(r.centerX()-w/2,r.centerY()-h/2,
                r.centerX()+w/2,r.centerY()+h/2);
            c.drawBitmap(bitmap,null,dest,paint);
            bitmap.recycle();
            return true;
        }catch(IOException e){return false;}
    }
    private static void header(Canvas c,Context ctx,int n,int total,String timestamp){
        V7ReportLayoutKit.drawHeader(c,ctx,false,
            "تقرير القياس البحثي للفواكه","Fruit Direct Measurement Research",
            V7ReportLayoutKit.missing(timestamp));
        V7ReportLayoutKit.drawFooter(c,false,n,total);
    }
    private static Map<String,FoodFreshnessEngine.Match> best(FoodFreshnessEngine.Result r){
        Map<String,FoodFreshnessEngine.Match> result=new HashMap<>();
        // rankedTemplates is already ordered by the source engine. Do not
        // calculate a new distance or freshness classification in the PDF.
        for(FoodFreshnessEngine.Match m:r.rankedTemplates)
            if(!result.containsKey(m.foodFamily))result.put(m.foodFamily,m);
        return result;
    }
    private static void mainPage(Canvas c,Context ctx,int page,int pages,
                                 FoodFreshnessEngine.Result result,
                                 Map<String,Double> readings,int frames,long duration,
                                 String id,Map<String,FoodFreshnessEngine.Match> best,
                                 String captureTimestampUtc){
        header(c,ctx,page,pages,captureTimestampUtc);
        float y=27*MM;
        String selected=result.foodSelection==null?"":result.foodSelection;
        text(c,"عملية الفحص / Scan: "+V7ReportLayoutKit.missing(id),
            new RectF(L+4*MM,y,L+W-4*MM,y+10*MM),true,true);
        text(c,"Selected: "+(selected.isEmpty()?"All source families":selected)+
            "  |  "+(duration>=0?fmt(duration/1000.0,1)+" s":"-")+
            "  |  Frames: "+(frames>=0?Integer.toString(frames):"-")+
            "  |  MQ: "+result.usableSensorCount+"/8",
            new RectF(L+4*MM,y+10*MM,L+W-4*MM,y+21*MM),false,false);
        float headerTop=52*MM;
        // Source-family photo is the FIRST column on the RIGHT in RTL.
        // Column geometry descends from the page's right printable boundary.
        float[] pos=new float[COL.length+1];pos[0]=L+W;
        for(int i=0;i<COL.length;i++)pos[i+1]=pos[i]-COL[i]*MM;
        for(int col=0;col<COL.length;col++)
            V7ReportLayoutKit.tableHeader(c,
                new RectF(pos[col+1],headerTop,pos[col],headerTop+16*MM),
                HEAD[col],col==0||col==1);
        for(int idx=0;idx<9;idx++){
            int current=(page-1)*9+idx;
            String family=FAMILIES[current];
            float top=(68+idx*18)*MM;
            float bottom=top+18*MM;
            FoodFreshnessEngine.Match m=best.get(family);
            boolean eligible=selected.isEmpty()||family.equals(selected);
            if(!eligible)m=null;
            RectF imageCell=new RectF(pos[1],top,pos[0],bottom);
            frame(c,imageCell,V7ReportLayoutKit.BLACK,.7f);
            RectF photo=new RectF(pos[1]+1.5f*MM,top+2*MM,pos[0]-1.5f*MM,bottom-2*MM);
            if(!image(ctx,c,family,photo))
                text(c,"-",photo,false,false);
            V7ReportLayoutKit.tableValue(c,new RectF(pos[2],top,pos[1],bottom),
                AR[current]+"\n"+family,true);
            V7ReportLayoutKit.tableValue(c,new RectF(pos[3],top,pos[2],bottom),
                m==null?"-":m.referenceLabel,false);
            V7ReportLayoutKit.tableValue(c,new RectF(pos[4],top,pos[3],bottom),
                m==null?"-":fmt(m.similarityIndex,1),false);
            V7ReportLayoutKit.tableValue(c,new RectF(pos[5],top,pos[4],bottom),
                m==null?"-":Integer.toString(m.overlappingSensors),false);
            V7ReportLayoutKit.tableValue(c,new RectF(pos[6],top,pos[5],bottom),
                m==null?"-":"Research only",false);
            if(!selected.isEmpty()&&selected.equals(family))
                frame(c,new RectF(L,top,L+W,bottom),V7ReportLayoutKit.AMBER,2f);
        }
        String chartName=page==1?
            "بصمة MQ الثمانية / 8-sensor fingerprint":
            "أيام مراجع الدراسة / Reference study days D1–D5";
        V7ReportLayoutKit.blankChart(c,new RectF(L,234*MM,L+W,274*MM),
            chartName,page==1?"MQ Sensor":"Study reference day",
            page==1?"Centered log10 Rs":"Similarity index");
        if(page==1)observedSourceChart(c,readings,result);
        else studyDayChart(c,result,readings);
    }
    /** Use measured source-comparable log shape and original references; no invented curve. */
    private static void observedSourceChart(Canvas c,Map<String,Double> readings,
                                           FoodFreshnessEngine.Result result){
        if(result.rankedTemplates.isEmpty())return;
        String selected=result.rankedTemplates.get(0).referenceLabel;
        double[] ref=null;
        for(FoodReferenceCatalog.Pattern p:FoodReferenceCatalog.patterns())
            if(p.label.equals(selected)){ref=p.vector();break;}
        if(ref==null)return;
        double[] obs=new double[8],reference=new double[8];boolean[] ok=new boolean[8];
        int n=0;double a=0,b=0;
        for(int i=0;i<8;i++){
            Double raw=readings.get(FoodReferenceCatalog.SENSOR_ORDER.get(i));
            if(raw!=null&&raw>0&&Double.isFinite(raw)&&ref[i]>0){
                obs[i]=Math.log10(raw);reference[i]=Math.log10(ref[i]);
                a+=obs[i];b+=reference[i];ok[i]=true;n++;
            }
        }
        if(n<2)return;
        a/=n;b/=n;double lo=Double.POSITIVE_INFINITY,hi=Double.NEGATIVE_INFINITY;
        for(int i=0;i<8;i++)if(ok[i]){
            obs[i]-=a;reference[i]-=b;
            lo=Math.min(lo,Math.min(obs[i],reference[i]));
            hi=Math.max(hi,Math.max(obs[i],reference[i]));
        }
        double span=Math.max(0.08,hi-lo);
        float left=L+17*MM,right=L+W-12*MM,top=246*MM,bottom=266*MM;
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setStrokeWidth(2.5f);
        for(int series=0;series<2;series++){
            p.setColor(series==0?0xFFE96920:0xFF1D65A2);
            float px=Float.NaN,py=Float.NaN;
            for(int i=0;i<8;i++){
                if(!ok[i]){px=Float.NaN;continue;}
                float x=left+(right-left)*i/7f;
                float y=bottom-(bottom-top)*(float)(((series==0?obs[i]:reference[i])-lo)/span);
                if(Float.isFinite(px))c.drawLine(px,py,x,y,p);
                c.drawCircle(x,y,2.5f,p);px=x;py=y;
            }
        }
        text(c,"Measured (orange) vs source (blue): "+selected,
             new RectF(L+4*MM,269*MM,L+W-4*MM,275*MM),false,false);
    }
    private static void studyDayChart(Canvas c,FoodFreshnessEngine.Result result,
                                      Map<String,Double> readings){
        if(result.rankedTemplates.isEmpty())return;
        String family=result.rankedTemplates.get(0).foodFamily;
        float left=L+18*MM,right=L+W-15*MM,top=244*MM,bottom=269*MM;
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFF1D65A2);p.setStrokeWidth(2.5f);
        float px=Float.NaN,py=Float.NaN;
        for(FoodFreshnessEngine.Match match:result.selectedFoodTrend){
            if(!match.foodFamily.equals(family)||match.studyDay<1||match.studyDay>5)continue;
            float x=left+(right-left)*(match.studyDay-1)/4f;
            float y=bottom-(bottom-top)*(float)(match.similarityIndex/100.0);
            if(Float.isFinite(px))c.drawLine(px,py,x,y,p);
            c.drawCircle(x,y,3,p);px=x;py=y;
        }
        FoodMeasurementDecision.Decision measured=FoodMeasurementDecision.fruit(readings,result.foodSelection);
        if(Double.isFinite(measured.fittedStudyPosition)){
            float mx=left+(right-left)*(float)((measured.fittedStudyPosition-1)/4.0);
            p.setColor(0xFFE96920);p.setStrokeWidth(2f);
            c.drawLine(mx,top,mx,bottom,p);
        }
        text(c,"Reference study positions D1-D5 - not actual storage days",
             new RectF(L+4*MM,270*MM,L+W-4*MM,275*MM),false,false);
    }
    /** Draw actually captured longitudinal MQ Rs samples; never interpolate missing frames. */
    private static void temporalCurve(Canvas c,Map<String,List<double[]>> trace,
                                      float left,float top,float right,float bottom){
        if(trace==null||trace.isEmpty()){
            text(c,"No time-series data in this archival export",
                new RectF(left,top,right,top+10*MM),false,false);
            return;
        }
        double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
        Map<String,Double> initial=new HashMap<>();
        for(String key:FoodReferenceCatalog.SENSOR_ORDER){
            List<double[]> list=trace.get(key);
            if(list==null||list.isEmpty())continue;
            for(double[] p:list){
                if(p==null||p.length<2||!Double.isFinite(p[0])||!Double.isFinite(p[1])||p[1]<=0)continue;
                if(!initial.containsKey(key))initial.put(key,p[1]);
                double v=Math.log10(p[1]/initial.get(key));
                if(Double.isFinite(v)){min=Math.min(min,v);max=Math.max(max,v);}
            }
        }
        if(!Double.isFinite(min)||!Double.isFinite(max))return;
        double span=Math.max(0.05,max-min);
        int[] colors={0xFF2962AA,0xFFCC5A27,0xFF2B9155,0xFF804BA7,
                      0xFFAD365C,0xFF117B86,0xFF825425,0xFF5B6472};
        Paint pen=new Paint(Paint.ANTI_ALIAS_FLAG);pen.setStrokeWidth(1.8f);
        for(int sensor=0;sensor<FoodReferenceCatalog.SENSOR_ORDER.size();sensor++){
            String key=FoodReferenceCatalog.SENSOR_ORDER.get(sensor);
            List<double[]> points=trace.get(key);
            if(points==null||points.size()<2||!initial.containsKey(key))continue;
            pen.setColor(colors[sensor]);
            float prevX=Float.NaN,prevY=Float.NaN;
            for(double[] point:points){
                if(point==null||point.length<2||point[1]<=0||!Double.isFinite(point[1])||
                   !Double.isFinite(point[0])){prevX=Float.NaN;continue;}
                double value=Math.log10(point[1]/initial.get(key));
                float x=left+(right-left)*(float)Math.max(0,Math.min(1,point[0]/180.0));
                float y=bottom-(bottom-top)*(float)((value-min)/span);
                if(Float.isFinite(prevX))c.drawLine(prevX,prevY,x,y,pen);
                prevX=x;prevY=y;
            }
        }
        text(c,"Measured relative Rs; 0-180 s | each line = one MQ channel",
            new RectF(left, bottom+1*MM,right,bottom+7*MM),false,false);
    }
    private static void measurementPage(Canvas c,Context ctx,int page,int total,
                                        FoodFreshnessEngine.Result result,
                                        Map<String,Double> readings,int samples,
                                        long durationMs,String id,String captureTimestampUtc,
                                        Map<String,List<double[]>> trace){
        header(c,ctx,page,total,captureTimestampUtc);
        FoodMeasurementDecision.Decision d=FoodMeasurementDecision.fruit(readings,result.foodSelection);
        V7ReportLayoutKit.tableHeader(c,new RectF(L,27*MM,L+W,45*MM),
            "نتيجة القياس المستقل / Independent fruit measurement",true);
        String stage=Double.isFinite(d.fittedStudyPosition)?
            "D"+fmt(d.fittedStudyPosition,2)+" (source curve position ONLY)":"NOT_AVAILABLE";
        V7ReportLayoutKit.tableValue(c,new RectF(L,48*MM,L+W,74*MM),
            "Reference match: "+d.referenceId+"  |  Status: "+d.status+
            "\nStudy curve position: "+stage+
            "  |  MQ: "+d.channels+"/8",false);
        V7ReportLayoutKit.tableValue(c,new RectF(L,77*MM,L+W,105*MM),
            "Study fingerprint distance: "+fmt(d.referenceDistance,5)+
            "  |  Relative competitor separation: "+
            (Double.isFinite(d.relativeSeparation)?fmt(d.relativeSeparation*100,2)+"%":"-")+
            "\nDiagnostic separation is not confidence/probability.",false);
        V7ReportLayoutKit.tableHeader(c,new RectF(L,110*MM,L+W,126*MM),
            "القراءات الفعلية / Frozen 180-second source-comparable MQ resistance",true);
        int k=0;
        for(String sensor:FoodReferenceCatalog.SENSOR_ORDER){
            float top=(125+k*8)*MM;
            Double v=readings.get(sensor);
            V7ReportLayoutKit.tableValue(c,new RectF(L,top,L+W,top+8*MM),
                sensor+"  |  Rs: "+(v!=null&&Double.isFinite(v)?fmt(v,5):"NOT_MEASURED"),false);
            k++;
        }
        V7ReportLayoutKit.blankChart(c,new RectF(L,191*MM,L+W,241*MM),
            "منحنى الاستجابة الفعلي / Actual Tank 180s sensor trace",
            "Time 0-180 seconds","Centered log10 Rs change");
        temporalCurve(c,trace,L+14*MM,202*MM,L+W-10*MM,231*MM);
        V7ReportLayoutKit.tableHeader(c,new RectF(L,243*MM,L+W,255*MM),
            "قيود الاستدلال / Scientific limitations",true);
        V7ReportLayoutKit.tableValue(c,new RectF(L,257*MM,L+W,283*MM),
            "Study day index is NOT specimen age, ripeness, microbiology, or edibility."+
            "\nTank transfer calibration and independent food validation are NOT completed."+
            "\nSource: "+FoodReferenceCatalog.SOURCE_DOI,false);
    }
    private static void appendixPage(Canvas c,Context ctx,int page,int count,
                                     FoodFreshnessEngine.Result result,int start,
                                     String captureTimestampUtc){
        header(c,ctx,page,count,captureTimestampUtc);
        V7ReportLayoutKit.tableHeader(c,
            new RectF(L,27*MM,L+W,45*MM),
            "تفاصيل المراجع البحثية / Complete source reference matches",true);
        int at=0;
        // 8 rows per page; 12pt text with whole source IDs, no arbitrary top-7.
        for(int i=start;i<result.rankedTemplates.size()&&at<8;i++,at++){
            FoodFreshnessEngine.Match m=result.rankedTemplates.get(i);
            float top=(51+27*at)*MM,bot=top+26*MM;
            V7ReportLayoutKit.tableValue(c,new RectF(L,top,L+W,bot),
                "#"+(i+1)+"  "+m.referenceLabel+"  |  Study Day: "+
                (m.studyDay>0?"D"+m.studyDay:"-")+"\nIndex: "+
                fmt(m.similarityIndex,2)+" / 100   |   Shape distance: "+
                fmt(m.logShapeDistance,4)+"   |   MQ: "+m.overlappingSensors,
                false);
        }
        text(c,"Similarity describes reference fingerprint matching, NOT food safety.",
            new RectF(L+4*MM,272*MM,L+W-4*MM,279*MM),false,true);
    }
    public static void write(Context ctx,OutputStream out,
                             FoodFreshnessEngine.Result result,
                             Map<String,Double> readings,int samples,long durationMs,
                             String reportId)throws Exception{
        write(ctx,out,result,readings,samples,durationMs,reportId,"-",null);
    }
    /** Optional original capture UTC; never replace with PDF export time. */
    public static void write(Context ctx,OutputStream out,
                             FoodFreshnessEngine.Result result,
                             Map<String,Double> readings,int samples,long durationMs,
                             String reportId,String captureTimestampUtc)throws Exception{
        write(ctx,out,result,readings,samples,durationMs,reportId,
              captureTimestampUtc,null);
    }
    public static void write(Context ctx,OutputStream out,
                             FoodFreshnessEngine.Result result,
                             Map<String,Double> readings,int samples,long durationMs,
                             String reportId,Map<String,List<double[]>> trace)throws Exception{
        write(ctx,out,result,readings,samples,durationMs,reportId,"-",trace);
    }
    public static void write(Context ctx,OutputStream out,
                             FoodFreshnessEngine.Result result,
                             Map<String,Double> readings,int samples,long durationMs,
                             String reportId,String captureTimestampUtc,
                             Map<String,List<double[]>> trace)throws Exception{
        if(ctx==null||out==null||result==null)
            throw new IllegalArgumentException("Frozen fruit result required");
        // Main matrix contains all eighteen source-selectable fruit/mix families.
        int annexPages=(result.rankedTemplates.size()+7)/8;
        int pages=3+annexPages;
        Map<String,FoodFreshnessEngine.Match> matches=best(result);
        PdfDocument pdf=new PdfDocument();
        try{
            for(int i=1;i<=pages;i++){
                PdfDocument.Page p=V7ReportLayoutKit.open(pdf,i,false);
                Canvas c=p.getCanvas();c.drawColor(Color.WHITE);
                if(i<=2)mainPage(c,ctx,i,pages,result,readings,samples,
                    durationMs,reportId,matches,captureTimestampUtc);
                else if(i==3)measurementPage(c,ctx,i,pages,result,readings,samples,
                    durationMs,reportId,captureTimestampUtc,trace);
                else appendixPage(c,ctx,i,pages,result,(i-4)*8,captureTimestampUtc);
                pdf.finishPage(p);
            }
            pdf.writeTo(out);
        }finally{pdf.close();}
    }
}
