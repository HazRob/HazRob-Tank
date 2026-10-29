package com.hazrob.tank.v6integration;

import com.hazrob.tank.chemcore.v6.ChemCoreTankV6;
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
            double rs=value.optDouble("rs",Double.NaN);
            double r0=value.optDouble("r0",Double.NaN);
            // No eligibility threshold except a mathematically invalid Rs:
            // independent V6 Engine retains valid raw Rs even without R0.
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
        root.put("ratio_ready_mq_channels",data.responseCurves.ratioReadyChannels);
        root.put("numerically_rejected",data.responseCurves.rejectedNumericalChannels);
        root.put("physical_calibration_proven",false);
        root.put("reportable_ppm",false);
        root.put("food_models_included",false);
        root.put("interpretation","EXPLORATORY_REFERENCE_FITS_NOT_CERTIFIED_GAS_IDENTITY");
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
        root.put("source_curve_hypotheses",sources);
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
        root.put("fingerprint_feature_spaces",fingerprints);
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
        root.put("chemical_evidence_ledger",ledger);
        return root;
    }
}
