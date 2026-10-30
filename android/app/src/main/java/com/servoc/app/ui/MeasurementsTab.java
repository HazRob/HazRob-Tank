package com.servoc.app.ui;

import android.app.AlertDialog;
import android.database.Cursor;
import android.content.Context;
import android.widget.EditText;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.view.Gravity;
import android.widget.HorizontalScrollView;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.servoc.app.ReleaseAudit;
import com.servoc.app.analytics.AnalyticsSnapshot;
import com.servoc.app.calibration.AcceptedR0Normalizer;
import com.servoc.app.calibration.ManualR0Calibration;
import com.servoc.app.calibration.FieldR0Baseline;
import com.servoc.app.calibration.SqliteCalibrationProvider;
import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.chemical.GhsPictograms;
import com.servoc.app.chemical.TankChemicalEvidence;
import com.servoc.app.chemical.TankChemicalCapabilityReport;
import com.servoc.app.chemcore.ChemCoreAnalysisPresenter;
import com.servoc.app.chemcore.TankPpmEstimate;
import com.servoc.app.chemcore.TankExposureReferenceCatalog;
import com.servoc.app.chemcore.TankScreeningExposureWarning;
import com.servoc.app.chemcore.TankCandidateHazardBadges;
import com.servoc.app.chemcore.TankInstantAnalyzer;
import com.servoc.app.report.TankInstantCharts;
import com.servoc.app.chemcore.ChemCoreDecisionRepository;
import com.servoc.app.chemcore.ChemCoreLabQuality;
import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.field.FieldSessionRepository;
import com.servoc.app.hardware.HazRobHardwareProfile;
import com.servoc.app.measurement.InstantMqWarningEngine;
import com.servoc.app.measurement.LiveGasStateEngine;
import com.servoc.app.measurement.LiveSensorDiagnostic;
import com.servoc.app.measurement.SensorHealthEngine;
import com.servoc.app.measurements.PhysicalCatalog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** HazRob Tank V5 operator measurements: one ChemCore analysis plus candidate workbench. */
public final class MeasurementsTab {
    private static final int BG=0xFFF7F8FA,CARD=0xFFFFFFFF,CARD_ALT=0xFFE8EBEF;
    private static final int TEXT=0xFF101820,MUTED=0xFF4B5563,GOOD=0xFF167443,WARN=0xFF955000,BAD=0xFFB42318,INFO=0xFF1455A3;

    private final Context c;
    private final HazRobHost host;
    private final LinearLayout root;
    private final FrameLayout page;
    private final Button[] tabs=new Button[5];
    private int selected=1;
    private int analysisSection=0;
    private final PhysicalPane physical;
    private final LivePane live;
    private volatile TankInstantAnalyzer.Result currentOperation;
    private final AnalysisPane analysis;
    private final AnalysisResultsPane analysisResults;
    private final LinearLayout analysisShell;
    private final FrameLayout analysisPage;
    private final Button[] analysisTabs=new Button[2];
    private final ScrollView searchView;
    private final android.os.Handler searchHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable pendingSearch;
    private ListView chemicalRows;
    private TextView chemicalCount;
    private ArrayAdapter<String> chemicalAdapter;
    private final List<ChemicalRepository.Chemical> visibleChemicals=new java.util.ArrayList<>();
    private final java.util.Map<Long,java.util.List<String>> ghsByChemical=new java.util.HashMap<>();
    private Spinner familySpinner;
    private Spinner sortSpinner;
    private EditText chemicalSearch;
    private String chosenChemical="";
    private final ScrollView particlesView;
    private final FoodFreshnessTab foodFreshness;
    private final View[] views;

    public MeasurementsTab(Context c,HazRobHost host){
        this.c=c;this.host=host;
        GhsPictograms.bindContext(c);
        root=UiKit.vbox(c);root.setBackgroundColor(BG);
        root.setPadding(UiKit.dp(c,5),UiKit.dp(c,5),UiKit.dp(c,5),UiKit.dp(c,5));
        LinearLayout bar=UiKit.hbox(c);
        String[] names={"🔎 البحث والتتبع","📊 التحليل","🌡 الفيزيائية","◌ الجزيئات","🍎 نضارة الأغذية"};
        for(int i=0;i<names.length;i++){
            final int index=i;tabs[i]=UiKit.button(c,names[i]);
            tabs[i].setTextSize(11);
            tabs[i].setOnClickListener(v->show(index));
            bar.addView(tabs[i],new LinearLayout.LayoutParams(UiKit.dp(c,128),UiKit.dp(c,44)));
        }
        HorizontalScrollView topTabs=new HorizontalScrollView(c);
        topTabs.setHorizontalScrollBarEnabled(true);
        topTabs.addView(bar);
        root.addView(topTabs,UiKit.full(c));
        page=new FrameLayout(c);root.addView(page,new LinearLayout.LayoutParams(-1,0,1f));
        physical=new PhysicalPane();live=new LivePane();analysis=new AnalysisPane();analysisResults=new AnalysisResultsPane();
        // V5 keeps one computed operation and a single results workspace.
        // Charts are injected into the candidate workbench without duplicating its results.
        analysisResults.box.addView(analysis.box,UiKit.full(c));
        searchView=buildSearchLanding();
        particlesView=buildParticlesLanding();
        foodFreshness=new FoodFreshnessTab(c,host);
        analysisShell=UiKit.vbox(c);
        LinearLayout sections=UiKit.hbox(c);
        String[] sectionNames={"⏱ القياس الآني","🧪 ChemCore والمرشحون"};
        for(int i=0;i<analysisTabs.length;i++){
            final int index=i;analysisTabs[i]=UiKit.button(c,sectionNames[i]);
            analysisTabs[i].setTextSize(11);
            analysisTabs[i].setOnClickListener(v->showAnalysisSection(index));
            sections.addView(analysisTabs[i],UiKit.weight(1));
        }
        analysisShell.addView(sections,UiKit.full(c));
        analysisPage=new FrameLayout(c);
        analysisShell.addView(analysisPage,new LinearLayout.LayoutParams(-1,0,1f));
        views=new View[]{searchView,analysisShell,physical.view,particlesView,foodFreshness.view()};
        ensureUnifiedTrackingIdentity();
        showAnalysisSection(0);
        show(1);
    }

    /**
     * Migration of saved tracking preferences from the former 0-based
     * 144 legacy indices to stable chemicals.uid. The persisted integer must
     * never be silently reinterpreted as a master UID.
     * Historical tracking without unambiguous identity is cleared, not guessed.
     */
    private void ensureUnifiedTrackingIdentity(){
        android.content.SharedPreferences prefs=host.preferences();
        if(prefs.getInt("tracking_uid_semantics_version",0)>=12)return;
        int oldId=host.trackingTargetId();
        if(oldId>=0){
            ChemicalRepository repo=new ChemicalRepository(host.database());
            ChemicalRepository.Chemical asUid=repo.byUid(oldId);
            ChemicalRepository.Chemical asLegacy=oldId<144?repo.byLegacyId(oldId):null;
            String oldLabel=host.trackingTargetName();
            boolean uidMatch=asUid!=null&&oldLabel.equals(asUid.label());
            if(uidMatch){
                // The previous candidate-picker already saved master UIDs.
                host.setTrackingTarget(oldId,asUid.label());
            }else if(asLegacy!=null&&asLegacy.uid<=Integer.MAX_VALUE){
                host.setTrackingTarget((int)asLegacy.uid,asLegacy.label());
            }else{
                // Never point to a different material by ID reinterpretation.
                host.setTrackingTarget(-1,"");
            }
        }
        prefs.edit().putInt("tracking_uid_semantics_version",12).apply();
    }

    /** All new targeting is against the same master UID used by the lab. */
    private void setUnifiedTrackingTarget(long uid,String name){
        if(uid>Integer.MAX_VALUE||uid< -1){
            host.showToast("معرف المادة خارج النطاق المدعوم");
            return;
        }
        host.setTrackingTarget((int)uid,name);
        host.preferences().edit().putInt("tracking_uid_semantics_version",12).apply();
    }


    /** Material classification icons; NOT a detection / alarm badge. */
    private View ghsDetailTitle(ChemicalRepository.Chemical chemical,String title){
        LinearLayout row=UiKit.hbox(c);
        row.setPadding(UiKit.dp(c,14),UiKit.dp(c,10),UiKit.dp(c,10),UiKit.dp(c,6));
        android.widget.TextView label=UiKit.title(c,title);
        label.setTextColor(Color.BLACK);
        row.addView(label,new LinearLayout.LayoutParams(0,-2,1f));
        GhsPictograms.addImages(row,c,ghsByChemical.get(chemical.uid),38,4);
        return row;
    }

    private ScrollView buildSearchLanding(){
        ScrollView scroll=new ScrollView(c);
        scroll.setVerticalScrollBarEnabled(true);
        scroll.setScrollbarFadingEnabled(false);
        scroll.setFillViewport(true);
        LinearLayout content=UiKit.vbox(c);
        content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        content.setPadding(UiKit.dp(c,10),UiKit.dp(c,12),UiKit.dp(c,10),UiKit.dp(c,18));
        scroll.addView(content);
        content.addView(UiKit.title(c,"🔎 البحث والتتبع"),UiKit.full(c));
        TextView ghsLegend=UiKit.small(c,
            "♦ رموز GHS تعرّف أخطار المادة من مصدر موثق، وليست دليلًا على اكتشافها.");
        ghsLegend.setTextColor(0xFF4A5568);
        content.addView(ghsLegend,UiKit.full(c));
        content.addView(card("اختر مادة من المكتبة أو مرشحي ChemCore. البصمات والمنحنيات والمعادلات والخوارزميات المتاحة تظل فعالة للحساب والتقدير؛ اضغط على المادة لمعرفة الأساليب المتوفرة. تختلف درجة الاعتماد وفق مصدر البيانات."),UiKit.full(c));
        chemicalSearch=UiKit.input(c,"");
        chemicalSearch.setHint("بحث بالاسم أو رقم CAS أو الصيغة");
        chemicalSearch.setSingleLine(true);
        content.addView(chemicalSearch,UiKit.full(c));
        familySpinner=new Spinner(c);
        UiKit.outlinedSpinner(familySpinner);
        familySpinner.setAdapter(UiKit.lightOptions(c,
            new String[]{"جميع العائلات","الكحولات","الهيدروكربونات","المركبات الكبريتية","المركبات النيتروجينية","أخرى"}));
        content.addView(familySpinner,UiKit.full(c));
        sortSpinner=new Spinner(c);
        UiKit.outlinedSpinner(sortSpinner);
        sortSpinner.setAdapter(UiKit.lightOptions(c,
            new String[]{"الترتيب: الاسم أ–ي","الترتيب: الاسم ي–أ","الترتيب: رقم CAS"}));
        content.addView(sortSpinner,UiKit.full(c));
        chemicalCount=card("المواد المطابقة: —");
        content.addView(chemicalCount,UiKit.full(c));
        HorizontalScrollView horizontal=new HorizontalScrollView(c);
        horizontal.setHorizontalScrollBarEnabled(true);
        horizontal.setScrollbarFadingEnabled(false);
        LinearLayout tableBox=UiKit.vbox(c);
        tableBox.setMinimumWidth(UiKit.dp(c,650));
        TextView header=card("المادة الكيميائية                 | رقم CAS                 | العائلة الكيميائية");
        header.setBackgroundColor(0xFFFFD36A);
        tableBox.addView(header,UiKit.full(c));
        chemicalRows=new ListView(c);
        chemicalRows.setVerticalScrollBarEnabled(true);
        chemicalRows.setScrollbarFadingEnabled(false);
        chemicalRows.setFastScrollEnabled(true);
        chemicalRows.setDividerHeight(UiKit.dp(c,1));
        chemicalRows.setBackground(UiKit.outline(c,Color.WHITE,0xFF8994A3));
        tableBox.addView(chemicalRows,new LinearLayout.LayoutParams(-1,UiKit.dp(c,410)));
        horizontal.addView(tableBox,new android.widget.FrameLayout.LayoutParams(-2,-2));
        content.addView(horizontal,UiKit.full(c));
        chemicalAdapter=new ArrayAdapter<String>(c,android.R.layout.simple_list_item_1,new java.util.ArrayList<String>()){
            @Override public View getView(int position,View reused,android.view.ViewGroup parent){
                View label=super.getView(position,null,parent);
                UiKit.ensureLightRow(label);
                LinearLayout wrapper=new LinearLayout(c);
                wrapper.setGravity(android.view.Gravity.CENTER_VERTICAL);
                wrapper.setBackgroundColor(Color.WHITE);
                wrapper.addView(label,new LinearLayout.LayoutParams(0,-2,1f));
                if(position>=0&&position<visibleChemicals.size()){
                    ChemicalRepository.Chemical chemical=visibleChemicals.get(position);
                    java.util.List<String> icons=ghsByChemical.get(chemical.uid);
                    GhsPictograms.addImages(wrapper,c,icons,26,4);
                }
                return wrapper;
            }
        };
        chemicalRows.setAdapter(chemicalAdapter);
        chemicalRows.setOnItemClickListener((parent,view,position,id)->{
            if(position<0||position>=visibleChemicals.size())return;
            final ChemicalRepository.Chemical chemical=visibleChemicals.get(position);
            final String label=(chemical.arabicName!=null&&!chemical.arabicName.isEmpty()
                ?chemical.arabicName:chemical.name);
            final long masterUid=chemical.uid;
            // The verified Stable snapshot is read from a file: never block UI.
            host.io().execute(()->{
                final String description;
                try{description=TankChemicalCapabilityReport.describe(c,host.database(),chemical);}
                catch(Exception ex){
                    android.util.Log.e("TankChemicalCapabilities",
                        "Cannot resolve active capabilities for chemical "+chemical.uid,ex);
                    scroll.post(()->host.showToast("تعذر فحص الأدلة؛ لم تتغير المكتبة"));
                    return;
                }
                scroll.post(()->{
                    new AlertDialog.Builder(c).setCustomTitle(ghsDetailTitle(chemical,label))
                        .setMessage("CAS: "+new ChemicalRepository(host.database()).catalogCasLabel(chemical)+
                            "\nالفئة: "+(chemical.chemicalClass==null?"—":chemical.chemicalClass)+
                            "\nالاسم: "+chemical.name+
                            "\nالصيغة: "+(chemical.formula==null?"—":chemical.formula)+
                            "\nالمصدر: "+(chemical.source==null?"—":chemical.source)+
                            "\n\nالإمكانات المتاحة ومصدرها:\n"+description+
                            "\n\nالتتبع: "+(masterUid<=Integer.MAX_VALUE?"اختيار المادة من الفهرس الموحد":"معرف غير مدعوم"))
                        .setPositiveButton(masterUid<=Integer.MAX_VALUE?"تتبع المادة":"إغلاق",
                            (d,w)->{if(masterUid<=Integer.MAX_VALUE){
                                chosenChemical=label;setUnifiedTrackingTarget(masterUid,chemical.label());
                            }})
                        .show();
                });
            });
        });
        chemicalSearch.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence x,int st,int count,int after){}
            public void onTextChanged(CharSequence x,int st,int before,int count){
                if(pendingSearch!=null)searchHandler.removeCallbacks(pendingSearch);
                pendingSearch=()->refreshChemicalSearch();
                searchHandler.postDelayed(pendingSearch,250);
            }
            public void afterTextChanged(Editable x){}
        });
        familySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> p,View v,int position,long id){refreshChemicalSearch();}
            public void onNothingSelected(AdapterView<?> p){}
        });
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> p,View v,int position,long id){refreshChemicalSearch();}
            public void onNothingSelected(AdapterView<?> p){}
        });
        Button candidates=UiKit.button(c,"🏅 أفضل مرشحي ChemCore — اختر مادة للتتبع");
        candidates.setOnClickListener(v->{show(1);showAnalysisSection(1);});
        content.addView(candidates,UiKit.full(c));
        refreshChemicalSearch();
        return scroll;
    }

    private boolean matchesFamily(String family,String group){
        if(group==null||group.trim().isEmpty())return family.equals("أخرى");
        String v=group.toLowerCase(Locale.ROOT);
        if(family.equals("الكحولات"))return v.contains("alcohol")||v.contains("كحول");
        if(family.equals("الهيدروكربونات"))return v.contains("hydrocarbon")||v.contains("alkane")||v.contains("aromatic")||v.contains("هيدروكربون");
        if(family.equals("المركبات الكبريتية"))return v.contains("sulfur")||v.contains("sulph")||v.contains("كبريت");
        if(family.equals("المركبات النيتروجينية"))return v.contains("nitrogen")||v.contains("amine")||v.contains("نيتروجين");
        return family.equals("أخرى")&&!matchesFamily("الكحولات",group)&&!matchesFamily("الهيدروكربونات",group)&&!matchesFamily("المركبات الكبريتية",group)&&!matchesFamily("المركبات النيتروجينية",group);
    }

    private void refreshChemicalSearch(){
        if(chemicalRows==null||chemicalSearch==null||familySpinner==null||sortSpinner==null)return;
        String query=chemicalSearch.getText().toString().trim();
        String family=String.valueOf(familySpinner.getSelectedItem());
        try{
            ChemicalRepository repo=new ChemicalRepository(host.database());
            List<ChemicalRepository.Chemical> results=repo.search(query,5000);
            ghsByChemical.clear();
            ghsByChemical.putAll(GhsPictograms.allQualified(host.database()));
            visibleChemicals.clear();
            java.util.ArrayList<String> labels=new java.util.ArrayList<>();
            for(ChemicalRepository.Chemical chemical:results){
                if(!"جميع العائلات".equals(family)&&!matchesFamily(family,chemical.chemicalClass))continue;
                visibleChemicals.add(chemical);
                String label=chemical.arabicName!=null&&!chemical.arabicName.isEmpty()?chemical.arabicName:chemical.name;
                labels.add("🧪 "+label+"    |    "+repo.catalogCasLabel(chemical)+"    |    "+(chemical.chemicalClass==null?"—":chemical.chemicalClass));
            }
            int sort=sortSpinner.getSelectedItemPosition();
            if(sort>=0){
                java.util.Collections.sort(visibleChemicals,(a,b)->{
                    String left=sort==2?(a.cas==null?"":a.cas):(a.arabicName!=null&&!a.arabicName.isEmpty()?a.arabicName:a.name);
                    String right=sort==2?(b.cas==null?"":b.cas):(b.arabicName!=null&&!b.arabicName.isEmpty()?b.arabicName:b.name);
                    int cmp=String.CASE_INSENSITIVE_ORDER.compare(left==null?"":left,right==null?"":right);
                    return sort==1?-cmp:cmp;
                });
                labels.clear();
                for(ChemicalRepository.Chemical chemical:visibleChemicals){
                    String label=chemical.arabicName!=null&&!chemical.arabicName.isEmpty()?chemical.arabicName:chemical.name;
                    labels.add("🧪 "+label+"    |    "+repo.catalogCasLabel(chemical)+"    |    "+(chemical.chemicalClass==null?"—":chemical.chemicalClass));
                }
            }
            chemicalAdapter.clear();
            chemicalAdapter.addAll(labels);
            chemicalAdapter.notifyDataSetChanged();
            chemicalRows.setSelection(0);
            chemicalCount.setText("المواد المطابقة: "+visibleChemicals.size()+(results.size()==5000?" • الحد الأقصى للفهرس 5000":""));
        }catch(Exception ex){
            visibleChemicals.clear();
            chemicalAdapter.clear();
            chemicalAdapter.notifyDataSetChanged();
            chemicalCount.setText("المواد المطابقة: —");
        }
    }

    private ScrollView buildParticlesLanding(){
        ScrollView scroll=new ScrollView(c);
        scroll.setVerticalScrollBarEnabled(true);
        scroll.setScrollbarFadingEnabled(false);
        LinearLayout content=UiKit.vbox(c);
        content.setPadding(UiKit.dp(c,10),UiKit.dp(c,12),UiKit.dp(c,10),UiKit.dp(c,18));
        scroll.addView(content);
        content.addView(UiKit.title(c,"◌ الجزيئات والأغبرة"),UiKit.full(c));
        content.addView(card("تركيز الجزيئات المعلقة  •  µg/m³"),UiKit.full(c));
        TableLayout table=new TableLayout(c);table.setStretchAllColumns(true);
        String[][] rows={{"القناة","القراءة","الوحدة"},{"PM1.0","—","µg/m³"},{"PM2.5","—","µg/m³"},{"PM10","—","µg/m³"}};
        for(int i=0;i<rows.length;i++){
            TableRow row=new TableRow(c);
            row.setBackgroundColor(i%2==0?0xFFF1F3F6:Color.WHITE);
            for(String value:rows[i]){
                TextView cell=UiKit.text(c,value);cell.setTextColor(TEXT);cell.setTextSize(13);
                cell.setGravity(Gravity.CENTER);row.addView(cell);
            }
            table.addView(row);
        }
        content.addView(table,UiKit.full(c));
        content.addView(UiKit.title(c,"📈 المخططات"),UiKit.full(c));
        TextView unavailable=card("المخططات غير متاحة: لم تُسجل بيانات حقيقية من حساس PM1.0/PM2.5/PM10.");
        unavailable.setTextColor(0xFF805100);
        unavailable.setContentDescription("لا توجد مستشعرات جزيئات نشطة، ولا يمكن عرض رسوم بيانية أو قياسات مصطنعة");
        content.addView(unavailable,UiKit.full(c));
        // Never present a selectable or clickable chart unless recorded particulate
        // samples are connected. This avoids confusing a placeholder with a real plot.
        Button report=UiKit.button(c,"📄 التقرير الذكي للجزيئات");
        report.setEnabled(false);
        content.addView(report,UiKit.full(c));
        return scroll;
    }

    public View view(){return root;}
    public void refresh(){
        switch(selected){
            case 1:
                if(analysisSection==0)live.refresh();
                else{analysisResults.refresh();analysis.refresh();}
                break;
            case 2:physical.refresh();break;
            case 4:foodFreshness.refresh();break;
            default:break;
        }
    }
    private void showAnalysisSection(int index){
        analysisSection=Math.max(0,Math.min(1,index));
        if(analysisPage==null)return;
        View target=analysisSection==0?live.view:analysisResults.view;
        if(target.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)target.getParent()).removeView(target);
        analysisPage.removeAllViews();
        analysisPage.addView(target,new FrameLayout.LayoutParams(-1,-1));
        for(int i=0;i<analysisTabs.length;i++){
            boolean active=i==analysisSection;
            analysisTabs[i].setTextColor(TEXT);
            analysisTabs[i].setBackgroundColor(active?0xFFFFBE32:0xFFE4E7EC);
        }
        if(selected==1)refresh();
    }
    private void show(int index){
        selected=Math.max(0,Math.min(4,index));
        View target=views[selected];
        if(target.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)target.getParent()).removeView(target);
        page.removeAllViews();
        page.addView(target,new FrameLayout.LayoutParams(-1,-1));
        for(int i=0;i<tabs.length;i++){
            boolean active=i==selected;
            tabs[i].setTextColor(TEXT);
            tabs[i].setBackgroundColor(active?0xFFFFBE32:0xFFE4E7EC);
        }
        refresh();
    }

    private TextView card(String title){
        TextView t=UiKit.text(c,title);t.setTextColor(TEXT);t.setTextSize(13);t.setBackgroundColor(CARD);t.setPadding(UiKit.dp(c,10),UiKit.dp(c,9),UiKit.dp(c,10),UiKit.dp(c,9));return t;
    }
    private TextView kpi(String title){
        TextView t=card(title);t.setTextSize(14);t.setTypeface(Typeface.DEFAULT_BOLD);return t;
    }
    private void colorState(TextView t,boolean good,boolean warning){t.setTextColor(good?GOOD:warning?WARN:BAD);}
    /** Highlight only confirmed, comparable exposure/flammability exceedances. */
    private boolean exposureExceeded(JSONObject result){
        if(result==null||!result.optBoolean("input_gate_passed",false))return false;
        String toxic=result.optString("toxicity_state","");
        return result.optBoolean("lfl_threshold_crossed",false)
            || result.optInt("crossed_reference_count",0)>0
            || "LFL_REACHED_OR_EXCEEDED".equals(result.optString("flammability_state",""))
            || "CURRENT_CONCENTRATION_ABOVE_REFERENCE".equals(toxic)
            || "TIME_INTEGRATED_REFERENCE_CROSSED".equals(toxic);
    }
    private void exposureStyle(TextView view,boolean exceeded){
        view.setTextColor(exceeded?Color.RED:TEXT);
        view.setTypeface(Typeface.DEFAULT,exceeded?Typeface.BOLD:Typeface.NORMAL);
    }
    /** Uncalibrated source-backed ppm may trigger a clearly CONDITIONAL operator warning. */
    private void styleScreeningWarning(TextView view,JSONObject analysis){
        JSONObject result=analysis==null?null:analysis.optJSONObject(TankScreeningExposureWarning.KEY);
        JSONArray warnings=result==null?null:result.optJSONArray("warnings");
        boolean hit=warnings!=null&&warnings.length()>0,high=false;
        if(hit)for(int i=0;i<warnings.length();i++){
            JSONObject w=warnings.optJSONObject(i);if(w==null)continue;
            String role=w.optString("reference_role","");
            if(("LFL".equals(role)&&w.optDouble("ratio_to_reference",0)>=1.0)
                    ||"IDLH".equals(role)||"CEILING".equals(role)){high=true;break;}
        }
        view.setBackgroundColor(high?0xFFFFE1E1:hit?0xFFFFF1CE:CARD);
        view.setTextColor(high?0xFF8E1818:hit?0xFF805100:TEXT);
        view.setTypeface(Typeface.DEFAULT,hit?Typeface.BOLD:Typeface.NORMAL);
    }


    /** Tank V3 physical measurements: two clean cards per row; PM belongs only to its own subtab. */
    private final class PhysicalPane {
        private static final int NORMAL_BG=0xFFEDF0F2,ALERT_BG=0xFFC62828;
        final ScrollView view;
        final TextView[] values=new TextView[PhysicalCatalog.COUNT],states=new TextView[PhysicalCatalog.COUNT],titles=new TextView[PhysicalCatalog.COUNT];
        final LinearLayout[] cards=new LinearLayout[PhysicalCatalog.COUNT];
        final TextView envDiagnostic,soundRms,soundRmsState,soundP2p,soundP2pState;
        final TextView[] extraTitles=new TextView[2];
        final LinearLayout[] extraCards=new LinearLayout[2];
        double lastSoundRms=Double.NaN,lastSoundP2p=Double.NaN;
        final String[] icons={"🌡️","💧","🌤️","☀️","💡","🔊","📳","☢️","🔆","🌫️","😷","🌪️","💨","🧭","⛰️"};

        PhysicalPane(){
            view=new ScrollView(c);view.setVerticalScrollBarEnabled(true);view.setScrollbarFadingEnabled(false);
            LinearLayout box=UiKit.vbox(c);box.setPadding(UiKit.dp(c,6),UiKit.dp(c,7),UiKit.dp(c,6),UiKit.dp(c,12));view.addView(box);
            box.addView(UiKit.title(c,"🌡️ القياسات الفيزيائية والتشخيصية"),UiKit.full(c));
            java.util.ArrayList<LinearLayout> visibleCards=new java.util.ArrayList<>();
            for(int i=0;i<PhysicalCatalog.COUNT;i++){
                if(i==9||i==10||i==11){
                    // Particle measurements have a dedicated fourth measurement subtab.
                    values[i]=UiKit.small(c,"—");states[i]=UiKit.small(c,"في تبويب الجزيئات");
                    continue;
                }
                TextView[] parts=new TextView[3];
                LinearLayout card=createPhysicalCard(icons[i]+" "+PhysicalCatalog.NAMES_AR[i],PhysicalCatalog.UNITS[i],parts);
                cards[i]=card;titles[i]=parts[0];values[i]=parts[1];states[i]=parts[2];
                visibleCards.add(card);
            }
            TextView[] rmsParts=new TextView[3];
            extraCards[0]=createPhysicalCard("🎙️ MAX4466 RMS","V",rmsParts);
            extraTitles[0]=rmsParts[0];soundRms=rmsParts[1];soundRmsState=rmsParts[2];
            visibleCards.add(extraCards[0]);
            TextView[] peakParts=new TextView[3];
            extraCards[1]=createPhysicalCard("📈 MAX4466 P-P","V",peakParts);
            extraTitles[1]=peakParts[0];soundP2p=peakParts[1];soundP2pState=peakParts[2];
            visibleCards.add(extraCards[1]);
            for(int i=0;i<visibleCards.size();i+=2){
                LinearLayout row=UiKit.hbox(c);
                LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,-2,1f);
                left.setMargins(UiKit.dp(c,3),UiKit.dp(c,3),UiKit.dp(c,3),UiKit.dp(c,3));
                row.addView(visibleCards.get(i),left);
                if(i+1<visibleCards.size()){
                    LinearLayout.LayoutParams right=new LinearLayout.LayoutParams(0,-2,1f);
                    right.setMargins(UiKit.dp(c,3),UiKit.dp(c,3),UiKit.dp(c,3),UiKit.dp(c,3));
                    row.addView(visibleCards.get(i+1),right);
                }else row.addView(new View(c),new LinearLayout.LayoutParams(0,1,1f));
                box.addView(row,UiKit.full(c));
            }
            Button physicalPdf=UiKit.button(c,"📄 تقرير القياسات الفيزيائية PDF");
            physicalPdf.setOnClickListener(v->host.requestPhysicalSnapshotPdf());
            box.addView(physicalPdf,UiKit.full(c));
            envDiagnostic=UiKit.small(c,"ENV • بانتظار الاتصال");
            envDiagnostic.setTextColor(MUTED);envDiagnostic.setTextIsSelectable(true);envDiagnostic.setBackgroundColor(CARD);
            box.addView(envDiagnostic,UiKit.full(c));
        }
        private LinearLayout createPhysicalCard(String label,String unit,TextView[] out){
            LinearLayout card=UiKit.vbox(c);
            card.setPadding(UiKit.dp(c,10),UiKit.dp(c,10),UiKit.dp(c,10),UiKit.dp(c,10));
            card.setBackgroundColor(NORMAL_BG);
            TextView heading=UiKit.small(c,label+" • "+unit);heading.setTextSize(12);heading.setTextColor(TEXT);
            heading.setMaxLines(2);card.addView(heading,UiKit.full(c));
            TextView number=UiKit.title(c,"—");number.setTextSize(23);number.setTextColor(TEXT);
            card.addView(number,UiKit.full(c));
            TextView detail=UiKit.small(c,"غير مركب");detail.setTextSize(10);detail.setTextColor(MUTED);
            detail.setMaxLines(3);card.addView(detail,UiKit.full(c));
            out[0]=heading;out[1]=number;out[2]=detail;return card;
        }
        private void cardAlert(int i,boolean alert){
            if(cards[i]==null)return;
            cards[i].setBackgroundColor(alert?ALERT_BG:NORMAL_BG);
            int color=alert?Color.WHITE:TEXT;
            titles[i].setTextColor(color);values[i].setTextColor(color);
            states[i].setTextColor(alert?Color.WHITE:MUTED);
            values[i].setTypeface(Typeface.DEFAULT,alert?Typeface.BOLD:Typeface.NORMAL);
        }
        private void reset(String state){
            for(int i=0;i<PhysicalCatalog.COUNT;i++){
                values[i].setText("—");states[i].setText(state);
                if(cards[i]!=null)cardAlert(i,false);
            }
            soundRms.setText("—");soundP2p.setText("—");
            soundRmsState.setText(state);soundP2pState.setText(state);
            for(int i=0;i<extraCards.length;i++){
                extraCards[i].setBackgroundColor(NORMAL_BG);extraTitles[i].setTextColor(TEXT);
            }
            soundRms.setTextColor(TEXT);soundP2p.setTextColor(TEXT);
            soundRmsState.setTextColor(MUTED);soundP2pState.setTextColor(MUTED);
        }
        private void setPhysical(int index,double value,String source){
            if(!Double.isFinite(value)){
                values[index].setText("—");states[index].setText(source);cardAlert(index,false);return;
            }
            values[index].setText(String.format(Locale.US,index==14?"%.1f":"%.2f",value));
            states[index].setText(source);
            // Do not classify uncalibrated instant values as OSHA exposure exceedances.
            com.servoc.app.measurements.PhysicalReferencePolicy.Result result=
                com.servoc.app.measurements.PhysicalReferencePolicy.evaluate(index,value,true,false,false,0L,false);
            cardAlert(index,result.exceeded);
            if(result.exceeded)states[index].setText(result.explanation);
        }
        void refresh(){
            JSONObject d=host.latestDiagnostics();
            if(d==null){reset("غير متصل");envDiagnostic.setText("ENV • غير متصل");return;}
            reset("غير مركب");
            JSONObject e=d.optJSONObject("env");
            envDiagnostic.setText(envDiagnosticText(e));
            if(e!=null&&e.optBoolean("ok",false)){
                String type=e.optString("type","ENV"),src=type+" • "+e.optString("address","—")+" • "+e.optString("chip_id","—");
                double t=e.optDouble("temperature",Double.NaN),h=e.optDouble("humidity",Double.NaN),p=e.optDouble("pressure",Double.NaN);
                setPhysical(0,t,src+" • ● حية");
                setPhysical(1,h,Double.isFinite(h)?src+" • ● حية":"غير متاح في "+type);
                setPhysical(2,p,src+" • ● حية");
                if(Double.isFinite(p)&&p>0)setPhysical(14,44330.0*(1.0-Math.pow(p/1013.25,0.1903)),"مشتق من الضغط • ● حية");
                else states[14].setText("ضغط غير صالح");
            }else{
                String reason=envReason(e);states[0].setText(reason);states[1].setText(reason);
                states[2].setText(reason);states[14].setText("يتطلب ضغطًا صالحًا");
            }
            JSONObject ldr=d.optJSONObject("ldr");
            if(ldr!=null&&ldr.optBoolean("installed",false)){
                states[3].setText("LDR • عتبة فقط، لا قياس lux");
                states[4].setText("لا توجد معايرة lm");
            }
            JSONObject sound=d.optJSONObject("sound");
            double rms=sound==null?Double.NaN:sound.optDouble("rms_v",Double.NaN);
            double p2p=sound==null?Double.NaN:sound.optDouble("p2p_v",Double.NaN);
            if(Double.isFinite(rms)){
                lastSoundRms=Double.isFinite(lastSoundRms)?0.70*lastSoundRms+0.30*rms:rms;
                soundRms.setText(String.format(Locale.US,"%.4f",lastSoundRms));
                soundRmsState.setText("MAX4466 • RMS ممهّد للعرض");
            }else{lastSoundRms=Double.NaN;soundRmsState.setText("لا توجد قراءة");}
            if(Double.isFinite(p2p)){
                lastSoundP2p=Double.isFinite(lastSoundP2p)?0.70*lastSoundP2p+0.30*p2p:p2p;
                soundP2p.setText(String.format(Locale.US,"%.4f",lastSoundP2p));
                soundP2pState.setText("MAX4466 • P-P ممهّد للعرض");
            }else{lastSoundP2p=Double.NaN;soundP2pState.setText("لا توجد قراءة");}
            // An actually calibrated A-weighted channel can trigger a 90 dBA instant UI warning.
            double dba=sound==null?Double.NaN:sound.optDouble("dba",Double.NaN);
            boolean calibrated=sound!=null&&sound.optBoolean("calibrated_dba",false);
            if(calibrated&&Double.isFinite(dba)){
                setPhysical(5,dba,"dBA معاير • عتبة تنبيه 90");
                cardAlert(5,dba>90.0);
                if(dba>90.0)states[5].setText("تجاوز عتبة التنبيه اللحظي 90 dBA");
            }else{
                double db=sound==null?Double.NaN:sound.optDouble("db_spl",Double.NaN);
                if(Double.isFinite(db))setPhysical(5,db,"MAX4466 • dB SPL غير معاير");
                else states[5].setText(Double.isFinite(rms)||Double.isFinite(p2p)?"MAX4466 • لا توجد قراءة dB معايرة":"MAX4466 غير متصل");
            }
        }
    }

    private final class LivePane {
        final ScrollView view;final LinearLayout box,cards;
        final TextView powerCard,arrayCard,gasCard,sessionCard;
        final ProgressBar progress;
        final SensorCard[] sensorCards=new SensorCard[HazRobHardwareProfile.MQ.length];
        final Button powerToggle,start,cancel,report;
        private boolean active=false;

        LivePane(){
            view=new ScrollView(c);view.setVerticalScrollBarEnabled(true);view.setScrollbarFadingEnabled(false);box=UiKit.vbox(c);box.setPadding(UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,10));view.addView(box);
            box.addView(UiKit.title(c,"القياس اللحظي • HazRob Tank"),UiKit.full(c));
            LinearLayout row1=UiKit.hbox(c);powerCard=kpi("MQ\n—");arrayCard=kpi("المصفوفة\n—");row1.addView(powerCard,UiKit.weight(1));row1.addView(arrayCard,UiKit.weight(1));box.addView(row1,UiKit.full(c));
            LinearLayout row2=UiKit.hbox(c);gasCard=kpi("الاستجابة\n—");sessionCard=kpi("الجلسة\n—");row2.addView(gasCard,UiKit.weight(1));row2.addView(sessionCard,UiKit.weight(1));box.addView(row2,UiKit.full(c));
            progress=new ProgressBar(c,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(1000);progress.setProgress(0);box.addView(progress,UiKit.full(c));
            LinearLayout actions=UiKit.hbox(c);
            powerToggle=UiKit.button(c,"تشغيل MQ");powerToggle.setOnClickListener(v->togglePower());actions.addView(powerToggle,UiKit.weight(1));
            start=UiKit.button(c,"بدء القياس الآني");start.setOnClickListener(v->beginOperation());actions.addView(start,UiKit.weight(1));
            cancel=UiKit.button(c,"إنهاء وتحليل القياس");cancel.setOnClickListener(v->finishOperation());cancel.setEnabled(false);actions.addView(cancel,UiKit.weight(1.2f));
            box.addView(actions,UiKit.full(c));
            report=UiKit.button(c,"📄 إصدار تقرير القياسات الآنية");report.setEnabled(false);
            report.setOnClickListener(v->{
                TankInstantAnalyzer.Result result=currentOperation;
                if(result==null){host.showToast("أكمل عملية القياس أولاً");return;}
                host.requestInstantReportPdf(result.original,result.analysis,result.id,result.capturedAtMs);
            });
            box.addView(report,UiKit.full(c));
            box.addView(UiKit.small(c,"عملية واحدة دون سجل لقطات. بعد إنهائها تظهر النتيجة نفسها في ChemCore والمرشحون والتقرير."),UiKit.full(c));
            TextView ch=UiKit.small(c,"قنوات MQ • الاستجابة والاتجاه وجودة الإشارة");ch.setTextColor(MUTED);box.addView(ch,UiKit.full(c));
            cards=UiKit.vbox(c);box.addView(cards,UiKit.full(c));for(int i=0;i<sensorCards.length;i++){sensorCards[i]=new SensorCard(HazRobHardwareProfile.MQ[i]);cards.addView(sensorCards[i].root,UiKit.full(c));}
        }
        private void togglePower(){JSONObject d=host.latestDiagnostics(),r=d==null?null:d.optJSONObject("relay");boolean on=r!=null&&r.optBoolean("on",r.optInt("mask",0)!=0);setPower(!on);}
        private void setPower(boolean on){powerToggle.setEnabled(false);host.io().execute(()->{try{host.robotApi().mqPower(on);box.post(()->{powerToggle.setEnabled(true);host.refreshAllTabs();});}catch(Exception e){box.post(()->{powerToggle.setEnabled(true);host.showToast("تعذر تغيير طاقة MQ");});}});}
        private void beginOperation(){
            if(active){host.showToast("عملية القياس الآني ما زالت جارية");return;}
            JSONObject data=host.latestDiagnostics();
            if(data==null||data.optJSONObject("mq")==null){
                host.showToast("اتصل بحساسات MQ لبدء قياس آني");return;
            }
            currentOperation=null;active=true;progress.setProgress(0);
            start.setEnabled(false);cancel.setEnabled(true);report.setEnabled(false);
            sessionCard.setText("العملية • قياس جارٍ\nاختر إنهاء لالتقاط وتحليل القراءة الختامية");
            host.refreshAllTabs();
        }
        private void finishOperation(){
            if(!active){host.showToast("ابدأ قياساً آنياً أولاً");return;}
            JSONObject data=host.latestDiagnostics();
            if(data==null||data.optJSONObject("mq")==null){
                host.showToast("لا تتوفر قراءة MQ حية لإنهاء العملية");return;
            }
            final JSONObject frozen;
            try{frozen=new JSONObject(data.toString());}catch(Exception e){
                host.showToast("تعذر تجميد القراءات");return;
            }
            active=false;cancel.setEnabled(false);
            sessionCard.setText("العملية • يجري تحليل القراءة الختامية");
            final long now=System.currentTimeMillis();
            host.io().execute(()->{
                try{
                    TankInstantAnalyzer.Result completed=TankInstantAnalyzer.analyze(c,host.database(),frozen,now);
                    box.post(()->{
                        currentOperation=completed;start.setEnabled(true);report.setEnabled(true);
                        progress.setProgress(1000);
                        sessionCard.setText("العملية مكتملة\n"+completed.channels+" قنوات MQ");
                        host.refreshAllTabs();showAnalysisSection(1);
                    });
                }catch(Exception ex){
                    box.post(()->{
                        currentOperation=null;start.setEnabled(true);report.setEnabled(false);
                        progress.setProgress(0);
                        sessionCard.setText("العملية لم تكتمل • بيانات التحليل غير كافية");
                        host.showToast("تعذر تحليل MQ: "+ex.getClass().getSimpleName());
                    });
                }
            });
        }
        void refresh(){
            JSONObject d=host.latestDiagnostics();if(d==null){
                powerCard.setText("MQ\nغير متصل");
                arrayCard.setText("المصفوفة\n—");
                gasCard.setText("الاستجابة\n—");
                powerToggle.setText("تشغيل MQ");
                TankInstantAnalyzer.Result completed=currentOperation;
                if(active){sessionCard.setText("العملية • انقطع الاتصال أثناء القياس؛ لا توجد نتيجة مكتملة");progress.setProgress(500);}
                else if(completed!=null){sessionCard.setText("العملية مكتملة • التقرير متاح دون اتصال\n"+completed.channels+" قنوات MQ");progress.setProgress(1000);}
                else{sessionCard.setText("العملية • لا توجد قراءات حية");progress.setProgress(0);}
                cancel.setEnabled(active);
                report.setEnabled(completed!=null);
                colorState(sessionCard,completed!=null,active);
                for(SensorCard sc:sensorCards)sc.refresh(false,false,null,null,null);
                return;
            }
            JSONObject relay=d.optJSONObject("relay"),adc=d.optJSONObject("adc"),mq=d.optJSONObject("mq"),m=d.optJSONObject("measurement");
            boolean powered=relay!=null&&relay.optBoolean("on",relay.optInt("mask",0)!=0),a1=adc!=null&&adc.optBoolean("ads1",false),a2=adc!=null&&adc.optBoolean("ads2",false);
            powerCard.setText("MQ "+(powered?"● ON":"○ OFF")+"\nADS "+((a1?1:0)+(a2?1:0))+"/2");colorState(powerCard,powered&&a1&&a2,powered);powerToggle.setText(powered?"إطفاء MQ":"تشغيل MQ");
            JSONObject sd=d.optJSONObject("sensor_power");if(sd==null)sd=LiveSensorDiagnostic.evaluate(relay,adc,mq).toJson();int valid=sd.optInt("valid_channels",0),available=sd.optInt("available_channels",0),unstable=0;
            for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ){SensorHealthEngine.Snapshot h=host.sensorHealth(hw.key);if(h!=null&&(h.state==SensorHealthEngine.State.UNSTABLE||h.state==SensorHealthEngine.State.DRIFT_WARNING))unstable++;}
            arrayCard.setText("المصفوفة "+valid+"/"+available+"\nغير مستقرة "+unstable);colorState(arrayCard,valid>=6&&unstable==0,valid>=4);
            LiveGasStateEngine.Snapshot gas=host.liveGasState();String gs=gas==null?"غير متاح":liveGasStateAr(gas.state);gasCard.setText("استجابة إشارات MQ (بحثية فقط)\n"+gs+(gas==null?"":" • "+gas.respondingChannels+" قنوات"));boolean gasNormal=false; // No laboratory-grade normal atmosphere certification at V6.boolean gasWatch=gas!=null&&(gas.state==LiveGasStateEngine.State.WARMING_UP||gas.state==LiveGasStateEngine.State.BASELINE_REQUIRED);colorState(gasCard,gasNormal,gasWatch);
            TankInstantAnalyzer.Result completed=currentOperation;
            if(active){sessionCard.setText("العملية • جارٍ القياس");progress.setProgress(500);}
            else if(completed!=null){sessionCard.setText("العملية مكتملة\n"+completed.channels+" قنوات MQ");progress.setProgress(1000);}
            else{sessionCard.setText("العملية • جاهزة");progress.setProgress(0);}
            colorState(sessionCard,completed!=null,active);
            cancel.setEnabled(active);report.setEnabled(completed!=null);
            for(SensorCard sc:sensorCards)sc.refresh(true,powered,relay,adc,mq==null?null:mq.optJSONObject(sc.hw.key));
        }
    }

    private String liveGasStateAr(LiveGasStateEngine.State s){if(s==null)return "غير متاح";switch(s){case INSTANT_RESPONSE:return "تغير لحظي";case RESPONSE_WATCH:return "تغير متعدد";case RESPONSE_ALERT:return "تغير كبير (غير معتمد)";case WARMING_UP:return "تسخين";case BASELINE_REQUIRED:return "خط أساس مطلوب";case DEGRADED:return "قياس متدهور";case NORMAL:return "بلا تغير ملحوظ (غير معتمد)";default:return "غير متاح";}}

    private final class SensorCard {
        final HazRobHardwareProfile.Channel hw;final LinearLayout root;final TextView head,data;final AlphaAnimation pulse;
        SensorCard(HazRobHardwareProfile.Channel hw){this.hw=hw;root=UiKit.card(c);head=UiKit.text(c,"");head.setTextSize(14);head.setTypeface(Typeface.DEFAULT_BOLD);root.addView(head,UiKit.full(c));data=UiKit.small(c,"");data.setTextColor(0xFFCFD8DC);root.addView(data,UiKit.full(c));pulse=new AlphaAnimation(.45f,1f);pulse.setDuration(450);pulse.setRepeatMode(Animation.REVERSE);pulse.setRepeatCount(Animation.INFINITE);}
        void refresh(boolean connected,boolean powered,JSONObject relay,JSONObject adc,JSONObject q){
            head.clearAnimation();if(!connected){head.setBackgroundColor(0xFF424242);head.setTextColor(Color.WHITE);head.setText(hw.key.toUpperCase(Locale.US)+" • غير متصل");data.setText("Raw — • Vadc — • Vao — • Rs/R0 —");return;}
            if(!powered){head.setBackgroundColor(0xFF455A64);head.setTextColor(Color.WHITE);head.setText(hw.key.toUpperCase(Locale.US)+" • MQ OFF");data.setText(InstantMqWarningEngine.target(hw.key));return;}
            boolean adsReady=adc!=null&&adc.optBoolean(hw.adsIndex==0?"ads1":"ads2",false);if(!adsReady){head.setBackgroundColor(0xFFB71C1C);head.setTextColor(Color.WHITE);head.setText(hw.key.toUpperCase(Locale.US)+" • ADS"+(hw.adsIndex+1)+" غير مكتشف");data.setText("تحقق من التغذية وSDA/SCL وADDR");return;}
            long powerMs=relay==null?0:relay.optLong("power_ms",0);boolean qValid=q!=null&&q.optBoolean("ok",q.optBoolean("valid",false));if(!qValid){boolean settling=powerMs>0&&powerMs<LiveSensorDiagnostic.SETTLE_DIAGNOSTIC_MS;head.setBackgroundColor(settling?0xFF616161:0xFFEF6C00);head.setTextColor(Color.WHITE);head.setText(hw.key.toUpperCase(Locale.US)+(settling?" • انتظار القراءة":" • إشارة غير صالحة"));data.setText("Raw "+num(q,"raw",0)+" • Vadc "+num(q,"adc",3)+" V • Vao "+num(q,"ao",3)+" V");return;}
            int warm=Math.max(0,host.preferences().getInt("warm",60));long remain=Math.max(0,warm*1000L-powerMs);boolean ready=remain==0;SensorHealthEngine.Snapshot health=host.sensorHealth(hw.key);InstantMqWarningEngine.Result warn=InstantMqWarningEngine.evaluate(hw.key,q,health);boolean baseline=health!=null&&Double.isFinite(health.baselineRatio)&&health.baselineRatio>0;
            if(!ready){head.setBackgroundColor(0xFF616161);head.setTextColor(Color.WHITE);head.setText(hw.key.toUpperCase(Locale.US)+" • تسخين "+clock(remain));}
            else if(warn.level==InstantMqWarningEngine.Level.HIGH){head.setBackgroundColor(0xFFC62828);head.setTextColor(Color.WHITE);head.setText("⚠ "+hw.key.toUpperCase(Locale.US)+" • "+warn.alert);head.startAnimation(pulse);}
            else if(warn.level==InstantMqWarningEngine.Level.ELEVATED){head.setBackgroundColor(0xFFD32F2F);head.setTextColor(Color.WHITE);head.setText("⚠ "+hw.key.toUpperCase(Locale.US)+" • "+warn.alert);}
            else if(warn.level==InstantMqWarningEngine.Level.RESPONSE){head.setBackgroundColor(0xFF00695C);head.setTextColor(Color.WHITE);head.setText("↕ "+hw.key.toUpperCase(Locale.US)+" • استجابة مرصودة");}
            else{head.setBackgroundColor(0xFF1565C0);head.setTextColor(Color.WHITE);head.setText(hw.key.toUpperCase(Locale.US)+(baseline?" • جاهز":" • baseline مدمج"));}
            double ratio=AcceptedR0Normalizer.ratio(hw.key,q),acceptedR0=ManualR0Calibration.get(hw.key),fwR0=q.optDouble("firmware_r0",Double.NaN);boolean fwSync=Double.isFinite(fwR0)&&Double.isFinite(acceptedR0)&&acceptedR0>0&&Math.abs(fwR0-acceptedR0)/acceptedR0<=0.001;String delta=Double.isFinite(warn.delta)?String.format(Locale.US,"%+.1f%%",100.0*warn.delta):"—";String trend="—";if(health!=null&&Double.isFinite(health.baselineRatio)&&health.baselineRatio>0&&Double.isFinite(ratio))trend=ratio>health.baselineRatio?"↑":"↓";
            data.setText("Δ "+delta+" • اتجاه "+trend+" • Noise CV "+(health!=null&&Double.isFinite(health.noiseCv)?String.format(Locale.US,"%.1f%%",100*health.noiseCv):"—")+"\nRaw "+num(q,"raw",0)+" • Vadc "+num(q,"adc",3)+" V • Vao "+num(q,"ao",3)+" V\nRs "+num(q,"rs",3)+" • Rs/R0 "+(Double.isFinite(ratio)?String.format(Locale.US,"%.3f",ratio):"—")+" • R0 "+(Double.isFinite(acceptedR0)?String.format(Locale.US,"%.3f kΩ",acceptedR0):"—")+"\n"+(fwSync?"R0 متزامن":"App R0 authority")+" • "+SensorHealthEngine.stateAr(health.state));
        }
    }

    /**
     * V5 charts-only companion to the ONE ChemCore candidate/results workspace.
     * Shares currentOperation.analysis with the candidate cards and instant PDF.
     * It is intentionally not a separate user-facing tab or a second analyzer.
     */
    private final class AnalysisPane {
        final LinearLayout box;
        final Spinner chartChooser;
        final ImageView chartImage;
        final TextView chartState;
        final Button largeChart;
        private final java.util.ArrayList<Integer> availableCharts=new java.util.ArrayList<>();
        private String chartOperationId="";

        AnalysisPane(){
            box=UiKit.vbox(c);
            box.addView(UiKit.title(c,"📊 مخططات ChemCore للعملية نفسها"),UiKit.full(c));
            box.addView(UiKit.small(c,"تُعرض المخططات الممكنة من بيانات العملية الواحدة، ولا تُختلق سلسلة زمنية من لقطة واحدة."),UiKit.full(c));
            chartChooser=new Spinner(c);UiKit.outlinedSpinner(chartChooser);
            box.addView(chartChooser,UiKit.full(c));
            chartState=card("أكمل القياس الآني لعرض المخططات المتاحة");
            box.addView(chartState,UiKit.full(c));
            chartImage=new ImageView(c);chartImage.setAdjustViewBounds(true);
            chartImage.setVisibility(View.GONE);box.addView(chartImage,UiKit.full(c));
            largeChart=UiKit.button(c,"🔍 تكبير المخطط");largeChart.setEnabled(false);
            largeChart.setOnClickListener(v->openLargeChart());
            chartImage.setOnClickListener(v->openLargeChart());
            box.addView(largeChart,UiKit.full(c));
            chartChooser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
                public void onItemSelected(AdapterView<?> parent,View view,int pos,long id){showSelectedChart(pos);}
                public void onNothingSelected(AdapterView<?> parent){}
            });
            Button pdf=UiKit.button(c,"📄 التقرير الذكي للعملية الآنية PDF");
            pdf.setOnClickListener(v->export());
            box.addView(pdf,UiKit.full(c));
        }
        void refresh(){refreshChartChoices(currentOperation);}
        private void refreshChartChoices(TankInstantAnalyzer.Result item){
            String id=item==null?"":item.id;
            if(id.equals(chartOperationId))return;
            chartOperationId=id;availableCharts.clear();
            java.util.ArrayList<String> names=new java.util.ArrayList<>();
            if(item!=null)for(int i=0;i<TankInstantCharts.LABELS.length;i++){
                if(TankInstantCharts.available(i,item.original,item.analysis)){
                    availableCharts.add(i);names.add(TankInstantCharts.LABELS[i]);
                }
            }
            if(names.isEmpty())names.add("لا تتوفر رسوم مبنية على قياسات صالحة");
            chartChooser.setAdapter(UiKit.lightOptions(c,names.toArray(new String[0])));
            chartChooser.setEnabled(!availableCharts.isEmpty());
            chartImage.setVisibility(View.GONE);
            chartImage.setImageDrawable(null);
            largeChart.setEnabled(false);
            chartState.setText(item==null?"أكمل القياس الآني لعرض المخططات":
                availableCharts.isEmpty()?"لا تتوفر بيانات كافية للرسم":
                "اختر أحد "+availableCharts.size()+" مخططات متاحة");
            if(!availableCharts.isEmpty())showSelectedChart(0);
        }
        private void showSelectedChart(int selectedIndex){
            TankInstantAnalyzer.Result item=currentOperation;
            if(item==null||selectedIndex<0||selectedIndex>=availableCharts.size())return;
            try{
                Bitmap b=TankInstantCharts.render(availableCharts.get(selectedIndex),item.original,item.analysis);
                chartImage.setImageBitmap(b);chartImage.setVisibility(View.VISIBLE);
                largeChart.setEnabled(true);
                chartState.setText(TankInstantCharts.LABELS[availableCharts.get(selectedIndex)]);
            }catch(Exception e){
                chartImage.setImageDrawable(null);chartImage.setVisibility(View.GONE);
                largeChart.setEnabled(false);chartState.setText("المخطط غير متاح لهذه العملية");
            }
        }
        private void openLargeChart(){
            android.graphics.drawable.Drawable drawable=chartImage.getDrawable();
            if(drawable==null)return;
            ImageView enlarged=new ImageView(c);
            enlarged.setImageDrawable(drawable);enlarged.setAdjustViewBounds(true);
            new AlertDialog.Builder(c).setTitle(String.valueOf(chartChooser.getSelectedItem()))
                .setView(enlarged).setPositiveButton("إغلاق",null).show();
        }
        private void export(){
            TankInstantAnalyzer.Result result=currentOperation;
            if(result==null){host.showToast("أكمل القياس الآني أولاً");return;}
            host.requestInstantReportPdf(result.original,result.analysis,result.id,result.capturedAtMs);
        }
    }

    private final class AnalysisResultsPane {
        final ScrollView view;final LinearLayout box,candidateList,screeningList;
        final TextView decisionCard,screeningCard,safetyCard,tracking,selected,referenceInventory,screeningWarnings;
        TextView technical;
        final Button startTracking,screeningToggle,technicalToggle;
        final ChemCoreDecisionRepository decisions;
        int selectedCandidateId=-1;String selectedCandidateName="";boolean showAll144=false,showTechnical=false;long lastRenderedAt=0;

        AnalysisResultsPane(){
            view=new ScrollView(c);box=UiKit.vbox(c);box.setPadding(UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,10));view.addView(box);
            box.addView(UiKit.title(c,"نتائج ChemCore"),UiKit.full(c));
            decisionCard=kpi("القرار: —");box.addView(decisionCard,UiKit.full(c));
            screeningCard=card("فحص 144: —");box.addView(screeningCard,UiKit.full(c));
            safetyCard=card("السلامة والتعرض: —");box.addView(safetyCard,UiKit.full(c));
            screeningWarnings=card("تحذيرات PPM التقديرية\n—");screeningWarnings.setTextIsSelectable(true);box.addView(screeningWarnings,UiKit.full(c));
            referenceInventory=card("حدود التعرض المرجعية\n—");referenceInventory.setTextIsSelectable(true);box.addView(referenceInventory,UiKit.full(c));
            tracking=card("التتبع: لا يوجد");box.addView(tracking,UiKit.full(c));selected=card("المرشح المحدد: لا يوجد");box.addView(selected,UiKit.full(c));
            TextView ch=UiKit.small(c,"المرشحون الديناميكيون • اللون ترتيب تحليلي وليس مستوى خطورة");ch.setTextColor(MUTED);box.addView(ch,UiKit.full(c));
            candidateList=UiKit.vbox(c);box.addView(candidateList,UiKit.full(c));
            LinearLayout actions=UiKit.hbox(c);startTracking=UiKit.button(c,"بدء التتبع");startTracking.setEnabled(false);startTracking.setOnClickListener(v->{if(selectedCandidateId<0){host.showToast("اختر مرشحًا أولًا");return;}setUnifiedTrackingTarget(selectedCandidateId,selectedCandidateName);host.showToast("بدأ تتبع: "+selectedCandidateName);refresh();});actions.addView(startTracking,UiKit.weight(1));Button pick=UiKit.button(c,"اختيار غاز من المكتبة");pick.setOnClickListener(v->chooseTrackingChemical());actions.addView(pick,UiKit.weight(1.15f));Button stop=UiKit.button(c,"إيقاف التتبع");stop.setOnClickListener(v->{setUnifiedTrackingTarget(-1,"");host.showToast("تم إيقاف التتبع");refresh();});actions.addView(stop,UiKit.weight(1));box.addView(actions,UiKit.full(c));
            Button showLimits=UiKit.button(c,"📘 حدود التعرض للمادة المحددة");
            showLimits.setOnClickListener(v->showSelectedExposureReferences());
            box.addView(showLimits,UiKit.full(c));
            screeningToggle=UiKit.button(c,"إظهار فحص 144");screeningToggle.setOnClickListener(v->{showAll144=!showAll144;screeningToggle.setText(showAll144?"إخفاء فحص 144":"إظهار فحص 144");lastRenderedAt=0;refresh();});box.addView(screeningToggle,UiKit.full(c));screeningList=UiKit.vbox(c);box.addView(screeningList,UiKit.full(c));
            technicalToggle=UiKit.button(c,"إظهار البواقي والتفاصيل");technicalToggle.setOnClickListener(v->{showTechnical=!showTechnical;technical.setVisibility(showTechnical?View.VISIBLE:View.GONE);technicalToggle.setText(showTechnical?"إخفاء البواقي والتفاصيل":"إظهار البواقي والتفاصيل");});box.addView(technicalToggle,UiKit.full(c));technical=card("");technical.setVisibility(View.GONE);technical.setTextIsSelectable(true);box.addView(technical,UiKit.full(c));
            decisions=new ChemCoreDecisionRepository(host.database());
        }
        private boolean isResearchOnlyV6(JSONObject analysis){
            return analysis!=null&&
                "hazrob-tank-v6-android-telemetry-bridge/1".equals(
                    analysis.optString("schema",""));
        }
        /** Research-only V6 presentation: no legacy posterior, hazard ranking,
         * occupational exposure alerts or automatic target tracking.
         */
        private void renderResearchOnlyV6(JSONObject p){
            // Manual selection remains permitted for exploratory gradient
            // tracking; never promote a source hypothesis to confirmed gas.
            startTracking.setEnabled(selectedCandidateId>=0);
            screeningToggle.setEnabled(false);
            selected.setText(selectedCandidateId>=0?
                "مادة محددة من الفهرس الموحد • "+selectedCandidateName+
                " • تتبع استدلالي وليس رصد هوية":
                "المرشح المحدد • لا توجد هوية غاز معتمدة للتتبع الآلي");
            decisionCard.setText("ChemCore Tank V6 • نتيجة بحثية فقط\n"+
                "الهوية: UNKNOWN • لم تثبت المعايرة الفيزيائية G3\n"+
                "لا يوجد تركيز ppm معتمد أو احتمال كشف موثوق");
            decisionCard.setTextColor(INFO);
            screeningCard.setText("فحص المصادر: فرضيات مستقلة، دون استبعاد آلي"+
                "\nمصادر Rs/R0 صالحة "+p.optInt("ratio_ready_mq_channels",0)+
                " • قراءات بغير R0 "+p.optInt("raw_only_mq_channels",0));
            safetyCard.setText("سلامة الغاز والتعرض: غير مؤهل لإصدار تحذير مهني"+
                "\nلا تستخدم نتيجة المصدر لقرار حدود التعرض أو الاشتعال");
            exposureStyle(safetyCard,false);
            screeningWarnings.setBackgroundColor(CARD);
            screeningWarnings.setTextColor(TEXT);
            screeningWarnings.setText("Open-set غير مؤهل على بيانات Tank؛"+
                "\nلا إثبات للغاز أو تركيزه ولا حكم بغياب الخطر");
            referenceInventory.setText("حدود التعرض: تُراجع مستقلة عن هذه النتيجة"+
                "\nلا توجد قياسات ppm أو تجاوزات مثبتة");
            // Operator candidates come ONLY from the unified master UIDs;
            // source-curve labels/legacy indices are retained separately
            // as research evidence, never as duplicate selectable chemicals.
            JSONArray source=p.optJSONArray("source_curve_hypotheses");
            JSONArray masterCandidates=p.optJSONArray("candidates");
            int n=source==null?0:source.length();
            int count=masterCandidates==null?0:masterCandidates.length();
            if(count==0){
                candidateList.addView(card(n==0?
                    "لا توجد مرشحات كيميائية: استجابة الهواء ضمن المرجع أو غير مؤكدة"+
                    "\nاحتُفظ بالقراءات الأصلية ومعادلات المصادر للتدقيق":
                    "منحنيات مصدرية بحثية متاحة لكن لا يوجد مرشح مرتبط بهوية موحدة"+
                    "\nتراجع أدلة المصادر دون إنشاء مادة وهمية"),UiKit.full(c));
            }else{
                for(int i=0;i<Math.min(count,12);i++){
                    JSONObject q=masterCandidates.optJSONObject(i);
                    if(q==null)continue;
                    long uid=q.optLong("chemical_uid",-1);
                    if(uid<0||uid>Integer.MAX_VALUE)continue;
                    String name=q.optString("label","—");
                    String line="#"+q.optInt("rank",i+1)+" • "+name+
                        "\nمنحنيات مصدرية: "+q.optInt("source_curve_count",0)+
                        " • الهوية والـPPM غير معتمدين؛ يمكن اختيار المادة للتتبع اليدوي";
                    Button row=UiKit.button(c,line);
                    row.setTextColor(Color.BLACK);
                    row.setBackground(UiKit.outline(c,CARD,0xFF8994A3));
                    row.setOnClickListener(v->{
                        selectedCandidateId=(int)uid;
                        selectedCandidateName=name;
                        selected.setText("مادة مختارة من الفهرس الموحد • "+name+
                            "\nهذا الاختيار ليس إثباتًا لوجود الغاز");
                        startTracking.setEnabled(true);
                    });
                    LinearLayout resultRow=UiKit.hbox(c);
                    resultRow.addView(row,new LinearLayout.LayoutParams(0,-2,1f));
                    java.util.List<String> codes=GhsPictograms.fromJson(
                        q.optJSONArray("ghs_pictograms"));
                    GhsPictograms.addImages(resultRow,c,codes,38,3);
                    candidateList.addView(resultRow,UiKit.full(c));
                }
                if(count>12)candidateList.addView(
                    card("توجد "+(count-12)+" مواد إضافية في مرشحي البحث"),
                    UiKit.full(c));
            }
            JSONArray raw=p.optJSONArray("original_mq_channel_evidence");
            StringBuilder audit=new StringBuilder("ChemCore V6 • سجل أدلة مصدرية\n"+
                "عدد فرضيات منحنيات المصادر: "+n+"\n"+
                "القنوات الأصلية: "+(raw==null?0:raw.length())+"\n"+
                "الهوية المبلغة: UNKNOWN\n"+
                "R0 ومعايرة Tank G3: غير مثبتتين\n"+
                "النتيجة لا تحتوي على Posterior أو ppm قابل للإبلاغ");
            JSONArray conflict=p.optJSONArray("source_curve_disagreements");
            audit.append("\nمقارنات منحنيات متعارضة: ")
                .append(conflict==null?0:conflict.length());
            technical.setText(audit.toString());
        }

        void refresh(){
            long now=android.os.SystemClock.elapsedRealtime();
            if(lastRenderedAt>0&&now-lastRenderedAt<750L)return;
            lastRenderedAt=now;
            TankInstantAnalyzer.Result completed=currentOperation;JSONObject p=completed==null?null:completed.analysis;candidateList.removeAllViews();screeningList.removeAllViews();tracking.setText(host.trackingTargetId()>=0?"التتبع • "+host.trackingTargetName()+" (#"+host.trackingTargetId()+") • فرضية مشغل":"التتبع • لا يوجد");
            if(p==null){startTracking.setEnabled(selectedCandidateId>=0);decisionCard.setText("القرار: لا توجد نتيجة محفوظة — يسمح بالتتبع الاستدلالي قبل المعايرة");screeningCard.setText("فحص 144: —");safetyCard.setText("السلامة والتعرض: —");exposureStyle(safetyCard,false);referenceInventory.setText("حدود التعرض: لا يوجد تحليل حالي.");screeningWarnings.setText("لا توجد نتيجة حالية لحساب تحذير تقديري.");technical.setText("—");return;}
            if(isResearchOnlyV6(p)){
                renderResearchOnlyV6(p);
                return;
            }
            screeningToggle.setEnabled(true);
            String chemical=p.optString("chemical","—"),outcome=p.optString("outcome","—"),tier=p.optString("evidence_tier","—");double post=p.optDouble("top_posterior",Double.NaN),margin=p.optDouble("posterior_separation_margin",Double.NaN);TankPpmEstimate.Result estimate=TankPpmEstimate.from(p);boolean ppmOk=estimate.reportable;double ppm=estimate.ppm;
            decisionCard.setText("القرار • "+outcome+"\n"+chemical+" • "+pct(post)+" • Δ "+pct(margin)+" • "+tier+(estimate.available()?" • "+String.format(Locale.US,"%.2f ppm • %s",ppm,estimate.qualityAr()):" • لا يوجد رقم PPM محسوب"));decisionCard.setTextColor("M4_HAZROB_VALIDATED".equals(tier)?GOOD:INFO);
            JSONObject s=p.optJSONObject("screening_144");screeningCard.setText("فحص 144 • "+(s==null?"غير متاح":s.optString("state","—")+" • "+s.optInt("evaluated_count",0)+"/"+s.optInt("target_count",144)));
            JSONObject g=p.optJSONObject("gas_exposure_ppm");safetyCard.setText(g==null?"السلامة والتعرض • محجوب/غير متاح":"السلامة والتعرض • "+g.optString("toxicity_state",g.optString("state","—"))+" • الاشتعال "+g.optString("flammability_state","—")+" • أولوية "+fmt(g.optDouble("hazard_priority_percent",Double.NaN))+"%");
            exposureStyle(safetyCard,exposureExceeded(g));
            referenceInventory.setText(TankExposureReferenceCatalog.summary(p,5,10));
            screeningWarnings.setText(TankScreeningExposureWarning.summary(p,8));styleScreeningWarning(screeningWarnings,p);
            if(ppmOk&&Double.isFinite(ppm)&&exposureExceeded(g))exposureStyle(decisionCard,true);
            renderCandidates(p);if(showAll144)renderScreening(s,p);
            JSONObject fingerprint=p.optJSONObject(com.servoc.app.chemcore.TankFingerprintScreening.KEY);
            String fpNote=fingerprint==null?"":("\n\n🧬 البصمة البحثية: "+
                (fingerprint.optBoolean("fingerprint_available",false)?
                    fingerprint.optString("label","—")+" • channels "+
                        fingerprint.optInt("contributors",0)+" • distance "+
                        fmt(fingerprint.optDouble("distance",Double.NaN)):
                    "لا توجد مطابقة حسابية")+
                " • مسار مستقل لا يشترط M4 أو Open-Set للحساب");
            technical.setText(ChemCoreAnalysisPresenter.equationsAndResiduals(p,8)+fpNote);
        }
        private void showSelectedExposureReferences(){
            if(selectedCandidateId<0){
                host.showToast("اختر مادة أو مرشحًا أولًا لعرض حدود التعرض دون اشتراط المعايرة");
                return;
            }
            String details;
            try{
                JSONObject inventory=TankExposureReferenceCatalog.forSelectedChemical(
                    host.database(),selectedCandidateId,selectedCandidateName);
                JSONObject current=new JSONObject();
                current.put(TankExposureReferenceCatalog.KEY,inventory);
                details=TankExposureReferenceCatalog.summary(current,1,32);
            }catch(Exception ex){
                details="تعذر تحميل حدود التعرض لهذه المادة من قاعدة البيانات المحلية.";
            }
            new AlertDialog.Builder(c)
                .setTitle("حدود التعرض المرجعية • لا تتطلب M4")
                .setMessage(details)
                .setPositiveButton("إغلاق",null)
                .show();
        }
        /** Operator can select a target from the local ChemCore catalogue before R0
         *  and even before the first stored analysis. Selection is NOT detection.
         */
        private void chooseTrackingChemical(){
            EditText query=new EditText(c);query.setSingleLine(true);
            query.setHint("اسم المادة أو رقم CAS");
            query.setInputType(InputType.TYPE_CLASS_TEXT);
            query.setTextColor(Color.BLACK);query.setHintTextColor(0xFF56616F);
            query.setBackground(UiKit.outline(c,Color.WHITE,0xFF8994A3));
            new AlertDialog.Builder(c)
                .setTitle("اختيار مادة للتتبع الاستدلالي")
                .setMessage("اختيار الغاز لا يعني رصده. قبل R0 يكون تتبع التدرج من تغير Rs النسبي فقط، دون هوية أو ppm معتمدين.")
                .setView(query)
                .setNegativeButton("إلغاء",null)
                .setPositiveButton("بحث",(d,w)->{
                    ChemicalRepository pickerRepo=new ChemicalRepository(host.database());
                    List<ChemicalRepository.Chemical> matches=pickerRepo.search(query.getText().toString(),30);
                    if(matches.isEmpty()){host.showToast("لا توجد مادة مطابقة في المكتبة");return;}
                    String[] labels=new String[matches.size()];
                    for(int i=0;i<matches.size();i++){
                        ChemicalRepository.Chemical m=matches.get(i);
                        String known=pickerRepo.catalogCasLabel(m);
                        labels[i]=m.label()+(m.cas==null&&!"—".equals(known)?
                            " • "+known:"");
                    }
                    android.widget.ListAdapter candidateAdapter=new ArrayAdapter<String>(c,android.R.layout.simple_list_item_1,labels){
                        @Override public View getView(int position,View reused,android.view.ViewGroup parent){
                            View row=super.getView(position,reused,parent);
                            UiKit.ensureLightRow(row);return row;
                        }
                    };
                    new AlertDialog.Builder(c)
                        .setTitle("حدد المادة")
                        .setAdapter(candidateAdapter,(dialog,index)->{
                            ChemicalRepository.Chemical item=matches.get(index);
                            if(item.uid>Integer.MAX_VALUE){host.showToast("معرّف غير مدعوم");return;}
                            selectedCandidateId=(int)item.uid;
                            selectedCandidateName=item.label();
                            selected.setText("مادة محددة من المكتبة • "+selectedCandidateName+" • لم يُثبت رصدها بعد");
                            startTracking.setEnabled(true);
                        })
                        .setNegativeButton("إلغاء",null)
                        .show();
                }).show();
        }

        private void renderCandidates(JSONObject p){
            // Recreate only the bounded current results. Do not append duplicates when selecting a candidate.
            candidateList.removeAllViews();
            JSONArray a=p.optJSONArray("candidates");if(a==null||a.length()==0){candidateList.addView(card("لا توجد فرضيات قابلة للحل"),UiKit.full(c));return;}int n=Math.min(12,a.length());
            for(int i=0;i<n;i++){JSONObject q=a.optJSONObject(i);if(q==null)continue;int rank=q.optInt("rank",i+1);long uid=q.optLong("chemical_uid",-1);String name=q.optString("label","—");double posterior=q.optDouble("posterior",Double.NaN),coverage=q.optDouble("coverage_pct",Double.NaN),agreement=q.optDouble("agreement_pct",Double.NaN),residual=q.optDouble("residual_rms",Double.NaN);String tier=q.optString("evidence_tier",p.optString("evidence_tier","—"));double fitPpm=q.optDouble("screening_fit_ppm",q.optDouble("best_ppm",Double.NaN));String ppmLine=Double.isFinite(fitPpm)&&fitPpm>=0?"\nPPM حسابي: "+String.format(Locale.US,"%.2f",fitPpm)+" • تقدير غير مرجعي":"";String hazard=TankCandidateHazardBadges.forCandidate(p,uid);int bg=candidateColor(rank-1,"");Button row=UiKit.button(c,(uid==selectedCandidateId?"◉ ":"")+"#"+rank+"  "+name+hazard+"\nPosterior "+pct(posterior)+" • Coverage "+pctAny(coverage)+" • Agreement "+pctAny(agreement)+" • Residual "+fmt(residual)+" • "+tier+ppmLine);row.setAllCaps(false);row.setTextColor(foregroundFor(bg));row.setBackgroundColor(bg);row.setOnClickListener(v->{selectedCandidateId=(int)uid;selectedCandidateName=name;selected.setText("المرشح المحدد • "+name+" (#"+uid+") • الاختيار لا يعني قبول الهوية");startTracking.setEnabled(uid>=0);renderCandidates(p);});candidateList.addView(row,UiKit.full(c));}
        }
        private void renderScreening(JSONObject screening,JSONObject p){
            JSONArray rows=screening==null?null:screening.optJSONArray("rows");if(rows==null){screeningList.addView(card("لا توجد مصفوفة فحص 144"),UiKit.full(c));return;}HashMap<Long,Integer> ranks=new HashMap<>();JSONArray candidates=p.optJSONArray("candidates");if(candidates!=null)for(int i=0;i<candidates.length();i++){JSONObject q=candidates.optJSONObject(i);if(q!=null)ranks.put(q.optLong("chemical_uid",-1),i);}
            for(int i=0;i<rows.length();i++){JSONObject r=rows.optJSONObject(i);if(r==null)continue;long uid=r.optLong("index",i);String name=r.optString("name","#"+uid),status=r.optString("status","");Integer rank=ranks.get(uid);int bg=candidateColor(rank,status);TextView row=card((rank==null?"":("#"+(rank+1)+"  "))+name+" • "+status);row.setBackgroundColor(bg);row.setTextColor(foregroundFor(bg));screeningList.addView(row,UiKit.full(c));}
        }
        private int candidateColor(Integer rank,String status){if("OUT_OF_DOMAIN".equals(status)||"UNSUPPORTED_BY_CURRENT_ARRAY".equals(status))return 0xFF616161;if(rank==null)return 0xFFF5F5F5;if(rank==0)return 0xFFEF5350;if(rank==1)return 0xFFFF9800;if(rank==2)return 0xFFFFEB3B;if(rank==3)return 0xFF66BB6A;return 0xFF42A5F5;}
        private int foregroundFor(int bg){int r=Color.red(bg),g=Color.green(bg),b=Color.blue(bg);double lum=.2126*r+.7152*g+.0722*b;return lum>150?Color.BLACK:Color.WHITE;}
    }

    private String compactCandidates(JSONObject p,int limit){
        JSONArray a=p==null?null:p.optJSONArray("candidates");if(a==null||a.length()==0)return "المرشحون: لا توجد فرضيات قابلة للحل";StringBuilder s=new StringBuilder("المرشحون الأعلى");int n=Math.min(Math.max(1,limit),a.length());for(int i=0;i<n;i++){JSONObject q=a.optJSONObject(i);if(q==null)continue;s.append("\n#").append(q.optInt("rank",i+1)).append(" ").append(q.optString("label","—")).append(" • ").append(pct(q.optDouble("posterior",Double.NaN))).append(" • residual ").append(fmt(q.optDouble("residual_rms",Double.NaN)));}return s.toString();
    }
    private static String pct(double v){if(!Double.isFinite(v))return "—";double q=v<=1.000001?v*100.0:v;return String.format(Locale.US,"%.1f%%",q);}
    private static String pctAny(double v){return pct(v);}
    private static String fmt(double v){return Double.isFinite(v)?String.format(Locale.US,"%.3f",v):"—";}
    private static String envDiagnosticText(JSONObject e){if(e==null)return"ENV I²C • SDA GPIO13 / SCL GPIO14 • 100 kHz\nلا توجد بيانات تشخيصية";String type=e.optString("type","NONE"),addr=e.optString("address","—"),chip=e.optString("chip_id","—"),lastAddr=e.optString("last_address","—"),lastChip=e.optString("last_chip_id","—"),status=e.optString("status_code",e.optString("status","unknown"));boolean a76=e.optBoolean("ack_0x76",false),a77=e.optBoolean("ack_0x77",false);long fails=Math.max(0,e.optLong("read_failures",0)),age=Math.max(0,e.optLong("last_ok_age_ms",0));return"ENV I²C • SDA GPIO13 / SCL GPIO14 • 100 kHz\n0x76 "+(a76?"ACK":"NO ACK")+" • 0x77 "+(a77?"ACK":"NO ACK")+"\nSensor "+type+" • address "+addr+" • chip "+chip+"\nLast address "+lastAddr+" • last chip "+lastChip+"\nStatus "+status+" • read failures "+fails+" • last OK age "+age+" ms"+"\nBME direct GPIO13/14 • total errors "+e.optLong("total_read_errors",0)+" • I²C bus recoveries "+e.optLong("bus_recoveries",0)+" • bus timeout "+e.optInt("i2c_timeout_ms",0)+" ms";}
    private static String envReason(JSONObject e){if(e==null)return"ENV: لا توجد بيانات تشخيصية";String s=e.optString("status_code",e.optString("status","unknown"));if("no_ack".equals(s))return"ENV: لا ACK على 0x76/0x77 • bus GPIO13/14";if("unsupported_chip_id".equals(s))return"ENV: العنوان يستجيب لكن Chip ID غير مدعوم • "+e.optString("last_chip_id","—");if("chip_id_read_failed".equals(s))return"ENV: ACK موجود وفشلت قراءة Chip ID";if("calibration_read_failed".equals(s))return"ENV: فشلت قراءة سجلات المعايرة";if("configuration_failed".equals(s))return"ENV: فشلت كتابة سجلات التهيئة";if("initial_read_failed".equals(s)||"read_failed".equals(s)||"runtime_read_failed_reprobe".equals(s))return"ENV: فشل قراءة البيانات • إعادة اكتشاف تلقائية";return"ENV غير جاهز • "+s+" • 0x76="+(e.optBoolean("ack_0x76",false)?"ACK":"—")+" 0x77="+(e.optBoolean("ack_0x77",false)?"ACK":"—");}
    private static String num(JSONObject o,String k,int d){if(o==null||o.isNull(k))return"—";double v=o.optDouble(k,Double.NaN);return Double.isFinite(v)?String.format(Locale.US,"%."+d+"f",v):"—";}
    private static String clock(long ms){long s=(ms+999)/1000;return String.format(Locale.US,"%02d:%02d",s/60,s%60);}
    private static String errorAr(String e){if("mq_power_off".equals(e))return"طاقة MQ متوقفة";if("ads_missing".equals(e))return"ADS1/ADS2 غير مكتشفين";if("power_changed".equals(e))return"تغيرت طاقة MQ أثناء الجلسة";if("no_valid_samples".equals(e))return"لم تصل عينات MQ صالحة";return e==null||e.isEmpty()?"سبب غير محدد":e;}
    private static String isoNow(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
}
