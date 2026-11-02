package com.servoc.app.chemcore.nextintegration;

import com.hazrob.tank.chemcore.next.*;
import java.util.Objects;
import org.json.*;

/**
 * V8 P07.4 one research-only JSON contract for operator screens and ALL
 * PDF/XLSX exporters. This is NOT the old v6 source_curve_hypotheses order.
 * Rendering cannot rerun ChemCore, change sorting, populate 144 legacy UIDs,
 * infer ppm, or promote unqualified GHS pictograms into detection evidence.
 */
public final class NextDecisionEnvelope {
  public static final String SCHEMA=NextDecisionPresentation.SCHEMA;
  private NextDecisionEnvelope(){}
  public static JSONObject fromReceipt(NextDecisionStore.Receipt receipt) throws JSONException {
    Objects.requireNonNull(receipt,"persisted Next receipt required");
    var frozen=receipt.sourceSnapshot;
    var decision=receipt.originalDecision;
    var projection=NextDecisionPresentation.project(frozen,decision);
    if(!receipt.snapshotSha256.equals(projection.snapshotSha256)||
       !receipt.snapshotUid.equals(projection.snapshotUid)||
       !receipt.engineVersion.equals(projection.engineVersion)||
       !receipt.status.equals(projection.status))
      throw new IllegalArgumentException("persisted decision receipt mismatch");
    JSONObject out=new JSONObject();
    out.put("schema",SCHEMA);
    out.put("decision_uid",receipt.decisionUid);
    out.put("session_id",receipt.sessionId);
    out.put("snapshot_uid",projection.snapshotUid);
    out.put("snapshot_sha256",projection.snapshotSha256);
    out.put("stable_manifest_sha256",projection.stableSha256);
    out.put("model_version",projection.engineVersion);
    out.put("decision_status",projection.status.name());
    out.put("captured_utc_ms",receipt.createdUtcMs);
    out.put("candidate_count",projection.totalEligible);
    JSONArray ordered=new JSONArray();
    for(var c:projection.orderedCandidates){
      JSONObject candidate=new JSONObject();
      candidate.put("rank",c.ordinal);
      candidate.put("chemical_uid",c.canonicalUid);
      candidate.put("evidence_summary",c.explanation);
      JSONArray roots=new JSONArray();
      for(String sourceRoot:c.evidenceRoots)roots.put(sourceRoot);
      candidate.put("source_roots",roots);
      candidate.put("research_only",true);
      candidate.put("reported_identity","UNKNOWN");
      candidate.put("ppm",JSONObject.NULL);
      ordered.put(candidate);
    }
    out.put("candidates",ordered);
    JSONArray reason=new JSONArray();
    for(String x:projection.problems)reason.put(x);
    out.put("problems",reason);
    out.put("reported_identity","UNKNOWN");
    out.put("open_set_status","UNKNOWN_IDENTITY_NOT_PHYSICALLY_VERIFIED");
    out.put("research_only",true);
    out.put("ppm_reportable",false);
    out.put("physical_g3_validated",false);
    out.put("gas_identity_confirmed",false);
    out.put("occupational_gas_alarm_authorized",false);
    out.put("ghs_is_detection_evidence",false);
    out.put("legacy_v6_fallback_used",false);
    out.put("source_curve_hypotheses_are_candidate_order",false);
    return out;
  }
  public static void assertParity(JSONObject screen,JSONObject exporter){
    if(screen==null||exporter==null)throw new IllegalArgumentException("two views required");
    for(String key:new String[]{"decision_uid","session_id","snapshot_uid",
      "snapshot_sha256","stable_manifest_sha256","model_version",
      "decision_status","candidate_count"}) {
      if(!Objects.equals(screen.opt(key),exporter.opt(key)))
        throw new IllegalStateException("Next source of truth changed in "+key);
    }
    if(!Objects.equals(screen.optJSONArray("candidates").toString(),
       exporter.optJSONArray("candidates").toString()))
      throw new IllegalStateException("Next ranked UIDs/order/evidence changed between views");
  }
}
