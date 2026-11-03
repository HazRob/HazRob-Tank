package com.servoc.app.report;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.servoc.app.chemcore.nextintegration.NextDecisionStore;
import com.hazrob.tank.chemcore.next.CoreContracts;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Site-level V8 Next visualization. Original V7 page dimensions and slots are
 * unchanged. Every count is per independent, singly-approved FIELD SESSION.
 * No source curve vote is multiplied by the number of MQ channels.
 * The Next research hypotheses are NOT identified chemicals, ppm, G3,
 * occupational exposure values, or M4-certified hazard calls.
 */
public final class NextSiteEvidenceCharts {
    private NextSiteEvidenceCharts(){}
    private static final int INK=0xFF263747, MUTED=0xFF57687A,
        BLUE=0xFF296EA2, GREEN=0xFF247B60, BORDER=0xFFCFD8DC;
    public static String topUid(NextDecisionStore.Receipt receipt){
        if(receipt==null||receipt.originalDecision.rankedEligibleCandidates.isEmpty())
            return "UNKNOWN";
        return "UID "+
            receipt.originalDecision.rankedEligibleCandidates.get(0).canonicalChemicalUid;
    }
    public static boolean hasAnyNext(List<SiteSummaryReportExporter.SessionSummary> sessions){
        if(sessions==null)return false;
        for(var s:sessions)if(s!=null&&s.hasNext)return true;
        return false;
    }
    public static int independentRoots(List<SiteSummaryReportExporter.SessionSummary> sessions){
        Set<String> unique=new HashSet<>();
        if(sessions!=null)for(var session:sessions)
            if(session!=null&&session.next!=null)
                for(var e:session.next.originalDecision.evidence)
                    unique.add(e.sourceRootId);
        return unique.size();
    }
    private static Paint p(int color,float size,boolean bold){
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);paint.setTextSize(size);paint.setFakeBoldText(bold);
        return paint;
    }
    private static void t(Canvas c,String s,float x,float y,float size,int color,boolean bold){
        c.drawText(s==null?"":s,x,y,p(color,size,bold));
    }
    private static String shortLabel(String value,int length){
        if(value==null)return "—";
        return value.length()>length?value.substring(0,length-1)+"…":value;
    }
    private static void frame(Canvas c,RectF r,String label){
        Paint bg=p(0xFFF8FAFC,9,false);c.drawRoundRect(r,6,6,bg);
        bg.setColor(BORDER);bg.setStyle(Paint.Style.STROKE);bg.setStrokeWidth(.8f);
        c.drawRoundRect(r,6,6,bg);
        t(c,label,r.left+9,r.top+23,10,INK,true);
    }
    private static Map<String,Integer> sourceRootSessionCount(
        List<SiteSummaryReportExporter.SessionSummary> sessions){
        Map<String,Integer> count=new LinkedHashMap<>();
        for(var s:sessions){
            if(s==null||s.next==null)continue;
            Set<String> roots=new HashSet<>();
            for(var e:s.next.originalDecision.evidence)roots.add(e.sourceRootId);
            for(String key:roots)count.put(key,count.getOrDefault(key,0)+1);
        }
        return count;
    }
    private static Map<String,Integer> canonicalUidSessionCount(
        List<SiteSummaryReportExporter.SessionSummary> sessions){
        Map<String,Integer> count=new LinkedHashMap<>();
        for(var s:sessions){
            if(s==null||s.next==null)continue;
            Set<Long> unique=new HashSet<>();
            for(CoreContracts.Candidate c:s.next.originalDecision.rankedEligibleCandidates)
                unique.add(c.canonicalChemicalUid);
            for(Long uid:unique){
                String key="UID "+uid;
                count.put(key,count.getOrDefault(key,0)+1);
            }
        }
        return count;
    }
    private static Map<String,Integer> statusSessions(
        List<SiteSummaryReportExporter.SessionSummary> sessions){
        Map<String,Integer> count=new LinkedHashMap<>();
        for(var s:sessions){
            if(s==null||s.next==null)continue;
            String key=s.next.status.name();
            count.put(key,count.getOrDefault(key,0)+1);
        }
        return count;
    }
    private static void bars(Canvas c,RectF r,String title,
                             Map<String,Integer> counts,int tint){
        frame(c,r,title);
        if(counts.isEmpty()){
            t(c,"لا توجد مخرجات Next من قياسات معتمدة",r.left+12,
                r.top+70,9,MUTED,false);
            return;
        }
        List<Map.Entry<String,Integer>> rows=new ArrayList<>(counts.entrySet());
        rows.sort(Comparator.comparingInt((Map.Entry<String,Integer> e)->e.getValue())
            .reversed().thenComparing(Map.Entry::getKey));
        int max=0;for(var row:rows)max=Math.max(max,row.getValue());
        int display=Math.min(12,rows.size());
        float bodyTop=r.top+44;
        float gap=Math.min(30f,(r.height()-82f)/Math.max(1,display));
        for(int i=0;i<display;i++){
            var item=rows.get(i);
            float y=bodyTop+gap*i;
            t(c,shortLabel(item.getKey(),24),r.left+12,y+7,8,MUTED,false);
            float left=r.left+177,width=Math.max(8,r.width()-232);
            Paint pen=p(tint,9,false);
            c.drawRoundRect(new RectF(left,y-3,
                left+width*(item.getValue()/(float)Math.max(1,max)),y+10),2,2,pen);
            t(c,Integer.toString(item.getValue()),r.right-42,y+7,8,INK,true);
        }
        t(c,"عدد الجلسات المستقلة الحاملة للدليل — لا احتمالات",
            r.left+11,r.bottom-25,7.4f,MUTED,false);
        if(rows.size()>display)t(c,"+"+(rows.size()-display)+" مصادر إضافية",
            r.left+11,r.bottom-9,7.2f,MUTED,false);
    }
    public static void rootRecurrence(Canvas c,RectF r,
        List<SiteSummaryReportExporter.SessionSummary> sessions){
        bars(c,r,"ChemCore Next • جذور المصدر عبر الجلسات",
            sourceRootSessionCount(sessions),BLUE);
    }
    public static void candidateRecurrence(Canvas c,RectF r,
        List<SiteSummaryReportExporter.SessionSummary> sessions){
        bars(c,r,"ChemCore Next • تكرار UID البحثي",
            canonicalUidSessionCount(sessions),GREEN);
    }
    public static void decisionStatuses(Canvas c,RectF r,
        List<SiteSummaryReportExporter.SessionSummary> sessions){
        bars(c,r,"ChemCore Next • حالات القرار المحفوظة",
            statusSessions(sessions),BLUE);
    }
    public static void originalEvidenceTable(Canvas c,RectF r,
        List<SiteSummaryReportExporter.SessionSummary> sessions){
        frame(c,r,"ChemCore Next • مصدر كل جلسة");
        float y=r.top+44;
        int shown=0,total=0;
        for(var session:sessions){
            if(session==null||session.next==null)continue;
            total++;
            if(shown++>=12)continue;
            NextDecisionStore.Receipt next=session.next;
            Set<String> roots=new HashSet<>();
            for(var e:next.originalDecision.evidence)roots.add(e.sourceRootId);
            t(c,shortLabel(session.bundle.fieldSession.name,20),
                r.left+10,y,7.6f,INK,true);
            t(c,shortLabel(next.status.name(),17),r.left+152,y,
                7.0f,MUTED,false);
            t(c,"roots "+roots.size()+" / UIDs "+
                next.originalDecision.rankedEligibleCandidates.size(),
                r.left+265,y,7,MUTED,false);
            y+=Math.min(29,(r.height()-92)/Math.max(1,Math.min(12,sessions.size())));
        }
        if(total==0)t(c,"لا توجد جلسة مخزنة بقرار ChemCore Next",
            r.left+10,r.top+70,8,MUTED,false);
        t(c,"قرارات بحثية مستقلة • بدون PPM أو G3",
            r.left+10,r.bottom-32,7.4f,MUTED,false);
        if(total>12)t(c,"+"+(total-12)+" جلسة أخرى في مصفوفة الجلسات",
            r.left+10,r.bottom-12,7.2f,MUTED,false);
    }
}
