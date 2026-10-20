package com.servoc.app.data;

import com.servoc.app.report.TankChartEligibility;
import org.json.JSONObject;
import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

/** A plot is selectable only when a stored series/result contains its actual plotted dimension. */
public final class TankChartEligibilityTest {
    private static SessionSeries.Series series(String e1,String e2,String sound,String mq){
        SessionSeries.Sample a=new SessionSeries.Sample(0L,e1,mq,sound);
        SessionSeries.Sample b=new SessionSeries.Sample(1000L,e2,mq,sound);
        return new SessionSeries.Series("S","t0","t1",Arrays.asList(a,b));
    }
    @Test public void temperatureAndHumidityAreIndependentlyAvailable()throws Exception{
        SessionSeries.Series temp=series("{\"temperature\":23}","{\"temperature\":24}","{}","{}");
        assertTrue(TankChartEligibility.hasData(16,temp,null));
        assertFalse(TankChartEligibility.hasData(17,temp,null));
        assertFalse(TankChartEligibility.hasData(18,temp,null));
        assertFalse(TankChartEligibility.hasData(19,temp,null));
    }
    @Test public void soundNeedsTwoRealValues()throws Exception{
        SessionSeries.Series sound=series("{}","{}","{\"sound\":{\"rms_v\":0.06}}","{}");
        assertTrue(TankChartEligibility.hasData(19,sound,null));
        assertFalse(TankChartEligibility.hasData(0,sound,null));
    }
    @Test public void activeMqCanShowTrendButNotThreeChannelRadar()throws Exception{
        String mq="{\"mq2\":{\"rs\":25,\"r0\":50,\"ratio\":0.5,\"r0_source\":\"automatic_clean_air_field_r0\"}}";
        SessionSeries.Series s=series("{}","{}","{}",mq);
        assertTrue(TankChartEligibility.hasData(0,s,null));
        assertTrue(TankChartEligibility.hasData(1,s,null));
        assertFalse(TankChartEligibility.hasData(2,s,null));
    }
    @Test public void chemcoreKindsRequireOwnEvidence()throws Exception{
        JSONObject cc=new JSONObject("{\"candidates\":[{\"label\":\"A\",\"posterior\":0.7}]}");
        assertTrue(TankChartEligibility.hasData(3,null,cc));
        assertFalse(TankChartEligibility.hasData(7,null,cc));
        assertFalse(TankChartEligibility.hasData(10,null,cc));
        cc.put("screening_144",new JSONObject("{\"rows\":[{\"status\":\"SUPPORTED\"}]}"));
        assertTrue(TankChartEligibility.hasData(7,null,cc));
        cc.put("gas_exposure_ppm",new JSONObject("{\"input_gate_passed\":true,\"percent_lfl\":30.0}"));
        assertTrue(TankChartEligibility.hasData(10,null,cc));
        assertFalse(TankChartEligibility.hasData(20,null,cc));
    }
}
