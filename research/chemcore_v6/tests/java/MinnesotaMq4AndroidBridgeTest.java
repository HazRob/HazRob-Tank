package com.hazrob.tank.v6integration;

import com.hazrob.tank.chemcore.v6.ChemCoreTankV6;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * N06-A source evidence replay: REAL Minnesota MQ4 physical Ohm resistances
 * through the EXACT Android JSON boundary TankV6TelemetryAdapter.
 *
 * Original three MQ4 units are replayed separately, because the actual Tank
 * hardware has ONE installed MQ4; never invent three installed MQ4 channels.
 * Original Ω / 1000 -> Tank kΩ schema at the boundary.
 * No source-specific R0, Li-Cor labels or physical G3 enter the coordinator.
 * A source-only Java JDK org.json library simulates Android's JSON API; this
 * is NOT a phone, UI render, PDF render or actual Tank hardware execution.
 */
public final class MinnesotaMq4AndroidBridgeTest {
    private MinnesotaMq4AndroidBridgeTest(){}
    private static int asserts=0;
    private static void require(boolean flag,String reason){
        asserts++;
        if(!flag)throw new AssertionError("MQ4 adapter assertion "+asserts+": "+reason);
    }
    private static double finite(String text){
        double value=Double.parseDouble(text);
        require(Double.isFinite(value),"source numeric finite");
        return value;
    }
    private static JSONObject jsonInput(double rsKohm,double t,double h,
                                   boolean valid,boolean powered,Double r0){
        JSONObject root=new JSONObject(),mq=new JSONObject(),sensor=new JSONObject();
        sensor.put("rs",rsKohm);
        sensor.put("ok",valid);
        sensor.put("powered",powered);
        if(r0!=null)sensor.put("r0",r0);
        mq.put("mq4",sensor);root.put("mq",mq);
        root.put("env",new JSONObject().put("t",t).put("h",h));
        return root;
    }
    private static void assertUnknownRawResult(ChemCoreTankV6.Result result,JSONObject evidence,
                                               double originalKohm){
        require(result.responseCurves.observedPhysicalChannels==1,
                "one actual Tank MQ4 channel, not three fabricated channels");
        require(result.responseCurves.ratioReadyChannels==0,
                "missing R0 must never yield qualified Rs/R0");
        require(result.responseCurves.rawChannelsAwaitingR0==1,
                "valid original Rs kept in raw-only ledger");
        require(result.responseCurves.allSourceFits.isEmpty()&&
                result.responseCurves.rankedCandidates.isEmpty(),
                "no gas identity from raw MQ4 resistance without R0");
        require(!result.reportablePpm&&!result.physicalCalibrationProven&&
                !result.foodModelAttached,
                "no certified gas ppm/G3 and food engine excluded");
        require(!evidence.optBoolean("reportable_ppm",true)&&
                !evidence.optBoolean("physical_calibration_proven",true)&&
                !evidence.optBoolean("food_models_included",true),
                "Android bridge research-only claim");
        require(evidence.optInt("observed_mq_channels",-1)==1&&
                evidence.optInt("raw_only_mq_channels",-1)==1&&
                evidence.optInt("ratio_ready_mq_channels",-1)==0,
                "source channel audit preserved in JSON");
        JSONArray rows=evidence.optJSONArray("original_mq_channel_evidence");
        require(rows!=null&&rows.length()==1,"one original MQ4 evidence row");
        JSONObject row=rows.getJSONObject(0);
        require(row.optString("sensor_model","").equals("MQ4")&&
                Math.abs(row.optDouble("rs_as_received")-originalKohm)<1e-9,
                "exact physical source MQ4 resistance converted Ω to kΩ");
        require(row.isNull("r0_as_received")&&
                !row.optBoolean("rs_r0_ratio_ready",true),
                "Android source evidence explicitly NULL R0, never simulated");
        require(evidence.optJSONArray("source_curve_hypotheses").length()==0&&
                evidence.optJSONArray("chemical_evidence_ledger").length()==0,
                "no bogus source gas identity candidates");
    }
    public static void main(String[] args)throws Exception {
        require(args.length==2,"input original Minnesota CSV and proof CSV");
        Path raw=Path.of(args[0]);
        int inputRows=0,physicalSensorReplays=0,passedRows=0;
        try(BufferedReader reader=Files.newBufferedReader(raw,StandardCharsets.UTF_8);
            BufferedWriter out=Files.newBufferedWriter(Path.of(args[1]),StandardCharsets.UTF_8)){
            String header=reader.readLine();
            require(header!=null&&header.equals(
                "datetime,licor_ch4,licor_h2o,t,rh,2600_1,2600_2,2600_3,"+
                "2602_1,2602_2,2602_3,2611C_1,2611C_2,2611C_3,"+
                "2611E_1,2611E_2,2611E_3,MQ4_1,MQ4_2,MQ4_3"),
                "exact original 20-column schema");
            out.write("source_row_id,source_datetime,original_mq4_unit,"+
                "original_rs_ohm,android_rs_kohm,source_r0_present,"+
                "v6_observed_mq4_channels,v6_ratio_ready,v6_source_fits,"+
                "v6_gas_identity_candidate_count,v6_reportable_ppm,"+
                "v6_physical_g3_qualified,v6_food_used\n");
            String line;
            while((line=reader.readLine())!=null){
                String[] cells=line.split(",",-1);
                require(cells.length==20,"original 20 columns for each source row");
                double temp=finite(cells[3]),humidity=finite(cells[4]);
                // Li-Cor methane reference NEVER reaches the Android adapter.
                for(int unit=0;unit<3;unit++){
                    double rsOhm=finite(cells[17+unit]);
                    require(rsOhm>0,"actual original MQ4 physical resistance");
                    double kohm=rsOhm/1000.0;
                    JSONObject snapshot=jsonInput(kohm,temp,humidity,true,true,null);
                    require(!snapshot.has("licor_ch4")&&
                            !snapshot.toString().contains("licor_ch4"),
                            "independent Li-Cor reference never used as sensor input");
                    ChemCoreTankV6.Result v6=TankV6TelemetryAdapter.analyzeSnapshot(snapshot);
                    JSONObject evidence=TankV6TelemetryAdapter.toJson(v6);
                    assertUnknownRawResult(v6,evidence,kohm);
                    out.write(String.format(Locale.ROOT,
                        "%d,%s,MQ4_%d,%.10f,%.10f,false,1,0,0,0,false,false,false%n",
                        inputRows,cells[0],unit+1,rsOhm,kohm));
                    physicalSensorReplays++;
                }
                passedRows++;inputRows++;
            }
        }
        require(inputRows==14186&&passedRows==14186&&physicalSensorReplays==42558,
                "all source rows * three physically distinct MQ4 source units");
        // Negative-eligibility controls: known finite Rs must not be accepted when
        // the ESP32 marks a sensor invalid or explicitly unpowered.
        for(boolean valid:new boolean[]{false,true}){
            for(boolean powered:new boolean[]{false,true}){
                JSONObject input=jsonInput(80.0,26.0,45.0,valid,powered,null);
                var data=TankV6TelemetryAdapter.analyzeSnapshot(input);
                int expected=valid&&powered?1:0;
                require(data.responseCurves.observedPhysicalChannels==expected,
                    "invalid/unpowered original Rs excluded before any V6 calculation");
            }
        }
        JSONObject invalid=new JSONObject();
        invalid.put("mq",new JSONObject().put("mq4",
            new JSONObject().put("rs",-1.0).put("ok",true).put("powered",true)));
        require(TankV6TelemetryAdapter.analyzeSnapshot(invalid).responseCurves
                 .observedPhysicalChannels==0,"invalid negative Rs refused");
        // Research-only R0 injection is NOT source data and is NEVER counted as
        // source validation. Test only that even source fits are non-certified.
        JSONObject synthetic=jsonInput(82.0,25,50,true,true,82.0);
        JSONObject report=TankV6TelemetryAdapter.toJson(
            TankV6TelemetryAdapter.analyzeSnapshot(synthetic));
        require(report.optInt("ratio_ready_mq_channels",0)==1,
            "ratio mathematically possible only in explicitly synthetic fixture");
        require(!report.optBoolean("reportable_ppm",true)&&
                !report.optBoolean("physical_calibration_proven",true),
                "even synthetic R0 never promotes Tank ppm/G3");
        System.out.println("PASS N06-A ORIGINAL MQ4_ANDROID_JSON_BRIDGE: "+
            inputRows+" original minutes, "+physicalSensorReplays+
            " separate physical MQ4 source resistances, "+asserts+
            " assertions; invalid/unpowered/refused, no R0/ppm/G3/food.");
    }
}
