package com.hazrob.tank.foodfreshness;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** One finished food-headspace operation => one phone-generated PDF. No gas report adapter. */
public final class FoodFreshnessPdf {
    private FoodFreshnessPdf(){}
    private static final int W=595,H=842;
    private static void text(Canvas c,Paint p,String value,float x,float y,int size,boolean bold){
        p.setColor(Color.rgb(25,38,57));p.setTextSize(size);
        p.setTypeface(bold?android.graphics.Typeface.DEFAULT_BOLD:android.graphics.Typeface.DEFAULT);
        c.drawText(value,x,y,p);
    }
    public static Uri save(Context context,FoodFreshnessEngine.Result result,
                           Map<String,Double> readings,int samples,long durationMs,
                           String reportId) throws Exception {
        if(context==null||result==null||reportId==null)throw new IllegalArgumentException("missing report");
        PdfDocument pdf=new PdfDocument();
        try{
            PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(W,H,1).create());
            Canvas c=page.getCanvas();
            c.drawColor(Color.WHITE);
            Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
            text(c,p,"HAZROB TANK | FOOD FRESHNESS RESEARCH",36,42,17,true);
            text(c,p,"Independent food headspace measurement - single operation",36,65,10,false);
            p.setColor(Color.rgb(222,231,239));c.drawRect(36,74,559,76,p);
            text(c,p,"Report ID: "+reportId,36,100,10,false);
            text(c,p,"Food family: "+result.foodSelection,36,117,10,false);
            text(c,p,"Duration: "+String.format(Locale.US,"%.1f",durationMs/1000.0)+" s"
                     +" | samples: "+samples+" | measured MQ: "+result.usableSensorCount+"/8",
                36,135,10,false);
            text(c,p,"Raw resistance profile (instrument units; research only)",36,165,11,true);
            int y=182;
            for(String key:FoodReferenceCatalog.SENSOR_ORDER){
                Double val=readings.get(key);
                text(c,p,key+" : "+(val==null?"no measurement":String.format(Locale.US,"%.6g",val)),
                     46,y,10,false);
                y+=17;
            }
            text(c,p,"Closest published reference patterns",36,340,11,true);
            y=360;
            int count=Math.min(7,result.rankedTemplates.size());
            for(int i=0;i<count;i++){
                FoodFreshnessEngine.Match m=result.rankedTemplates.get(i);
                String label=m.referenceLabel;
                if(label.length()>30)label=label.substring(0,30);
                text(c,p,(i+1)+". "+label,45,y,10,false);
                text(c,p,String.format(Locale.US,"Similarity index %.2f / 100",m.similarityIndex),
                     300,y,9,false);
                y+=19;
            }
            text(c,p,"Similarity by study day (not microbial concentration)",36,532,11,true);
            List<FoodFreshnessEngine.Match> trend=result.selectedFoodTrend;
            if(!trend.isEmpty()){
                float left=65,right=536,top=558,bottom=681;
                p.setColor(Color.rgb(208,218,230));p.setStrokeWidth(1.1f);
                c.drawLine(left,bottom,right,bottom,p);c.drawLine(left,top,left,bottom,p);
                int maxDay=0;for(FoodFreshnessEngine.Match m:trend)maxDay=Math.max(maxDay,m.studyDay);
                float prevX=Float.NaN,prevY=Float.NaN;
                for(FoodFreshnessEngine.Match m:trend){
                    float x=left+(right-left)*Math.max(0,m.studyDay-1)/Math.max(1,maxDay-1);
                    float yy=bottom-(bottom-top)*(float)(m.similarityIndex/100.0);
                    p.setColor(Color.rgb(28,102,163));p.setStrokeWidth(2);
                    if(Float.isFinite(prevX))c.drawLine(prevX,prevY,x,yy,p);
                    c.drawCircle(x,yy,3,p);
                    text(c,p,"D"+m.studyDay,x-7,bottom+15,8,false);
                    prevX=x;prevY=yy;
                }
            }else text(c,p,"No day-labelled reference patterns for the selection",46,580,10,false);
            p.setColor(Color.rgb(222,231,239));c.drawRect(36,727,559,729,p);
            text(c,p,"SCIENTIFIC LIMITATION",36,747,10,true);
            text(c,p,"Pattern similarity is NOT microbiological testing, spoilage proof,",36,763,9,false);
            text(c,p,"food safety certification, gas identification, or quantitative ppm.",36,777,9,false);
            text(c,p,"Source: Zenodo DOI 10.5281/zenodo.17285312; 59 reference profiles.",36,793,8,false);
            pdf.finishPage(page);
            // App-owned Downloads on Android 10+: no external storage permission,
            // no modification to firmware, gas engine, or field session reports.
            if(Build.VERSION.SDK_INT>=29){
                ContentValues values=new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME,"HazRob-Food-"+reportId+".pdf");
                values.put(MediaStore.MediaColumns.MIME_TYPE,"application/pdf");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                           Environment.DIRECTORY_DOWNLOADS+"/HazRobTank/");
                Uri uri=context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
                if(uri==null)throw new java.io.IOException("Unable to create Downloads file");
                try(OutputStream output=context.getContentResolver().openOutputStream(uri)){
                    if(output==null)throw new java.io.IOException("Unable to write PDF");
                    pdf.writeTo(output);
                }
                return uri;
            }
            throw new UnsupportedOperationException("Document export uses Android 10+ MediaStore");
        }finally{pdf.close();}
    }
}
