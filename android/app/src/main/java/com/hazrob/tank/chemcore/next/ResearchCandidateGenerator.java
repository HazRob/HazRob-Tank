package com.hazrob.tank.chemcore.next;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * P05.1 source-qualified research candidate eligibility across ALL canonical
 * material UIDs. A list of ten is not a quota. Source labels alone, source
 * root duplicates, unknown materials, absent feature compatibility and
 * NOT_EVALUABLE/AMBIGUOUS evidence never create an eligible candidate.
 */
public final class ResearchCandidateGenerator {
    private ResearchCandidateGenerator() {}
    public static final String VERSION="chemcore-next-candidate-generator/0.1-g1";
    public enum MaterialKind { MOLECULAR_CHEMICAL, GENERAL_CLASS }
    public static final class Material {
        public final long canonicalUid;
        public final String label;
        public final MaterialKind kind;
        public Material(long uid,String label,MaterialKind kind) {
            if(uid<=0||label==null||label.isBlank()||kind==null)
                throw new IllegalArgumentException("canonical material UID, label and class required");
            canonicalUid=uid;this.label=label.trim();this.kind=kind;
        }
    }
    /** Proves the same source root and feature transform can be evaluated, not G3. */
    public static final class AssetLink {
        public final String sourceRoot, sourceAssetUid;
        public final long materialUid;
        public final boolean sourceFeatureComparable;
        public AssetLink(String root,String asset,long uid,boolean comparable) {
            if(root==null||root.isBlank()||asset==null||asset.isBlank()||uid<=0)
                throw new IllegalArgumentException("source root, asset UID and material UID required");
            sourceRoot=root;sourceAssetUid=asset;materialUid=uid;sourceFeatureComparable=comparable;
        }
    }
    public static final class Proposal {
        public final Material material;
        public final List<String> independentRoots,sourceAssets;
        public final int researchIndependentRootCount;
        public final boolean ppmAuthorized,identityConfirmed,physicalG3Verified;
        private Proposal(Material m,Set<String> roots,Set<String> assets) {
            material=m;independentRoots=Collections.unmodifiableList(new ArrayList<>(roots));
            sourceAssets=Collections.unmodifiableList(new ArrayList<>(assets));
            researchIndependentRootCount=roots.size();
            ppmAuthorized=false;identityConfirmed=false;physicalG3Verified=false;
        }
    }
    public static final class Output {
        public final List<Proposal> eligibleResearchCandidates,displayCandidates;
        public final int fullCatalogCount,eligibleCount,displayLimit;
        public final boolean displayTruncated,unknownPreserved,ppmAuthorized,gasIdentityConfirmed;
        private Output(List<Proposal> all,int materialCount,int limit) {
            eligibleResearchCandidates=Collections.unmodifiableList(new ArrayList<>(all));
            displayCandidates=Collections.unmodifiableList(new ArrayList<>(
                all.subList(0,Math.min(limit,all.size()))));
            fullCatalogCount=materialCount;eligibleCount=all.size();displayLimit=limit;
            displayTruncated=limit<all.size();unknownPreserved=true;
            ppmAuthorized=false;gasIdentityConfirmed=false;
        }
    }
    public static Output generate(Collection<Material> masterCatalog,
                                  Collection<AssetLink> linkedSourceAssets,
                                  IndependentEvidenceLedger.Summary evidence,
                                  int maximumDisplay) {
        if(masterCatalog==null||linkedSourceAssets==null||evidence==null||maximumDisplay<0)
            throw new IllegalArgumentException("catalog, assets, exposure source ledger and display limit required");
        Map<Long,Material> all=new HashMap<>();
        for(Material m:masterCatalog) {
            if(m==null||all.putIfAbsent(m.canonicalUid,m)!=null)
                throw new IllegalArgumentException("catalog contains missing or duplicate canonical UID");
        }
        Map<Long,Map<String,Set<String>>> links=new HashMap<>();
        for(AssetLink a:linkedSourceAssets) {
            if(a==null)throw new IllegalArgumentException("null asset");
            if(!all.containsKey(a.materialUid)||!a.sourceFeatureComparable)continue;
            links.computeIfAbsent(a.materialUid,k->new HashMap<>())
                 .computeIfAbsent(a.sourceRoot,k->new HashSet<>()).add(a.sourceAssetUid);
        }
        Map<Long,Set<String>> rootsByUid=new LinkedHashMap<>();
        Map<Long,Set<String>> assetsByUid=new LinkedHashMap<>();
        for(IndependentEvidenceLedger.RootGroup g:evidence.roots) {
            if(!g.positiveResearchHypothesis())continue;
            long uid=g.uniqueChemicalUid;
            if(!all.containsKey(uid))continue;
            Map<String,Set<String>> byRoot=links.get(uid);
            if(byRoot==null || !byRoot.containsKey(g.sourceRootId))continue;
            rootsByUid.computeIfAbsent(uid,k->new HashSet<>()).add(g.sourceRootId);
            assetsByUid.computeIfAbsent(uid,k->new HashSet<>())
                       .addAll(byRoot.get(g.sourceRootId));
        }
        List<Long> order=new ArrayList<>(rootsByUid.keySet());
        Collections.sort(order); // no invented probability/ranking
        List<Proposal> eligible=new ArrayList<>();
        for(Long uid:order)eligible.add(new Proposal(all.get(uid),rootsByUid.get(uid),assetsByUid.get(uid)));
        return new Output(eligible,all.size(),maximumDisplay);
    }
}
