package com.servoc.app.measurement;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.field.FieldCycleRepository;
import com.servoc.app.ui.HazRobHost;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Proxy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * F02 Android runtime acceptance: when the host worker pool has shut down,
 * commands must not strand the busy UI or a shared field-operation lease.
 * This test uses isolated on-device SQLite, but NO robot/relay or live capture.
 */
@RunWith(AndroidJUnit4.class)
public final class V8FieldRejectedExecutorInstrumentedTest {
    @Test public void failedSchedulingLeavesCommandControlsRecoverable() throws Exception {
        Context app=InstrumentationRegistry.getInstrumentation().getTargetContext();
        final String isolated="v8_rejected_executor_"+System.nanoTime()+".db";
        Context testContext=new ContextWrapper(app) {
            @Override public SQLiteDatabase openOrCreateDatabase(
                    String name,int mode,SQLiteDatabase.CursorFactory factory){
                return app.openOrCreateDatabase(isolated,mode,factory);
            }
            @Override public SQLiteDatabase openOrCreateDatabase(String name,int mode,
                    SQLiteDatabase.CursorFactory factory,DatabaseErrorHandler errorHandler){
                return app.openOrCreateDatabase(isolated,mode,factory,errorHandler);
            }
        };
        ExperimentsDb db=new ExperimentsDb(testContext);
        ExecutorService rejecting=Executors.newSingleThreadExecutor();
        rejecting.shutdown();
        HazRobHost host=(HazRobHost)Proxy.newProxyInstance(
            HazRobHost.class.getClassLoader(),new Class[]{HazRobHost.class},
            (proxy,method,args)->{
                switch(method.getName()){
                    case "database": return db;
                    case "io": return rejecting;
                    case "preferences": return app.getSharedPreferences(
                        "v8_field_rejected_executor_test",0);
                    default:
                        if(method.getReturnType()==boolean.class)return false;
                        if(method.getReturnType()==int.class)return 0;
                        return null;
                }
            });
        AtomicReference<String> event=new AtomicReference<>("");
        FieldCycleController controller=new FieldCycleController(host,
            new FieldCycleController.Observer(){
                public void changed(String state,String message,String id){
                    event.set(state+":"+message);
                }
                public void refreshed(){}
            });
        try {
            controller.begin("not_a_real_session",
                new FieldCycleRepository.Options(true,false,true));
            assertFalse("begin rejection must not leave UI busy",controller.busy());
            assertTrue("begin rejection must explain its reason",event.get().startsWith("FAILED:"));

            event.set("");
            controller.retry("not_a_real_cycle");
            assertFalse("retry rejection must not leave UI busy",controller.busy());
            assertTrue(event.get().startsWith("FAILED:"));

            event.set("");
            controller.approve("not_a_real_cycle");
            assertFalse("approve rejection must not leave UI busy",controller.busy());
            assertTrue(event.get().startsWith("FAILED:"));

            event.set("");
            controller.delete("not_a_real_cycle");
            assertFalse("delete rejection must not leave UI busy",controller.busy());
            assertTrue(event.get().startsWith("DELETE_REFUSED:"));
        } finally {
            controller.release();
            db.close();
            app.deleteDatabase(isolated);
        }
    }
}
