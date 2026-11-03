package com.servoc.app.ui;

import static org.junit.Assert.*;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

/**
 * F01/F02 Android UI smoke: settings R0 entry/back controls must not crash.
 * Host has NO robot connection and cannot perform any physical calibration.
 */
@RunWith(AndroidJUnit4.class)
public final class V8SettingsR0NavigationInstrumentedTest {
    private static Button button(View v,String label) {
        if(v instanceof Button&&label.equals(((Button)v).getText().toString()))
            return (Button)v;
        if(v instanceof ViewGroup){
            ViewGroup group=(ViewGroup)v;
            for(int i=0;i<group.getChildCount();i++){
                Button candidate=button(group.getChildAt(i),label);
                if(candidate!=null)return candidate;
            }
        }
        return null;
    }
    private static boolean containsText(View v,String phrase){
        if(v instanceof android.widget.TextView&&
            ((android.widget.TextView)v).getText().toString().contains(phrase))return true;
        if(v instanceof ViewGroup){
            ViewGroup group=(ViewGroup)v;
            for(int i=0;i<group.getChildCount();i++)
                if(containsText(group.getChildAt(i),phrase))return true;
        }
        return false;
    }
    @Test public void settingsR0OpensAllEightChannelsAndBackReturns()throws Exception{
        Context app=InstrumentationRegistry.getInstrumentation().getTargetContext();
        final HazRobHost mock=(HazRobHost)Proxy.newProxyInstance(
            HazRobHost.class.getClassLoader(),new Class[]{HazRobHost.class},
            (proxy,method,args)->{
                if("preferences".equals(method.getName()))
                    return app.getSharedPreferences("v8-settings-r0-smoke-isolated",0);
                if("latestDiagnostics".equals(method.getName()))
                    return new JSONObject();
                if(method.getReturnType()==boolean.class)return false;
                if(method.getReturnType()==int.class)return 0;
                return null;
            });
        AtomicReference<Throwable> problem=new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            SettingsTab settings=null;
            try{
                settings=new SettingsTab(app,mock);
                View root=settings.view();
                assertTrue(root instanceof FrameLayout);
                assertEquals(1,((FrameLayout)root).getChildCount());
                Button open=button(root,"فتح معايرة R0");
                assertNotNull("Settings must expose R0 entry",open);
                assertTrue(open.performClick());
                assertEquals(2,((FrameLayout)root).getChildCount());
                assertTrue("All eight MQ channels available",containsText(root,"MQ135"));
                assertTrue(containsText(root,"MQ8"));
                assertTrue(containsText(root,"3 دقائق تسخين"));
                Button back=button(root,"← العودة إلى الإعدادات");
                assertNotNull(back);
                assertTrue(back.performClick());
                assertEquals(View.GONE,((FrameLayout)root).getChildAt(1).getVisibility());
                assertEquals(View.VISIBLE,((FrameLayout)root).getChildAt(0).getVisibility());
                // Re-entering must reuse the same controller, not create duplicates.
                assertTrue(open.performClick());
                assertEquals(2,((FrameLayout)root).getChildCount());
            }catch(Throwable ex){problem.set(ex);}
            finally{if(settings!=null)settings.release();}
        });
        if(problem.get()!=null)throw new AssertionError(
            "Settings > R0 UI navigation/regression failed",problem.get());
    }
}
