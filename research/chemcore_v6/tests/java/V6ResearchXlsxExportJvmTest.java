package com.servoc.app.report;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * N06-C: invoke the SAME source V6 XLSX sheets consumed by Android production
 * report export, then the original pinned SERVOC SmartXlsxPackage OOXML writer.
 * Test observations are explicitly synthetic fixtures; never source G3 evidence.
 */
public final class V6ResearchXlsxExportJvmTest {
    private static int checks;
    private static void check(boolean valid,String message){
        checks++;
        if(!valid)throw new AssertionError(message);
    }
    private static JSONObject specimen(int cycle,int sensor,int hypotheses)throws Exception{
        JSONObject s=new JSONObject()
            .put("sensor_model",new String[]{"MQ2","MQ3","MQ4","MQ5","MQ7","MQ8","MQ9","MQ135"}[sensor])
            .put("state",sensor==7?"INSUFFICIENT_UNIQUE_SOURCE_SCANS":"SOURCE_ONLY_RESEARCH_UNVALIDATED")
            .put("distinct_original_scan_cycles",sensor==7?7:15)
            .put("eligible_original_channel_rows",sensor==7?14:30)
            .put("selected_original_t_ms",1725000000000L+1000L*cycle+sensor)
            .put("selected_original_scan_cycle",cycle*100+sensor)
            .put("selected_original_rs",72.0+sensor+cycle*0.01)
            .put("ratio_ready_channels",sensor%2);
        if(sensor%3==0)s.put("selected_original_r0",JSONObject.NULL);
        else s.put("selected_original_r0",70.0+sensor);
        JSONArray fits=new JSONArray(),disagreements=new JSONArray();
        for(int i=0;i<hypotheses;i++){
            fits.put(new JSONObject()
                .put("source_curve_id","SOURCE-<"+i+">&MQ"+sensor)
                .put("source_root","SOURCE_ROOT_"+i)
                .put("chemical_reference_id","CHEM_REF_"+i)
                .put("gas_or_class","RESEARCH_FIXTURE_CLASS_"+i)
                .put("source_estimate_ppm_research",8.5+i+sensor*0.2)
                .put("outside_original_reference_domain",i%2==0));
        }
        disagreements.put(new JSONObject().put("curve_a","TEST_SOURCE_1")
            .put("curve_b","TEST_SOURCE_2"));
        s.put("source_curve_hypotheses",fits);
        s.put("source_curve_disagreements",disagreements);
        return s;
    }
    private static JSONObject evidence(int n,int hypotheses)throws Exception{
        JSONObject source=new JSONObject().put("schema",V6ResearchXlsxSheets.SCHEMA)
            .put("field_session_id","FS-TEST-ONLY")
            .put("reported_identity","UNKNOWN")
            .put("reportable_ppm",false)
            .put("physical_tank_g3_validated",false)
            .put("food_freshness_included",false)
            .put("no_db_write",true)
            .put("v5_approval_and_decisions_unchanged",true)
            .put("approved_gas_cycles",n);
        JSONArray cycles=new JSONArray();
        for(int i=0;i<n;i++){
            JSONObject cycle=new JSONObject()
                .put("measurement_session_id","MS-FIXTURE-"+i)
                .put("cycle_id","FC-TEST-"+i)
                .put("research_source_sha256",String.format("%064x",i+1));
            JSONArray ss=new JSONArray();
            for(int j=0;j<8;j++)ss.put(specimen(i,j,j==7?0:hypotheses));
            cycle.put("sensors",ss);
            cycles.put(cycle);
        }
        source.put("cycles",cycles);
        return source;
    }
    private static void write(Path output,int n,int hypotheses)throws Exception{
        List<SmartXlsxPackage.Sheet> sheets=new ArrayList<>();
        SmartXlsxPackage.Grid legacy=new SmartXlsxPackage.Grid(27,40);
        legacy.title("V5 legacy approved result — fixture, unchanged");
        legacy.row("LEGACY_V5_APPROVED_MARKER","preserve alongside V6");
        sheets.add(new SmartXlsxPackage.Sheet("V5 محفوظة",legacy.finish()));
        JSONObject source=evidence(n,hypotheses);
        int appended=V6ResearchXlsxSheets.append(sheets,
            "الموقع الاختباري","SITE-TEST",
            "الجلسة الاختبارية","FS-TEST-ONLY",source);
        check(appended==2&&sheets.size()==3,"both V6 sheets are additive to legacy sheet");
        check(sheets.get(1).xml.contains("UNKNOWN"),"UNKNOWN qualification present");
        check(sheets.get(2).xml.contains("SOURCE-&lt;0&gt;&amp;MQ0"),
            "OOXML producer escapes XML-special reference IDs");
        check(sheets.get(2).xml.contains("RESEARCH_FIXTURE_CLASS_0"),
            "actual V6 source hypotheses preserved");
        check(sheets.get(1).xml.contains("FS-TEST-ONLY"),
            "approved identity retained on every V6 sheet");
        try(FileOutputStream out=new FileOutputStream(output.toFile())){
            SmartXlsxPackage.write(out,sheets);
        }
        check(output.toFile().length()>4500,"real XLSX ZIP bytes produced");
        System.out.println("PASS V6_REAL_XLSX_OOXML "+output+
            " fixture_cycles="+n+" fixture_source_fits="+(n*7*hypotheses)+
            " tests="+checks+" bytes="+output.toFile().length());
    }
    public static void main(String[] args)throws Exception{
        check(args.length==2,"small and large actual XLSX output paths");
        JSONObject valid=evidence(2,2);
        List<SmartXlsxPackage.Sheet> sheets=new ArrayList<>();
        JSONObject wrongId=new JSONObject(valid.toString()).put("field_session_id","FS-INTRUDER");
        try{
            V6ResearchXlsxSheets.append(sheets,"x","site","y","FS-TEST-ONLY",wrongId);
            throw new AssertionError("foreign approved field session injected into XLSX");
        }catch(IllegalArgumentException expected){checks++;}
        JSONObject falsePpm=new JSONObject(valid.toString()).put("reportable_ppm",true);
        try{
            V6ResearchXlsxSheets.append(sheets,"x","site","y","FS-TEST-ONLY",falsePpm);
            throw new AssertionError("certified ppm impossible in research XLSX");
        }catch(IllegalArgumentException expected){checks++;}
        JSONObject falseG3=new JSONObject(valid.toString()).put("physical_tank_g3_validated",true);
        try{
            V6ResearchXlsxSheets.append(sheets,"x","site","y","FS-TEST-ONLY",falseG3);
            throw new AssertionError("G3 promotion impossible in research XLSX");
        }catch(IllegalArgumentException expected){checks++;}
        JSONObject wrongCount=new JSONObject(valid.toString()).put("approved_gas_cycles",3);
        try{
            V6ResearchXlsxSheets.append(sheets,"x","site","y","FS-TEST-ONLY",wrongCount);
            throw new AssertionError("approved cycle mismatch not allowed");
        }catch(IllegalArgumentException expected){checks++;}
        JSONObject noGas=evidence(0,0);
        check(V6ResearchXlsxSheets.append(sheets,"x","site","y","FS-TEST-ONLY",noGas)==0&&
            sheets.isEmpty(),"zero approved gas cycles must not create V6 sheets");
        write(Path.of(args[0]),3,2);
        write(Path.of(args[1]),100,4);
        System.out.println("PASS N06C_REAL_PRODUCTION_SHEET_GENERATOR all "+checks+
            " actual OOXML writer and fail closed contract checks; no real Tank data/G3");
    }
}
