package com.hazrob.tank.emissions;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Dedicated engine identity + results storage, no ChemCore Next or food database tables. */
public final class EmissionSessionStore extends SQLiteOpenHelper {
    private static final String DB="hazrob_emissions_v1.db";
    public static final class Engine {
        public final long id;
        public final String name;
        public Engine(long id,String name){this.id=id;this.name=name;}
    }
    public EmissionSessionStore(Context context){super(context.getApplicationContext(),DB,null,1);}
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE emission_engines (id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE,created_ms INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE emission_measurements (id INTEGER PRIMARY KEY AUTOINCREMENT,engine_id INTEGER NOT NULL,at_ms INTEGER NOT NULL,results_json TEXT NOT NULL, FOREIGN KEY(engine_id) REFERENCES emission_engines(id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX emission_measurements_by_engine ON emission_measurements(engine_id,at_ms DESC)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldV,int newV){}
    @Override public void onConfigure(SQLiteDatabase db){
        super.onConfigure(db);db.setForeignKeyConstraintsEnabled(true);
    }
    public long create(String name){
        if(name==null||name.trim().isEmpty()||name.trim().length()>80)
            throw new IllegalArgumentException("INVALID_ENGINE_NAME");
        ContentValues v=new ContentValues();
        v.put("name",name.trim());v.put("created_ms",System.currentTimeMillis());
        long id=getWritableDatabase().insertOrThrow("emission_engines",null,v);
        return id;
    }
    public List<Engine> engines(){
        List<Engine> items=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery(
            "SELECT id,name FROM emission_engines ORDER BY created_ms DESC,id DESC",null)){
            while(c.moveToNext())items.add(new Engine(c.getLong(0),c.getString(1)));
        }
        return items;
    }
    public void delete(long id){
        if(id<=0)throw new IllegalArgumentException("INVALID_ENGINE");
        // Foreign keys are ON DELETE CASCADE, ensuring measurements are not orphaned.
        getWritableDatabase().delete("emission_engines","id=?",new String[]{Long.toString(id)});
    }
    public long addMeasurement(long engineId,JSONObject report){
        if(engineId<=0||report==null)throw new IllegalArgumentException("INVALID_RESULT");
        ContentValues v=new ContentValues();
        v.put("engine_id",engineId);
        v.put("at_ms",report.optLong("measured_at_ms",System.currentTimeMillis()));
        v.put("results_json",report.toString());
        return getWritableDatabase().insertOrThrow("emission_measurements",null,v);
    }
    public JSONObject latest(long engineId){
        if(engineId<=0)return null;
        try(Cursor c=getReadableDatabase().rawQuery(
            "SELECT results_json FROM emission_measurements WHERE engine_id=? ORDER BY at_ms DESC,id DESC LIMIT 1",
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
