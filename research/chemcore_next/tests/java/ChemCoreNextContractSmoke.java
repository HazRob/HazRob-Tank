package com.hazrob.tank.chemcore.next;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ChemCoreNextContractSmoke {
    private static int count = 0;
    private static void ok(boolean good, String what) {
        count++;
        if (!good) throw new AssertionError("FAILED test " + count + ": " + what);
    }
    private static void rejects(Runnable r, String what) {
        boolean failed = false;
        try { r.run(); } catch (IllegalArgumentException | UnsupportedOperationException expected) { failed = true; }
        ok(failed, what);
    }
    private static CoreContracts.Reading reading(String id, double rs) {
        return new CoreContracts.Reading(id, 1200, 1.15, rs, 10.0, 2.0,
            CoreContracts.CalibrationReference.SOURCE_R0,
            "HEATER_PROFILE_UNVERIFIED", CoreContracts.SampleQuality.VALID);
    }
    private static CoreContracts.Snapshot snapshot(List<CoreContracts.Reading> readings) {
        return new CoreContracts.Snapshot("snap-001", "tank-demo", CoreContracts.Mode.INSTANT,
            120000, 26.5, 49.0, true, readings, "stable-source-sha");
    }
    private static List<CoreContracts.SensorInstance> sensors() {
        List<CoreContracts.SensorInstance> out = new ArrayList<>();
        for (String m : Arrays.asList("MQ2", "MQ3", "MQ4", "MQ5", "MQ7", "MQ8", "MQ9", "MQ135"))
            out.add(new CoreContracts.SensorInstance("tank:" + m, m, "ADS1115-1", "UNVERIFIED"));
        return out;
    }
    public static void main(String[] args) {
        List<CoreContracts.SensorInstance> eight = sensors();
        SensorCapabilityRegistry reg8 = new SensorCapabilityRegistry(eight);
        ok(reg8.installedCount() == 8, "8 source installed sensors");
        eight.add(new CoreContracts.SensorInstance("tank:future", "MQ137", "ADS1115-2", "UNVERIFIED"));
        ok(reg8.installedCount() == 8, "source list mutation cannot change registry");
        SensorCapabilityRegistry reg9 = new SensorCapabilityRegistry(eight);
        ok(reg9.installedCount() == 9, "dynamic sensor registry supports future 9th channel");
        ok(reg9.byUid("tank:future") != null, "future sensor discoverable");
        rejects(() -> new SensorCapabilityRegistry(Arrays.asList(eight.get(0), eight.get(0))),
            "duplicate physical sensor IDs rejected");

        List<CoreContracts.Reading> sample = new ArrayList<>();
        sample.add(reading("tank:MQ2", 8.0));
        CoreContracts.Snapshot s = snapshot(sample);
        sample.add(reading("tank:MQ3", 9.0));
        ok(s.readings.size() == 1, "snapshot deep structural copy");
        rejects(() -> s.readings.add(reading("tank:MQ8", 6.0)),
            "snapshot reading list immutable");
        ok(Math.abs(s.readings.get(0).airRatioOrNaN() - 0.8) < 1e-12,
            "physical air ratio distinct");
        ok(Math.abs(s.readings.get(0).sourceRatioOrNaN() - 4.0) < 1e-12,
            "source R0 ratio distinct");

        CoreContracts.Result r = ChemCoreNext.evaluate(s, reg8);
        ok(r.status == CoreContracts.DecisionStatus.NO_INFERENCE_MODEL,
            "research skeleton reports no inference model");
        ok(r.rankedEligibleCandidates.isEmpty(), "no evidence-free chemical guesses");
        ok(!r.reportablePpm && !r.physicallyValidatedG3, "no certified ppm or G3 flags");
        ok(r.evidence.isEmpty(), "no invented sensor evidence");
        ok(ChemCoreNext.snapshotHash(s).equals(ChemCoreNext.snapshotHash(snapshot(
            Collections.singletonList(reading("tank:MQ2", 8.0))))),
            "deterministic hash for same fully defined input");
        ok(!ChemCoreNext.snapshotHash(s).equals(ChemCoreNext.snapshotHash(snapshot(
            Collections.singletonList(reading("tank:MQ2", 8.1))))),
            "hash tracks physical raw change");

        CoreContracts.Snapshot bad = snapshot(Collections.singletonList(reading("other:MQ2", 8.0)));
        ok(ChemCoreNext.evaluate(bad, reg8).status == CoreContracts.DecisionStatus.INVALID_INPUT,
            "unregistered physical sensor fails closed");
        rejects(() -> new CoreContracts.Reading("tank:MQ2", null, null, 0.0, 10.0,
            null, CoreContracts.CalibrationReference.AIR_RS_MEAN, "UNKNOWN",
            CoreContracts.SampleQuality.VALID), "invalid Rs rejected");
        rejects(() -> new CoreContracts.Reading("tank:MQ2", null, null, 8.0, 10.0,
            null, CoreContracts.CalibrationReference.SOURCE_R0, "UNKNOWN",
            CoreContracts.SampleQuality.VALID), "source R0 cannot be missing if selected");
        rejects(() -> snapshot(Arrays.asList(reading("tank:MQ2", 8.0), reading("tank:MQ2", 7.0))),
            "duplicate physical channel in snapshot rejected");


        CoreContracts.DeviceProfile profile = new CoreContracts.DeviceProfile(
            "tank-demo", "TANK-DOIT-V1", "ESP32-TANK-CANDIDATE", eight);
        ok(profile.installedSensors.size() == 9, "device supports expandable inventory");
        rejects(() -> profile.installedSensors.clear(), "device profile immutable");
        rejects(() -> new CoreContracts.DeviceProfile("x","hw","fw",
            Arrays.asList(eight.get(0),eight.get(0))), "duplicated device sensor rejected");
        CoreContracts.EvidenceAsset theoretical = new CoreContracts.EvidenceAsset(
            "curve-mq2-h2", "datasheet-root", "https://example.org/datasheet",
            "git-sha", "MQ2", "H2", CoreContracts.EvidenceAsset.Origin.DATASHEET,
            null, 10.0, 1000.0);
        ok(theoretical.canonicalChemicalUid == null, "unresolved source label does not invent chemical UID");
        ok(theoretical.withinDeclaredPpmRange(100.0), "source range boundaries tracked");
        ok(!theoretical.withinDeclaredPpmRange(1.0), "out of source range is not silently accepted");
        rejects(() -> new CoreContracts.EvidenceAsset("id","root","url","rev","MQ2","H2",
            CoreContracts.EvidenceAsset.Origin.DATASHEET,null,100.0,10.0),
            "invalid curve concentration interval rejected");

        CoreContracts.Reading missing = new CoreContracts.Reading("tank:MQ2", null, null,
            null, null, null, CoreContracts.CalibrationReference.NOT_AVAILABLE,
            "NOT_RECORDED", CoreContracts.SampleQuality.NO_SAMPLE);
        ok(Double.isNaN(missing.airRatioOrNaN()), "missing sample not treated as absence of gas");
        ok(ChemCoreNext.evaluate(snapshot(Collections.singletonList(missing)), reg8)
                .status == CoreContracts.DecisionStatus.INVALID_INPUT,
            "missing sensor signal never proves chemical absence");

        System.out.println("CHEMCORE_NEXT_P01_CORE_CONTRACT_SMOKE_PASS assertions=" + count
            + " version=" + ChemCoreNext.VERSION
            + " sensor_dynamic=8_to_9"
            + " certified_ppm=false g3=false");
    }
}
