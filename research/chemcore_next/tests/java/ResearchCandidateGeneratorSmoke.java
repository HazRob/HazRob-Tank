package com.hazrob.tank.chemcore.next;

import java.util.ArrayList;
import java.util.List;

/** G1 tests: no legacy 144 boundary, no ten-candidate quota, no fake source evidence. */
public final class ResearchCandidateGeneratorSmoke {
    private static int checks;
    private static void ok(boolean yes,String why){
        checks++;if(!yes)throw new AssertionError("P05.1 #"+checks+": "+why);
    }
    private static CoreContracts.EvidenceNode node(String root,long uid){
        return new CoreContracts.EvidenceNode(root,"CURVE","s-1",uid,
            CoreContracts.EvidencePolarity.POSITIVE,"research");
    }
    private static IndependentEvidenceLedger.Summary ledger(List<CoreContracts.EvidenceNode> nodes){
        return IndependentEvidenceLedger.consolidate("exposure-synthetic",nodes);
    }
    private static ResearchCandidateGenerator.Output generate(int count,int shown){
        List<ResearchCandidateGenerator.Material> materials=new ArrayList<>();
        List<ResearchCandidateGenerator.AssetLink> assets=new ArrayList<>();
        List<CoreContracts.EvidenceNode> evidence=new ArrayList<>();
        for(int i=0;i<736;i++)
            materials.add(new ResearchCandidateGenerator.Material(i+1,"material-"+(i+1),
                i>=734?ResearchCandidateGenerator.MaterialKind.GENERAL_CLASS:
                    ResearchCandidateGenerator.MaterialKind.MOLECULAR_CHEMICAL));
        for(int i=0;i<count;i++){
            long uid=150+i; // explicitly outside historical legacy144
            String root="ORIGINAL-SOURCE-"+i;
            evidence.add(node(root,uid));
            assets.add(new ResearchCandidateGenerator.AssetLink(root,"CURVE-ASSET-"+i,uid,true));
        }
        return ResearchCandidateGenerator.generate(materials,assets,ledger(evidence),shown);
    }
    private static void failed(Runnable r){
        boolean refused=false;try{r.run();}catch(IllegalArgumentException x){refused=true;}
        ok(refused,"invalid catalog structure rejected");
    }
    public static void main(String[] args){
        for(int n:new int[]{0,1,5,10,12}){
            ResearchCandidateGenerator.Output o=generate(n,10);
            ok(o.fullCatalogCount==736,"all canonical UIDs searched without legacy gate");
            ok(o.eligibleCount==n,"eligible count exactly "+n+" no padding");
            ok(o.displayCandidates.size()==Math.min(n,10),"UI limit is not inference quota");
            ok(o.displayTruncated==(n>10),"more than 10 eligible not discarded");
            ok(o.unknownPreserved&&!o.gasIdentityConfirmed&&!o.ppmAuthorized,"UNKNOWN and G3 guard");
            if(n>0)ok(o.eligibleResearchCandidates.get(0).material.canonicalUid==150,
                "nonlegacy material first candidate");
        }
        List<ResearchCandidateGenerator.Material> catalog=List.of(
            new ResearchCandidateGenerator.Material(735,"Smoke",
                ResearchCandidateGenerator.MaterialKind.GENERAL_CLASS),
            new ResearchCandidateGenerator.Material(76,"Methane",
                ResearchCandidateGenerator.MaterialKind.MOLECULAR_CHEMICAL));
        List<ResearchCandidateGenerator.AssetLink> links=List.of(
            new ResearchCandidateGenerator.AssetLink("source-shared","asset-a",735,true),
            new ResearchCandidateGenerator.AssetLink("source-shared","asset-b",76,true));
        var evidence=ledger(List.of(node("source-shared",735),node("source-shared",76)));
        var o=ResearchCandidateGenerator.generate(catalog,links,evidence,10);
        ok(o.eligibleCount==0,"competing source labels cannot create two molecular candidates");
        links=List.of(new ResearchCandidateGenerator.AssetLink("source-1","asset",735,false));
        o=ResearchCandidateGenerator.generate(catalog,links,ledger(List.of(node("source-1",735))),10);
        ok(o.eligibleCount==0,"source incompatible feature not eligible");
        links=List.of(new ResearchCandidateGenerator.AssetLink("source-1","asset",735,true));
        o=ResearchCandidateGenerator.generate(catalog,links,ledger(List.of(node("source-1",735))),0);
        ok(o.eligibleCount==1&&o.displayCandidates.isEmpty(),"zero display does not delete candidate");
        ok(o.eligibleResearchCandidates.get(0).material.kind==
            ResearchCandidateGenerator.MaterialKind.GENERAL_CLASS,"class distinguished from molecule");
        failed(()->ResearchCandidateGenerator.generate(
            List.of(new ResearchCandidateGenerator.Material(1,"a",ResearchCandidateGenerator.MaterialKind.MOLECULAR_CHEMICAL),
                    new ResearchCandidateGenerator.Material(1,"duplicate",ResearchCandidateGenerator.MaterialKind.MOLECULAR_CHEMICAL)),
            List.of(),ledger(List.of()),10));
        failed(()->ResearchCandidateGenerator.generate(List.of(),List.of(),ledger(List.of()),-1));
        System.out.println("CHEMCORE_NEXT_P05_1_CANDIDATE_PASS assertions="+checks+
            " full_catalog=736 scenarios=0_1_5_10_12 no_legacy_144_limit=true G3=0");
    }
}
