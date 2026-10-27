package com.servoc.app.calibration;

import android.content.SharedPreferences;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/** Tank-only persistent R0: last user-approved device calibration, never inherited HR069 values. */
public final class ManualR0Calibration {
    private ManualR0Calibration(){}
    private static final String[] KEYS={"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"};
    private static final Map<String,Double> values=new HashMap<>();
    private static final Map<String,Integer> sampleCounts=new HashMap<>();
    private static final Map<String,String> sources=new HashMap<>(),sessions=new HashMap<>(),utcs=new HashMap<>(),deviceIds=new HashMap<>();
    private static boolean loaded=false;

    public static synchronized void load(SharedPreferences p){
        values.clear();sources.clear();sessions.clear();utcs.clear();deviceIds.clear();sampleCounts.clear();
        for(String k:KEYS){
            if(p!=null){
                String s=p.getString("manual_r0_"+k,"");
                try{
                    double v=Double.parseDouble(s);
                    if(Double.isFinite(v)&&v>0){
                        values.put(k,v);sources.put(k,p.getString("manual_r0_source_"+k,"manual_device_r0"));
                        String session=p.getString("manual_r0_session_"+k,"");if(!session.isEmpty())sessions.put(k,session);
                        String utc=p.getString("manual_r0_utc_"+k,"");if(!utc.isEmpty())utcs.put(k,utc);
                        String device=p.getString("manual_r0_device_"+k,"");if(!device.isEmpty())deviceIds.put(k,device);
                        try{int n=Integer.parseInt(p.getString("manual_r0_samples_"+k,"0"));if(n>0)sampleCounts.put(k,n);}catch(NumberFormatException ignored){}
                    }
                }catch(Exception ignored){}
            }
            // Never silently fall back to historic HR069 constants for independent Tank hardware.
        }
        loaded=true;
    }

    public static synchronized boolean isLoaded(){return loaded;}
    public static synchronized double get(String key){key=norm(key);Double v=values.get(key);return v==null?Double.NaN:v;}
    public static synchronized String getSource(String key){key=norm(key);String s=sources.get(key);return s!=null&&!s.isEmpty()?s:"unavailable";}
    public static synchronized String getSession(String key){key=norm(key);return sessions.getOrDefault(key,"");}
    public static synchronized String getUtc(String key){key=norm(key);return utcs.getOrDefault(key,"");}
    public static synchronized String getDeviceId(String key){return deviceIds.getOrDefault(norm(key),"");}
    public static synchronized int getSamples(String key){return sampleCounts.getOrDefault(norm(key),0);}
    public static synchronized boolean has(String key){double v=get(key);return Double.isFinite(v)&&v>0;}

    /** Device-specific clean-air baseline, NOT a reference-gas/ppm curve calibration. */
    public static synchronized boolean save(SharedPreferences p,String key,double r0){
        return saveForDevice(p,key,r0,"");
    }
    public static synchronized boolean saveForDevice(SharedPreferences p,String key,double r0,String deviceId){
        key=norm(key);
        deviceId=deviceId==null?"":deviceId.trim();
        if(p==null||!supported(key)||!Double.isFinite(r0)||r0<=0)return false;
        String utc=iso(System.currentTimeMillis()),session="MANUAL-R0-"+System.currentTimeMillis();
        SharedPreferences.Editor edit=p.edit()
                .putString("manual_r0_"+key,Double.toString(r0))
                .putString("manual_r0_source_"+key,"manual_device_r0")
                .putString("manual_r0_session_"+key,session)
                .putString("manual_r0_utc_"+key,utc);
        edit.remove("manual_r0_samples_"+key);
        if(deviceId.isEmpty())edit.remove("manual_r0_device_"+key);
        else edit.putString("manual_r0_device_"+key,deviceId);
        // Do not change live ChemCore R0 until durable phone storage has succeeded.
        if(!edit.commit())return false;
        values.put(key,r0);sources.put(key,"manual_device_r0");sessions.put(key,session);utcs.put(key,utc);
        if(deviceId.isEmpty())deviceIds.remove(key);else deviceIds.put(key,deviceId);
        sampleCounts.remove(key);
        loaded=true;
        return true;
    }

    /**
     * Commits a complete eight-sensor revision as one atomic SharedPreferences transaction.
     * All values are checked before writing; no partial in-memory or on-disk updates.
     */
    public static synchronized boolean saveAll(SharedPreferences p,Map<String,Double> updated){
        return saveAllForDevice(p,updated,"");
    }
    public static synchronized boolean saveAllForDevice(SharedPreferences p,Map<String,Double> updated,String deviceId){
        deviceId=deviceId==null?"":deviceId.trim();
        if(p==null||updated==null||updated.size()!=KEYS.length)return false;
        for(String k:KEYS){
            Double v=updated.get(k);
            if(v==null||!Double.isFinite(v)||v<=0)return false;
        }
        long now=System.currentTimeMillis();String utc=iso(now),session="MANUAL-R0-"+now;
        SharedPreferences.Editor e=p.edit();
        for(String k:KEYS){
            double r0=updated.get(k);
            e.putString("manual_r0_"+k,Double.toString(r0));
            e.putString("manual_r0_source_"+k,"manual_device_r0");
            e.putString("manual_r0_session_"+k,session);
            e.putString("manual_r0_utc_"+k,utc);
            e.remove("manual_r0_samples_"+k);
            if(deviceId.isEmpty())e.remove("manual_r0_device_"+k);
            else e.putString("manual_r0_device_"+k,deviceId);
        }
        if(!e.commit())return false;
        for(String k:KEYS){
            values.put(k,updated.get(k));sources.put(k,"manual_device_r0");
            sessions.put(k,session);utcs.put(k,utc);
            if(deviceId.isEmpty())deviceIds.remove(k);else deviceIds.put(k,deviceId);
            sampleCounts.remove(k);
        }
        loaded=true;
        return true;
    }

    /** Persist measured user clean-air operating R0 values for available channels in one transaction. */
    public static synchronized boolean saveAutomatic(SharedPreferences p,Map<String,Double> proposed){
        if(p==null||proposed==null||proposed.isEmpty())return false;
        for(Map.Entry<String,Double> a:proposed.entrySet()){
            if(!supported(a.getKey())||a.getValue()==null||!Double.isFinite(a.getValue())||a.getValue()<=0)return false;
        }
        long now=System.currentTimeMillis();String utc=iso(now),session="TANK-AUTO-R0-"+now;
        SharedPreferences.Editor e=p.edit();
        for(Map.Entry<String,Double> a:proposed.entrySet()){
            String k=a.getKey();double r0=a.getValue();
            e.putString("manual_r0_"+k,Double.toString(r0));
            e.putString("manual_r0_source_"+k,"automatic_clean_air_field_r0");
            e.putString("manual_r0_session_"+k,session);
            e.putString("manual_r0_utc_"+k,utc);
            e.remove("manual_r0_device_"+k);
        }
        if(!e.commit())return false;
        for(Map.Entry<String,Double> a:proposed.entrySet()){
            String k=a.getKey();values.put(k,a.getValue());sources.put(k,"automatic_clean_air_field_r0");
            sessions.put(k,session);utcs.put(k,utc);deviceIds.remove(k);
        }
        loaded=true;return true;
    }


    /** Persist one complete, firmware-verified R0 revision with its originating ESP32 identity. */
    public static synchronized boolean saveFirmware(SharedPreferences p,Map<String,Double> proposal,String deviceId){
        // Legacy caller does not supply count evidence; preserve values but never invent samples.
        return saveFirmware(p,proposal,deviceId,null);
    }
    /** Commit firmware R0 and its measured samples in one phone-side storage transaction. */
    public static synchronized boolean saveFirmware(SharedPreferences p,Map<String,Double> proposal,
                                                     String deviceId,Map<String,Integer> samples){
        if(p==null||proposal==null||proposal.size()!=KEYS.length||deviceId==null||deviceId.trim().isEmpty())return false;
        if(samples!=null){
            if(samples.size()!=KEYS.length)return false;
            for(String key:KEYS){Integer n=samples.get(key);if(n==null||n<10)return false;}
        }
        for(String key:KEYS){
            Double v=proposal.get(key);
            if(v==null||!Double.isFinite(v)||v<=0)return false;
        }
        long now=System.currentTimeMillis();String utc=iso(now),session="FIRMWARE-R0-"+now;
        SharedPreferences.Editor edit=p.edit();
        for(String key:KEYS){
            edit.putString("manual_r0_"+key,Double.toString(proposal.get(key)))
                .putString("manual_r0_source_"+key,"firmware_clean_air_r0")
                .putString("manual_r0_session_"+key,session)
                .putString("manual_r0_utc_"+key,utc)
                .putString("manual_r0_device_"+key,deviceId.trim());
            if(samples!=null)edit.putString("manual_r0_samples_"+key,Integer.toString(samples.get(key)));
            else edit.remove("manual_r0_samples_"+key);
        }
        if(!edit.commit())return false;
        for(String key:KEYS){
            values.put(key,proposal.get(key));sources.put(key,"firmware_clean_air_r0");
            sessions.put(key,session);utcs.put(key,utc);deviceIds.put(key,deviceId.trim());
            if(samples!=null)sampleCounts.put(key,samples.get(key));else sampleCounts.remove(key);
        }
        loaded=true;return true;
    }

    public static String firmwareDevice(SharedPreferences prefs,String key){
        if(prefs==null||!supported(norm(key)))return "";
        return prefs.getString("manual_r0_device_"+norm(key),"");
    }

    /** Reset one Tank channel to uncalibrated; never reactivate inherited HR069 R0 constants. */
    public static synchronized boolean reset(SharedPreferences p,String key){
        key=norm(key);
        if(p==null||!supported(key))return false;
        if(!p.edit().remove("manual_r0_"+key).remove("manual_r0_source_"+key)
                .remove("manual_r0_session_"+key).remove("manual_r0_utc_"+key)
                .remove("manual_r0_device_"+key)
                .remove("manual_r0_samples_"+key).commit())return false;
        values.remove(key);sources.remove(key);sessions.remove(key);utcs.remove(key);deviceIds.remove(key);sampleCounts.remove(key);
        // Clearing Tank calibration must not revive legacy field or firmware R0.
        loaded=true;
        return true;
    }

    /** Legacy no-return API; delegates to the durable reset implementation. */
    public static synchronized void clear(SharedPreferences p,String key){reset(p,key);}

    /** Recomputes Rs/R0 from received Rs using the active device-specific normalization baseline. */
    public static synchronized void apply(String key,JSONObject q){
        if(q==null)return;key=norm(key);double r0=get(key),rs=q.optDouble("rs",Double.NaN);String source=getSource(key),session=getSession(key),utc=getUtc(key);
        try{
            if(Double.isFinite(r0)&&r0>0&&Double.isFinite(rs)&&rs>0
                    && !isUnavailable(q)){
                q.put("r0",r0);q.put("ratio",rs/r0);q.put("r0_calibrated",true);q.put("r0_source",source);q.put("r0_reference_validated",false);
                if(!session.isEmpty())q.put("r0_session",session);if(!utc.isEmpty())q.put("r0_utc",utc);
                if(!getDeviceId(key).isEmpty())q.put("r0_device_id",getDeviceId(key));
                if(getSamples(key)>0)q.put("r0_samples",getSamples(key));
                long age=ageMs(utc);q.put("r0_age_known",age>=0);if(age>=0)q.put("r0_age_ms",age);
                q.put("r0_provenance_complete",!source.isEmpty()&&!"unavailable".equals(source)&&!session.isEmpty()&&!utc.isEmpty());
                if(FieldR0Baseline.SOURCE.equals(source)){
                    q.put("r0_samples",FieldR0Baseline.SAMPLE_COUNT);q.put("r0_window_s",FieldR0Baseline.WINDOW_SECONDS);q.put("r0_environment_compensated",FieldR0Baseline.ENVIRONMENT_COMPENSATED);
                }
            }else{
                // A sensor without valid Rs has no current Rs/R0. Discard any ratio
                // and firmware R0 copied into the normalised object, so stale
                // development-era calibration cannot masquerade as a fresh capture.
                if(Double.isFinite(r0)&&r0>0)q.put("r0",r0);else q.remove("r0");
                q.remove("ratio");
                q.remove("r0_session");q.remove("r0_utc");q.remove("r0_age_ms");
                q.remove("r0_samples");q.remove("r0_window_s");q.remove("r0_environment_compensated");
                q.put("r0_calibrated",false);q.put("r0_source","unavailable");q.put("r0_reference_validated",false);q.put("r0_age_known",false);q.put("r0_provenance_complete",false);
            }
        }catch(Exception ignored){}
    }

    /** An explicit hardware-loss status invalidates chemical ratios, even with stale Rs. */
    private static boolean isUnavailable(JSONObject q){
        String status=q.optString("status","").trim();
        if(status.isEmpty()){
            JSONObject quality=q.optJSONObject("quality");
            status=quality==null?"":quality.optString("status","").trim();
        }
        status=status.toUpperCase(Locale.US);
        return "POWER_OFF".equals(status)||"ADC_UNAVAILABLE".equals(status)
                ||"MISSING_SOURCE".equals(status);
    }

    public static String summary(){
        StringBuilder s=new StringBuilder();
        for(String k:KEYS){if(s.length()>0)s.append('\n');double v=get(k);s.append(k.toUpperCase(Locale.US)).append(": ").append(Double.isFinite(v)?String.format(Locale.US,"%.5f",v):"غير معاير");if(Double.isFinite(v))s.append(" • ").append(getSource(k));}
        return s.toString();
    }

    public static String[] keys(){return KEYS.clone();}
    // Historical FieldR0Baseline remains available to archived measurements only.
    private static boolean supported(String key){for(String k:KEYS)if(k.equals(key))return true;return false;}
    private static String norm(String key){return key==null?"":key.trim().toLowerCase(Locale.US);}
    private static String iso(long ms){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date(ms));}
    private static long ageMs(String utc){if(utc==null||utc.isEmpty())return -1;try{SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));Date d=f.parse(utc);return d==null?-1:Math.max(0,System.currentTimeMillis()-d.getTime());}catch(Exception e){return -1;}}
}
