package com.servoc.app.report;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.hazrob.tank.chemcore.next.CoreContracts;
import com.servoc.app.chemcore.nextintegration.NextDecisionStore;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * One V7-size approved-source PDF appendix for ChemCore Next field sessions.
 * This is a read-only rendering of the verified NextDecisionStore.Receipt.
 * It NEVER reruns the V5/V6 chemical engine or alters original source data.
 *
 * The old V6 appendix is retained solely for historical sessions without
 * a verified Next receipt; the PDF geometry and V7 chrome are unchanged.
 */
final class V8NextFieldResearchAppendixPdf {
    private static final int INK=0xFF263238, MUTED=0xFF57687A,
        LINE=0xFFCFD8DC, LIGHT=0xFFEDF2F6, ALERT=0xFFC62828;
    private V8NextFieldResearchAppendixPdf(){}
    private static Paint paint(int color,float size,boolean bold){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);p.setTextSize(size);p.setFakeBoldText(bold);return p;
    }
    private static void write(Canvas c,String value,float x,float y,float size,
                              int color,boolean bold){
        c.drawText(value==null?"—":value,x,y,paint(color,size,bold));
    }
    private static String shortValue(String v,int length){
        if(v==null||v.isEmpty())return "—";
        return v.length()<=length?v:v.substring(0,length-1)+"…";
    }
    private static String num(Double x){
        return x==null||!Double.isFinite(x)?"—":
            String.format(Locale.US,"%.3f",x);
    }
    private static void band(Canvas c,float top,float bottom){
        Paint bg=paint(LIGHT,8,false);
        c.drawRoundRect(new RectF(30,top,812,bottom),5,5,bg);
    }
    private static int valid(NextDecisionStore.Receipt r){
        int n=0;
        for(CoreContracts.Reading x:r.sourceSnapshot.readings)
            if(x.quality==CoreContracts.SampleQuality.VALID)n++;
        return n;
    }
    static void draw(Canvas c,NextDecisionStore.Receipt receipt,String fieldSessionName){
        if(c==null||receipt==null)throw new IllegalArgumentException("verified Next receipt required");
        NextDecisionStore.Receipt n=receipt;
        CoreContracts.Result d=n.originalDecision;
        Set<String> roots=new LinkedHashSet<>();
        for(CoreContracts.EvidenceNode node:d.evidence)roots.add(node.sourceRootId);
        write(c,"ChemCore Next • أرشيف القياس الأصلي والأدلة البحثية",
            42,54,16,Color.BLACK,true);
        write(c,"READ ONLY • NO CERTIFIED PPM / GAS ID / G3 • ONE APPROVED FIELD MEASUREMENT",
            36,76,8.6f,ALERT,true);
        write(c,"Session "+shortValue(n.sessionId,29)+
                "   Decision "+shortValue(n.decisionUid,31)+
                "   Source roots "+roots.size(),
            36,96,8.5f,MUTED,false);
        band(c,109,136);
        write(c,"Next "+d.status.name(),42,127,10,INK,true);
        write(c,"MQ valid "+valid(n)+"/"+n.sourceSnapshot.readings.size(),
            302,127,9,INK,true);
        write(c,"Stable SHA "+shortValue(n.sourceSnapshot.stableManifestHash,22),
            504,127,8.5f,MUTED,false);

        write(c,"القناة الأصلية",42,155,8,INK,true);
        write(c,"ADC",249,155,8,INK,true);
        write(c,"Rs kΩ",312,155,8,INK,true);
        write(c,"R_air kΩ",405,155,8,INK,true);
        write(c,"Rs/R_air",506,155,8,INK,true);
        write(c,"حالة المصدر",623,155,8,INK,true);
        int row=0;
        for(CoreContracts.Reading q:n.sourceSnapshot.readings){
            if(row>=8)break; // exactly eight physical Tank MQ sensor slots
            float y=175+row*22;
            if((row%2)==0)band(c,y-15,y+5);
            write(c,shortValue(q.sensorUid,35),42,y,7.8f,INK,true);
            write(c,q.rawAdc==null?"—":String.valueOf(q.rawAdc),249,y,8,INK,false);
            write(c,num(q.rsKohm),312,y,8,INK,false);
            write(c,num(q.rAirKohm),405,y,8,INK,false);
            double ratio=q.airRatioOrNaN();
            write(c,Double.isFinite(ratio)?String.format(Locale.US,"%.3f",ratio):"—",
                506,y,8,INK,false);
            write(c,shortValue(q.quality.name(),22),623,y,7.7f,
                q.quality==CoreContracts.SampleQuality.VALID?0xFF1B5E20:ALERT,false);
            row++;
        }
        write(c,"SOURCE EVIDENCE AND CANDIDATE UIDs • NOT A MOLECULAR IDENTITY CLAIM",
            42,374,9,INK,true);
        Paint line=paint(LINE,1,false);line.setStrokeWidth(.75f);
        c.drawLine(42,381,794,381,line);
        if(d.rankedEligibleCandidates.isEmpty()){
            write(c,"لا يوجد مرشح مستوفٍ لشروط مصدر Next في هذا القياس.",
                42,405,9,MUTED,false);
        }else{
            int k=0;
            for(CoreContracts.Candidate candidate:d.rankedEligibleCandidates){
                if(k>=4)break;
                Set<String> distinct=new LinkedHashSet<>(candidate.sourceRoots);
                write(c,"UID "+candidate.canonicalChemicalUid,43,405+k*23,8.5f,INK,true);
                write(c,distinct.size()+" roots",182,405+k*23,8,MUTED,false);
                write(c,shortValue(candidate.evidenceSummary,94),
                    285,405+k*23,7.5f,MUTED,false);
                k++;
            }
            if(d.rankedEligibleCandidates.size()>4)
                write(c,"+"+(d.rankedEligibleCandidates.size()-4)+
                    " remaining UIDs recorded in the detailed Next Excel workbook",
                    42,507,8,MUTED,false);
        }
        write(c,"Snapshot SHA256 "+shortValue(n.snapshotSha256,30)+
                "  • Engine "+shortValue(n.engineVersion,25),
            42,533,7.9f,MUTED,false);
        write(c,"No replay, source extrapolation or occupational exposure decision during PDF export.",
            42,549,7.8f,MUTED,false);
    }
}
