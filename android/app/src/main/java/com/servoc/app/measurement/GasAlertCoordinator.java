package com.servoc.app.measurement;

import com.servoc.app.chemcore.ChemCoreLiveSensingEngine;
import org.json.JSONObject;

/**
 * V6 live-gas alarm boundary. Sensor drift and response are only engineering
 * diagnostics, never identified gas, certified PPM or a safety-grade gas alarm.
 *
 * No G3 Tank gas/threshold certification exists (0/80). Old V5 live chemical
 * inference is NOT invoked. Independent proximity/flame/e-stop indications
 * remain handled in MainActivity/firmware, never suppressed by this class.
 */
public final class GasAlertCoordinator {
    public static final int NONE=0,WARNING=1,DANGER=2;

    private LiveGasStateEngine live=new LiveGasStateEngine();
    // Compatibility-only state for V5 widgets that still expect this object.
    // Its historical identity-refinement code is not called by V6.
    private ChemCoreLiveSensingEngine legacyForUi=new ChemCoreLiveSensingEngine();
    private LiveGasStateEngine.Snapshot lastSnapshot;
    private double lastMaxDelta=Double.NaN;

    public synchronized int evaluate(JSONObject snapshot,SensorHealthEngine health,
                                     int warmupSec,long nowMs){
        if(snapshot==null||health==null)return clear();
        JSONObject relay=snapshot.optJSONObject("relay");
        boolean powered=relay!=null&&relay.optBoolean("on",relay.optInt("mask",0)!=0);
        long powerMs=relay==null?0:relay.optLong("power_ms",0);
        boolean warming=powered&&powerMs<Math.max(0,warmupSec)*1000L;
        // Preserve raw diagnostic state without upgrading it to gas identity.
        lastSnapshot=live.evaluate(health.snapshots(),powered,warming);
        lastMaxDelta=lastSnapshot.strongestAbsLogDelta;
        try{
            JSONObject diagnostic=new JSONObject();
            diagnostic.put("engine","HAZROB_TANK_V6");
            diagnostic.put("classification","UNKNOWN");
            diagnostic.put("reported_identity","UNKNOWN");
            diagnostic.put("reportable_ppm",false);
            diagnostic.put("physical_tank_g3_confirmed",false);
            diagnostic.put("safety_grade_gas_alarm",false);
            diagnostic.put("operational_gas_alert_level",NONE);
            diagnostic.put("r0_clean_air_not_certified",true);
            diagnostic.put("response_state_for_research_only",lastSnapshot.state.name());
            diagnostic.put("response_reason",lastSnapshot.reason);
            snapshot.put("live_chemcore",diagnostic);
        }catch(Exception ignored){}
        // A deviation of Rs from a self-referenced baseline cannot establish
        // hazardous ppm or even confirm gas identity: no audible/visual gas
        // hazard is triggered by this empirical-noise-unqualified monitor.
        return NONE;
    }
    public synchronized int level(){return NONE;}
    public synchronized double maxDelta(){return lastMaxDelta;}
    public synchronized LiveGasStateEngine.Snapshot liveSnapshot(){return lastSnapshot;}
    public synchronized ChemCoreLiveSensingEngine.Snapshot chemCoreSnapshot(){
        return legacyForUi.snapshot();
    }
    public synchronized void refreshChemCoreCandidates(){
        // No predecessor ChemCore path in Tank V6 live monitoring.
        // Dedicated TankInstantAnalyzer computes isolated V6 hypotheses.
    }
    public synchronized int reset(){return clear();}
    private int clear(){
        lastSnapshot=null;lastMaxDelta=Double.NaN;
        live=new LiveGasStateEngine();
        legacyForUi=new ChemCoreLiveSensingEngine();
        return NONE;
    }
}
