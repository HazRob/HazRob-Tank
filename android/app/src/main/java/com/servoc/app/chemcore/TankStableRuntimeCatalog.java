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
        java.io.File file=store.activePayload();
        if(file==null||store.activeVersion().isEmpty()||
           store.activeSha256().length()!=64||file.length()<1||file.length()>16*1024*1024)
            return null;
        try(java.io.FileInputStream in=new java.io.FileInputStream(file)){
            java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");
            byte[] buf=new byte[8192];int n;
            while((n=in.read(buf))!=-1)if(n>0)digest.update(buf,0,n);
            StringBuilder hash=new StringBuilder();
            for(byte v:digest.digest())hash.append(String.format(java.util.Locale.US,"%02x",v&255));
            if(!store.activeSha256().equalsIgnoreCase(hash.toString()))return null;
        }catch(Exception ex){return null;}
        // Open-set digest is checked above independently of response priors.
        // The reader further validates feature dimensions, thresholds and protocols.
        return ChemCoreStableKnowledgeReader.openSetArtifact(
            store.activePayload(),profile.profileId);
    }
}
