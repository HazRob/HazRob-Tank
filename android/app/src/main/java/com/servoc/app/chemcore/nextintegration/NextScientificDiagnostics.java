package com.servoc.app.chemcore.nextintegration;

import com.hazrob.tank.chemcore.v6.SourceCatalog;
import com.hazrob.tank.chemcore.next.CoreContracts;
import com.hazrob.tank.chemcore.next.ChemCoreNext;
import com.servoc.app.chemical.ChemicalRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

/** Displays source-derived numerical fits without promoting gas identity or alarms.
 * The device clean-air R0 need not equal a research curve's source R0:
 * all finite results are explicitly conditional on that unverified transfer.
 */
public final class NextScientificDiagnostics {
    private NextScientificDiagnostics() {}

    /**
     * Historical two-argument entry point deliberately cannot infer a trusted
     * calibration denominator from unverified firmware JSON. Its source rows
     * are retained as NOT_EVALUABLE until a frozen Next snapshot is supplied.
     */
    @Deprecated
    public static JSONObject compute(JSONObject original,ChemicalRepository master)
        throws org.json.JSONException {
        return compute(original,master,null);
    }

    /** Research-only source equations evaluated against the SAME frozen,
     * device-bound clean-air reference used by the persisted Next decision.
     * Device R_air is not relabelled as a source/datasheet calibration R0.
     */
    public static JSONObject compute(JSONObject original,ChemicalRepository master,
            CoreContracts.Snapshot frozen) throws org.json.JSONException {
        if(original==null||master==null)
            throw new IllegalArgumentException("original data and unified catalog required");
        if(frozen!=null&&!frozen.deviceUid.equals(original.optString("device_id","").trim()))
            throw new IllegalArgumentException("SCIENTIFIC_DEVICE_ID_SNAPSHOT_MISMATCH");
        Map<String,CoreContracts.Reading> linked=new HashMap<>();
        if(frozen!=null)for(CoreContracts.Reading reading:frozen.readings){
            String uid=reading.sensorUid;
            int separator=uid.lastIndexOf(':');
            if(separator<0)throw new IllegalArgumentException("unbound MQ channel UID");
            linked.put(uid.substring(separator+1).toLowerCase(Locale.ROOT),reading);
        }
        JSONArray curves=new JSONArray();
        int computed=0,outside=0,unavailable=0;
        for(SourceCatalog.Curve curve:SourceCatalog.allCurves()){
            JSONObject row=new JSONObject();
            row.put("source_curve_id",curve.id());
            row.put("source_root",curve.sourceRoot());
            row.put("source_class",curve.id().startsWith("MQDATA43-")?
                "RESEARCH_DIGITIZED_MQ_REFERENCE":"RESEARCH_ALTERNATIVE_DIGITIZED");
            row.put("source_equation","Rs/R0 = a * (ppm/scale)^b");
            row.put("algorithm","LOG_INVERSE_POWER_LAW");
            row.put("sensor_model",curve.sensorType);
            row.put("gas_or_class",curve.label);
            row.put("source_legacy_id",curve.legacyChemicalId);
            ChemicalRepository.Chemical identity=curve.legacyChemicalId>=0 &&
                curve.legacyChemicalId<Integer.MAX_VALUE ?
                master.byLegacyId((int)curve.legacyChemicalId):null;
            row.put("chemical_uid",identity==null?JSONObject.NULL:identity.uid);
            row.put("a",curve.a);
            row.put("b",curve.b);
            row.put("reference_scale_ppm",curve.outputScale);
            row.put("source_scale_primary_verified",false);
            row.put("source_scale_origin",curve.id().startsWith("MQDATA43-")?
                "V6_ASSUMED_SCALE_SOURCE_ORIGINAL_UNKNOWN":
                "LITERATURE_DIGITIZED_OR_ALTERNATIVE_UNVERIFIED");
            row.put("reference_min_ppm",curve.minSourcePpm);
            row.put("reference_max_ppm",curve.maxSourcePpm);
            row.put("source_domain_primary_verified",false);
            row.put("estimated_ppm",JSONObject.NULL);
            row.put("physical_g3_validated",false);
            row.put("safety_alarm_authorized",false);
            row.put("source_r0_equivalence_verified",false);
            // Never use arbitrary firmware mq.r0, even when the firmware
            // reports ok=true. It may carry a manufacturer-factor or legacy
            // definition incompatible with the research source equation.
            String model=curve.sensorType.toLowerCase(Locale.ROOT);
            CoreContracts.Reading reading=linked.get(model);
            double rs=reading==null||reading.rsKohm==null?
                Double.NaN:reading.rsKohm;
            double r0=reading==null||reading.rAirKohm==null?
                Double.NaN:reading.rAirKohm;
            boolean valid=reading!=null&&frozen!=null&&frozen.mqRelayOn&&
                reading.quality==CoreContracts.SampleQuality.VALID&&
                reading.r0Definition==CoreContracts.CalibrationReference.AIR_RS_MEAN;
            row.put("r0_definition",valid?
                "DEVICE_CLEAN_AIR_RS_MEAN":"MISSING_VERIFIED_DEVICE_AIR_REFERENCE");
            row.put("reference_denominator","DEVICE_AIR_RS_MEAN_NOT_SOURCE_R0");
            row.put("reference_device_bound",valid);
            if(reading!=null)row.put("device_sensor_quality",reading.quality.name());
            if(Double.isFinite(rs)&&rs>0)row.put("rs_kohm",rs);
            if(Double.isFinite(r0)&&r0>0) {
                row.put("device_air_reference_kohm",r0);
                row.put("device_r0_kohm",r0);
            }
            if(!valid || !(Double.isFinite(rs)&&rs>0) ||
                !(Double.isFinite(r0)&&r0>0)){
                row.put("status","NOT_EVALUABLE_NO_MATCHED_FROZEN_AIR_REFERENCE");
                unavailable++;
            }else if(!curve.numericalDefinitionValid()){
                row.put("status","NOT_EVALUABLE_INVALID_SOURCE_EQUATION");
                unavailable++;
            }else{
                double ratio=rs/r0;
                double logPpm=Math.log10(curve.outputScale)+
                    (Math.log10(ratio)-Math.log10(curve.a))/curve.b;
                double ppm=Math.pow(10.0,logPpm);
                row.put("observed_rs_r0",ratio);
                if(Double.isFinite(ppm)&&ppm>0){
                    boolean extra=ppm<curve.minSourcePpm||ppm>curve.maxSourcePpm;
                    row.put("estimated_ppm",ppm);
                    row.put("log10_estimated_ppm",logPpm);
                    row.put("extrapolated",extra);
                    row.put("status",extra?"RESEARCH_EXTRAPOLATED":"RESEARCH_WITHIN_SOURCE_DOMAIN");
                    row.put("caveat","DEVICE_CLEAN_AIR_R0_NOT_PROVEN_EQUAL_TO_CURVE_REFERENCE_R0");
                    computed++;if(extra)outside++;
                }else{
                    row.put("status","NOT_EVALUABLE_NONFINITE_INVERSE");
                    unavailable++;
                }
            }
            curves.put(row);
        }
        JSONObject result=new JSONObject();
        result.put("schema","hazrob.tank.v8.scientific_source_diagnostics/1");
        result.put("all_reference_models",curves);
        result.put("source_snapshot_uid",frozen==null?"UNBOUND":frozen.uid);
        result.put("source_snapshot_sha256",frozen==null?JSONObject.NULL:
            ChemCoreNext.snapshotHash(frozen));
        result.put("device_air_reference_is_not_source_r0",true);
        result.put("firmware_raw_r0_never_used_as_verified_reference",true);
        result.put("source_model_count",curves.length());
        result.put("computed_ppm_count",computed);
        result.put("extrapolated_count",outside);
        result.put("not_evaluable_count",unavailable);
        result.put("source_only_estimate_not_a_verified_concentration",true);
        result.put("non_manufacturer_research_not_rebranded_as_manufacturer",true);
        return result;
    }
}
