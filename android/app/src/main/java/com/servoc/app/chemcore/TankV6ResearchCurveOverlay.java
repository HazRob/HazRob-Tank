package com.servoc.app.chemcore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * One-shot V6 research overlay of all original source curves matching actual
 * MQ sensor types. No heuristics, lab-metadata, confidence, G3 or source-domain
 * threshold excludes any finite/defined numerical estimate.
 *
 * Does not alter V5 candidates, controls, warnings, DB tables or reported ppm.
 * The 43 curves come from a single research lineage; 43 != 43 independent trials.
 */
public final class TankV6ResearchCurveOverlay {
    public static final String KEY="v6_all_reference_curves_research";
    public static final String SCHEMA="hazrob-tank-v6-all-curves/1";
    private TankV6ResearchCurveOverlay(){}

    public static final class Row {
        public final String id,sourceRoot,sensorModel,gasLabel;
        public final long chemicalLegacyId;
        public final double ppmResearch,ratioObserved;
        public final boolean finite,outsideReferenceRange;
        public final List<String> advisories;
        private Row(SourceCurveRegistryV6.Entry entry,SourceCurveRegistryV6.Estimate fit) {
            id=entry.recordId();sourceRoot=SourceCurveRegistryV6.SOURCE_ROOT;
            sensorModel=entry.sensorModel;gasLabel=entry.gasLabel;
            chemicalLegacyId=entry.legacyId;
            ppmResearch=fit.ppmResearch;ratioObserved=fit.observedRsOverR0;
            finite=fit.numericallyUsable();outsideReferenceRange=fit.outsideSourceDomain;
            advisories=fit.notes;
        }
    }
    public static final class Result {
        public final int sourceCurves,validNumericalFits,missingSensorCurves,invalidNumericalFits;
        public final List<Row> rows;
        public final boolean physicalCalibrationProven,reportablePpm;
        private Result(int count,int numeric,int missing,int invalid,List<Row> fits) {
            sourceCurves=count;validNumericalFits=numeric;missingSensorCurves=missing;
            invalidNumericalFits=invalid;
            rows=Collections.unmodifiableList(new ArrayList<>(fits));
            physicalCalibrationProven=false;reportablePpm=false;
        }
    }

    /** Observed ratios are dimensionless. Passing rs=ratio,r0=1 is exact arithmetic,
     * not a claim that physical R0 equals 1 ohm.
     */
    public static Result evaluate(Map<String,Double> recordedRsOverR0) {
        List<Row> out=new ArrayList<>();
        int missing=0,invalid=0,valid=0;
        for(SourceCurveRegistryV6.Entry entry:SourceCurveRegistryV6.all()){
            Double ratio=recordedRsOverR0==null?null:
                         recordedRsOverR0.get(entry.sensorModel.toLowerCase(java.util.Locale.ROOT));
            if(ratio==null){missing++;continue;}
            SourceCurveRegistryV6.Estimate estimate=
                SourceCurveRegistryV6.explore(entry,entry.sensorModel,ratio,1.0);
            out.add(new Row(entry,estimate));
            if(estimate.numericallyUsable())valid++;else invalid++;
        }
        return new Result(SourceCurveRegistryV6.all().size(),valid,missing,invalid,out);
    }
}
