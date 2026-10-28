package com.servoc.app.ui;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

public final class TankV5StableStatusContractTest {
    private static String src(String f)throws Exception{
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/"+f)),
                StandardCharsets.UTF_8);
    }
    @Test public void userSeesRealVersionShaAndSourceWithUpdateFaults()throws Exception{
        String s=src("ui/StatusTab.java");
        assertTrue(s.contains("libraryUpdater.check(ReleaseAudit.VERSION)"));
        assertTrue(s.contains("libraryUpdater.install(checked,ReleaseAudit.VERSION"));
        assertTrue(s.contains("private void renderUnifiedUpdateState("));
        assertTrue(s.contains("lib.version"));
        assertTrue(s.contains("lib.sha256"));
        assertTrue(s.contains("lib.sourceLabel"));
        assertTrue(s.contains("libraryUpdater.manifestUrl()"));
        assertTrue(s.contains("tank_v5_library_last_check_status"));
        assertTrue(s.contains("FAILED: "));
    }
}
