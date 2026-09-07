package com.simplestsoft.twostrokecalc.ui.screens.tools

import com.simplestsoft.twostrokecalc.domain.model.ToolId
import com.simplestsoft.twostrokecalc.domain.model.ToolMeasurementSession
import java.util.Locale
import org.json.JSONObject

fun toolSessionMetrics(session: ToolMeasurementSession): String {
    if (session.resultJson.isBlank()) return ""
    return runCatching {
        val data = JSONObject(session.resultJson)
        when (session.toolId) {
            ToolId.GPS_DYNO -> formatDynoMetrics(data)
            ToolId.RPM_TACHOMETER -> formatRpmMetrics(data)
            ToolId.VIBRATION_ANALYZER -> formatVibrationMetrics(data)
            ToolId.ANGLE_METER -> formatAngleMetrics(data)
            ToolId.PORT_TIMING_ASSIST -> formatPortTimingMetrics(data)
            ToolId.COMMUNITY_SETUPS -> ""
        }
    }.getOrDefault("")
}

private fun formatDynoMetrics(data: JSONObject): String {
    return buildList {
        if (data.has("peakPs")) {
            add(String.format(Locale.getDefault(), "%.1f PS", data.getDouble("peakPs")))
        }
        if (data.has("maxKmh")) {
            add(String.format(Locale.getDefault(), "%.1f km/h", data.getDouble("maxKmh")))
        }
        if (data.has("confidence")) {
            add("${(data.getDouble("confidence") * 100).toInt()}%")
        }
    }.joinToString(" · ")
}

private fun formatRpmMetrics(data: JSONObject): String {
    return buildList {
        if (data.has("rpm")) {
            add("${data.getDouble("rpm").toInt()} RPM")
        }
        if (data.has("peak")) {
            add("Peak ${data.getDouble("peak").toInt()} RPM")
        }
    }.joinToString(" · ")
}

private fun formatVibrationMetrics(data: JSONObject): String {
    return buildList {
        if (data.has("dominantHz")) {
            add(String.format(Locale.getDefault(), "%.1f Hz", data.getDouble("dominantHz")))
        }
        if (data.has("rms")) {
            add(String.format(Locale.getDefault(), "RMS %.2f", data.getDouble("rms")))
        }
    }.joinToString(" · ")
}

private fun formatAngleMetrics(data: JSONObject): String {
    return if (data.has("degrees")) {
        String.format(Locale.getDefault(), "%.1f°", data.getDouble("degrees"))
    } else {
        ""
    }
}

private fun formatPortTimingMetrics(data: JSONObject): String {
    return buildList {
        if (data.has("stroke")) {
            add(String.format(Locale.getDefault(), "Hub %.1f mm", data.getDouble("stroke")))
        }
        if (data.has("exhaust")) {
            add(String.format(Locale.getDefault(), "A %.1f mm", data.getDouble("exhaust")))
        }
        if (data.has("transfer")) {
            add(String.format(Locale.getDefault(), "U %.1f mm", data.getDouble("transfer")))
        }
    }.joinToString(" · ")
}
