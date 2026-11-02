package com.hazrob.tank.chemcore.next;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * ChemCore Next research skeleton / P01 only. It validates provenance and
 * emits NO_INFERENCE_MODEL, not speculative chemical identities or ppm.
 */
public final class ChemCoreNext {
    public static final String VERSION = "0.1.0-research-contract";
    private ChemCoreNext() {}
    public static CoreContracts.Result evaluate(CoreContracts.Snapshot snapshot,
                                                SensorCapabilityRegistry registry) {
        if (snapshot == null || registry == null) throw new IllegalArgumentException("snapshot and registry required");
        List<String> problems = new ArrayList<>();
        for (CoreContracts.Reading r : snapshot.readings) {
            if (registry.byUid(r.sensorUid) == null) problems.add("UNREGISTERED_SENSOR:" + r.sensorUid);
            if (r.quality != CoreContracts.SampleQuality.VALID)
                problems.add("INVALID_SENSOR_SIGNAL:" + r.sensorUid + ":" + r.quality);
        }
        if (snapshot.readings.isEmpty()) problems.add("NO_READINGS");
        if (snapshot.readings.size() > 0 && !snapshot.mqRelayOn)
            problems.add("RELAY_OFF_WHILE_MQ_READING_PRESENT");
        return new CoreContracts.Result(VERSION, snapshot, snapshotHash(snapshot),
            problems.isEmpty() ? CoreContracts.DecisionStatus.NO_INFERENCE_MODEL
                               : CoreContracts.DecisionStatus.INVALID_INPUT,
            Collections.emptyList(), Collections.emptyList(), problems);
    }

    /**
     * Stable, source-only numeric signature. Not an authenticated signature.
     * Uses explicit units, presence markers, and preserves physical channel order.
     */
    public static String snapshotHash(CoreContracts.Snapshot s) {
        if (s == null) throw new IllegalArgumentException("snapshot required");
        StringBuilder b = new StringBuilder("NEXT-P01|");
        append(b, CoreContracts.SCHEMA);
        append(b, s.uid); append(b, s.deviceUid); append(b, s.mode.name());
        append(b, Long.toString(s.monotonicMillis));
        append(b, numeric(s.temperatureC)); append(b, numeric(s.relativeHumidityPercent));
        append(b, Boolean.toString(s.mqRelayOn)); append(b, s.stableManifestHash);
        for (CoreContracts.Reading r : s.readings) {
            append(b, r.sensorUid); append(b, r.rawAdc == null ? "N" : r.rawAdc.toString());
            append(b, numeric(r.voltageV)); append(b, numeric(r.rsKohm));
            append(b, numeric(r.rAirKohm)); append(b, numeric(r.r0SourceKohm));
            append(b, r.r0Definition.name()); append(b, r.heaterPhase); append(b, r.quality.name());
        }
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(
                b.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte v : bytes) hex.append(String.format(Locale.ROOT, "%02x", v & 255));
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
    private static String numeric(Double n) {
        return n == null ? "N" : Long.toHexString(Double.doubleToLongBits(n));
    }
    private static void append(StringBuilder b, String value) {
        b.append(value.length()).append(':').append(value).append('|');
    }
}
