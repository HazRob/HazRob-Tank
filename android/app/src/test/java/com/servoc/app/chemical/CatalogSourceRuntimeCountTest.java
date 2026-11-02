package com.servoc.app.chemical;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import com.servoc.app.data.ExperimentsDb;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/**
 * P00.4 actual Android SQLite bootstrap test, using pinned SERVOC base + Tank V8 overlay.
 * Counts are observed at runtime, NOT guessed from independent JSON source lists.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 32)
public final class CatalogSourceRuntimeCountTest {
    private static int scalar(SQLiteDatabase db, String sql) {
        try (Cursor c = db.rawQuery(sql, null)) {
            assertTrue("SQL did not produce one row", c.moveToFirst());
            return c.getInt(0);
        }
    }
    @Test public void freshInstallAndRepeatSeedKeepsCanonicalUidCatalogStable() {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase("servoc.db");
        ExperimentsDb db = new ExperimentsDb(context);
        try {
            int legacyOnly = scalar(db.getReadableDatabase(), "SELECT COUNT(*) FROM chemicals");
            assertEquals("fresh SQLite v14 must start from original 144 identity rows", 144, legacyOnly);
            ChemicalSeedBootstrap.ensureNiosh(context, db);
            ChemicalRepository repo = new ChemicalRepository(db);
            int full = repo.count();
            int countSql = scalar(db.getReadableDatabase(),
                "SELECT COUNT(*) FROM v14_unified_chemical_catalog");
            int uniqueUids = scalar(db.getReadableDatabase(),
                "SELECT COUNT(DISTINCT uid) FROM v14_unified_chemical_catalog");
            int legacyIds = scalar(db.getReadableDatabase(),
                "SELECT COUNT(*) FROM chemicals WHERE legacy_id IS NOT NULL");
            int duplicateOriginalLegacy = scalar(db.getReadableDatabase(),
                "SELECT COUNT(*) FROM (SELECT legacy_id FROM chemicals WHERE legacy_id IS NOT NULL GROUP BY legacy_id HAVING COUNT(*)>1)");
            int active = scalar(db.getReadableDatabase(),
                "SELECT COUNT(*) FROM chemicals WHERE active=1");
            int nonLegacy = scalar(db.getReadableDatabase(),
                "SELECT COUNT(*) FROM chemicals WHERE legacy_id IS NULL");
            assertEquals(full, countSql);
            assertEquals(full, uniqueUids);
            assertEquals(144, legacyIds);
            assertEquals(0, duplicateOriginalLegacy);
            assertTrue("expected full unified library, not legacy subset", full >= 700);
            assertTrue(nonLegacy > 500);
            ChemicalSeedBootstrap.ensureNiosh(context, db);
            int repeated = repo.count();
            assertEquals("idempotent bootstrap must not create new IDs on retry", full, repeated);
            System.out.println("P00_4_SQLITE_CATALOG_RUNTIME_PASS"
                + " legacy_before=" + legacyOnly
                + " full=" + full
                + " active=" + active
                + " nonlegacy=" + nonLegacy
                + " unique_uids=" + uniqueUids
                + " repeated=" + repeated
                + " documented_734_target=UNASSERTED");
        } finally {
            db.close();
        }
    }
}
