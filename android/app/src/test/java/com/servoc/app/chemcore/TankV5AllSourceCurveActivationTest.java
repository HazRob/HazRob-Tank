package com.servoc.app.chemcore;

import org.json.JSONObject;
import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

/** Mathematical/source tests; measured gas accuracy requires Tank G3 experiments. */
public final class TankV5AllSourceCurveActivationTest {
    private static JSONObject prior(String source,double a,double b,String tier)throws Exception{
        return new JSONObject().put("sensor_key","mq135").put("form","LOG10_LINEAR")
            .put("intercept",Math.log10(a)).put("slope",b)
            .put("sigma_log10",0.20).put("min_ppm",10.0).put("max_ppm",100.0)
            .put("evidence_tier",tier).put("device_validated",false)
            .put("model_version",source).put("source_id",source);
    }
    @Test public void overlappingResearchAndLiteratureArePreservedAsAlternatives()throws Exception{
        TankStableScreeningModelRegistry.StableModel research=
            new TankStableScreeningModelRegistry.StableModel(
                prior("research",4.0,-.3,"M1"));
        TankStableScreeningModelRegistry.StableModel literature=
            new TankStableScreeningModelRegistry.StableModel(
                prior("literature",3.8659,-.3091,"M2"));
        TankMultiSourceResponseModel selected=new TankMultiSourceResponseModel(
            "mq135",Arrays.asList(research,literature));
        assertEquals(2,selected.sources().size());
        ChemCoreResponseModel.Estimate y=selected.predict(50,25,50,null);
        assertTrue(y.available);
        assertEquals(Math.log10(3.8659)-0.3091*Math.log10(50),y.meanLog10Ratio,1e-12);
        assertEquals("literature",y.sourceId);
        assertFalse(selected.reportableQuantitative());
        assertEquals("mq135",selected.sensorKey());
    }
    @Test public void insideDomainSourceReplacesMissingAlternativeNotWholeSensor()throws Exception{
        TankStableScreeningModelRegistry.StableModel first=
            new TankStableScreeningModelRegistry.StableModel(prior("first",3.0,-.3,"M1"));
        JSONObject second=prior("second",4.0,-.4,"M2")
            .put("min_ppm",101).put("max_ppm",500);
        TankStableScreeningModelRegistry.StableModel other=
            new TankStableScreeningModelRegistry.StableModel(second);
        TankMultiSourceResponseModel m=new TankMultiSourceResponseModel(
            "mq135",Arrays.asList(first,other));
        assertEquals("first",m.predict(50,25,50,null).sourceId);
        assertEquals("second",m.predict(200,25,50,null).sourceId);
        assertFalse(m.predict(100.5,25,50,null).available);
    }
    @Test public void fullSourceCurveMayProvideScreeningWithoutM4()throws Exception{
        TankStableScreeningModelRegistry.StableModel m=
            new TankStableScreeningModelRegistry.StableModel(prior("factory",2.0,-.3,"M2"));
        assertTrue(m.predict(40,25,50,null).available);
        assertFalse(m.reportableQuantitative());
    }
}
