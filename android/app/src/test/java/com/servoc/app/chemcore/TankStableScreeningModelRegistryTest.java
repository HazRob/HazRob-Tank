package com.servoc.app.chemcore;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Mathematical checks on SHA-verified Stable prior adapters (no Android handset required). */
public final class TankStableScreeningModelRegistryTest {
    private JSONObject prior(String tier)throws Exception {
        return new JSONObject()
            .put("sensor_key","mq135").put("form","LOG10_LINEAR")
            .put("intercept",Math.log10(5.2629)).put("slope",-0.2668)
            .put("sigma_log10",0.20).put("min_ppm",10.0).put("max_ppm",500.0)
            .put("evidence_tier",tier).put("device_validated",false)
            .put("model_version","STABLE_NUMERIC_TEST").put("source_id","doi:mock-reference")
            .put("note","synthetic test input (not a real robot calibration)");
    }
    @Test public void literatureCurveProducesFiniteBoundedLogRatioWithoutM4()throws Exception {
        JSONObject p=prior("M2");
        TankStableScreeningModelRegistry.StableModel m=
            new TankStableScreeningModelRegistry.StableModel(p);
        assertTrue(m.valid(p));
        ChemCoreResponseModel.Estimate predicted=m.predict(100.0,25,50,null);
        assertTrue(predicted.available);
        double expected=Math.log10(5.2629)-0.2668*Math.log10(100);
        assertEquals(expected,predicted.meanLog10Ratio,1e-10);
        assertEquals(ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,m.evidenceTier());
        assertFalse(m.reportableQuantitative());
        assertEquals("doi:mock-reference",m.sourceId());
        assertFalse(m.predict(1000.0,25,50,null).available);
    }
    @Test public void researchTierCanEstimateButNeverBePromotedToDeviceM4()throws Exception {
        JSONObject p=prior("M1");
        TankStableScreeningModelRegistry.StableModel m=
            new TankStableScreeningModelRegistry.StableModel(p);
        assertTrue(m.valid(p));
        assertEquals(ChemCoreEvidenceObservation.Type.RESEARCH_CURVE,m.evidenceType());
        assertTrue(m.predict(40.0,25,50,null).available);
        assertFalse(m.reportableQuantitative());
    }
    @Test public void forgedDeviceValidationOrUnknownSourceIsRejected()throws Exception {
        JSONObject device=prior("M2").put("device_validated",true);
        assertFalse(new TankStableScreeningModelRegistry.StableModel(device).valid(device));
        JSONObject missing=prior("M2").put("source_id","");
        assertFalse(new TankStableScreeningModelRegistry.StableModel(missing).valid(missing));
        JSONObject fakeM4=prior("M4");
        assertFalse(new TankStableScreeningModelRegistry.StableModel(fakeM4).valid(fakeM4));
    }
}
