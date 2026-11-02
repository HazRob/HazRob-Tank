package com.hazrob.tank.chemcore.next;

import java.util.*;

/**
 * V8 P07.4 render-only immutable projection from the EXACT original Next
 * decision and frozen snapshot. Neither sorting, recomputing gas fits nor
 * backfilling the legacy 144 chemical list is allowed downstream.
 */
public final class NextDecisionPresentation {
  private NextDecisionPresentation(){}
  public static final String SCHEMA="hazrob.tank.v8.next.decision.presentation/1";
  public static final class Item {
    public final int ordinal;
    public final long canonicalUid;
    public final String explanation;
    public final List<String> evidenceRoots;
    private Item(int ordinal,CoreContracts.Candidate original){
      this.ordinal=ordinal;canonicalUid=original.canonicalChemicalUid;
      explanation=original.evidenceSummary;
      evidenceRoots=List.copyOf(original.sourceRoots);
    }
  }
  public static final class View {
    public final String snapshotUid,snapshotSha256,stableSha256,engineVersion;
    public final CoreContracts.DecisionStatus status;
    public final List<Item> orderedCandidates;
    public final List<String> problems;
    public final int totalEligible;
    public final boolean needsUnknownOpenSet,physicallyValidatedG3,
      ppmReportable,gasIdentityConfirmed,fieldSafetyAlarmAllowed;
    private View(CoreContracts.Result source,List<Item> ranked){
      snapshotUid=source.snapshotUid;snapshotSha256=source.snapshotHash;
      stableSha256=source.stableManifestHash;engineVersion=source.engineVersion;
      status=source.status;orderedCandidates=List.copyOf(ranked);
      problems=List.copyOf(source.problems);totalEligible=ranked.size();
      needsUnknownOpenSet=true;physicallyValidatedG3=false;ppmReportable=false;
      gasIdentityConfirmed=false;fieldSafetyAlarmAllowed=false;
    }
  }
  public static View project(CoreContracts.Snapshot frozen,CoreContracts.Result source){
    if(frozen==null||source==null)throw new IllegalArgumentException("frozen source/decision required");
    String hash=ChemCoreNext.snapshotHash(frozen);
    if(!source.snapshotUid.equals(frozen.uid)||!source.snapshotHash.equals(hash)||
       !source.stableManifestHash.equals(frozen.stableManifestHash)||
       source.reportablePpm||source.physicallyValidatedG3)
      throw new IllegalArgumentException("tampered/stale Next decision cannot be displayed");
    Set<Long> seen=new HashSet<>();List<Item> items=new ArrayList<>();
    for(var c:source.rankedEligibleCandidates){
      if(c==null||!seen.add(c.canonicalChemicalUid))
        throw new IllegalArgumentException("duplicate/null canonical research candidate");
      items.add(new Item(items.size()+1,c));
    }
    if((source.status==CoreContracts.DecisionStatus.UNKNOWN||
        source.status==CoreContracts.DecisionStatus.INSUFFICIENT_EVIDENCE||
        source.status==CoreContracts.DecisionStatus.NO_INFERENCE_MODEL||
        source.status==CoreContracts.DecisionStatus.INVALID_INPUT)&&!items.isEmpty())
      throw new IllegalArgumentException("fail-closed state cannot contain ranked identities");
    return new View(source,items);
  }
}
