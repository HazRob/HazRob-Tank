package com.servoc.app.report;

import com.hazrob.tank.chemcore.next.CoreContracts;
import com.hazrob.tank.chemcore.next.MQSensorsLibExampleCatalog;
import com.hazrob.tank.chemcore.v6.SourceCatalog;
import com.servoc.app.chemcore.nextintegration.NextDecisionStore;
import com.servoc.app.data.ExperimentsDb;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * P11.3: read the STORED field dual-reference research ledger, not a live MQ
 * snapshot or a re-analysis triggered by the XLSX export.
 *
 * Every present ledger must match the immutable ChemCore Next source receipt
 * and its original 60s source archive digest; corrupt, mismatched or
 * physically over-promoted values fail closed. Historical Next sessions without
 * a P11 ledger remain exportable with an EXPLICIT absence marker.
 *
 * This is an Excel research appendix only: no effect on V7 PDF design,
 * candidate decision, alarm thresholds, R_air storage or field approval.
 */
final class V8NextDualReferenceXlsxLedger {
    static final String KEY_PREFIX="v8_field_next_dual_reference_";
    static final String SCHEMA="hazrob.tank.v8.field.next_dual_reference/1";
    static final String DIAGNOSTICS_SCHEMA="hazrob.tank.v8.scientific_source_diagnostics/2";
    final boolean available;
    final String status,sourceArchiveSha256;
    final JSONArray rows;
    final int computedCount;

    private V8NextDualReferenceXlsxLedger(boolean available,String state,
            String archive,JSONArray rows,int computed) {
        this.available=available;status=state;sourceArchiveSha256=archive;
        this.rows=rows;computedCount=computed;
    }

    static V8NextDualReferenceXlsxLedger load(ExperimentsDb db,
            String measurementId,NextDecisionStore.Receipt next) {
        if(db==null||measurementId==null||measurementId.isBlank()||next==null)
            throw new IllegalArgumentException("P11_FIELD_LEDGER_SOURCE_MISSING");
        if(!measurementId.equals(next.sessionId))
            throw new IllegalStateException("P11_FIELD_LEDGER_SESSION_MISMATCH");
        String saved=db.getMeta(KEY_PREFIX+measurementId,"");
        if(saved==null||saved.isBlank())
            return new V8NextDualReferenceXlsxLedger(false,
                "NOT_CAPTURED_PRE_P11_OR_RESEARCH_LEDGER_MISSING","",new JSONArray(),0);
        try {
            JSONObject envelope=new JSONObject(saved);
            require(SCHEMA.equals(envelope.optString("schema","")),
                "P11_FIELD_LEDGER_SCHEMA_MISMATCH");
            require(measurementId.equals(envelope.optString("measurement_session_id","")),
                "P11_FIELD_LEDGER_MEASUREMENT_ID_MISMATCH");
            require(next.decisionUid.equals(envelope.optString("next_decision_uid","")),
                "P11_FIELD_LEDGER_DECISION_UID_MISMATCH");
            require(Boolean.TRUE.equals(envelope.opt("research_only_no_gas_identity_or_safety_alarm")),
                "P11_FIELD_LEDGER_SAFETY_SCOPE_INVALID");
            String archive=envelope.optString("source_archive_sha256","");
            require(archive.matches("[0-9a-f]{64}"),"P11_FIELD_LEDGER_ARCHIVE_SHA_INVALID");
            require(next.snapshotUid.equals("FIELD-ARCHIVE-"+archive),
                "P11_FIELD_LEDGER_SOURCE_ARCHIVE_BINDING_MISMATCH");
            JSONObject scientific=envelope.optJSONObject("scientific_source_diagnostics");
            require(scientific!=null&&DIAGNOSTICS_SCHEMA.equals(scientific.optString("schema","")),
                "P11_FIELD_LEDGER_SCIENTIFIC_SCHEMA_MISMATCH");
            require(next.snapshotUid.equals(scientific.optString("source_snapshot_uid","")),
                "P11_FIELD_LEDGER_SNAPSHOT_UID_MISMATCH");
            require(next.snapshotSha256.equals(scientific.optString("source_snapshot_sha256","")),
                "P11_FIELD_LEDGER_SNAPSHOT_SHA_MISMATCH");
            require(Boolean.TRUE.equals(scientific.opt("r_air_preserved_no_r0_storage_mutation")),
                "P11_FIELD_LEDGER_R_AIR_MUTATION_CLAIM");
            require(Boolean.TRUE.equals(scientific.opt("source_k_heater_qualification_pending")),
                "P11_FIELD_LEDGER_SOURCE_K_GATE_MISSING");
            JSONArray models=scientific.optJSONArray("all_reference_models");
            require(models!=null&&models.length()==scientific.optInt("source_model_count",-1)
                    && models.length()==62,
                "P11_FIELD_LEDGER_MODEL_COUNT_INVALID");
            Map<String,CoreContracts.Reading> actual=new HashMap<>();
            for(CoreContracts.Reading reading:next.sourceSnapshot.readings){
                String uid=reading.sensorUid;
                int n=uid.lastIndexOf(':');
                if(n>=0)actual.put(uid.substring(n+1).toUpperCase(java.util.Locale.ROOT),reading);
            }
            Map<String,MQSensorsLibExampleCatalog.Model> pinned=new HashMap<>();
            for(MQSensorsLibExampleCatalog.Model model:
                    MQSensorsLibExampleCatalog.installedModels())pinned.put(model.id,model);
            Map<String,SourceCatalog.Curve> original49=new HashMap<>();
            for(SourceCatalog.Curve curve:SourceCatalog.allCurves())
                original49.put(curve.id(),curve);
            require(original49.size()==49,
                "P11_FIELD_LEDGER_SOURCE_CATALOG_COUNT_INVALID");
            Set<String> sourceIds=new HashSet<>();
            int measured=0,seenExamples=0,seenHistorical=0;
            for(int i=0;i<models.length();i++){
                JSONObject row=models.optJSONObject(i);
                require(row!=null,"P11_FIELD_LEDGER_SOURCE_ROW_NOT_JSON");
                String curveId=row.optString("source_curve_id","");
                require(!curveId.isBlank()&&sourceIds.add(curveId),
                    "P11_FIELD_LEDGER_DUPLICATE_CURVE");
                require(!row.optBoolean("physical_g3_validated",true)
                        && !row.optBoolean("safety_alarm_authorized",true),
                    "P11_FIELD_LEDGER_UNSUPPORTED_PHYSICAL_QUALIFICATION");
                boolean computed=!row.isNull("estimated_ppm")
                    && row.has("estimated_ppm");
                if(computed){
                    require(finitePositive(row,"estimated_ppm"),
                        "P11_FIELD_LEDGER_INVALID_RESEARCH_PPM");
                    measured++;
                }
                if(curveId.startsWith("MQSL-EX-")){
                    MQSensorsLibExampleCatalog.Model model=pinned.get(curveId);
                    require(model!=null,"P11_FIELD_LEDGER_MQSENSORSLIB_UNKNOWN_MODEL");
                    seenExamples++;
                    require(model.model.equals(row.optString("sensor_model","")) &&
                            model.sourceRoot.equals(row.optString("source_root","")) &&
                            model.sourceBlob.equals(row.optString("source_blob","")) &&
                            near(model.a,row.optDouble("a",Double.NaN)) &&
                            near(model.b,row.optDouble("b",Double.NaN)) &&
                            near(model.cleanAirK,row.optDouble("source_example_clean_air_K",Double.NaN)),
                        "P11_FIELD_LEDGER_PINNED_SOURCE_MISMATCH");
                    require(row.isNull("chemical_uid") &&
                            !row.optBoolean("source_heater_phase_verified",true) &&
                            !row.optBoolean("source_r0_equivalence_verified",true) &&
                            !row.optBoolean("source_example_lab_reference_validated_on_tank",true),
                        "P11_FIELD_LEDGER_MQSENSORSLIB_IDENTITY_OR_R0_PROMOTION");
                    CoreContracts.Reading reading=actual.get(model.model);
                    if(computed){
                        require(reading!=null&&reading.quality==CoreContracts.SampleQuality.VALID
                                &&reading.r0Definition==CoreContracts.CalibrationReference.AIR_RS_MEAN
                                &&next.sourceSnapshot.mqRelayOn
                                &&reading.rsKohm!=null&&reading.rAirKohm!=null,
                            "P11_FIELD_LEDGER_PPM_WITHOUT_FROZEN_SENSOR");
                        double rs=reading.rsKohm,air=reading.rAirKohm,k=model.cleanAirK;
                        double expected=model.a*Math.pow(k*rs/air,model.b);
                        require(near(rs,row.optDouble("rs_kohm",Double.NaN)) &&
                                near(air,row.optDouble("device_air_reference_kohm",Double.NaN)) &&
                                near(air/k,row.optDouble("source_r0_research_kohm",Double.NaN)) &&
                                near(rs/air,row.optDouble("observed_rs_over_air",Double.NaN)) &&
                                near(k*rs/air,row.optDouble("observed_rs_r0",Double.NaN)) &&
                                near(expected,row.optDouble("estimated_ppm",Double.NaN)),
                            "P11_FIELD_LEDGER_SOURCE_MATH_OR_SNAPSHOT_MISMATCH");
                    }
                } else {
                    SourceCatalog.Curve curve=original49.get(curveId);
                    require(curve!=null && curve.id().equals(curveId) &&
                            curve.sourceRoot().equals(row.optString("source_root","")) &&
                            curve.sensorType.equalsIgnoreCase(row.optString("sensor_model","")) &&
                            near(curve.a,row.optDouble("a",Double.NaN)) &&
                            near(curve.b,row.optDouble("b",Double.NaN)) &&
                            near(curve.outputScale,row.optDouble("reference_scale_ppm",Double.NaN)),
                        "P11_FIELD_LEDGER_HISTORICAL_SOURCE_COEFFICIENT_MISMATCH");
                    seenHistorical++;
                    require(!row.optBoolean("source_scale_primary_verified",true) &&
                            !row.optBoolean("source_domain_primary_verified",true),
                        "P11_FIELD_LEDGER_UNVERIFIED_HISTORICAL_SOURCE_PROMOTION");
                    if(computed){
                        CoreContracts.Reading reading=actual.get(
                            curve.sensorType.toUpperCase(java.util.Locale.ROOT));
                        require(reading!=null&&reading.quality==CoreContracts.SampleQuality.VALID
                                &&reading.r0Definition==CoreContracts.CalibrationReference.AIR_RS_MEAN
                                &&next.sourceSnapshot.mqRelayOn &&
                                reading.rsKohm!=null&&reading.rAirKohm!=null,
                            "P11_FIELD_LEDGER_HISTORICAL_PPM_WITHOUT_FROZEN_SENSOR");
                        double rs=reading.rsKohm,air=reading.rAirKohm;
                        double logPpm=Math.log10(curve.outputScale)+
                            (Math.log10(rs/air)-Math.log10(curve.a))/curve.b;
                        double expected=Math.pow(10.0,logPpm);
                        require(near(rs,row.optDouble("rs_kohm",Double.NaN)) &&
                                near(air,row.optDouble("device_air_reference_kohm",Double.NaN)) &&
                                near(rs/air,row.optDouble("observed_rs_r0",Double.NaN)) &&
                                near(expected,row.optDouble("estimated_ppm",Double.NaN)),
                            "P11_FIELD_LEDGER_HISTORICAL_SOURCE_MATH_MISMATCH");
                    }
                }
            }
            require(seenHistorical==49,
                "P11_FIELD_LEDGER_HISTORICAL_MODEL_COUNT_INVALID");
            require(seenExamples==13,"P11_FIELD_LEDGER_INSTALLED_EXAMPLE_COUNT_INVALID");
            require(measured==scientific.optInt("computed_ppm_count",-1),
                "P11_FIELD_LEDGER_COMPUTED_COUNT_MISMATCH");
            return new V8NextDualReferenceXlsxLedger(true,
                "SOURCE_BOUND_RESEARCH_ONLY_G3_NOT_QUALIFIED",archive,models,measured);
        } catch(JSONException malformed){
            throw new IllegalStateException("P11_FIELD_LEDGER_CORRUPT_JSON",malformed);
        }
    }

    private static void require(boolean condition,String reason){
        if(!condition)throw new IllegalStateException(reason);
    }
    private static boolean finitePositive(JSONObject q,String key){
        double n=q.optDouble(key,Double.NaN);
        return Double.isFinite(n)&&n>0;
    }
    private static boolean near(double expected,double recorded){
        return Double.isFinite(expected)&&Double.isFinite(recorded)&&
            Math.abs(expected-recorded)<=1e-9*Math.max(1.,Math.abs(expected));
    }
}
