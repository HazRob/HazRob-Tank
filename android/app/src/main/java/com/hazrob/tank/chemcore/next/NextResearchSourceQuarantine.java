package com.hazrob.tank.chemcore.next;

import java.util.Locale;

/**
 * Temporary V8 research source-evidence quarantine.
 * Prevents UCI1081 low-concentration source fingerprints/curves/bundles from
 * influencing ChemCoreNext.analyze(). This is not a deletion from the
 * reference bank or a global ban on the FingerprintExpert research API.
 *
 * Source metadata must retain original provenance. When a record has no
 * trustworthy source identifier, it cannot be qualified and should be
 * withheld by the ingestion process rather than assigned a false root.
 */
public final class NextResearchSourceQuarantine {
  private NextResearchSourceQuarantine() {}

  public static final String POLICY_ID =
      "temporary-uci1081-research-inference-quarantine/1";
  public static final boolean UCI1081_ALLOWED_IN_NEXT_INFERENCE = false;

  private static boolean matches(String sourceIdentifier) {
    if(sourceIdentifier==null || sourceIdentifier.isBlank())return false;
    String normalized=sourceIdentifier.toUpperCase(Locale.ROOT)
        .replaceAll("[^A-Z0-9]", "");
    return normalized.contains("UCI1081")
        || normalized.contains("GASSENSORARRAYLOWCONCENTRATION")
        || normalized.contains("1024432C5CK6F");
  }

  public static boolean blocked(String... provenanceIdentifiers) {
    if(UCI1081_ALLOWED_IN_NEXT_INFERENCE || provenanceIdentifiers==null)return false;
    for(String id:provenanceIdentifiers)if(matches(id))return true;
    return false;
  }
}
