package com.hazrob.tank.foodfreshness.meat;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.OutputStream;
import java.util.Locale;
import java.util.Map;

/** One independent beef research session produces one PDF, never a gas report. */
public final class BeefResearchPdf {
    private BeefResearchPdf(){}
    private static void t(Canvas c,Paint p,String message,int x,int y,int size,boolean strong){
        p.setColor(Color.rgb(22,36,52));p.setTypeface(strong?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);
        p.setTextSize(size);c.drawText(message,x,y,p);
    }
    public static Uri save(Context ctx,BeefResearchEngine.Result result,
                           Map<String,Double> channels,int sampleCount,
                           long durationMs,String id) throws Exception{
        if(ctx==null||result==null||id==null)throw new IllegalArgumentException("missing beef report input");
        PdfDocument pdf=new PdfDocument();
        try{
            PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,1).create());
            Canvas c=page.getCanvas();c.drawColor(Color.WHITE);
            Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
            t(c,p,"HAZROB TANK | BEEF HEADSPACE RESEARCH",35,42,16,true);
            t(c,p,"Separate meat workflow - 2018 Wijaya e-nose data",35,65,10,false);
            p.setColor(0xFFCBD5E1);c.drawLine(35,74,560,74,p);
            t(c,p,"Report: "+id+" | Scan: "+String.format(Locale.US,"%.1f",durationMs/1000.0)+
                "s | snapshots: "+sampleCount,35,96,10,false);
            t(c,p,"Source model: Wijaya TS1-TS4 training / TS5 held out",35,114,10,false);
            t(c,p,"Seven matching sensor types: MQ2 MQ3 MQ4 MQ5 MQ8 MQ9 MQ135",35,134,9,false);
            t(c,p,"Source resistances: geomean-centered log10 Rs shape",35,151,9,false);
            t(c,p,"Tank captured channels",35,179,11,true);
            int y=199;
            for(String sensor:BeefResearchCatalog.SENSOR_ORDER){
                Double rs=channels.get(sensor);
                t(c,p,sensor+": "+(rs==null?"no sample":String.format(Locale.US,"%.6g",rs)),46,y,10,false);
                y+=20;
            }
            t(c,p,"Comparison to published four beef class labels",35,367,11,true);
            y=388;
            for(BeefResearchEngine.Match m:result.rankedClasses){
                t(c,p,m.classCode+" ("+m.trainingRows+" source rows)",45,y,11,false);
                t(c,p,String.format(Locale.US,"distance %.4f | index %.1f/100",
                    m.distance,m.descriptiveSimilarity),295,y,9,false);
                y+=25;
            }
            t(c,p,"Research similarity chart (not measured microbial TVC)",35,510,11,true);
            float left=57,right=530,top=535,bottom=671;
            p.setColor(0xFFCCD6E2);p.setStrokeWidth(1.5f);
            c.drawLine(left,bottom,right,bottom,p);c.drawLine(left,top,left,bottom,p);
            for(int i=0;i<result.rankedClasses.size();i++){
                BeefResearchEngine.Match m=result.rankedClasses.get(i);
                float x=left+10+i*((right-left-20)/4f);
                float barHeight=(bottom-top)*(float)(m.descriptiveSimilarity/100.);
                p.setColor(0xFF2C76B8);
                c.drawRect(x,bottom-barHeight,x+48,bottom,p);
                t(c,p,m.classCode.substring(0,Math.min(6,m.classCode.length())),
                    (int)x,(int)bottom+18,8,false);
            }
            t(c,p,"VALIDATION LIMITATIONS",35,725,11,true);
            t(c,p,String.format(Locale.US,
                "Held-out cut accuracy %.1f%%; balanced accuracy %.1f%%.",
                100*BeefResearchCatalog.HELDOUT_ACCURACY,
                100*BeefResearchCatalog.HELDOUT_BALANCED_ACCURACY),
                35,744,9,false);
            t(c,p,"Only a 3-minute scan; original beef data cover 36 hours per cut.",35,761,9,false);
            t(c,p,"Not a lab TVC measurement or clearance for human consumption.",35,777,9,false);
            t(c,p,"Dataset DOI: 10.17632/mwmhh766fc.3 - research only.",35,792,9,false);
            pdf.finishPage(page);
            if(Build.VERSION.SDK_INT<29)
                throw new UnsupportedOperationException("Report export requires Android 10+");
            ContentValues meta=new ContentValues();
            meta.put(MediaStore.MediaColumns.DISPLAY_NAME,"HazRob-Beef-"+id+".pdf");
            meta.put(MediaStore.MediaColumns.MIME_TYPE,"application/pdf");
            meta.put(MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS+"/HazRobTank/");
            Uri uri=ctx.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,meta);
            if(uri==null)throw new java.io.IOException("PDF URI not created");
            try(OutputStream out=ctx.getContentResolver().openOutputStream(uri)){
                if(out==null)throw new java.io.IOException("PDF output unavailable");
                pdf.writeTo(out);
            }
            return uri;
        }finally{pdf.close();}
    }
}
