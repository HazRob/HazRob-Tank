package com.servoc.app.chemical;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import com.servoc.app.data.ExperimentsDb;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/**
 * P03.4 ChemCore Next source class linkage to SAME master material catalog.
 * Links do not establish gas identification or a chemically pure analyte.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=32)
public final class ChemCoreNextGeneralSourceAliasTest {
    @Test public void mapSixteenGenericSourceNamesToFiveExistingOrAddedMasterCategoryUids()
            throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        context.deleteDatabase("servoc.db");
        ExperimentsDb db=new ExperimentsDb(context);
        try {
            ChemicalSeedBootstrap.ensureNiosh(context, db);
            assertEquals(734,new ChemicalRepository(db).count());
            ChemCoreNextGeneralHazardBootstrap.Result bootstrap =
                ChemCoreNextGeneralHazardBootstrap.ensure(db);
            assertEquals(5,bootstrap.catalogIds.size());
            SQLiteDatabase sql=db.getReadableDatabase();
            Map<String,Integer> occurrences=new HashMap<>();
            Set<String> sourceUids=new HashSet<>();
            int mapped=0, unchangedOriginal=0;
            try(BufferedReader input=new BufferedReader(new InputStreamReader(
                context.getAssets().open("chemcore_next_p03_4_unlinked_60_source_identity_policy.csv"),
                StandardCharsets.UTF_8))) {
                assertNotNull(input.readLine()); // guaranteed by previous P03.4 CSV source contract
                String line;
                while((line=input.readLine())!=null){
                    if(line.trim().isEmpty())continue;
                    String[] row=line.split(",",-1);
                    assertEquals(10,row.length);
                    String sourceId=row[0],model=row[1],originalLabel=row[2],
                           categoryStatus=row[5];
                    if("CAS_SPECIES_EXACT_SOURCE_LABEL".equals(categoryStatus))continue;
                    String normalized=originalLabel.trim().toLowerCase(java.util.Locale.ROOT);
                    String masterName,expectedClass;
                    switch(normalized) {
                        case "lpg":
                            masterName="LPG";expectedClass="MIXTURE_FUEL";break;
                        case "smoke":
                            masterName="Smoke";expectedClass="MIXTURE_COMPLEX_AEROSOL";break;
                        case "nox":
                            masterName="NOx";expectedClass="AGGREGATE_REACTIVE_GAS_FAMILY";break;
                        case "alcohol":
                            masterName=ChemCoreNextGeneralIdentityBootstrap.ALCOHOLS;
                            expectedClass="RESEARCH_CHEMICAL_FAMILY_NOT_SPECIFIC_MOLECULE";break;
                        case "nh4":
                        case "nh4_unresolved_class":
                            masterName=ChemCoreNextGeneralIdentityBootstrap.AMMONIUM;
                            expectedClass="AMMONIUM_ION_SOURCE_CLASS_NOT_GAS";break;
                        default:
                            throw new IllegalArgumentException("Unresolved source category: "+originalLabel);
                    }
                    Long uid=bootstrap.catalogIds.get(masterName);
                    assertNotNull("category UID unavailable",uid);
                    assertTrue(sourceUids.add(sourceId));
                    try(Cursor c=sql.rawQuery(
                        "SELECT name_en,chemical_class,cas,formula,legacy_id FROM chemicals " +
                        "WHERE uid=? AND active=1",new String[]{Long.toString(uid)})) {
                        assertTrue(c.moveToFirst());
                        assertEquals(masterName,c.getString(0));
                        assertEquals(expectedClass,c.getString(1));
                        assertTrue("generic master entry must not claim one pure chemical CAS",
                            c.isNull(2));
                        assertTrue("generic master entry must not claim molecule formula",
                            c.isNull(3));
                        if("LPG".equals(masterName)||"Smoke".equals(masterName)||
                            "NOx".equals(masterName)) {
                            assertFalse("existing generic must retain original legacy reference",
                                c.isNull(4));
                            unchangedOriginal++;
                        } else assertTrue("new classes never repurpose historical IDs",c.isNull(4));
                        assertFalse(c.moveToNext());
                    }
                    occurrences.put(masterName,occurrences.getOrDefault(masterName,0)+1);
                    mapped++;
                    System.out.println("P03_4_GENERAL_SOURCE_ROW|"+sourceId+"|"+model+"|"+
                        originalLabel+"|"+masterName+"|"+uid+"|"+
                        "CLASS_REFERENCE_ONLY_NOT_CONFIRMED_GAS");
                }
            }
            assertEquals(16,mapped);
            assertEquals(7,occurrences.get(ChemCoreNextGeneralIdentityBootstrap.ALCOHOLS).intValue());
            assertEquals(4,occurrences.get("LPG").intValue());
            assertEquals(2,occurrences.get("NOx").intValue());
            assertEquals(1,occurrences.get("Smoke").intValue());
            assertEquals(2,occurrences.get(ChemCoreNextGeneralIdentityBootstrap.AMMONIUM).intValue());
            assertEquals(7,unchangedOriginal);
            assertEquals(736,new ChemicalRepository(db).count());
            assertEquals(0,sql.rawQuery(
                "SELECT uid FROM chemicals WHERE name_en='NH4 gas' LIMIT 1",null).getCount());
            System.out.println("CHEMCORE_NEXT_GENERAL_ALIAS_MAPPING_PASS"
                +" source_curves=16 master_generic_uids=5"
                +" unchanged_existing_groups=3 novel_groups=2"
                +" master_total=736 confirmed_gases=0 g3=0");
        } finally {
            db.close();
        }
    }
}
