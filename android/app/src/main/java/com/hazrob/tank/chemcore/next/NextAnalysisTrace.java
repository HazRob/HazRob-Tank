package com.hazrob.tank.chemcore.next;

import java.util.*;

/**
 * R3 immutable provenance-aware trace of EACH EXECUTED source technique,
 * not an assertion that all 80 research techniques were activated.
 * Result values are research diagnostics and never safety-authorized ppm.
 */
public final class NextAnalysisTrace {
  private NextAnalysisTrace() {}
  public static final String VERSION="chemcore-next-execution-trace/0.3-g1";
  public static final class Event {
    public final String stage,sourceAssetId,sourceRoot,sensorUid,status,reason;
    public final Long chemicalUid;
    public final Double sourceOnlyDiagnosticValue;
    public Event(String stage,String asset,String root,String sensor,Long uid,
                 String status,String reason,Double value) {
      this.stage=required(stage);
      sourceAssetId=asset==null?"":asset;
      sourceRoot=root==null?"":root;
      sensorUid=sensor==null?"":sensor;
      chemicalUid=uid;
      this.status=required(status);
      this.reason=reason==null?"":reason;
      if(value!=null&&!Double.isFinite(value))
        throw new IllegalArgumentException("trace value cannot be nonfinite");
      sourceOnlyDiagnosticValue=value;
    }
    private static String required(String x) {
      if(x==null||x.isBlank())throw new IllegalArgumentException("trace field required");
      return x;
    }
  }
  public static List<Event> immutable(List<Event> events) {
    if(events==null||events.contains(null))
      throw new IllegalArgumentException("trace requires all non-null events");
    return List.copyOf(events);
  }
}
