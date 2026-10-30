package com.servoc.app.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.hazrob.tank.chemcore.v6.SourceCatalog;
import java.util.List;

/**
 * V14 maps scientific MQ source equations to canonical material UIDs.
 * It NEVER imports a research/source R0 as a measured Tank R0.
 * Numerical curves refer to their source-specific R0 definition and heater
 * protocol, which are explicitly unresolved unless evidenced individually.
 */
public final class MqReferenceCurveSeedV14 {
    private MqReferenceCurveSeedV14(){}
    public static final String MQDATA="TANK_V6_MQDATA43_SOURCE";
    public static final String MQ135ALT="TANK_V6_MQ135_ALTERNATIVE_SOURCE";

    private static void source(SQLiteDatabase db,String id,String title,String url,String rev){
        ContentValues v=new ContentValues();
        v.put("source_id",id);v.put("title",title);
        v.put("authority","ORIGINAL_RESEARCH_SNAPSHOT");
        v.put("url",url);v.put("version",rev);
        v.put("evidence_tier","RESEARCH_ONLY");
        v.put("notes","Numerical transfer relation only. R0 definition, heater phase, source fixture "+
            "and Tank physics are not known. Do not infer oxygen or other analyte concentration.");
        db.insertWithOnConflict("scientific_sources",null,v,SQLiteDatabase.CONFLICT_IGNORE);
    }
    private static long uid(SQLiteDatabase db,long legacy){
        try(Cursor c=db.rawQuery("SELECT uid FROM chemicals WHERE legacy_id=? LIMIT 1",
                new String[]{Long.toString(legacy)})){
            return c.moveToFirst()?c.getLong(0):-1;
        }
    }
    public static int ensure(SQLiteDatabase db){
        source(db,MQDATA,"MQ DataScience source 43 source response curves",
            "https://github.com/HazRob/HazRob-Tank/blob/feature/tank-v6-implementation/research/chemcore_v6/source_snapshots/hr069/mq_research_curves_43.json",
            SourceCatalog.SOURCE_BLOB);
        source(db,MQ135ALT,"MQ135 six alternate digitized source equations",
            "https://github.com/HazRob/HazRob-Tank/blob/feature/tank-v6-implementation/android/app/src/main/java/com/hazrob/tank/chemcore/v6/AlternativeCurveCatalog.java",
            "v6-alt-six");
        int inserted=0;
        List<SourceCatalog.Curve> curves=SourceCatalog.allCurves();
        for(SourceCatalog.Curve curve:curves){
            long chemicalUid=uid(db,curve.legacyChemicalId);
            if(chemicalUid<1||!curve.numericalDefinitionValid())continue;
            String sourceId=curve.index<=43?MQDATA:MQ135ALT;
            ContentValues v=new ContentValues();
            v.put("curve_id",curve.id());v.put("chemical_uid",chemicalUid);
            v.put("sensor_model",curve.sensorType);v.put("source_legacy_id",curve.legacyChemicalId);
            v.put("source_label",curve.label);
            v.put("ratio_a",curve.a);v.put("ratio_b",curve.b);
            v.put("output_scale",curve.outputScale);
            v.put("min_source_ppm",curve.minSourcePpm);
            v.put("max_source_ppm",curve.maxSourcePpm);
            v.put("source_id",sourceId);
            v.put("reference_r0_definition","UNVERIFIED_SOURCE_CONDITION");
            v.put("tank_r0_equivalence","NOT_ESTABLISHED");
            v.put("evidence_status","RESEARCH_REFERENCE_ONLY");
            v.put("source_curve_formula","Rs/R0 = a*(ppm/output_scale)^b");
            v.put("source_revision",SourceCatalog.SOURCE_BLOB);
            long id=db.insertWithOnConflict("mq_chemical_reference_curves",null,v,
                SQLiteDatabase.CONFLICT_IGNORE);
            if(id!=-1)inserted++;
        }
        return inserted;
    }
}
