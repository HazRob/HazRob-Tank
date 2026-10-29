package com.servoc.app.chemcore;

/**
 * V6 source-curve numerical kernel (research only).
 *
 * Uses published MQDataScience convention: Rs/R0 = a*(ppm/outputScale)^b.
 * A source may be marked R0_OVER_RS only if its original curve actually
 * uses that direction. Never use a guessed R0 definition to fit real data.
 */
public final class TankV6CurveKernel {
    private TankV6CurveKernel(){}
    public enum State { RESEARCH_SCREENING, INVALID_SOURCE_CURVE, SOURCE_R0_MISMATCH,
        INVALID_SAMPLE, OUTSIDE_SOURCE_RANGE, SOURCE_NOT_COMPATIBLE }
    public static final class Curve {
        public final String id;
        public final TankV6SourceCompatibility.Source source;
        public final double ratioA, ratioB, outputScale;
        public Curve(String id,TankV6SourceCompatibility.Source source,
                     double a,double b,double outputScale) {
            this.id=id;this.source=source;ratioA=a;ratioB=b;this.outputScale=outputScale;
        }
        boolean usable() {
            return id!=null && !id.trim().isEmpty() && source!=null &&
                source.id!=null && !source.id.trim().isEmpty() &&
                source.sensorModel!=null && !source.sensorModel.trim().isEmpty() &&
                source.r0Definition!=null && source.ratioDirection!=null &&
                valid(ratioA) && Double.isFinite(ratioB) && Math.abs(ratioB)>1e-12 &&
                valid(outputScale) && valid(source.minPpm) &&
                valid(source.maxPpm) && source.maxPpm>source.minPpm;
        }
    }
    public static final class Fit {
        public final State state;
        public final double researchFitPpm, observedSourceRatio;
        public final String reason,sourceId;
        public final boolean reportablePpm,deviceValidated;
        private Fit(State s,double ppm,double ratio,String why,String src){
            state=s;researchFitPpm=ppm;observedSourceRatio=ratio;
            reason=why;sourceId=src;
            reportablePpm=false;deviceValidated=false;
        }
    }
    private static boolean valid(double value){
        return Double.isFinite(value) && value>0;
    }
    private static Fit denied(State s,String reason,String sourceId){
        return new Fit(s,Double.NaN,Double.NaN,reason,sourceId);
    }

    public static double forwardResearchRatio(Curve curve,double ppm) {
        if(curve==null||!curve.usable())throw new IllegalArgumentException("UNQUALIFIED_CURVE");
        if(!valid(ppm)||ppm<curve.source.minPpm||ppm>curve.source.maxPpm)
            throw new IllegalArgumentException("PPM_OUTSIDE_SOURCE_RANGE");
        double logRatio=Math.log(curve.ratioA)+curve.ratioB*(Math.log(ppm)-Math.log(curve.outputScale));
        double sourceRatio=Math.exp(logRatio);
        if(!valid(sourceRatio))throw new IllegalArgumentException("INVALID_PREDICTED_RATIO");
        return sourceRatio;
    }

    public static Fit inverseResearchFit(TankV6SourceCompatibility.Sample sample,Curve curve){
        if(curve==null||!curve.usable())return denied(State.INVALID_SOURCE_CURVE,
                  "UNQUALIFIED_SOURCE_CURVE",curve==null?"":curve.id);
        String sourceId=curve.source.id;
        if(sample==null || sample.sensorModel==null ||
           !sample.sensorModel.equalsIgnoreCase(curve.source.sensorModel) ||
           !valid(sample.rsOhm) || !valid(sample.r0Ohm))
            return denied(State.INVALID_SAMPLE,"MISSING_OR_INVALID_SAMPLE",sourceId);
        if(sample.r0Definition!=curve.source.r0Definition)
            return denied(State.SOURCE_R0_MISMATCH,"INCOMPATIBLE_R0_DEFINITION",sourceId);
        if(sample.ratioDirection==null)return denied(State.INVALID_SAMPLE,
                  "UNKNOWN_ACQUISITION_RATIO_DIRECTION",sourceId);
        double sourceRatio=sample.rsOhm/sample.r0Ohm;
        if(curve.source.ratioDirection==TankV6SourceCompatibility.RatioDirection.R0_OVER_RS)
            sourceRatio=1.0/sourceRatio;
        if(!valid(sourceRatio))return denied(State.INVALID_SAMPLE,"INVALID_SOURCE_RATIO",sourceId);
        double logPpm=Math.log(curve.outputScale)+
                       (Math.log(sourceRatio)-Math.log(curve.ratioA))/curve.ratioB;
        double ppm=Math.exp(logPpm);
        if(!valid(ppm)||ppm<curve.source.minPpm*(1-1e-10) ||
                          ppm>curve.source.maxPpm*(1+1e-10))
            return denied(State.OUTSIDE_SOURCE_RANGE,"INVERSE_PPM_OUTSIDE_SOURCE_DOMAIN",sourceId);
        // Source compatibility applies device and instance scopes. This kernel deliberately
        // requests RESEARCH only. Source flags never confer M4 without independent G3 evidence.
        TankV6SourceCompatibility.Verdict compatible=
            TankV6SourceCompatibility.evaluate(sample,curve.source,
                Math.max(curve.source.minPpm,Math.min(curve.source.maxPpm,ppm)),false);
        if(!compatible.usable)
            return denied(State.SOURCE_NOT_COMPATIBLE,compatible.reason,sourceId);
        return new Fit(State.RESEARCH_SCREENING,ppm,sourceRatio,
                       "RESEARCH_CURVE_FIT_ONLY_NOT_TANK_CALIBRATED",sourceId);
    }
}
