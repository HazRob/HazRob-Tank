package com.servoc.app.measurement;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;

/**
 * V8 F02: an OFF acknowledgement is positive device telemetry, never the
 * absence of an ON state. Test-only Android JSON runtime, no physical Tank.
 */
@RunWith(AndroidJUnit4.class)
public final class FieldCycleRelayOffAckInstrumentedTest {
    private boolean off(String payload) throws Exception {
        Method method=FieldCycleController.class.getDeclaredMethod(
            "relayOffAcknowledged",JSONObject.class);
        method.setAccessible(true);
        return (Boolean)method.invoke(null,new JSONObject(payload));
    }

    @Test public void confirmsOnlyExplicitUnambiguousRelayOff() throws Exception {
        assertTrue(off("{\"device_id\":\"esp32\",\"relay\":{\"on\":false}}"));
        assertTrue(off("{\"device_id\":\"esp32\",\"relay\":{\"mask\":0}}"));
        assertTrue(off("{\"device_id\":\"esp32\",\"relay\":{\"on\":false,\"mask\":0}}"));
    }

    @Test public void missingAndUnknownRelayCannotBeApproved() throws Exception {
        assertFalse(off("{\"device_id\":\"esp32\"}"));
        assertFalse(off("{\"device_id\":\"esp32\",\"relay\":{}}"));
        assertFalse(off("{\"device_id\":\"esp32\",\"relay\":null}"));
        assertFalse(off("{\"relay\":{\"on\":null}}"));
        assertFalse(off("{\"relay\":{\"mask\":null}}"));
    }

    @Test public void activeContradictoryAndMalformedRelayStatesAreRejected() throws Exception {
        assertFalse(off("{\"relay\":{\"on\":true}}"));
        assertFalse(off("{\"relay\":{\"mask\":1}}"));
        assertFalse(off("{\"relay\":{\"mask\":0,\"on\":true}}"));
        assertFalse(off("{\"relay\":{\"mask\":1,\"on\":false}}"));
        assertFalse(off("{\"relay\":{\"mask\":0.5}}"));
        assertFalse(off("{\"relay\":{\"mask\":\"0\"}}"));
        assertFalse(off("{\"relay\":{\"on\":\"false\"}}"));
    }
}
