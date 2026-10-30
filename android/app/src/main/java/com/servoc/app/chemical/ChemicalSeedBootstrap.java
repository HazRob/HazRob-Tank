package com.servoc.app.chemical;

import android.content.Context;

import com.servoc.app.chemcore.ChemCoreIdentityCurationRepository;
import com.servoc.app.chemcore.ChemCoreMolecularSeedImporter;
import com.servoc.app.chemcore.ChemCoreQsprRuntimeRegistry;
import com.servoc.app.chemcore.library.ChemCoreEmbeddedStableInstaller;
import com.servoc.app.chemcore.library.ChemCoreLibraryStore;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.data.ChemicalHazardEvidenceSeedV13;
import com.servoc.app.data.MqExternalR0RsSeedV14;
import com.servoc.app.exposure.ExposureSeedBootstrap;
import com.servoc.app.exposure.GasFlammabilitySeedBootstrap;

import java.io.InputStream;

/** Loads versioned offline NIOSH scientific seeds, HR069 analytical research priors and the best available ChemCore molecular snapshot. */
public final class ChemicalSeedBootstrap {
    private ChemicalSeedBootstrap(){}

    public static ChemicalSeedImporter.Result ensureNiosh(Context context,ExperimentsDb db){
        final String asset="generated/niosh/niosh_chemicals.json";
        ChemicalSeedImporter.Result result=null;
        try{
            String hash;try(InputStream h=context.getAssets().open(asset)){hash=ChemicalSeedImporter.sha256(h);}
            if(ChemicalSeedImporter.needsImport(db.getReadableDatabase(),hash)){
                try(InputStream in=context.getAssets().open(asset)){result=ChemicalSeedImporter.importNioshIdentity(db.getWritableDatabase(),in);}
            }
        }catch(Exception e){db.putMeta("chemical_seed_niosh_last_error",e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage()));}

        // Explicitly type legacy labels for mixtures, chemical groups and
        // measurement indices: no fictitious CAS or formula per group.
        try{
            ChemicalIdentityCategoryV6.Result categories=
                ChemicalIdentityCategoryV6.ensure(db);
            db.putMeta("v6_non_single_substance_classification",categories.summary());
        }catch(Exception e){
            db.putMeta("v6_non_single_substance_classification",
                "error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));
        }

        // Establish traceable links between historical Tank labels and
        // the matching CAS-bearing NIOSH source identities. Keep both original
        // UIDs/FKs intact; a naming alias never overwrites old session rows.
        try{
            ChemicalIdentityEquivalenceV6.Result equivalent=
                ChemicalIdentityEquivalenceV6.ensure(db);
            db.putMeta("v6_chemical_niosh_alias_crosswalk",equivalent.summary());
        }catch(Exception e){
            db.putMeta("v6_chemical_niosh_alias_crosswalk",
                "error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));
        }

        ExposureSeedBootstrap.ensureNioshIdlh(context,db);

        // Tank V6 scientific risk inventory: load authoritative OSHA Z-1 occupational
        // limits alongside NIOSH IDLH (same CAS-based identity master). Preserve
        // source authority, original units, duration and historical provenance.
        ExposureSeedBootstrap.ensureOshaZ1(context,db);
        // Import ALL original NIOSH/OSHA identity-bearing rows FIRST.
        // Enriching an earlier legacy CAS before those seed lookups changes
        // which UID gets assigned to the final OSHA-linked chemical on a
        // pristine database, causing fresh-install vs upgrade divergence.
        // Only AFTER the base identities are frozen can NIST fill the
        // existing 27 stable rows without renumbering any historical UID.
        // NIST SRD69 independently identifies the remaining 27 historical
        // *single* chemical substances. Verify exact legacy UID, name,
        // formula, CAS checksum, source and non-conflicting identity before
        // filling ONLY missing CAS/molecular-weight data. All nine mixtures
        // and measurement indices remain correctly CAS-not-applicable.
        try{
            ChemicalNistIdentityCompletionV6.Result nist=
                ChemicalNistIdentityCompletionV6.ensure(db);
            db.putMeta("v6_chemical_nist_cas_completion",nist.summary());
        }catch(Exception e){
            db.putMeta("v6_chemical_nist_cas_completion",
                "error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));
        }

        // NIST SRD69 Oxygen 7782-44-7 and Methane 74-82-8: fill only
        // missing g/mol identity properties. Never treat as an ambient range.
        try{
            ChemicalNistEssentialGasMassV6.Result atmosphere=
                ChemicalNistEssentialGasMassV6.ensure(db);
            db.putMeta("v6_nist_essential_gas_molecular_weight",atmosphere.summary());
        }catch(Exception e){
            db.putMeta("v6_nist_essential_gas_molecular_weight",
                "error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));
        }

        // Source-checked correction of TWO malformed CAS tokens from the
        // imported scientific seed. Keep the invalid originals in an audit
        // table and never reintroduce them into identifier/search indices.
        try{
            ChemicalCasCorrectionV6.Result repairs=
                ChemicalCasCorrectionV6.ensure(db);
            db.putMeta("v6_invalid_cas_source_repairs",repairs.summary());
        }catch(Exception e){
            db.putMeta("v6_invalid_cas_source_repairs",
                "error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));
        }


        // Source qualified hazards must be linked AFTER authoritative identity
        // imports resolve CAS -> stable chemicals.uid, and again safely on
        // subsequent starts; otherwise benzene's legacy CAS may be blank at
        // database creation time and its OSHA/IARC classifications lost.
        try{ChemicalHazardEvidenceSeedV13.ensure(db.getWritableDatabase());}
        catch(Exception ex){db.putMeta("chemical_v13_hazard_seed_error",
            ex.getClass().getSimpleName()+": "+String.valueOf(ex.getMessage()));}

        GasFlammabilitySeedBootstrap.ensure(context,db);
        // Scientific publications report R0/Rs for THEIR sensor instances:
        // these are not Tank calibration, not chemical properties, and not G3.
        try{
            int peerRows=MqExternalR0RsSeedV14.ensure(context,db);
            db.putMeta("mq_peer_r0_rs_seed_status",
                "source_qualified_research_only:new_rows="+peerRows);
        }catch(Exception e){
            db.putMeta("mq_peer_r0_rs_seed_error",
                e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));
        }


        try{AnalyticalResearchBootstrap.ensure(db.getWritableDatabase());db.putMeta("analytical_research_seed_status","available:"+AnalyticalResearchBootstrap.SEED_VERSION);}catch(Exception e){db.putMeta("analytical_research_seed_status","error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));}
        try{AnalyticalTransferBootstrap.ensure(db.getWritableDatabase());db.putMeta("analytical_transfer_seed_status","available:"+AnalyticalTransferBootstrap.SEED_VERSION);}catch(Exception e){db.putMeta("analytical_transfer_seed_status","error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));}
        try{AnalyticalPrioritySourceBootstrap.ensure(db.getWritableDatabase());db.putMeta("analytical_priority_sources_seed_status","available:"+AnalyticalPrioritySourceBootstrap.SEED_VERSION);}catch(Exception e){db.putMeta("analytical_priority_sources_seed_status","error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));}
        try{MqUsefulEvidenceBootstrap.ensure(db.getWritableDatabase());db.putMeta("mq_useful_evidence_seed_status","available:"+MqUsefulEvidenceBootstrap.SEED_VERSION);}catch(Exception e){db.putMeta("mq_useful_evidence_seed_status","error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));}
        try{MqSingleSensorEvidenceBootstrap.ensure(db.getWritableDatabase());db.putMeta("mq_single_sensor_evidence_seed_status","available:"+MqSingleSensorEvidenceBootstrap.SEED_VERSION);}catch(Exception e){db.putMeta("mq_single_sensor_evidence_seed_status","error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));}
        try{MqCalibrationSeriesBootstrap.ensure(db.getWritableDatabase());db.putMeta("mq_calibration_series_seed_status","available:"+MqCalibrationSeriesBootstrap.SEED_VERSION);}catch(Exception e){db.putMeta("mq_calibration_series_seed_status","error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));}
        try{MqVocFingerprintEvidenceBootstrap.ensure(db.getWritableDatabase());db.putMeta("mq_voc_fingerprint_evidence_seed_status","available:"+MqVocFingerprintEvidenceBootstrap.SEED_VERSION);}catch(Exception e){db.putMeta("mq_voc_fingerprint_evidence_seed_status","error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));}
        try{MqLargeVocEvidenceBootstrap.ensure(db.getWritableDatabase());db.putMeta("mq_large_voc_evidence_seed_status","available:"+MqLargeVocEvidenceBootstrap.SEED_VERSION);}catch(Exception e){db.putMeta("mq_large_voc_evidence_seed_status","error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));}
        try{MqReactiveEvidenceBootstrap.ensure(db.getWritableDatabase());db.putMeta("mq_reactive_evidence_seed_status","available:"+MqReactiveEvidenceBootstrap.SEED_VERSION);}catch(Exception e){db.putMeta("mq_reactive_evidence_seed_status","error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));}
        try{OperationalHazardTransferBootstrap.ensure(db.getWritableDatabase());db.putMeta("operational_hazard_transfer_seed_status","available:"+OperationalHazardTransferBootstrap.SEED_VERSION);}catch(Exception e){db.putMeta("operational_hazard_transfer_seed_status","error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));}

        // HR069-R1: initialize the genuine embedded Stable-channel snapshot before
        // molecular/QSPR imports. This does not write developer reference data or
        // override an installed remote Stable; failures retain existing APK fallback.
        ChemCoreLibraryStore.InstallResult embedded=ChemCoreEmbeddedStableInstaller.ensureInstalled(context);
        db.putMeta("chemcore_embedded_stable_bootstrap",
                embedded.installed?"installed:"+embedded.version:"unchanged_or_failed:"+embedded.reason+":"+embedded.version);

        ChemCoreMolecularSeedImporter.Result molecular=ChemCoreMolecularSeedImporter.importBestAvailable(context,db);
        db.putMeta("chemcore_molecular_seed_status",molecular.available?("available:"+molecular.records+":"+molecular.matched+":"+molecular.origin):molecular.message);
        ChemCoreIdentityCurationRepository.ImportResult curation=ChemCoreIdentityCurationRepository.importIfPresent(context,db);
        db.putMeta("chemcore_identity_curation_status",curation.available?("available:"+curation.records+":"+curation.sha256):curation.message);

        // Fill only missing identity fields from previously imported, CAS-matched
        // and source-retained PubChem A_SOURCE observations. This cannot create
        // chemical R0/Rs, a gas identity, an exposure alarm, or a new master UID.
        try{
            ChemicalMolecularProjectionV6.Result projection=
                ChemicalMolecularProjectionV6.ensure(db);
            db.putMeta("v6_chemical_molecular_projection",projection.summary());
        }catch(Exception e){
            db.putMeta("v6_chemical_molecular_projection",
                "error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));
        }

        // NIOSH-verified historical aliases may share the same molecule as
        // a PubChem A_SOURCE canonical UID. Fill only the absent mass, after
        // canonical properties exist. Never change historical identities.
        try{
            ChemicalAliasMolecularProjectionV6.Result aliasMass=
                ChemicalAliasMolecularProjectionV6.ensure(db);
            db.putMeta("v6_niosh_alias_molecular_mass",aliasMass.summary());
        }catch(Exception e){
            db.putMeta("v6_niosh_alias_molecular_mass",
                "error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));
        }

        // PubChem/QSPR is rebuilt only after the molecular seed and the negative curation layer
        // are installed. It remains an M1 ranking/screening prior and cannot create M3/M4 alone.
        try{
            ChemCoreQsprRuntimeRegistry.Status qspr=ChemCoreQsprRuntimeRegistry.rebuild(db);
            db.putMeta("chemcore_qspr_runtime_status",qspr.summary());
        }catch(Exception e){
            db.putMeta("chemcore_qspr_runtime_status","error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));
        }

        // Priority is a view/subset over the same master chemical records. Run it only after all
        // MQ evidence and molecular/identity enrichment have been loaded so newly supported
        // master-library compounds are promoted automatically without duplicating chemical facts.
        try{ChemicalPriorityBootstrap.ensure(db.getWritableDatabase());db.putMeta("chemical_priority_seed_status","available:"+ChemicalPriorityBootstrap.VERSION);}catch(Exception e){db.putMeta("chemical_priority_seed_status","error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));}
        return result;
    }
}
