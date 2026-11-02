package com.servoc.app.chemcore.nextintegration;

import android.os.SystemClock;
import com.hazrob.tank.chemcore.next.CoreContracts;
import com.hazrob.tank.chemcore.next.NextTelemetryMapper;
import com.servoc.app.calibration.ManualR0Calibration;
import com.servoc.app.hardware.HazRobHardwareProfile;
import java.util.*;
import org.json.JSONObject;

/**
 * V8 strictly read-only boundary. Captures one already received immutable
 * /data snapshot; never sends firmware or relay commands; never fabricates
 * R0_source, a source-specific ppm, or hardware readings for absent channels.
 */
public final class NextAndroidAdapter {
  private NextAndroidAdapter(){}
  private static Double finite(JSONObject obj,String name){
    if(obj==null||!obj.has(name)||obj.isNull(name))return null;
    double x=obj.optDouble(name,Double.NaN);
    return Double.isFinite(x)?x:null;
  }
  private static int rawAdc(JSONObject src){
    if(src==null||!src.has("raw")||src.isNull("raw"))return -1;
    double v=src.optDouble("raw",Double.NaN);
    return Double.isFinite(v)&&v>=0&&v<=65535&&v==Math.floor(v)?(int)v:-1;
  }
  public static NextTelemetryMapper.Frozen freeze(JSONObject original,
      String snapshotUid,String verifiedStablePayloadSha256,CoreContracts.Mode mode) throws org.json.JSONException{
    if(original==null)throw new IllegalArgumentException("actual robot /data required");
    final JSONObject data=new JSONObject(original.toString());
    String deviceUid=data.optString("device_id","").trim();
    if(deviceUid.isEmpty())throw new IllegalArgumentException("Tank hardware device_id missing");
    JSONObject relay=data.optJSONObject("relay");
    boolean relayOn=relay!=null&&relay.optBoolean("on",relay.optInt("mask",0)!=0);
    JSONObject mq=data.optJSONObject("mq"),env=data.optJSONObject("env");
    List<NextTelemetryMapper.Channel> readings=new ArrayList<>();
    for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ){
      JSONObject x=mq==null?null:mq.optJSONObject(hw.key);
      if(x==null)continue;
      String source=ManualR0Calibration.getSource(hw.key);
      String referenceDevice=ManualR0Calibration.getDeviceId(hw.key);
      String definition=ManualR0Calibration.getDefinition(hw.key);
      int windowCount=ManualR0Calibration.getWindowCount(hw.key);
      int samples=ManualR0Calibration.getSamples(hw.key);
      // Never accept an arbitrary 'r0' / 'ratio' carried in telemetry: the
      // original firmware could have a different (source/factor-scaled) R0.
      Double deviceRAir=ManualR0Calibration.has(hw.key)?
        ManualR0Calibration.get(hw.key):null;
      String status=x.optString("status","").trim();
      if(status.isEmpty()){
        JSONObject quality=x.optJSONObject("quality");
        status=quality==null?"":quality.optString("status","");
      }
      int raw=rawAdc(x);
      Double voltage=finite(x,"adc");
      if(voltage==null)voltage=finite(x,"vadc");
      readings.add(new NextTelemetryMapper.Channel(hw.key,
        raw<0?null:raw,voltage,finite(x,"rs"),deviceRAir,
        x.optBoolean("ok",x.optBoolean("valid",false)),
        !x.has("powered")||x.optBoolean("powered",false),
        x.optLong("last_ok_age_ms",0),status,source,definition,
        referenceDevice,ManualR0Calibration.getSession(hw.key),
        ManualR0Calibration.getUtc(hw.key),samples,windowCount,
        HazRobHardwareProfile.manufacturerHeaterProtocolValid(hw.sensorId)));
    }
    String firmware=data.optString("fw",data.optString("firmware","UNKNOWN_UNVERIFIED_BUILD"));
    Double t=finite(env,"t");if(t==null)t=finite(env,"temperature");
    Double h=finite(env,"h");if(h==null)h=finite(env,"humidity");
    return NextTelemetryMapper.freeze(new NextTelemetryMapper.Input(
      snapshotUid,deviceUid,HazRobHardwareProfile.ID,firmware,
      verifiedStablePayloadSha256,mode,SystemClock.elapsedRealtime(),t,h,
      relayOn,readings));
  }
}
