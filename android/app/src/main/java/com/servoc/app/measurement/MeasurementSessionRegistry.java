package com.servoc.app.measurement;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Process-local bridge for the single TelemetryRecorder session and explicit ChemCore capture
 * requests. It carries identifiers/control intent only; telemetry and scientific records remain
 * persisted in SQLite by TelemetryRecorder.
 */
public final class MeasurementSessionRegistry {
    private static final AtomicReference<String> ACTIVE = new AtomicReference<>("");
    private static final AtomicReference<String> LAST_COMPLETED = new AtomicReference<>("");
    private static final AtomicBoolean CHEMCORE_CAPTURE = new AtomicBoolean(false);
    private static final AtomicBoolean DEVELOPER_MQ_CYCLE = new AtomicBoolean(false);
    private static final AtomicBoolean CHEMCORE_DEVELOPER = new AtomicBoolean(true);
    private static final AtomicReference<String> CHEMCORE_STOP_REASON = new AtomicReference<>("");
    // V5: a user's documented 60 s capture is distinct from scientific ChemCore.
    private static final AtomicBoolean FIELD_CAPTURE = new AtomicBoolean(false);
    private static final AtomicBoolean FIELD_OPERATION = new AtomicBoolean(false);
    private static final AtomicReference<String> FIELD_STOP_REASON = new AtomicReference<>("");
    private static final AtomicReference<String> FIELD_OWNER_SESSION = new AtomicReference<>("");


    private MeasurementSessionRegistry() {}

    static void started(String sessionId) { ACTIVE.set(sessionId == null ? "" : sessionId); }

    static void completed(String sessionId) {
        String value = sessionId == null ? "" : sessionId;
        LAST_COMPLETED.set(value);
        ACTIVE.compareAndSet(value, "");
    }

    static void resetActive(String expected) {
        if (expected == null || expected.isEmpty()) ACTIVE.set("");
        else ACTIVE.compareAndSet(expected, "");
    }

    public static String activeSessionId() { String s=ACTIVE.get();return s==null?"":s; }
    public static String lastCompletedSessionId() { String s=LAST_COMPLETED.get();return s==null?"":s; }

    /** Explicit origin tag for a developer-started firmware averaging cycle. */
    public static void markDeveloperMqCycle(){DEVELOPER_MQ_CYCLE.set(true);}
    public static boolean consumeDeveloperMqCycle(){return DEVELOPER_MQ_CYCLE.getAndSet(false);}
    public static void clearDeveloperMqCycle(){DEVELOPER_MQ_CYCLE.set(false);}

    /** Requests continuous local telemetry capture even while the legacy ESP32 measurement state is idle. */
    public static synchronized boolean requestChemCoreCapture(boolean developer) {
        if (!ACTIVE.get().isEmpty() || FIELD_CAPTURE.get() || FIELD_OPERATION.get()) return false;
        CHEMCORE_DEVELOPER.set(developer);
        CHEMCORE_STOP_REASON.set("");
        return CHEMCORE_CAPTURE.compareAndSet(false, true);
    }

    /** Blocks R0 edits, Stable updates and developer captures for the full 60+60 run. */
    public static synchronized boolean reserveFieldOperation(){
        if(FIELD_OPERATION.get()||CHEMCORE_CAPTURE.get()||
            !ACTIVE.get().isEmpty())return false;
        return FIELD_OPERATION.compareAndSet(false,true);
    }
    public static boolean fieldOperationActive(){return FIELD_OPERATION.get();}
    public static void endFieldOperation(){FIELD_OPERATION.set(false);}

    /** Explicit, operator-owned documented capture. Never a developer experiment. */
    public static synchronized boolean requestFieldCapture(String fieldSessionId){
        String owner=fieldSessionId==null?"":fieldSessionId.trim();
        if(owner.isEmpty()||!FIELD_OPERATION.get()||FIELD_CAPTURE.get()||
            CHEMCORE_CAPTURE.get()||!ACTIVE.get().isEmpty())return false;
        FIELD_STOP_REASON.set("");
        FIELD_OWNER_SESSION.set(owner);
        boolean reserved=FIELD_CAPTURE.compareAndSet(false,true);
        if(!reserved)FIELD_OWNER_SESSION.set("");
        return reserved;
    }
    /** Owner is immutable until this field capture has finished or failed. */
    public static String fieldCaptureOwnerSessionId(){
        return FIELD_CAPTURE.get()?FIELD_OWNER_SESSION.get():"";
    }
    public static boolean fieldCaptureRequested(){return FIELD_CAPTURE.get();}
    public static boolean requestFieldStop(String reason){
        if(!FIELD_CAPTURE.get())return false;
        String value=reason==null||reason.trim().isEmpty()?"cancelled":reason.trim();
        if(!"ready".equals(value)&&!"cancelled".equals(value)&&!"error".equals(value))
            throw new IllegalArgumentException("Invalid field capture terminal state");
        return FIELD_STOP_REASON.compareAndSet("",value);
    }
    public static String fieldStopReason(){return FIELD_STOP_REASON.get();}
    static void clearFieldCapture(){
        FIELD_CAPTURE.set(false);FIELD_STOP_REASON.set("");FIELD_OWNER_SESSION.set("");
    }

    /** Requests graceful finalization on the next telemetry snapshot. */
    public static boolean requestChemCoreStop(String terminalReason) {
        if (!CHEMCORE_CAPTURE.get()) return false;
        String reason=terminalReason==null||terminalReason.trim().isEmpty()?"chemcore_complete":terminalReason.trim();
        CHEMCORE_STOP_REASON.set(reason);
        return true;
    }

    public static boolean chemCoreCaptureRequested() { return CHEMCORE_CAPTURE.get(); }
    static boolean chemCoreDeveloper() { return CHEMCORE_DEVELOPER.get(); }
    static String chemCoreStopReason() { String s=CHEMCORE_STOP_REASON.get();return s==null?"":s; }

    static void clearChemCoreCapture() {
        CHEMCORE_CAPTURE.set(false);
        CHEMCORE_STOP_REASON.set("");
        CHEMCORE_DEVELOPER.set(true);
    }
}
