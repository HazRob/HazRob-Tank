package com.servoc.app.chemcore;

import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

/** Source contracts; these tests are not evidence of real-gas ppm accuracy. */
public final class TankV5NoResearchSourceSuppressionContractTest {
    private static String s(String path)throws Exception{
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/"+path)),
            StandardCharsets.UTF_8);
    }
    @Test public void allSourcedManufacturerAndResearchCurvesRemainAvailable()throws Exception{
        String ref=s("chemcore/AcceptedReferenceResponseModel.java");
        assertTrue(ref.contains("tank-v5-all-source-ppm/3"));
        assertFalse(ref.contains("if(!MqConditioningProfile.acquisitionReady(sensor))return false"));
        assertFalse(ref.contains("if(!MqConditioningProfile.initialPreheatSatisfied(sensor))return unavailable"));
        assertTrue(ref.contains("LiteratureCurveRegistry.CURVES"));
        assertTrue(ref.contains("LinearLiteratureResponseRegistry.CURVES"));
        assertTrue(ref.contains("ManufacturerEvidenceMatrix.get"));
        assertTrue(ref.contains("ResearchCurveRegistry.CURVES"));
    }
    @Test public void overlappingPriorsAreRetainedWithoutDoubleCountingMqChannels()throws Exception{
        String loader=s("chemcore/TankStableScreeningModelRegistry.java");
        assertFalse(loader.contains("if(overlap){rejected++;continue;}"));
        assertTrue(loader.contains("new TankMultiSourceResponseModel(group.getKey(),sources)"));
        assertTrue(loader.contains("minimum=1"));
        String composite=s("chemcore/TankMultiSourceResponseModel.java");
        assertTrue(composite.contains("sources.size()"));
        assertTrue(composite.contains("public Estimate predict("));
        assertTrue(composite.contains("sources(){return sources;}"));
    }
    @Test public void allOperationalPathsUseSameStableModelRegistry()throws Exception{
        assertTrue(s("chemcore/TankInstantAnalyzer.java").contains("TankStableRuntimeCatalog.responseModels(androidContext,db)"));
        assertTrue(s("chemcore/CompletedSessionChemCoreAnalyzer.java").contains("TankStableRuntimeCatalog.responseModels(appContext,db)"));
        assertTrue(s("ui/ChemCoreAnalysisTab.java").contains("TankStableRuntimeCatalog.responseModels(context,host.database())"));
        assertTrue(s("measurement/TelemetryRecorder.java").contains("analyzeAndPersist(appContext,db,completed,snapshot,duration)"));
        assertTrue(s("chemical/TankChemicalCapabilityReport.java").contains("TankStableRuntimeCatalog.responseModels(context.getApplicationContext(),db)"));
    }
    @Test public void researchFingerprintIsComputedWithoutIdentityApproval()throws Exception{
        String calc=s("chemcore/TankFingerprintScreening.java");
        assertTrue(calc.contains("AcceptedFingerprintIdentityEngine.evaluate(ratios)"));
        assertTrue(calc.contains("calibrated_identity"));
        assertTrue(s("chemcore/TankInstantAnalyzer.java").contains("TankFingerprintScreening.evaluate(ratios)"));
        assertTrue(s("chemcore/CompletedSessionChemCoreAnalyzer.java").contains("TankFingerprintScreening.evaluate(ratios)"));
        assertTrue(s("ui/ChemCoreAnalysisTab.java").contains("TankFingerprintScreening.evaluate(steadyRatios(x.features))"));
    }
}
