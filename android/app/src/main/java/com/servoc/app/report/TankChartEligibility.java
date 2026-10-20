package com.servoc.app.report;

import com.servoc.app.data.SessionSeries;
import com.servoc.app.hardware.HazRobHardwareProfile;
import org.json.JSONArray;
import org.json.JSONObject;

/** UI only: hide unsupported graphs, never disables ChemCore calculation paths. */
public final class TankChartEligibility {
    private TankChartEligibility(){}
    public static boolean hasData(int chart,SessionSeries.Series series,JSONObject chemcore){
        if(chart<0||chart>19)return false;
        switch(chart){
            case 3:case 4:case 5:case 6:
                return rows(chemcore,"candidates")>0;
            case 7:case 8:case 9:
                return rows(chemcore==null?null:chemcore.optJSONObject("screening_144"),"rows")>0;
            case 10:{
                JSONObject g=chemcore==null?null:chemcore.optJSONObject("gas_exposure_ppm");
                return g!=null&&g.optBoolean("input_gate_passed",false)
                    &&(finite(g.optDouble("toxicity_priority_percent",Double.NaN))
                        ||finite(g.optDouble("percent_lfl",Double.NaN)));
            }
            default:break;
        }
        if(series==null||!series.usable())return false;
        if(chart>=16&&chart<=18){
            String key=chart==16?"temperature":chart==17?"humidity":"pressure";
            int n=0;for(SessionSeries.Sample s:series.samples){
                if(finite(SessionSeries.value(s.env,key))&&++n>=2)return true;
            }
            return false;
        }
        if(chart==19){
            int rms=0,pp=0;
            for(SessionSeries.Sample s:series.samples){
                JSONObject q=s.state.optJSONObject("sound");if(q==null)continue;
                if(finite(q.optDouble("rms_v",Double.NaN)))rms++;
                if(finite(q.optDouble("p2p_v",Double.NaN)))pp++;
                if(rms>=2||pp>=2)return true;
            }
            return false;
        }
        int[] valid=new int[HazRobHardwareProfile.MQ.length];
        for(SessionSeries.Sample s:series.samples){
            for(int i=0;i<valid.length;i++){
                JSONObject q=SessionSeries.sensor(s,HazRobHardwareProfile.MQ[i].key);
                if(q!=null&&positive(q.optDouble("ratio",Double.NaN)))valid[i]++;
            }
        }
        int active=0;
        for(int n:valid)if(n>=2)active++;
        if(chart==2)return active>=3; // radar across more than two channels
        return active>=1; // graphs may show partial MQ arrays truthfully
    }
    private static int rows(JSONObject o,String key){
        JSONArray a=o==null?null:o.optJSONArray(key);return a==null?0:a.length();
    }
    private static boolean finite(double v){return Double.isFinite(v);}
    private static boolean positive(double v){return finite(v)&&v>0;}
}
