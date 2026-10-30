package com.servoc.app.report;

import org.json.JSONObject;
import java.io.OutputStream;

/**
 * Stable production entrypoint for R02 physical snapshot export.
 * V7 changes the PDF layout only. Its sensor payload and upstream measurement
 * logic remain unchanged. Missing values print "-", and 15 source indicators,
 * including PM1/PM2.5/PM10, are always represented in the PDF.
 *
 * Scientific decisions, calculated altitude, environmental exposure
 * thresholds and temporal series are NEVER created by this adapter.
 */
public final class TankPhysicalSnapshotPdf {
    private TankPhysicalSnapshotPdf(){}
    public static void write(OutputStream out,JSONObject frozenPhysicalSnapshot,
                             String originalMeasurementTimeUtc) throws Exception{
        V7PhysicalReportPdfWriter.write(out,frozenPhysicalSnapshot,
            originalMeasurementTimeUtc);
    }
}
