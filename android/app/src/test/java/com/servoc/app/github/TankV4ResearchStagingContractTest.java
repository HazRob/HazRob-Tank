package com.servoc.app.github;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Source-contract checks only. An authenticated GitHub PUT/read-back remains an integration test. */
public final class TankV4ResearchStagingContractTest {
    private static String source(String path)throws Exception{
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/"+path)),
                StandardCharsets.UTF_8);
    }

    @Test public void tankOnlyBranchAndNonRuntimeReviewLocation()throws Exception{
        String pub=source("github/GitHubReferencePublisher.java");
        assertTrue(pub.contains("REPOSITORY=\"HazRob/HazRob-Tank\""));
        assertTrue(pub.contains("BRANCH=\"research/tank-v4-review-staging\""));
        assertTrue(pub.contains("LIBRARY_ROOT=\"research_data/tank_v4_scientific_review\""));
        assertFalse(pub.contains("sohpany/SERVOC-Android"));
        assertFalse(pub.contains("release/hazrob-0.10.2-rc2"));
        assertFalse(pub.contains("research_data/hr069_developer_submissions"));
    }

    @Test public void uploadCannotReportSuccessBeforeReadbackHash()throws Exception{
        String pub=source("github/GitHubReferencePublisher.java");
        assertTrue(pub.contains("if(!path.startsWith(LIBRARY_ROOT+\"/\"))"));
        assertTrue(pub.contains("if(code!=201)return new Result(false"));
        assertTrue(pub.contains("if(!readBackMatches(secret,path,payloadHash))"));
        assertTrue(pub.contains("expectedHash.equalsIgnoreCase(sha256(verified))"));
        assertTrue(pub.contains("verified.length==item.optLong(\"size\",-1)"));
        assertTrue(pub.contains("return new Result(true,code,path,commitSha"));
        assertTrue(pub.contains("if(!ExperimentQueries.isDeveloperReference(db,record.id)"));
    }

    @Test public void uploadIsOperatorInitiatedAndNeverAnAutomaticStablePromotion()throws Exception{
        String ui=source("ui/DeveloperExperimentsTab.java");
        assertTrue(ui.contains("ReferenceLineageGuard.valid(host.database(),r)"));
        assertTrue(ui.contains(".setPositiveButton(\"إرسال\","));
        assertTrue(ui.contains("GitHubReferencePublisher.publish(token,host.database(),r)"));
        assertTrue(ui.contains("STAGED_FOR_REVIEW"));
        assertFalse(ui.contains("منطقة مراجعة HR069-R1"));
        assertFalse(ui.contains("التجارب المرجعية • HazRob Tank V4"));
    }
}
