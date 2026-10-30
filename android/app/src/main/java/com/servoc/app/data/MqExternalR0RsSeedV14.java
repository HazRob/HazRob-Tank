package com.servoc.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Research-only R0/Rs values measured or reported by OTHER MQ devices.
 *
 * These are sensor-instance/source-study observations; never values for a
 * chemical itself and NEVER a substitute for a current HazRob Tank baseline.
 * Disputed published values are retained with REJECTED status for audits.
 */
public final class MqExternalR0RsSeedV14 {
    public static final String VERSION="hazrob-tank-v6-peered-r0-rs-reference/1";
    public static final String ASSET=
        "chemcore_v6/reference/mq_external_r0_rs_peer_evidence_v14.json";
    private MqExternalR0RsSeedV14(){}

    private static String read(Context context)throws Exception{
        try(InputStream in=context.getAssets().open(ASSET);
            ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] b=new byte[8192];int n;
            while((n=in.read(b))!=-1)out.write(b,0,n);
            return new String(out.toByteArray(),StandardCharsets.UTF_8);
        }
    }
    private static void source(SQLiteDatabase db,JSONObject source){
        ContentValues v=new ContentValues();
        v.put("source_id",source.getClass()==null?"":source.optString("id",""));
        v.put("title",source.optString("title","Peer MQ R0 study"));
        v.put("authority","EXTERNAL_STUDY_RESEARCH_NOT_MANUFACTURER_CALIBRATION");
        v.put("url",source.optString("url",""));
        v.put("version",VERSION);
        v.put("evidence_tier","RESEARCH_ONLY");
        v.put("notes","Historical published sensor-instance R0/Rs. Explicit device and clean-air "+
            "factor definitions; never a Tank air R0 or specific gas ppm.");
        db.insertWithOnConflict("scientific_sources",null,v,SQLiteDatabase.CONFLICT_IGNORE);
    }
    public static int ensure(Context context,ExperimentsDb owner)throws Exception{
        if(context==null||owner==null)
            throw new IllegalArgumentException("Context and DB required");
        JSONObject root=new JSONObject(read(context));
        if(!VERSION.equals(root.optString("schema","")))
            throw new IllegalArgumentException("Wrong external peer evidence version");
        JSONArray catalog=root.getJSONArray("source_index");
        JSONArray observations=root.getJSONArray("observations");
        SQLiteDatabase db=owner.getWritableDatabase();
        int inserted=0;
        db.beginTransaction();
        try{
            for(int i=0;i<catalog.length();i++)
                source(db,catalog.getJSONObject(i));
            for(int i=0;i<observations.length();i++){
                JSONObject row=observations.getJSONObject(i);
                String study=row.optString("study_id","");
                String sensor=row.optString("sensor_model","");
                String instance=row.optString("instance","");
                double r0=row.optDouble("r0_kohm",Double.NaN);
                if(study.isEmpty()||sensor.isEmpty()||instance.isEmpty()||
                   !Double.isFinite(r0)||r0<=0)
                    throw new IllegalArgumentException("Missing source provenance row "+i);
                // A study and its own sensor-instance form a unique row;
                // duplicate paper summaries may not double-weight evidence.
                String key="study="+study+";sensor="+sensor+";instance="+instance;
                try(Cursor c=db.rawQuery(
                    "SELECT 1 FROM sensor_evidence WHERE chemical_uid IS NULL "+
                    "AND source_id=? AND sensor_key=? AND evidence_type=? "+
                    "AND equation=? LIMIT 1",
                    new String[]{study,sensor,"EXTERNAL_PEER_R0_RS",key})){
                    if(c.moveToFirst())continue;
                }
                boolean disputed="SOURCE_AUDIT_DISPUTED".equals(
                    row.optString("source_status",""));
                ContentValues v=new ContentValues();
                v.put("sensor_key",sensor);v.putNull("chemical_uid");
                v.put("evidence_type","EXTERNAL_PEER_R0_RS");
                v.put("equation",key);v.put("coefficients",row.toString());
                v.put("source_id",study);
                v.put("evidence_tier",disputed?"DISPUTED_RESEARCH_REFERENCE":
                        "SOURCE_AUDIT_RESEARCH_REFERENCE");
                v.put("status",disputed?"REJECTED_FOR_AUTOMATIC_CALIBRATION":
                        "REFERENCE_ONLY_NOT_TANK_CALIBRATION");
                long id=db.insertOrThrow("sensor_evidence",null,v);
                if(id>0)inserted++;
            }
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        return inserted;
    }
}
