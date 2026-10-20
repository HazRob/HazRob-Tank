package com.servoc.app.chemcore;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
public final class TankPpmEstimatePresentationTest {
    @Test public void finiteUncertifiedPpmIsShownNotHidden()throws Exception{
        JSONObject p=new JSONObject().put("session_id","T3")
          .put("ppm",38.75).put("ppm_reportable",false)
          .put("chemical","Example gas").put("ppm_withheld_reason","reference_gate_not_passed");
        String result=ChemCoreAnalysisPresenter.summary(p);
        assertTrue(result.contains("PPM: 38.75 ppm"));
        assertTrue(result.contains("COMPUTED ESTIMATE"));
        assertTrue(result.contains("WITHHELD from regulatory conclusions"));
    }
    @Test public void missingPpmDoesNotFabricateAnEstimate()throws Exception{
        JSONObject p=new JSONObject().put("ppm_reportable",false)
          .put("ppm_withheld_reason","not_enough_inputs");
        String result=ChemCoreAnalysisPresenter.summary(p);
        assertTrue(result.contains("PPM: WITHHELD"));
        assertTrue(result.contains("not_enough_inputs"));
    }
}