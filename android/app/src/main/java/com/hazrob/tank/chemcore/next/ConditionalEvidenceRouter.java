package com.hazrob.tank.chemcore.next;

/**
 * P05.2: bridge specialist evidence to the shared qualitative evidence graph.
 * A conditional negative is valid ONLY for a specified externally verified
 * challenge and demonstrably independent challenge provenance. Failure to
 * evaluate is not a negative classification or proof of clean air.
 */
public final class ConditionalEvidenceRouter {
    private ConditionalEvidenceRouter() {}
    public static final String VERSION="chemcore-next-conditional-evidence/0.1-g1";
    public static final class ChallengeProvenance {
        public final String independentChallengeSourceRoot,referenceRunUid;
        public final boolean provenanceVerified;
        public ChallengeProvenance(String sourceRoot,String runUid,boolean verified) {
            independentChallengeSourceRoot=sourceRoot==null?"":sourceRoot.trim();
            referenceRunUid=runUid==null?"":runUid.trim();
            provenanceVerified=verified;
        }
        public boolean separateFrom(String sensitivityRoot) {
            return provenanceVerified&&!independentChallengeSourceRoot.isEmpty()
                &&!referenceRunUid.isEmpty()
                &&!independentChallengeSourceRoot.equals(sensitivityRoot);
        }
    }
    public static final class Output {
        public final CoreContracts.EvidenceNode evidence;
        public final String reason,challengeSourceRoot,referenceRunUid;
        public final boolean negativeChallengeProvenanceVerified,gasIdentityConfirmed,
            ppmAuthorized,fieldAlarmAuthorized,g3Qualified;
        private Output(CoreContracts.EvidenceNode evidence,String reason,ChallengeProvenance proof,
                       boolean negativeQualified) {
            this.evidence=evidence;this.reason=reason;
            challengeSourceRoot=proof==null?"":proof.independentChallengeSourceRoot;
            referenceRunUid=proof==null?"":proof.referenceRunUid;
            negativeChallengeProvenanceVerified=negativeQualified;
            gasIdentityConfirmed=false;ppmAuthorized=false;
            fieldAlarmAuthorized=false;g3Qualified=false;
        }
    }
    public static Output evaluate(SingleSensorEvidenceGate.SensitivityModel model,
                                  SingleSensorEvidenceGate.Observation observed,
                                  SingleSensorEvidenceGate.Challenge challenge,
                                  ChallengeProvenance provenance) {
        if(model==null||observed==null)
            throw new IllegalArgumentException("specialist source and actual observed signal required");
        SingleSensorEvidenceGate.Decision decision=
            SingleSensorEvidenceGate.evaluate(model,observed,challenge);
        CoreContracts.EvidencePolarity effective=decision.polarity;
        boolean verified=challenge!=null&&challenge.independentReferenceVerified
            &&provenance!=null&&provenance.separateFrom(model.sourceRoot);
        if(effective==CoreContracts.EvidencePolarity.CONDITIONAL_NEGATIVE&&!verified)
            effective=CoreContracts.EvidencePolarity.NOT_EVALUABLE;
        String note="research_only; source_reason="+decision.reason
            +"; challenge_provenance="+(verified?"separate verified source":"not independently traceable")
            +"; no molecular exclusion outside known challenge";
        CoreContracts.EvidenceNode node=new CoreContracts.EvidenceNode(
            model.sourceRoot,"NEXT_P05_CONDITIONAL_SPECIALIST",model.physicalSensorUid,
            model.canonicalChemicalUid,effective,note);
        return new Output(node,decision.reason.toString(),provenance,
            effective==CoreContracts.EvidencePolarity.CONDITIONAL_NEGATIVE&&verified);
    }
}
