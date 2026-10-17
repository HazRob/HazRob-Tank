package com.servoc.app.measurement;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.servoc.app.measurement.TankAcquisitionPolicy.*;

public class TankAcquisitionPolicyTest {
    @Test public void onlyApprovedSessionsAreVisibleToReports() {
        for (State s : State.values()) {
            assertEquals(s == State.APPROVED, showInFieldReportPicker(s));
            assertEquals(s == State.APPROVED, showFinalFieldReports(s));
            assertEquals(s == State.APPROVED ? "معتمدة" : "جديدة", visibleFieldStatus(s));
        }
    }
    @Test public void approvalRequiresCompletedDocumentedValidAcquisition() {
        assertTrue(canApprove(Mode.DOCUMENTED, State.COMPLETED, true));
        assertFalse(canApprove(Mode.INSTANT, State.COMPLETED, true));
        assertFalse(canApprove(Mode.DOCUMENTED, State.MEASURING, true));
        assertFalse(canApprove(Mode.DOCUMENTED, State.COMPLETED, false));
    }
    @Test public void newAndCancelledCanBeRetriedButApprovedCannot() {
        assertTrue(canStart(State.NEW));
        assertTrue(canStart(State.CANCELLED));
        assertTrue(canStart(State.COMPLETED));
        assertFalse(canStart(State.WARMUP));
        assertFalse(canStart(State.MEASURING));
        assertFalse(canStart(State.APPROVED));
        assertTrue(canCancel(State.COMPLETED));
        assertFalse(canCancel(State.MEASURING));
    }
    @Test public void siteMustBeEmptyBeforeDeletion() {
        assertTrue(canDeleteSite(0));
        assertFalse(canDeleteSite(1));
    }
    @Test public void liveWarningsAndTrackingDoNotRequireSessions() {
        assertTrue(shouldShowGenericMqWarnings(true));
        assertTrue(canTrackWithoutSession(true));
        assertFalse(shouldShowGenericMqWarnings(false));
    }
}
