package com.servoc.app.measurement;

/**
 * Pure state and visibility policy for Tank's instant and documented acquisitions.
 * UI labels deliberately collapse every non-approved field capture to NEW.
 * Hardware timing is owned by the firmware; this class never starts/stops a motor or sensor.
 */
public final class TankAcquisitionPolicy {
    public enum Mode { INSTANT, DOCUMENTED }
    public enum State { NEW, WARMUP, MEASURING, COMPLETED, CANCELLED, APPROVED }
    private TankAcquisitionPolicy() {}

    public static boolean canStart(State state) {
        return state == State.NEW || state == State.CANCELLED || state == State.COMPLETED;
    }
    public static boolean canApprove(Mode mode, State state, boolean hasValidSamples) {
        return mode == Mode.DOCUMENTED && state == State.COMPLETED && hasValidSamples;
    }
    public static boolean canCancel(State state) {
        return state == State.COMPLETED;
    }
    public static boolean isApproved(State state) {
        return state == State.APPROVED;
    }
    public static String visibleFieldStatus(State state) {
        return isApproved(state) ? "معتمدة" : "جديدة";
    }
    public static boolean showFinalFieldReports(State state) {
        return isApproved(state);
    }
    public static boolean showInFieldReportPicker(State state) {
        return isApproved(state);
    }
    public static boolean canDeleteSite(int childSessionCount) {
        return childSessionCount == 0;
    }
    public static boolean canTrackWithoutSession(boolean sensorsPowered) {
        return sensorsPowered;
    }
    public static boolean shouldShowGenericMqWarnings(boolean sensorsPowered) {
        return sensorsPowered;
    }
}
