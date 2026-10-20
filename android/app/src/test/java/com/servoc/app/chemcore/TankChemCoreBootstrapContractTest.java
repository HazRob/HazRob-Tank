package com.servoc.app.chemcore;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;
import static org.junit.Assert.*;

/** Prevent embedded Stable being merely packaged without activating its supported knowledge path. */
public final class TankChemCoreBootstrapContractTest {
    private static String text(String path)throws Exception{
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/"+path)),StandardCharsets.UTF_8);
    }
    @Test public void installedStableAndMolecularQsprRuntimeAreActivatedOnStartup()throws Exception{
        String m=text("MainActivity.java");
        assertTrue(m.contains("ChemCoreEmbeddedStableInstaller.ensureInstalled"));
        assertTrue(m.contains("ChemCoreMolecularSeedImporter.importBestAvailable(getApplicationContext(),db)"));
        assertTrue(m.contains("ChemCoreQsprRuntimeRegistry.rebuild(db)"));
        assertTrue(m.indexOf("ChemicalSeedBootstrap.ensureNiosh")<m.indexOf("ChemCoreMolecularSeedImporter.importBestAvailable"));
        assertTrue(m.contains("tank_v3_chemcore_bootstrap"));
    }
    @Test public void statusShowsRealImportAndModelCountNotOnlyAssetName()throws Exception{
        String status=text("ui/StatusTab.java");
        assertTrue(status.contains("ChemCoreQsprRuntimeRegistry.status()"));
        assertTrue(status.contains("tank_v3_chemcore_bootstrap"));
        assertTrue(status.contains("qspr.enabledSensorModels"));
        assertTrue(status.contains("qspr.liveCandidates"));
    }
    @Test public void installingNewKnowledgeUpdatesLiveQsprInBothOperatorPaths()throws Exception{
        String status=text("ui/StatusTab.java"),settings=text("ui/SettingsTab.java");
        assertTrue(status.contains("ChemCoreMolecularSeedImporter.importVerifiedPayload"));
        assertTrue(status.contains("ChemCoreQsprRuntimeRegistry.rebuild(host.database())"));
        assertTrue(settings.contains("ChemCoreMolecularSeedImporter.importBestAvailable"));
        assertTrue(settings.contains("ChemCoreQsprRuntimeRegistry.rebuild(host.database())"));
    }

}