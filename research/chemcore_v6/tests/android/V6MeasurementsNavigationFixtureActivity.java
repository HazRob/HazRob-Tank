package com.servoc.app.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.content.Context;

import com.servoc.app.data.ExperimentsDb;
import java.io.FileOutputStream;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

/**
 * Dedicated disposable API31 Android UI test only; not a release activity.
 * Click all five REAL production MeasurementsTab buttons, then verify meat /
 * fruit selector is reachable without the gas-analysis engine or hardware.
 */
public final class V6MeasurementsNavigationFixtureActivity extends Activity {
    private int checks=0;
    private ExperimentsDb db;
    private ExecutorService io;
    private void require(boolean good,String text){
        checks++;
        if(!good)throw new AssertionError("N14-"+checks+": "+text);
    }
    private HazRobHost host(){
        InvocationHandler handler=(proxy,method,args)->{
            switch(method.getName()){
                case "database":return db;
                case "preferences":return getSharedPreferences("n14-ui-tests",MODE_PRIVATE);
                case "io":return io;
                case "latestDiagnostics":return new JSONObject();
                case "trackingTargetId":return -1;
                case "trackingTargetName":return "";
                case "operatorRole":return "USER";
                case "hashCode":return System.identityHashCode(proxy);
                case "equals":return proxy==args[0];
                case "toString":return "N14-FakeHost";
                default:
                    Class<?> result=method.getReturnType();
                    if(result==boolean.class)return false;
                    if(result==int.class)return 0;
                    if(result==long.class)return 0L;
                    if(result==double.class)return 0.0;
                    if(result==float.class)return 0f;
                    return null;
            }
        };
        return (HazRobHost)Proxy.newProxyInstance(
            HazRobHost.class.getClassLoader(),new Class[]{HazRobHost.class},handler);
    }
    private static void buttons(View v,List<Button> found){
        if(v instanceof Button)found.add((Button)v);
        if(v instanceof ViewGroup){
            ViewGroup group=(ViewGroup)v;
            for(int i=0;i<group.getChildCount();i++)buttons(group.getChildAt(i),found);
        }
    }
    private static void spinners(View v,List<Spinner> found){
        if(v instanceof Spinner)found.add((Spinner)v);
        if(v instanceof ViewGroup){
            ViewGroup group=(ViewGroup)v;
            for(int i=0;i<group.getChildCount();i++)spinners(group.getChildAt(i),found);
        }
    }
    private Button findButton(View view,String term){
        ArrayList<Button> options=new ArrayList<>();
        buttons(view,options);
        for(Button b:options)if(b.getText().toString().contains(term))return b;
        return null;
    }
    private void execute()throws Exception {
        db=new ExperimentsDb(this);
        io=Executors.newSingleThreadExecutor();
        MeasurementsTab tab=new MeasurementsTab(this,host());
        setContentView(tab.view());
        View root=tab.view();
        String[] names={"البحث والتتبع","التحليل","الفيزيائية","الجزيئات","نضارة الأغذية"};
        ArrayList<Button> top=new ArrayList<>();
        buttons(root,top);
        int n=0;
        for(String name:names)if(findButton(root,name)!=null)n++;
        require(n==5,"all five main measurement navigation controls exist");
        for(String name:names){
            Button button=findButton(root,name);
            require(button!=null&&button.isEnabled(),"main tab accessible "+name);
            require(button.performClick(),"main navigation responds "+name);
        }
        require(findButton(root,"بدء الفحص")!=null,
            "food-freshness tab is actually displayed after fifth button click");
        List<Spinner> selectors=new ArrayList<>();
        spinners(root,selectors);
        boolean modeFound=false;
        for(Spinner s:selectors){
            if(s.getAdapter()==null||s.getAdapter().getCount()!=2)continue;
            String a=String.valueOf(s.getAdapter().getItem(0));
            String b=String.valueOf(s.getAdapter().getItem(1));
            if(a.contains("اللحوم")&&b.contains("الفواكه")){
                modeFound=true;
                require(s.isEnabled(),"operator can select food protocol");
                s.setSelection(1);
                require(s.getSelectedItemPosition()==1,
                    "fruit protocol is selectable, not merely a static label");
                s.setSelection(0);
                require(s.getSelectedItemPosition()==0,
                    "meat protocol remains separately selectable");
                break;
            }
        }
        require(modeFound,"independent meat/fruit dropdown is visible in operator UI");
        require(findButton(root,"PDF التقرير")!=null,
            "food PDF control exists and is not a gas ChemCore export");
        JSONObject status=new JSONObject()
            .put("schema","hazrob-tank-v6-n14-navigation/1")
            .put("status","PASS").put("main_navigation_tabs",5)
            .put("food_protocols",2).put("hardware_connected",false)
            .put("gas_chemcore_validation",false)
            .put("assertions",checks);
        try(FileOutputStream o=openFileOutput("n14_measurements_navigation.json",
                Context.MODE_PRIVATE)){
            o.write(status.toString(2).getBytes(StandardCharsets.UTF_8));
        }
    }
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        try{execute();}
        catch(Throwable failure){
            try{
                JSONObject status=new JSONObject().put("status","FAIL")
                    .put("error",failure.toString()).put("assertions",checks);
                try(FileOutputStream o=openFileOutput(
                        "n14_measurements_navigation.json",Context.MODE_PRIVATE)){
                    o.write(status.toString(2).getBytes(StandardCharsets.UTF_8));
                }
            }catch(Exception ignored){}
            android.util.Log.e("N14", "Operator navigation failed",failure);
        }
    }
    @Override public void onDestroy(){
        super.onDestroy();
        if(io!=null)io.shutdownNow();
        if(db!=null)db.close();
    }
}
