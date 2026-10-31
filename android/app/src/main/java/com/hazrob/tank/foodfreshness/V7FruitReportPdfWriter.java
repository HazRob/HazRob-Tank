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
            "تقرير بصمة الفواكه","Fruit Research Fingerprint",
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
        float[] pos=new float[COL.length+1];pos[0]=L;
        for(int i=0;i<COL.length;i++)pos[i+1]=pos[i]+COL[i]*MM;
        for(int col=0;col<COL.length;col++)
            V7ReportLayoutKit.tableHeader(c,
                new RectF(pos[col],headerTop,pos[col+1],headerTop+16*MM),
                HEAD[col],col==0||col==1);
        for(int idx=0;idx<9;idx++){
            int current=(page-1)*9+idx;
            String family=FAMILIES[current];
            float top=(68+idx*18)*MM;
            float bottom=top+18*MM;
            FoodFreshnessEngine.Match m=best.get(family);
            boolean eligible=selected.isEmpty()||family.equals(selected);
            if(!eligible)m=null;
            RectF imageCell=new RectF(pos[0],top,pos[1],bottom);
            frame(c,imageCell,V7ReportLayoutKit.BLACK,.7f);
            RectF photo=new RectF(pos[0]+1.5f*MM,top+2*MM,pos[1]-1.5f*MM,bottom-2*MM);
            if(!image(ctx,c,family,photo))
                text(c,"-",photo,false,false);
            V7ReportLayoutKit.tableValue(c,new RectF(pos[1],top,pos[2],bottom),
                AR[current]+"\n"+family,true);
            V7ReportLayoutKit.tableValue(c,new RectF(pos[2],top,pos[3],bottom),
                m==null?"-":m.referenceLabel,false);
            V7ReportLayoutKit.tableValue(c,new RectF(pos[3],top,pos[4],bottom),
                m==null?"-":fmt(m.similarityIndex,1),false);
            V7ReportLayoutKit.tableValue(c,new RectF(pos[4],top,pos[5],bottom),
                m==null?"-":Integer.toString(m.overlappingSensors),false);
            V7ReportLayoutKit.tableValue(c,new RectF(pos[5],top,pos[6],bottom),
                m==null?"-":"Research only",false);
            if(!selected.isEmpty()&&selected.equals(family))
                frame(c,new RectF(L,top,L+W,bottom),V7ReportLayoutKit.AMBER,2f);
        }
        String chartName=page==1?
            "بصمة MQ الثمانية / 8-sensor fingerprint":
            "أيام مراجع الدراسة / Reference study days D1–D5";
        V7ReportLayoutKit.blankChart(c,new RectF(L,234*MM,L+W,274*MM),
            chartName,page==1?"MQ Sensor":"Study reference day",
            page==1?"Observed resistance":"Similarity index");
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
        write(ctx,out,result,readings,samples,durationMs,reportId,"-");
    }
    /** Optional original capture UTC; never replace with PDF export time. */
    public static void write(Context ctx,OutputStream out,
                             FoodFreshnessEngine.Result result,
                             Map<String,Double> readings,int samples,long durationMs,
                             String reportId,String captureTimestampUtc)throws Exception{
        if(ctx==null||out==null||result==null)
            throw new IllegalArgumentException("Frozen fruit result required");
        // Main matrix contains all eighteen source-selectable fruit/mix families.
        int annexPages=(result.rankedTemplates.size()+7)/8;
        int pages=2+annexPages;
        Map<String,FoodFreshnessEngine.Match> matches=best(result);
        PdfDocument pdf=new PdfDocument();
        try{
            for(int i=1;i<=pages;i++){
                PdfDocument.Page p=V7ReportLayoutKit.open(pdf,i,false);
                Canvas c=p.getCanvas();c.drawColor(Color.WHITE);
                if(i<=2)mainPage(c,ctx,i,pages,result,readings,samples,
                    durationMs,reportId,matches,captureTimestampUtc);
                else appendixPage(c,ctx,i,pages,result,(i-3)*8,captureTimestampUtc);
                pdf.finishPage(p);
            }
            pdf.writeTo(out);
        }finally{pdf.close();}
    }
}
