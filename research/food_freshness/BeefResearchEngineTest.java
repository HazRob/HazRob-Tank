package com.hazrob.tank.foodfreshness.meat;

import java.util.LinkedHashMap;
import java.util.Map;

/** Study-prototype algebraic fixtures; not real food safety certification. */
public final class BeefResearchEngineTest {
    static int n=0;
    private static void ok(boolean v,String m){n++;if(!v)throw new AssertionError(n+": "+m);}
    public static void main(String[] args){
        ok(BeefResearchCatalog.classes().size()==4,"4 beef source groups");
        ok(BeefResearchCatalog.SENSOR_ORDER.size()==7,"only 7 overlapping MQ models");
        ok(!BeefResearchCatalog.SENSOR_ORDER.contains("MQ7"),"no invented MQ7 training channel");
        ok(BeefResearchCatalog.HELDOUT_ACCURACY>0.43 &&
           BeefResearchCatalog.HELDOUT_ACCURACY<0.45,"source held-out accuracy not inflated");
        ok(BeefResearchCatalog.HELDOUT_BALANCED_ACCURACY<0.4,
           "weak held-out group performance explicitly known");
        int count=0;
        for(BeefResearchCatalog.Template label:BeefResearchCatalog.classes()){
            double[] shape=label.logShape();
            Map<String,Double> input=new LinkedHashMap<>();
            for(int i=0;i<7;i++)
                input.put(BeefResearchCatalog.SENSOR_ORDER.get(i),
                          2500*Math.pow(10,shape[i]));
            BeefResearchEngine.Result r=BeefResearchEngine.analyze(input);
            ok(r.validChannels==7,"7 sensor values");
            ok(r.rankedClasses.size()==4,"no class filtered");
            ok(r.rankedClasses.get(0).classCode.equals(label.label),
               "source exemplar self-consistent");
            ok(r.rankedClasses.get(0).distance<1e-9,
               "geometric mean common scale cancels");
            ok(!r.foodSafetyCertified&&!r.directMicrobiologyMeasured
                &&!r.modelValidatedForCurrentTank,"not certified for Tank or consumption");
            count++;
        }
        Map<String,Double> partial=new LinkedHashMap<>();
        partial.put("MQ2",430.0);
        partial.put("MQ135",310.0);
        var r=BeefResearchEngine.analyze(partial);
        ok(r.validChannels==2,"partial MQ7 overlap still usable");
        ok(r.rankedClasses.size()==4,"numeric partial matching retained");
        Map<String,Double> corrupt=new LinkedHashMap<>();
        corrupt.put("MQ2",Double.NaN);corrupt.put("MQ135",-2.0);
        var empty=BeefResearchEngine.analyze(corrupt);
        ok(empty.rankedClasses.isEmpty(),"no invented math");
        ok(empty.validChannels==0,"all corrupted channels excluded");
        ok(BeefResearchEngine.analyze(null).rankedClasses.isEmpty(),
           "null raw observation has no inferred quality");
        System.out.println("PASS BeefResearchEngine: "+n+" assertions, "+
                           count+" source templates, weak heldout flagged, separate food science");
    }
}
