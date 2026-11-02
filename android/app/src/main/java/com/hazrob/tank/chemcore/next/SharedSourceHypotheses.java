package com.hazrob.tank.chemcore.next;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * ChemCore Next P03.4: one MQ135 source relation, two provisional *labels*.
 * This is a source interpretation registry, not a gas identity/ppm engine.
 * NH4+ is aqueous/ionic, not a separate free atmospheric gas.
 */
public final class SharedSourceHypotheses {
    private SharedSourceHypotheses() {}

    public static final String ORIGINAL_CURVE_UID = "MQDS-SRC-053";
    public static final String ORIGINAL_ROOT = "MQDATASCIENCE_ORIGINAL_GAS_SENSOR_CURVE_FAMILY";
    public static final String SOURCE_BLOB_SHA = "c9519bcebd22c79fadeaa6b21d22d4fb821ba290";
    public static final double SOURCE_COEFFICIENT_A = 6.2968;
    public static final double SOURCE_COEFFICIENT_B = -0.3892;

    public static final class Hypothesis {
        public final String hypothesisUid, label, speciesPhase, sourceCurveUid, sourceRoot;
        public final Long provisionalChemicalUid;
        public final boolean confirmedGas, qualifiedPpm, physicallyValidatedG3;
        private Hypothesis(String id, String label, String phase, Long provisional) {
            this.hypothesisUid = id;
            this.label = label;
            this.speciesPhase = phase;
            this.sourceCurveUid = ORIGINAL_CURVE_UID;
            this.sourceRoot = ORIGINAL_ROOT;
            this.provisionalChemicalUid = provisional;
            confirmedGas = false;
            qualifiedPpm = false;
            physicallyValidatedG3 = false;
        }
    }
    private static final List<Hypothesis> NH3_NH4 = Collections.unmodifiableList(
        Arrays.asList(
            new Hypothesis("MQ135-SRC053-NH3", "NH3", "GAS_RESEARCH_ONLY", 22L),
            new Hypothesis("MQ135-SRC053-NH4", "NH4+", "AQUEOUS_ION_NOT_GAS", null)
        ));
    /** Two visible research interpretations, one physically independent source root. */
    public static List<Hypothesis> provisionalInterpretations() { return NH3_NH4; }

    /**
     * Ratio expected from source equation at a specified ppm scale.
     * Must never run until source ppm scale and R0 definition are source-verified.
     */
    public static double sourceRatioResearchOnly(double ppm, double sourceScalePpm) {
        if (!Double.isFinite(ppm) || ppm <= 0 || !Double.isFinite(sourceScalePpm)
                || sourceScalePpm <= 0)
            throw new IllegalArgumentException("positive finite source ppm and source scale required");
        double ratio = SOURCE_COEFFICIENT_A * Math.pow(ppm / sourceScalePpm, SOURCE_COEFFICIENT_B);
        if (!Double.isFinite(ratio) || ratio <= 0)
            throw new IllegalArgumentException("source equation outside numeric limits");
        return ratio;
    }

    public static int independentSourceEvidenceUnits() { return 1; }

    public static boolean canDeclareGasIdentityOrPpm() { return false; }
}
