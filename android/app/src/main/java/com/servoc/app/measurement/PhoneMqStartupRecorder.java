package com.servoc.app.measurement;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Android-local research-only MQ first-minute log. Never polls the ESP32,
 * changes heater state, writes calibration R0 or performs gas classification.
 *
 * Each existing /data telemetry snapshot is passed by MainActivity's existing
 * polling worker. The app creates a bounded JSONL log in its PRIVATE files dir:
 * files/chemcore_v6/mq_startup/. Not in firmware/NVS/ESP32 flash or SQLite.
 *
 * Scientific limits: phone arrival rate != ADS1115 acquisition rate; duplicate
 * firmware scan_cycle values, stale sensor reads and missing first seconds are
 * captured/flagged explicitly. Unsupported MQTT/late connections cannot be
 * promoted to a full startup fingerprint.
 */
public final class PhoneMqStartupRecorder {
    private static final String TAG="TankV6StartupPhone";
    private static final String[] CHANNELS={"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"};
    private static final int MAX_FILES=20, MAX_ROWS=640, FLUSH_EVERY_ROWS=8;
    private final TankV6StartupWindow window=new TankV6StartupWindow();
    private final File directory;
    private BufferedWriter writer;
    private int loggedRows=0,unflushedRows=0;
    private long activeSequence=-1;
    private boolean startedNearPowerOn=false;
    private String endpoint="";
    private String lastError="";

    public PhoneMqStartupRecorder(Context context) {
        if(context==null)throw new IllegalArgumentException("Android context is required");
        directory=new File(context.getApplicationContext().getFilesDir(),
                           "chemcore_v6/mq_startup");
    }
    public synchronized String lastError(){return lastError;}
    public synchronized File directory(){return directory;}

    /** Called only after an ALREADY-EXISTING telemetry snapshot is received.
     * Must run on a worker thread, not the UI/drive thread.
     */
    public synchronized void accept(JSONObject snapshot,String robotEndpoint) {
        if(snapshot==null)return;
        try {
            JSONObject relay=snapshot.optJSONObject("relay");
            if(relay==null)return; // do not infer heater start from a missing field
            boolean on=relay.optBoolean("on",relay.optInt("mask",0)!=0);
            long powerMs=relay.optLong("power_ms",-1);
            JSONObject mq=snapshot.optJSONObject("mq");
            List<TankV6StartupWindow.Reading> readings=new ArrayList<>();
            if(mq!=null)for(String key:CHANNELS){
                JSONObject r=mq.optJSONObject(key);
                if(r==null)continue;
                readings.add(new TankV6StartupWindow.Reading(
                    key,r.optLong("scan_cycle",-1),r.optLong("age_us",-1),
                    r.optLong("sample_us",-1),r.optDouble("rs",Double.NaN),
                    r.optBoolean("ok",false)&&r.optBoolean("powered",on)));
            }
            TankV6StartupWindow.Batch batch=window.accept(on,powerMs,readings);
            if(batch.newlyPoweredOn) {
                end("RELAY_RESTART_OR_NEW_POWER_EVENT");
                endpoint=robotEndpoint==null?"":robotEndpoint;
                startedNearPowerOn=batch.startedNearPowerOn;
                activeSequence=batch.eventSequence;
                begin(powerMs);
            }
            if(writer!=null)for(TankV6StartupWindow.Row row:batch.rows) {
                if(loggedRows>=MAX_ROWS) {
                    end("RECORD_LIMIT_REACHED");
                    break;
                }
                JSONObject payload=new JSONObject();
                payload.put("schema","hazrob-tank-v6-phone-startup-sample/1");
                payload.put("kind","MQ_STARTUP_RESEARCH_ROW");
                payload.put("phone_received_utc_ms",System.currentTimeMillis());
                payload.put("phone_received_elapsed_ms",SystemClock.elapsedRealtime());
                payload.put("event_sequence",row.eventSequence);
                payload.put("relay_power_ms",row.relayPowerMs);
                payload.put("sample_since_relay_ms",row.sampleSinceRelayMs);
                payload.put("sensor_key",row.sensorKey);
                payload.put("scan_cycle",row.scanCycle);
                payload.put("sample_us",row.sampleUs);
                payload.put("rs_native_firmware_unit",row.resistance);
                payload.put("rs_unit_note","FIRMWARE_NATIVE_NOT_R0_CALIBRATION");
                payload.put("started_near_power_on",row.captureBeginsNearPowerOn);
                JSONObject env=snapshot.optJSONObject("env");
                if(env!=null){
                    double temp=env.optDouble("temperature",Double.NaN);
                    double rh=env.optDouble("humidity",Double.NaN);
                    if(Double.isFinite(temp))payload.put("temperature_c",temp);
                    if(Double.isFinite(rh))payload.put("relative_humidity_pct",rh);
                }
                write(payload);
                loggedRows++;
                if(++unflushedRows>=FLUSH_EVERY_ROWS)flush();
            }
            if(batch.completed && writer!=null)
                end(on?"WINDOW_60_SECONDS_REACHED":"RELAY_POWER_OFF");
        }catch(Exception ex){
            lastError=ex.getClass().getSimpleName();
            Log.w(TAG,"Phone MQ startup log stopped: "+lastError);
            end("STORAGE_ERROR");
        }
    }

    private void begin(long observedPowerMs) throws Exception {
        if(!directory.exists() && !directory.mkdirs())throw new IOException("Storage directory unavailable");
        pruneCompleted();
        String fname="startup-"+System.currentTimeMillis()+"-"+activeSequence+".jsonl";
        writer=new BufferedWriter(new OutputStreamWriter(
            new FileOutputStream(new File(directory,fname),false),StandardCharsets.UTF_8),8192);
        loggedRows=unflushedRows=0;
        JSONObject event=new JSONObject();
        event.put("schema","hazrob-tank-v6-phone-startup-capture/1");
        event.put("kind","EVENT_START");
        event.put("capture_created_utc_ms",System.currentTimeMillis());
        event.put("device_endpoint_label",endpoint);
        event.put("device_identity_confirmed",false);
        event.put("event_sequence",activeSequence);
        event.put("initial_relay_power_ms",observedPowerMs);
        event.put("started_near_power_on",startedNearPowerOn);
        event.put("window_duration_ms",TankV6StartupWindow.WINDOW_MS);
        event.put("transport","EXISTING_ANDROID_TELEMETRY_POLL_ONLY");
        event.put("no_extra_esp32_requests",true);
        event.put("power_and_heater_control","UNCHANGED");
        event.put("scientific_status","RESEARCH_ONLY_NOT_M4");
        event.put("gas_identity",JSONObject.NULL);
        event.put("reportable_ppm",JSONObject.NULL);
        write(event);
        flush();
    }

    private void write(JSONObject row) throws IOException {
        if(writer==null)return;
        writer.write(row.toString());
        writer.newLine();
    }
    private void flush() throws IOException {
        if(writer!=null){writer.flush();unflushedRows=0;}
    }
    private void end(String reason) {
        if(writer==null)return;
        try{
            JSONObject closed=new JSONObject();
            closed.put("schema","hazrob-tank-v6-phone-startup-capture/1");
            closed.put("kind","EVENT_END");
            closed.put("end_reason",reason);
            closed.put("event_sequence",activeSequence);
            closed.put("sample_rows",loggedRows);
            closed.put("started_near_power_on",startedNearPowerOn);
            closed.put("completed_first_minute",
                "WINDOW_60_SECONDS_REACHED".equals(reason)&&startedNearPowerOn);
            closed.put("requires_qualified_heater_variant_and_research_holdout",true);
            closed.put("source_sensor_unit_verified",false);
            closed.put("M4_eligible",false);
            write(closed);
            flush();
        }catch(Exception ex){lastError=ex.getClass().getSimpleName();}
        finally{
            try{writer.close();}catch(IOException ignored){}
            writer=null;activeSequence=-1;loggedRows=unflushedRows=0;
        }
    }
    public synchronized void close(){end("PHONE_RECORDER_CLOSED_OR_ACTIVITY_DESTROYED");}

    private void pruneCompleted() {
        File[] list=directory.listFiles((d,name)->name.startsWith("startup-")&&name.endsWith(".jsonl"));
        if(list==null || list.length<MAX_FILES)return;
        Arrays.sort(list,Comparator.comparingLong(File::lastModified)
                                   .thenComparing(File::getName));
        // Never prune an open writer; called before opening a new file only.
        for(int i=0;i<=list.length-MAX_FILES;i++) {
            if(!list[i].delete())Log.w(TAG,"Unable to prune old phone capture");
        }
    }
}
