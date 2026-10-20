package com.servoc.app.chemcore;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Presentation-only bridge. Does not run or gate any ChemCore technique.
 * Displays calculated screening-fit PPM when the separately reportable field is absent.
 */
public final class TankPpmEstimate {
    private TankPpmEstimate(){}
    public static final class Result {
        public final double ppm;
        public final boolean reportable;
        public final boolean screening;
        public final String method;
        private Result(double v,boolean accepted,boolean candidate,String methodName){
            ppm=v;reportable=accepted;screening=candidate;method=methodName;
        }
        public boolean available(){return Double.isFinite(ppm)&&ppm>=0;}
        public String qualityAr(){
            return !available()?"لا تتوفر قيمة PPM محسوبة":
                screening?"تقدير المرشح الأول من مطابقة المنحنيات":
                reportable?"كمية قابلة للتقرير":"تقدير حسابي";
        }
    }
    public static Result from(JSONObject result){
        if(result==null)return new Result(Double.NaN,false,false,"none");
        double direct=result.optDouble("ppm",Double.NaN);
        if(Double.isFinite(direct)&&direct>=0){
            return new Result(direct,result.optBoolean("ppm_reportable",false),false,"chemcore_ppm");
        }
        JSONArray candidates=result.optJSONArray("candidates");
        JSONObject first=candidates==null||candidates.length()==0?null:candidates.optJSONObject(0);
        double fit=first==null?Double.NaN:first.optDouble("screening_fit_ppm",Double.NaN);
        if(Double.isFinite(fit)&&fit>=0){
            return new Result(fit,false,true,"screening_fit_ppm");
        }
        return new Result(Double.NaN,false,false,"none");
    }
}
