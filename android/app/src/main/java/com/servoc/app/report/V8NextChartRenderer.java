package com.servoc.app.report;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.hazrob.tank.chemcore.next.CoreContracts;
import com.servoc.app.chemcore.nextintegration.NextDecisionStore;
import com.servoc.app.data.SessionSeries;
import com.servoc.app.hardware.HazRobHardwareProfile;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One evidence-faithful renderer shared by field PDF and ReportsTab charts.
 * Every number is derived from the immutable Next receipt (same session and
 * SHA) or its separately identified, original SessionSeries.
 *
 * Original V7 six-slot geometry is unchanged. Never invent a posterior,
 * fitted source curve, 144-screening score, ppm, or safety/LEL alarm.
 */
public final class V8NextChartRenderer {
    private V8NextChartRenderer(){}
    public static final String SCHEMA="hazrob.tank.v8.next.scientific_visualization/1";
    private static final int INK=0xFF243647, MUTED=0xFF526576, GREEN=0xFF187554;
    private static final int BLUE=0xFF2F6EAD, BACK=0xFFF7F9FB, BORDER=0xFFD2DBE4;
    private static final int AMBER=0xFFC67C23;
    public static final int CANDIDATES=0, SOURCE_ROOTS=1, NORMALIZED_MQ=2,
        SENSOR_EVIDENCE=3, POLARITY=4, DECISION=5, SOURCE_COVERAGE=6,
        RS_SOURCE=7, ADC_RAW=8, SOURCE_SENSOR=9, ENVIRONMENT=10, QUALITY=11;
    private static Paint paint(int color,float size,boolean bold){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);p.setTextSize(size);p.setFakeBoldText(bold);
        return p;
    }
    private static void t(Canvas c,String text,float x,float y,float size,int color,boolean bold){
        c.drawText(text==null?"":text,x,y,paint(color,size,bold));
    }
    private static String shortText(String s,int max){
        if(s==null)return "—";
        if(s.length()<=max)return s;
        return s.substring(0,Math.max(1,max-1))+"…";
    }
    private static void panel(Canvas c,RectF r,String title){
        Paint bg=paint(BACK,9,false);
        c.drawRoundRect(r,6,6,bg);
        bg.setColor(BORDER);bg.setStyle(Paint.Style.STROKE);bg.setStrokeWidth(0.8f);
        c.drawRoundRect(r,6,6,bg);
        t(c,shortText(title,46),r.left+8,r.top+19,10.2f,INK,true);
    }
    private static void lines(Canvas c,RectF r,String... entries){
        float y=r.top+40f;float gap=Math.min(22f,Math.max(11f,(r.height()-52f)/Math.max(1,entries.length)));
        for(String value:entries){
            if(y>r.bottom-7)break;
            t(c,shortText(value,54),r.left+10,y,8.3f,MUTED,false);
            y+=gap;
        }
    }
    private static void bar(Canvas c,RectF r,String title,Map<String,? extends Number> data,int tint){
        panel(c,r,title);
        if(data.isEmpty()){lines(c,r,"لا توجد نقاط مسجلة لهذا النوع",
            "NOT_AVAILABLE — no synthetic value");return;}
        double max=0;
        for(Number value:data.values())
            if(value!=null&&Double.isFinite(value.doubleValue())&&value.doubleValue()>max)
                max=value.doubleValue();
        if(max<=0){lines(c,r,"لا توجد قيم عددية موجبة مؤهلة");return;}
        int maxItems=Math.max(1,Math.min(7,(int)((r.height()-48)/22)));
        float top=r.top+37;int shown=0;
        for(Map.Entry<String,? extends Number> e:data.entrySet()){
            if(shown++>=maxItems)break;
            double value=e.getValue()==null?Double.NaN:e.getValue().doubleValue();
            if(!Double.isFinite(value)||value<0)continue;
            float y=top+(shown-1)*22;
            t(c,shortText(e.getKey(),14),r.left+9,y+8,7.5f,INK,false);
            float left=r.left+85, width=Math.max(2,r.width()-148);
            Paint p=paint(tint,8,false);
            float filled=(float)(width*Math.min(1,Math.max(0,value/max)));
            c.drawRoundRect(new RectF(left,y-2,left+filled,y+10),2,2,p);
            t(c,String.format(Locale.US,"%.3g",value),
                r.right-57,y+8,7.3f,INK,true);
        }
    }
    private static Map<String,Integer> candidateRoots(CoreContracts.Result d){
        Map<String,Integer> result=new LinkedHashMap<>();
        for(CoreContracts.Candidate c:d.rankedEligibleCandidates)
            result.put("UID "+c.canonicalChemicalUid,c.sourceRoots.size());
        return result;
    }
    private static Map<String,Integer> rootEvidence(CoreContracts.Result d){
        Map<String,Integer> result=new LinkedHashMap<>();
        for(CoreContracts.EvidenceNode e:d.evidence){
            if(e.sourceRootId.isBlank())continue;
            result.put(e.sourceRootId,result.getOrDefault(e.sourceRootId,0)+1);
        }
        return result;
    }
    private static Map<String,Integer> sensorEvidence(CoreContracts.Result d){
        Map<String,Integer> result=new LinkedHashMap<>();
        for(CoreContracts.EvidenceNode e:d.evidence){
            result.put(e.sensorUid,result.getOrDefault(e.sensorUid,0)+1);
        }
        return result;
    }
    private static Map<String,Integer> polarity(CoreContracts.Result d){
        Map<String,Integer> result=new LinkedHashMap<>();
        for(CoreContracts.EvidenceNode e:d.evidence)
            result.put(e.polarity.name(),result.getOrDefault(e.polarity.name(),0)+1);
        return result;
    }
    private static Map<String,Integer> sourceCoverage(CoreContracts.Result d){
        Map<String,Integer> result=new LinkedHashMap<>();
        for(CoreContracts.Candidate x:d.rankedEligibleCandidates)
            for(String root:x.sourceRoots)
                result.put(root,result.getOrDefault(root,0)+1);
        return result;
    }
    private static Map<String,Integer> sourceSensorPairs(CoreContracts.Result d){
        Map<String,Integer> result=new LinkedHashMap<>();
        for(CoreContracts.EvidenceNode e:d.evidence){
            String key=shortText(e.sourceRootId,8)+" / "+shortText(e.sensorUid,8);
            result.put(key,result.getOrDefault(key,0)+1);
        }
        return result;
    }
    private static Map<String,Double> measuredRs(CoreContracts.Snapshot s){
        Map<String,Double> result=new LinkedHashMap<>();
        for(CoreContracts.Reading x:s.readings)
            if(x.rsKohm!=null&&Double.isFinite(x.rsKohm)&&x.rsKohm>0)
                result.put(x.sensorUid,x.rsKohm);
        return result;
    }
    private static Map<String,Double> airRatios(CoreContracts.Snapshot s){
        Map<String,Double> result=new LinkedHashMap<>();
        for(CoreContracts.Reading x:s.readings){
            double v=x.airRatioOrNaN();
            if(Double.isFinite(v)&&v>0)result.put(x.sensorUid,v);
        }
        return result;
    }
    private static Map<String,Integer> adcCodes(CoreContracts.Snapshot s){
        Map<String,Integer> result=new LinkedHashMap<>();
        for(CoreContracts.Reading x:s.readings)
            if(x.rawAdc!=null)result.put(x.sensorUid,x.rawAdc);
        return result;
    }
    private static Map<String,Integer> quality(CoreContracts.Snapshot s){
        Map<String,Integer> result=new LinkedHashMap<>();
        for(CoreContracts.Reading x:s.readings){
            String key=x.quality.name();
            result.put(key,result.getOrDefault(key,0)+1);
        }
        return result;
    }
    /**
     * Actual original time evolution, not synthetic manufacturer curves.
     * Per MQ physical channel draw delta natural-log(Rs/Rs_at_first_valid_scan),
     * against the captured t_ms, deduplicating repeated firmware scan cycles.
     * The vertical axis is dimensionless, never ppm.
     */
    public static void drawActualMqKinetics(Canvas canvas,RectF r,SessionSeries.Series series){
        panel(canvas,r,"MQ • تغير ln(Rs/Rs0 الزمنية) • 60s");
        if(series==null||!series.usable()){
            lines(canvas,r,"لا توجد سلسلة عينات محفوظة","NOT_AVAILABLE: no actual MQ timeline");return;
        }
        Map<String,List<double[]>> traces=new LinkedHashMap<>();
        long minTime=Long.MAX_VALUE,maxTime=Long.MIN_VALUE;
        double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
        for(HazRobHardwareProfile.Channel sensor:HazRobHardwareProfile.MQ){
            List<double[]> pts=new ArrayList<>();
            double first=Double.NaN;
            long lastScan=Long.MIN_VALUE;
            for(SessionSeries.Sample sample:series.samples){
                JSONObject q=SessionSeries.sensor(sample,sensor.key);
                if(q==null||!q.optBoolean("ok",q.optBoolean("valid",false))||
                   !q.optBoolean("powered",true))continue;
                double rs=q.optDouble("rs",Double.NaN);
                if(!Double.isFinite(rs)||rs<=0)continue;
                if(q.has("scan_cycle")){
                    long cycle=q.optLong("scan_cycle",-1);
                    if(cycle<0||cycle==lastScan)continue;
                    lastScan=cycle;
                }
                if(!Double.isFinite(first))first=rs;
                double value=Math.log(rs/first);
                if(!Double.isFinite(value))continue;
                pts.add(new double[]{sample.tMs,value});
                minTime=Math.min(minTime,sample.tMs);
                maxTime=Math.max(maxTime,sample.tMs);
                min=Math.min(min,value);max=Math.max(max,value);
            }
            if(pts.size()>=2)traces.put(sensor.key,pts);
        }
        if(traces.isEmpty()||maxTime<=minTime){
            lines(canvas,r,"لا توجد دورتان أصليتان صحيحتان لكل حساس",
                "No duplicated / interpolated source scans");return;
        }
        min=Math.min(0,min);max=Math.max(0,max);
        if(max-min<0.001){min-=0.02;max+=0.02;}
        double span=max-min;
        RectF plot=new RectF(r.left+36,r.top+39,r.right-12,r.bottom-50);
        Paint axis=paint(0xFFA6B4C1,7,false);axis.setStrokeWidth(0.7f);
        canvas.drawLine(plot.left,plot.top,plot.left,plot.bottom,axis);
        canvas.drawLine(plot.left,plot.bottom,plot.right,plot.bottom,axis);
        float zero=plot.bottom-(float)((0-min)/span)*plot.height();
        if(zero>=plot.top&&zero<=plot.bottom)
            canvas.drawLine(plot.left,zero,plot.right,zero,axis);
        t(canvas,String.format(Locale.US,"%.2g",max),r.left+5,plot.top+8,7,MUTED,false);
        t(canvas,String.format(Locale.US,"%.2g",min),r.left+5,plot.bottom,7,MUTED,false);
        t(canvas,String.format(Locale.US,"%.1fs",minTime/1000.0),
            plot.left,plot.bottom+14,7,MUTED,false);
        t(canvas,String.format(Locale.US,"%.1fs",maxTime/1000.0),
            plot.right-31,plot.bottom+14,7,MUTED,false);
        int[] palette={GREEN,BLUE,0xFFC67C23,0xFF8A4BBB,0xFFC24F58,
                       0xFF137F8C,0xFF93663B,0xFF5F7837};
        int index=0;
        for(Map.Entry<String,List<double[]>> trace:traces.entrySet()){
            int tint=palette[index%palette.length];
            Paint pen=paint(tint,7,false);
            pen.setStyle(Paint.Style.STROKE);pen.setStrokeWidth(1.65f);
            float previousX=Float.NaN,previousY=Float.NaN;
            for(double[] point:trace.getValue()){
                float x=plot.left+(float)((point[0]-minTime)/(double)(maxTime-minTime))
                    *plot.width();
                float y=plot.bottom-(float)((point[1]-min)/span)*plot.height();
                if(Float.isFinite(previousX)&&Float.isFinite(previousY))
                    canvas.drawLine(previousX,previousY,x,y,pen);
                previousX=x;previousY=y;
            }
            float lx=r.left+12+(index%4)*59,ly=r.bottom-27+(index/4)*12;
            Paint key=paint(tint,7,false);key.setStrokeWidth(2);
            canvas.drawLine(lx,ly-3,lx+10,ly-3,key);
            t(canvas,trace.getKey().toUpperCase(Locale.US),lx+12,ly,6.4f,INK,false);
            index++;
        }
    }

    /** Coverage percentage uses EACH ORIGINAL saved sample, not the last
     * /diag frame. Repeated firmware scan-cycle IDs count only once.
     * This is a diagnostic visualization, NOT a calibrated gas result.
     */
    public static void drawActualMqCoverage(Canvas canvas,RectF r,SessionSeries.Series series){
        panel(canvas,r,"صلاحية قنوات MQ الزمنية • % من العينات");
        if(series==null||!series.usable()){
            lines(canvas,r,"سلسلة المصدر غير متاحة","No coverage percentage invented");return;
        }
        int total=series.samples.size();
        if(total<=0){lines(canvas,r,"لا توجد عينات");return;}
        int index=0;
        float top=r.top+37f;
        for(HazRobHardwareProfile.Channel sensor:HazRobHardwareProfile.MQ){
            int valid=0;long lastCycle=Long.MIN_VALUE;
            for(SessionSeries.Sample sample:series.samples){
                JSONObject q=SessionSeries.sensor(sample,sensor.key);
                if(q==null||!q.optBoolean("ok",q.optBoolean("valid",false))||
                   !q.optBoolean("powered",true))continue;
                double rs=q.optDouble("rs",Double.NaN);
                if(!Double.isFinite(rs)||rs<=0)continue;
                if(q.has("scan_cycle")){
                    long cycle=q.optLong("scan_cycle",-1);
                    if(cycle<0||cycle==lastCycle)continue;
                    lastCycle=cycle;
                }
                valid++;
            }
            float y=top+index*21.3f;
            t(canvas,sensor.key.toUpperCase(Locale.US),r.left+9,y+8,7.5f,INK,false);
            float left=r.left+67,width=Math.max(2,r.width()-122);
            Paint pen=paint(BLUE,8,false);
            canvas.drawRoundRect(new RectF(left,y-2,
                left+(float)valid/total*width,y+10),2,2,pen);
            t(canvas,String.format(Locale.US,"%.0f%%",valid*100.0/total),
                r.right-48,y+8,7.5f,INK,true);
            index++;
        }
        t(canvas,"Valid original scan rows / total recorded rows",
            r.left+9,r.bottom-9,6.9f,MUTED,false);
    }

    public static void requireSameSession(NextDecisionStore.Receipt receipt,String sessionId){
        if(receipt==null||sessionId==null||!receipt.sessionId.equals(sessionId))
            throw new IllegalStateException("NEXT_VISUAL_SOURCE_SESSION_MISMATCH");
        if(receipt.snapshotSha256==null||!receipt.snapshotSha256.matches("[0-9a-fA-F]{64}"))
            throw new IllegalStateException("NEXT_VISUAL_SNAPSHOT_SHA_MISSING");
        if(!receipt.sourceSnapshot.uid.equals(receipt.snapshotUid)||
           !receipt.originalDecision.snapshotHash.equals(receipt.snapshotSha256))
            throw new IllegalStateException("NEXT_VISUAL_DECISION_SOURCE_INCONSISTENT");
    }
    /** Each chart is semantically named for its actual persisted evidence. */
    public static void draw(Canvas canvas,RectF r,NextDecisionStore.Receipt receipt,
                            SessionSeries.Series series,int chart){
        requireSameSession(receipt,receipt.sessionId);
        if(series!=null&&!receipt.sessionId.equals(series.sessionId))
            throw new IllegalStateException("NEXT_VISUAL_TIME_SERIES_WRONG_SESSION");
        CoreContracts.Result d=receipt.originalDecision;
        CoreContracts.Snapshot snap=receipt.sourceSnapshot;
        switch(chart){
            case CANDIDATES:
                bar(canvas,r,"المرشحون • جذور أدلة مستقلة",candidateRoots(d),GREEN);break;
            case SOURCE_ROOTS:
                bar(canvas,r,"جذور المصادر المحفوظة",rootEvidence(d),BLUE);break;
            case NORMALIZED_MQ:
                bar(canvas,r,"البصمة Rs/R_air من مصدر الجهاز",airRatios(snap),GREEN);break;
            case SENSOR_EVIDENCE:
                bar(canvas,r,"الأدلة حسب حساس فعلي",sensorEvidence(d),BLUE);break;
            case POLARITY:
                bar(canvas,r,"قطبية أدلة المصادر",polarity(d),GREEN);break;
            case DECISION:
                panel(canvas,r,"قرار ChemCore Next • نسخة ثابتة");
                lines(canvas,r,"Status: "+d.status.name(),
                    "Candidate UIDs: "+d.rankedEligibleCandidates.size(),
                    "Evidence roots: "+rootEvidence(d).size(),
                    "Snapshot SHA: "+shortText(receipt.snapshotSha256,22),
                    "Decision UID: "+shortText(receipt.decisionUid,22),
                    "No certified ppm / Tank G3");break;
            case SOURCE_COVERAGE:
                drawActualMqKinetics(canvas,r,series);break;
            case RS_SOURCE:
                bar(canvas,r,"Rs kΩ أصلي • ليست قيمة غاز ppm",measuredRs(snap),GREEN);break;
            case ADC_RAW:
                bar(canvas,r,"شيفرات ADC الأصلية المحفوظة",adcCodes(snap),BLUE);break;
            case SOURCE_SENSOR:
                bar(canvas,r,"علاقة أصل الدليل بالحساس",sourceSensorPairs(d),GREEN);break;
            case ENVIRONMENT:
                if(series!=null&&series.usable())
                    AnalyticalCharts.mqEnvironmentMatrix(canvas,r,series);
                else{
                    panel(canvas,r,"البيئة المقاسة من جلسة المصدر");
                    lines(canvas,r,"T °C: "+(snap.temperatureC==null?"N/A":snap.temperatureC),
                        "RH %: "+(snap.relativeHumidityPercent==null?"N/A":snap.relativeHumidityPercent),
                        "Time series: NOT_AVAILABLE");
                }
                break;
            case QUALITY:
                if(series!=null&&series.usable())drawActualMqCoverage(canvas,r,series);
                else bar(canvas,r,"جودة قنوات MQ • المصدر النهائي",quality(snap),AMBER);
                break;
            default:throw new IllegalArgumentException("NEXT_CHART_KEY_UNSUPPORTED");
        }
    }
    public static void drawPdfDecisionSix(Canvas canvas,RectF[] six,
                                          NextDecisionStore.Receipt receipt,
                                          SessionSeries.Series series){
        if(six==null||six.length!=6)throw new IllegalArgumentException("SIX_CHART_SLOTS_REQUIRED");
        for(int i=0;i<6;i++)draw(canvas,six[i],receipt,series,i);
    }
    public static void drawPdfContextSix(Canvas canvas,RectF[] six,
                                         NextDecisionStore.Receipt receipt,
                                         SessionSeries.Series series){
        if(six==null||six.length!=6)throw new IllegalArgumentException("SIX_CHART_SLOTS_REQUIRED");
        for(int i=0;i<6;i++)draw(canvas,six[i],receipt,series,i+6);
    }
    public static String[] nextChartLabels(){
        return new String[]{
            "Next • المرشحون وعدد المصادر", "Next • جذور أدلة المصادر",
            "Next • البصمة Rs/R_air", "Next • دليل الحساسات",
            "Next • قطبية الدليل", "Next • القرار وبصمة المصدر",
            "Next • منحنى MQ الزمني الأصلي", "Next • Rs الأصلي kΩ",
            "Next • قيم ADC الخام", "Next • علاقة المصدر بالحساس",
            "Next • العلاقة مع البيئة", "Next • اكتمال MQ عبر الوقت"
        };
    }
}
