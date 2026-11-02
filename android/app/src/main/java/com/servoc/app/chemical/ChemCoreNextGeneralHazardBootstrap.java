package com.servoc.app.chemical;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.servoc.app.data.ExperimentsDb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ChemCore Next category hazard enrichment IN THE EXISTING unified catalog.
 *
 * Adds no new physical database, side library, hazard table or separate
 * catalog view. Category identities are rows in chemicals, while their
 * hazards use the already-shared chemical_hazard_classifications table.
 *
 * GHS icon assignment requires distinction:
 * - SOURCE_QUALIFIED: LPG GHS02 flammable fuel gas, NIOSH source-qualified.
 * - PROVISIONAL: potential constituent/product hazard for generic classes;
 *   NOT a final GHS/SDS label and not rendered by legacy allQualified().
 *
 * The analysis engine cannot infer chemical presence, ppm or an alarm from
 * a row's existence or from these research hazard annotations.
 */
public final class ChemCoreNextGeneralHazardBootstrap {
    private ChemCoreNextGeneralHazardBootstrap() {}

    public static final String SCHEMA = "chemcore-next-unified-catalog-general-ghs/1";
    public static final String SOURCE_ID = "CHEMCORE_NEXT_GENERAL_HAZARD_RESEARCH";
    public static final String SOURCE_NIOSH_LPG = "https://www.cdc.gov/niosh/npg/npgd0679.html";
    public static final String SOURCE_OSHA_PHYSICAL = "https://www.osha.gov/hazcom/appendix-b";
    public static final String SOURCE_OSHA_LABEL = "https://www.osha.gov/hazcom/appendix-c";
    public static final String SOURCE_OSHA_MIXTURE = "https://www.osha.gov/hazcom/appendix-a";
    public static final String SOURCE_NIOSH_ETHANOL = "https://www.cdc.gov/niosh/npg/npgd0262.html";
    public static final String SOURCE_NIOSH_AMMONIA = "https://www.cdc.gov/niosh/npg/npgd0028.html";
    public static final String SOURCE_NIOSH_NO2 = "https://www.cdc.gov/niosh/npg/npgd0454.html";

    /* category name, pictogram, hazard type from existing CHECK constraint,
     * exact classification detail, evidence status, primary URL, note.
     * Generic labels are expressly CONDITIONAL. */
    private static final String[][] FACTS = {
        {"LPG", "GHS02", "FLAMMABLE", "FLAMMABLE_FUEL_GAS_LPG",
            "SOURCE_QUALIFIED", SOURCE_NIOSH_LPG,
            "NIOSH identifies L.P.G. as a flammable fuel-gas mixture. This is a category hazard, not detected identity or a supplier-specific label."},
        {"NOx", "GHS03", "OXIDIZER", "NOX_SPECIFIC_OXIDIZING_CONSTITUENT_DEPENDENT",
            "PROVISIONAL", SOURCE_OSHA_PHYSICAL,
            "Potential oxidizing gas depends on the actual nitrogen oxide and mixture classification; never universally assign GHS03 to NOx."},
        {"NOx", "GHS06", "TOXIC", "NOX_NO2_COMPONENT_ACUTE_INHALATION_POTENTIAL",
            "PROVISIONAL", SOURCE_NIOSH_NO2,
            "Component-level NO2 inhalation toxicity; not equivalent to all NOx concentrations or a classified NOx mixture."},
        {"NOx", "GHS05", "CORROSIVE", "NOX_NO2_COMPONENT_CORROSIVE_POTENTIAL",
            "PROVISIONAL", SOURCE_NIOSH_NO2,
            "Potential corrosive constituent; actual mixture may require different or no pictogram."},
        {"Smoke", "GHS07", "IRRITANT", "COMBUSTION_SMOKE_POTENTIAL_RESPIRATORY_IRRITATION",
            "PROVISIONAL", SOURCE_OSHA_MIXTURE,
            "Smoke is an unknown complex aerosol; constituent evidence, source and actual mixture conditions are required for final GHS classification."},
        {"Alcohols (general)", "GHS02", "FLAMMABLE", "SOME_ALCOHOLS_FLAMMABLE_ETHANOL_EXAMPLE",
            "PROVISIONAL", SOURCE_NIOSH_ETHANOL,
            "Ethanol is flammable; alcohols as a chemical family cannot all be assigned GHS02 without a specific substance or mixture."},
        {"Ammonium-containing materials (NH4+ source class)", "GHS07", "IRRITANT",
            "AMMONIUM_FORMULATION_DEPENDENT_NH3_RELEASE",
            "PROVISIONAL", SOURCE_NIOSH_AMMONIA,
            "Ammonium ion NH4+ is not a free atmospheric gas. Certain conditions may generate NH3 vapor; use actual salt/formulation SDS and reference-gas experiments."}
    };

    public static final class Fact {
        public final long chemicalUid;
        public final String icon, classDetail, evidenceStatus, sourceUrl, explanation;
        Fact(long id, String icon, String detail, String status, String source, String note) {
            chemicalUid=id;this.icon=icon;classDetail=detail;
            evidenceStatus=status;sourceUrl=source;explanation=note;
        }
    }
    public static final class Result {
        public final int inserted, existing, totalFacts;
        public final Map<String,Long> catalogIds;
        Result(int inserted, int existing, Map<String,Long> ids) {
            this.inserted=inserted;this.existing=existing;totalFacts=inserted+existing;
            catalogIds=Collections.unmodifiableMap(new LinkedHashMap<>(ids));
        }
    }

    /** Populate existing master tables only; idempotent on repeated calls. */
    public static Result ensure(ExperimentsDb db) {
        if (db==null) throw new IllegalArgumentException("unified master database required");
        ChemCoreNextGeneralIdentityBootstrap.Result catalog=
            ChemCoreNextGeneralIdentityBootstrap.ensure(db);
        SQLiteDatabase sql=db.getWritableDatabase();
        int inserted=0, existing=0;
        sql.beginTransaction();
        try {
            ContentValues provenance=new ContentValues();
            provenance.put("source_id",SOURCE_ID);
            provenance.put("title","ChemCore Next source-qualified and provisional class GHS research annotations");
            provenance.put("authority","NIOSH Pocket Guide and OSHA HCS criteria (source per classification)");
            provenance.put("url",SOURCE_OSHA_LABEL);
            provenance.put("evidence_tier","B");
            provenance.put("version","2026-11-02");
            provenance.put("notes","Class hazard source references are not product-specific SDS, monitored gas identities, ppm or alarm authorization.");
            sql.insertWithOnConflict("scientific_sources",null,provenance,SQLiteDatabase.CONFLICT_IGNORE);
            for (String[] row:FACTS) {
                Long uid=catalog.uids.get(row[0]);
                if(uid==null||uid<=0)throw new IllegalStateException("missing canonical group UID "+row[0]);
                ContentValues v=new ContentValues();
                v.put("chemical_uid",uid);
                v.put("hazard_category",row[2]);
                v.put("classification_system","CHEMCORE_NEXT_GROUP_GHS_RESEARCH");
                v.put("classification_detail",row[3]);
                v.put("ghs_pictogram",row[1]);
                v.put("evidence_status",row[4]);
                v.put("source_id",SOURCE_ID);
                v.put("source_url",row[5]);
                v.put("source_revision","2026-11-02");
                v.put("notes",row[6]);
                long key=sql.insertWithOnConflict("chemical_hazard_classifications",
                    null,v,SQLiteDatabase.CONFLICT_IGNORE);
                if(key<0)existing++;else inserted++;
            }
            sql.setTransactionSuccessful();
        } finally {
            sql.endTransaction();
        }
        return new Result(inserted,existing,catalog.uids);
    }

    /**
     * Same source-linked master hazard rows for scientific catalog browsing.
     * Include provisional icons WITH explicit status, never as unqualified icons.
     */
    public static List<Fact> forMasterCatalog(ExperimentsDb db,long uid) {
        if(db==null||uid<=0) return Collections.emptyList();
        List<Fact> out=new ArrayList<>();
        try(Cursor c=db.getReadableDatabase().rawQuery(
            "SELECT hc.chemical_uid,hc.ghs_pictogram,hc.classification_detail," +
            "hc.evidence_status,hc.source_url,hc.notes " +
            "FROM chemical_hazard_classifications hc " +
            "JOIN chemicals ch ON ch.uid=hc.chemical_uid " +
            "WHERE ch.uid=? AND ch.active=1 " +
            "AND hc.classification_system='CHEMCORE_NEXT_GROUP_GHS_RESEARCH' " +
            "AND hc.source_id=? ORDER BY hc.ghs_pictogram,hc.classification_detail",
            new String[]{Long.toString(uid),SOURCE_ID})) {
            while(c.moveToNext())out.add(new Fact(c.getLong(0),c.getString(1),
                c.getString(2),c.getString(3),c.getString(4),c.getString(5)));
        }
        return Collections.unmodifiableList(out);
    }

    /** Only definitive source-qualified category facts; no mixture-conditional extrapolation. */
    public static List<String> qualifiedIcons(ExperimentsDb db,long uid) {
        Set<String> icons=new LinkedHashSet<>();
        for(Fact f:forMasterCatalog(db,uid))
            if("SOURCE_QUALIFIED".equals(f.evidenceStatus))icons.add(f.icon);
        return Collections.unmodifiableList(new ArrayList<>(icons));
    }
}
