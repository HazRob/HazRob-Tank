package com.servoc.app.chemical;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.servoc.app.data.ExperimentsDb;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ChemCore Next scientific catalog extension, not an Android V8 startup hook.
 *
 * Adds ONLY missing general *research ontology* entries to the existing
 * v14 unified materials table. Intentionally not wired into V6/V8 launch:
 * the independent ChemCore Next catalog initializer calls this explicitly.
 *
 * New category entries have null CAS/formula and no GHS, LOD, ppm, reference gas or
 * occupational limits. An NH4 source token is an ionic category, NOT NH4 gas.
 */
public final class ChemCoreNextGeneralIdentityBootstrap {
    public static final String VERSION = "chemcore-next-general-identities/1";
    public static final String ORIGIN = "CHEMCORE_NEXT_RESEARCH_ONTOLOGY";
    public static final String LPG = "LPG";
    public static final String SMOKE = "Smoke";
    public static final String NOX = "NOx";
    public static final String ALCOHOLS = "Alcohols (general)";
    public static final String AMMONIUM = "Ammonium-containing materials (NH4+ source class)";

    private static final String[][] ENTRIES = {
        {LPG, "غاز البترول المسال (خليط)", "MIXTURE_FUEL", "135"},
        {SMOKE, "الدخان (خليط/هباء معقد)", "MIXTURE_COMPLEX_AEROSOL", "139"},
        {NOX, "أكاسيد النيتروجين (فئة)", "AGGREGATE_REACTIVE_GAS_FAMILY", "141"},
        {ALCOHOLS, "الكحولات (فئة عامة)", "RESEARCH_CHEMICAL_FAMILY_NOT_SPECIFIC_MOLECULE", ""},
        {AMMONIUM, "مواد تحتوي على الأمونيوم (فئة مصادر)", "AMMONIUM_ION_SOURCE_CLASS_NOT_GAS", ""}
    };
    private ChemCoreNextGeneralIdentityBootstrap() {}

    public static final class Result {
        public final int present, inserted;
        public final Map<String,Long> uids;
        Result(int present, int inserted, Map<String,Long> uids) {
            this.present = present;
            this.inserted = inserted;
            this.uids = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(uids));
        }
    }

    public static Result ensure(ExperimentsDb db) {
        if (db == null) throw new IllegalArgumentException("catalog DB required");
        SQLiteDatabase sql = db.getWritableDatabase();
        int inserted = 0, present = 0;
        Map<String,Long> ids = new LinkedHashMap<>();
        sql.beginTransaction();
        try {
            for (String[] entry : ENTRIES) {
                String name = entry[0], type = entry[2], legacyIndex = entry[3];
                Long existing = lookupExact(sql, name, type, legacyIndex);
                if (existing != null) {
                    present++;
                    ids.put(name, existing);
                    continue;
                }
                ContentValues values = new ContentValues();
                values.put("name_en", name);
                values.put("name_ar", entry[1]);
                values.put("chemical_class", type);
                values.put("record_source", ORIGIN);
                values.put("active", 1);
                // Explicitly do not insert CAS, formula, molar_mass, legacy_id,
                // chemical hazards, GHS, limits or sensor identity claims.
                long uid = sql.insertOrThrow("chemicals", null, values);
                if (uid <= 0) throw new IllegalStateException("failed to allocate category UID");
                Long check = lookupExact(sql, name, type, legacyIndex);
                if (check == null || check != uid)
                    throw new IllegalStateException("category UID readback mismatch");
                ids.put(name, uid);
                inserted++;
            }
            sql.setTransactionSuccessful();
        } finally {
            sql.endTransaction();
        }
        return new Result(present, inserted, ids);
    }

    /** Fail closed on homonyms, duplicate active identities or CAS/formula attached to a class. */
    private static Long lookupExact(SQLiteDatabase sql, String name, String requiredClass, String legacyIndex) {
        try (Cursor cursor = sql.rawQuery(
            "SELECT uid,legacy_id,cas,formula,chemical_class,record_source " +
            "FROM chemicals WHERE active=1 AND lower(trim(name_en))=lower(trim(?))",
            new String[]{name})) {
            if (!cursor.moveToFirst()) return null;
            long uid = cursor.getLong(0);
            if (cursor.moveToNext())
                throw new IllegalStateException("duplicate active catalog identity name: " + name);
            cursor.moveToFirst();
            if ((legacyIndex.isEmpty() ? !cursor.isNull(1) : (cursor.isNull(1) || !String.valueOf(cursor.getInt(1)).equals(legacyIndex)))
                || !empty(cursor.getString(2))
                || !empty(cursor.getString(3))
                || (!empty(cursor.getString(4)) && !requiredClass.equals(cursor.getString(4))))
                throw new IllegalStateException("existing row is incompatible with research category: " + name);
            if (empty(cursor.getString(4))) {
                // Reuse and classify a historically seeded generic entry;
                // do not create a second LPG/Smoke/NOx row or change legacy UID.
                ContentValues classify = new ContentValues();
                classify.put("chemical_class", requiredClass);
                int changed = sql.update("chemicals", classify,
                    "uid=? AND (chemical_class IS NULL OR trim(chemical_class)='')",
                    new String[]{Long.toString(uid)});
                if (changed != 1)
                    throw new IllegalStateException("failed to retain existing category UID");
            }
            if (uid <= 0) throw new IllegalStateException("bad existing UID");
            return uid;
        }
    }
    private static boolean empty(String x) {return x == null || x.trim().isEmpty();}
}
