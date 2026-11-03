package com.servoc.app.report;

import android.database.Cursor;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.hardware.HazRobHardwareProfile;
import org.json.JSONObject;
import java.util.List;

/**
 * V8 supplemental, read-only XLSX sheets for ONE approved field measurement.
 *
 * Raw samples come directly from telemetry_samples ordered by (t_ms,id);
 * no interpolation, guessed zeros, replacement calibration, source
 * recomputation, or latest-device mixing. Every sheet includes site/session IDs.
 * The existing 14 core workbook sheets, 2 V6 research sheets and ALL PDF
 * templates and writers remain unchanged.
 */
final class V8FieldFullTelemetryXlsx {
    private V8FieldFullTelemetryXlsx(){}

    static void append(List<SmartXlsxPackage.Sheet> sheets, ExperimentsDb db,
                       FieldSessionReportExporter.Bundle b){
        if(sheets==null||db==null||b==null)
            throw new IllegalArgumentException("XLSX source required");
        sheets.add(new SmartXlsxPackage.Sheet("V8 البيئة الأصلية",environment(db,b)));
        sheets.add(new SmartXlsxPackage.Sheet("V8 ADC ADS الأصلية",adc(db,b)));
        sheets.add(new SmartXlsxPackage.Sheet("V8 القنوات المساعدة",auxiliary(db,b)));
        sheets.add(new SmartXlsxPackage.Sheet("V8 JSON كامل",rawArchive(db,b)));
    }

    private static SmartXlsxPackage.Grid sheet(FieldSessionReportExporter.Bundle b,
                                                String title,double... widths){
        SmartXlsxPackage.Grid g=new SmartXlsxPackage.Grid(widths);
        g.title(title);
        g.header("Site name","Site ID","Field session","Field session ID");
        g.row(b.site.name,b.site.id,b.fieldSession.name,b.fieldSession.id);
        g.blank();
        return g;
    }
    private static JSONObject parse(String raw){
        if(raw==null||raw.isEmpty())return new JSONObject();
        try{return new JSONObject(raw);}
        catch(org.json.JSONException error){
            throw new IllegalStateException("Archived telemetry JSON is corrupted",error);
        }
    }
    private static Object cell(JSONObject obj,String key){
        if(obj==null||!obj.has(key)||obj.isNull(key))return null;
        Object value=obj.opt(key);
        if(value instanceof Number){
            double numeric=((Number)value).doubleValue();
            return Double.isFinite(numeric)?value:null;
        }
        if(value instanceof Boolean||value instanceof String)return value;
        return value==null?null:value.toString();
    }
    private static Object any(JSONObject obj,String primary,String fallback){
        Object value=cell(obj,primary);
        return value==null?cell(obj,fallback):value;
    }
    private static String json(JSONObject obj){
        return obj==null?"":obj.toString();
    }
    private static String environment(ExperimentsDb db,FieldSessionReportExporter.Bundle b){
        SmartXlsxPackage.Grid g=sheet(b,"V8 / complete archived environmental channel data",
            35,14,20,22,23,19,19,18,18,18,18,18,18,18,18,22,40,90);
        g.rowStyled(SmartXlsxPackage.WARN,"المصدر",
            "كل صف من SQLite telemetry_samples؛ الخانة الفارغة تعني عدم توفر القراءة في العينة. "+
            "القيم البيئية ليست استنتاجًا من بيانات الحساسات الكيميائية.");
        g.header("Measurement ID","Telemetry row ID","t_ms","UTC ms",
            "Temperature °C","Humidity %RH","Pressure hPa","Altitude m",
            "Dew point °C","Heat index °C","BME ok","BME age ms",
            "Env sample_us","Sample seq","Clock ms","State","Transport JSON","Original env_payload JSON");
        for(String sid:b.measurementSessionIds){
            try(Cursor c=db.getReadableDatabase().rawQuery(
                "SELECT id,t_ms,env_payload,motion_payload FROM telemetry_samples "+
                "WHERE session_id=? ORDER BY t_ms,id",new String[]{sid})){
                while(c.moveToNext()){
                    JSONObject env=parse(c.getString(2)),state=parse(c.getString(3));
                    g.row(sid,c.getLong(0),c.getLong(1),cell(state,"sample_utc_ms"),
                        any(env,"temperature","t"),any(env,"humidity","h"),
                        any(env,"pressure","p"),cell(env,"altitude"),
                        cell(env,"dew_point"),cell(env,"heat_index"),
                        cell(env,"ok"),cell(env,"last_ok_age_ms"),
                        cell(env,"sample_us"),cell(state,"sample_seq"),
                        cell(state,"sample_monotonic_ms"),
                        cell(state,"measurement_state"),
                        json(state.optJSONObject("transport")),c.getString(2));
                }
            }
        }
        return g.finish();
    }
    private static String adc(ExperimentsDb db,FieldSessionReportExporter.Bundle b){
        SmartXlsxPackage.Grid g=sheet(b,"V8 / complete ADC, ADS1115 and original MQ channel results",
            36,15,14,18,14,18,18,18,18,18,19,18,20,18,18,18,18,18,18,20,18,19,25,42,80,80);
        g.rowStyled(SmartXlsxPackage.WARN,"المصدر",
            "raw = سجل التحويل الأصلي ADS إن توفر؛ adc/ao = الفولتية المسجلة. "+
            "لا تساوي القيم الفارغة صفرًا. كل صف مرتبط بحساس MQ وعينة أصلية محددة.");
        g.header("Measurement ID","Telemetry ID","t_ms","UTC ms","MQ channel",
            "ADC raw code","ADC voltage V","AO voltage V","Rs recorded",
            "R0 recorded","Rs/R0 recorded","Firmware R0","R0 source",
            "ADS index","ADS channel","Scan cycle","Sample us","Offset us",
            "Age us","MQ ok","MQ powered","ADS1 ready","ADS2 ready",
            "ADC acquisition JSON","Original MQ channel JSON","Original adc JSON");
        for(String sid:b.measurementSessionIds){
            try(Cursor c=db.getReadableDatabase().rawQuery(
                "SELECT id,t_ms,mq_payload,motion_payload FROM telemetry_samples "+
                "WHERE session_id=? ORDER BY t_ms,id",new String[]{sid})){
                while(c.moveToNext()){
                    JSONObject mq=parse(c.getString(2)),state=parse(c.getString(3));
                    JSONObject sourceAdc=state.optJSONObject("adc");
                    for(HazRobHardwareProfile.Channel channel:HazRobHardwareProfile.MQ){
                        JSONObject q=mq.optJSONObject(channel.key);
                        g.row(sid,c.getLong(0),c.getLong(1),cell(state,"sample_utc_ms"),
                            channel.key,cell(q,"raw"),cell(q,"adc"),cell(q,"ao"),
                            cell(q,"rs"),cell(q,"r0"),cell(q,"ratio"),
                            cell(q,"firmware_r0"),cell(q,"r0_source"),
                            cell(q,"ads"),cell(q,"ch"),cell(q,"scan_cycle"),
                            cell(q,"sample_us"),cell(q,"channel_offset_us"),
                            cell(q,"age_us"),cell(q,"ok"),cell(q,"powered"),
                            cell(sourceAdc,"ads1"),cell(sourceAdc,"ads2"),
                            sourceAdc==null?"":json(sourceAdc.optJSONObject("acquisition")),
                            json(q),json(sourceAdc));
                    }
                }
            }
        }
        return g.finish();
    }
    private static String auxiliary(ExperimentsDb db,FieldSessionReportExporter.Bundle b){
        SmartXlsxPackage.Grid g=sheet(b,"V8 / every archived non-MQ auxiliary signal and system state",
            38,16,16,20,18,14,14,16,16,18,18,18,18,18,18,18,18,20,24,55,60,65,65,65,55,60,60);
        g.rowStyled(SmartXlsxPackage.WARN,"المصدر",
            "قيم الدخان/الجزيئات والصوت و IR والريلاي واتجاهات الحركة والاتصال "+
            "تُنقل دون تعديل من motion_payload. غير المتصل أو غير المثبت يبقى فارغًا.");
        g.header("Measurement ID","Telemetry ID","t_ms","UTC ms","Sample seq",
            "Drive left","Drive right","Measure state","Measure phase",
            "Measure mask","Valid channels","Measure error","Failsafe","Retrace",
            "Safety auto stop","Forward hazard","Forward blocked","Forward reason",
            "Hardware profile","Particles JSON","Sound JSON","IR JSON","LDR JSON",
            "Relay JSON","Power JSON","Transport JSON","Original motion JSON");
        for(String sid:b.measurementSessionIds){
            try(Cursor c=db.getReadableDatabase().rawQuery(
                "SELECT id,t_ms,motion_payload FROM telemetry_samples "+
                "WHERE session_id=? ORDER BY t_ms,id",new String[]{sid})){
                while(c.moveToNext()){
                    JSONObject state=parse(c.getString(2));
                    g.row(sid,c.getLong(0),c.getLong(1),cell(state,"sample_utc_ms"),
                        cell(state,"sample_seq"),cell(state,"left"),
                        cell(state,"right"),cell(state,"measurement_state"),
                        cell(state,"measurement_phase"),cell(state,"measurement_mask"),
                        cell(state,"measurement_valid_channels"),cell(state,"measurement_error"),
                        cell(state,"failsafe"),cell(state,"retrace"),
                        cell(state,"safety_auto_stop"),cell(state,"forward_hazard"),
                        cell(state,"forward_blocked"),cell(state,"forward_block_reason"),
                        cell(state,"hardware_profile"),
                        json(state.optJSONObject("particles")),
                        json(state.optJSONObject("sound")),
                        json(state.optJSONObject("ir")),
                        json(state.optJSONObject("ldr")),
                        json(state.optJSONObject("relay")),
                        json(state.optJSONObject("sensor_power")),
                        json(state.optJSONObject("transport")),c.getString(2));
                }
            }
        }
        return g.finish();
    }
    private static String rawArchive(ExperimentsDb db,FieldSessionReportExporter.Bundle b){
        SmartXlsxPackage.Grid g=sheet(b,"V8 / full immutable SQLite sample payload per timepoint",
            36,16,15,20,24,110,130,130);
        g.rowStyled(SmartXlsxPackage.WARN,"المصدر",
            "النصوص الثلاثة مأخوذة حرفيًا من SQLite: env_payload / mq_payload / motion_payload. "+
            "لكل عينة معرّف وأزمنة مستقلة لتكرار التحليل خارجيًا دون إعادة تشكيل مصطنعة.");
        g.header("Measurement ID","Telemetry row ID","t_ms","UTC ms","Stored state",
            "Original environmental JSON","Original MQ JSON","Original auxiliary JSON");
        for(String sid:b.measurementSessionIds){
            try(Cursor c=db.getReadableDatabase().rawQuery(
                "SELECT id,t_ms,env_payload,mq_payload,motion_payload "+
                "FROM telemetry_samples WHERE session_id=? ORDER BY t_ms,id",
                new String[]{sid})){
                while(c.moveToNext()){
                    JSONObject state=parse(c.getString(4));
                    g.row(sid,c.getLong(0),c.getLong(1),cell(state,"sample_utc_ms"),
                        cell(state,"measurement_state"),c.getString(2),c.getString(3),
                        c.getString(4));
                }
            }
        }
        return g.finish();
    }
}
