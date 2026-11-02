package com.servoc.app.chemical;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.servoc.app.data.ExperimentsDb;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ChemCore Next research hazard ontology for aggregate/non-molecular catalog rows.
 *
 * Deliberately separate from chemical_hazard_classifications:
 * GhsPictograms.allQualified() only consumes source-qualified definite PRODUCT
 * classifications. These advisory labels must NOT leak into alarms, SDS labels,
 * confirmed gas detection, clinical/occupational conclusions or measured ppm.
 *
 * Type labels include source and a qualifier; a source-qualified *category
 * characteristic* is NOT a source-qualified formula-/product-specific GHS label.
 */
public final class ChemCoreNextGeneralHazardBootstrap {
    private ChemCoreNextGeneralHazardBootstrap() {}

    public static final String SCHEMA = "chemcore-next-group-hazard-advisory/1";
    public static final String GROUP = "CHEMCORE_NEXT_GROUP_HAZARD_GUIDANCE";
    public static final String SOURCE_OSHA_PHYSICAL = "https://www.osha.gov/hazcom/appendix-b";
    public static final String SOURCE_OSHA_LABEL = "https://www.osha.gov/hazcom/appendix-c";
    public static final String SOURCE_OSHA_MIXTURE = "https://www.osha.gov/hazcom/appendix-a";
    public static final String SOURCE_NIOSH_LPG = "https://www.cdc.gov/niosh/npg/npgd0679.html";
    public static final String SOURCE_NIOSH_ETHANOL = "https://www.cdc.gov/niosh/npg/npgd0262.html";
    public static final String SOURCE_NIOSH_AMMONIA = "https://www.cdc.gov/niosh/npg/npgd0028.html";
    public static final String SOURCE_ATSDR_NOX = "https://wwwn.cdc.gov/TSP/MMG/MMGDetails.aspx?mmgid=394&toxid=69";
    public static final String SOURCE_ATSDR_NOX_CLASS = "https://wwwn.cdc.gov/TSP/substances/ToxSubstance.aspx?toxid=69";

    /** Source class evidence, never as a generic mixture SDS or confirmed analyte.
      * name, code, relevant property, qualifier, sourceURL, hazard text, exact-context */
    private static final String[][] ADVISORIES = {
        {"LPG", "GHS02", "FLAMMABLE_FUEL_GAS", "CLASS_CHARACTERISTIC_SOURCE_SUPPORTED",
            SOURCE_NIOSH_LPG, "Liquefied petroleum fuel gas is flammable",
            "Generic LPG fuel-gas family; actual product SDS controls required GHS label"},
        {"LPG", "GHS04", "GAS_UNDER_PRESSURE", "PRESSURIZED_PRODUCT_ONLY",
            SOURCE_NIOSH_LPG, "LPG is transported or sold as liquefied compressed gas",
            "Applies to pressurized/liquefied LPG packaging, not an arbitrary ambient gas plume"},
        {"NOx", "GHS03", "OXIDIZING_POTENTIAL", "CONSTITUENT_AND_COMPOSITION_DEPENDENT",
            SOURCE_OSHA_PHYSICAL, "Some nitrogen oxides may contribute to combustion",
            "Not every NOx mixture is an oxidizing gas; require ISO 10156 test or composition"},
        {"NOx", "GHS06", "POTENTIAL_ACUTE_INHALATION_TOXICITY",
            "CONSTITUENT_AND_CONCENTRATION_DEPENDENT",
            SOURCE_ATSDR_NOX, "NO2 is hazardous by acute inhalation; severity differs from NO",
            "A specific NO2 substance/mixture SDS, not a blanket NOx category label"},
        {"NOx", "GHS05", "POTENTIAL_CORROSIVE_EFFECTS",
            "CONSTITUENT_AND_CONCENTRATION_DEPENDENT",
            SOURCE_ATSDR_NOX, "NO2 may be corrosive and forms acids in moisture",
            "Conditional NO2 component effect, not generic class GHS05 assignment"},
        {"Smoke", "GHS07", "POTENTIAL_RESPIRATORY_IRRITATION",
            "COMBUSTION_COMPOSITION_DEPENDENT",
            SOURCE_OSHA_MIXTURE, "Combustion smoke may contain irritant vapors and particulates",
            "Smoke is an unknown complex aerosol; formal GHS needs specified composition and hazard data"},
        {"Alcohols (general)", "GHS02", "POTENTIAL_FLAMMABILITY",
            "SPECIFIC_ALCOHOL_AND_FORMULATION_DEPENDENT",
            SOURCE_NIOSH_ETHANOL, "Some alcohols, including ethanol, are flammable",
            "Cannot generalize a definite GHS02 label to all alcohols; check individual SDS"},
        {"Ammonium-containing materials (NH4+ source class)", "GHS07",
            "POTENTIAL_IRRITANT_OR_AMMONIA_RELEASE",
            "SPECIFIC_SALT_PH_TEMPERATURE_AND_EXPOSURE_DEPENDENT",
            SOURCE_NIOSH_AMMONIA, "Some ammonium materials can release irritant NH3 under relevant chemistry",
            "NH4+ is an aqueous ionic species, not an NH4 gas; SDS and gas NH3 evidence required"}
    };

    public static final class Result {
        public final int inserted, existing, advisoryRows;
        public final Map<String, Long> catalogIds;
        Result(int inserted, int existing, int advisoryRows, Map<String, Long> catalogIds) {
            this.inserted = inserted;
            this.existing = existing;
            this.advisoryRows = advisoryRows;
            this.catalogIds = Collections.unmodifiableMap(new LinkedHashMap<>(catalogIds));
        }
    }

    public static Result ensure(ExperimentsDb db) {
        if (db == null) throw new IllegalArgumentException("actual canonical catalog required");
        SQLiteDatabase sql = db.getWritableDatabase();
        // Include the 3 historical classes, seed only the 2 truly new classes.
        ChemCoreNextGeneralIdentityBootstrap.Result catalog =
            ChemCoreNextGeneralIdentityBootstrap.ensure(db);

        sql.execSQL("CREATE TABLE IF NOT EXISTS chemcore_next_group_hazard_advisories (" +
            "chemical_uid INTEGER NOT NULL," +
            "pictogram TEXT NOT NULL CHECK(pictogram IN ('GHS01','GHS02','GHS03','GHS04','GHS05','GHS06','GHS07','GHS08','GHS09'))," +
            "hazard_key TEXT NOT NULL," +
            "qualifier TEXT NOT NULL," +
            "source_url TEXT NOT NULL," +
            "hazard_statement TEXT NOT NULL," +
            "applicability_note TEXT NOT NULL," +
            "catalog_scope TEXT NOT NULL DEFAULT 'RESEARCH_CLASS_ADVISORY_ONLY'," +
            "approval_state TEXT NOT NULL DEFAULT 'NOT_PRODUCT_GHS_CLASSIFIED'," +
            "PRIMARY KEY(chemical_uid,pictogram,hazard_key)," +
            "FOREIGN KEY(chemical_uid) REFERENCES chemicals(uid)," +
            "CHECK(catalog_scope='RESEARCH_CLASS_ADVISORY_ONLY')," +
            "CHECK(approval_state='NOT_PRODUCT_GHS_CLASSIFIED'))");
        sql.execSQL("CREATE INDEX IF NOT EXISTS ix_next_group_hazard_uid " +
            "ON chemcore_next_group_hazard_advisories(chemical_uid)");

        // Separate presentation view with explicit status. Do not merge into
        // allQualified() or the v14 formal hazard source/chemical tables.
        sql.execSQL("CREATE VIEW IF NOT EXISTS chemcore_next_general_hazard_catalog AS " +
            "SELECT c.uid,c.name_en,c.cas,c.chemical_class,a.pictogram,a.hazard_key," +
            "a.qualifier,a.source_url,a.hazard_statement,a.applicability_note," +
            "a.catalog_scope,a.approval_state FROM chemicals c " +
            "JOIN chemcore_next_group_hazard_advisories a ON a.chemical_uid=c.uid " +
            "WHERE c.active=1");

        int inserted = 0, existing = 0;
        sql.beginTransaction();
        try {
            for (String[] source : ADVISORIES) {
                Long uid = catalog.uids.get(source[0]);
                if (uid == null || uid <= 0) throw new IllegalStateException("group identity missing: "+source[0]);
                ContentValues values = new ContentValues();
                values.put("chemical_uid", uid);
                values.put("pictogram", source[1]);
                values.put("hazard_key", source[2]);
                values.put("qualifier", source[3]);
                values.put("source_url", source[4]);
                values.put("hazard_statement", source[5]);
                values.put("applicability_note", source[6]);
                values.put("catalog_scope", "RESEARCH_CLASS_ADVISORY_ONLY");
                values.put("approval_state", "NOT_PRODUCT_GHS_CLASSIFIED");
                long changed = sql.insertWithOnConflict("chemcore_next_group_hazard_advisories",
                    null, values, SQLiteDatabase.CONFLICT_IGNORE);
                if (changed < 0) existing++;
                else inserted++;
            }
            sql.setTransactionSuccessful();
        } finally {
            sql.endTransaction();
        }
        return new Result(inserted, existing, inserted+existing, catalog.uids);
    }
}
