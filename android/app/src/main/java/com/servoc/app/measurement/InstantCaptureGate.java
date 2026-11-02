package com.servoc.app.measurement;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Research-only fail-closed source gate for operator INSTANT captures.
 * Monotonic 60s warmup + at least 60s sampling, 10 distinct firmware cycles,
 * and >=58s observed source span. No inferred gas identity or safety claim.
 * The relay is operator-owned and must already be confirmed ON.
 *
 * This is an in-process guard; it is NOT the field session durable controller
 * and must not be used as evidence of Android process-death recovery.
 */
public final class InstantCaptureGate {
    public static final long WARMUP_MS=60_000L, SAMPLING_MS=60_000L;
    public static final long MIN_SOURCE_SPAN_MS=58_000L;
    public static final int MIN_DISTINCT_CYCLES=10;
    private final long beginMs;
    private final Set<Long> observedCycles=new HashSet<>();
    private final JSONArray rows=new JSONArray();
    private long firstSourceMs=-1L,lastSourceMs=-1L;
    private String failure="";

    public InstantCaptureGate(long nowMs,JSONObject initial){
        if(nowMs<0||initial==null||initial.optJSONObject("mq")==null)
            throw new IllegalArgumentException("MQ source unavailable");
        if(!relayConfirmed(initial))
            throw new IllegalArgumentException("شغّل ريلاي MQ وتأكد من استجابة ESP32 أولاً");
        beginMs=nowMs;
    }
    private static boolean relayConfirmed(JSONObject data){
        JSONObject relay=data==null?null:data.optJSONObject("relay");
        return relay!=null&&relay.optBoolean("on",relay.optInt("mask",0)!=0);
    }
    public synchronized void observe(long nowMs,JSONObject source){
        if(!failure.isEmpty())return;
        if(nowMs<beginMs){failure="MONOTONIC_CLOCK_ROLLBACK";return;}
        if(source==null||source.optJSONObject("mq")==null){
            failure="MQ_TELEMETRY_DISCONNECTED";return;
        }
        if(!relayConfirmed(source)){failure="MQ_RELAY_NOT_CONFIRMED_ON";return;}
        long elapsed=nowMs-beginMs;
        if(elapsed<WARMUP_MS)return;
        JSONObject mq=source.optJSONObject("mq");
        Set<Long> fresh=new HashSet<>();
        Iterator<String> keys=mq.keys();
        while(keys.hasNext()){
            JSONObject q=mq.optJSONObject(keys.next());
            if(q==null||!q.has("scan_cycle"))continue;
            if(!q.optBoolean("ok",q.optBoolean("valid",false)))continue;
            long cycle=q.optLong("scan_cycle",-1L);
            if(cycle>=0&&!observedCycles.contains(cycle))fresh.add(cycle);
        }
        if(fresh.isEmpty())return; // repeated /diag frame is not a new sample
        observedCycles.addAll(fresh);
        if(firstSourceMs<0)firstSourceMs=nowMs;
        lastSourceMs=nowMs;
        try{
            JSONObject row=new JSONObject();
            row.put("t_monotonic_from_start_ms",elapsed);
            row.put("scan_cycles_observed",new JSONArray(fresh));
            row.put("mq_original",new JSONObject(mq.toString()));
            rows.put(row);
        }catch(JSONException ex){
            failure="SOURCE_SNAPSHOT_SERIALIZATION_FAILED";
        }
    }
    public synchronized boolean hasFailure(){return !failure.isEmpty();}
    public synchronized String failureReason(){return failure;}
    public synchronized boolean isReady(long nowMs){
        return failure.isEmpty()&&nowMs>=beginMs+WARMUP_MS+SAMPLING_MS
            &&observedCycles.size()>=MIN_DISTINCT_CYCLES
            &&firstSourceMs>=beginMs+WARMUP_MS
            &&lastSourceMs-firstSourceMs>=MIN_SOURCE_SPAN_MS;
    }
    public synchronized int progressPermille(long nowMs){
        long elapsed=Math.max(0,Math.min(WARMUP_MS+SAMPLING_MS,nowMs-beginMs));
        return (int)(1000L*elapsed/(WARMUP_MS+SAMPLING_MS));
    }
    public synchronized String readinessMessage(long nowMs){
        if(!failure.isEmpty())return "فشل المصدر: "+failure;
        long elapsed=Math.max(0,nowMs-beginMs);
        if(elapsed<WARMUP_MS)
            return "التسخين • متبقٍّ "+((WARMUP_MS-elapsed+999)/1000)+" ثانية";
        if(elapsed<WARMUP_MS+SAMPLING_MS)
            return "تسجيل MQ الفعلي • متبقٍّ "+
                ((WARMUP_MS+SAMPLING_MS-elapsed+999)/1000)+
                " ثانية • دورات مستقلة "+observedCycles.size();
        if(isReady(nowMs))return "التسجيل مكتمل • "+observedCycles.size()+
            " دورة مصدرية • اضغط إنهاء وتحليل";
        return "غير كافٍ للاعتماد • "+observedCycles.size()+
            "/10 دورات، انتشار "+
            (firstSourceMs<0?0:Math.max(0,lastSourceMs-firstSourceMs)/1000)+
            "/58 ثانية • تابع التسجيل";
    }
    public synchronized JSONObject verifiedEvidence(long nowMs)throws JSONException {
        if(!isReady(nowMs))
            throw new IllegalStateException("source gate NOT READY: "+readinessMessage(nowMs));
        JSONObject evidence=new JSONObject();
        evidence.put("schema","hazrob.tank.v8.instant_source_capture/1");
        evidence.put("source","ESP32_MQ_DIAGNOSTICS_DISTINCT_SCAN_CYCLES");
        evidence.put("warmup_monotonic_ms",WARMUP_MS);
        evidence.put("capture_elapsed_monotonic_ms",nowMs-(beginMs+WARMUP_MS));
        evidence.put("first_sample_offset_ms",firstSourceMs-beginMs);
        evidence.put("last_sample_offset_ms",lastSourceMs-beginMs);
        evidence.put("source_observed_span_ms",lastSourceMs-firstSourceMs);
        evidence.put("distinct_scan_cycles",observedCycles.size());
        evidence.put("has_validated_gas_identity",false);
        evidence.put("source_rows",new JSONArray(rows.toString()));
        return evidence;
    }
}
