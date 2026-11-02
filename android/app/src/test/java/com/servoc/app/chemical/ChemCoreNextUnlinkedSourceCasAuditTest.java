package com.servoc.app.chemical;

import android.content.Context;
import com.servoc.app.data.ExperimentsDb;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import static org.junit.Assert.*;

/**
 * Standalone ChemCore Next P03.4 source identity audit with real SQLite catalog.
 * Curated NIOSH CAS links are source/model metadata, not MQ-measured identity.
 * No dynamic gas classification, no Android release, no G3 claim.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=32)
public final class ChemCoreNextUnlinkedSourceCasAuditTest {
    private static String norm(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }
    private static boolean same(String a, String b) { return norm(a).equals(norm(b)); }
    private static String safe(String text) {
        return text == null ? "" : text.replace("|", "/").replace("\n", " ");
    }
    private static List<String[]> policyRows(Context context) throws Exception {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
            context.getAssets().open("chemcore_next_p03_4_unlinked_60_source_identity_policy.csv"),
            StandardCharsets.UTF_8))) {
            String header = in.readLine();
            assertNotNull(header);
            assertEquals("source_curve_uid,sensor_model,source_label,source_root_id,"
                + "source_blob_sha,chemical_identity_policy,expected_cas,"
                + "expected_chemical_name,authority_url,classification_reason", header);
            String line;
            while ((line=in.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] values = line.split(",", -1);
                assertEquals("No guessed comma escaping in policy", 10, values.length);
                rows.add(values);
            }
        }
        return rows;
    }
    private static boolean installed(String sensor) {
        return sensor.matches("MQ(?:2|3|4|5|7|8|9|135)");
    }
    @Test public void audit60CandidateSourcesOnActual734EntryLibrary() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        context.deleteDatabase("servoc.db");
        ExperimentsDb db=new ExperimentsDb(context);
        try {
            ChemicalSeedBootstrap.ensureNiosh(context,db);
            ChemicalRepository repository=new ChemicalRepository(db);
            assertEquals("authoritative 734 catalog must already exist",734,repository.count());
            List<ChemicalRepository.Chemical> catalog=repository.all(1000);
            assertEquals(734,catalog.size());
            List<String[]> source=policyRows(context);
            assertEquals(60,source.size());
            Set<String> sourceIds=new HashSet<>();
            int exact=0,ambiguous=0,ionic=0,current=0,matched=0,unresolved=0;
            int currentMatched=0;
            Set<Long> matchedUniqueUids=new HashSet<>();
            for(String[] row:source) {
                String id=row[0],sensor=row[1],label=row[2],root=row[3],
                       sha=row[4],policy=row[5],cas=row[6],name=row[7],authority=row[8];
                assertTrue("unique source UID",sourceIds.add(id));
                assertTrue("original source root required",!root.isEmpty());
                assertTrue("source blob hash required",sha.matches("[0-9a-f]{40}"));
                boolean isCurrent=installed(sensor);
                if(isCurrent) current++;
                String status="SOURCE_ABSTAINS";
                Long selectedUid=null;
                String selectedName="",selectedCas="";
                if("CAS_SPECIES_EXACT_SOURCE_LABEL".equals(policy)) {
                    exact++;
                    assertTrue("source CAS absent",cas.matches("[0-9]+-[0-9]+-[0-9]"));
                    assertTrue("authority reference required",authority.startsWith("https://www.cdc.gov/niosh/npg/"));
                    List<ChemicalRepository.Chemical> direct=new ArrayList<>();
                    for(ChemicalRepository.Chemical c:catalog) if(same(c.cas,cas))direct.add(c);
                    if(direct.size()==1) {
                        ChemicalRepository.Chemical c=direct.get(0);
                        assertEquals("Direct CAS must read back actual UID",c.uid,repository.byUid(c.uid).uid);
                        if(same(c.name,name)) {
                            selectedUid=c.uid;
                            selectedName=c.name;
                            selectedCas=c.cas;
                            status="DIRECT_CAS_AND_NAME_UNIQUE";
                            matched++;
                            matchedUniqueUids.add(c.uid);
                            if(isCurrent)currentMatched++;
                        } else {
                            status="CAS_MATCH_NAME_MISMATCH_REVIEW";
                            unresolved++;
                        }
                    } else if(direct.isEmpty()) {
                        status="CAS_NOT_FOUND_IN_734";
                        unresolved++;
                    } else {
                        status="DUPLICATE_CAS_MASTER_UID_REVIEW";
                        unresolved++;
                    }
                } else if("NH4_ION_SOURCE_LABEL_UNVERIFIED".equals(policy)) {
                    ionic++;
                    assertTrue(label.equals("NH4")||label.equals("NH4_UNRESOLVED_CLASS"));
                    assertEquals("",cas);
                    status="AMMONIUM_ION_NOT_ATMOSPHERIC_GAS";
                    unresolved++;
                } else {
                    ambiguous++;
                    assertEquals("CLASS_MIXTURE_OR_NON_SPECIFIC_NOT_MOLECULE",policy);
                    assertEquals("",cas);
                    status="NON_MOLECULAR_CLASS_ABSTAIN";
                    unresolved++;
                }
                if(selectedUid!=null) assertTrue(selectedUid>0);
                System.out.println("P03_4_CAS_SOURCE_ROW|"+id+"|"+sensor+"|"+safe(label)
                    +"|"+policy+"|"+status+"|"+(selectedUid==null?"":selectedUid)
                    +"|"+safe(selectedName)+"|"+safe(selectedCas)+"|"+root+"|"+sha);
            }
            assertEquals(44,exact);
            assertEquals(14,ambiguous);
            assertEquals(2,ionic);
            assertEquals(13,current);
            assertEquals(60,matched+unresolved);
            assertTrue("source identity audit should resolve some CAS sources",matched>0);
            System.out.println("P03_4_CAS_AUDIT_PASS"
                +" rows=60"
                +" exact_name_cas_policy="+exact
                +" mapped_direct_cas_name="+matched
                +" unresolved="+unresolved
                +" current_sensor_rows="+current
                +" current_sensor_rows_mapped="+currentMatched
                +" unique_master_uids="+matchedUniqueUids.size()
                +" gas_identity_confirmed=0"
                +" g3=0");
        } finally { db.close(); }
    }
}
