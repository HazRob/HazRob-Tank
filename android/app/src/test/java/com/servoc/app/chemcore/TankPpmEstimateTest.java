package com.servoc.app.chemcore;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
public final class TankPpmEstimateTest {
    @Test public void fallbackShowsRealNumericFirstCandidateFitOnly()throws Exception{
        JSONObject p=new JSONObject().put("ppm_reportable",false)
            .put("candidates",new JSONArray().put(new JSONObject()
                .put("label","A").put("screening_fit_ppm",27.5)));
        TankPpmEstimate.Result v=TankPpmEstimate.from(p);
        assertTrue(v.available());assertEquals(27.5,v.ppm,1e-12);
        assertFalse(v.reportable);assertTrue(v.screening);
        assertEquals("screening_fit_ppm",v.method);
    }
    @Test public void directAcceptedPpmIsNeverReplacedByCandidateFit()throws Exception{
        JSONObject p=new JSONObject().put("ppm",15.0).put("ppm_reportable",true)
          .put("candidates",new JSONArray().put(new JSONObject().put("screening_fit_ppm",9000)));
        TankPpmEstimate.Result v=TankPpmEstimate.from(p);
        assertEquals(15.0,v.ppm,1e-12);assertTrue(v.reportable);assertFalse(v.screening);
    }
    @Test public void unacceptedDirectPpmStillVisible()throws Exception{
        JSONObject p=new JSONObject().put("ppm",30.5).put("ppm_reportable",false);
        TankPpmEstimate.Result v=TankPpmEstimate.from(p);
        assertEquals(30.5,v.ppm,1e-12);assertFalse(v.reportable);assertFalse(v.screening);
    }
    @Test public void missingInvalidOrNegativeDataNeverCreatesValue()throws Exception{
        assertFalse(TankPpmEstimate.from(new JSONObject()).available());
        JSONObject p=new JSONObject().put("candidates",new JSONArray()
            .put(new JSONObject().put("screening_fit_ppm",-2.0)));
        assertFalse(TankPpmEstimate.from(p).available());
    }
}
