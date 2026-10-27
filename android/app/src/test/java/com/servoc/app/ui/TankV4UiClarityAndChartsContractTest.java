package com.servoc.app.ui;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/**
 * Source-level UI regressions for V4. Screenshot and usability validation on
 * Android devices remain an independent release-acceptance requirement.
 */
public final class TankV4UiClarityAndChartsContractTest {
    private static String src(String name)throws Exception{
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/"+name)),StandardCharsets.UTF_8);
    }
    @Test public void allFiveDeveloperTabsUseReadableScrollNavigation()throws Exception{
        String s=src("ui/DeveloperTab.java");
        assertTrue(s.contains("HorizontalScrollView strip=new HorizontalScrollView(c)"));
        assertTrue(s.contains("b.setTextSize(13)"));
        assertTrue(s.contains("b.setMinWidth(UiKit.dp(context,96))"));
        assertTrue(s.contains("tabs[i].setTextColor(Color.BLACK)"));
        assertTrue(s.contains("tabs[i].setBackground(UiKit.outline("));
        assertFalse(s.contains("b.setTextSize(9)"));
    }
    @Test public void centralLibraryTabsReadableOnSmallScreens()throws Exception{
        String s=src("ui/CentralLibraryDashboardTab.java");
        assertTrue(s.contains("HorizontalScrollView strip=new HorizontalScrollView(context)"));
        assertTrue(s.contains("tabs[i].setTextSize(13)"));
        assertTrue(s.contains("tabs[i].setMinWidth(UiKit.dp(context,105))"));
        assertFalse(s.contains("tabs[i].setTextSize(9)"));
    }
    @Test public void reportChartsDoNotReappearOnOldOrDestroyedScreens()throws Exception{
        String r=src("ui/ReportsTab.java");
        String main=src("MainActivity.java");
        assertTrue(r.contains("private volatile long chartGeneration=0"));
        assertTrue(r.contains("private volatile boolean released=false"));
        assertTrue(r.contains("if(released||chartGeneration!=request)return"));
        assertTrue(r.contains("if(released||chartGeneration!=request||selectedId!=experimentId){b.recycle();return;}"));
        assertTrue(r.contains("chartView.setScaleType(ImageView.ScaleType.FIT_CENTER)"));
        assertTrue(r.contains("تعذر العثور على جلسة المخطط المحفوظة"));
        assertTrue(main.contains("if(reportsTab!=null)reportsTab.release()"));
    }
    @Test public void chartChoicesExcludeUnsupportedOrInventedSeries()throws Exception{
        String chart=src("report/TankInstantCharts.java");
        String eligibility=src("report/TankChartEligibility.java");
        assertTrue(chart.contains("if(rows.isEmpty())throw new IllegalArgumentException"));
        assertFalse(chart.contains("Math.random("));
        assertTrue(eligibility.contains("if(chart<0||chart>19)return false"));
        assertTrue(eligibility.contains("if(series==null||!series.usable())return false"));
    }
}
