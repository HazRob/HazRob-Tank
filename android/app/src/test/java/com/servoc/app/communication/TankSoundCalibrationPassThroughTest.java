package com.servoc.app.communication;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** No false dBA source: an explicit calibration flag plus a numeric dBA value are required. */
public final class TankSoundCalibrationPassThroughTest {
    @Test public void genuineCalibratedAWeightedReadingSurvivesApi4Normalization() throws Exception {
        JSONObject src=new JSONObject().put("sound",new JSONObject()
            .put("dba",92.3).put("calibrated_dba",true)
            .put("rms_v",0.15));
        JSONObject sound=RobotProtocolV4.normalize(new JSONObject(),src,0,0).getJSONObject("sound");
        assertEquals(92.3,sound.getDouble("dba"),0.001);
        assertTrue(sound.getBoolean("calibrated_dba"));
    }
    @Test public void voltageAndUncalibratedDecibelReadingsCannotClaimDba()throws Exception{
        JSONObject src=new JSONObject().put("sound",new JSONObject()
            .put("rms_v",0.4).put("p2p_v",0.7).put("db_spl",115.0));
        JSONObject sound=RobotProtocolV4.normalize(new JSONObject(),src,0,0).getJSONObject("sound");
        assertFalse(sound.getBoolean("calibrated_dba"));
        assertTrue(sound.isNull("dba"));
        assertEquals(115.0,sound.getDouble("db_spl"),0.001);
        JSONObject unverified=new JSONObject().put("sound",new JSONObject().put("dba",120.0));
        assertFalse(RobotProtocolV4.normalize(new JSONObject(),unverified,0,0)
            .getJSONObject("sound").getBoolean("calibrated_dba"));
        JSONObject flagWithoutValue=new JSONObject().put("sound",new JSONObject().put("calibrated_dba",true));
        assertFalse(RobotProtocolV4.normalize(new JSONObject(),flagWithoutValue,0,0)
            .getJSONObject("sound").getBoolean("calibrated_dba"));
    }
}
