package com.hazrob.tank.emissions;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import com.servoc.app.report.V7ReportLayoutKit;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Styled source-traceable report for the selected registered engine only. */
public final class EmissionReportPdf {
    private EmissionReportPdf(){}
    private static final float MM=V7ReportLayoutKit.mm(1);
    private static final float LEFT=5*MM,RIGHT=V7ReportLayoutKit.A4_PORTRAIT_W-LEFT;
    private static RectF r(float top,float bottom){
        return new RectF(LEFT,top*MM,RIGHT,bottom*MM);
    }
    private static void info(Canvas c,String text,float top,float bottom,boolean bold){
        V7ReportLayoutKit.text(c,text,
            new RectF(LEFT+4*MM,top*MM,RIGHT-4*MM,bottom*MM),
            false,bold,12,V7ReportLayoutKit.INK);
    }
    private static void pageOne(Canvas c,Context ctx,String engine,JSONObject scan,int count){
        V7ReportLayoutKit.drawHeader(c,ctx,false,
            "تقرير قياس انبعاثات المحركات","Engine Emissions Measurement",
            new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(
                new Date(scan.optLong("measured_at_ms",0))));
        V7ReportLayoutKit.drawFooter(c,false,1,3);
        V7ReportLayoutKit.tableHeader(c,r(27,42),"المحرك المحدد / Engine name: "+engine,true);
        info(c,"Measurement sessions stored: "+count+
            "   |  Latest captured frames: "+scan.optInt("frames")+
            "   |  Duration: "+String.format(Locale.US,"%.1f",
                scan.optLong("duration_ms",0)/1000.0)+" s",46,60,false);
        info(c,"Analysis status: "+scan.optString("mode","PENDING_ENGINE_SELECTION"),
            61,76,true);
        info(c,"IFC status: "+scan.optString("ifc_assessment_state","NOT_EVALUABLE"),
            77,95,false);
        V7ReportLayoutKit.tableHeader(c,r(99,111),
            "ملوث / Pollutant                       Concentration                    IFC reference",true);
        JSONArray rows=scan.optJSONArray("pollutants");
        int n=rows==null?0:rows.length();
        for(int i=0;i<n;i++){
            JSONObject data=rows.optJSONObject(i);if(data==null)continue;
            float top=115+i*17;
            float bot=top+17;
            float a=LEFT,b=LEFT+63*MM,d=LEFT+133*MM;
            V7ReportLayoutKit.tableValue(c,new RectF(a,top*MM,b,bot*MM),
                data.optString("label"),false);
            boolean numeric=!data.isNull("value_ppm")&&data.has("value_ppm");
            boolean red=numeric&&data.optBoolean("ifc_comparable",false)&&
                data.optBoolean("exceeded",false);
            V7ReportLayoutKit.measuredValue(c,new RectF(b,top*MM,d,bot*MM),
                numeric?data.optString("value_ppm")+" "+data.optString("unit","ppm"):
                    "NOT_CALIBRATED",false,red);
            V7ReportLayoutKit.tableValue(c,new RectF(d,top*MM,RIGHT,bot*MM),
                data.isNull("limit")?"REFERENCE_PENDING":
                    data.optString("limit")+" "+data.optString("limit_unit",""),false);
        }
        info(c,"Red cells require a real measured value, a valid comparable IFC limit, "+
            "and a verified exceedance. No unverified reading is colored red.",
            259,277,false);
    }
    private static void pageTwo(Canvas c,Context ctx,String engine,JSONObject scan){
        V7ReportLayoutKit.drawHeader(c,ctx,false,
            "مرفق الدليل الفني للانبعاثات","Source and Instrument Diagnostic",
            new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(
                new Date(scan.optLong("measured_at_ms",0))));
        V7ReportLayoutKit.drawFooter(c,false,2,3);
        V7ReportLayoutKit.tableHeader(c,r(27,44),"المحرك / Engine: "+engine,true);
        info(c,"MQ values below are captured mean source resistance signals, "+
            "not pollutant concentration in ppm or mg/Nm3.",47,67,true);
        JSONObject raw=scan.optJSONObject("mq_average_rs");
        String[] sensors={"MQ2","MQ3","MQ4","MQ5","MQ7","MQ8","MQ9","MQ135"};
        for(int i=0;i<sensors.length;i++){
            float top=72+i*16;
            String sensor=sensors[i];
            String value=raw!=null&&raw.has(sensor)?
                String.format(Locale.US,"%.6g",raw.optDouble(sensor)):"NOT_CAPTURED";
            V7ReportLayoutKit.tableValue(c,new RectF(LEFT,top*MM,RIGHT,(top+16)*MM),
                sensor+"    |    Mean Tank Rs (device units): "+value,false);
        }
        V7ReportLayoutKit.tableHeader(c,r(206,223),
            "حدود التطبيق / Qualification and source restrictions",true);
        info(c,"SOx, NOx, CO2 have no validated selective sensor in the current Tank. "+
            "MQ4 CH4 and MQ2 smoke values are cross-sensitive research signals only.",
            225,245,false);
        info(c,"Particulates: "+(scan.optBoolean("include_particles")?
            "requested, hardware channel pending":"not requested")+
            ". Specific IFC sector, engine size, fuel, units and reference conditions "+
            "must be qualified before any emissions-compliance comparison.",
            249,278,false);
    }
    /** Full research model ledger for only the five pollutant families selected.
     * This does NOT claim emission concentrations or IFC compliance.
     */
    private static void pageThree(Canvas c,Context ctx,String engine,JSONObject scan){
        V7ReportLayoutKit.drawHeader(c,ctx,false,
            "مخرجات ChemCore Next البحثية","ChemCore Next Source Evidence",
            new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(
                new Date(scan.optLong("measured_at_ms",0))));
        V7ReportLayoutKit.drawFooter(c,false,3,3);
        V7ReportLayoutKit.tableHeader(c,r(27,43),
            "مصدر التحليل / Frozen Next provenance",true);
        info(c,"Engine: "+engine+" | Next status: "+
            scan.optString("next_status","NOT_RUN"),45,58,false);
        info(c,"Next decision: "+scan.optString("next_decision_uid","NOT_RUN")+
            " | Identity: "+scan.optString("next_decision_status","UNKNOWN"),60,75,false);
        info(c,"Stable manifest SHA256: "+
            scan.optString("stable_manifest_sha256","UNAVAILABLE"),77,93,false);
        V7ReportLayoutKit.tableHeader(c,r(96,109),
            "مقارنات المنحنيات البحثية / Study curve source hypotheses",true);
        JSONArray pollutants=scan.optJSONArray("pollutants");
        int n=0;
        if(pollutants!=null)for(int i=0;i<pollutants.length();i++){
            JSONObject pollutant=pollutants.optJSONObject(i);
            if(pollutant==null)continue;
            JSONArray list=pollutant.optJSONArray("next_source_hypotheses");
            if(list==null)continue;
            for(int k=0;k<list.length()&&n<13;k++){
                JSONObject fit=list.optJSONObject(k);if(fit==null)continue;
                float top=112+12*n;
                String ppm=fit.isNull("research_estimate_ppm")?"NOT_EVALUABLE":
                    String.format(Locale.US,"%.5g research ppm",
                        fit.optDouble("research_estimate_ppm",Double.NaN));
                V7ReportLayoutKit.tableValue(c,r(top,top+12),
                    pollutant.optString("pollutant")+" / "+
                    fit.optString("mq_sensor")+" / "+
                    fit.optString("source_curve_id")+" / "+ppm,false);
                n++;
            }
        }
        if(n==0)info(c,"No qualified or numerically evaluable source curves "+
            "were generated for the selected emissions.",118,148,false);
        info(c,"Research fits reflect alternative source-gas hypotheses. "+
            "They are NOT validated exhaust concentrations or IFC limits. "+
            "SOx/NOx and PM require dedicated instruments and reference methods.",
            275,291,false);
    }

    public static void write(Context ctx,OutputStream output,String name,JSONObject scan,
                             int measurementCount)throws Exception{
        if(ctx==null||output==null||scan==null||name==null)
            throw new IllegalArgumentException("Missing frozen engine emissions");
        PdfDocument pdf=new PdfDocument();
        try{
            for(int i=1;i<=3;i++){
                PdfDocument.Page page=V7ReportLayoutKit.open(pdf,i,false);
                Canvas c=page.getCanvas();c.drawColor(Color.WHITE);
                if(i==1)pageOne(c,ctx,name,scan,measurementCount);
                else if(i==2)pageTwo(c,ctx,name,scan);
                else pageThree(c,ctx,name,scan);
                pdf.finishPage(page);
            }
            pdf.writeTo(output);
        }finally{pdf.close();}
    }
    public static Uri save(Context ctx,String engine,JSONObject scan,int count)throws Exception{
        if(Build.VERSION.SDK_INT<29)
            throw new UnsupportedOperationException("Android 10+ MediaStore required");
        ContentValues values=new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME,
            "HazRob-Emissions-"+scan.optLong("engine_id")+"-"+
            scan.optLong("measured_at_ms")+".pdf");
        values.put(MediaStore.MediaColumns.MIME_TYPE,"application/pdf");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH,
            Environment.DIRECTORY_DOWNLOADS+"/HazRobTank/");
        Uri uri=ctx.getContentResolver().insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
        if(uri==null)throw new IOException("Emissions PDF target unavailable");
        try(OutputStream output=ctx.getContentResolver().openOutputStream(uri)){
            if(output==null)throw new IOException("Emissions PDF stream unavailable");
            write(ctx,output,engine,scan,count);return uri;
        }catch(Exception error){
            ctx.getContentResolver().delete(uri,null,null);throw error;
        }
    }
}
