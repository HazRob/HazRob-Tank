package com.servoc.app.chemcore.library;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Static policy regression; HTTPS and download/rollback network tests still pending. */
public final class TankV5GithubManifestPolicyContractTest {
    @Test public void manifestSourceMustBeExactHazRobGitHubJson()throws Exception{
        String s=new String(Files.readAllBytes(Paths.get(
            "src/main/java/com/servoc/app/chemcore/library/ChemCoreLibraryManager.java")),
            StandardCharsets.UTF_8);
        assertTrue(s.contains("validateGitHubManifestUrl(u)"));
        assertTrue(s.contains("validateGitHubManifestUrl(source)"));
        assertTrue(s.contains("\"raw.githubusercontent.com\".equals(host)"));
        assertTrue(s.contains("\"api.github.com\".equals(host)"));
        assertTrue(s.contains("\"github.com\".equals(host)"));
        assertTrue(s.contains("path.startsWith(\"/HazRob/\")"));
        assertTrue(s.contains("path.startsWith(\"/repos/HazRob/\")"));
        assertTrue(s.contains("path.contains(\"/../\")"));
        assertTrue(s.contains("!path.toLowerCase(java.util.Locale.US).endsWith(\".json\")"));
        assertTrue(s.contains("u.getUserInfo()!=null||u.getPort()!=-1||u.getRef()!=null"));
        assertTrue(s.contains("ChemCoreLibraryVerifier.verifyManifestOnly("));
        assertTrue(s.contains("store.installVerifiedSnapshot("));
        assertTrue(s.contains("MeasurementSessionRegistry.fieldOperationActive()"));
    }
}
