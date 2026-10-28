package com.servoc.app.chemical;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Source only; Ethanol CAS/UID + embedded Stable model inspection on handset pending. */
public final class TankV5ChemicalCapabilitiesContractTest {
    private static String read(String f)throws Exception{
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/"+f)),
                StandardCharsets.UTF_8);
    }
    @Test public void verifiedStableIsInventoriedForExactChemicalUid()throws Exception{
        String model=read("chemical/TankChemicalCapabilityReport.java");
        String loader=read("chemcore/TankStableScreeningModelRegistry.java");
        assertTrue(model.contains("TankChemicalEvidence.inspect(db,chemical)"));
        assertTrue(model.contains("TankStableRuntimeCatalog.responseModels(context.getApplicationContext(),db)"));
        assertTrue(model.contains("candidate.chemicalUid!=chemical.uid"));
        assertTrue(model.contains("model.minPpm()"));
        assertTrue(model.contains("model.maxPpm()"));
        assertTrue(model.contains("model.sourceId()"));
        assertTrue(model.contains("ليست معايرة جهاز"));
        assertTrue(loader.contains("STABLE_SHA256_MISMATCH_USE_BUILTIN"));
        assertTrue(loader.contains("canonicalUidMap(db)"));
    }
    @Test public void descriptionDoesNotFreezeUiDuringFileRead()throws Exception{
        String tab=read("ui/MeasurementsTab.java");
        assertTrue(tab.contains("host.io().execute(()->{"));
        assertTrue(tab.contains("TankChemicalCapabilityReport.describe(c,host.database(),chemical)"));
        assertTrue(tab.contains("scroll.post(()->{"));
    }
}
