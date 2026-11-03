package com.hazrob.tank.emissions;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * One engine = one optional frozen emission measurement, never a time-series
 * list of sessions. One-to-one relation is enforced by SQLite UNIQUE engine_id.
 * Re-acquiring for the SAME engine requires an explicit replacement operation.
 */
public final class EmissionSessionStore extends SQLiteOpenHelper {
    private static final String DB="hazrob_emissions_v1.db";
    public static final class Engine {
        public final long id;
        public final String name,ifcProfile;
        public final EmissionEngineConfig config;
        public Engine(long id,String name,String profile,EmissionEngineConfig config){
            this.id=id;this.name=name;this.ifcProfile=profile;
            this.config=config==null?EmissionEngineConfig.empty():config;
        }
    }
    public EmissionSessionStore(Context context){
        super(context.getApplicationContext(),DB,null,3);
    }
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE emission_engines ("+
            "id INTEGER PRIMARY KEY AUTOINCREMENT,"+
            "name TEXT NOT NULL UNIQUE,created_ms INTEGER NOT NULL,"+
            "ifc_profile TEXT NOT NULL DEFAULT 'NOT_CONFIGURED',"+
            "engine_config_json TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE emission_measurements ("+
            "id INTEGER PRIMARY KEY AUTOINCREMENT,"+
            "engine_id INTEGER NOT NULL UNIQUE,at_ms INTEGER NOT NULL,"+
            "results_json TEXT NOT NULL,"+
            "FOREIGN KEY(engine_id) REFERENCES emission_engines(id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX emission_measurements_by_engine ON emission_measurements(engine_id,at_ms DESC)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldV,int newV){
        if(oldV<2)db.execSQL("ALTER TABLE emission_engines ADD COLUMN ifc_profile TEXT NOT NULL DEFAULT 'NOT_CONFIGURED'");
        if(oldV<3){
            db.execSQL("ALTER TABLE emission_engines ADD COLUMN engine_config_json TEXT NOT NULL DEFAULT ''");
            // Migrate older many-measurement records without sacrificing the
            // most recent genuine result: retain highest (at_ms,id) per engine.
            db.execSQL("DELETE FROM emission_measurements WHERE id NOT IN ("+
                "SELECT m.id FROM emission_measurements m WHERE m.id=("+
                "SELECT s.id FROM emission_measurements s "+
                "WHERE s.engine_id=m.engine_id "+
                "ORDER BY s.at_ms DESC,s.id DESC LIMIT 1))");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS emission_one_measurement_per_engine "+
                "ON emission_measurements(engine_id)");
        }
    }
    @Override public void onConfigure(SQLiteDatabase db){
        super.onConfigure(db);db.setForeignKeyConstraintsEnabled(true);
    }
    public long create(String name){
        return create(name,EmissionEngineConfig.empty());
    }
    public long create(String name,EmissionEngineConfig config){
        if(name==null||name.trim().isEmpty()||name.trim().length()>80)
            throw new IllegalArgumentException("INVALID_ENGINE_NAME");
        if(config==null)throw new IllegalArgumentException("MISSING_ENGINE_DEFINITION");
        ContentValues v=new ContentValues();
        v.put("name",name.trim());v.put("created_ms",System.currentTimeMillis());
        v.put("ifc_profile",config.profile());
        try{v.put("engine_config_json",config.toJson().toString());}
        catch(Exception invalid){throw new IllegalArgumentException("INVALID_CONFIG",invalid);}
        return getWritableDatabase().insertOrThrow("emission_engines",null,v);
    }
    public List<Engine> engines(){
        List<Engine> items=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery(
            "SELECT id,name,ifc_profile,engine_config_json FROM emission_engines "+
            "ORDER BY created_ms DESC,id DESC",null)){
            while(c.moveToNext()){
                EmissionEngineConfig config=EmissionEngineConfig.fromJson(c.getString(3));
                items.add(new Engine(c.getLong(0),c.getString(1),c.getString(2),config));
            }
        }
        return items;
    }
    public void updateDefinition(long engineId,String name,EmissionEngineConfig config){
        if(engineId<=0||config==null||name==null||name.trim().isEmpty()||
           name.trim().length()>80)throw new IllegalArgumentException("INVALID_DEFINITION");
        ContentValues v=new ContentValues();
        v.put("name",name.trim());
        v.put("ifc_profile",config.profile());
        try{v.put("engine_config_json",config.toJson().toString());}
        catch(Exception err){throw new IllegalArgumentException("INVALID_CONFIG",err);}
        if(getWritableDatabase().update("emission_engines",v,"id=?",
            new String[]{Long.toString(engineId)})!=1)
            throw new IllegalArgumentException("ENGINE_NOT_FOUND");
    }
    /** Legacy method: does not invent bore/facility readings for new definitions. */
    @Deprecated public void setIfcProfile(long engineId,String profile){
        if(engineId<=0||!EmissionIfcComparator.knownProfile(profile))
            throw new IllegalArgumentException("UNKNOWN_IFC_PROFILE");
        ContentValues v=new ContentValues();v.put("ifc_profile",profile);
        if(getWritableDatabase().update("emission_engines",v,"id=?",
            new String[]{Long.toString(engineId)})!=1)
            throw new IllegalArgumentException("ENGINE_NOT_FOUND");
    }
    public void delete(long id){
        if(id<=0)throw new IllegalArgumentException("INVALID_ENGINE");
        getWritableDatabase().delete("emission_engines","id=?",new String[]{Long.toString(id)});
    }
    /** First measurement only. An existing result must NEVER silently be overwritten. */
    public long addMeasurement(long engineId,JSONObject report){
        return writeSingle(engineId,report,false);
    }
    /** Must only be invoked by UI after explicit remeasurement confirmation. */
    public long replaceMeasurement(long engineId,JSONObject report){
        return writeSingle(engineId,report,true);
    }
    private long writeSingle(long engineId,JSONObject report,boolean replace){
        if(engineId<=0||report==null||
           report.optLong("engine_id",-1)!=engineId)
            throw new IllegalArgumentException("INVALID_ENGINE_RESULT_ID");
        SQLiteDatabase db=getWritableDatabase();
        db.beginTransaction();
        try{
            long old=-1;
            try(Cursor c=db.rawQuery(
                "SELECT id FROM emission_measurements WHERE engine_id=?",
                new String[]{Long.toString(engineId)})){
                if(c.moveToFirst())old=c.getLong(0);
            }
            if(old!=-1&&!replace)throw new IllegalStateException(
                "ONE_MEASUREMENT_ALREADY_EXISTS_USE_EXPLICIT_REMEASUREMENT");
            if(old==-1&&replace)throw new IllegalStateException("NOTHING_TO_REPLACE");
            ContentValues values=new ContentValues();
            values.put("at_ms",report.optLong("measured_at_ms",System.currentTimeMillis()));
            values.put("results_json",report.toString());
            long recordId;
            if(old!=-1){
                if(db.update("emission_measurements",values,"id=?",
                   new String[]{Long.toString(old)})!=1)
                    throw new IllegalStateException("REPLACEMENT_FAILED");
                recordId=old;
            }else{
                values.put("engine_id",engineId);
                recordId=db.insertOrThrow("emission_measurements",null,values);
            }
            db.setTransactionSuccessful();
            return recordId;
        }finally{db.endTransaction();}
    }
    public JSONObject latest(long engineId){
        if(engineId<=0)return null;
        try(Cursor c=getReadableDatabase().rawQuery(
            "SELECT results_json FROM emission_measurements WHERE engine_id=? LIMIT 1",
            new String[]{Long.toString(engineId)})){
            if(c.moveToFirst())return new JSONObject(c.getString(0));
        }catch(Exception ignored){}
        return null;
    }
    public int count(long engineId){
        try(Cursor c=getReadableDatabase().rawQuery(
            "SELECT COUNT(*) FROM emission_measurements WHERE engine_id=?",
            new String[]{Long.toString(engineId)})){
            return c.moveToFirst()?c.getInt(0):0;
        }
    }
}