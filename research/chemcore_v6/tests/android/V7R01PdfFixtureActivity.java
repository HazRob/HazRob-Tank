package com.servoc.app.report;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
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
import com.hazrob.tank.foodfreshness.V7FruitReportPdfWriter;
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
    /** Verify unlimited pagination and full source row preservation. */
    private void exerciseV7FlexiblePagination()throws Exception {
        List<V7ReportPagePlanner.Item> rows=new ArrayList<>();
        for(int i=0;i<18;i++)
            rows.add(new V7ReportPagePlanner.Item("fruit-family-"+i,38f));
        V7ReportPagePlanner.Section section=
            new V7ReportPagePlanner.Section("all-18-fruit-options",30f,rows);
        List<V7ReportPagePlanner.Page> pages=V7ReportPagePlanner.plan(
            100f,500f,Collections.singletonList(section));
        if(pages.size()!=2)
            throw new IllegalStateException("R05 dynamic pages != 2: "+pages.size());
        int rowCount=0,headingCount=0;
        for(V7ReportPagePlanner.Page page:pages)
            for(V7ReportPagePlanner.Element item:page.elements){
                if(item.sectionHeader)headingCount++;
                else rowCount++;
            }
        if(rowCount!=18 || headingCount!=2)
            throw new IllegalStateException("source rows dropped: "+rowCount);
        rows.clear();
        for(int i=0;i<45;i++)
            rows.add(new V7ReportPagePlanner.Item("arbitrary-extra-"+i,38f));
        List<V7ReportPagePlanner.Page> grown=V7ReportPagePlanner.plan(
            100f,500f,Collections.singletonList(
                new V7ReportPagePlanner.Section("arbitrary-extra-table",30f,rows)));
        if(grown.size()<=pages.size())
            throw new IllegalStateException("page planner uses a fixed cap");
        int largeRows=0;
        for(V7ReportPagePlanner.Page page:grown)
            for(V7ReportPagePlanner.Element item:page.elements)
                if(!item.sectionHeader)largeRows++;
        if(largeRows!=45)throw new IllegalStateException("overflow rows truncated");
        status.put("v7_dynamic_pagination",
            new JSONObject().put("R05_18_rows_pages",pages.size())
                .put("R05_18_rows_preserved",rowCount)
                .put("stress_45_rows_pages",grown.size())
                .put("stress_45_rows_preserved",largeRows));
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

    /** R03 geometry-only Android proof: no sampled curves or candidate hazards. */
    private void exerciseR03ChartGeometry()throws Exception {
        PdfDocument pdf=new PdfDocument();
        try {
            for(int index=1;index<=3;index++){
                PdfDocument.Page page=V7ReportLayoutKit.open(pdf,index,true);
                Canvas c=page.getCanvas();c.drawColor(Color.WHITE);
                V7ReportLayoutKit.drawHeader(c,this,true,
                    "تقرير الجلسة الميدانية", "R03 Field Session Geometry", "-");
                if(index==1)V7FieldChartLayout.drawFirstFour(c);
                else if(index==2)V7FieldChartLayout.drawAdvancedPage(c);
                else {
                    float x=V7FieldChartLayout.MM*5f;
                    float y=V7FieldChartLayout.MM*27f;
                    float w=V7FieldChartLayout.MM*287f;
                    V7ReportLayoutKit.tableHeader(c,
                        new RectF(x,y,x+w,y+V7FieldChartLayout.MM*16f),
                        "المرشحون، GHS، المرجع، مقارنة PPM / Candidate columns",
                        true);
                    float m=V7FieldChartLayout.MM;
                    V7FieldChartLayout.blankPpmMicro(c,new RectF(
                        x,y+16*m,x+70*m,y+48*m));
                    V7ReportLayoutKit.tableValue(c,new RectF(
                        x+70*m,y+16*m,x+w,y+48*m),"-",true);
                }
                V7ReportLayoutKit.drawFooter(c,true,index,3);
                pdf.finishPage(page);
            }
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            pdf.writeTo(out);
            store("V7_R03_Blank_Layout_Actual_Android.pdf",out);
        }finally{pdf.close();}
        int p=verify("V7_R03_Blank_Layout_Actual_Android.pdf");
        if(p!=3)throw new IllegalStateException("R03 geometry page count "+p);
        status.put("v7_r03_geometry",
            "3 PAGE ANDROID BLANK DESIGN ONLY; NO REPORT OR MEASUREMENT");
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
        exerciseV7FlexiblePagination();
        exerciseV7BlankLayoutKit();
        exerciseR03ChartGeometry();
        store("V7_R01_Instant_ChemCore_Approved_Layout_Test.pdf",bytes);
        verify("V7_R01_Instant_ChemCore_Approved_Layout_Test.pdf");
        bytes=new ByteArrayOutputStream();
        TankPhysicalSnapshotPdf.write(bytes,telemetry,"2026-10-30 20:45:00 UTC");
        store("V7_R02_Actual_Android_Report.pdf",bytes);
        int physicalPages=verify("V7_R02_Actual_Android_Report.pdf");
        if(physicalPages<2)throw new IllegalStateException(
            "V7 physical report must paginate complete 15-MQ-independent indicators");
        status.put("v7_r02",new JSONObject()
            .put("pages",physicalPages)
            .put("indicators_expected",15)
            .put("snapshot","EXPLICIT_SYNTHETIC_TEST_FIXTURE_NOT_TANK_MEASUREMENT"));

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
        ByteArrayOutputStream fruitLayout=new ByteArrayOutputStream();
        V7FruitReportPdfWriter.write(this,fruitLayout,fruitResult,fruitReadings,
            177,180000L,"CI-V7-FRUIT-SELECTED");
        store("V7_R05_Fruit_18_Families_No_Photos_Yet.pdf",fruitLayout);
        int fruitLayoutPages=verify("V7_R05_Fruit_18_Families_No_Photos_Yet.pdf");
        if(fruitLayoutPages<3)throw new IllegalStateException(
            "R05 must show all 18 options plus complete source matches");
        status.put("v7_r05_preview",new JSONObject()
            .put("pages",fruitLayoutPages)
            .put("source_families",18)
            .put("photographs_packed",false)
            .put("layout_only_not_production",true));
        // Missing data must remain '-'; no fabricated matching index.
        FoodFreshnessEngine.Result fruitBlank=FoodFreshnessEngine.analyzeFruit(
            new ArrayList<FoodFreshnessEngine.Reading>(),"Banana");
        fruitLayout.reset();
        V7FruitReportPdfWriter.write(this,fruitLayout,fruitBlank,
            Collections.<String,Double>emptyMap(),0,0L,"CI-V7-FRUIT-BLANK");
        store("V7_R05_Fruit_No_Data_18_Families.pdf",fruitLayout);
        if(verify("V7_R05_Fruit_No_Data_18_Families.pdf")!=2)
            throw new IllegalStateException("R05 blank source must retain two matrix pages");

        Map<String,Double> beef=new LinkedHashMap<>();
        double[] profile=BeefResearchCatalog.classes().get(0).logShape();
        for(int i=0;i<BeefResearchCatalog.SENSOR_ORDER.size();i++)
            beef.put(BeefResearchCatalog.SENSOR_ORDER.get(i),Math.pow(10,3.6+profile[i])*(1+.008*i));
        BeefResearchEngine.Result beefResult=BeefResearchEngine.analyze(beef);
        if(beefResult.rankedClasses.size()<4)throw new IllegalStateException("Missing beef classes");
        Uri meatUri=BeefResearchPdf.save(this,beefResult,beef,179,180000L,
            "CI-V6-MEAT",26.4,52.5);
        copy("V7_R06_Actual_Android_Report.pdf",meatUri);
        int beefPages=verify("V7_R06_Actual_Android_Report.pdf");
        if(beefPages<3)throw new IllegalStateException(
            "V7 beef must include study research evidence section");
        status.put("v7_r06",new JSONObject().put("pages",beefPages)
            .put("research_model","SOURCE_ONLY_BEEF_TS5_CONFUSION")
            .put("sample","SYNTHETIC_TEST_FIXTURE_NOT_TANK_MEAT_VALIDATION"));
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