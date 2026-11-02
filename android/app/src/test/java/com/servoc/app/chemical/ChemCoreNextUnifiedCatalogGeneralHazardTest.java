package com.servoc.app.chemical;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import com.servoc.app.data.ExperimentsDb;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/**
 * Scientific ChemCore Next verification on the SAME 734-entry master SQLite.
 * Not Android V8 integration or a device/ppm/G3 test.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=32)
public final class ChemCoreNextUnifiedCatalogGeneralHazardTest {
    private static long count(SQLiteDatabase db,String sql) {
        try (Cursor c=db.rawQuery(sql,null)) {
            assertTrue("count query returned no rows",c.moveToFirst());
            return c.getLong(0);
        }
    }
    private static long legacyUid(SQLiteDatabase sql,String name,int legacy) {
        try(Cursor c=sql.rawQuery(
            "SELECT uid FROM chemicals WHERE name_en=? AND legacy_id=? AND active=1",
            new String[]{name,Integer.toString(legacy)})) {
            assertTrue("missing original historical class "+name,c.moveToFirst());
            long id=c.getLong(0);
            assertFalse("duplicate historical class "+name,c.moveToNext());
            return id;
        }
    }
    private static void assertMasterRow(SQLiteDatabase sql,long uid,String expectedClass,
                                        boolean requireNonLegacy) {
        try(Cursor c=sql.rawQuery(
            "SELECT legacy_id,cas,formula,chemical_class,active " +
            "FROM chemicals WHERE uid=?",new String[]{Long.toString(uid)})) {
            assertTrue("missing master UID",c.moveToFirst());
            if(requireNonLegacy)assertTrue("new class must not steal old legacy ID",c.isNull(0));
            assertTrue("general class must not fabricate CAS",c.isNull(1));
            assertTrue("general class must not fabricate molecule formula",
                c.isNull(2) || c.getString(2).trim().isEmpty());
            assertEquals(expectedClass,c.getString(3));
            assertEquals(1,c.getInt(4));
        }
    }
    @Test public void useExistingUnifiedMaterialAndHazardTablesOnly() {
        Context context=ApplicationProvider.getApplicationContext();
        context.deleteDatabase("servoc.db");
        ExperimentsDb db=new ExperimentsDb(context);
        try {
            ChemicalSeedBootstrap.ensureNiosh(context,db);
            SQLiteDatabase sql=db.getWritableDatabase();
            assertEquals(734,count(sql,"SELECT COUNT(*) FROM chemicals WHERE active=1"));
            long lpg0=legacyUid(sql,"LPG",135);
            long smoke0=legacyUid(sql,"Smoke",139);
            long nox0=legacyUid(sql,"NOx",141);
            long classificationsBefore=count(sql,
                "SELECT COUNT(*) FROM chemical_hazard_classifications");

            ChemCoreNextGeneralHazardBootstrap.Result first =
                ChemCoreNextGeneralHazardBootstrap.ensure(db);
            assertEquals(5,first.catalogIds.size());
            assertEquals("two genuinely absent generic source categories",2,
                count(sql,"SELECT COUNT(*) FROM chemicals WHERE record_source='"+
                    ChemCoreNextGeneralIdentityBootstrap.ORIGIN+"'"));
            assertEquals(736,count(sql,"SELECT COUNT(*) FROM chemicals WHERE active=1"));
            assertEquals(736,count(sql,"SELECT COUNT(DISTINCT uid) FROM chemicals WHERE active=1"));
            assertEquals(lpg0,first.catalogIds.get("LPG").longValue());
            assertEquals(smoke0,first.catalogIds.get("Smoke").longValue());
            assertEquals(nox0,first.catalogIds.get("NOx").longValue());
            assertMasterRow(sql,lpg0,"MIXTURE_FUEL",false);
            assertMasterRow(sql,smoke0,"MIXTURE_COMPLEX_AEROSOL",false);
            assertMasterRow(sql,nox0,"AGGREGATE_REACTIVE_GAS_FAMILY",false);

            long alcohols=first.catalogIds.get("Alcohols (general)");
            long ammonium=first.catalogIds.get("Ammonium-containing materials (NH4+ source class)");
            assertMasterRow(sql,alcohols,"RESEARCH_CHEMICAL_FAMILY_NOT_SPECIFIC_MOLECULE",true);
            assertMasterRow(sql,ammonium,"AMMONIUM_ION_SOURCE_CLASS_NOT_GAS",true);
            assertNotEquals("NH4+ category not identical to ammonia molecular record",22L,ammonium);
            ChemicalRepository repo=new ChemicalRepository(db);
            assertEquals(736,repo.count());
            assertNotNull(repo.byUid(ammonium));
            assertNotNull(repo.byUid(alcohols));

            // Seven actual annotations inserted INTO the existing shared hazard classifications table.
            assertEquals(7,first.inserted);
            assertEquals(0,first.existing);
            assertEquals(7,first.totalFacts);
            assertEquals(classificationsBefore+7,count(sql,
                "SELECT COUNT(*) FROM chemical_hazard_classifications"));
            assertEquals(7,count(sql,
                "SELECT COUNT(*) FROM chemical_hazard_classifications " +
                "WHERE source_id='CHEMCORE_NEXT_GENERAL_HAZARD_RESEARCH'"));
            assertEquals(1,count(sql,
                "SELECT COUNT(*) FROM chemical_hazard_classifications " +
                "WHERE source_id='CHEMCORE_NEXT_GENERAL_HAZARD_RESEARCH' " +
                "AND evidence_status='SOURCE_QUALIFIED'"));
            assertEquals(6,count(sql,
                "SELECT COUNT(*) FROM chemical_hazard_classifications " +
                "WHERE source_id='CHEMCORE_NEXT_GENERAL_HAZARD_RESEARCH' " +
                "AND evidence_status='PROVISIONAL'"));
            assertEquals(1,count(sql,
                "SELECT COUNT(*) FROM chemical_hazard_classifications " +
                "WHERE chemical_uid="+lpg0+" AND ghs_pictogram='GHS02' AND " +
                "source_id='CHEMCORE_NEXT_GENERAL_HAZARD_RESEARCH' " +
                "AND evidence_status='SOURCE_QUALIFIED'"));
            assertEquals(0,count(sql,
                "SELECT COUNT(*) FROM chemical_hazard_classifications " +
                "WHERE chemical_uid="+nox0+" AND evidence_status='SOURCE_QUALIFIED' " +
                "AND source_id='CHEMCORE_NEXT_GENERAL_HAZARD_RESEARCH'"));
            assertEquals(0,count(sql,
                "SELECT COUNT(*) FROM chemical_hazard_classifications " +
                "WHERE chemical_uid="+ammonium+" AND evidence_status='SOURCE_QUALIFIED'"));
            assertTrue(ChemCoreNextGeneralHazardBootstrap.qualifiedIcons(db,lpg0).contains("GHS02"));
            assertTrue(ChemCoreNextGeneralHazardBootstrap.qualifiedIcons(db,nox0).isEmpty());
            assertTrue(ChemCoreNextGeneralHazardBootstrap.qualifiedIcons(db,smoke0).isEmpty());
            assertTrue(ChemCoreNextGeneralHazardBootstrap.qualifiedIcons(db,alcohols).isEmpty());
            assertTrue(ChemCoreNextGeneralHazardBootstrap.qualifiedIcons(db,ammonium).isEmpty());
            assertTrue("existing renderer only shows source-qualified GHS for LPG",
                GhsPictograms.forChemical(db,lpg0).contains("GHS02"));
            // No product SDS icon issued for provisional NOx, smoke, alcohol family or NH4 class.
            assertFalse("GHS03 cannot be generally certified for NOx",
                GhsPictograms.forChemical(db,nox0).contains("GHS03"));
            assertFalse(GhsPictograms.forChemical(db,ammonium).contains("GHS07"));

            Set<String> icons=new HashSet<>();
            List<ChemCoreNextGeneralHazardBootstrap.Fact> nitrogen =
                ChemCoreNextGeneralHazardBootstrap.forMasterCatalog(db,nox0);
            assertEquals(3,nitrogen.size());
            for(ChemCoreNextGeneralHazardBootstrap.Fact fact:nitrogen) {
                icons.add(fact.icon);
                assertEquals("PROVISIONAL",fact.evidenceStatus);
                assertTrue(fact.sourceUrl.startsWith("https://"));
                assertTrue(fact.explanation.length()>35);
            }
            assertTrue(icons.contains("GHS03"));
            assertTrue(icons.contains("GHS05"));
            assertTrue(icons.contains("GHS06"));

            // Not a new parallel SQLite hazard table, view or materials library.
            assertEquals(0,count(sql,
                "SELECT COUNT(*) FROM sqlite_master WHERE type IN ('table','view') " +
                "AND name IN ('chemcore_next_group_hazard_advisories'," +
                "'chemcore_next_general_hazard_catalog')"));
            ChemCoreNextGeneralHazardBootstrap.Result second =
                ChemCoreNextGeneralHazardBootstrap.ensure(db);
            assertEquals(0,second.inserted);
            assertEquals(7,second.existing);
            assertEquals(736,repo.count());
            assertEquals(classificationsBefore+7,count(sql,
                "SELECT COUNT(*) FROM chemical_hazard_classifications"));
            assertEquals(lpg0,second.catalogIds.get("LPG").longValue());
            assertEquals(ammonium,second.catalogIds.get(
                "Ammonium-containing materials (NH4+ source class)").longValue());
            System.out.println("CHEMCORE_NEXT_SINGLE_UNIFIED_GHS_CATALOG_PASS"
                +" before=734 after=736 existing_legacy_reused=3"
                +" added_generic_categories=2 hazards=7 verified=1 provisional=6"
                +" second_run_extra=0 separate_hazard_tables=0 gas_identity=0 G3=0");
        } finally {db.close();}
    }
}
