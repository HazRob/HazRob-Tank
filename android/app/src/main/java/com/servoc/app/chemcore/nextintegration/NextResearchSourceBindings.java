package com.servoc.app.chemcore.nextintegration;

import com.hazrob.tank.chemcore.next.ChemCoreNext;
import com.hazrob.tank.chemcore.next.SourceCurveExpert;
import com.hazrob.tank.chemcore.next.MQSensorsLibExampleCatalog;
import com.hazrob.tank.chemcore.v6.SourceCatalog;
import com.servoc.app.chemical.ChemicalRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * F03: connect the installed 43+alternative source equations to ChemCore Next
 * for honest per-source eligibility/abstention diagnostics. Their original
 * experimental R0, heater phase and source-applicability gates have NOT been
 * proven equivalent to this Tank's clean-air R_air.
 *
 * Therefore EVERY bound curve has Qualification.unverified(); this bridge
 * MUST NOT be used to confer source ppm, a positive gas identity, alarms or
 * G3 qualification. Research-only numerical results remain in the separate
 * NextScientificDiagnostics ledger with their denominator assumption shown.
 *
 * No new chemical table: canonical UIDs are resolved from the single V14
 * unified chemical catalog using the source's historical legacyId flag.
 */
public final class NextResearchSourceBindings {
    private NextResearchSourceBindings() {}

    public static List<ChemCoreNext.CurveBinding> abstentionOnlyCurves(
            ChemicalRepository master,String actualDeviceId) {
        if(master==null||actualDeviceId==null||actualDeviceId.isBlank())
            throw new IllegalArgumentException("unified catalog and real device_id required");
        Map<Integer,Long> canonicalByLegacy=new HashMap<>();
        for(ChemicalRepository.Chemical row:master.all(5000))
            if(row.legacyId!=null)canonicalByLegacy.put(row.legacyId,row.uid);
        List<ChemCoreNext.CurveBinding> bindings=new ArrayList<>();
        for(SourceCatalog.Curve original:SourceCatalog.allCurves()){
            if(!original.numericalDefinitionValid()) {
                // Invalid equations are still listed as NOT_EVALUABLE in the
                // independent scientific ledger; never substitute coefficients.
                continue;
            }
            String model=original.sensorType.toUpperCase(Locale.ROOT);
            Long canonical=original.legacyChemicalId>=Integer.MIN_VALUE&&
                original.legacyChemicalId<=Integer.MAX_VALUE?
                canonicalByLegacy.get((int)original.legacyChemicalId):null;
            SourceCurveExpert.Curve source=new SourceCurveExpert.Curve(
                original.id(),original.sourceRoot(),model,original.label,canonical,
                SourceCurveExpert.Equation.RATIO_EQUALS_A_TIMES_PPM_OVER_SCALE_POWER_B,
                original.a,original.b,original.outputScale,
                original.minSourcePpm,original.maxSourcePpm,"");
            bindings.add(new ChemCoreNext.CurveBinding(
                source,actualDeviceId+":"+model,SourceCurveExpert.Qualification.unverified()));
        }
        // The other 13 installed-model MQSensorsLib example formulas have
        // a DISTINCT mathematical equation and source example K. Do not
        // merge their denominator into R_air or infer the chemical by label.
        // These bindings expose explicit abstention inside Next; the separate
        // dual-reference numerical ledger computes conditional examples.
        // The 14th original formula (MQ6) is retained in the source register,
        // but is NOT a physical sensor in this Tank device profile.
        for(MQSensorsLibExampleCatalog.Model example:
                MQSensorsLibExampleCatalog.installedModels()){
            SourceCurveExpert.Curve curve=new SourceCurveExpert.Curve(
                example.id,example.sourceRoot,example.model,example.label,null,
                SourceCurveExpert.Equation.PPM_EQUALS_A_TIMES_RATIO_POWER_B,
                example.a,example.b,null,null,null,"");
            bindings.add(new ChemCoreNext.CurveBinding(curve,
                actualDeviceId+":"+example.model,
                SourceCurveExpert.Qualification.unverified()));
        }
        return List.copyOf(bindings);
    }
}
