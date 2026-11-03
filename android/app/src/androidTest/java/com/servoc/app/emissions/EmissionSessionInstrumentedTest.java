package com.servoc.app.emissions;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.hazrob.tank.emissions.EmissionSessionStore;
import com.hazrob.tank.emissions.EmissionResearchEngine;
import com.hazrob.tank.emissions.EmissionReportPdf;
import org.json.JSONObject;
import org.json.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public final class EmissionSessionInstrumentedTest {
    @Test public void independentEngineRegistryAndReportLifecycle()throws Exception{
        Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
        EmissionSessionStore db=new EmissionSessionStore(ctx);
        String salt=Long.toHexString(System.nanoTime());
        long engineOne=db.create("محرك ديزل - CI "+salt);
        long engineTwo=db.create("مولد منفصل - CI "+salt);
        try{
            Map<String,Double> signals=new LinkedHashMap<>();
            signals.put("MQ4",5.17);signals.put("MQ2",7.13);
            Map<String,Integer> counts=new LinkedHashMap<>();
            counts.put("MQ4",32);counts.put("MQ2",30);
            JSONObject first=EmissionResearchEngine.complete(engineOne,
                "محرك ديزل - CI "+salt,System.currentTimeMillis()-60000,
                60000,true,35,signals,counts);
            assertEquals(8,first.getJSONArray("pollutants").length());
            // Fixture represents a frozen Next research envelope, not a
            // synthetic gas calibration. CH4 / smoke / CO2 only.
            JSONObject next=new JSONObject()
                .put("schema","hazrob.tank.v8.next.decision.presentation/1")
                .put("decision_uid","NEXT-FIXTURE-DO-NOT-REPORT")
                .put("model_version","ChemCore Next")
                .put("decision_status","UNKNOWN")
                .put("stable_manifest_sha256","test-digest");
            org.json.JSONArray fit=new org.json.JSONArray();
            fit.put(new JSONObject().put("source_curve_id","MQDATA43-014")
                .put("source_root","MQDATASCIENCE_6_0_0")
                .put("sensor_model","MQ4").put("gas_or_class","CH4")
                .put("estimated_ppm",214.2).put("status","RESEARCH_WITHIN_SOURCE_DOMAIN"));
            fit.put(new JSONObject().put("source_curve_id","MQDATA43-006")
                .put("source_root","MQDATASCIENCE_6_0_0")
                .put("sensor_model","MQ2").put("gas_or_class","smoke")
                .put("estimated_ppm",82.0).put("status","RESEARCH_EXTRAPOLATED"));
            fit.put(new JSONObject().put("source_curve_id","MQDATA43-041")
                .put("sensor_model","MQ135").put("gas_or_class","CO2")
                .put("estimated_ppm",430.0));
            next.put("source_curve_hypotheses",fit);
            com.hazrob.tank.emissions.EmissionNextBridge.attachAnalysis(first,next);
            assertEquals("SUCCESS_RESEARCH_ONLY",first.getString("next_status"));
            assertEquals(3,first.getInt("next_source_hypotheses"));
            assertTrue(first.getJSONArray("pollutants").getJSONObject(0)
                .getJSONArray("next_source_hypotheses").length()==0);
            assertTrue(first.getJSONArray("pollutants").getJSONObject(1)
                .getJSONArray("next_source_hypotheses").length()==0);
            assertTrue(first.getJSONArray("pollutants").getJSONObject(2)
                .getJSONArray("next_source_hypotheses").length()==1);

            assertEquals(EmissionResearchEngine.INFERENCE,first.optString("mode"));
            for(int i=0;i<first.getJSONArray("pollutants").length();i++){
                JSONObject p=first.getJSONArray("pollutants").getJSONObject(i);
                assertTrue(p.isNull("value_ppm"));
                assertFalse(p.optBoolean("exceeded",true));
            }
            db.addMeasurement(engineOne,first);
            assertEquals(1,db.count(engineOne));
            assertEquals(0,db.count(engineTwo));
            assertEquals(engineOne,db.latest(engineOne).optLong("engine_id"));
            assertNull(db.latest(engineTwo));
            JSONObject second=EmissionResearchEngine.complete(engineTwo,
                "مولد منفصل - CI "+salt,System.currentTimeMillis()-60000,
                60000,false,31,signals,counts);
            assertEquals(5,second.getJSONArray("pollutants").length());
            db.addMeasurement(engineTwo,second);
            assertEquals(1,db.count(engineTwo));
            ByteArrayOutputStream buffer=new ByteArrayOutputStream();
            EmissionReportPdf.write(ctx,buffer,"مولد منفصل - CI "+salt,
                db.latest(engineTwo),db.count(engineTwo));
            assertTrue(buffer.size()>4000);
            File f=File.createTempFile("hazrob-emission-ci-", ".pdf",ctx.getCacheDir());
            try{
                try(FileOutputStream out=new FileOutputStream(f)){out.write(buffer.toByteArray());}
                try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY);
                    PdfRenderer renderer=new PdfRenderer(fd)){
                    assertEquals(3,renderer.getPageCount());
                    for(int i=0;i<renderer.getPageCount();i++){
                        try(PdfRenderer.Page page=renderer.openPage(i)){
                            Bitmap bmp=Bitmap.createBitmap(350,500,Bitmap.Config.ARGB_8888);
                            try{page.render(bmp,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);}
                            finally{bmp.recycle();}
                        }
                    }
                }
            }finally{f.delete();}
            db.delete(engineOne);
            assertNull(db.latest(engineOne));
            assertEquals(0,db.count(engineOne));
            assertEquals(1,db.count(engineTwo)); // independent-engine isolation
        }finally{
            db.delete(engineOne);db.delete(engineTwo);db.close();
        }
    }
}
