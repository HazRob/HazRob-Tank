package com.servoc.app.chemcore.nextintegration;

import com.hazrob.tank.chemcore.v6.SourceCatalog;
import com.hazrob.tank.chemcore.next.CoreContracts;
import com.hazrob.tank.chemcore.next.ChemCoreNext;
import com.hazrob.tank.chemcore.next.R0ReferenceMath;
import com.hazrob.tank.chemcore.next.MQSensorsLibExampleCatalog;
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
     * Explain why a specific source curve cannot be calculated using the
     * SAME frozen evidence as Next. Return a reason, never an invented Rs/R0.
     */
    private static String missingSourceReason(CoreContracts.Snapshot frozen,
                                              CoreContracts.Reading reading){
        if(frozen==null)return "MISSING_FROZEN_SNAPSHOT";
        if(!frozen.mqRelayOn)return "MQ_RELAY_OFF_IN_FROZEN_SNAPSHOT";
        if(reading==null)return "MISSING_INSTALLED_MQ_SAMPLE";
        if(reading.quality!=CoreContracts.SampleQuality.VALID)
            return "SENSOR_QUALITY_"+reading.quality.name();
        if(reading.r0Definition!=CoreContracts.CalibrationReference.AIR_RS_MEAN)
            return "WRONG_R0_DEFINITION";
        if(reading.rAirKohm==null||!Double.isFinite(reading.rAirKohm)||
           reading.rAirKohm<=0)return "MISSING_AIR_RS_MEAN";
        if(reading.rsKohm==null||!Double.isFinite(reading.rsKohm)||
           reading.rsKohm<=0)return "MISSING_POSITIVE_RS";
        return "MISSING_POSITIVE_RS";
    }

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
                row.put("failure_reason_code",missingSourceReason(frozen,reading));
                unavailable++;
            }else if(!curve.numericalDefinitionValid()){
                row.put("status","NOT_EVALUABLE_INVALID_SOURCE_EQUATION");
                row.put("failure_reason_code","INVALID_SOURCE_EQUATION");
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
                    row.put("failure_reason_code","NONFINITE_SOURCE_RESULT");
                    unavailable++;
                }
            }
            curves.put(row);
        }
        // P11 dual reference: preserve Tank R_air, derive the ORIGINAL
        // MQSensorsLib example R0 only as a per-curve mathematical view.
        // These 13 models are never merged with MQDataScience equations:
        // ppm = A*(K*Rs/R_air)^B, whereas MQDataScience reverses a DIFFERENT
        // ratio-vs-ppm relation whose K was not established by its source.
        for(MQSensorsLibExampleCatalog.Model example:
                MQSensorsLibExampleCatalog.installedModels()){
            JSONObject row=new JSONObject();
            row.put("source_curve_id",example.id);
            row.put("source_root",example.sourceRoot);
            row.put("source_class","MQSENSORSLIB_PINNED_LIBRARY_EXAMPLE");
            row.put("source_repository","miguel5612/MQSensorsLib");
            row.put("source_commit",MQSensorsLibExampleCatalog.COMMIT);
            row.put("source_blob",example.sourceBlob);
            row.put("source_file",example.sourceFile);
            row.put("source_line",example.sourceLine);
            row.put("sensor_model",example.model);
            row.put("gas_or_class",example.label);
            row.put("source_equation","ppm = A * (Rs / R0_source)^B");
            row.put("algorithm","SOURCE_EXAMPLE_DIRECT_POWER_LAW");
            row.put("a",example.a);row.put("b",example.b);
            row.put("source_example_clean_air_K",example.cleanAirK);
            row.put("dual_reference_equation",
                "R0_source=R_air/K; ppm=A*(K*Rs/R_air)^B");
            row.put("reference_denominator","DEVICE_AIR_RS_MEAN_NOT_SOURCE_R0");
            row.put("source_reference_definition",
                "MQSENSORSLIB_EXAMPLE_RS_CLEAN_AIR_OVER_K");
            row.put("estimated_ppm",JSONObject.NULL);
            row.put("chemical_uid",JSONObject.NULL);
            row.put("source_scale_primary_verified",JSONObject.NULL);
            row.put("source_equation_no_ppm_scale_parameter",true);
            row.put("source_domain_primary_verified",false);
            row.put("source_heater_phase_verified",false);
            row.put("source_r0_equivalence_verified",false);
            row.put("physical_g3_validated",false);
            row.put("safety_alarm_authorized",false);
            row.put("source_independent_gas_identity_evidence",false);
            row.put("source_example_lab_reference_validated_on_tank",false);
            row.put("source_co2_example_prints_400ppm_offset_not_applied",
                "CO2".equals(example.label));
            row.put("reference_min_ppm",JSONObject.NULL);
            row.put("reference_max_ppm",JSONObject.NULL);
            CoreContracts.Reading observed=linked.get(
                example.model.toLowerCase(Locale.ROOT));
            Double rs=observed==null?null:observed.rsKohm;
            Double air=observed==null?null:observed.rAirKohm;
            boolean eligible=observed!=null&&frozen!=null&&frozen.mqRelayOn&&
                observed.quality==CoreContracts.SampleQuality.VALID&&
                observed.r0Definition==CoreContracts.CalibrationReference.AIR_RS_MEAN;
            row.put("reference_device_bound",eligible);
            row.put("r0_definition",eligible?"DEVICE_CLEAN_AIR_RS_MEAN":
                "MISSING_VERIFIED_DEVICE_AIR_REFERENCE");
            if(observed!=null)row.put("device_sensor_quality",observed.quality.name());
            if(rs!=null&&Double.isFinite(rs)&&rs>0)row.put("rs_kohm",rs);
            if(air!=null&&Double.isFinite(air)&&air>0){
                row.put("device_air_reference_kohm",air);
                row.put("device_r0_kohm",air); // legacy UI alias; explicitly R_air
            }
            if(!eligible||rs==null||air==null){
                row.put("status","NOT_EVALUABLE_NO_MATCHED_FROZEN_AIR_REFERENCE");
                row.put("failure_reason_code",missingSourceReason(frozen,observed));
                unavailable++;
            }else{
                // The source example does NOT provide a Tank heater validation.
                // Give this SourceK an explicitly UNVERIFIED heater provenance;
                // the algebraic path never certifies physical phase matching.
                R0ReferenceMath.Reference device=new R0ReferenceMath.Reference(
                    observed.sensorUid,example.model,
                    "TANK_HEATER_PHASE_NOT_SOURCE_VERIFIED",
                    R0ReferenceMath.FIRMWARE_DEVICE_AIR,air);
                R0ReferenceMath.SourceK k=new R0ReferenceMath.SourceK(
                    example.sourceRoot+"@"+example.sourceBlob,example.model,
                    "MQSL_EXAMPLE_HEATER_PHASE_UNVERIFIED",
                    example.cleanAirK,false);
                R0ReferenceMath.Comparison calc=
                    R0ReferenceMath.researchTransform(device,rs,k);
                if(calc.status!=R0ReferenceMath.Status.SOURCE_K_RESEARCH_ONLY){
                    row.put("status","NOT_EVALUABLE_SOURCE_REFERENCE_TRANSFORM_"+
                        calc.status.name());
                    row.put("failure_reason_code","INVALID_SOURCE_REFERENCE_TRANSFORM");
                    unavailable++;
                }else{
                    row.put("source_r0_research_kohm",calc.sourceR0Kohm);
                    row.put("observed_rs_over_air",calc.rsOverAir);
                    row.put("observed_rs_r0",calc.rsOverSourceR0);
                    row.put("source_reference_transform_research_only",true);
                    double logPpm=Math.log10(example.a)+
                        example.b*Math.log10(calc.rsOverSourceR0);
                    double ppm=Math.pow(10.0,logPpm);
                    if(!Double.isFinite(ppm)||ppm<=0){
                        row.put("status","NOT_EVALUABLE_NONFINITE_DIRECT_POWER");
                        row.put("failure_reason_code","NONFINITE_SOURCE_RESULT");
                        unavailable++;
                    }else{
                        row.put("estimated_ppm",ppm);
                        row.put("log10_estimated_ppm",logPpm);
                        row.put("extrapolated",JSONObject.NULL);
                        row.put("status",
                            "RESEARCH_ONLY_SOURCE_DOMAIN_AND_HEATER_UNVERIFIED");
                        row.put("caveat",
                            "SOURCE_K_AND_HEATER_NOT_VALIDATED_ON_THIS_TANK; "+
                            "LIBRARY_EXAMPLE_NOT_A_CERTIFIED_CONCENTRATION");
                        computed++;
                    }
                }
            }
            curves.put(row);
        }
        JSONObject result=new JSONObject();
        result.put("schema","hazrob.tank.v8.scientific_source_diagnostics/2");
        result.put("all_reference_models",curves);
        result.put("source_snapshot_uid",frozen==null?"UNBOUND":frozen.uid);
        result.put("source_snapshot_sha256",frozen==null?JSONObject.NULL:
            ChemCoreNext.snapshotHash(frozen));
        result.put("device_air_reference_is_not_source_r0",true);
        result.put("firmware_raw_r0_never_used_as_verified_reference",true);
        result.put("source_model_count",curves.length());
        result.put("dual_reference_example_model_count",MQSensorsLibExampleCatalog.installedModels().size());
        result.put("r_air_preserved_no_r0_storage_mutation",true);
        result.put("source_k_heater_qualification_pending",true);
        result.put("computed_ppm_count",computed);
        result.put("extrapolated_count",outside);
        result.put("not_evaluable_count",unavailable);
        result.put("source_only_estimate_not_a_verified_concentration",true);
        result.put("non_manufacturer_research_not_rebranded_as_manufacturer",true);
        return result;
    }
}
