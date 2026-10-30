package com.hazrob.tank.foodfreshness.meat;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/** Stable beef-only export facade; retain Android MediaStore name/location. */
public final class BeefResearchPdf {
    private BeefResearchPdf(){}
    public static Uri save(Context ctx,BeefResearchEngine.Result result,
                           Map<String,Double> channels,int sampleCount,
                           long durationMs,String id,double temperatureC,
                           double humidityPct) throws Exception {
        if(ctx==null||result==null||id==null||id.trim().isEmpty())
            throw new IllegalArgumentException("missing beef report input");
        if(Build.VERSION.SDK_INT<29)
            throw new UnsupportedOperationException("Report export requires Android 10+");
        ContentValues meta=new ContentValues();
        meta.put(MediaStore.MediaColumns.DISPLAY_NAME,"HazRob-Beef-"+id+".pdf");
        meta.put(MediaStore.MediaColumns.MIME_TYPE,"application/pdf");
        meta.put(MediaStore.MediaColumns.RELATIVE_PATH,
            Environment.DIRECTORY_DOWNLOADS+"/HazRobTank/");
        Uri uri=ctx.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,meta);
        if(uri==null)throw new IOException("Beef PDF URI not created");
        try(OutputStream out=ctx.getContentResolver().openOutputStream(uri)){
            if(out==null)throw new IOException("Beef PDF output unavailable");
            V7BeefResearchPdfWriter.write(ctx,out,result,channels,
                sampleCount,durationMs,id,temperatureC,humidityPct);
            return uri;
        }catch(Exception e){
            ctx.getContentResolver().delete(uri,null,null);
            throw e;
        }
    }
}
