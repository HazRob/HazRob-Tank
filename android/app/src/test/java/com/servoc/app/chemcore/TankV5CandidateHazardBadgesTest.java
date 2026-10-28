package com.servoc.app.chemcore;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public final class TankV5CandidateHazardBadgesTest {
    private static JSONObject record(long uid,String role,double fraction) throws Exception {
        return new JSONObject().put("chemical_uid",uid).put("reference_role",role)
            .put("ratio_to_reference",fraction).put("conditional",true);
    }
    private static JSONObject analysis(JSONObject... warnings)throws Exception {
        JSONArray a=new JSONArray();for(JSONObject w:warnings)a.put(w);
        return new JSONObject().put(TankScreeningExposureWarning.KEY,
            new JSONObject().put("warnings",a));
    }
    @Test public void exposureAndLflOnSameCandidateShowBothIcons()throws Exception{
        String s=TankCandidateHazardBadges.forCandidate(analysis(
            record(64,"CEILING",1.2),record(64,"LFL",1.1)),64);
        assertTrue(s.contains("☠"));assertTrue(s.contains("🔥"));
        assertTrue(s.contains("تقديري/مشروط"));
    }
    @Test public void candidateBadgesNeverLeakToAnotherUid()throws Exception{
        JSONObject a=analysis(record(64,"IDLH",1.3),record(64,"LFL",1.4));
        assertEquals("",TankCandidateHazardBadges.forCandidate(a,65));
        assertEquals("",TankCandidateHazardBadges.forCandidate(a,-1));
    }
    @Test public void earlyLflIsNotRepresentedAsFireReached()throws Exception{
        String s=TankCandidateHazardBadges.forCandidate(analysis(record(64,"LFL",0.19)),64);
        assertFalse(s.contains("🔥"));assertTrue(s.contains("⚠"));
    }
    @Test public void twaWarningExplicitlyStatesHypotheticalDuration()throws Exception{
        String s=TankCandidateHazardBadges.forCandidate(analysis(record(64,"TWA",1.25)),64);
        assertTrue(s.contains("☠"));assertTrue(s.contains("افتراضي"));
    }
    @Test public void absentOrInvalidEstimateDoesNotProduceSafetyIcons()throws Exception{
        assertEquals("",TankCandidateHazardBadges.forCandidate(null,64));
        assertEquals("",TankCandidateHazardBadges.forCandidate(analysis(record(64,"LFL",Double.NaN)),64));
        assertEquals("",TankCandidateHazardBadges.forCandidate(analysis(record(64,"CEILING",0.8)),64));
    }
}
