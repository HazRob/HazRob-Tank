package com.hazrob.tank.foodfreshness;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.List;

/** Food-only fruit PDF: approved V7 brand and original reference matrix,
 * plus independently calculated single-scan source study day measurements.
 * Never invokes the gas ChemCore or creates a user fingerprint.
 */
public final class FoodFreshnessPdf {
    private FoodFreshnessPdf(){}
    public static Uri save(Context ctx,FoodFreshnessEngine.Result result,
                           Map<String,Double> readings,int samples,long durationMs,
                           String id)throws Exception {
        return save(ctx,result,readings,samples,durationMs,id,null);
    }
    public static Uri save(Context ctx,FoodFreshnessEngine.Result result,
                           Map<String,Double> readings,int samples,long durationMs,
                           String id,Map<String,List<double[]>> trace)throws Exception {
        if(ctx==null||result==null||readings==null||id==null||id.trim().isEmpty())
            throw new IllegalArgumentException("missing frozen food measurement");
        if(Build.VERSION.SDK_INT<29)
            throw new UnsupportedOperationException("Report export requires Android 10+");
        ContentValues meta=new ContentValues();
        meta.put(MediaStore.MediaColumns.DISPLAY_NAME,"HazRob-Food-"+id+".pdf");
        meta.put(MediaStore.MediaColumns.MIME_TYPE,"application/pdf");
        meta.put(MediaStore.MediaColumns.RELATIVE_PATH,
            Environment.DIRECTORY_DOWNLOADS+"/HazRobTank/");
        Uri uri=ctx.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,meta);
        if(uri==null)throw new IOException("Fruit report URI not created");
        try(OutputStream out=ctx.getContentResolver().openOutputStream(uri)){
            if(out==null)throw new IOException("Fruit PDF output unavailable");
            V7FruitReportPdfWriter.write(ctx,out,result,readings,samples,durationMs,id,trace);
            return uri;
        }catch(Exception e){
            ctx.getContentResolver().delete(uri,null,null);
            throw e;
        }
    }
}