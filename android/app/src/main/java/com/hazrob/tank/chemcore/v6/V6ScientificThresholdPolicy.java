package com.hazrob.tank.chemcore.v6;

import java.util.Locale;

/**
 * V11 source-limit screening boundary. This class compares concentrations with
 * documented limits; it DOES NOT establish true gas identity, calibrated ppm,
 * safe-entry clearance, an occupational TWA/STEL exceedance or certified alarms.
 *
 * 90% of a PEL/REL/STEL/ceiling is a configurable HazRob PRE-ALERT policy, not a
 * regulation. A source limit at 100% is an immediate potential-area-hazard
 * indicator if the instrument measures that gas validly. TWA/STEL time
 * aggregation must still be performed separately.
 *
 * Flammability is independent: precaution begins at 10% of the lower
 * flammable limit when the source provides an applicable LFL, not 90% LFL.
 */
public final class V6ScientificThresholdPolicy {
    public static final String SCHEMA="hazrob-tank-v6-scientific-thresholds/1";
    public static final double DEFAULT_EARLY_EXPOSURE_FRACTION=0.90;
    public static final double PRECAUTIONARY_LFL_FRACTION=0.10;
    private V6ScientificThresholdPolicy(){}

    public static final class Decision {
        public final String state,role;
        public final double thresholdFraction,thresholdPpm,referencePpm,
                            observedPpm,observedFraction;
        public final boolean sitePotentialHazard,regulatoryTwaExceeded,
                             certifiedGasAlarm;
        private Decision(String s,String r,double trigger,double ref,double observed){
            state=s;role=r;thresholdFraction=trigger;referencePpm=ref;
            observedPpm=observed;thresholdPpm=ref*trigger;
            observedFraction=observed/ref;
            sitePotentialHazard="AT_OR_ABOVE_REFERENCE".equals(s)||
                "AT_OR_ABOVE_LFL".equals(s);
            regulatoryTwaExceeded=false; // instantaneous estimate cannot establish a TWA
            certifiedGasAlarm=false;      // Tank G3 qualification is not achieved
        }
    }

    public static Decision compare(double observedPpm,double referencePpm,
                                   String role,double earlyFraction){
        if(!Double.isFinite(observedPpm)||observedPpm<0||
           !Double.isFinite(referencePpm)||referencePpm<=0)
            throw new IllegalArgumentException("Nonfinite or negative chemical concentration");
        if(!Double.isFinite(earlyFraction)||earlyFraction<=0||earlyFraction>1)
            throw new IllegalArgumentException("Early fraction must be (0,1]");
        String normalized=role==null?"OTHER":role.toUpperCase(Locale.ROOT);
        boolean lfl="LFL".equals(normalized);
        double fraction=lfl?PRECAUTIONARY_LFL_FRACTION:earlyFraction;
        double ratio=observedPpm/referencePpm;
        String state=ratio>=1.0?(lfl?"AT_OR_ABOVE_LFL":"AT_OR_ABOVE_REFERENCE"):
            ratio>=fraction?(lfl?"LFL_PRECAUTIONARY":"EARLY_WARNING"):
            "BELOW_ADVISORY";
        return new Decision(state,normalized,fraction,referencePpm,observedPpm);
    }
}
