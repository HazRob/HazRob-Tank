package com.servoc.app.chemical;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.servoc.app.R;
import com.servoc.app.data.ExperimentsDb;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Offline GHS01..GHS09 pictogram identifiers backed ONLY by a material's
 * source-qualified, explicitly coded hazard classification.
 *
 * Pictograms describe hazards of an independently confirmed substance.
 * They never prove that the robot detected that substance, nor certify its
 * airborne concentration or operator alarm. No inference from CO2 abundance,
 * generic sensor curves, LFL alone, unknown classes or G3 status.
 *
 * Radiation and simple asphyxiation are NOT GHS pictograms; represent their
 * warnings separately rather than fabricating GHS10.
 */
public final class GhsPictograms {
    public static final String SCHEMA="hazrob-tank-ghs-provenance/1";
    private GhsPictograms(){}
    // Only hold the application context (never an Activity) for deferred
    // PDF export on a worker thread. Graphics remain offline resources.
    private static volatile Context applicationContext;
    public static void bindContext(Context context){
        if(context!=null)applicationContext=context.getApplicationContext();
    }
    public static Context applicationContext(){return applicationContext;}


    public static boolean valid(String code){
        return code!=null&&code.matches("GHS0[1-9]");
    }
    private static int resource(String c){
        switch(c){
            case "GHS01":return R.drawable.ghs_01;
            case "GHS02":return R.drawable.ghs_02;
            case "GHS03":return R.drawable.ghs_03;
            case "GHS04":return R.drawable.ghs_04;
            case "GHS05":return R.drawable.ghs_05;
            case "GHS06":return R.drawable.ghs_06;
            case "GHS07":return R.drawable.ghs_07;
            case "GHS08":return R.drawable.ghs_08;
            case "GHS09":return R.drawable.ghs_09;
            default:return 0;
        }
    }
    public static String arabic(String code){
        switch(code){
            case "GHS01":return "متفجر";
            case "GHS02":return "قابل للاشتعال";
            case "GHS03":return "مؤكسد";
            case "GHS04":return "غاز مضغوط";
            case "GHS05":return "أكال / تآكل";
            case "GHS06":return "سمية حادة شديدة";
            case "GHS07":return "ضار / مهيج";
            case "GHS08":return "خطر صحي جسيم";
            case "GHS09":return "خطر على البيئة المائية";
            default:return "غير مصنف";
        }
    }
    private static List<String> ordered(Set<String> codes){
        ArrayList<String> out=new ArrayList<>(codes);Collections.sort(out);return out;
    }
    private static void addVerified(Set<String> into,String raw){
        if(raw==null)return;
        for(String candidate:raw.toUpperCase(Locale.ROOT).split("[,;|\\s]+")){
            if(valid(candidate))into.add(candidate);
        }
    }
    /**
     * All symbols in ONE SQL scan, suitable for the 734-row list without
     * per-row database traffic. Each material can display multiple symbols,
     * with each official GHS symbol shown only once.
     */
    public static Map<Long,List<String>> allQualified(ExperimentsDb db){
        Map<Long,Set<String>> byUid=new HashMap<>();
        try(Cursor c=db.getReadableDatabase().rawQuery(
                "SELECT hc.chemical_uid,hc.ghs_pictogram "+
                "FROM chemical_hazard_classifications hc "+
                "JOIN scientific_sources ss ON ss.source_id=hc.source_id "+
                "JOIN chemicals ch ON ch.uid=hc.chemical_uid "+
                "WHERE ch.active=1 AND hc.evidence_status='SOURCE_QUALIFIED' "+
                "AND hc.ghs_pictogram IS NOT NULL",null)){
            while(c.moveToNext()){
                Set<String> values=byUid.get(c.getLong(0));
                if(values==null){values=new LinkedHashSet<>();byUid.put(c.getLong(0),values);}
                addVerified(values,c.getString(1));
            }
        }
        Map<Long,List<String>> out=new HashMap<>();
        for(Map.Entry<Long,Set<String>> x:byUid.entrySet())
            out.put(x.getKey(),Collections.unmodifiableList(ordered(x.getValue())));
        return out;
    }
    public static List<String> forChemical(ExperimentsDb db,long uid){
        if(uid<=0)return Collections.emptyList();
        Map<Long,List<String>> found=allQualified(db);
        List<String> result=found.get(uid);
        return result==null?Collections.emptyList():result;
    }
    public static JSONArray asJson(List<String> codes){
        JSONArray a=new JSONArray();
        if(codes!=null)for(String c:codes)if(valid(c))a.put(c);
        return a;
    }
    public static List<String> fromJson(JSONArray codes){
        Set<String> out=new LinkedHashSet<>();
        if(codes!=null)for(int i=0;i<codes.length();i++)
            addVerified(out,codes.optString(i,""));
        return ordered(out);
    }
    public static int addImages(LinearLayout parent,Context context,List<String> codes,
                                int sizeDp,int maxSymbols){
        if(codes==null||parent==null||context==null)return 0;
        float density=context.getResources().getDisplayMetrics().density;
        int px=Math.round(Math.max(16,sizeDp)*density);
        int count=0;
        for(String code:codes){
            if(!valid(code)||count>=maxSymbols)continue;
            ImageView image=new ImageView(context);
            image.setImageResource(resource(code));
            image.setContentDescription(code+" — "+arabic(code));
            image.setAdjustViewBounds(true);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(px,px);
            p.setMargins(Math.round(2*density),0,Math.round(2*density),0);
            p.gravity=Gravity.CENTER_VERTICAL;
            parent.addView(image,p);count++;
        }
        return count;
    }
    public static int drawOnPdf(Canvas canvas,Context context,List<String> codes,
                                float startX,float top,float sizePt,float gapPt,int limit){
        if(canvas==null||context==null||codes==null)return 0;
        int shown=0;
        for(String code:codes){
            if(!valid(code)||shown>=limit)continue;
            Drawable d=context.getDrawable(resource(code));
            if(d==null)continue;
            canvas.save();
            // Draw at report-point coordinates independent of Android display DPI.
            canvas.translate(startX+shown*(sizePt+gapPt),top);
            canvas.scale(sizePt/96f,sizePt/96f);
            d.setBounds(0,0,96,96);d.draw(canvas);
            canvas.restore();shown++;
        }
        return shown;
    }
}
