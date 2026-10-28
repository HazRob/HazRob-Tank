package com.servoc.app.chemcore;

import android.content.Context;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.chemcore.library.ChemCoreLibraryStore;
import com.servoc.app.chemcore.library.ChemCoreStableKnowledgeReader;
import java.util.List;

/**
 * One source of truth for all three Tank inference paths: instant, documented
 * field cycles and controlled ChemCore laboratory sessions.
 *
 * Activates all ACTUALLY SUPPORTED, verified Stable response priors plus the
 * existing source-backed curves and PubChem/QSPR supplements. Other Stable
 * assets (molecular descriptors, curation, feature/open-set artifacts) retain
 * their own scientifically appropriate readers; an identity is not a curve.
 *
 * In particular, this never turns a literature prior into an M4 calibration
 * or calls a fingerprint a calibrated ppm sensor.
 */
public final class TankStableRuntimeCatalog {
    private TankStableRuntimeCatalog(){}

    public static TankStableScreeningModelRegistry.LoadResult responseModels(
            Context context,ExperimentsDb db){
        List<InverseMolecularSolver.Candidate> direct=
            ChemCoreLegacyCandidateRegistry.acceptedReferenceCandidates();
        return TankStableScreeningModelRegistry.load(context,db,direct);
    }

    /** Validated open-set assets are independent of M1/M2 concentration priors. */
    public static ChemCoreStableKnowledgeReader.OpenSetArtifact validatedOpenSet(
            Context context,SamplingProfile profile){
        if(context==null||profile==null)return null;
        ChemCoreLibraryStore store=new ChemCoreLibraryStore(context.getApplicationContext());
        // No model artifact may be used without an active verified library pointer.
        if(store.activePayload()==null||store.activeVersion().isEmpty()||
           store.activeSha256().length()!=64)return null;
        // The shared response loader independently checks SHA-256 before using
        // a prior. Open-set reader performs its own validation and feature gates.
        return ChemCoreStableKnowledgeReader.openSetArtifact(
            store.activePayload(),profile.profileId);
    }
}
