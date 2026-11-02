package com.servoc.app.chemcore.nextintegration;

import android.content.Context;
import android.content.SharedPreferences;
import com.hazrob.tank.chemcore.next.*;
import com.servoc.app.chemcore.library.ChemCoreLibraryStore;
import com.servoc.app.data.ExperimentsDb;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.json.JSONObject;

/**
 * V8 P07.3. Explicit RESEARCH_ONLY entrypoint. Stable SHA is computed from the
 * installed payload bytes before freezing the telemetry. No Android UI, alarm,
 * legacy inference or output rendering is performed here.
 *
 * Designed for controlled worker-thread use after telemetry has been sealed.
 * P08–P12 will replace old live/UI/field renderers at their own tested gates.
 */
public final class NextEngineRouter {
  public static final String FLAG="v8_next_research_enabled";
  private NextEngineRouter(){}
  public static final class Outcome {
    public final NextTelemetryMapper.Frozen original;
    public final ChemCoreNext.Output next;
    public final NextDecisionStore.Receipt receipt;
    public final NextEngineRoutingPolicy.Selection selection;
    private Outcome(NextTelemetryMapper.Frozen frozen,ChemCoreNext.Output output,
                    NextDecisionStore.Receipt receipt,NextEngineRoutingPolicy.Selection route){
      original=frozen;next=output;this.receipt=receipt;selection=route;
    }
  }
  /**
   * Side-by-side V6 comparison is not routed through this method and never
   * inserted in the append-only Next store. Reject malformed device snapshots.
   */
  public static Outcome analyzeAndPersist(Context context,ExperimentsDb db,
       JSONObject sealedTelemetry,String sessionUid,String captureUid,long createdUtc,
       CoreContracts.Mode mode,
       List<ResearchCandidateGenerator.Material> catalog,
       List<ChemCoreNext.CurveBinding> verifiedCurves,
       List<ChemCoreNext.FingerprintBinding> verifiedFingerprints,
       List<ResearchFingerprintBundle.Bundle> sourceBundles,
       int displayLimit) throws org.json.JSONException {
    Objects.requireNonNull(context,"Android context");
    Objects.requireNonNull(db,"SQLite research store");
    if(sealedTelemetry==null||sessionUid==null||sessionUid.isBlank()||
       captureUid==null||captureUid.isBlank()||createdUtc<0||
       mode==null||catalog==null||verifiedCurves==null||
       verifiedFingerprints==null||sourceBundles==null||displayLimit<0)
       throw new IllegalArgumentException("complete real source context required");
    SharedPreferences prefs=context.getSharedPreferences("servoc",Context.MODE_PRIVATE);
    NextEngineRoutingPolicy.Selection route=NextEngineRoutingPolicy.choose(
      prefs.getBoolean(FLAG,true),false);
    NextEngineRoutingPolicy.requireNext(route);
    final String actualStableSha=verifiedInstalledStableSha(context);
    NextTelemetryMapper.Frozen frozen=NextAndroidAdapter.freeze(
      sealedTelemetry,captureUid,actualStableSha,mode);
    ChemCoreNext.Input research=new ChemCoreNext.Input(
      frozen.device,frozen.snapshot,frozen.snapshotSha256,
      catalog,verifiedCurves,verifiedFingerprints,sourceBundles,displayLimit);
    ChemCoreNext.Output result=ChemCoreNext.analyze(research);
    if(result.molecularIdentityConfirmed||result.ppmAuthorized||result.g3Qualified||
       result.safetyAlarmAuthorized)
      throw new IllegalStateException("Next research core attempted safety promotion");
    var persisted=new NextDecisionStore(db).append(
      sessionUid,createdUtc,frozen.snapshot,result.research);
    if(!persisted.snapshotSha256.equals(frozen.snapshotSha256)||
       !persisted.decisionUid.startsWith("NEXT-"))
      throw new IllegalStateException("immutable Next readback failed");
    return new Outcome(frozen,result,persisted,route);
  }
  /** Verify file bytes, not the untrusted SHA string stored in preferences alone. */
  public static String verifiedInstalledStableSha(Context context) {
    if(context==null)throw new IllegalArgumentException("context required");
    ChemCoreLibraryStore store=new ChemCoreLibraryStore(context.getApplicationContext());
    File payload=store.activePayload();
    String accepted=store.activeSha256();
    if(payload==null||!payload.isFile()||accepted==null||
       !accepted.matches("[0-9a-fA-F]{64}"))
      throw new IllegalStateException("NO_VERIFIED_STABLE_PAYLOAD");
    try(FileInputStream in=new FileInputStream(payload)){
      MessageDigest sha=MessageDigest.getInstance("SHA-256");
      byte[] buf=new byte[8192];int len;
      while((len=in.read(buf))!=-1)if(len>0)sha.update(buf,0,len);
      StringBuilder digest=new StringBuilder(64);
      for(byte b:sha.digest())digest.append(String.format(Locale.ROOT,"%02x",b&255));
      if(!digest.toString().equalsIgnoreCase(accepted))
        throw new IllegalStateException("STABLE_PAYLOAD_CONTENT_HASH_MISMATCH");
      return digest.toString();
    }catch(java.io.IOException e){
      throw new IllegalStateException("STABLE_PAYLOAD_IO_FAILURE",e);
    }catch(java.security.NoSuchAlgorithmException e){
      throw new IllegalStateException("SHA256_PROVIDER_UNAVAILABLE",e);
    }
  }
}
