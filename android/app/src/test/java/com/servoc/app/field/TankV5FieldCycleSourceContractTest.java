package com.servoc.app.field;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Static source regression only. Real SQLite migration and Android firmware tests are separate. */
public final class TankV5FieldCycleSourceContractTest {
    private static String read(String path)throws Exception{
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/"+path)),
                StandardCharsets.UTF_8);
    }

    @Test public void database10IsAdditiveAndImportsExistingV9Links()throws Exception{
        String db=read("data/ExperimentsDb.java");
        assertTrue(db.contains("DB_VERSION=10"));
        assertTrue(db.contains("createV9UserFieldTables(db);createV10FieldCycleTables(db)"));
        assertTrue(db.contains("if(oldVersion<10){createV10FieldCycleTables(db);importV9FieldCycleLinks(db);}"));
        assertTrue(db.contains("CREATE TABLE IF NOT EXISTS field_measurement_cycles("));
        assertTrue(db.contains("UNIQUE(field_session_id,sequence_no)"));
        assertTrue(db.contains("measurement_session_id TEXT UNIQUE"));
        assertTrue(db.contains("INSERT OR IGNORE INTO field_measurement_cycles("));
        assertTrue(db.contains("'LEGACY-'||m.measurement_session_id"));
        assertFalse(db.contains("'LEGACY-'+m.measurement_session_id"));
        assertTrue(db.contains("CASE WHEN f.status='APPROVED' THEN 'LEGACY_APPROVED' ELSE 'LEGACY_IMPORTED' END"));
        assertFalse(db.contains("DROP TABLE field_sessions"));
    }

    @Test public void userBoundCyclesAreUniqueAndApprovalIsEvidenceGated()throws Exception{
        String cycles=read("field/FieldCycleRepository.java");
        String field=read("field/FieldSessionRepository.java");
        assertTrue(cycles.contains("include_gas"));
        assertTrue(cycles.contains("include_particles"));
        assertTrue(cycles.contains("include_physical"));
        assertTrue(cycles.contains("public boolean relayConfirmed("));
        assertTrue(cycles.contains("public boolean samplingStarted("));
        assertTrue(cycles.contains(">=60000"));
        assertTrue(cycles.contains("elapsedRealtime()-c.getLong(8)<60000L"));
        assertTrue(cycles.contains("FieldSessionApprovalPolicy.completedSuccessfully("));
        assertTrue(cycles.contains("MessageDigest.getInstance(\"SHA-256\")"));
        assertTrue(cycles.contains("public boolean approve("));
        assertTrue(field.contains("AND measurement_session_id IS NULL AND state IN ('ARMED','WARMUP','SAMPLING')"));
        assertTrue(field.contains("ContentValues owned=new ContentValues()"));
        assertTrue(field.contains("if(complete<1)return false;"));
        assertTrue(field.contains("LEGACY_IMPORTED"));
        assertTrue(field.contains("FieldCycleRepository.APPROVED.equals(cycleState)"));
    }

    @Test public void instantSessionsRemainIndependentAndRetryIsTransactional()throws Exception{
        String policy=read("measurement/TankAcquisitionPolicy.java");
        String cycles=read("field/FieldCycleRepository.java");
        String recorder=read("measurement/TelemetryRecorder.java");
        assertTrue(policy.contains("Mode { INSTANT, DOCUMENTED }"));
        assertTrue(policy.contains("mode == Mode.DOCUMENTED && state == State.COMPLETED"));
        assertTrue(cycles.contains("Only review/failed/cancelled attempts may be retried"));
        assertTrue(cycles.contains("w.beginTransaction()"));
        assertTrue(cycles.contains("replacement.put(\"supersedes_cycle_id\",old.id)"));
        assertTrue(cycles.contains("w.setTransactionSuccessful()"));
        assertTrue(recorder.contains("fieldSessions.linkMeasurement(active.id,sessionId)"));
        assertFalse(cycles.contains("developer_chemcore"));
    }
}
