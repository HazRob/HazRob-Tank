package com.servoc.app.calibration;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Tank V4 policy: no implicit field R0 or telemetry-only strict identity. */
public class AcceptedR0NormalizerTest {
    @Test public void noAcceptedR0MeansNoStrictRatio() throws Exception {
        ManualR0Calibration.load(null);
        JSONObject q=new JSONObject().put("ok",true).put("valid",true).put("status","VALID")
            .put("rs",100.0).put("ratio",0.25).put("r0",10.0);
        assertTrue(Double.isNaN(AcceptedR0Normalizer.ratio("mq2",q)));
        assertFalse(AcceptedR0Normalizer.usesAcceptedFieldR0("mq2",q));
    }
    @Test public void telemetryRatioNeverBecomesStrictRatio() throws Exception {
        ManualR0Calibration.load(null);
        JSONObject q=new JSONObject().put("valid",true).put("status","VALID").put("ratio",0.75);
        assertTrue(Double.isNaN(AcceptedR0Normalizer.ratio("mq2",q)));
    }
    @Test public void invalidChannelFailsClosed() throws Exception {
        ManualR0Calibration.load(null);
        JSONObject q=new JSONObject().put("ok",false).put("status","INVALID").put("rs",100.0);
        assertTrue(Double.isNaN(AcceptedR0Normalizer.ratio("mq2",q)));
        assertEquals(0.35,AcceptedR0Normalizer.evidenceWeight(q),1e-9);
    }
}
