package com.servoc.app.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Conservative, source-qualified starter evidence; a missing classification is UNKNOWN. */
public final class ChemicalHazardEvidenceSeedV13 {
    public static final String VERSION="tank-v6-v13-sourced-hazard-evidence/1";
    private ChemicalHazardEvidenceSeedV13(){}

    private static long chemical(SQLiteDatabase db,String cas){
        try(Cursor c=db.rawQuery("SELECT uid FROM chemicals WHERE cas=? LIMIT 1",new String[]{cas})){
            return c.moveToFirst()?c.getLong(0):-1;
        }
    }
    private static long canonicalRadon(SQLiteDatabase db){
        long matched=chemical(db,"10043-92-2");
        if(matched>0)return matched;
        try(Cursor c=db.rawQuery(
                "SELECT uid,cas FROM chemicals WHERE lower(trim(name_en))='radon' LIMIT 1",null)){
            if(!c.moveToFirst())return -1;
            long uid=c.getLong(0);
            if(c.isNull(1)||c.getString(1).trim().isEmpty()){
                ContentValues v=new ContentValues();v.put("cas","10043-92-2");
                db.update("chemicals",v,"uid=?",new String[]{Long.toString(uid)});
            }else if(!"10043-92-2".equals(c.getString(1).trim()))return -1;
            return uid;
        }
    }
    private static void source(SQLiteDatabase db,String id,String title,String authority,String url,String version){
        ContentValues v=new ContentValues();
        v.put("source_id",id);v.put("title",title);v.put("authority",authority);
        v.put("url",url);v.put("version",version);v.put("evidence_tier","A");
        v.put("notes","Only individually verified substance/physical hazard facts, no inference from molecule family or absent data.");
        db.insertWithOnConflict("scientific_sources",null,v,SQLiteDatabase.CONFLICT_IGNORE);
    }
    private static void classify(SQLiteDatabase db,long uid,String category,String system,
            String detail,String source,String sourceUrl,String severity,String ghs,String pictogram,
            String iarc,String notes){
        if(uid<=0)return;
        ContentValues v=new ContentValues();
        v.put("chemical_uid",uid);v.put("hazard_category",category);
        v.put("classification_system",system);v.put("classification_detail",detail);
        v.put("severity_level",severity);v.put("ghs_h_codes",ghs);
        v.put("ghs_pictogram",pictogram);v.put("iarc_group",iarc);
        v.put("source_id",source);v.put("source_url",sourceUrl);
        v.put("evidence_status","SOURCE_QUALIFIED");v.put("notes",notes);
        db.insertWithOnConflict("chemical_hazard_classifications",null,v,SQLiteDatabase.CONFLICT_IGNORE);
    }
    private static void property(SQLiteDatabase db,long uid,String key,Double number,
                                 String text,String unit,String source){
        if(uid<=0)return;
        ContentValues v=new ContentValues();
        v.put("chemical_uid",uid);v.put("property_key",key);
        if(number!=null)v.put("value_real",number);
        if(text!=null)v.put("value_text",text);
        if(unit!=null)v.put("unit",unit);
        v.put("environment_scope","GENERAL");v.put("source_id",source);
        v.put("evidence_status","SOURCE_QUALIFIED");
        db.insertWithOnConflict("chemical_hazard_properties",null,v,SQLiteDatabase.CONFLICT_IGNORE);
    }
    public static void ensure(SQLiteDatabase db){
        source(db,"OSHA_BENZENE_1910_1028",
            "OSHA benzene hazard communication and substance technical guidelines",
            "OSHA","https://www.osha.gov/laws-regs/regulations/standardnumber/1910/1910.1028",
            "29 CFR 1910.1028 (reviewed 2026-10-30)");
        source(db,"IARC_BENZENE_120",
            "IARC Monographs benzene, Volume 120, Group 1",
            "WHO/IARC","https://monographs.iarc.who.int/agents-classified-by-the-iarc/",
            "2026-07-23");
        source(db,"PUBCHEM_DIOXYGEN_977",
            "PubChem CID 977 / CAMEO oxygen identity and oxidation properties",
            "NIH PubChem / NOAA CAMEO","https://pubchem.ncbi.nlm.nih.gov/compound/977",
            "2026-10-24");
        source(db,"NIOSH_OSHA_TANK_GAUGING_2016_108",
            "OSHA NIOSH Hazard Alert: manual tank gauging and sampling, Appendix B",
            "NIOSH/OSHA","https://www.cdc.gov/niosh/docs/2016-108/pdfs/2016-108.pdf",
            "NIOSH 2016-108");

        source(db,"EPA_RADON",
            "US EPA: naturally occurring radioactive radon gas and lung cancer",
            "US EPA","https://www.epa.gov/radiation/frequent-questions-radiation-sources",
            "accessed 2026-10-30");
        long radon=canonicalRadon(db);
        classify(db,radon,"RADIOACTIVE","EPA_RADIOLOGICAL_HAZARD",
            "natural radioactive gas, radon isotopes",
            "EPA_RADON","https://www.epa.gov/radiation/frequent-questions-radiation-sources",
            "IONIZING_RADIATION",null,null,null,
            "Not a GHS class; radiation activity units are Bq/m3 or pCi/L and must not be inferred from MQ.");
        classify(db,radon,"CARCINOGEN","EPA_LUNG_CANCER_HAZARD",
            "radon inhalation linked to lung cancer",
            "EPA_RADON","https://www.epa.gov/radiation/frequent-questions-radiation-sources",
            "LUNG_CARCINOGEN",null,null,null,
            "Risk concerns accumulated radon exposure and must be measured with a suitable radon instrument.");
        property(db,radon,"HEALTH_EFFECTS",null,
            "Ionizing alpha emission from radon decay products; elevated inhalation exposure increases lung cancer risk",
            null,"EPA_RADON");

        long benzene=chemical(db,"71-43-2");
        classify(db,benzene,"CARCINOGEN","OSHA_HCS","benzene carcinogenicity",
            "OSHA_BENZENE_1910_1028",
            "https://www.osha.gov/laws-regs/regulations/standardnumber/1910/1910.1028",
            "CAUSES_CANCER",null,"GHS08",null,"OSHA 1910.1028(j) cites cancer hazard");
        classify(db,benzene,"FLAMMABLE","OSHA_HCS","benzene highly flammable liquid and vapor",
            "OSHA_BENZENE_1910_1028",
            "https://www.osha.gov/laws-regs/regulations/standardnumber/1910/1910.1028",
            "HIGHLY_FLAMMABLE",null,"GHS02",null,"OSHA 1910.1028(j)(2)");
        classify(db,benzene,"IRRITANT","OSHA_HCS","skin eye and respiratory irritation",
            "OSHA_BENZENE_1910_1028",
            "https://www.osha.gov/laws-regs/regulations/standardnumber/1910/1910.1028",
            "SOURCE_DOCUMENTED",null,null,null,"OSHA 1910.1028(j)(1)(ii)");
        classify(db,benzene,"ORGAN_TOXIC","OSHA_HCS","central nervous system and blood effects",
            "OSHA_BENZENE_1910_1028",
            "https://www.osha.gov/laws-regs/regulations/standardnumber/1910/1910.1028",
            "SOURCE_DOCUMENTED",null,"GHS08",null,"OSHA 1910.1028(j)(1)(ii)");
        classify(db,benzene,"CARCINOGEN","IARC","benzene Group 1",
            "IARC_BENZENE_120",
            "https://monographs.iarc.who.int/agents-classified-by-the-iarc/",
            "CARCINOGENIC_TO_HUMANS",null,null,"1","IARC Group 1, not a numeric concentration limit");
        property(db,benzene,"FLASH_POINT_C",-11.0,null,"degC_closed_cup","OSHA_BENZENE_1910_1028");
        property(db,benzene,"AUTOIGNITION_C",580.0,null,"degC","OSHA_BENZENE_1910_1028");
        property(db,benzene,"BOILING_POINT_C",80.1,null,"degC","OSHA_BENZENE_1910_1028");
        property(db,benzene,"VAPOR_DENSITY_AIR",2.7,null,"air=1","OSHA_BENZENE_1910_1028");
        property(db,benzene,"HEALTH_EFFECTS",null,
            "Cancer; central nervous system effects; hematological toxicity; irritation; aspiration hazard",
            null,"OSHA_BENZENE_1910_1028");
        property(db,benzene,"TARGET_ORGANS",null,
            "Blood and bone marrow; central nervous system; skin, eyes and respiratory tract",
            null,"OSHA_BENZENE_1910_1028");

        long oxygen=chemical(db,"7782-44-7");
        classify(db,oxygen,"OXIDIZER","CHEMICAL_PHYSICAL_EVIDENCE",
            "pure oxygen gas supports combustion and intensifies ignition",
            "PUBCHEM_DIOXYGEN_977","https://pubchem.ncbi.nlm.nih.gov/compound/977",
            "OXIDIZING_GAS",null,null,null,
            "Oxygen itself is nonflammable: never infer LFL/UFL or mark it flammable.");

        long methane=chemical(db,"74-82-8");
        classify(db,methane,"ASPHYXIANT","NIOSH_OSHA_HAZARD_ALERT",
            "methane displaces oxygen when concentrated",
            "NIOSH_OSHA_TANK_GAUGING_2016_108",
            "https://www.cdc.gov/niosh/docs/2016-108/pdfs/2016-108.pdf",
            "SIMPLE_ASPHYXIANT",null,null,null,
            "Oxygen displacement is the risk, not toxicity at atmospheric trace levels.");
    }
}
