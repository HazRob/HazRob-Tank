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
    @Test public void allTwentySelectorsHaveAnEvidenceBackedEligibilityPath()throws Exception{
        String env=new JSONObject().put("temperature",22.0).put("humidity",45.0)
            .put("pressure",1007.0).toString();
        String mq=new JSONObject()
            .put("mq2",new JSONObject().put("ratio",0.75))
            .put("mq3",new JSONObject().put("ratio",1.20))
            .put("mq4",new JSONObject().put("ratio",0.90)).toString();
        String sound=new JSONObject().put("sound",new JSONObject()
            .put("rms_v",0.07).put("p2p_v",0.23)).toString();
        SessionSeries.Series capture=series(env,env,sound,mq);
        JSONObject result=new JSONObject()
            .put("candidates",new org.json.JSONArray().put(new JSONObject()
                .put("label","Test A").put("posterior",0.65)))
            .put("screening_144",new JSONObject().put("rows",
                new org.json.JSONArray().put(new JSONObject().put("status","SUPPORTED"))))
            .put("gas_exposure_ppm",new JSONObject()
                .put("input_gate_passed",true).put("percent_lfl",19.0));
        for(int chart=0;chart<20;chart++)
            assertTrue("Real-data selector "+chart+" missing a route",
                TankChartEligibility.hasData(chart,capture,result));
    }
    @Test public void missingEvidenceNeverOffersAnEmptyGraph()throws Exception{
        for(int chart=0;chart<20;chart++)
            assertFalse("Missing-data selector "+chart+" should be unavailable",
                TankChartEligibility.hasData(chart,null,new JSONObject()));
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
