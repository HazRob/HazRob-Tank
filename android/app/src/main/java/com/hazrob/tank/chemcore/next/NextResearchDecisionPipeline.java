package com.hazrob.tank.chemcore.next;

import java.util.*;

/**
 * P05.4/6/7 independent research-only hierarchical arbitration and ranking.
 * Roots, not sensor count or number of source curves, control corroboration.
 * UNKNOWN and MIXTURE_UNRESOLVED are first-class outputs, NOT synthetic ppm.
 * Field alarms are outside this research engine and can never be enabled here.
 */
public final class NextResearchDecisionPipeline {
  private NextResearchDecisionPipeline(){}
  public static final String VERSION="chemcore-next-p05-decision/0.2-g1-source-coherence";
  public static final class Ranked {
    public final long uid;public final String label;
    public final ResearchCandidateGenerator.MaterialKind kind;
    public final List<String> roots;public final int independentPositiveRoots,conditionalNegativeRoots;
    public final boolean contradicted,ppmAuthorized,identityConfirmed,g3Qualified;
    /** Research-only score from an independently-source-rooted multi-MQ fit.
      * It is not a posterior, safety confidence or Tank validation. */
    public final Double sourceConcentrationCoherenceScore;
    private Ranked(ResearchCandidateGenerator.Proposal p,EvidenceConflictGraph.MaterialEvidence evidence,
                   Double researchCoherence){
      uid=p.material.canonicalUid;label=p.material.label;kind=p.material.kind;
      roots=List.copyOf(evidence.positiveRoots);
      independentPositiveRoots=roots.size();conditionalNegativeRoots=evidence.negativeRoots.size();
      contradicted=evidence.crossSourceContradiction;
      sourceConcentrationCoherenceScore=researchCoherence;
      ppmAuthorized=false;identityConfirmed=false;g3Qualified=false;
    }
  }
  public static final class Decision {
    public final CoreContracts.DecisionStatus status;
    public final List<Ranked> eligibleRanked,display;
    public final int eligibleCount,displayCount,blockedContradictoryCount,allSourceRoots;
    public final List<String> reasons;
    public final boolean unknownPossible,mixtureConfirmed,gasIdentityConfirmed,
        ppmCertified,fieldSafetyAlarmEnabled,physicalG3Qualified;
    private Decision(CoreContracts.DecisionStatus state,List<Ranked> list,List<String> why,
                     int maxDisplay,int nBlocked,int roots){
      status=state;eligibleRanked=List.copyOf(list);
      display=List.copyOf(list.subList(0,Math.min(maxDisplay,list.size())));
      eligibleCount=list.size();displayCount=display.size();blockedContradictoryCount=nBlocked;
      allSourceRoots=roots;reasons=List.copyOf(why);
      unknownPossible=true;mixtureConfirmed=false;gasIdentityConfirmed=false;
      ppmCertified=false;fieldSafetyAlarmEnabled=false;physicalG3Qualified=false;
    }
  }
  public static Decision evaluate(EvidenceConflictGraph.Graph graph,
                                 ResearchCandidateGenerator.Output proposals,int maxDisplay) {
    return evaluate(graph,proposals,maxDisplay,Map.of());
  }
  /** Rank eligible hypotheses by root corroboration then uncalibrated
   * MULTI-channel source coherence; keep every eligible hypothesis and
   * unknown/multi-hypothesis states. No root multiplication or invented %.
   */
  public static Decision evaluate(EvidenceConflictGraph.Graph graph,
             ResearchCandidateGenerator.Output proposals,int maxDisplay,
             Map<Long,Double> researchOnlyCoherenceScores) {
    if(researchOnlyCoherenceScores==null)
      throw new IllegalArgumentException("coherence scores map required");
    for(var e:researchOnlyCoherenceScores.entrySet())
      if(e.getKey()==null||e.getKey()<=0||e.getValue()==null||
          !Double.isFinite(e.getValue())||e.getValue()<0)
        throw new IllegalArgumentException("invalid research coherence score");
    if(graph==null||proposals==null||maxDisplay<0)
      throw new IllegalArgumentException("graph, canonical candidates and display cap required");
    List<Ranked> allowed=new ArrayList<>();List<String> reasons=new ArrayList<>();
    int contradicted=0;
    for(var p:proposals.eligibleResearchCandidates) {
      var m=graph.materials.get(p.material.canonicalUid);
      if(m==null||m.positiveRoots.isEmpty())continue;
      // Reject a proposal built using an unrelated root, even if it has an eligible material UID.
      if(!m.positiveRoots.containsAll(p.independentRoots)||
         !p.independentRoots.containsAll(m.positiveRoots)) {
        reasons.add("SOURCE_ROOT_MISMATCH uid="+p.material.canonicalUid);continue;
      }
      if(m.crossSourceContradiction){
        contradicted++;reasons.add("UNRESOLVED_CROSS_SOURCE_CONTRADICTION uid="+m.uid);
        continue; // fail-closed, not an unqualified molecular exclusion
      }
      allowed.add(new Ranked(p,m,researchOnlyCoherenceScores.get(p.material.canonicalUid)));
    }
    // No artificial probability, missing coherence always remains eligible.
    // Prefer a jointly measured multichannel source root over a single
    // unconstrained curve IF the number of independent roots is identical.
    allowed.sort(Comparator.comparingInt((Ranked x)->x.independentPositiveRoots).reversed()
        .thenComparingInt(x->x.sourceConcentrationCoherenceScore==null?1:0)
        .thenComparingDouble(x->x.sourceConcentrationCoherenceScore==null?
            Double.POSITIVE_INFINITY:x.sourceConcentrationCoherenceScore)
        .thenComparingLong(x->x.uid));
    final CoreContracts.DecisionStatus status;
    if(allowed.isEmpty()){
      status=CoreContracts.DecisionStatus.UNKNOWN;
      reasons.add("NO_INDEPENDENT_UNCONTRADICTED_SOURCE_QUALIFIED_CANDIDATE");
    }else if(allowed.size()>1) {
      status=CoreContracts.DecisionStatus.MIXTURE_UNRESOLVED;
      reasons.add("MULTIPLE_SOURCE_HYPOTHESES; MIXTURE_POSSIBLE_BUT_NOT_DEMONSTRATED");
    }else{
      status=CoreContracts.DecisionStatus.RESEARCH_CANDIDATES;
      reasons.add("ONE_SOURCE_SUPPORTED_RESEARCH_HYPOTHESIS_NOT_MOLECULAR_IDENTIFICATION");
    }
    if(graph.hasUnresolvedAlternatives)reasons.add("SOME_SOURCE_ROOTS_HAVE_COMPETING_IDENTITIES");
    if(graph.hasUnresolvedConflicts)reasons.add("SOURCE_GRAPH_CONTAINS_UNRESOLVED_CONTRADICTION");
    if(graph.independentSourceRoots==0)reasons.add("NO_SOURCE_EVIDENCE");
    return new Decision(status,allowed,reasons,maxDisplay,contradicted,graph.independentSourceRoots);
  }
  /** Produce immutable research result. No safety actuator can read a certified flag from it. */
  public static CoreContracts.Result result(CoreContracts.Snapshot capture,String snapshotHash,
                                            EvidenceConflictGraph.Graph graph,Decision decision){
    if(capture==null||graph==null||decision==null)
      throw new IllegalArgumentException("snapshot and evidence decision required");
    List<CoreContracts.EvidenceNode> sources=new ArrayList<>();
    for(var r:graph.roots){
      String uid=r.sensorUids.get(0);
      sources.add(new CoreContracts.EvidenceNode(r.sourceRootId,
        "NEXT_P05_RESEARCH_ARBITRATION",uid,r.uniqueChemicalUid,r.effectivePolarity,
        "SOURCE_ONLY_WITHOUT_TANK_G3; alternatives="+r.alternativeChemicalUids.size()));
    }
    List<CoreContracts.Candidate> ranked=new ArrayList<>();
    for(var c:decision.eligibleRanked)
      ranked.add(new CoreContracts.Candidate(c.uid,
          "research roots="+c.independentPositiveRoots+
            "; multi_MQ_source_score="+c.sourceConcentrationCoherenceScore+
            "; false certified ppm/identity; "+decision.status,
          c.roots));
    return new CoreContracts.Result(VERSION,capture,snapshotHash,decision.status,
      sources,ranked,decision.reasons);
  }
  /** Immutable operator comments are appended as feedback; never alter raw evidence. */
  public static final class Feedback {
    public final String operatorUid,feedbackUid,snapshotUid,note;
    public Feedback(String operator,String uid,String snapshot,String text){
      if(blank(operator)||blank(uid)||blank(snapshot)||blank(text))
        throw new IllegalArgumentException("operator feedback requires provenance");
      operatorUid=operator;feedbackUid=uid;snapshotUid=snapshot;note=text;
    }
    private static boolean blank(String x){return x==null||x.isBlank();}
  }
  public static List<Feedback> appendFeedback(List<Feedback> previous,Feedback next){
    if(previous==null||next==null)throw new IllegalArgumentException("feedback ledger required");
    List<Feedback> copy=new ArrayList<>();
    for(Feedback f:previous){
      if(f==null)throw new IllegalArgumentException("null historic feedback");
      if(f.feedbackUid.equals(next.feedbackUid))throw new IllegalArgumentException("duplicate feedback UID");
      copy.add(f);
    }
    copy.add(next);
    return List.copyOf(copy);
  }
}
