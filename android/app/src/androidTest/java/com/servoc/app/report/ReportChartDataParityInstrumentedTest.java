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
                assertEquals(pagesExpected,pdf.getPageCount());
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
