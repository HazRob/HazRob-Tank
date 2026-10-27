package com.servoc.app.calibration;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public final class ManualR0CalibrationTest {
    @Test public void newTankNeverInventsFieldFallbackOrQuantitativeProvenance()throws Exception{
        ManualR0Calibration.load(null);
        JSONObject q=new JSONObject().put("rs",FieldR0Baseline.value("mq2")*2.0);
        ManualR0Calibration.apply("mq2",q);
        assertEquals("unavailable",q.getString("r0_source"));
        assertFalse(q.has("r0"));assertFalse(q.has("ratio"));
        assertFalse(q.getBoolean("r0_calibrated"));
        assertFalse(q.getBoolean("r0_provenance_complete"));
        assertFalse(q.getBoolean("r0_reference_validated"));
        assertTrue(Double.isNaN(ManualR0Calibration.get("mq2")));
    }

    @Test public void unknownChannelFailsClosed()throws Exception{
        ManualR0Calibration.load(null);JSONObject q=new JSONObject();q.put("rs",10.0);ManualR0Calibration.apply("unknown",q);
        assertFalse(q.getBoolean("r0_calibrated"));assertFalse(q.getBoolean("r0_provenance_complete"));assertFalse(q.getBoolean("r0_reference_validated"));
    }
}
