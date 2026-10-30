package com.servoc.app.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * V11 source-scoped atmosphere benchmarks and oxygen hazards.
 * Distinguishes naturally occurring atmospheric background from advice for
 * ventilation and from independently qualified hazardous-atmosphere readings.
 * Never modifies chemical UIDs, research models or approved session evidence.
 */
public final class ChemicalHazardSeedV11 {
    public static final String VERSION="hazrob-tank-v6-n09b-air-context/1";
    private ChemicalHazardSeedV11(){}

    private static long uid(SQLiteDatabase db,int legacyId){
        try(Cursor c=db.rawQuery("SELECT uid FROM chemicals WHERE legacy_id=?",
            new String[]{Integer.toString(legacyId)})){
            return c.moveToFirst()?c.getLong(0):-1;
        }
    }
    private static void source(SQLiteDatabase db,String id,String title,String authority,
                               String url,String version,String note){
        ContentValues v=new ContentValues();
        v.put("source_id",id);v.put("title",title);v.put("authority",authority);
        v.put("url",url);v.put("version",version);v.put("evidence_tier","A");
        v.put("notes",note);
        db.insertWithOnConflict("scientific_sources",null,v,SQLiteDatabase.CONFLICT_IGNORE);
    }
    private static void casIfAbsent(SQLiteDatabase db,int legacy,String cas){
        ContentValues v=new ContentValues();v.put("cas",cas);
        db.update("chemicals",v,"legacy_id=? AND (cas IS NULL OR TRIM(cas)='')",
            new String[]{Integer.toString(legacy)});
    }
    private static void range(SQLiteDatabase db,long uid,String scope,Double min,
                              Double max,String unit,String source,String period,String status){
        if(uid<=0)return;
        try(Cursor c=db.rawQuery("SELECT 1 FROM chemical_background_ranges "+
            "WHERE chemical_uid=? AND environment_type=? AND source_id=? LIMIT 1",
            new String[]{Long.toString(uid),scope,source})){
            if(c.moveToFirst())return;
        }
        ContentValues v=new ContentValues();v.put("chemical_uid",uid);
        v.put("environment_type",scope);
        if(min!=null)v.put("range_min",min);if(max!=null)v.put("range_max",max);
        v.put("unit",unit);v.put("source_id",source);v.put("reference_period",period);
        v.put("location_scope",scope);v.put("qualification_status",status);
        db.insertOrThrow("chemical_background_ranges",null,v);
    }
    private static void nominal(SQLiteDatabase db,long uid,String scope,double value,
                                String unit,String period,String source,String status,String note){
        if(uid<=0)return;
        ContentValues v=new ContentValues();v.put("chemical_uid",uid);
        v.put("environment_type",scope);v.put("nominal_value",value);
        v.put("unit",unit);v.put("observation_period",period);v.put("source_id",source);
        v.put("qualification_status",status);v.put("notes",note);
        db.insertWithOnConflict("chemical_ambient_benchmarks",null,v,SQLiteDatabase.CONFLICT_IGNORE);
    }
    private static void hazard(SQLiteDatabase db,long uid,String hazardType,String comparison,
                               double value,String unit,String scope,String source,String revision){
        if(uid<=0)return;
        ContentValues v=new ContentValues();v.put("chemical_uid",uid);
        v.put("hazard_type",hazardType);v.put("comparison_operator",comparison);
        v.put("threshold",value);v.put("unit",unit);
        v.put("regulatory_scope",scope);v.put("source_id",source);
        v.put("source_revision",revision);
        v.put("qualification_status","REFERENCE_ONLY_INSTRUMENT_NOT_G3_QUALIFIED");
        v.put("requires_confirmed_analyte_measurement",1);
        db.insertWithOnConflict("chemical_hazard_boundaries",null,v,SQLiteDatabase.CONFLICT_IGNORE);
    }

    public static void ensure(SQLiteDatabase db){
        if(db==null)throw new IllegalArgumentException("SQLite required");
        source(db,"NOAA_AIR_COMPOSITION",
            "NOAA JetStream Atmosphere: composition of dry and moist air","NOAA",
            "https://www.noaa.gov/jetstream/atmosphere","accessed 2026-10-30",
            "Dry O2 20.946%; 0 to 4% water vapour gives O2 20.947 to 20.11 vol%; these are not oxygen-hazard thresholds.");
        source(db,"NOAA_GML_2026_07_CO2",
            "NOAA GML global monthly mean CO2 July 2026","NOAA/GML",
            "https://www.gml.noaa.gov/ccgg/trends/global.html",
            "updated 2026-10-09",
            "426.10 ppm dry-air global marine surface mean; NOT a maximum acceptable CO2 value.");
        source(db,"OSHA_1910_146_O2",
            "OSHA 29 CFR 1910.146 hazardous confined-space atmospheres","OSHA",
            "https://www.osha.gov/laws-regs/regulations/standardnumber/1910/1910.146",
            "29 CFR 1910.146",
            "Below 19.5 or above 23.5 vol% O2 hazardous in confined-space scope; does not certify robot's measurement.");
        source(db,"OSHA_OTM_IAQ_CO2",
            "OSHA Technical Manual Section III Chapter 2: CO2 indoor air screening",
            "OSHA","https://www.osha.gov/otm/section-3-health-hazards/chapter-2",
            "accessed 2026-10-30",
            "1000 ppm indoor ventilation investigation guidance, explicitly NOT evacuation or occupational danger.");
        source(db,"NIST_ASHRAE_CO2_CONTEXT",
            "NIST Persily 2020: no ASHRAE Standard 62.1 1000 ppm CO2 limit",
            "NIST","https://www.nist.gov/publications/quit-blaming-ashrae-standard-621-1000-ppm-co2",
            "2020","Cautions against labeling 1000 ppm as universal safety or indoor air-quality limit.");

        // Only fill empty CAS fields: never overwrite source-resolved identifiers.
        casIfAbsent(db,22,"7782-44-7");casIfAbsent(db,23,"124-38-9");
        casIfAbsent(db,20,"630-08-0");casIfAbsent(db,76,"74-82-8");
        casIfAbsent(db,15,"10028-15-6");
        final long o2=uid(db,22),co2=uid(db,23),co=uid(db,20),
                   methane=uid(db,76),ozone=uid(db,15);
        // Ambient oxygen range applies to measured water-vapor volume content,
        // NOT to 0-4% relative humidity; do not use as universal indoor standard.
        range(db,o2,"TROPOSPHERE_WATER_VAPOUR_0_TO_4_VOL_PERCENT",20.11,20.947,
            "vol%","NOAA_AIR_COMPOSITION","NOAA JetStream composition",
            "CONTEXT_SPECIFIC_DESCRIPTIVE_NOT_SAFETY");
        nominal(db,o2,"DRY_TROPOSPHERE",20.946,"vol%",
            "NOAA JetStream","NOAA_AIR_COMPOSITION","NOMINAL_DRY_AIR",
            "Oxygen in dry air; ambient vapor displaces volume.");
        hazard(db,o2,"OXYGEN_DEFICIENCY","<",19.5,"vol%",
            "US_OSHA_CONFINED_SPACES","OSHA_1910_146_O2","29 CFR 1910.146");
        hazard(db,o2,"OXYGEN_ENRICHMENT",">",23.5,"vol%",
            "US_OSHA_CONFINED_SPACES","OSHA_1910_146_O2","29 CFR 1910.146");

        // Latest NOAA global mean is an observation, not a natural min/max.
        nominal(db,co2,"GLOBAL_MARINE_SURFACE_DRY_AIR_MONTHLY_MEAN",426.10,"ppm",
            "2026-07","NOAA_GML_2026_07_CO2","OBSERVED_MEAN_NOT_ACCEPTABLE_MAXIMUM",
            "Dynamic location/season correction needed before using as ambient background.");
        // Outdoor air already contains CO2; modest occupant-related increases
        // cannot be classified automatically as toxic exposure or hazard.
        hazard(db,co2,"VENTILATION_SCREENING_ADVISORY",">",1000.0,"ppm",
            "INDOOR_AIR_ONLY_NOT_SAFETY_LIMIT","OSHA_OTM_IAQ_CO2",
            "OSHA Technical Manual Section III Chapter 2");

        // Other NOAA atmospheric gas values are mean composition, not a
        // scientifically established normal minimum or maximum for a site.
        nominal(db,methane,"DRY_OUTDOOR_AIR_NOAA_COMPOSITION",1.92,"ppm",
            "NOAA JetStream","NOAA_AIR_COMPOSITION","NOMINAL_NOT_LIMIT",
            "Not a safe methane exposure threshold.");
        nominal(db,co,"DRY_OUTDOOR_AIR_NOAA_COMPOSITION",0.10,"ppm",
            "NOAA JetStream","NOAA_AIR_COMPOSITION","NOMINAL_NOT_LIMIT",
            "Not a safe CO exposure threshold.");
        nominal(db,ozone,"DRY_OUTDOOR_AIR_NOAA_COMPOSITION",0.07,"ppm",
            "NOAA JetStream","NOAA_AIR_COMPOSITION","NOMINAL_NOT_LIMIT",
            "May vary substantially with region/photochemistry; not universal normal range.");
    }
}
