package com.servoc.app.report;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

/** Source contract: SAF system picker cannot silently switch to a newer telemetry sample. */
public final class TankPhysicalSnapshotLifecycleContractTest {
    @Test public void snapshotIsFrozenAndRestoredWhenAndroidRecreatesActivity()throws Exception{
        String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/MainActivity.java")),StandardCharsets.UTF_8);
        assertTrue(s.contains("pendingPhysicalSnapshot=snap.toString()"));
        assertTrue(s.contains("pendingPhysicalUtc=isoNow()"));
        assertTrue(s.contains("pendingPhysicalDocumentRequest=true"));
        assertTrue(s.contains("pendingPhysicalDocumentRequest=b.getBoolean"));
        assertTrue(s.contains("out.putString(\"tank_v3_physical_export_snapshot\""));
        assertTrue(s.contains("final String frozen=pendingPhysicalSnapshot,stamp=pendingPhysicalUtc;"));
        assertTrue(s.contains("TankPhysicalSnapshotPdf.write(out,new JSONObject(frozen),stamp)"));
        assertTrue(s.contains("if(pendingPhysicalDocumentRequest){showToast("));
        assertTrue(s.contains("if(requestCode==EXPORT_PHYSICAL_PDF&&(resultCode!=RESULT_OK"));
        assertTrue(s.contains("pendingPhysicalDocumentRequest=false"));
    }
}
