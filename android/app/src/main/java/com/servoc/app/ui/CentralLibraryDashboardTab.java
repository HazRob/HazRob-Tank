package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.servoc.app.BuildConfig;
import com.servoc.app.ReleaseAudit;
import com.servoc.app.chemical.ChemicalExperimentStatusRepository;
import com.servoc.app.chemical.ChemicalReferenceSyncV6;
import com.servoc.app.chemcore.ChemCoreMolecularSeedImporter;
import com.servoc.app.chemcore.library.ChemCoreLibraryManager;
import com.servoc.app.chemcore.library.ChemCoreLibraryManifest;
import com.servoc.app.chemcore.library.ChemCoreLibraryStore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

/** Developer dashboard for the canonical HazRob scientific library and its distribution channel. */
public final class CentralLibraryDashboardTab {
    private final Context context;
    private final HazRobHost host;
    private final ChemCoreLibraryManager manager;
    private final ChemicalExperimentStatusRepository statusRepo;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final LinearLayout root,bar;
    private final FrameLayout page;
    private final View dashboardView,distributionView,platformsView,changelogView;
    private final Button[] tabs=new Button[4];
    private final TextView versionText,coverageText,distributionText,updateTelemetryText,operationText,platformText,changelogText;
    private int selected=0;

    public CentralLibraryDashboardTab(Context context,HazRobHost host){
        this.context=context.getApplicationContext();this.host=host;this.manager=new ChemCoreLibraryManager(context);this.statusRepo=new ChemicalExperimentStatusRepository(host.database());
        root=UiKit.vbox(context);root.setBackgroundColor(0xFFF7F8FA);root.setPadding(UiKit.dp(context,4),UiKit.dp(context,4),UiKit.dp(context,4),UiKit.dp(context,8));
        bar=UiKit.hbox(context);
        String[] names={"لوحة المكتبة","التوزيع","المنصات","السجل"};
        for(int i=0;i<names.length;i++){
            final int x=i;tabs[i]=UiKit.button(context,names[i]);
            tabs[i].setTextSize(13);tabs[i].setMinWidth(UiKit.dp(context,105));
            tabs[i].setOnClickListener(v->show(x));
            LinearLayout.LayoutParams buttonLayout=new LinearLayout.LayoutParams(-2,-2);
            buttonLayout.setMargins(UiKit.dp(context,2),0,UiKit.dp(context,2),0);
            bar.addView(tabs[i],buttonLayout);
        }
        HorizontalScrollView strip=new HorizontalScrollView(context);
        strip.setHorizontalScrollBarEnabled(false);
        strip.addView(bar,new HorizontalScrollView.LayoutParams(-2,-2));
        root.addView(strip,UiKit.full(context));page=new FrameLayout(context);root.addView(page,new LinearLayout.LayoutParams(-1,0,1f));

        ScrollView dashboard=scroll(context);LinearLayout d=(LinearLayout)dashboard.getChildAt(0);d.addView(UiKit.title(context,"المكتبة المركزية • HazRob"),UiKit.full(context));TextView canon=UiKit.small(context,"المصدر العلمي المرجعي: HazRob/build-harness • التحديث للمستخدم قراءة فقط • R0 معايرة جهاز محلية ولا تدخل في المعرفة المركزية.");canon.setTextColor(0xFF167443);canon.setBackgroundColor(0xFFE8F5E9);d.addView(canon,UiKit.full(context));LinearLayout vcard=UiKit.card(context);vcard.addView(UiKit.title(context,"الإصدار والمراجعات"),UiKit.full(context));versionText=box(context);vcard.addView(versionText,UiKit.full(context));d.addView(vcard,UiKit.full(context));LinearLayout ccard=UiKit.card(context);ccard.addView(UiKit.title(context,"تغطية المكتبة"),UiKit.full(context));coverageText=box(context);ccard.addView(coverageText,UiKit.full(context));d.addView(ccard,UiKit.full(context));dashboardView=dashboard;

        ScrollView distribution=scroll(context);LinearLayout u=(LinearLayout)distribution.getChildAt(0);u.addView(UiKit.title(context,"التحديث والتوزيع"),UiKit.full(context));distributionText=box(context);u.addView(distributionText,UiKit.full(context));Button check=UiKit.button(context,"استعلام GitHub عن إمكانات المواد الجديدة");check.setOnClickListener(v->checkDistribution());u.addView(check,UiKit.full(context));Button rollback=UiKit.button(context,"الرجوع للنسخة السابقة المتحققة");rollback.setOnClickListener(v->rollback());u.addView(rollback,UiKit.full(context));updateTelemetryText=box(context);u.addView(updateTelemetryText,UiKit.full(context));operationText=UiKit.small(context,"جاهز");operationText.setBackgroundColor(0xFFF1F3F6);u.addView(operationText,UiKit.full(context));distributionView=distribution;

        ScrollView platforms=scroll(context);LinearLayout p=(LinearLayout)platforms.getChildAt(0);p.addView(UiKit.title(context,"المنصات والبنية السحابية"),UiKit.full(context));TextView note=UiKit.small(context,"هذه الصفحة للقراءة فقط. مصدر المكتبة هو البيانات المركزية المنشورة؛ لا تحتاج عملية المزامنة إلى إعادة تأهيل.");note.setTextColor(0xFF985000);p.addView(note,UiKit.full(context));platformText=box(context);p.addView(platformText,UiKit.full(context));Button refresh=UiKit.button(context,"تحديث العرض المحلي");refresh.setOnClickListener(v->refresh());p.addView(refresh,UiKit.full(context));platformsView=platforms;

        ScrollView changes=scroll(context);LinearLayout ch=(LinearLayout)changes.getChildAt(0);ch.addView(UiKit.title(context,"سجل تغييرات المكتبة"),UiKit.full(context));changelogText=box(context);changelogText.setTextIsSelectable(true);ch.addView(changelogText,UiKit.full(context));changelogView=changes;

        show(0);
    }

    public View view(){return root;}

    private ScrollView scroll(Context c){ScrollView s=new ScrollView(c);s.setFillViewport(true);LinearLayout x=UiKit.vbox(c);x.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,14));s.addView(x);return s;}
    private static TextView box(Context c){TextView v=UiKit.text(c,"—");v.setBackgroundColor(0xFFF1F3F6);return v;}

    private void show(int index){selected=Math.max(0,Math.min(3,index));page.removeAllViews();View[] views={dashboardView,distributionView,platformsView,changelogView};page.addView(views[selected],new FrameLayout.LayoutParams(-1,-1));for(int i=0;i<tabs.length;i++){tabs[i].setTextColor(Color.BLACK);tabs[i].setBackground(UiKit.outline(context,i==selected?0xFFF6B83D:0xFFF1F3F6,0xFF8994A3));}refresh();}

    public void refresh(){
        ChemCoreLibraryManager.Status s=manager.status();
        String bootstrap=host.database().getMeta("tank_v4_stable_bootstrap_result","not_attempted");
        String bootstrapVersion=host.database().getMeta("tank_v4_stable_bootstrap_version","");
        String bootstrapActive=host.database().getMeta("tank_v4_stable_bootstrap_active","false");
        versionText.setText("تطبيق HazRob Tank: "+ReleaseAudit.VERSION+" • التحديث التشخيصي غير معتمد بعد\\nإصدار المكتبة الفعال: "+blank(s.version)+"\\nالمصدر: "+blank(s.sourceLabel)+
                "\\nSource commit: "+shortId(s.sourceCommit)+"\\nData revision: "+shortId(s.dataRevision)+
                "\\nEvidence revision: "+shortId(s.evidenceRevision)+"\\nSHA-256: "+shortId(s.sha256)+
                "\\nفحص Stable المضمنة: "+bootstrap+
                "\\nالإصدار المثبت: "+blank(bootstrapVersion)+" • مفعلة أو محفوظة: "+bootstrapActive+
                "\\nالحزمة المضمنة بحثية للفحص الأولي فقط؛ لا تحتوي على معايرات M4 مرجعية.");

        int total=host.database().chemicalCount();
        int covered=statusRepo.countTestedMaterials();
        int noEvidence=statusRepo.countUntestedNoEvidenceMaterials();
        int local=statusRepo.countLocalExperimentMaterials();
        int central=statusRepo.countCentralEvidenceMaterials();
        int fpAny=statusRepo.countFingerprintMaterials();
        int fpReady=statusRepo.countReadyFingerprintMaterials();
        int ppm=statusRepo.countPpmMaterials();
        int m4=statusRepo.countM4Materials();
        int exposure=statusRepo.countExposureLimitMaterials();
        coverageText.setText(
                "إجمالي المواد: "+total+"\n"+
                "لها دليل مركزي أو تجربة محلية: "+covered+" ("+pct(covered,total)+")\n"+
                "بدون دليل أو تجربة بعد: "+noEvidence+" ("+pct(noEvidence,total)+")\n"+
                "مواد لها دليل مركزي فعال: "+central+" ("+pct(central,total)+")\n"+
                "مواد جُرّبت على هذا الجهاز: "+local+" ("+pct(local,total)+")\n"+
                "بصمة/دليل هوية — أي مستوى: "+fpAny+" ("+pct(fpAny,total)+")\n"+
                "بصمة READY: "+fpReady+" ("+pct(fpReady,total)+")\n"+
                "بيانات PPM/منحنى — تشمل Transfer: "+ppm+" ("+pct(ppm,total)+")\n"+
                "M4 مرجعي قابل للتقرير: "+m4+" ("+pct(m4,total)+")\n"+
                "منحنيات منشورة ضمن تحديثات السجلات: "+statusRepo.countRemoteResearchCurveMaterials()+"\n"+
                "M4 منشور في المكتبة المركزية (منفصل عن اعتماد الجهاز): "+statusRepo.countRemoteResearchM4Materials()+"\n"+
                "مواد لها حدود تعرض: "+exposure+" ("+pct(exposure,total)+")\n"+
                "التجارب المعتمدة محليًا: "+host.database().approvedCount()+"\n"+
                "المراجع المشتقة محليًا: "+host.database().referenceArtifactCount()+"\n"+
                "كل منحنيات المعايرة المحلية: "+host.database().calibrationCurveCount()+"\n"+
                "Evidence revision: "+blank(statusRepo.libraryRevision()));

        String endpoint=manager.manifestUrl()==null?"":manager.manifestUrl().trim();
        distributionText.setText("مصدر التطوير العلمي المركزي: HazRob/build-harness (خاص)\nمزامنة محتويات المواد من GitHub (حسب UID/CAS والتغيير): "+ChemicalReferenceSyncV6.DEFAULT_INDEX+"\nآخر مزامنة: "+host.database().getMeta("v6_reference_sync_result","لم تنفذ")+
                "\nقناة استعادة حزمة Stable كاملة — للمطور فقط: "+(endpoint.isEmpty()?"غير مهيأة":endpoint)+
                "\nالتأهيل يتم في المكتبة المركزية قبل النشر، ولا يعاد عند الاستعلام.");
        updateTelemetryText.setText(formatUpdateTelemetry(host.database().getMeta("chemcore_update_telemetry_json","")));
        platformText.setText(formatPlatforms(host.database().getMeta("hazrob_platform_metrics_json","")));
        changelogText.setText(formatChangelog(host.database().getMeta("chemcore_changelog_summary",""),s));
    }

    private void checkDistribution(){
        operationText.setText("جارٍ الاستعلام عن حالة السجلات العلمية...");
        host.io().execute(()->{
            try{
                ChemicalReferenceSyncV6.Result result=ChemicalReferenceSyncV6.refresh(
                    host.database(),0,25,
                    ChemCoreLibraryManager.measurementOrAnalysisActive(host.latestDiagnostics()));
                host.database().putMeta("v6_reference_sync_result",result.summary());
                main.post(()->{
                    operationText.setText("GitHub • تمت مزامنة "+result.applied+
                        " سجلات • بدون تغيير "+result.unchanged+
                        " • المراجعة "+result.revision);
                    refresh();
                    if(result.applied>0)host.refreshAllTabs();
                });
            }catch(Exception e){
                main.post(()->operationText.setText(
                    "تعذر الاستعلام عن محتويات المكتبة المنشورة: "+safe(e)));
            }
        });
    }

    private void rollback(){
        operationText.setText("جارٍ التحقق والرجوع للمكتبة السابقة...");
        host.io().execute(()->{
            String summary;
            boolean installed=false;
            try{
                boolean active=ChemCoreLibraryManager.measurementOrAnalysisActive(host.latestDiagnostics());
                ChemCoreLibraryStore.InstallResult r=manager.rollback(active,ReleaseAudit.VERSION);
                installed=r.installed;
                if(installed)ChemCoreMolecularSeedImporter.importBestAvailable(context,host.database());
                summary=installed?"تم الرجوع إلى "+r.version:"تعذر الرجوع: "+r.reason;
            }catch(Exception ex){
                summary="فشل الاسترجاع دون تغيير المكتبة الحالية: "+safe(ex);
            }
            final String result=summary;
            final boolean success=installed;
            main.post(()->{
                operationText.setText(result);
                if(success)host.showToast("تم استرجاع مكتبة ChemCore السابقة");
                refresh();
            });
        });
    }

    private static String formatUpdateTelemetry(String raw){try{JSONObject j=new JSONObject(raw);return "إحصاءات التحديثات\nChecks: "+j.optInt("checks",0)+" • Downloads: "+j.optInt("downloads",0)+"\nInstall success: "+j.optInt("install_success",0)+" • Fail: "+j.optInt("install_fail",0)+" • Rollback: "+j.optInt("rollback",0)+"\nActive installations: "+j.optInt("active_installations",0)+"\nWindow: "+j.optString("window","—");}catch(Exception e){return "إحصاءات التحديثات: غير متاحة بعد. سيتم ملؤها من telemetry مجهولة الهوية بعد تفعيل قناة التوزيع العامة.";}}
    private static String formatPlatforms(String raw){try{JSONObject j=new JSONObject(raw);JSONArray a=j.optJSONArray("services");if(a==null||a.length()==0)throw new IllegalArgumentException();StringBuilder s=new StringBuilder();for(int i=0;i<a.length();i++){JSONObject q=a.optJSONObject(i);if(q==null)continue;if(s.length()>0)s.append('\n');s.append(q.optString("name","Service")).append(": ").append(q.optString("status","—"));String used=q.optString("used","");String limit=q.optString("limit","");String remaining=q.optString("remaining","");if(!used.isEmpty()||!limit.isEmpty())s.append(" • ").append(used).append(" / ").append(limit);if(!remaining.isEmpty())s.append(" • متبقٍ ").append(remaining);String reset=q.optString("reset_at","");if(!reset.isEmpty())s.append(" • reset ").append(reset);}return s.toString();}catch(Exception e){return "Railway: —\nGitHub: —\nMQTT: —\nCloudflare: —\nRender: —\nلم تتم مزامنة أرصدة المنصات بعد.";}}
    private static String formatChangelog(String raw,ChemCoreLibraryManager.Status s){if(raw!=null&&!raw.trim().isEmpty())return raw.trim();return "لا يوجد Changelog مركزي مستورد لهذه النسخة بعد.\nالإصدار الفعال: "+blank(s.version)+"\nData revision: "+shortId(s.dataRevision)+"\nEvidence revision: "+shortId(s.evidenceRevision);}
    private static String pct(int n,int total){return total<=0?"0%":String.format(Locale.US,"%.1f%%",100.0*n/total);}
    private static String blank(String s){return s==null||s.trim().isEmpty()?"—":s;}
    private static String shortId(String s){s=blank(s);return s.length()<=24?s:s.substring(0,24)+"…";}
    private static String safe(Exception e){String s=e==null?"unknown":e.getMessage();return s==null||s.trim().isEmpty()?e.getClass().getSimpleName():s;}
}
