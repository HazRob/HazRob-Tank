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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

/**
 * P03.6 Source-qualified hazard/background and LOD COVERAGE, on actual unified
 * seeded SQLite. This never estimates gas concentration, or guesses any limits.
 * Material UID is read from frozen source crosswalk, not from a name lookup.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=32)
public final class ChemCoreNextSourceRiskCoverageAuditTest {
    private static int count(SQLiteDatabase db, String query, long uid) {
        try(Cursor cursor=db.rawQuery(query,new String[]{Long.toString(uid)})){
            assertTrue("SQL count missing",cursor.moveToFirst());
            int value=cursor.getInt(0);
            assertTrue(value>=0);
            return value;
        }
    }
    private static int countNoParam(SQLiteDatabase db, String query) {
        try(Cursor c=db.rawQuery(query,null)) {
            assertTrue(c.moveToFirst());
            return c.getInt(0);
        }
    }
    private static String maybe(String x) {
        return x == null ? "" : x.replace("|","/").replace('\n',' ');
    }
    private static String referenceField(SQLiteDatabase db, long uid, String query) {
        try(Cursor c=db.rawQuery(query,new String[]{Long.toString(uid)})){
            if(!c.moveToFirst())return "";
            return c.isNull(0) ? "" : c.getString(0);
        }
    }
    @Test public void auditActualUnifiedSourceQualifiedRiskCoverage() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        context.deleteDatabase("servoc.db");
        ExperimentsDb db=new ExperimentsDb(context);
        try {
            ChemicalSeedBootstrap.ensureNiosh(context, db);
            // The ONLY canonical material catalog includes the two needed general classes;
            // GHS advisory risks are in the existing classifications, not a parallel table.
            ChemCoreNextGeneralHazardBootstrap.Result groups =
                ChemCoreNextGeneralHazardBootstrap.ensure(db);
            assertEquals(5,groups.catalogIds.size());
            ChemicalRepository repo=new ChemicalRepository(db);
            assertEquals(736,repo.count());
            SQLiteDatabase sql=db.getReadableDatabase();
            Set<Long> inspected = new HashSet<>();
            int withExposure=0,withBackground=0,withQualifiedGhs=0,
                withProvisionalGhs=0,withLfl=0,withUfl=0,withModelLod=0;
            try(BufferedReader file=new BufferedReader(new InputStreamReader(
                context.getAssets().open("chemcore_next_p03_6_21_material_uid_scope.csv"),
                StandardCharsets.UTF_8))) {
                String header=file.readLine();
                assertEquals("master_uid,master_name,master_cas,review_scope,mq_g3_qualified",header);
                String line;
                while((line=file.readLine())!=null) {
                    if(line.trim().isEmpty())continue;
                    String[] record=line.split(",",-1);
                    assertEquals(5,record.length);
                    long uid=Long.parseLong(record[0]);
                    assertTrue("duplicated canonical material UID",inspected.add(uid));
                    assertTrue(uid>0 && uid<=736);
                    assertEquals("NO",record[4]);
                    ChemicalRepository.Chemical material=repo.byUid(uid);
                    assertNotNull("actual master material absent",material);
                    assertEquals(record[1],material.name);
                    assertEquals(record[2],maybe(material.cas));

                    int exposure=count(sql,
                        "SELECT COUNT(*) FROM exposure_limits e WHERE e.chemical_uid=? " +
                        "AND e.value IS NOT NULL AND e.unit IS NOT NULL AND " +
                        "EXISTS(SELECT 1 FROM scientific_sources s WHERE s.source_id=e.source_id)",
                        uid);
                    int background=count(sql,
                        "SELECT COUNT(*) FROM chemical_background_ranges b WHERE b.chemical_uid=? " +
                        "AND b.range_min IS NOT NULL AND b.range_max IS NOT NULL " +
                        "AND b.unit IS NOT NULL AND b.qualification_status='SOURCE_QUALIFIED' " +
                        "AND EXISTS(SELECT 1 FROM scientific_sources s WHERE s.source_id=b.source_id)",
                        uid);
                    int backgroundResearch=count(sql,
                        "SELECT COUNT(*) FROM chemical_background_ranges WHERE chemical_uid=?",
                        uid);
                    int qualifiedGhs=count(sql,
                        "SELECT COUNT(*) FROM chemical_hazard_classifications h " +
                        "WHERE h.chemical_uid=? AND h.evidence_status='SOURCE_QUALIFIED' " +
                        "AND h.ghs_pictogram IS NOT NULL AND " +
                        "EXISTS(SELECT 1 FROM scientific_sources s WHERE s.source_id=h.source_id)",
                        uid);
                    int provisionalGhs=count(sql,
                        "SELECT COUNT(*) FROM chemical_hazard_classifications h " +
                        "WHERE h.chemical_uid=? AND h.evidence_status='PROVISIONAL' " +
                        "AND h.ghs_pictogram IS NOT NULL AND " +
                        "EXISTS(SELECT 1 FROM scientific_sources s WHERE s.source_id=h.source_id)",
                        uid);
                    int lowerFlammability=count(sql,
                        "SELECT COUNT(*) FROM exposure_limits e " +
                        "WHERE e.chemical_uid=? AND UPPER(e.limit_type) IN ('LFL','LEL') " +
                        "AND LOWER(e.unit) IN ('%vol','vol%') AND e.value>0 " +
                        "AND EXISTS(SELECT 1 FROM scientific_sources s WHERE s.source_id=e.source_id)",
                        uid);
                    int upperFlammability=count(sql,
                        "SELECT COUNT(*) FROM exposure_limits e " +
                        "WHERE e.chemical_uid=? AND UPPER(e.limit_type) IN ('UFL','UEL') " +
                        "AND LOWER(e.unit) IN ('%vol','vol%') AND e.value>0 " +
                        "AND EXISTS(SELECT 1 FROM scientific_sources s WHERE s.source_id=e.source_id)",
                        uid);
                    int modelLod=count(sql,
                        "SELECT COUNT(*) FROM sensor_evidence e WHERE e.chemical_uid=? " +
                        "AND e.evidence_type IN ('LOD','LOQ') " +
                        "AND e.status='G3_QUALIFIED'",
                        uid);
                    int enabledAlarm=count(sql,
                        "SELECT COUNT(*) FROM chemical_risk_profiles WHERE chemical_uid=? " +
                        "AND occupational_alarm_authorized<>0",uid);
                    assertEquals("research-only catalog cannot activate occupational alarms",
                        0,enabledAlarm);
                    assertEquals("no actual Tank G3 LOD is qualified in current research",
                        0,modelLod);
                    if(exposure>0)withExposure++;
                    if(background>0)withBackground++;
                    if(qualifiedGhs>0)withQualifiedGhs++;
                    if(provisionalGhs>0)withProvisionalGhs++;
                    if(lowerFlammability>0)withLfl++;
                    if(upperFlammability>0)withUfl++;
                    if(modelLod>0)withModelLod++;
                    String lflVal=referenceField(sql,uid,
                        "SELECT CAST(value AS TEXT) FROM exposure_limits " +
                        "WHERE chemical_uid=? AND UPPER(limit_type) IN ('LFL','LEL') " +
                        "AND LOWER(unit) IN ('%vol','vol%') AND value>0 " +
                        "AND source_id IS NOT NULL ORDER BY id LIMIT 1");
                    String lflSource=referenceField(sql,uid,
                        "SELECT source_id FROM exposure_limits " +
                        "WHERE chemical_uid=? AND UPPER(limit_type) IN ('LFL','LEL') " +
                        "AND LOWER(unit) IN ('%vol','vol%') AND value>0 " +
                        "AND source_id IS NOT NULL ORDER BY id LIMIT 1");
                    String uflVal=referenceField(sql,uid,
                        "SELECT CAST(value AS TEXT) FROM exposure_limits " +
                        "WHERE chemical_uid=? AND UPPER(limit_type) IN ('UFL','UEL') " +
                        "AND LOWER(unit) IN ('%vol','vol%') AND value>0 " +
                        "AND source_id IS NOT NULL ORDER BY id LIMIT 1");
                    String uflSource=referenceField(sql,uid,
                        "SELECT source_id FROM exposure_limits " +
                        "WHERE chemical_uid=? AND UPPER(limit_type) IN ('UFL','UEL') " +
                        "AND LOWER(unit) IN ('%vol','vol%') AND value>0 " +
                        "AND source_id IS NOT NULL ORDER BY id LIMIT 1");
                    System.out.println("NEXT_P03_6_RISK_ROW|"+uid+"|"+maybe(material.name)+
                        "|"+maybe(material.cas)+"|"+exposure+"|"+background+"|"+
                        backgroundResearch+"|"+qualifiedGhs+"|"+provisionalGhs+"|"+
                        lowerFlammability+"|"+upperFlammability+"|"+maybe(lflVal)+"|"+
                        maybe(lflSource)+"|"+maybe(uflVal)+"|"+maybe(uflSource)+"|"+
                        "UNKNOWN_NOT_MEASURED"+"|"+modelLod+"|"+enabledAlarm+"|"+
                        "NOT_DEVICE_G3");
                }
            }
            assertEquals(21,inspected.size());
            assertEquals(0,withModelLod);
            assertEquals(0,countNoParam(sql,
                "SELECT COUNT(*) FROM sqlite_master WHERE type IN ('table','view') " +
                "AND name LIKE 'chemcore_next_group_hazard%'"));
            System.out.println("CHEMCORE_NEXT_P03_6_RISK_COVERAGE_PASS"
                +" catalog=736 materials=21 with_exposure="+withExposure
                +" with_qualified_background="+withBackground
                +" with_qualified_ghs="+withQualifiedGhs
                +" with_provisional_ghs="+withProvisionalGhs
                +" with_LFL="+withLfl+" with_UFL="+withUfl
                +" qualified_LOD=0 occupational_alarm=0 Tank_G3=0");
        } finally {
            db.close();
        }
    }
}
