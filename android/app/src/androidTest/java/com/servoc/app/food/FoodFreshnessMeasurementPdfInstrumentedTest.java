package com.servoc.app.food;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.hazrob.tank.foodfreshness.FoodFreshnessEngine;
import com.hazrob.tank.foodfreshness.FoodMeasurementDecision;
import com.hazrob.tank.foodfreshness.FoodReferenceCatalog;
import com.hazrob.tank.foodfreshness.V7FruitReportPdfWriter;
import com.hazrob.tank.foodfreshness.meat.BeefGaussianResearchEngine;
import com.hazrob.tank.foodfreshness.meat.BeefResearchCatalog;
import com.hazrob.tank.foodfreshness.meat.BeefResearchEngine;
import com.hazrob.tank.foodfreshness.meat.V7BeefResearchPdfWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

/** Android12 PDF smoke with genuine source vectors and synthetic acquisition trace.
 * Does not assert edible/safe, calibration, or physical Tank validation.
 */
@RunWith(AndroidJUnit4.class)
public class FoodFreshnessMeasurementPdfInstrumentedTest {
    static Map<String,List<double[]>> traces(Map<String,Double> readings){
        Map<String,List<double[]>> out=new LinkedHashMap<>();
        for(Map.Entry<String,Double> row:readings.entrySet()){
            List<double[]> points=new ArrayList<>();
            for(int i=0;i<35;i++)points.add(new double[]{i*5.0,
                    row.getValue()*(1+0.003*i)});
            out.put(row.getKey(),points);
        }
        return out;
    }
    static int render(byte[] source)throws Exception{
        assertTrue(source.length>5000);
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        File f=File.createTempFile("food-ci-", ".pdf", context.getCacheDir());
        try{
            try(FileOutputStream output=new FileOutputStream(f)){output.write(source);}
            try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY);
                PdfRenderer pdf=new PdfRenderer(fd)){
                for(int i=0;i<pdf.getPageCount();i++){
                    try(PdfRenderer.Page page=pdf.openPage(i)){
                        Bitmap bmp=Bitmap.createBitmap(350,500,Bitmap.Config.ARGB_8888);
                        try{page.render(bmp,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);}
                        finally{bmp.recycle();}
                    }
                }
                return pdf.getPageCount();
            }
        }finally{f.delete();}
    }
    @Test public void bananaDirectMeasurementAndBrandedPdfRender()throws Exception{
        double[] v=null;
        for(FoodReferenceCatalog.Pattern p:FoodReferenceCatalog.patterns())
            if(p.label.equals("Banana D3"))v=p.vector();
        assertNotNull(v);
        List<FoodFreshnessEngine.Reading> input=new ArrayList<>();
        Map<String,Double> rs=new LinkedHashMap<>();
        for(int i=0;i<8;i++){
            double raw=v[i]*3800;
            String sensor=FoodReferenceCatalog.SENSOR_ORDER.get(i);
            input.add(new FoodFreshnessEngine.Reading(sensor,raw));
            rs.put(sensor,raw);
        }
        FoodMeasurementDecision.Decision decision=FoodMeasurementDecision.fruit(rs,"banana");
        assertEquals(3.0,decision.fittedStudyPosition,1e-5);
        assertFalse(decision.safeToEat);
        FoodFreshnessEngine.Result source=FoodFreshnessEngine.analyzeFruit(input,"banana");
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        V7FruitReportPdfWriter.write(InstrumentationRegistry.getInstrumentation().getTargetContext(),
            out,source,rs,180,180000,"FRUIT-TEST",traces(rs));
        assertEquals(4,render(out.toByteArray()));
    }
    @Test public void beefSourceMeasurementAndBrandedPdfRender()throws Exception{
        Map<String,Double> rs=new LinkedHashMap<>();
        double[] shape=BeefResearchCatalog.classes().get(0).logShape();
        for(int i=0;i<7;i++)rs.put(BeefResearchCatalog.SENSOR_ORDER.get(i),
            Math.pow(10.0,shape[i])*2300.0);
        assertEquals(4,BeefGaussianResearchEngine.analyze(rs).ranked.size());
        BeefResearchEngine.Result result=BeefResearchEngine.analyze(rs);
        assertFalse(result.foodSafetyCertified);
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        V7BeefResearchPdfWriter.write(InstrumentationRegistry.getInstrumentation().getTargetContext(),
            out,result,rs,180,180000,"MEAT-TEST",25.0,51.0,traces(rs));
        assertEquals(3,render(out.toByteArray()));
    }
}
