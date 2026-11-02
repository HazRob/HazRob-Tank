package com.servoc.app.chemical;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import com.servoc.app.data.ExperimentsDb;
import com.hazrob.tank.chemcore.v6.SourceCatalog;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/**
 * P03.4: Export source-to-UID historical lookup from *real seeded SQLite*.
 *
 * This is an independent ChemCore Next research database crosswalk test.
 * It does NOT change V6, V8 app behavior, or claim that a source gas name
 * is a verified chemical identity. Legacy index is NEVER itself the UID.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 32)
public final class ChemCoreNextLegacySourceUidAuditTest {

    private static String printable(String raw) {
        return raw == null ? "" : raw.replace("|", "/").replace("\n", " ");
    }

    @Test public void resolve49HistoricalSourceCurvesToActual734CatalogWithoutGuessingGasIdentity() {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase("servoc.db");
        ExperimentsDb db = new ExperimentsDb(context);
        try {
            ChemicalSeedBootstrap.ensureNiosh(context, db);
            ChemicalRepository chemicals = new ChemicalRepository(db);
            assertEquals("source library requires current unified catalog", 734, chemicals.count());
            assertEquals("original MQDataScience curves", 43, SourceCatalog.curves().size());
            assertEquals("43 + six MQ135 alternative research formulas", 49,
                         SourceCatalog.allCurves().size());

            Set<String> curveUids = new HashSet<>();
            Set<Long> resolvedMaterialUids = new HashSet<>();
            int seen=0;
            for (SourceCatalog.Curve curve: SourceCatalog.allCurves()) {
                assertTrue("source legacy reference index must be in original legacy catalog",
                           curve.legacyChemicalId >= 0 && curve.legacyChemicalId < 144);
                assertTrue("source curve ID must be unique", curveUids.add(curve.id()));
                ChemicalRepository.Chemical material =
                    chemicals.byLegacyId((int)curve.legacyChemicalId);
                assertNotNull("historical reference not found: " + curve.id(), material);
                assertNotNull("master chemical UID missing", material.uid > 0 ? material : null);
                assertNotNull("must not confuse legacy index with master UID",
                    chemicals.byUid(material.uid));
                assertEquals(Integer.valueOf((int)curve.legacyChemicalId), material.legacyId);
                assertEquals(material.uid, chemicals.byUid(material.uid).uid);
                resolvedMaterialUids.add(material.uid);
                seen++;
                // Classification-only historical link. A retained source label
                // (e.g. smoke, LPG, alcohol, NH4/NH3) is not molecular identification.
                System.out.println("P03_4_LEGACY_CROSSWALK"
                    + "|" + curve.id()
                    + "|" + curve.sensorType
                    + "|" + printable(curve.label)
                    + "|" + curve.legacyChemicalId
                    + "|" + material.uid
                    + "|" + printable(material.name)
                    + "|" + printable(material.cas));
            }
            assertEquals(49, seen);
            assertTrue("expected several distinct referenced records", resolvedMaterialUids.size()>5);
            System.out.println("P03_4_LEGACY_MAPPING_PASS"
                + " curves=" + seen
                + " unique_material_uids=" + resolvedMaterialUids.size()
                + " catalog_size=" + chemicals.count()
                + " canonical_source_link_not_gas_identity=true"
                + " g3=false");
        } finally {
            db.close();
        }
    }
}
