package com.servoc.app.report;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.hazrob.tank.foodfreshness.FoodReferenceCatalog;
import com.hazrob.tank.foodfreshness.FoodFreshnessEngine;
import com.hazrob.tank.foodfreshness.V7FruitReportPdfWriter;
import com.hazrob.tank.foodfreshness.meat.BeefResearchCatalog;
import com.hazrob.tank.foodfreshness.meat.BeefResearchEngine;
import com.hazrob.tank.foodfreshness.meat.V7BeefResearchPdfWriter;
import com.hazrob.tank.emissions.EmissionResearchEngine;
import com.hazrob.tank.emissions.EmissionNextBridge;
import com.hazrob.tank.emissions.EmissionReportPdf;
import com.servoc.app.report.TankInstantReportPdf;
import com.servoc.app.report.V7PhysicalReportPdfWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import static org.junit.Assert.*;

/** R05/R06/R07 PDF data-binding pixel differential, API31 PdfRenderer.
 * Input vectors are TEST FIXTURES only: not physical Tank measurements.
 * Preserves V7 geometry. Identical ID/time/source, only telemetry differs.
 */
@RunWith(AndroidJUnit4.class)
public final class ReportChartDataParityInstrumentedTest {
    private final Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();

    private Bitmap page(byte[] bytes,int zeroIndex,int pagesExpected)throws Exception{
        File f=File.createTempFile("v8-pdf-chart-", ".pdf",ctx.getCacheDir());
        try{
            try(FileOutputStream out=new FileOutputStream(f)){out.write(bytes);}
            try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY);
                PdfRenderer pdf=new PdfRenderer(fd)){
                if(pagesExpected>0)assertEquals(pagesExpected,pdf.getPageCount());
                try(PdfRenderer.Page p=pdf.openPage(zeroIndex)){
                    Bitmap image=Bitmap.createBitmap(595,842,Bitmap.Config.ARGB_8888);
                    p.render(image,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    return image;
                }
            }
        }finally{f.delete();}
    }
    private int changed(Bitmap a,Bitmap b,int left,int top,int right,int bottom){
        assertEquals(a.getWidth(),b.getWidth());
        assertEquals(a.getHeight(),b.getHeight());
        int count=0;
        for(int y=top;y<bottom;y+=2)
            for(int x=left;x<right;x+=2)
                if(a.getPixel(x,y)!=b.getPixel(x,y))count++;
        return count;
    }
    private Map<String,List<double[]>> temporal(Map<String,Double> resistance,boolean up){
        Map<String,List<double[]>> all=new LinkedHashMap<>();
        int sensor=0;
        for(Map.Entry<String,Double> row:resistance.entrySet()){
            List<double[]> values=new ArrayList<>();
            for(int k=0;k<=30;k++){
                double time=6.0*k;
                // Source-independent TEST telemetry: opposite direction and
                // sensor-specific kinetics. Never enters application storage.
                double slope=(0.012+sensor*0.004)*(up?1:-1);
                double factor=Math.exp(slope*k)*(1+(sensor%3)*0.0007*k*k);
                values.add(new double[]{time,row.getValue()*factor});
            }
            all.put(row.getKey(),values);
            sensor++;
        }
        return all;
    }

    @Test public void fruit180SecondGraphChangesOnlyWhenOriginalTraceChanges()
        throws Exception {
        double[] profile=null;
        for(FoodReferenceCatalog.Pattern p:FoodReferenceCatalog.patterns())
            if("Banana D3".equals(p.label))profile=p.vector();
        assertNotNull(profile);
        Map<String,Double> readings=new LinkedHashMap<>();
        List<FoodFreshnessEngine.Reading> source=new ArrayList<>();
        for(int i=0;i<8;i++){
            String sensor=FoodReferenceCatalog.SENSOR_ORDER.get(i);
            double resistance=profile[i]*1500;
            readings.put(sensor,resistance);
            source.add(new FoodFreshnessEngine.Reading(sensor,resistance));
        }
        FoodFreshnessEngine.Result result=FoodFreshnessEngine.analyzeFruit(source,"banana");
        ByteArrayOutputStream up=new ByteArrayOutputStream(),down=new ByteArrayOutputStream();
        V7FruitReportPdfWriter.write(ctx,up,result,readings,60,180000,
            "FOOD-DATA-TRACE-TEST","2026-11-03T00:00:00.000Z",temporal(readings,true));
        V7FruitReportPdfWriter.write(ctx,down,result,readings,60,180000,
            "FOOD-DATA-TRACE-TEST","2026-11-03T00:00:00.000Z",temporal(readings,false));
        Bitmap a=page(up.toByteArray(),2,4);
        Bitmap b=page(down.toByteArray(),2,4);
        try{
            // 191-241 mm chart is preserved; actual data occupy 202-231 mm.
            assertTrue("R05 chart must respond to its own 180s MQ samples",
                changed(a,b,38,575,547,655)>30);
        }finally{a.recycle();b.recycle();}
    }

    @Test public void beef180SecondGraphChangesOnlyWhenOriginalTraceChanges()
        throws Exception {
        Map<String,Double> readings=new LinkedHashMap<>();
        double[] shape=BeefResearchCatalog.classes().get(0).logShape();
        for(int i=0;i<7;i++)
            readings.put(BeefResearchCatalog.SENSOR_ORDER.get(i),
                Math.pow(10.0,shape[i])*1600);
        BeefResearchEngine.Result result=BeefResearchEngine.analyze(readings);
        ByteArrayOutputStream up=new ByteArrayOutputStream(),down=new ByteArrayOutputStream();
        V7BeefResearchPdfWriter.write(ctx,up,result,readings,61,180000,
            "BEEF-DATA-TRACE-TEST",25.0,50.0,"2026-11-03T00:00:00.000Z",
            temporal(readings,true));
        V7BeefResearchPdfWriter.write(ctx,down,result,readings,61,180000,
            "BEEF-DATA-TRACE-TEST",25.0,50.0,"2026-11-03T00:00:00.000Z",
            temporal(readings,false));
        Bitmap a=page(up.toByteArray(),1,3);
        Bitmap b=page(down.toByteArray(),1,3);
        try{
            assertTrue("R06 temporal plot must depend on captured MQ evidence",
                changed(a,b,55,385,535,490)>30);
        }finally{a.recycle();b.recycle();}
    }


    private JSONObject telemetry(double scaling)throws Exception{
        JSONObject mq=new JSONObject();
        String[] names={"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"};
        for(int i=0;i<names.length;i++){
            double measured=4.5+(0.65*i)+scaling*(i%3==0?1.0:0.05);
            mq.put(names[i],new JSONObject().put("ok",true)
                .put("powered",true).put("rs",measured)
                .put("r0",12.0+i*0.4).put("r0_source","DEVICE"));
        }
        return new JSONObject().put("mq",mq);
    }
    /**
     * P11.6: radar/table parity is tested from the SAME frozen Next source
     * evidence, never firmware mq.r0 (which could be old factor-scaled R0).
     * Synthetic unique snapshot hashes distinguish independently captured
     * fixture vectors and do not qualify gases or Tank G3.
     */
    private JSONObject nextEnvelope(double scaling)throws Exception{
        String digest=scaling==0.0?
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa":
            "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
        String uid=scaling==0.0?"R01-QA-SOURCE-A":"R01-QA-SOURCE-B";
        JSONArray frozen=new JSONArray();
        String[] models={"MQ2","MQ3","MQ4","MQ5","MQ7","MQ8","MQ9","MQ135"};
        for(int i=0;i<models.length;i++){
            double rs=4.5+0.65*i+scaling*(i%3==0?1.0:0.05);
            double rAir=12.0+i*0.4;
            frozen.put(new JSONObject()
                .put("source_curve_id","R01-QA-FROZEN-MQ-"+models[i])
                .put("sensor_model",models[i])
                .put("reference_device_bound",true)
                .put("rs_kohm",rs)
                .put("device_air_reference_kohm",rAir)
                .put("status","RESEARCH_ONLY_SOURCE_HEATER_UNVERIFIED"));
        }
        JSONObject science=new JSONObject()
            .put("source_snapshot_uid",uid)
            .put("source_snapshot_sha256",digest)
            .put("all_reference_models",frozen);
        return new JSONObject()
            .put("schema","hazrob.tank.v8.next.decision.presentation/1")
            .put("decision_uid","R01-QA-DECISION-"+uid)
            .put("snapshot_sha256",digest)
            .put("snapshot_uid",uid)
            .put("candidate_count",0)
            .put("candidates",new JSONArray())
            .put("observed_channels",8)
            .put("decision_status","UNKNOWN")
            .put("scientific_source_diagnostics",science);
    }
    @Test public void instantRadarUsesSameFrozenRsR0SourceAsReportTable()throws Exception{
        JSONObject sourceTelemetry=telemetry(0);
        ByteArrayOutputStream outA=new ByteArrayOutputStream(),outB=new ByteArrayOutputStream();
        TankInstantReportPdf.write(outA,sourceTelemetry,nextEnvelope(0),
            "R01-TRACE-TEST",1793664000000L);
        TankInstantReportPdf.write(outB,sourceTelemetry,nextEnvelope(3.2),
            "R01-TRACE-TEST",1793664000000L);
        // Original V7 main page plus 8 frozen source-curve details (1 page).
        Bitmap a=page(outA.toByteArray(),0,2),b=page(outB.toByteArray(),0,2);
        try{
            assertTrue("R01 radar must vary with frozen Next Rs/R_air, not firmware r0",
                changed(a,b,35,575,252,743)>15);
            assertTrue("R01 numeric table must vary with the SAME frozen Next Rs/R_air",
                changed(a,b,265,232,565,510)>15);
        }finally{a.recycle();b.recycle();}
    }
    @Test public void changingRawFirmwareR0CannotRewriteFrozenNextRadar()throws Exception{
        JSONObject original=telemetry(0);
        JSONObject corrupted=telemetry(3.2);
        ByteArrayOutputStream first=new ByteArrayOutputStream(),second=new ByteArrayOutputStream();
        TankInstantReportPdf.write(first,original,nextEnvelope(0),
            "R01-NO-REPLAY",1793664000000L);
        TankInstantReportPdf.write(second,corrupted,nextEnvelope(0),
            "R01-NO-REPLAY",1793664000000L);
        Bitmap a=page(first.toByteArray(),0,2),b=page(second.toByteArray(),0,2);
        try{
            assertEquals("Changing firmware MQ resistance must not mutate frozen Next radar",
                0,changed(a,b,35,575,252,743));
            assertEquals("Frozen Next sensor table must not use later raw telemetry",
                0,changed(a,b,265,232,565,510));
        }finally{a.recycle();b.recycle();}
    }
    @Test public void physicalPdfDisplaysSourceSnapshotInsteadOfOtherSample()throws Exception{
        JSONObject physicalA=new JSONObject().put("env",
            new JSONObject().put("temperature",22.5).put("humidity",58.0)
                .put("pressure",1008.2));
        JSONObject physicalB=new JSONObject().put("env",
            new JSONObject().put("temperature",45.8).put("humidity",58.0)
                .put("pressure",1008.2));
        ByteArrayOutputStream outA=new ByteArrayOutputStream(),outB=new ByteArrayOutputStream();
        V7PhysicalReportPdfWriter.write(outA,physicalA,"2026-11-03T00:00:00Z");
        V7PhysicalReportPdfWriter.write(outB,physicalB,"2026-11-03T00:00:00Z");
        Bitmap a=page(outA.toByteArray(),0,-1),b=page(outB.toByteArray(),0,-1);
        try{
            assertTrue("R02 displayed temperature must depend on this snapshot",
                changed(a,b,125,70,400,770)>10);
        }finally{a.recycle();b.recycle();}
    }

    @Test public void explicitlyInvalidPhysicalSensorCannotAppearInReport()
        throws Exception {
        JSONObject invalid=new JSONObject().put("env",
            new JSONObject().put("ok",false).put("temperature",99.9));
        JSONObject missing=new JSONObject();
        ByteArrayOutputStream aOut=new ByteArrayOutputStream(),bOut=new ByteArrayOutputStream();
        V7PhysicalReportPdfWriter.write(aOut,invalid,"2026-11-03T00:00:00Z");
        V7PhysicalReportPdfWriter.write(bOut,missing,"2026-11-03T00:00:00Z");
        Bitmap a=page(aOut.toByteArray(),0,-1),b=page(bOut.toByteArray(),0,-1);
        try{
            assertEquals("R02 invalid sensor must render identical to missing evidence",
                0,changed(a,b,45,70,550,770));
        }finally{a.recycle();b.recycle();}
    }

    private JSONObject emissions(double baseline,int modelCount)throws Exception{
        Map<String,Double> rs=new LinkedHashMap<>();
        Map<String,Integer> n=new LinkedHashMap<>();
        rs.put("MQ4",3.9);n.put("MQ4",43);
        JSONObject result=EmissionResearchEngine.complete(991L,"Engine-A-TEST",
            1793664000000L,60000,false,43,rs,n);
        JSONArray models=new JSONArray();
        for(int i=0;i<modelCount;i++)
            models.put(new JSONObject().put("gas_or_class","CH4")
                .put("sensor_model","MQ4")
                .put("source_curve_id","STABLE-TEST-CURVE-"+i)
                .put("source_root","TEST_FIXTURE_NOT_PRODUCTION")
                .put("estimated_ppm",baseline+(i+1)*1.25)
                .put("status","SOURCE_ESTIMATE"));
        JSONObject decision=new JSONObject()
            .put("schema","hazrob.tank.v8.next.decision.presentation/1")
            .put("decision_uid","SOURCE-TEST-991")
            .put("decision_status","RESEARCH_ONLY")
            .put("source_curve_hypotheses",models);
        EmissionNextBridge.attachAnalysis(result,decision);
        result.put("measured_at_ms",1793664000000L);
        return result;
    }
    @Test public void allEmissionCurveEstimatesPaginateAndRenderFromSelectedEngine()
        throws Exception {
        JSONObject aResult=emissions(100,19),bResult=emissions(900,19);
        assertEquals(19,aResult.optInt("next_finite_ppm_hypotheses"));
        ByteArrayOutputStream aPdf=new ByteArrayOutputStream(),bPdf=new ByteArrayOutputStream();
        EmissionReportPdf.write(ctx,aPdf,"Engine-A-TEST",aResult,1);
        EmissionReportPdf.write(ctx,bPdf,"Engine-A-TEST",bResult,1);
        Bitmap first=page(aPdf.toByteArray(),0,5);
        Bitmap changed=page(bPdf.toByteArray(),0,5);
        Bitmap last=page(aPdf.toByteArray(),4,5);
        try {
            assertTrue("R07 matrix numeric source results must change with source PPM",
                changed(first,changed,160,320,400,540)>20);
            assertEquals(595,last.getWidth());
        }finally{first.recycle();changed.recycle();last.recycle();}
    }
}
