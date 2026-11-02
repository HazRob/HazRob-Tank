package com.hazrob.tank.chemcore.next;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * ChemCore Next 0.1.0-research: immutable scientific contracts.
 * No Android, database, sensor hardware, or historical ChemCore dependency.
 * A sensor signal is NOT a chemically verified identity or ppm measurement.
 */
public final class CoreContracts {
    private CoreContracts() {}
    public static final String SCHEMA = "hazrob.chemcore.next.contract/1";

    public enum Mode { INSTANT, FIELD_SESSION, TRACKING, GENERAL, LAB }
    public enum CalibrationReference { AIR_RS_MEAN, SOURCE_R0, NOT_AVAILABLE }
    public enum SampleQuality {
        VALID, NO_SAMPLE, STALE, INVALID_VOLTAGE, SATURATED_ADC,
        MISSING_R_AIR, BAD_HEATER_PHASE, UNCERTAIN_REFERENCE
    }
    public enum EvidencePolarity { POSITIVE, CONDITIONAL_NEGATIVE, AMBIGUOUS, NOT_EVALUABLE }
    public enum DecisionStatus {
        NO_INFERENCE_MODEL, INSUFFICIENT_EVIDENCE, UNKNOWN,
        RESEARCH_CANDIDATES, MIXTURE_UNRESOLVED, INVALID_INPUT
    }

    private static String required(String value, String field) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(field + " required");
        return value.trim();
    }
    private static void optionalFinite(Double value, String field) {
        if (value != null && !Double.isFinite(value)) throw new IllegalArgumentException(field + " must be finite");
    }
    private static void positive(Double value, String field) {
        optionalFinite(value, field);
        if (value != null && value <= 0) throw new IllegalArgumentException(field + " must be positive");
    }

    public static final class SensorInstance {
        public final String uid, model, boardId, heaterProfile;
        public SensorInstance(String uid, String model, String boardId, String heaterProfile) {
            this.uid = required(uid, "sensor UID");
            this.model = required(model, "sensor model");
            this.boardId = required(boardId, "board ID");
            this.heaterProfile = required(heaterProfile, "heater profile");
        }
    }

    public static final class Reading {
        public final String sensorUid, heaterPhase;
        public final Integer rawAdc;
        public final Double voltageV, rsKohm, rAirKohm, r0SourceKohm;
        public final CalibrationReference r0Definition;
        public final SampleQuality quality;
        public Reading(String sensorUid, Integer rawAdc, Double voltageV, Double rsKohm,
                       Double rAirKohm, Double r0SourceKohm, CalibrationReference r0Definition,
                       String heaterPhase, SampleQuality quality) {
            this.sensorUid = required(sensorUid, "reading sensor UID");
            if (rawAdc != null && rawAdc < 0) throw new IllegalArgumentException("ADC cannot be negative");
            optionalFinite(voltageV, "voltage V");
            positive(rsKohm, "Rs kOhm");
            positive(rAirKohm, "R_air kOhm");
            positive(r0SourceKohm, "R0_source kOhm");
            this.rawAdc = rawAdc;
            this.voltageV = voltageV;
            this.rsKohm = rsKohm;
            this.rAirKohm = rAirKohm;
            this.r0SourceKohm = r0SourceKohm;
            this.r0Definition = Objects.requireNonNull(r0Definition, "R0 definition");
            this.heaterPhase = required(heaterPhase, "heater phase");
            this.quality = Objects.requireNonNull(quality, "reading quality");
            if (quality == SampleQuality.VALID && (rsKohm == null || rAirKohm == null))
                throw new IllegalArgumentException("VALID requires Rs and R_air; no fabricated zero readings");
            if (r0Definition == CalibrationReference.SOURCE_R0 && r0SourceKohm == null)
                throw new IllegalArgumentException("SOURCE_R0 requires a physical/source reference");
        }
        public double airRatioOrNaN() {
            return quality == SampleQuality.VALID && rsKohm != null && rAirKohm != null
                ? rsKohm / rAirKohm : Double.NaN;
        }
        public double sourceRatioOrNaN() {
            return quality == SampleQuality.VALID && r0Definition == CalibrationReference.SOURCE_R0
                    && rsKohm != null && r0SourceKohm != null
                ? rsKohm / r0SourceKohm : Double.NaN;
        }
    }

    public static final class Snapshot {
        public final String uid, deviceUid, stableManifestHash;
        public final Mode mode;
        public final long monotonicMillis;
        public final Double temperatureC, relativeHumidityPercent;
        public final boolean mqRelayOn;
        public final List<Reading> readings;
        public Snapshot(String uid, String deviceUid, Mode mode, long monotonicMillis,
                        Double temperatureC, Double relativeHumidityPercent,
                        boolean mqRelayOn, List<Reading> readings, String stableManifestHash) {
            this.uid = required(uid, "snapshot UID");
            this.deviceUid = required(deviceUid, "device UID");
            this.mode = Objects.requireNonNull(mode, "measurement mode");
            if (monotonicMillis < 0) throw new IllegalArgumentException("negative monotonic time");
            optionalFinite(temperatureC, "temperature C");
            optionalFinite(relativeHumidityPercent, "RH percent");
            if (relativeHumidityPercent != null &&
                    (relativeHumidityPercent < 0 || relativeHumidityPercent > 100))
                throw new IllegalArgumentException("RH outside physical percentage range");
            if (readings == null) throw new IllegalArgumentException("readings required");
            List<Reading> copy = new ArrayList<>(readings);
            Set<String> found = new HashSet<>();
            for (Reading r : copy) {
                if (r == null || !found.add(r.sensorUid))
                    throw new IllegalArgumentException("duplicate/null sensor in one snapshot");
            }
            this.monotonicMillis = monotonicMillis;
            this.temperatureC = temperatureC;
            this.relativeHumidityPercent = relativeHumidityPercent;
            this.mqRelayOn = mqRelayOn;
            this.readings = Collections.unmodifiableList(copy);
            this.stableManifestHash = stableManifestHash == null ? "" : stableManifestHash;
        }
    }

    public static final class EvidenceNode {
        public final String sourceRootId, expertId, sensorUid, note;
        public final Long canonicalChemicalUid;
        public final EvidencePolarity polarity;
        public EvidenceNode(String sourceRootId, String expertId, String sensorUid,
                            Long canonicalChemicalUid, EvidencePolarity polarity, String note) {
            this.sourceRootId = required(sourceRootId, "independent evidence source root");
            this.expertId = required(expertId, "expert ID");
            this.sensorUid = required(sensorUid, "physical sensor UID");
            if (canonicalChemicalUid != null && canonicalChemicalUid <= 0)
                throw new IllegalArgumentException("chemical UID must be positive");
            this.canonicalChemicalUid = canonicalChemicalUid;
            this.polarity = Objects.requireNonNull(polarity, "polarity");
            this.note = note == null ? "" : note;
        }
    }

    public static final class Candidate {
        public final long canonicalChemicalUid;
        public final String evidenceSummary;
        public final List<String> sourceRoots;
        public Candidate(long canonicalChemicalUid, String evidenceSummary, List<String> sourceRoots) {
            if (canonicalChemicalUid <= 0) throw new IllegalArgumentException("canonical chemical UID required");
            if (sourceRoots == null || sourceRoots.isEmpty())
                throw new IllegalArgumentException("candidate requires actual evidence source roots");
            this.canonicalChemicalUid = canonicalChemicalUid;
            this.evidenceSummary = required(evidenceSummary, "candidate evidence summary");
            this.sourceRoots = Collections.unmodifiableList(new ArrayList<>(sourceRoots));
        }
    }

    public static final class Result {
        public final String schema, engineVersion, snapshotUid, snapshotHash, stableManifestHash;
        public final DecisionStatus status;
        public final List<EvidenceNode> evidence;
        public final List<Candidate> rankedEligibleCandidates;
        public final List<String> problems;
        public final boolean reportablePpm, physicallyValidatedG3;
        public Result(String engineVersion, Snapshot source, String snapshotHash,
                      DecisionStatus status, List<EvidenceNode> evidence,
                      List<Candidate> candidates, List<String> problems) {
            this.schema = SCHEMA;
            this.engineVersion = required(engineVersion, "engine version");
            this.snapshotUid = source.uid;
            this.snapshotHash = required(snapshotHash, "snapshot hash");
            this.stableManifestHash = source.stableManifestHash;
            this.status = Objects.requireNonNull(status, "decision status");
            this.evidence = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(evidence)));
            this.rankedEligibleCandidates =
                Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(candidates)));
            this.problems = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(problems)));
            // Not a certified device; no caller can flip these flags in research core.
            this.reportablePpm = false;
            this.physicallyValidatedG3 = false;
        }
    }
}
