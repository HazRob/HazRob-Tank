package com.servoc.app.measurement;

import android.content.ContentValues;
import android.content.Context;
import android.os.SystemClock;
import com.servoc.app.chemcore.ChemCoreSchema;
import com.servoc.app.chemcore.CompletedSessionChemCoreAnalyzer;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.hardware.HazRobHardwareProfile;
import com.servoc.app.field.FieldSessionRepository;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Automatically records normalized telemetry across ESP32 MQ cycles and controlled ChemCore captures. */
public final class TelemetryRecorder {
    private final ExperimentsDb db;private final Context appContext;private final FieldSessionRepository fieldSessions;private String sessionId,lastCompletedSessionId="";private long startedElapsed,startedUtcMs,lastSampleElapsed,sampleSeq;private boolean chemCoreCaptureSession=false,fieldCaptureSession=false;
    public TelemetryRecorder(ExperimentsDb db){this(null,db);}
    public TelemetryRecorder(Context context,ExperimentsDb db){
        if(db==null)throw new IllegalArgumentException("database required");
        this.appContext=context==null?null:context.getApplicationContext();
        this.db=db;this.fieldSessions=new FieldSessionRepository(db);
        ChemCoreSchema.ensure(db.getWritableDatabase());
        lastCompletedSessionId=db.getMeta("last_measurement_session_id","");
    }

    public synchronized void accept(JSONObject snapshot,int left,int right,boolean developer){
        if(snapshot==null)return;JSONObject m=snapshot.optJSONObject("measurement");String state=m==null?"idle":m.optString("state","idle");long now=SystemClock.elapsedRealtime();
        boolean firmwareActive="warmup".equals(state)||"measuring".equals(state);boolean forced=MeasurementSessionRegistry.chemCoreCaptureRequested();
        boolean fieldForced=MeasurementSessionRegistry.fieldCaptureRequested();
        boolean activeState=firmwareActive||forced||fieldForced;
        if(sessionId==null&&activeState){
            boolean fromDeveloper=MeasurementSessionRegistry.consumeDeveloperMqCycle();
            start(fieldForced?false:forced?MeasurementSessionRegistry.chemCoreDeveloper():(fromDeveloper||developer),
                now,forced,fieldForced);
        }

        boolean terminalFirmware="ready".equals(state)||"error".equals(state)||"idle".equals(state);
        long samplePeriod=(chemCoreCaptureSession||fieldCaptureSession)?200:250;
        if(sessionId!=null&&(now-lastSampleElapsed>=samplePeriod||(!chemCoreCaptureSession&&!fieldCaptureSession&&terminalFirmware))){sample(snapshot,left,right,now);lastSampleElapsed=now;}

        String forcedStop=MeasurementSessionRegistry.chemCoreStopReason();
        String fieldStop=MeasurementSessionRegistry.fieldStopReason();
        if(sessionId!=null&&fieldCaptureSession&&!fieldStop.isEmpty())finish(snapshot,fieldStop);
        else if(sessionId!=null&&chemCoreCaptureSession&&!forcedStop.isEmpty())finish(snapshot,forcedStop);
        else if(sessionId!=null&&!chemCoreCaptureSession&&!fieldCaptureSession&&terminalFirmware)finish(snapshot,state);
    }

    public synchronized String activeSessionId(){return sessionId==null?"":sessionId;}
    public synchronized String lastCompletedSessionId(){return lastCompletedSessionId==null?"":lastCompletedSessionId;}

    private void start(boolean developer,long now,boolean chemCore,boolean fieldControlled){
        long utc=System.currentTimeMillis();chemCoreCaptureSession=chemCore;fieldCaptureSession=fieldControlled;sessionId=(developer?"DEV":"FIELD")+(fieldControlled?"-CYCLE-":chemCore?"-CHEMCORE-":"-")+utc+"-"+java.util.UUID.randomUUID().toString().substring(0,8);startedElapsed=lastSampleElapsed=now;startedUtcMs=utc;sampleSeq=0;
        ContentValues v=new ContentValues();v.put("session_id",sessionId);v.put("started_ts",isoUtc(utc));v.put("kind",fieldControlled?"field_60s_cycle":chemCore?"developer_chemcore_controlled_capture":(developer?"developer_mq_cycle":"instant_mq_cycle"));v.put("operator_role",developer?"DEVELOPER":"USER");
        try{JSONObject meta=new JSONObject();meta.put("chemcore_controlled_capture",chemCore);meta.put("field_60s_capture",fieldControlled);meta.put("sample_period_ms",(chemCore||fieldControlled)?200:250);meta.put("mq_scan_timing_capture",chemCore);meta.put("measurement_schema",CanonicalMeasurementNormalizer.SCHEMA);meta.put("started_utc_ms",utc);meta.put("started_monotonic_ms",now);meta.put("clock_contract","UTC_EPOCH_MS_PLUS_ANDROID_ELAPSED_REALTIME");v.put("analytics",meta.toString());}catch(Exception ignored){}
        // The session insert and its user-field link form one semantic operation.
        // Never silently produce an orphan record while the operator assumes the active
        // site's report will include the acquisition.
        try{
            db.getWritableDatabase().insertOrThrow("measurement_sessions",null,v);
            // Only explicitly armed FIELD cycles may link to a user session; instant
            // and scientific experiments are never linked automatically.
            if(fieldControlled){
                String owner=MeasurementSessionRegistry.fieldCaptureOwnerSessionId();
                if(owner.isEmpty()||!fieldSessions.linkMeasurement(owner,sessionId))
                    throw new IllegalStateException("field telemetry owner is missing or no longer active");
            }
        }catch(RuntimeException failure){
            try{db.getWritableDatabase().delete("measurement_sessions","session_id=?",
                    new String[]{sessionId});}catch(Exception ignored){}
            sessionId=null;startedElapsed=startedUtcMs=lastSampleElapsed=sampleSeq=0;
            chemCoreCaptureSession=false;fieldCaptureSession=false;
            if(fieldControlled)MeasurementSessionRegistry.clearFieldCapture();
            throw failure;
        }
        MeasurementSessionRegistry.started(sessionId);
    }

    private void sample(JSONObject snapshot,int left,int right,long now){
        long tMs=Math.max(0,now-startedElapsed);ContentValues v=new ContentValues();v.put("session_id",sessionId);v.put("t_ms",tMs);
        JSONObject e=snapshot.optJSONObject("env"),mq=snapshot.optJSONObject("mq");v.put("env_payload",e==null?"{}":e.toString());v.put("mq_payload",mq==null?"{}":mq.toString());
        JSONObject state=new JSONObject();
        try{
            JSONObject transport=snapshot.optJSONObject("_transport");long utc=transport==null?System.currentTimeMillis():transport.optLong("received_utc_ms",System.currentTimeMillis());
            long monotonic=transport==null?now:transport.optLong("received_monotonic_ms",now);
            state.put("sample_seq",++sampleSeq);state.put("sample_utc_ms",utc);state.put("sample_monotonic_ms",monotonic);state.put("session_t_ms",tMs);
            state.put("measurement_schema",snapshot.optString("measurement_schema",CanonicalMeasurementNormalizer.SCHEMA));
            if(transport!=null)state.put("transport",new JSONObject(transport.toString()));
            state.put("left",left);state.put("right",right);state.put("hardware_profile",HazRobHardwareProfile.ID);state.put("chemcore_controlled_capture",chemCoreCaptureSession);
            JSONObject d=snapshot.optJSONObject("drive");if(d!=null){
                state.put("failsafe",d.optBoolean("failsafe",false));state.put("retrace",d.optBoolean("retrace",false));state.put("hold",d.optBoolean("hold",false));
                state.put("safety_auto_stop",d.optBoolean("safety_auto_stop",false));state.put("forward_hazard",d.optBoolean("forward_hazard",false));state.put("forward_blocked",d.optBoolean("forward_blocked",false));state.put("forward_block_reason",d.optString("forward_block_reason",""));
            }
            JSONObject particles=snapshot.optJSONObject("particles");
            if(particles!=null)state.put("particles",new JSONObject(particles.toString()));
            JSONObject sound=snapshot.optJSONObject("sound");if(sound!=null)state.put("sound",sound);
            JSONObject ir=snapshot.optJSONObject("ir");if(ir!=null)state.put("ir",ir);
            JSONObject ldr=snapshot.optJSONObject("ldr");if(ldr!=null&&ldr.optBoolean("installed",false))state.put("ldr",ldr);
            JSONObject adc=snapshot.optJSONObject("adc");if(adc!=null)state.put("adc",adc);
            JSONObject relay=snapshot.optJSONObject("relay");if(relay!=null)state.put("relay",relay);
            JSONObject sensorPower=snapshot.optJSONObject("sensor_power");if(sensorPower!=null)state.put("sensor_power",sensorPower);
            JSONObject measurement=snapshot.optJSONObject("measurement");if(measurement!=null){
                String measurementState=measurement.optString("state","idle");int stateCode=measurement.optInt("state_code",0);
                state.put("measurement_state",measurementState);state.put("measurement_state_code",stateCode);state.put("measurement_phase",stateCode);state.put("measurement_mask",measurement.optInt("mask",0));
                state.put("measurement_valid_channels",measurement.optInt("valid_channels",0));state.put("measurement_error",measurement.optString("error",""));
            }
        }catch(Exception ignored){}
        v.put("motion_payload",state.toString());db.getWritableDatabase().insert("telemetry_samples",null,v);
        if(chemCoreCaptureSession)persistMqTiming(mq,tMs);
    }

    private void persistMqTiming(JSONObject mq,long sampleTMs){
        if(mq==null||sessionId==null)return;
        for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ){
            JSONObject r=mq.optJSONObject(hw.key);if(r==null||!r.has("scan_cycle")||!r.has("channel_offset_us"))continue;
            long cycle=r.optLong("scan_cycle",-1),offset=r.optLong("channel_offset_us",-1);if(cycle<0||offset<0)continue;
            ContentValues t=new ContentValues();t.put("session_id",sessionId);t.put("sample_t_ms",sampleTMs);t.put("scan_cycle_id",cycle);t.put("sensor_key",hw.key);t.put("channel_offset_us",offset);
            db.getWritableDatabase().insert("chemcore_channel_timing",null,t);
        }
    }

    private void finish(JSONObject snapshot,String state){
        final String completed=sessionId;final boolean wasChemCore=chemCoreCaptureSession;final long endedMonotonic=SystemClock.elapsedRealtime(),endedUtcMs=System.currentTimeMillis();ContentValues v=new ContentValues();v.put("ended_ts",isoUtc(endedUtcMs));v.put("payload",snapshot.toString());
        try{
            JSONObject meta=new JSONObject();meta.put("terminal_state",state);meta.put("contains_aux_sensors",true);meta.put("chemcore_controlled_capture",wasChemCore);meta.put("field_60s_capture",fieldCaptureSession);
            meta.put("hardware_profile",HazRobHardwareProfile.ID);meta.put("measurement_schema",CanonicalMeasurementNormalizer.SCHEMA);meta.put("sample_count",sampleSeq);
            meta.put("started_utc_ms",startedUtcMs);meta.put("ended_utc_ms",endedUtcMs);meta.put("started_monotonic_ms",startedElapsed);meta.put("ended_monotonic_ms",endedMonotonic);meta.put("duration_monotonic_ms",Math.max(0,endedMonotonic-startedElapsed));meta.put("clock_contract","UTC_EPOCH_MS_PLUS_ANDROID_ELAPSED_REALTIME");
            meta.put("mq_power_architecture","single_relay_8_sensors");meta.put("installed_relays","R1");meta.put("relay_gpio",25);meta.put("mq_sensor_count",8);meta.put("manual_warmup",true);meta.put("sample_period_ms",(wasChemCore||fieldCaptureSession)?200:250);meta.put("mq_scan_timing_capture",wasChemCore);
            JSONObject transport=snapshot.optJSONObject("_transport");if(transport!=null)meta.put("terminal_transport",transport.optString("mode","UNKNOWN"));
            JSONObject adc=snapshot.optJSONObject("adc");if(adc!=null&&adc.optJSONObject("acquisition")!=null)meta.put("adc_acquisition",adc.optJSONObject("acquisition"));
            v.put("analytics",meta.toString());
        }catch(Exception ignored){}
        db.getWritableDatabase().update("measurement_sessions",v,"session_id=?",new String[]{completed});

        // Ordinary completed measurements must produce a persisted ChemCore screening ranking.
        // Candidate ranking is allowed here; named identity and reportable ppm remain gated elsewhere.
        if(!wasChemCore&&"ready".equalsIgnoreCase(state)){
            try{
                long duration=Math.max(0,endedMonotonic-startedElapsed);
                CompletedSessionChemCoreAnalyzer.Outcome analysis=CompletedSessionChemCoreAnalyzer.analyzeAndPersist(appContext,db,completed,snapshot,duration,fieldCaptureSession);
                db.putMeta("last_chemcore_screening_session_id",completed);
                db.putMeta("last_chemcore_screening_candidate_count",Integer.toString(analysis.candidateCount));
                db.putMeta("last_chemcore_screening_observed_channels",Integer.toString(analysis.observedChannels));
            }catch(Exception ignored){}
        }

        lastCompletedSessionId=completed;db.putMeta("last_measurement_session_id",completed);db.putMeta("last_measurement_session_terminal_state",state);
        MeasurementSessionRegistry.completed(completed);if(wasChemCore)MeasurementSessionRegistry.clearChemCoreCapture();if(fieldCaptureSession)MeasurementSessionRegistry.clearFieldCapture();sessionId=null;startedElapsed=startedUtcMs=lastSampleElapsed=sampleSeq=0;chemCoreCaptureSession=false;fieldCaptureSession=false;
    }

    /**
     * Run only AFTER telemetry workers have drained on Activity destruction.
     * A killed UI must not leave an in-memory capture marked active or an
     * apparently unended technical session. This NEVER creates a READY result.
     */
    public synchronized void abortInterruptedFieldCapture(String reason){
        if(!fieldCaptureSession||sessionId==null||sessionId.isEmpty())return;
        final String interrupted=sessionId;
        try{
            long utc=System.currentTimeMillis();
            long mono=SystemClock.elapsedRealtime();
            JSONObject metadata=new JSONObject();
            metadata.put("terminal_state","error");
            metadata.put("field_60s_capture",true);
            metadata.put("sample_count",sampleSeq);
            metadata.put("started_utc_ms",startedUtcMs);
            metadata.put("ended_utc_ms",utc);
            metadata.put("started_monotonic_ms",startedElapsed);
            metadata.put("ended_monotonic_ms",mono);
            metadata.put("duration_monotonic_ms",Math.max(0,mono-startedElapsed));
            metadata.put("clock_contract","UTC_EPOCH_MS_PLUS_ANDROID_ELAPSED_REALTIME");
            metadata.put("failure_reason",reason==null?"ACTIVITY_DESTROYED":reason);
            ContentValues values=new ContentValues();
            values.put("ended_ts",isoUtc(utc));
            values.put("analytics",metadata.toString());
            values.put("payload","{}");
            db.getWritableDatabase().update("measurement_sessions",values,
                "session_id=? AND (ended_ts IS NULL OR TRIM(ended_ts)='')",
                new String[]{interrupted});
        }catch(Exception error){
            android.util.Log.e("TankV5FieldRecorder","Unable to persist interrupted field capture",error);
        }finally{
            MeasurementSessionRegistry.resetActive(interrupted);
            MeasurementSessionRegistry.clearFieldCapture();
            sessionId=null;startedElapsed=startedUtcMs=lastSampleElapsed=sampleSeq=0;
            fieldCaptureSession=false;chemCoreCaptureSession=false;
        }
    }

    private static String isoUtc(long utcMs){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date(utcMs));}
}
