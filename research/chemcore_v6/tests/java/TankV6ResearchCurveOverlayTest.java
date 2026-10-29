package com.servoc.app.chemcore;

import java.util.HashMap;
import java.util.Map;

/** Validate simple 43-curve one-shot research overlay; NOT Tank G3. */
public final class TankV6ResearchCurveOverlayTest {
    private static int assertions=0;
    static void ok(boolean b,String why){assertions++;if(!b)throw new AssertionError(why);}
    public static void main(String[] args){
        Map<String,Double> ratios=new HashMap<>();
        for(String sensor:new String[]{"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"})
            ratios.put(sensor,1.0);
        var result=TankV6ResearchCurveOverlay.evaluate(ratios);
        ok(result.sourceCurves==43,"source count");
        ok(result.validNumericalFits==43,"all original source fits visible");
        ok(result.missingSensorCurves==0,"all original sensor models present");
        ok(result.invalidNumericalFits==0,"all finite");
        ok(result.rows.size()==43,"rows one per reference");
        ok(!result.physicalCalibrationProven&&!result.reportablePpm,
           "research math is not certified device evidence");
        for(var row:result.rows){
            ok(row.finite,"source formula finite");
            ok(row.ppmResearch>0&&Double.isFinite(row.ppmResearch),"positive finite result");
            ok(row.advisories.contains("SOURCE_R0_REFERENCE_NOT_INDEPENDENTLY_VERIFIED"),
              "unknown reference R0 does not block but remains visible");
            ok(row.sourceRoot.equals(SourceCurveRegistryV6.SOURCE_ROOT),
               "source family retained without inflating independent experiments");
        }
        var partial=new HashMap<String,Double>();
        partial.put("mq2",2.0);
        var p=TankV6ResearchCurveOverlay.evaluate(partial);
        ok(p.validNumericalFits==7,"all seven MQ2 relations reused");
        ok(p.missingSensorCurves==36,"missing sensor channels not fabricated");
        partial.put("mq2",Double.NaN);
        var invalid=TankV6ResearchCurveOverlay.evaluate(partial);
        ok(invalid.validNumericalFits==0,"invalid arithmetic not labeled finite");
        ok(invalid.invalidNumericalFits==7,"all seven invalid calculations tracked");
        ok(invalid.rows.size()==7,"audit rows remain visible despite failure");
        var empty=TankV6ResearchCurveOverlay.evaluate(null);
        ok(empty.validNumericalFits==0 && empty.missingSensorCurves==43,
           "absence of measurement is not invented");
        System.out.println("PASS TankV6ResearchCurveOverlay: "+assertions+
                           " assertions; all 43 numerical research hypotheses tested");
    }
}
