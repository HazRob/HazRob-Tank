package com.servoc.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import com.hazrob.tank.emissions.EmissionEngineConfig;

/** Captures one independent engine's IFC characteristics at creation time.
 * Optional fields are left null instead of inventing a generator rating or
 * assuming a dry 15% O2-normalized exhaust sample.
 */
final class EmissionEngineEditor {
    interface Listener {
        void accepted(String name,EmissionEngineConfig config)throws Exception;
        void failure(String reason);
    }
    private EmissionEngineEditor(){}

    private static TextView note(Context context,String message){
        TextView t=UiKit.small(context,message);
        t.setTextColor(0xFF36475A);
        return t;
    }
    private static void heading(Context c,LinearLayout box,String label){
        TextView t=UiKit.text(c,label);
        t.setTextColor(0xFF17263A);
        box.addView(t,UiKit.full(c));
    }
    private static EditText field(Context c,LinearLayout box,String title,String placeholder){
        heading(c,box,title);
        EditText e=UiKit.input(c,"");
        e.setSingleLine(true);
        e.setHint(placeholder);
        e.setInputType(InputType.TYPE_CLASS_NUMBER |
            InputType.TYPE_NUMBER_FLAG_DECIMAL);
        box.addView(e,UiKit.full(c));
        return e;
    }
    private static Double optional(EditText input){
        String value=input.getText().toString().trim().replace(',','.');
        if(value.isEmpty())return null;
        double number=Double.parseDouble(value);
        if(!Double.isFinite(number))
            throw new IllegalArgumentException("UNSUPPORTED_NUMBER");
        return number;
    }
    static void create(Context c,Listener listener){
        ScrollView scroll=new ScrollView(c);
        LinearLayout box=UiKit.vbox(c);
        box.setPadding(UiKit.dp(c,12),UiKit.dp(c,8),
            UiKit.dp(c,12),UiKit.dp(c,20));
        scroll.addView(box);
        box.addView(note(c,"يُستحدث سجل مستقل لهذا المحرك، ويسمح بقياس واحد فقط. "+
            "يُستخدم تعريفه لتحديد المرجع المقابل في IFC دون تغيير نتائج المنحنيات. "+
            "المجالات المدخلة تُعد معلومات صرّح بها المستخدم ولا تثبت إجراء معايرة للعادم."),
            UiKit.full(c));
        heading(c,box,"اسم المحرك أو المولد *");
        EditText name=UiKit.input(c,"");
        name.setHint("مثال: مولد الديزل رقم 1");
        name.setSingleLine(true);
        box.addView(name,UiKit.full(c));

        heading(c,box,"نوع الوقود *");
        Spinner fuel=new Spinner(c);UiKit.outlinedSpinner(fuel);
        fuel.setAdapter(UiKit.lightOptions(c,new String[]{
            "غير محدد","ديزل / وقود سائل","غازي"}));
        box.addView(fuel,UiKit.full(c));

        LinearLayout liquid=UiKit.vbox(c);
        EditText bore=field(c,liquid,
            "قطر أسطوانة المحرك (ملم) — لمحركات الوقود السائل",
            "مثال: 105 أو 420");
        liquid.addView(note(c,"القطر الأقل من 400 ملم أو 400 ملم فأكثر يحدد "+
            "فئة حد NOx لمحركات الوقود السائل."),UiKit.full(c));
        box.addView(liquid,UiKit.full(c));

        LinearLayout gas=UiKit.vbox(c);
        heading(c,gas,"تقنية محرك الغاز");
        Spinner ignition=new Spinner(c);UiKit.outlinedSpinner(ignition);
        ignition.setAdapter(UiKit.lightOptions(c,new String[]{
            "غير معروفة","إشعال شراري","وقود مزدوج","إشعال بالانضغاط"}));
        gas.addView(ignition,UiKit.full(c));
        box.addView(gas,UiKit.full(c));

        heading(c,box,"بيانات انطباق IFC — أدخل القيم المعروفة فقط");
        EditText capacity=field(c,box,"القدرة الحرارية الإجمالية للمنشأة (MWth)",
            "مثال: 5");
        EditText hours=field(c,box,"ساعات التشغيل السنوية (ساعة/سنة)",
            "مثال: 1800");
        EditText utilization=field(c,box,"نسبة استغلال القدرة السنوية (%)",
            "مثال: 70");
        box.addView(note(c,"جدول IFC 1.1.2 المخصص لهذه الفئة يتطلب تحقق نطاق "+
            "3–50 MWth، والتشغيل أكثر من 500 ساعة/سنة، والاستغلال أكثر من 30%."),
            UiKit.full(c));

        EditText sulfur=field(c,box,"نسبة الكبريت في الوقود (%) — إن كانت معلومة",
            "مثال: 0.5");
        EditText oxygen=field(c,box,"O₂ المقاس في العادم (%) — إن كان معلومًا",
            "اتركه فارغًا إن لم يُقَس");
        CheckBox dry=new CheckBox(c);
        dry.setText("تتوفر قياسات عادم مصححة إلى الحالة الجافة والظروف المرجعية");
        dry.setTextColor(0xFF17263A);
        box.addView(dry,UiKit.full(c));
        box.addView(note(c,"تسجيل O₂ أو اختيار التصحيح لا يولّد قياسًا مخبريًا "+
            "من عدم؛ حدود IFC بوحدة mg/Nm³ عند شروط مرجعية محددة."),
            UiKit.full(c));

        fuel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(AdapterView<?> parent,View item,
                                                  int position,long id){
                liquid.setVisibility(position==1?View.VISIBLE:View.GONE);
                gas.setVisibility(position==2?View.VISIBLE:View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> parent){}
        });
        liquid.setVisibility(View.GONE);gas.setVisibility(View.GONE);

        AlertDialog dialog=new AlertDialog.Builder(c)
            .setTitle("استحداث محرك مستقل — تعريف IFC")
            .setView(scroll)
            .setNegativeButton("إلغاء",(d,w)->{})
            .setPositiveButton("تأكيد",null).create();
        dialog.setOnShowListener(ignored->{
            Button approve=dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            approve.setOnClickListener(v->{
                try{
                    String engineName=name.getText().toString().trim();
                    if(engineName.isEmpty()||engineName.length()>80)
                        throw new IllegalArgumentException("اسم المحرك مطلوب (80 حرفًا كحد أقصى)");
                    int kind=fuel.getSelectedItemPosition();
                    String fuelCode=kind==1?EmissionEngineConfig.LIQUID:
                        kind==2?EmissionEngineConfig.GAS:EmissionEngineConfig.UNKNOWN;
                    int gasKind=ignition.getSelectedItemPosition();
                    String firing=gasKind==1?EmissionEngineConfig.SPARK:
                        gasKind==2?EmissionEngineConfig.DUAL:
                        gasKind==3?EmissionEngineConfig.COMPRESSION:
                            EmissionEngineConfig.UNSPECIFIED;
                    EmissionEngineConfig config=EmissionEngineConfig.of(fuelCode,
                        kind==2?firing:EmissionEngineConfig.UNSPECIFIED,
                        kind==1?optional(bore):null,
                        optional(capacity),optional(hours),optional(utilization),
                        optional(sulfur),optional(oxygen),dry.isChecked());
                    listener.accepted(engineName,config);
                    dialog.dismiss();
                }catch(NumberFormatException badNumber){
                    listener.failure("تحقق من الحقول الرقمية المدخلة");
                }catch(Exception bad){
                    String msg=bad.getMessage();
                    listener.failure(msg==null?"تعذر حفظ بيانات المحرك":msg);
                }
            });
        });
        dialog.show();
    }
}
