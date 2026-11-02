package com.hazrob.tank.chemcore.next;

import java.util.*;

/**
 * P05.3 evidence association graph. This graph groups all algorithms/sensors
 * under one source root into ONE non-independent observation family, preserves
 * alternative identities and records contradictory observations without
 * creating posterior probabilities. No source citation is a physical G3 vote.
 */
public final class EvidenceConflictGraph {
  private EvidenceConflictGraph() {}
  public enum Link { SAME_SOURCE_CORRELATED, ALTERNATIVE_HYPOTHESIS,
      CROSS_SOURCE_CONTRADICTION, CROSS_SOURCE_SUPPORT }
  public static final class Edge {
    public final String a,b;public final Link kind;public final Long materialUid;
    private Edge(String a,String b,Link kind,Long uid){
      this.a=a;this.b=b;this.kind=kind;materialUid=uid;
    }
  }
  public static final class MaterialEvidence {
    public final long uid;public final List<String> positiveRoots,negativeRoots,ambiguousRoots;
    public final boolean crossSourceContradiction;
    private MaterialEvidence(long uid,Set<String> positive,Set<String> negative,Set<String> ambiguous){
      this.uid=uid;positiveRoots=List.copyOf(positive);negativeRoots=List.copyOf(negative);
      ambiguousRoots=List.copyOf(ambiguous);crossSourceContradiction=!positive.isEmpty()&&!negative.isEmpty();
    }
  }
  public static final class Graph {
    public final String exposureUid;public final List<IndependentEvidenceLedger.RootGroup> roots;
    public final List<Edge> edges;public final Map<Long,MaterialEvidence> materials;
    public final int rawNodes,independentSourceRoots;
    public final boolean hasUnresolvedAlternatives,hasUnresolvedConflicts,g3Qualified,ppmAuthorized;
    private Graph(IndependentEvidenceLedger.Summary s,List<Edge> edges,
                  Map<Long,MaterialEvidence> materials,boolean alt,boolean conflict){
      exposureUid=s.exposureUid;roots=s.roots;rawNodes=s.rawExpertOutputs;
      independentSourceRoots=s.independentSourceRoots;
      this.edges=List.copyOf(edges);this.materials=Collections.unmodifiableMap(materials);
      hasUnresolvedAlternatives=alt;hasUnresolvedConflicts=conflict;
      g3Qualified=false;ppmAuthorized=false;
    }
  }
  private static final class Bucket {
    final Set<String> positive=new TreeSet<>(),negative=new TreeSet<>(),ambiguous=new TreeSet<>();
  }
  public static Graph construct(IndependentEvidenceLedger.Summary summary){
    if(summary==null)throw new IllegalArgumentException("source ledger required");
    List<Edge> edges=new ArrayList<>();TreeMap<Long,Bucket> byUid=new TreeMap<>();
    boolean alternatives=false,conflicts=false;
    for(var root:summary.roots){
      if(root.expertIds.size()>1||root.sensorUids.size()>1)
        edges.add(new Edge(root.sourceRootId,root.sourceRootId,
            Link.SAME_SOURCE_CORRELATED,null));
      if(root.sharedHypothesisRoot){
        alternatives=true;
        edges.add(new Edge(root.sourceRootId,root.sourceRootId,
            Link.ALTERNATIVE_HYPOTHESIS,null));
      }
      if(root.conflictingPolarity)conflicts=true;
      for(long uid:root.alternativeChemicalUids){
        Bucket b=byUid.computeIfAbsent(uid,k->new Bucket());
        switch(root.effectivePolarity){
          case POSITIVE:b.positive.add(root.sourceRootId);break;
          case CONDITIONAL_NEGATIVE:b.negative.add(root.sourceRootId);break;
          case AMBIGUOUS:b.ambiguous.add(root.sourceRootId);break;
          default:break;
        }
      }
    }
    Map<Long,MaterialEvidence> out=new TreeMap<>();
    for(var e:byUid.entrySet()){
      Bucket bucket=e.getValue();long uid=e.getKey();
      for(String a:bucket.positive)for(String z:bucket.negative){
        if(!a.equals(z)){
          edges.add(new Edge(a,z,Link.CROSS_SOURCE_CONTRADICTION,uid));conflicts=true;
        }
      }
      List<String> positives=new ArrayList<>(bucket.positive);
      for(int i=0;i<positives.size();i++)for(int j=i+1;j<positives.size();j++)
        edges.add(new Edge(positives.get(i),positives.get(j),
            Link.CROSS_SOURCE_SUPPORT,uid));
      out.put(uid,new MaterialEvidence(uid,bucket.positive,bucket.negative,bucket.ambiguous));
    }
    return new Graph(summary,edges,out,alternatives,conflicts);
  }
}
