package com.hazrob.tank.chemcore.next;

import java.util.*;

/**
 * V8 P07.3 explicit source-of-truth engine selection; never silently fall
 * back to legacy V6's 144-item source-curve pseudo-ranking.
 */
public final class NextEngineRoutingPolicy {
  private NextEngineRoutingPolicy(){}
  public enum Target { NEXT_RESEARCH, DISABLED_REQUIRE_OPERATOR_ACTION, V6_EXPLICIT_COMPARISON_ONLY }
  public static final String SCHEMA="hazrob.tank.v8.next.routing/1";
  public static final class Selection {
    public final Target target;
    public final boolean eligibleForNewDecisionPersistence,ppmReportable,gasIdentityConfirmed,g3Qualified;
    public final String provenance;
    private Selection(Target target,String provenance){
      this.target=target;this.provenance=provenance;
      eligibleForNewDecisionPersistence=target==Target.NEXT_RESEARCH;
      ppmReportable=false;gasIdentityConfirmed=false;g3Qualified=false;
    }
  }
  public static Selection choose(boolean researchEnabled,boolean explicitV6Comparison){
    if(explicitV6Comparison)
      return new Selection(Target.V6_EXPLICIT_COMPARISON_ONLY,
        "V6_DIAGNOSTIC_SIDE_BY_SIDE_ONLY_NOT_NEXT_RESULTS");
    if(!researchEnabled)
      return new Selection(Target.DISABLED_REQUIRE_OPERATOR_ACTION,
        "NEXT_DISABLED_NO_LEGACY_FALLBACK_NO_GAS_HYPOTHESIS");
    return new Selection(Target.NEXT_RESEARCH,"NEXT_CHEMCORE_RESEARCH_ONLY_G1_NO_TANK_G3");
  }
  public static void requireNext(Selection selected) {
    if(selected==null||selected.target!=Target.NEXT_RESEARCH)
      throw new IllegalStateException("NEXT_NOT_AUTHORIZED_OR_COMPARISON_MODE_NO_SILENT_V6");
  }
}
