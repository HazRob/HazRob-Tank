package com.servoc.app.chemical;

import android.database.Cursor;

import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.reference.ReferenceArtifactBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Read-only material readiness projection used by the developer experiment matrix.
 *
 * The local database is the activated snapshot of the central HazRob scientific library plus
 * device-local approved experiments. Device-local R0 is deliberately excluded: R0 is sensor/device
 * calibration state and must never be promoted to a material-library status.
 *
 * READY quantitative status is deliberately stricter than "some ppm evidence": only an active
 * curve explicitly qualified as HR069_M4 may produce READY/M4. External or transferred
 * concentration evidence remains PARTIAL.
 */
public final class ChemicalExperimentStatusRepository {
    public enum Level { NONE, PARTIAL, READY }

    public static final class Row {
        public final ChemicalRepository.Chemical chemical;
        public final Level fingerprint, ppm;
        public final boolean m4, tested, referenceCurve, referenceFingerprint,
                referencePpm, referenceM4, publishedM4Qualified,
                publishedFingerprintQualified, publishedCurveQualified, publishedPpmQualified;
        public final String evidenceRevision;

        Row(ChemicalRepository.Chemical chemical, Level fingerprint, Level ppm,
            boolean m4, boolean tested, String evidenceRevision,
            boolean curveRef, boolean fingerprintRef, boolean ppmRef, boolean m4Ref,
            boolean m4Qualified, boolean fpQualified, boolean curveQualified,
            boolean ppmQualified) {
            this.chemical=chemical;this.fingerprint=fingerprint;this.ppm=ppm;
            this.m4=m4;this.tested=tested;
            this.referenceCurve=curveRef;this.referenceFingerprint=fingerprintRef;
            this.referencePpm=ppmRef;this.referenceM4=m4Ref;this.publishedM4Qualified=m4Qualified;
            this.publishedFingerprintQualified=fpQualified;
            this.publishedCurveQualified=curveQualified;
            this.publishedPpmQualified=ppmQualified;
            this.evidenceRevision=evidenceRevision==null?"":evidenceRevision;
        }

        public String fingerprintMark(){
            return fingerprint==Level.READY?"✓ جهاز":
                publishedFingerprintQualified?"✓ مكتبة":
                referenceFingerprint?"◐ مكتبة":mark(fingerprint);
        }
        public String ppmMark(){
            return ppm==Level.READY?"✓ جهاز":
                publishedPpmQualified?"✓ مكتبة":
                referencePpm?"◐ مكتبة":mark(ppm);
        }
        public String m4Mark(){return m4?"✓ جهاز":publishedM4Qualified?"✓ مكتبة":
                referenceM4?"◐ مكتبة":"—";}
        public String curveMark(){return ppm==Level.READY?"✓ منحنى":
                publishedCurveQualified?"✓ مكتبة":
                referenceCurve?"◐ مكتبة":"—";}
        public String capabilityLine(){
            return "بصمة "+fingerprintMark()+" • منحنى "+curveMark()+
                " • PPM "+ppmMark()+" • M4 "+m4Mark();
        }
        public int priority(){
            if(fingerprint==Level.NONE)return 0;
            if(ppm==Level.NONE)return 1;
            if(ppm==Level.PARTIAL&&!m4)return 2;
            if(!m4)return 3;
            return 4;
        }
        private static String mark(Level x){return x==Level.READY?"✓":x==Level.PARTIAL?"◐":"—";}
    }

    private final ExperimentsDb db;
    private final ChemicalRepository chemicals;

    public ChemicalExperimentStatusRepository(ExperimentsDb db){
        if(db==null)throw new IllegalArgumentException("database required");
        this.db=db;this.chemicals=new ChemicalRepository(db);
        ChemicalReferenceSyncV6.ensure(db.getWritableDatabase());
    }

    public List<Row> search(String query,int limit){
        List<ChemicalRepository.Chemical> source=chemicals.search(query,Math.max(1,Math.min(limit,5000)));
        List<Row> out=new ArrayList<>(source.size());
        String revision=libraryRevision();
        for(ChemicalRepository.Chemical c:source)out.add(status(c,revision));
        return out;
    }

    public Row status(ChemicalRepository.Chemical c){return status(c,libraryRevision());}

    private Row status(ChemicalRepository.Chemical c,String revision){
        if(c==null)return null;

        boolean localFingerprint=exists("SELECT 1 FROM reference_artifacts WHERE chemical_uid=? AND kind='fingerprint' AND active=1 LIMIT 1",c.uid);
        boolean acceptedCentralFingerprint=exists("SELECT 1 FROM sensor_evidence WHERE chemical_uid=? AND status NOT LIKE 'REJECT%' AND (lower(evidence_type) LIKE '%fingerprint%' OR lower(evidence_type) LIKE '%identity%') AND (upper(status) LIKE 'ACCEPTED_IDENTITY_FINGERPRINT%' OR upper(status) LIKE 'VERIFIED%FINGERPRINT%') LIMIT 1",c.uid);
        boolean transferFingerprint=exists("SELECT 1 FROM sensor_evidence WHERE chemical_uid=? AND status NOT LIKE 'REJECT%' AND (lower(evidence_type) LIKE '%fingerprint%' OR lower(evidence_type) LIKE '%identity%') LIMIT 1",c.uid);
        Level fingerprint=(localFingerprint||acceptedCentralFingerprint)?Level.READY:transferFingerprint?Level.PARTIAL:Level.NONE;

        // GitHub record synchronization reports scientific evidence AVAILABILITY.
        // These flags are never certified as active on-device MQ calibration.
        boolean referenceFp=false,referenceCurve=false,referencePpm=false,
            referenceM4=false,publishedM4Qualified=false,
            fpQualified=false,curveQualified=false,ppmQualified=false;
        // One indexed query per material instead of eight queries for each
        // row of a 734-item Android list. Mirror published central flags;
        // never run scientific approval logic on the phone.
        try(Cursor q=db.getReadableDatabase().rawQuery(
                "SELECT property_key,evidence_status FROM chemical_reference_updates_v6 "+
                "WHERE chemical_uid=?",new String[]{Long.toString(c.uid)})){
            while(q.moveToNext()){
                String key=q.getString(0),status=q.getString(1);
                boolean qualified="PUBLISHED_LIBRARY_QUALIFIED".equals(status);
                boolean available=qualified||
                    "PUBLISHED_LIBRARY_AVAILABLE".equals(status);
                if(!available)continue;
                if("fingerprint_reference".equals(key)){
                    referenceFp=true;fpQualified=qualified;
                }else if("mq_curve_reference".equals(key)){
                    referenceCurve=true;curveQualified=qualified;
                }else if("ppm_reference".equals(key)){
                    referencePpm=true;ppmQualified=qualified;
                }else if("m4_reference".equals(key)){
                    referenceM4=true;publishedM4Qualified=qualified;
                }
            }
        }catch(Exception ignored){/* offline old DB displays its bundled statuses */}

        if(fingerprint==Level.NONE&&referenceFp)fingerprint=Level.PARTIAL;

        boolean anyActiveCurve=false,strictM4=false;
        if(c.legacyId!=null){
            anyActiveCurve=exists("SELECT 1 FROM calibration_curves WHERE chemical_id=? AND active=1 LIMIT 1",c.legacyId.longValue());
            strictM4=exists("SELECT 1 FROM calibration_curves WHERE chemical_id=? AND active=1 AND source_note LIKE '"+ReferenceArtifactBuilder.HR069_M4_SOURCE_PREFIX+"%' LIMIT 1",c.legacyId.longValue());
        }
        boolean transferPpm=exists("SELECT 1 FROM sensor_evidence WHERE chemical_uid=? AND status NOT LIKE 'REJECT%' AND ((min_ppm IS NOT NULL AND min_ppm>=0) OR (max_ppm IS NOT NULL AND max_ppm>0)) LIMIT 1",c.uid);
        Level ppm=strictM4?Level.READY:
            (anyActiveCurve||transferPpm||referenceCurve||referencePpm)?Level.PARTIAL:Level.NONE;

        boolean centralEvidence=exists("SELECT 1 FROM sensor_evidence WHERE chemical_uid=? AND status NOT LIKE 'REJECT%' LIMIT 1",c.uid);
        boolean tested=centralEvidence
                ||exists("SELECT 1 FROM experiments WHERE chemical_uid=? LIMIT 1",c.uid)
                ||exists("SELECT 1 FROM reference_artifacts WHERE chemical_uid=? LIMIT 1",c.uid);
        return new Row(c,fingerprint,ppm,strictM4,
            tested||referenceFp||referenceCurve||referencePpm||referenceM4,
            revision,referenceCurve,referenceFp,referencePpm,referenceM4,
            publishedM4Qualified,fpQualified,curveQualified,ppmQualified);
    }

    /** Revision shown beside the matrix so the developer can verify which active central snapshot drives status. */
    public String libraryRevision(){
        String version=db.getMeta("chemcore_library_version","");
        String evidence=db.getMeta("chemcore_evidence_revision","");
        String r=(version==null?"":version.trim());
        String e=(evidence==null?"":evidence.trim());
        if(!r.isEmpty()&&!e.isEmpty())r=r+" / "+e;
        else if(r.isEmpty())r=e;
        String remoteRevision=db.getMeta("v6_reference_query_revision","");
        if(remoteRevision!=null&&!remoteRevision.trim().isEmpty())
            r+=(r.isEmpty()?"":" / ")+"QUERY "+remoteRevision.trim();
        if(r.isEmpty())r=db.getMeta("chemcore_molecular_seed_sha256","");
        if(r==null)r="";r=r.trim();return r.length()<=28?r:r.substring(0,28)+"…";
    }

    /** Any usable identity/fingerprint coverage from the active central snapshot or approved local artifact. */
    public int countFingerprintMaterials(){return scalar("SELECT COUNT(DISTINCT uid) FROM chemicals WHERE uid IN (SELECT chemical_uid FROM reference_artifacts WHERE kind='fingerprint' AND active=1) OR uid IN (SELECT chemical_uid FROM sensor_evidence WHERE chemical_uid IS NOT NULL AND status NOT LIKE 'REJECT%' AND (lower(evidence_type) LIKE '%fingerprint%' OR lower(evidence_type) LIKE '%identity%')) OR uid IN (SELECT chemical_uid FROM chemical_reference_updates_v6 WHERE property_key='fingerprint_reference' AND evidence_status LIKE 'PUBLISHED_LIBRARY_%')");}
    /** READY fingerprints only: approved local artifact or centrally accepted/verified identity fingerprint. */
    public int countReadyFingerprintMaterials(){return scalar("SELECT COUNT(DISTINCT uid) FROM chemicals WHERE uid IN (SELECT chemical_uid FROM reference_artifacts WHERE kind='fingerprint' AND active=1) OR uid IN (SELECT chemical_uid FROM sensor_evidence WHERE chemical_uid IS NOT NULL AND status NOT LIKE 'REJECT%' AND (lower(evidence_type) LIKE '%fingerprint%' OR lower(evidence_type) LIKE '%identity%') AND (upper(status) LIKE 'ACCEPTED_IDENTITY_FINGERPRINT%' OR upper(status) LIKE 'VERIFIED%FINGERPRINT%'))");}
    /** Any known concentration/curve evidence, including transfer evidence. */
    public int countPpmMaterials(){return scalar("SELECT COUNT(DISTINCT uid) FROM chemicals WHERE legacy_id IN (SELECT chemical_id FROM calibration_curves WHERE active=1) OR uid IN (SELECT chemical_uid FROM sensor_evidence WHERE chemical_uid IS NOT NULL AND status NOT LIKE 'REJECT%' AND ((min_ppm IS NOT NULL AND min_ppm>=0) OR (max_ppm IS NOT NULL AND max_ppm>0))) OR uid IN (SELECT chemical_uid FROM chemical_reference_updates_v6 WHERE property_key IN ('ppm_reference','mq_curve_reference') AND evidence_status LIKE 'PUBLISHED_LIBRARY_%')");}
    /** Remote reference claims are listed separately and cannot become approved M4. */
    public int countRemoteResearchM4Materials(){
        return scalar("SELECT COUNT(DISTINCT chemical_uid) FROM chemical_reference_updates_v6 "+
            "WHERE property_key='m4_reference' AND "+
            "evidence_status LIKE 'PUBLISHED_LIBRARY_%'");
    }
    public int countRemoteResearchCurveMaterials(){
        return scalar("SELECT COUNT(DISTINCT chemical_uid) FROM chemical_reference_updates_v6 "+
            "WHERE property_key='mq_curve_reference' AND "+
            "evidence_status LIKE 'PUBLISHED_LIBRARY_%'");
    }
    /** Strict reportable HR069 M4 material count. */
    public int countM4Materials(){return scalar("SELECT COUNT(DISTINCT chemical_id) FROM calibration_curves WHERE active=1 AND source_note LIKE '"+ReferenceArtifactBuilder.HR069_M4_SOURCE_PREFIX+"%'");}
    /** Union of local experiment activity and active central scientific evidence. */
    public int countTestedMaterials(){return scalar("SELECT COUNT(DISTINCT uid) FROM chemicals WHERE uid IN (SELECT chemical_uid FROM experiments WHERE chemical_uid IS NOT NULL AND chemical_uid>0) OR uid IN (SELECT chemical_uid FROM sensor_evidence WHERE chemical_uid IS NOT NULL AND status NOT LIKE 'REJECT%') OR uid IN (SELECT chemical_uid FROM chemical_reference_updates_v6 WHERE evidence_status LIKE 'PUBLISHED_LIBRARY_%')");}
    public int countLocalExperimentMaterials(){return scalar("SELECT COUNT(DISTINCT chemical_uid) FROM experiments WHERE chemical_uid IS NOT NULL AND chemical_uid>0");}
    public int countCentralEvidenceMaterials(){return scalar("SELECT COUNT(DISTINCT uid) FROM chemicals WHERE uid IN (SELECT chemical_uid FROM sensor_evidence WHERE chemical_uid IS NOT NULL AND status NOT LIKE 'REJECT%') OR uid IN (SELECT chemical_uid FROM chemical_reference_updates_v6 WHERE evidence_status LIKE 'PUBLISHED_LIBRARY_%')");}
    public int countExposureLimitMaterials(){return scalar("SELECT COUNT(DISTINCT chemical_uid) FROM exposure_limits WHERE chemical_uid IS NOT NULL");}
    public int countUntestedNoEvidenceMaterials(){int total=chemicals.count();return Math.max(0,total-countTestedMaterials());}

    private boolean exists(String sql,long value){
        try(Cursor c=db.getReadableDatabase().rawQuery(sql,new String[]{String.valueOf(value)})){return c.moveToFirst();}
        catch(Exception ignored){return false;}
    }
    private int scalar(String sql){
        try(Cursor c=db.getReadableDatabase().rawQuery(sql,null)){return c.moveToFirst()?c.getInt(0):0;}
        catch(Exception ignored){return 0;}
    }

    public static String displayName(ChemicalRepository.Chemical c){
        if(c==null)return "";String name=c.arabicName!=null&&!c.arabicName.trim().isEmpty()?c.arabicName:c.name;
        StringBuilder s=new StringBuilder(name==null?"":name.trim());
        if(c.cas!=null&&!c.cas.trim().isEmpty())s.append(" • CAS ").append(c.cas.trim());
        return s.toString();
    }

    public static int compareText(String a,String b){
        return (a==null?"":a.toLowerCase(Locale.ROOT)).compareTo(b==null?"":b.toLowerCase(Locale.ROOT));
    }
}
