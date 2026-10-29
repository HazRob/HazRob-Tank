package com.servoc.app.chemcore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** V6 P06: research reuse of every numerically valid historic curve. */
public final class SourceCurveRegistryV6Test {
    private static int assertions=0;
    private static void check(boolean yes,String message){
        assertions++;if(!yes)throw new AssertionError("assertion "+assertions+": "+message);
    }
    public static void main(String[] args){
        List<SourceCurveRegistryV6.Entry> all=SourceCurveRegistryV6.all();
        check(all.size()==43,"43 available curves");
        check(SourceCurveRegistryV6.SOURCE_GIT_BLOB.equals(
          "bd597ffe9ecaee6a0ccfddc5ec1b89c1cccc0ba1"),"source remains pinned");
        Map<String,Integer> counts=new LinkedHashMap<>();
        int scale50=0,classes=0,calc=0;
        for(int i=0;i<all.size();i++){
            SourceCurveRegistryV6.Entry e=all.get(i);
            check(e.index==i+1,"source order unchanged");
            check(e.recordId().equals(String.format(java.util.Locale.ROOT,"MQDATA43-%03d",i+1)),
                  "source id stable");
            counts.put(e.sensorModel,counts.getOrDefault(e.sensorModel,0)+1);
            if(e.outputScale==50)scale50++;
            if(e.classOrMixedLabel())classes++;
            double midpoint=Math.sqrt(e.minPpm*e.maxPpm);
            double ratio=e.a*Math.pow(midpoint/e.outputScale,e.b);
            SourceCurveRegistryV6.Estimate fit=
                SourceCurveRegistryV6.explore(e,e.sensorModel,ratio*10000,10000);
            check(fit.numericallyUsable(),"all source models usable despite unverified R0");
            check(Math.abs(fit.ppmResearch-midpoint)/midpoint<1e-8,
                  "source mathematics forward inverse");
            check(!fit.outsideSourceDomain,"midpoint inside reference range");
            check(!fit.tankPhysicalCalibrationProven(),"no spurious Tank calibration");
            check(fit.notes.contains("SOURCE_R0_REFERENCE_NOT_INDEPENDENTLY_VERIFIED"),
                  "missing R0 source metadata noted, not blocked");
            calc++;
        }
        check(counts.size()==8,"all original eight MQ models included");
        check(scale50==6,"six MQ3 50x source-axis formulas");
        check(classes>=14,"aggregate and ambiguous labels remain available");
        check(SourceCurveRegistryV6.forSensor("MQ2").size()==7,"MQ2 curves");
        check(SourceCurveRegistryV6.forSensor("mq135").size()==6,"MQ135 curves");
        check(SourceCurveRegistryV6.forSensor("missing").isEmpty(),"no invented model");
        SourceCurveRegistryV6.Entry example=all.get(7);
        check(example.sensorModel.equals("MQ3"),"source MQ3 index 8");
        // Out of range no longer blocks; it is a visible research warning.
        double outside=example.maxPpm*2;
        double ratio=example.a*Math.pow(outside/example.outputScale,example.b);
        var extrap=SourceCurveRegistryV6.explore(example,"MQ3",ratio*10000,10000);
        check(extrap.numericallyUsable(),"finite extrapolated value remains visible");
        check(extrap.outsideSourceDomain,"marked outside source support");
        check(extrap.notes.contains("EXTRAPOLATED_BEYOND_SOURCE_RANGE"),
              "out of range warning");
        check(extrap.notes.contains("NONUNITY_OUTPUT_SCALE_SOURCE_ASSUMPTION"),
              "unknown scale interpretation noted");
        var mismatch=SourceCurveRegistryV6.explore(example,"MQ8",ratio*10000,10000);
        check(mismatch.numericallyUsable(),"numerically usable model not silently lost");
        check(mismatch.sensorLabelMismatch,"mismatched sensor flagged");
        check(mismatch.notes.contains("SENSOR_MODEL_MISMATCH_EXPLORATORY_ONLY"),
              "sensor mismatch advisory");
        for(double bad:new double[]{Double.NaN,Double.POSITIVE_INFINITY,0.0,-3.0}){
            var invalid=SourceCurveRegistryV6.explore(example,"MQ3",bad,10000);
            check(!invalid.numericallyUsable(),"bad numeric Rs filtered");
            check(Double.isNaN(invalid.ppmResearch),"undefined concentration not made up");
        }
        check(!SourceCurveRegistryV6.explore(example,"MQ3",10000,0).numericallyUsable(),
              "R0 cannot be zero");
        try{all.clear();throw new AssertionError("mutable registry");}
        catch(UnsupportedOperationException expected){assertions++;}
        check(calc==43,"all 43 numerical inverse fits exercised");
        System.out.println("PASS SourceCurveRegistryV6: "+assertions+
          " assertions, 43/43 hypotheses retained with warnings; no device validation claim");
    }
}
