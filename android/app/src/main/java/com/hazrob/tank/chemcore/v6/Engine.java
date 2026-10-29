package com.hazrob.tank.chemcore.v6;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * New, self-contained HazRob Tank ChemCore V6 inference prototype.
 *
 * NO dependency on any prior ChemCore engine, response model, identity solver,
 * application database, manufacturer library, or legacy result envelope.
 * Reads independent sensor instances and 43 source response curves, performs
 * deterministic log-domain least squares and exposes ALL finite candidate fits.
 *
 * Source reference equations: Rs/R0 = a*(ppm/outputScale)^b.
 * Regression in log10 ppm is convex with closed-form optimum for the present
 * single-source power-law family. Future source adapters may implement
 * different forward models without changing this input/output contract.
 *
 * Research estimates, not calibrated gas measurement or certified identity.
 */
public final class Engine {
    public static final String SCHEMA="hazrob-tank-chemcore-new-v6/1";
    private Engine(){}

    public static final class Channel {
        public final String sensorType,sensorInstance;
        public final double rs,r0;
        public Channel(String type,String instance,double observedRs,double baselineR0){
            sensorType=normalizeType(type);
            sensorInstance=instance==null||instance.trim().isEmpty()?sensorType:instance.trim();
            rs=observedRs;r0=baselineR0;
        }
        public boolean numericallyValid(){
            return positiveFinite(rs)&&positiveFinite(r0)&&positiveFinite(rs/r0);
        }
    }

    public static final class SourceFit {
        public final String id, sensorType, sensorInstance, sourceRoot,gasLabel;
        public final long chemicalId;
        public final double observedRatio,fitPpm;
        public final boolean outsideSourceRange;
        public final List<String> advisories;
        private SourceFit(Channel input,SourceCatalog.Curve src,double ppm){
            id=src.id();sourceRoot=SourceCatalog.SOURCE_ROOT;
            sensorType=src.sensorType;sensorInstance=input.sensorInstance;
            gasLabel=src.label;chemicalId=src.legacyChemicalId;
            observedRatio=input.rs/input.r0;fitPpm=ppm;
            outsideSourceRange=ppm<src.minSourcePpm || ppm>src.maxSourcePpm;
            List<String> caveats=new ArrayList<>();
            caveats.add("SOURCE_R0_DEFINITION_UNVERIFIED");
            caveats.add("SOURCE_HEATER_PROFILE_UNVERIFIED");
            if(outsideSourceRange)caveats.add("EXTRAPOLATED_BEYOND_REFERENCE_DOMAIN");
            if(src.outputScale!=1)caveats.add("SOURCE_OUTPUT_SCALE_NONUNITY");
            advisories=Collections.unmodifiableList(caveats);
        }
    }

    public static final class Candidate {
        public final long chemicalId;
        public final String primaryLabel;
        public final List<String> observedLabels,sourceCurveIds;
        public final int supportingSensorInstances, contributingSourceCurves;
        public final double jointResearchPpm, jointResidualRmseLog10;
        public final boolean anySourceDomainExtrapolated;
        private Candidate(long uid,String label,Set<String> names,Set<String> curves,
                          int sensorCount,int curveCount,double ppm,double residual,
                          boolean extrapolation){
            chemicalId=uid;primaryLabel=label;
            observedLabels=Collections.unmodifiableList(new ArrayList<>(names));
            sourceCurveIds=Collections.unmodifiableList(new ArrayList<>(curves));
            supportingSensorInstances=sensorCount;
            contributingSourceCurves=curveCount;
            jointResearchPpm=ppm;
            jointResidualRmseLog10=residual;
            anySourceDomainExtrapolated=extrapolation;
        }
    }

    public static final class Result {
        public final String schema;
        public final List<SourceFit> allSourceFits;
        public final List<Candidate> rankedCandidates;
        public final int observedPhysicalChannels, rejectedNumericalChannels;
        public final boolean actualTankCalibrationVerified,ppmReportable;
        public final String rankingSemantics;
        private Result(List<SourceFit> rows,List<Candidate> rankings,int observed,int rejected){
            schema=SCHEMA;allSourceFits=Collections.unmodifiableList(new ArrayList<>(rows));
            rankedCandidates=Collections.unmodifiableList(new ArrayList<>(rankings));
            observedPhysicalChannels=observed;rejectedNumericalChannels=rejected;
            actualTankCalibrationVerified=false;ppmReportable=false;
            rankingSemantics="sensor_breadth_then_joint_log_residual_not_calibrated_probability";
        }
    }

    private static boolean positiveFinite(double x){return Double.isFinite(x)&&x>0;}
    private static String normalizeType(String s){
        return s==null?"":s.trim().toUpperCase(Locale.ROOT);
    }

    private static final class Term {
        final Channel observed;final SourceCatalog.Curve curve;
        Term(Channel x,SourceCatalog.Curve y){observed=x;curve=y;}
    }
    private static final class Group {
        final long uid;
        String firstLabel;
        final Set<String> labels=new LinkedHashSet<>(),ids=new LinkedHashSet<>();
        final List<Term> terms=new ArrayList<>();
        Group(long id){uid=id;}
        void add(Term term){
            if(firstLabel==null)firstLabel=term.curve.label;
            labels.add(term.curve.label);
            ids.add(term.curve.id());
            terms.add(term);
        }
        Candidate fit() {
            double num=0,denom=0;
            Set<String> sensors=new LinkedHashSet<>();
            // A source model for the same physical channel is counted only once.
            Map<String,Term> unique=new LinkedHashMap<>();
            for(Term term:terms){
                String key=term.observed.sensorInstance+"::"+term.curve.sensorType;
                if(!unique.containsKey(key))unique.put(key,term);
            }
            for(Term t:unique.values()){
                double y=Math.log10(t.observed.rs/t.observed.r0);
                double b=t.curve.b;
                double alpha=Math.log10(t.curve.a)-b*Math.log10(t.curve.outputScale);
                num+=b*(y-alpha);
                denom+=b*b;
                sensors.add(t.observed.sensorInstance);
            }
            if(!positiveFinite(denom))return null;
            double logPpm=num/denom;
            double ppm=Math.pow(10,logPpm);
            if(!positiveFinite(ppm))return null;
            double residual=0;
            boolean extrapolation=false;
            for(Term t:unique.values()){
                double y=Math.log10(t.observed.rs/t.observed.r0);
                double predicted=t.curve.log10Prediction(logPpm);
                double err=y-predicted;
                residual+=err*err;
                if(ppm<t.curve.minSourcePpm||ppm>t.curve.maxSourcePpm)
                    extrapolation=true;
            }
            double rmse=Math.sqrt(residual/unique.size());
            if(!Double.isFinite(rmse))return null;
            return new Candidate(uid,firstLabel,labels,ids,sensors.size(),unique.size(),
                                 ppm,rmse,extrapolation);
        }
    }

    public static Result analyze(List<Channel> inputs){
        // One measurement per real physical instance and sensor model.
        Map<String,Channel> channels=new LinkedHashMap<>();
        int invalid=0;
        if(inputs!=null)for(Channel channel:inputs){
            if(channel==null||channel.sensorType.isEmpty()||!channel.numericallyValid()){
                invalid++;continue;
            }
            String key=channel.sensorInstance+"::"+channel.sensorType;
            channels.put(key,channel);
        }
        List<SourceFit> sourceFits=new ArrayList<>();
        Map<Long,Group> candidates=new LinkedHashMap<>();
        for(SourceCatalog.Curve source:SourceCatalog.curves()){
            if(!source.numericalDefinitionValid()){invalid++;continue;}
            for(Channel ch:channels.values()){
                if(!source.sensorType.equalsIgnoreCase(ch.sensorType))continue;
                double ratio=ch.rs/ch.r0;
                double logPpm=Math.log10(source.outputScale)+
                    (Math.log10(ratio)-Math.log10(source.a))/source.b;
                double concentration=Math.pow(10,logPpm);
                if(!positiveFinite(concentration)){invalid++;continue;}
                sourceFits.add(new SourceFit(ch,source,concentration));
                Group group=candidates.get(source.legacyChemicalId);
                if(group==null){group=new Group(source.legacyChemicalId);
                    candidates.put(source.legacyChemicalId,group);}
                group.add(new Term(ch,source));
            }
        }
        List<Candidate> rank=new ArrayList<>();
        for(Group group:candidates.values()){
            Candidate candidate=group.fit();
            if(candidate!=null)rank.add(candidate);
            else invalid++;
        }
        // Ordering is only an interpretive preference. No candidate is suppressed.
        rank.sort(Comparator
            .comparingInt((Candidate c)->c.supportingSensorInstances).reversed()
            .thenComparingDouble(c->c.jointResidualRmseLog10)
            .thenComparingLong(c->c.chemicalId));
        return new Result(sourceFits,rank,channels.size(),invalid);
    }
}
