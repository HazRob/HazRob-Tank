package com.hazrob.tank.chemcore.next;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Protect scientific lineage, ionic-vapor distinction and single-source voting. */
public final class SharedSourceHypothesesSmoke {
    private static int checks;
    private static void ok(boolean condition, String why) {
        checks++;
        if (!condition) throw new AssertionError("Shared MQ135 hypothesis test " + checks + ": " + why);
    }
    public static void main(String[] args) {
        List<SharedSourceHypotheses.Hypothesis> list =
            SharedSourceHypotheses.provisionalInterpretations();
        ok(list.size() == 2, "two provisional interpretations visible");
        ok(list.get(0).label.equals("NH3") && list.get(1).label.equals("NH4+"),
            "ammonia vs ammonium preserved");
        ok(list.get(0).sourceCurveUid.equals(list.get(1).sourceCurveUid),
            "one numeric curve, no synthetic second independent formula");
        Set<String> roots = new HashSet<>();
        for (SharedSourceHypotheses.Hypothesis h : list) {
            roots.add(h.sourceRoot);
            ok(!h.confirmedGas, "research label is never confirmed gas");
            ok(!h.qualifiedPpm && !h.physicallyValidatedG3, "no ppm or G3");
        }
        ok(roots.size() == 1 && SharedSourceHypotheses.independentSourceEvidenceUnits() == 1,
            "two interpretations one evidence root");
        ok(list.get(0).provisionalChemicalUid == 22L,
            "historical NH3 catalog reference 22 only tentative");
        ok(list.get(1).provisionalChemicalUid == null,
            "NH4 ion must not inherit ammonia's molecular UID");
        ok(list.get(1).speciesPhase.equals("AQUEOUS_ION_NOT_GAS"),
            "ammonium ion not falsely reported as an atmospheric gas");
        ok(!SharedSourceHypotheses.canDeclareGasIdentityOrPpm(),
            "no positive gas classification issued");
        double predicted = SharedSourceHypotheses.sourceRatioResearchOnly(100, 1);
        ok(predicted > 0 && Double.isFinite(predicted), "same source equation computable only with supplied scale");
        boolean rejected = false;
        try { SharedSourceHypotheses.sourceRatioResearchOnly(100, 0); }
        catch (IllegalArgumentException expected) { rejected = true; }
        ok(rejected, "missing source scale refused");
        System.out.println("CHEMCORE_NEXT_NH3_NH4_SHARED_SOURCE_PASS assertions=" + checks +
            " interpretations=2 unique_source_roots=1 confirmed_gas=0 g3=0");
    }
}
