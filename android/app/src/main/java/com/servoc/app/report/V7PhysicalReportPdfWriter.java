package com.servoc.app.report;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import com.servoc.app.chemical.GhsPictograms;
import com.servoc.app.measurements.PhysicalCatalog;
import org.json.JSONObject;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Native Android V7 PDF for a single frozen physical measurement.
 *
 * Pure presentation: all 15 PhysicalCatalog measurements remain visible
 * (including PM1/2.5/10). Never computes barometric altitude, heat index,
 * ambient dose, occupational OEL, exceedance, or a time-series from a snapshot.
 * Only an explicitly transmitted value and engine-owned status may be printed.
 * No bogus zero, graph series, thresholds or artificial trends are produced.
 */
public final class V7PhysicalReportPdfWriter {
    private V7PhysicalReportPdfWriter() {}
    private static final float M=V7ReportLayoutKit.mm(5);
    private static final float CONTENT_TOP=V7ReportLayoutKit.mm(27);
    private static final float CONTENT_BOTTOM=V7ReportLayoutKit.A4_PORTRAIT_H-
        V7ReportLayoutKit.mm(20);
    private static final float CONTENT_WIDTH=V7ReportLayoutKit.A4_PORTRAIT_W-2*M;
    private static final float HEADER_H=41f, ROW_H=62f, CHART_H=320f;
    private static final String[] ICONS={
        "T","RH","P","LUX","LM","dB","V","RAD","UV","P1","P2","P10","WS","WD","ALT"
    };
    private static final String[] KEYS={
        "temperature","humidity","pressure","illuminance","luminous_flux",
        "noise","vibration","ionizing_radiation","uv_index","pm1",
        "pm2_5","pm10","wind_speed","wind_direction","altitude"
    };
    private static final String[] THERMAL_KEYS={
        "heat_index","dew_point","thermal_burden"
    };
    private static final String[] THERMAL_AR={
        "مؤشر الحرارة","نقطة الندى","عبء الحرارة"
    };
    private static final String[] THERMAL_EN={
        "Heat Index","Dew Point","Thermal Burden"
    };
    private static final String[] CHART_TITLES={
        "الحرارة والرطوبة / Temperature & Humidity",
        "الجسيمات PM1 وPM2.5 وPM10 / Particles",
        "الضوضاء عبر الزمن / Noise over time",
        "مقارنات المؤشرات البيئية / Environmental Comparison"
    };
    private static final String[] CHART_X={
        "Time","Time","Time","Measurement"
    };
    private static final String[] CHART_Y={
        "T / RH","µg/m³","dB","Value"
    };
    private static final String[] GROUPS={
        "جميع المؤشرات الفيزيائية والجسيمات / Physical & PM",
        "المؤشرات الحرارية المستمدة من المصدر / Engine-derived thermal",
        "مساحات المخططات التحليلية / Reserved analytic charts"
    };

    private static final class Reading {
        String value="-", origin="-";
        boolean exceeded=false;
    }
    private static JSONObject group(JSONObject base,String key) {
        return base==null?null:base.optJSONObject(key);
    }
    private static JSONObject firstGroup(JSONObject base,String... keys) {
        for(String k:keys) {
            JSONObject found=group(base,k);
            if(found!=null)return found;
        }
        return null;
    }
    private static JSONObject metricObject(JSONObject snapshot,String key){
        JSONObject physical=firstGroup(snapshot,"physical_metrics","physical",
            "measurements","physical_readings");
        return physical==null?null:physical.optJSONObject(key);
    }
    private static Reading readNumber(JSONObject o,String key,int decimals,String source){
        Reading r=new Reading();
        if(o==null)return r;
        // Values in an engine result are either numeric directly or in {value,status}.
        JSONObject embedded=o.optJSONObject(key);
        double val=embedded==null?o.optDouble(key,Double.NaN):
            embedded.optDouble("value",Double.NaN);
        if(!Double.isFinite(val))return r;
        r.value=String.format(Locale.US,"%."+decimals+"f",val);
        r.origin=embedded!=null?embedded.optString("source",source):source;
        if(r.origin==null||r.origin.trim().isEmpty())r.origin=source;
        if(embedded!=null)r.exceeded=embedded.optBoolean("exceeded",false);
        return r;
    }
    private static Reading fromEngine(JSONObject snap,String key){
        JSONObject base=firstGroup(snap,"physical_metrics","physical",
            "measurements","physical_readings");
        return readNumber(base,key,2,"Physical source");
    }
    private static Reading observed(JSONObject snap,int index){
        String key=KEYS[index];
        Reading r=fromEngine(snap,key);
        if(!"-".equals(r.value))return r;
        JSONObject e=group(snap,"env");
        JSONObject a=group(snap,"sound");
        JSONObject pm=firstGroup(snap,"particles","pm","particulate");
        JSONObject light=firstGroup(snap,"light","illuminance");
        JSONObject wind=group(snap,"wind");
        JSONObject u=group(snap,"uv");
        JSONObject rad=group(snap,"radiation");
        switch(index){
            case 0:return readNumber(e,"temperature",2,"env.temperature");
            case 1:return readNumber(e,"humidity",2,"env.humidity");
            case 2:return readNumber(e,"pressure",2,"env.pressure");
            case 3:return readNumber(light,"lux",2,"light.lux");
            case 4:return readNumber(light,"luminous_flux",2,"light.luminous_flux");
            case 5:
                r=readNumber(a,"dba",1,"sound.dba");
                if(!"-".equals(r.value)) {
                    r.origin=a.optBoolean("calibrated_dba",false)?
                        "dBA calibrated":"dBA uncalibrated";
                    return r;
                }
                return readNumber(a,"db_spl",1,"sound.db_spl uncalibrated");
            case 6:return readNumber(group(snap,"vibration"),"value",2,"vibration");
            case 7:return readNumber(rad,"usv_h",3,"radiation.usv_h");
            case 8:return readNumber(u,"uvi",2,"uv.uvi");
            case 9:
                r=readNumber(pm,"pm1",2,"particles.pm1");
                if("-".equals(r.value))r=readNumber(pm,"pm_1",2,"particles.pm_1");
                return r;
            case 10:
                r=readNumber(pm,"pm2_5",2,"particles.pm2_5");
                if("-".equals(r.value))r=readNumber(pm,"pm25",2,"particles.pm25");
                return r;
            case 11:return readNumber(pm,"pm10",2,"particles.pm10");
            case 12:return readNumber(wind,"speed",2,"wind.speed");
            case 13:return readNumber(wind,"direction",1,"wind.direction");
            case 14:
                // Altitude must be transmitted by its source, never calculated in PDF.
                return readNumber(e,"altitude",2,"env.altitude");
            default:return r;
        }
    }
    private static Reading thermal(JSONObject snap,int index){
        return fromEngine(snap,THERMAL_KEYS[index]);
    }
    private static void box(Canvas canvas,float x,float y,float w,float h,
                            int fill,int stroke){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);p.setColor(fill);
        canvas.drawRect(x,y,x+w,y+h,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.65f);p.setColor(stroke);
        canvas.drawRect(x,y,x+w,y+h,p);
    }
    private static void label(Canvas c,String s,float x,float y,float w,float h,
                              boolean rtl,boolean bold,int color){
        V7ReportLayoutKit.text(c,s,new RectF(x,y,x+w,y+h),rtl,bold,12,color);
    }
    private static void section(Canvas c,String sectionId,float y){
        int index="readings".equals(sectionId)?0:"thermal".equals(sectionId)?1:2;
        box(c,M,y,CONTENT_WIDTH,HEADER_H,V7ReportLayoutKit.AMBER,
            V7ReportLayoutKit.BLACK);
        label(c,GROUPS[index],M+8,y+7,CONTENT_WIDTH-16,HEADER_H-10,
            true,true,V7ReportLayoutKit.BLACK);
    }
    private static void icon(Canvas c,String icon,float x,float y){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(V7ReportLayoutKit.INK);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.25f);
        c.drawCircle(x+18,y+30,18,p);
        label(c,icon,x+2,y+23,32,20,false,true,V7ReportLayoutKit.INK);
    }
    private static void sensorRow(Canvas c,int idx,float y,Reading r){
        box(c,M,y,CONTENT_WIDTH,ROW_H,Color.WHITE,V7ReportLayoutKit.BLACK);
        float iconStart=M+CONTENT_WIDTH-47;
        icon(c,ICONS[idx],iconStart,y);
        float namesX=M+CONTENT_WIDTH-292;
        label(c,PhysicalCatalog.NAMES_AR[idx],namesX+6,y+5,231,25,
            true,true,V7ReportLayoutKit.INK);
        label(c,PhysicalCatalog.NAMES[idx],namesX+6,y+29,231,22,
            false,false,V7ReportLayoutKit.INK);
        float vX=M+145,vW=116;
        V7ReportLayoutKit.measuredValue(c,new RectF(vX,y+4,vX+vW,y+ROW_H-4),
            r.value,false,r.exceeded);
        label(c,PhysicalCatalog.UNITS[idx],vX+5,y+ROW_H-21,102,18,
            false,false,r.exceeded?Color.WHITE:V7ReportLayoutKit.INK);
        label(c,r.origin,M+6,y+8,135,ROW_H-12,
            false,false,V7ReportLayoutKit.INK);
    }
    private static void thermalRow(Canvas c,int idx,float y,Reading r){
        box(c,M,y,CONTENT_WIDTH,ROW_H,Color.WHITE,V7ReportLayoutKit.BLACK);
        label(c,THERMAL_AR[idx],M+294,y+4,255,25,true,true,V7ReportLayoutKit.INK);
        label(c,THERMAL_EN[idx],M+294,y+31,255,22,false,false,V7ReportLayoutKit.INK);
        V7ReportLayoutKit.tableValue(c,new RectF(M+157,y+4,M+280,y+ROW_H-4),
            r.value,false);
        label(c,r.origin,M+7,y+10,135,ROW_H-12,false,false,V7ReportLayoutKit.INK);
    }
    private static List<V7ReportPagePlanner.Section> sections(){
        List<V7ReportPagePlanner.Item> rs=new ArrayList<>();
        for(int i=0;i<PhysicalCatalog.COUNT;i++)
            rs.add(new V7ReportPagePlanner.Item("reading-"+i,ROW_H));
        List<V7ReportPagePlanner.Item> dt=new ArrayList<>();
        for(int i=0;i<THERMAL_KEYS.length;i++)
            dt.add(new V7ReportPagePlanner.Item("thermal-"+i,ROW_H));
        List<V7ReportPagePlanner.Item> plots=new ArrayList<>();
        for(int i=0;i<CHART_TITLES.length;i++)
            plots.add(new V7ReportPagePlanner.Item("chart-"+i,CHART_H));
        return Arrays.asList(new V7ReportPagePlanner.Section("readings",HEADER_H,rs),
            new V7ReportPagePlanner.Section("thermal",HEADER_H,dt),
            new V7ReportPagePlanner.Section("charts",HEADER_H,plots));
    }
    public static void write(OutputStream output,JSONObject snapshot,
                             String measuredTime)throws Exception{
        if(output==null||snapshot==null)
            throw new IllegalArgumentException("Frozen physical snapshot required");
        List<V7ReportPagePlanner.Page> pages=
            V7ReportPagePlanner.plan(CONTENT_TOP,CONTENT_BOTTOM,sections());
        Context context=GhsPictograms.applicationContext();
        PdfDocument pdf=new PdfDocument();
        try {
            for(V7ReportPagePlanner.Page placement:pages){
                PdfDocument.Page page=V7ReportLayoutKit.open(pdf,placement.number,false);
                Canvas canvas=page.getCanvas();canvas.drawColor(Color.WHITE);
                V7ReportLayoutKit.drawHeader(canvas,context,false,
                    "تقرير القياسات الفيزيائية والجسيمات","Physical & Particle Report",
                    measuredTime);
                for(V7ReportPagePlanner.Element e:placement.elements){
                    if(e.sectionHeader) {
                        section(canvas,e.sectionId,e.topPt);
                    } else if(e.itemId.startsWith("reading-")){
                        int i=Integer.parseInt(e.itemId.substring(8));
                        sensorRow(canvas,i,e.topPt,observed(snapshot,i));
                    } else if(e.itemId.startsWith("thermal-")){
                        int i=Integer.parseInt(e.itemId.substring(8));
                        thermalRow(canvas,i,e.topPt,thermal(snapshot,i));
                    } else if(e.itemId.startsWith("chart-")){
                        int i=Integer.parseInt(e.itemId.substring(6));
                        V7ReportLayoutKit.blankChart(canvas,
                            new RectF(M,e.topPt+4,M+CONTENT_WIDTH,e.topPt+e.heightPt-4),
                            CHART_TITLES[i],CHART_X[i],CHART_Y[i]);
                    }
                }
                V7ReportLayoutKit.drawFooter(canvas,false,placement.number,pages.size());
                pdf.finishPage(page);
            }
            pdf.writeTo(output);
        } finally {pdf.close();}
    }
}
