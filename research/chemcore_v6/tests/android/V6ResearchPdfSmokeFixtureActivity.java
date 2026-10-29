package com.servoc.app.report;

import android.app.Activity;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * N06-C TEST ONLY. Never copied into production Tank source, only into
 * temporary GitHub CI baseline checkout for an Android emulator smoke run.
 * Exercises the EXACT current private V6 research appendix page renderer.
 * Fixture is NOT an approved real field session or Tank G3 experiment.
 */
public final class V6ResearchPdfSmokeFixtureActivity extends Activity {
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        new Thread(()->{
            JSONObject result=new JSONObject();
            try{
                execute();
                result.put("status","PASS");
                result.put("scope","TEST_FIXTURE_RENDER_OF_PRODUCTION_V6_PDF_APPENDIX_PAGE_ONLY");
                result.put("approved_field_db_consumed",false);
                result.put("gas_identity_certified",false);
                result.put("reportable_ppm",false);
                result.put("physical_tank_g3",0);
                result.put("pages_created",3);
                result.put("pages_rasterized_with_android_pdf_renderer",3);
            }catch(Throwable e){
                try{result.put("status","FAIL");result.put("error",e.toString());}
                catch(Exception ignored){}
            }
            try(FileOutputStream f=openFileOutput("v6_pdf_status.json",MODE_PRIVATE)){
                f.write(result.toString(2).getBytes(StandardCharsets.UTF_8));
            }catch(Exception ignored){}
            runOnUiThread(this::finish);
        }).start();
    }

    private static JSONObject testSource()throws Exception{
        JSONObject root=new JSONObject().put("schema",
            "hazrob-tank-v6-approved-field-readonly/1")
            .put("reported_identity","UNKNOWN")
            .put("reportable_ppm",false)
            .put("physical_tank_g3_validated",false)
            .put("no_db_write",true);
        JSONArray cycles=new JSONArray();
        String[] models={"MQ2","MQ3","MQ4","MQ5","MQ7","MQ8","MQ9","MQ135"};
        for(int i=0;i<7;i++){
            JSONObject c=new JSONObject()
                .put("measurement_session_id","MS-TEST-CYCLE-"+i)
                .put("cycle_id","FC-TEST-"+i)
                .put("research_source_sha256",String.format("%064x",i+1))
                .put("original_archived_telemetry_rows",75)
                .put("independently_evaluated_sensor_models",6);
            JSONArray arr=new JSONArray();
            for(int k=0;k<8;k++){
                JSONObject s=new JSONObject()
                    .put("sensor_model",models[k])
                    .put("distinct_original_scan_cycles",k<6?20:5)
                    .put("state",k<6?"SOURCE_ONLY_RESEARCH_UNVALIDATED":
                         "INSUFFICIENT_UNIQUE_SOURCE_SCANS");
                if(k<6)s.put("selected_original_rs",89.0+k+i)
                    .put("selected_original_r0",k%2==0?JSONObject.NULL:78.0+k);
                arr.put(s);
            }
            cycles.put(c.put("sensors",arr));
        }
        return root.put("cycles",cycles).put("approved_gas_cycles",cycles.length());
    }

    private void execute()throws Exception{
        JSONObject source=testSource();
        PdfDocument output=new PdfDocument();
        Method page=SmartFieldSessionPdfWriter.class.getDeclaredMethod(
            "v6ResearchAppendixPage",Canvas.class,
            FieldSessionReportExporter.Bundle.class,JSONObject.class,int.class);
        page.setAccessible(true);
        for(int i=0;i<3;i++){
            PdfDocument.Page p=output.startPage(
                new PdfDocument.PageInfo.Builder(842,595,i+1).create());
            p.getCanvas().drawColor(Color.WHITE);
            page.invoke(null,p.getCanvas(),null,source,i);
            output.finishPage(p);
        }
        File pdf=new File(getFilesDir(),"v6_pdf_fixture.pdf");
        try(FileOutputStream f=new FileOutputStream(pdf)){output.writeTo(f);}
        finally{output.close();}
        if(pdf.length()<3000)throw new IllegalStateException("PDF unexpectedly small");
        try(ParcelFileDescriptor descriptor=ParcelFileDescriptor.open(
                pdf,ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer renderer=new PdfRenderer(descriptor)){
            if(renderer.getPageCount()!=3)
                throw new IllegalStateException("PDF expected exactly three pages");
            for(int i=0;i<renderer.getPageCount();i++){
                try(PdfRenderer.Page p=renderer.openPage(i)){
                    Bitmap b=Bitmap.createBitmap(842,595,Bitmap.Config.ARGB_8888);
                    b.eraseColor(Color.WHITE);
                    p.render(b,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    int dark=0;
                    for(int y=10;y<565;y+=3)
                        for(int x=15;x<825;x+=3){
                            int pix=b.getPixel(x,y);
                            if(Color.red(pix)<210||Color.green(pix)<210||
                               Color.blue(pix)<210)dark++;
                        }
                    if(dark<450)
                        throw new IllegalStateException("PDF page "+i+
                          " contains almost no rendered content: "+dark);
                    File image=new File(getFilesDir(),
                         "v6_pdf_fixture_page_"+(i+1)+".png");
                    try(FileOutputStream out=new FileOutputStream(image)){
                        b.compress(Bitmap.CompressFormat.PNG,100,out);
                    }
                    b.recycle();
                }
            }
        }
    }
}
