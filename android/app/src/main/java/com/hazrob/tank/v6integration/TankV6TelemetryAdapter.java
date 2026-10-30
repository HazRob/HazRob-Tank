package com.hazrob.tank.v6integration;

import com.hazrob.tank.chemcore.v6.ChemCoreTankV6;
import com.hazrob.tank.chemcore.v6.CleanAirResponseGateV6;
import com.hazrob.tank.chemcore.v6.Engine;
import com.hazrob.tank.chemcore.v6.FingerprintEngineV6;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Thin Android JSON boundary for NEW ChemCore Tank V6 only.
 *
 * Consumes a copy of the EXISTING ESP32 Tank /data snapshot.
 * NO second polling, robot commands, heater switching, predecessor inference,
 * food-freshness references or network I/O. Missing R0 never destroys valid Rs.
 *
 * Fingerprint feature spaces require properly labeled feature-window inputs;
 * a single MQ snapshot is NOT silently converted into source ADC, RoboFusion
 * raw features or a 900-second UCI1081 temporal fingerprint.
 */
public final class TankV6TelemetryAdapter {
    public static final String SCHEMA="hazrob-tank-v6-android-telemetry-bridge/1";
    private static final String[] MQ={
        "MQ2","MQ3","MQ4","MQ5","MQ7","MQ8","MQ9","MQ135"
    };
    private TankV6TelemetryAdapter(){}

    public static ChemCoreTankV6.Result analyzeSnapshot(JSONObject data){
        return analyzeSnapshot(data,null);
    }
    /** Explicit source-space inputs only; may be empty in instantaneous mode. */
    public static ChemCoreTankV6.Result analyzeSnapshot(JSONObject data,
                                             List<FingerprintEngineV6.Input> features){
        List<Engine.Channel> channels=new ArrayList<>();
        JSONObject mq=data==null?null:data.optJSONObject("mq");
        if(mq!=null)for(String model:MQ){
            JSONObject value=mq.optJSONObject(model.toLowerCase(Locale.ROOT));
            if(value==null)continue;
            // Snapshot eligibility must be consistent with the user-facing instant
            // operation. A stale or explicitly unpowered channel is NOT a live
            // measurement merely because its last finite Rs remains in telemetry.
            if(!value.optBoolean("ok",value.optBoolean("valid",false)))continue;
            if(value.has("powered")&&!value.optBoolean("powered",false))continue;
            double rs=value.optDouble("rs",Double.NaN);
            double r0=value.optDouble("r0",Double.NaN);
            // Preserve valid raw Rs with missing R0 for evidence; it can never
            // yield a source-curve concentration until a valid R0 is supplied.
            if(!Double.isFinite(rs)||rs<=0)continue;
            channels.add(new Engine.Channel(model,model+"-tank-current-snapshot",rs,r0));
        }
        JSONObject env=data==null?null:data.optJSONObject("env");
        double temp=env==null?Double.NaN:
            env.optDouble("t",env.optDouble("temperature",Double.NaN));
        double humidity=env==null?Double.NaN:
            env.optDouble("h",env.optDouble("humidity",Double.NaN));
        JSONObject relay=data==null?null:data.optJSONObject("relay");
        double sec=relay==null?Double.NaN:relay.optDouble("power_ms",Double.NaN)/1000.0;
        return ChemCoreTankV6.analyze(new ChemCoreTankV6.Request(
            channels,features,temp,humidity,sec));
    }

    private static Object numberOrNull(double v){
        return Double.isFinite(v)?v:JSONObject.NULL;
    }

    /** Serializes ALL research evidence. Nothing is promoted to certified gas ppm. */
    public static JSONObject toJson(ChemCoreTankV6.Result data) throws Exception {
        if(data==null)throw new IllegalArgumentException("new ChemCore result unavailable");
        JSONObject root=new JSONObject();
        root.put("schema",SCHEMA);
        root.put("independent_engine_schema",data.schema);
        root.put("temperature_c",numberOrNull(data.temperatureC));
        root.put("relative_humidity_percent",numberOrNull(data.humidityPct));
        root.put("elapsed_seconds",numberOrNull(data.elapsedSeconds));
        root.put("observed_mq_channels",data.responseCurves.observedPhysicalChannels);
        root.put("raw_only_mq_channels",data.responseCurves.rawChannelsAwaitingR0);
        // Full raw channel ledger, independent of the existence of curve fits.
        // R0 is NULL rather than guessed when missing.
        JSONArray rawChannels=new JSONArray();
        for(Engine.Channel raw:data.responseCurves.retainedRawSensorRecords){
            JSONObject row=new JSONObject();
            row.put("sensor_model",raw.sensorType);
            row.put("sensor_instance",raw.sensorInstance);
            row.put("rs_as_received",numberOrNull(raw.rs));
            row.put("r0_as_received",numberOrNull(raw.r0));
            row.put("rs_r0_ratio_ready",raw.normalizedRatioAvailable());
            row.put("tank_g3_reference_calibration_validated",false);
            rawChannels.put(row);
        }
        root.put("original_mq_channel_evidence",rawChannels);
        root.put("ratio_ready_mq_channels",data.responseCurves.ratioReadyChannels);
        root.put("numerically_rejected",data.responseCurves.rejectedNumericalChannels);
        root.put("physical_calibration_proven",false);
        root.put("reportable_ppm",false);
        root.put("food_models_included",false);
        root.put("interpretation","EXPLORATORY_REFERENCE_FITS_NOT_CERTIFIED_GAS_IDENTITY");
        // N08: never confuse the mathematical inverse of clean-air Rs/R0 with
        // a detected chemical. Keep raw analytical math in research diagnostics,
        // but show NO gas hypotheses or operator warnings at the baseline.
        CleanAirResponseGateV6.Decision baseline=
            CleanAirResponseGateV6.assess(data.responseCurves.retainedRawSensorRecords);
        JSONObject gate=new JSONObject();
        gate.put("schema",CleanAirResponseGateV6.SCHEMA);
        gate.put("state",baseline.state);
        gate.put("threshold_percent_provisional",baseline.thresholdDropFraction*100.0);
        gate.put("threshold_is_manufacturer_or_safety_limit",false);
        gate.put("independent_responding_sensor_models",baseline.respondingSensorModels);
        gate.put("minimum_independent_sensor_models",
            CleanAirResponseGateV6.MIN_DISTINCT_SENSOR_MODELS);
        gate.put("rising_or_opposite_sensor_models",baseline.risingOrOppositeModels);
        gate.put("visible_gas_hypotheses",baseline.showGasHypotheses);
        gate.put("occupational_gas_alarm_authorized",false);
        gate.put("interpretation",baseline.showGasHypotheses?
            "SIGNAL_CHANGE_RESEARCH_HYPOTHESES_NOT_IDENTITY_OR_CERTIFIED_PPM":
            "BASELINE_OR_UNCONFIRMED_CHANGE_NO_OPERATOR_CHEMICAL_FINDING");
        root.put("baseline_response_gate",gate);
        root.put("gas_response_screening_state",baseline.state);
        root.put("gas_response_detected_research_only",baseline.showGasHypotheses);
        root.put("gas_alerts",new JSONArray());
        root.put("alarm_eligible",false);
        root.put("clean_air_proven",false); // equality does NOT certify atmospheric safety
        root.put("operator_chemical_finding_count",0);
        root.put("source_fits_research_only_count",data.responseCurves.allSourceFits.size());
        JSONArray sources=new JSONArray();
        for(Engine.SourceFit fit:data.responseCurves.allSourceFits){
            JSONObject row=new JSONObject();
            row.put("source_curve_id",fit.id);
            row.put("source_root",fit.sourceRoot);
            row.put("sensor_model",fit.sensorType);
            row.put("sensor_instance",fit.sensorInstance);
            row.put("chemical_reference_id",fit.chemicalId);
            row.put("gas_or_class",fit.gasLabel);
            row.put("rs_over_r0",numberOrNull(fit.observedRatio));
            row.put("source_estimate_ppm_research",numberOrNull(fit.fitPpm));
            row.put("outside_original_reference_domain",fit.outsideSourceRange);
            JSONArray caveats=new JSONArray();
            for(String message:fit.advisories)caveats.put(message);
            row.put("advisories",caveats);
            sources.put(row);
        }
        // Preserve full mathematics for reproducibility WITHOUT surfacing gas
        // claims or misleading ppm in the instant/field operator results.
        root.put("source_curve_research_diagnostic_ledger",sources);
        root.put("source_curve_hypotheses",baseline.showGasHypotheses?
            sources:new JSONArray());
        // Descriptive comparison only; contradictory source curves never
        // suppress the rest of the research candidates or trigger robot action.
        JSONArray disagreements=new JSONArray();
        for(com.hazrob.tank.chemcore.v6.EvidenceConflictV6.Pair pair:
                data.sourceCurveDisagreements.allComparablePairs){
            JSONObject row=new JSONObject();
            row.put("chemical_reference_id",pair.chemicalReferenceId);
            row.put("curve_a",pair.sourceCurveA);
            row.put("curve_b",pair.sourceCurveB);
            row.put("source_root_a",pair.rootA);
            row.put("source_root_b",pair.rootB);
            row.put("same_physical_sensor",pair.samePhysicalSensor);
            row.put("different_reference_sources",pair.differentSourceRoots);
            row.put("absolute_log10_ppm_difference",numberOrNull(pair.log10PpmDifference));
            row.put("research_ppm_ratio",numberOrNull(pair.concentrationRatioMaxMin));
            row.put("interpretation","SOURCE_DISAGREEMENT_NOT_A_VALIDATED_GAS_VETO");
            disagreements.put(row);
        }
        root.put("source_curve_disagreements",baseline.showGasHypotheses?
            disagreements:new JSONArray());
        JSONArray fingerprints=new JSONArray();
        for(FingerprintEngineV6.Result space:data.fingerprintSpaces){
            JSONObject group=new JSONObject();
            group.put("feature_space",space.space);
            group.put("total_source_templates",space.sourceTemplatesInSpace);
            JSONArray hypotheses=new JSONArray();
            for(FingerprintEngineV6.Hypothesis item:space.hypotheses){
                JSONObject entry=new JSONObject();
                entry.put("fingerprint_id",item.fingerprintId);
                entry.put("source_root",item.sourceRoot);
                entry.put("label",item.label);
                entry.put("source_chemical_reference_id",item.chemicalId);
                entry.put("distance",numberOrNull(item.distance));
                entry.put("descriptive_similarity_index",numberOrNull(item.similarityIndex));
                entry.put("matched_feature_dimensions",item.overlapDimensions);
                entry.put("source_concentration_ppm_not_tank_measured",
                          numberOrNull(item.sourceConcentrationPpm));
                hypotheses.put(entry);
            }
            group.put("hypotheses",hypotheses);
            fingerprints.put(group);
        }
        root.put("fingerprint_feature_spaces",baseline.showGasHypotheses?
            fingerprints:new JSONArray());
        root.put("fingerprint_unfiltered_research_diagnostic_ledger",fingerprints);
        JSONArray ledger=new JSONArray();
        for(ChemCoreTankV6.ChemicalEvidence e:data.chemicalEvidence){
            JSONObject chemical=new JSONObject();
            chemical.put("chemical_reference_id",e.chemicalId);
            chemical.put("physical_sensors",e.actualPhysicalSensorCount);
            chemical.put("source_roots",e.sourceRootCount);
            chemical.put("curve_hypotheses",e.sourceCurveCount);
            chemical.put("fingerprint_hypotheses",e.featurePrototypeCount);
            chemical.put("verified_identity",false);
            ledger.put(chemical);
        }
        root.put("chemical_unfiltered_research_diagnostic_ledger",ledger);
        root.put("chemical_evidence_ledger",baseline.showGasHypotheses?
            ledger:new JSONArray());
        return root;
    }
}
