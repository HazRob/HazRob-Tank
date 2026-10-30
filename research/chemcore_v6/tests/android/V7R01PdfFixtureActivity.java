package com.servoc.app.report;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import com.hazrob.tank.chemcore.v6.ChemCoreTankV6;
import com.hazrob.tank.v6integration.TankV6TelemetryAdapter;
import com.hazrob.tank.foodfreshness.FoodFreshnessEngine;
import com.hazrob.tank.foodfreshness.FoodFreshnessPdf;
import com.hazrob.tank.foodfreshness.FoodReferenceCatalog;
import com.hazrob.tank.foodfreshness.meat.BeefResearchCatalog;
import com.hazrob.tank.foodfreshness.meat.BeefResearchEngine;
import com.hazrob.tank.foodfreshness.meat.BeefResearchPdf;
import com.servoc.app.hardware.HazRobHardwareProfile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

public final class V7R01PdfFixtureActivity extends Activity {
    private static final String[] MQ={"mq2","mq4","mq5","mq8","mq3","mq7","mq9","mq135"};
    private final JSONObject status=new JSONObject();

    private File output(String name){return new File(getFilesDir(),name);}
    private void store(String name,ByteArrayOutputStream output)throws Exception {
        try(FileOutputStream out=new FileOutputStream(output(name))){out.write(output.toByteArray());}
    }
    private void copy(String name,Uri uri)throws Exception{
        try(InputStream in=getContentResolver().openInputStream(uri);
            FileOutputStream out=new FileOutputStream(output(name))){
            if(in==null)throw new IOException("No MediaStore stream for "+name);
            byte[] buf=new byte[16384];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);
        }
    }
    private int verify(String file)throws Exception {
        File f=output(file);
        if(!f.isFile()||f.length()<1500)throw new IllegalStateException("Missing PDF: "+file);
        try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer render=new PdfRenderer(fd)){
            int pages=render.getPageCount();
            if(pages<1)throw new IllegalStateException("No pages "+file);
            for(int i=0;i<pages;i++){
                try(PdfRenderer.Page page=render.openPage(i)){
                    Bitmap img=Bitmap.createBitmap(595,842,Bitmap.Config.ARGB_8888);
                    img.eraseColor(Color.WHITE);
                    page.render(img,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    int count=0;
                    for(int y=28;y<810;y+=12)for(int x=25;x<565;x+=12){
                        int p=img.getPixel(x,y);
                        if(Color.red(p)<230||Color.green(p)<230||Color.blue(p)<230)count++;
                    }
                    img.recycle();
                    if(count<10)throw new IllegalStateException("Blank PDF page: "+file+" #"+i);
                }
            }
            status.put(file,new JSONObject().put("size_bytes",f.length()).put("pages",pages));
            return pages;
        }
    }
    private static JSONObject channel(int n)throws Exception{
        JSONObject x=new JSONObject();
        double r0=65.0+5*n;
        double rs=r0*(0.67+0.033*n);
        x.put("ok",true).put("powered",true).put("rs",rs).put("r0",r0);
        x.put("ratio",rs/r0);
        x.put("r0_source","CI_TEST_FIXTURE_NOT_DEVICE_CALIBRATION");
        return x;
    }
    /** Explicitly blank, nonclinical design fixture: exercises V7 shared kit in Android Skia. */
    private void exerciseV7BlankLayoutKit()throws Exception {
        PdfDocument pdf=new PdfDocument();
        try {
            PdfDocument.Page portrait=V7ReportLayoutKit.open(pdf,1,false);
            android.graphics.Canvas c=portrait.getCanvas();
            c.drawColor(Color.WHITE);
            V7ReportLayoutKit.drawHeader(c,this,false,
                "فحص قالب التقرير", "Blank Layout Kit QA", "-");
            V7ReportLayoutKit.tableHeader(c,new RectF(20,125,310,165),
                "قيمة الحساس / Sensor value",true);
            V7ReportLayoutKit.tableValue(c,new RectF(20,165,310,205),"-",true);
            // A non-existent value must not be red even if an inconsistent
            // upstream display flag is true.
            V7ReportLayoutKit.measuredValue(c,new RectF(310,165,570,205),
                "-",true,true);
            V7ReportLayoutKit.blankChart(c,new RectF(20,260,570,710),
                "مخطط فارغ / Blank Chart","Time","MQ");
            V7ReportLayoutKit.drawFooter(c,false,1,2);
            pdf.finishPage(portrait);
            PdfDocument.Page landscape=V7ReportLayoutKit.open(pdf,2,true);
            c=landscape.getCanvas();c.drawColor(Color.WHITE);
            V7ReportLayoutKit.drawHeader(c,this,true,
                "أربع لوحات قياس متساوية", "Four Equal Blank Charts", "-");
            float x1=20,x2=430,y1=100,y2=300,w=390,h=170;
            V7ReportLayoutKit.blankChart(c,new RectF(x1,y1,x1+w,y1+h),
                "مخطط أول / Plot 1","X","Y");
            V7ReportLayoutKit.blankChart(c,new RectF(x2,y1,x2+w,y1+h),
                "مخطط ثان / Plot 2","X","Y");
            V7ReportLayoutKit.blankChart(c,new RectF(x1,y2,x1+w,y2+h),
                "مخطط ثالث / Plot 3","X","Y");
            V7ReportLayoutKit.blankChart(c,new RectF(x2,y2,x2+w,y2+h),
                "مخطط رابع / Plot 4","X","Y");
            V7ReportLayoutKit.drawFooter(c,true,2,2);
            pdf.finishPage(landscape);
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            pdf.writeTo(out);
            store("V7_Blank_Layout_Kit_Actual_Android.pdf",out);
        } finally { pdf.close(); }
        verify("V7_Blank_Layout_Kit_Actual_Android.pdf");
        status.put("v7_shared_layout_kit","ANDROID_2_PAGE_PORTRAIT_LANDSCAPE_BLANK_RENDER");
    }

    private void execute()throws Exception {
        com.servoc.app.chemical.GhsPictograms.bindContext(this);
        TankInstantV7PdfWriter.bindContext(this);
        JSONObject telemetry=new JSONObject(),mq=new JSONObject();
        for(int i=0;i<MQ.length;i++)mq.put(MQ[i],channel(i));
        telemetry.put("mq",mq);
        telemetry.put("env",new JSONObject().put("ok",true).put("temperature",26.4)
            .put("humidity",52.5).put("pressure",1006.7));
        telemetry.put("sound",new JSONObject().put("dba",85.7).put("calibrated_dba",false)
            .put("db_spl",79.3).put("rms_v",0.0276).put("p2p_v",0.0831));
        telemetry.put("ldr",new JSONObject().put("installed",true));
        telemetry.put("relay",new JSONObject().put("power_ms",480000));
        // Crucially, the actual production V6 engine and JSON adapter produce the analytical
        // object. Test input has NO valid Tank clean-air R0 provenance and cannot create gas alarms.
        ChemCoreTankV6.Result analysis=TankV6TelemetryAdapter.analyzeSnapshot(telemetry);
        JSONObject actual=TankV6TelemetryAdapter.toJson(analysis,telemetry);
        if(!TankV6TelemetryAdapter.SCHEMA.equals(actual.optString("schema")) ||
            actual.optBoolean("reportable_ppm",true))
            throw new IllegalStateException("Not V6 production scientific boundary");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        TankInstantReportPdf.write(bytes,telemetry,actual,"CI-DEMO-V6-ONE-SHOT",1793376000000L);
        exerciseV7BlankLayoutKit();
        store("V7_R01_Instant_ChemCore_Approved_Layout_Test.pdf",bytes);
        verify("V7_R01_Instant_ChemCore_Approved_Layout_Test.pdf");
        bytes=new ByteArrayOutputStream();
        TankPhysicalSnapshotPdf.write(bytes,telemetry,"2026-10-30 20:45:00 UTC");
        store("02_V6_Physical_Snapshot_Original.pdf",bytes);
        verify("02_V6_Physical_Snapshot_Original.pdf");

        // Fruit values are an illustrative copy of one published source pattern multiplied
        // by a common gain. They are NOT a real Tank observation.
        FoodReferenceCatalog.Pattern fruit=null;
        for(FoodReferenceCatalog.Pattern p:FoodReferenceCatalog.patterns())
            if("Banana D3".equals(p.label)){fruit=p;break;}
        if(fruit==null)throw new IllegalStateException("Fruit reference missing");
        List<FoodFreshnessEngine.Reading> observations=new ArrayList<>();
        Map<String,Double> fruitReadings=new LinkedHashMap<>();
        double[] fruitSource=fruit.vector();
        for(int i=0;i<FoodReferenceCatalog.SENSOR_ORDER.size();i++){
            String sensor=FoodReferenceCatalog.SENSOR_ORDER.get(i);
            double val=12*fruitSource[i]*(1.0+0.002*i);
            fruitReadings.put(sensor,val);
            observations.add(new FoodFreshnessEngine.Reading(sensor,val));
        }
        FoodFreshnessEngine.Result fruitResult=FoodFreshnessEngine.analyzeFruit(observations,"Banana");
        if(fruitResult.rankedTemplates.isEmpty())throw new IllegalStateException("No fruit ranking");
        Uri fruitUri=FoodFreshnessPdf.save(this,fruitResult,fruitReadings,177,180000L,"CI-V6-FRUIT");
        copy("05_V6_Fruit_Freshness_Original.pdf",fruitUri);
        verify("05_V6_Fruit_Freshness_Original.pdf");

        Map<String,Double> beef=new LinkedHashMap<>();
        double[] profile=BeefResearchCatalog.classes().get(0).logShape();
        for(int i=0;i<BeefResearchCatalog.SENSOR_ORDER.size();i++)
            beef.put(BeefResearchCatalog.SENSOR_ORDER.get(i),Math.pow(10,3.6+profile[i])*(1+.008*i));
        BeefResearchEngine.Result beefResult=BeefResearchEngine.analyze(beef);
        if(beefResult.rankedClasses.size()<4)throw new IllegalStateException("Missing beef classes");
        Uri meatUri=BeefResearchPdf.save(this,beefResult,beef,179,180000L,
            "CI-V6-MEAT",26.4,52.5);
        copy("06_V6_Meat_Freshness_Original.pdf",meatUri);
        verify("06_V6_Meat_Freshness_Original.pdf");
        status.put("status","PASS");
        status.put("data_origin","EXPLICIT_SYNTHETIC_TEST_FIXTURES_NOT_PHYSICAL_TANK");
        status.put("renderer","ACTUAL_ANDROID_SKIA_PRODUCTION_PDF_GENERATORS");
        status.put("v7_r01_layout","HEADER+LOGO+MQ-TABLE+EVIDENCE-MATRIX+RADAR");
        status.put("scientific_status","UNKNOWN_G3_ZERO_NO_CERTIFIED_PPM");
        status.put("production_apk_generated",false);
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        new Thread(()->{
            try{execute();}
            catch(Throwable error){
                try{
                    status.put("status","FAIL").put("error",error.toString());
                    StringWriter w=new StringWriter();error.printStackTrace(new PrintWriter(w));
                    status.put("stacktrace",w.toString());
                }catch(Exception ignored){}
                android.util.Log.e("V7R01REPORTS","report fixture failed",error);
            }
            try(FileOutputStream out=openFileOutput("v7_r01_pdf_status.json",MODE_PRIVATE)){
                out.write(status.toString(2).getBytes(StandardCharsets.UTF_8));
            }catch(Exception ignored){}
            runOnUiThread(this::finish);
        }).start();
    }
}