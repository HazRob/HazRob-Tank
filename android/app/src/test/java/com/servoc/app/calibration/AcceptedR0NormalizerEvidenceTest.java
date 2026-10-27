package com.servoc.app.calibration;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Tank-specific evidence rules: no synthetic current R0; archived data remains frozen. */
public final class AcceptedR0NormalizerEvidenceTest {
    private static JSONObject sample(boolean valid,String status,double ratio)throws Exception{
        JSONObject q=new JSONObject().put("valid",valid).put("ok",valid)
            .put("status",status).put("rs",ratio*10.0).put("r0",10.0)
            .put("ratio",ratio).put("r0_source","manual_device_r0")
            .put("r0_provenance_complete",true);
        q.put("quality",new JSONObject().put("status",status));
        return q;
    }

    @Test public void staleArchivedMeasurementRetainedOnlyForQualifiedCorroboration()throws Exception{
        ManualR0Calibration.load(null);
        JSONObject q=sample(false,"STALE",2.5);
        assertTrue(Double.isNaN(AcceptedR0Normalizer.ratio("mq2",q)));
        assertTrue(Double.isNaN(AcceptedR0Normalizer.availableRatio("mq2",q)));
        assertEquals(2.5,AcceptedR0Normalizer.availableRecordedRatio("mq2",q),1e-9);
        assertEquals(0.55,AcceptedR0Normalizer.evidenceWeight(q),1e-9);
        assertEquals("DEGRADED",AcceptedR0Normalizer.evidenceQualityClass(q));
    }

    @Test public void saturatedAndInvalidArchivedEvidenceRemainWeak()throws Exception{
        ManualR0Calibration.load(null);
        JSONObject saturated=sample(false,"RAIL_HIGH_OR_SATURATED",3.0);
        assertEquals(3.0,AcceptedR0Normalizer.availableRecordedRatio("mq3",saturated),1e-9);
        assertEquals(0.25,AcceptedR0Normalizer.evidenceWeight(saturated),1e-9);
        assertEquals("WEAK",AcceptedR0Normalizer.evidenceQualityClass(saturated));
        JSONObject invalid=sample(false,"INVALID",1.8);
        assertEquals(0.35,AcceptedR0Normalizer.evidenceWeight(invalid),1e-9);
        assertEquals(1.8,AcceptedR0Normalizer.availableRecordedRatio("mq135",invalid),1e-9);
    }

    @Test public void physicalPowerAdcOrSourceFailureCannotManufactureChemicalEvidence()throws Exception{
        for(String state:new String[]{"POWER_OFF","ADC_UNAVAILABLE","MISSING_SOURCE"}){
            JSONObject q=sample(true,state,2.0);
            assertTrue(Double.isNaN(AcceptedR0Normalizer.ratio("mq2",q)));
            assertTrue(Double.isNaN(AcceptedR0Normalizer.recordedRatio("mq2",q)));
            assertTrue(Double.isNaN(AcceptedR0Normalizer.availableRecordedRatio("mq2",q)));
            assertEquals(0.0,AcceptedR0Normalizer.evidenceWeight(q),0.0);
            assertEquals("DIAGNOSTIC_ONLY",AcceptedR0Normalizer.evidenceQualityClass(q));
            assertFalse(AcceptedR0Normalizer.usesAcceptedDeviceR0("mq2",q));
            assertFalse(AcceptedR0Normalizer.usesRecordedDeviceR0("mq2",q));
        }
    }

    @Test public void noiseReducesWeightWithoutInventingActiveR0()throws Exception{
        ManualR0Calibration.load(null);
        JSONObject q=sample(true,"VALID",2.0);
        q.getJSONObject("quality").put("noise_evaluable",true).put("noise_flag",true)
            .put("temporal_state","UNSTABLE");
        assertTrue(Double.isNaN(AcceptedR0Normalizer.ratio("mq5",q)));
        assertEquals(2.0,AcceptedR0Normalizer.recordedRatio("mq5",q),1e-9);
        assertEquals(0.39,AcceptedR0Normalizer.evidenceWeight(q),1e-9);
        assertEquals("WEAK",AcceptedR0Normalizer.evidenceQualityClass(q));
    }

    @Test public void contradictoryNestedAdcStatusAlwaysFailsClosed()throws Exception{
        JSONObject nested=sample(true,"VALID",2.0);
        nested.remove("status");nested.getJSONObject("quality").put("status","ADC_UNAVAILABLE");
        assertEquals(0.0,AcceptedR0Normalizer.evidenceWeight(nested),0.0);
        assertTrue(Double.isNaN(AcceptedR0Normalizer.recordedRatio("mq3",nested)));
        JSONObject inconsistent=sample(false,"VALID",2.0);
        assertEquals(0.35,AcceptedR0Normalizer.evidenceWeight(inconsistent),1e-9);
        assertTrue(Double.isNaN(AcceptedR0Normalizer.recordedRatio("mq3",inconsistent)));
    }
}
