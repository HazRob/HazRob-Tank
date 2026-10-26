package com.servoc.app.communication;

import com.servoc.app.measurement.CanonicalMeasurementNormalizer;
import com.servoc.app.calibration.ManualR0Calibration;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** HazRob API facade: HTTP for Direct control/telemetry, MQTT for Remote. */
public final class RobotApi {
    // Main firmware stops stale motion after 500 ms. Refresh well inside that lease and keep
    // a local drive request timeout below the full firmware watchdog so one slow HTTP exchange
    // cannot dominate steering responsiveness.
    private static final long DIRECT_KEEPALIVE_MS=150L;
    private static final int DIRECT_DRIVE_TIMEOUT_MS=250;
    private static final int DIRECT_STOP_TIMEOUT_MS=350;
    private static volatile boolean operatorForeground=true;
    private static volatile boolean tankOffline=false;
    /** Process-wide hard network guard: a view cannot bypass offline mode via raw RobotApi. */
    public static void setTankOffline(boolean offline){tankOffline=offline;}
    public static boolean tankOffline(){return tankOffline;}
    private static void requireTransport(){if(tankOffline)throw new IllegalStateException("Tank offline preview: robot network disabled");}
    private static volatile boolean requireNeutralAfterBackground=false;

    private volatile String robotHost;
    private int lastDirectDriveLeft=Integer.MIN_VALUE,lastDirectDriveRight=Integer.MIN_VALUE;
    private long lastDirectDriveAt=0;

    public RobotApi(String host){setRobotHost(host);}

    public static void setOperatorForeground(boolean foreground){
        if(!foreground){operatorForeground=false;requireNeutralAfterBackground=true;}
        else operatorForeground=true;
    }
    public static boolean operatorForeground(){return operatorForeground;}

    public synchronized void setRobotHost(String host){
        if(host==null||host.trim().isEmpty())host=LocalNetworkContract.ROBOT_HOST;
        host=host.trim().replace("http://","").replace("https://","");while(host.endsWith("/"))host=host.substring(0,host.length()-1);
        if(!host.equals(robotHost)){lastDirectDriveLeft=lastDirectDriveRight=Integer.MIN_VALUE;lastDirectDriveAt=0;}
        robotHost=host;
    }
    public String getRobotHost(){return robotHost;}
    private String url(String path){return "http://"+robotHost+(path.startsWith("/")?path:"/"+path);}
    private static RemoteConnection remote(){requireTransport();RemoteConnection r=RemoteConnection.current();if(r==null&&RemoteConnection.transportOwned())throw new IllegalStateException("remote transport reconnecting");return r;}

    public String getText(String path,int timeoutMs)throws Exception{requireTransport();HttpURLConnection c=(HttpURLConnection)new URL(url(path)).openConnection();c.setUseCaches(false);c.setConnectTimeout(timeoutMs);c.setReadTimeout(timeoutMs);c.setRequestMethod("GET");c.setRequestProperty("Cache-Control","no-store");return finish(c);}
    public String postForm(String path,String body,int timeoutMs)throws Exception{requireTransport();HttpURLConnection c=(HttpURLConnection)new URL(url(path)).openConnection();c.setUseCaches(false);c.setConnectTimeout(timeoutMs);c.setReadTimeout(timeoutMs);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");c.setRequestProperty("Cache-Control","no-store");byte[] b=(body==null?"":body).getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(b.length);try(OutputStream out=c.getOutputStream()){out.write(b);out.flush();}return finish(c);}
    private static String finish(HttpURLConnection c)throws Exception{int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();StringBuilder s=new StringBuilder();if(in!=null){try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String line;while((line=r.readLine())!=null)s.append(line);}}c.disconnect();if(code<200||code>=300)throw new IllegalStateException("HTTP "+code+" "+s);return s.toString();}
    public JSONObject getJson(String path,int timeoutMs)throws Exception{return new JSONObject(getText(path,timeoutMs));}

    public synchronized void drive(int left,int right)throws Exception{
        requireTransport();
        // The foreground service latches drive after backgrounding until a neutral command is seen.
        // This prevents a stale joystick value from resuming motion when the activity returns.
        if(!operatorForeground){if(left==0&&right==0)lastDirectDriveLeft=lastDirectDriveRight=0;return;}
        if(requireNeutralAfterBackground){
            if(left!=0||right!=0)return;
            requireNeutralAfterBackground=false;
        }
        RemoteConnection r=remote();if(r!=null){r.drive(left,right);return;}

        long now=System.nanoTime()/1_000_000L;
        boolean changed=left!=lastDirectDriveLeft||right!=lastDirectDriveRight;
        if(!changed){
            if(left==0&&right==0)return;
            if(now-lastDirectDriveAt<DIRECT_KEEPALIVE_MS)return;
        }
        getText("/drive?l="+left+"&r="+right,DIRECT_DRIVE_TIMEOUT_MS);
        lastDirectDriveLeft=left;lastDirectDriveRight=right;lastDirectDriveAt=now;
    }
    public synchronized void motionStop()throws Exception{RemoteConnection r=remote();if(r!=null){r.motionStop();return;}try{getText("/motion/stop",DIRECT_STOP_TIMEOUT_MS);}catch(Exception e){getText("/stop",DIRECT_STOP_TIMEOUT_MS);}lastDirectDriveLeft=lastDirectDriveRight=0;lastDirectDriveAt=System.nanoTime()/1_000_000L;}
    public void stop()throws Exception{motionStop();}
    public synchronized void emergencyStop()throws Exception{RemoteConnection r=remote();if(r!=null){r.emergencyStop();return;}getText("/estop",650);lastDirectDriveLeft=lastDirectDriveRight=0;lastDirectDriveAt=System.nanoTime()/1_000_000L;}
    public void emergencyReset()throws Exception{RemoteConnection r=remote();if(r!=null){r.emergencyReset();return;}getText("/estop/reset",650);}
    public void sensorPowerOff()throws Exception{RemoteConnection r=remote();if(r!=null){r.sensorOff();return;}try{getText("/sensor/off",650);}catch(Exception e){mqPower(false);}}
    public void heartbeat()throws Exception{RemoteConnection r=remote();if(r!=null){r.heartbeat();return;}getText("/heartbeat",500);}
    public void disarmHeartbeat()throws Exception{RemoteConnection r=remote();if(r!=null){r.command("heartbeat_disarm",new JSONObject(),2000,false,0);return;}getText("/heartbeat?arm=0",500);}

    /** Raw endpoints intentionally remain local for provisioning and Direct compatibility checks. */
    public JSONObject diagnosticsRaw()throws Exception{return getJson("/diag",1000);}
    public JSONObject dataRaw()throws Exception{return getJson("/data",1200);}
    private JSONObject directNormalizedSnapshot(int commandedLeft,int commandedRight)throws Exception{return RobotProtocolV4.normalize(diagnosticsRaw(),dataRaw(),commandedLeft,commandedRight);}
    private static JSONObject applyActiveR0(JSONObject snapshot){
        // Handle both published canonical snapshots and direct diag/data MQTT envelopes.
        // All eight channels use the latest DURABLY saved per-sensor R0 on Android.
        if(snapshot==null)return null;
        String deviceId=snapshot.optString("device_id","");
        JSONObject mq=snapshot.optJSONObject("mq");
        reconcileMq(mq,deviceId);
        JSONObject measurement=snapshot.optJSONObject("measurement");
        if(measurement!=null)reconcileMq(measurement.optJSONObject("mq"),deviceId);
        return snapshot;
    }
    private static void reconcileMq(JSONObject mq,String deviceId){
        if(mq==null)return;
        for(String key:ManualR0Calibration.keys()){
            JSONObject sensor=mq.optJSONObject(key);
            if(sensor==null)continue;
            String source=ManualR0Calibration.getSource(key);
            String bound=ManualR0Calibration.getDeviceId(key);
            // Reject cross-robot reuse of firmware-measured R0, including MQTT
            // snapshots which lack a verified controller identity.
            if("firmware_clean_air_r0".equals(source)&&
                    (bound.isEmpty()||!bound.equals(deviceId))){
                sensor.remove("ratio");sensor.remove("r0");
                try{
                    sensor.put("r0_calibrated",false);
                    sensor.put("r0_source","device_mismatch");
                    sensor.put("r0_provenance_complete",false);
                    sensor.put("r0_reference_validated",false);
                }catch(org.json.JSONException ignored){}
                continue;
            }
            ManualR0Calibration.apply(key,sensor);
        }
    }
    public JSONObject snapshot(int commandedLeft,int commandedRight)throws Exception{
        RemoteConnection r=remote();
        if(r!=null){
            if(!r.isConnected())throw new IllegalStateException("remote MQTT disconnected");
            JSONObject s=r.snapshot(commandedLeft,commandedRight);if(s==null)throw new IllegalStateException("remote telemetry unavailable");
            // MQTT may deliver a cached canonical snapshot whose firmware R0 predates the last
            // phone calibration. Re-normalize a copy before any UI, alarm or ChemCore consumer.
            return CanonicalMeasurementNormalizer.enrich(applyActiveR0(new JSONObject(s.toString())),"REMOTE");
        }
        return CanonicalMeasurementNormalizer.enrich(directNormalizedSnapshot(commandedLeft,commandedRight),"DIRECT");
    }
    public JSONObject diagnostics()throws Exception{RemoteConnection r=remote();if(r!=null){if(!r.isConnected())throw new IllegalStateException("remote MQTT disconnected");JSONObject s=r.snapshot(0,0);if(s==null)throw new IllegalStateException("remote telemetry unavailable");return s;}return diagnosticsRaw();}
    public JSONObject data()throws Exception{RemoteConnection r=remote();if(r!=null){if(!r.isConnected())throw new IllegalStateException("remote MQTT disconnected");JSONObject s=r.snapshot(0,0);if(s==null)throw new IllegalStateException("remote telemetry unavailable");return s;}return dataRaw();}

    public JSONObject config()throws Exception{RemoteConnection r=remote();if(r!=null){JSONObject a=r.command("config_get",new JSONObject(),3000,true,2500);JSONObject p=a.optJSONObject("payload");return p==null?new JSONObject():p;}return getJson("/cfg",1200);}
    /** Local endpoints bootstrap or switch the robot WAN transport. */
    public JSONObject remoteStatus()throws Exception{return getJson("/remote/status",1800);}
    public JSONObject remoteScan()throws Exception{return getJson("/remote/scan",2800);}
    public JSONObject remoteProvision(String ssid,String password)throws Exception{return new JSONObject(postForm("/remote/provision","ssid="+enc(ssid)+"&password="+enc(password),3000));}
    public JSONObject remoteDisconnect()throws Exception{return new JSONObject(postForm("/remote/disconnect","",1500));}
    public JSONObject remoteAuthorize(boolean enabled)throws Exception{return new JSONObject(postForm("/remote/authorize","enabled="+(enabled?"1":"0"),1800));}
    public JSONObject remoteReconnect()throws Exception{return new JSONObject(postForm("/remote/connect","",1800));}
    public JSONObject remoteClear()throws Exception{return new JSONObject(postForm("/remote/clear","confirm=1",1500));}

    /** Tank servo drive configuration. Direction mapping stays in firmware; operator sets only speed. */
    public JSONObject setConfig(String cameraIp,int speed,int warmupSec,int measureSec)throws Exception{
        RemoteConnection r=remote();if(r!=null){JSONObject p=new JSONObject();p.put("cam",cameraIp==null?"":cameraIp);p.put("speed",speed);p.put("warm",warmupSec);p.put("measure",measureSec);JSONObject a=r.command("config_set",p,4000,true,3000);JSONObject q=a.optJSONObject("payload");return q==null?p:q;}
        String cam=URLEncoder.encode(cameraIp==null?"":cameraIp,StandardCharsets.UTF_8.name());return getJson("/cfg?cam="+cam+"&speed="+speed+"&warm="+warmupSec+"&measure="+measureSec,1500);
    }

    /** Direct-only firmware V4 R0 service: phone never synthesizes an R0 sampling series. */
    public JSONObject calibrationStatus()throws Exception{
        RemoteConnection r=remote();
        if(r!=null)throw new UnsupportedOperationException("Firmware R0 remote command not implemented");
        return getJson("/r0",1500);
    }
    public JSONObject calibrationStart(int warmupSeconds,int captureSeconds)throws Exception{
        if(warmupSeconds<0||warmupSeconds>3600||captureSeconds<1||captureSeconds>3600)
            throw new IllegalArgumentException("Invalid R0 time window");
        RemoteConnection r=remote();
        if(r!=null)throw new UnsupportedOperationException("Firmware R0 remote command not implemented");
        return getJson("/r0?cmd=start&warm="+warmupSeconds+"&duration="+captureSeconds,2000);
    }
    public JSONObject calibrationCancel()throws Exception{
        RemoteConnection r=remote();
        if(r!=null)throw new UnsupportedOperationException("Firmware R0 remote command not implemented");
        return getJson("/r0?cmd=cancel",1500);
    }

    public void startMeasurement()throws Exception{RemoteConnection r=remote();if(r!=null){r.startMeasurement();return;}getText("/measure?cmd=start",800);}
    public void cancelMeasurement()throws Exception{RemoteConnection r=remote();if(r!=null){r.cancelMeasurement();return;}getText("/measure?cmd=cancel",800);}

    public void mqPower(boolean on)throws Exception{relayMask(on?1:0);}
    public void relay(int state)throws Exception{relayMask(state);}
    public void relayMask(int mask)throws Exception{if(mask!=0&&mask!=1)throw new IllegalArgumentException("MQ power state must be 0 or 1");RemoteConnection r=remote();if(r!=null){r.relayMask(mask);return;}getText("/relay?mask="+mask,800);}

    public void setAlertLevel(int severity)throws Exception{setAlertLevel(severity,3000);}
    public void setAlertLevel(int severity,int ttlMs)throws Exception{if(severity<0||severity>2)throw new IllegalArgumentException("Alert severity must be 0..2");int ttl=Math.max(500,Math.min(10000,ttlMs));RemoteConnection r=remote();if(r!=null){r.setAlertLevel(severity,ttl);return;}getText("/alert?severity="+severity+"&ttl="+ttl,800);}
    public void alertTest()throws Exception{RemoteConnection r=remote();if(r!=null){r.alertTest();return;}getText("/alert?cmd=test",800);}

    public void cameraFlash(String mode)throws Exception{RemoteConnection r=remote();if(r==null)throw new IllegalStateException("remote session unavailable");r.cameraFlash(mode);}
    private static String enc(String s)throws Exception{return URLEncoder.encode(s==null?"":s,StandardCharsets.UTF_8.name());}
}
