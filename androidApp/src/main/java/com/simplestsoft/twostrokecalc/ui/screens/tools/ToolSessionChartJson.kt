package com.simplestsoft.twostrokecalc.ui.screens.tools

import com.simplestsoft.twostrokecalc.domain.tools.dyno.DynoPowerPoint
import com.simplestsoft.twostrokecalc.domain.tools.dyno.DynoSpeedChartPoint
import com.simplestsoft.twostrokecalc.domain.tools.vibration.SpectrumBin
import com.simplestsoft.twostrokecalc.domain.tools.vibration.VibrationAnalysisResult
import org.json.JSONArray
import org.json.JSONObject

data class VibrationRmsChartPoint(
    val elapsedMs: Long,
    val rms: Double,
)

data class DynoSessionCharts(
    val speedChart: List<DynoSpeedChartPoint>,
    val powerChart: List<DynoPowerPoint>,
)

data class VibrationSessionCharts(
    val rmsChart: List<VibrationRmsChartPoint>,
    val spectrum: List<SpectrumBin>,
    val result: VibrationAnalysisResult,
)

object ToolSessionChartJson {
    private const val MAX_CHART_POINTS = 400

    fun encodeDynoResult(
        peakPs: Double,
        maxKmh: Double,
        confidence: Double,
        speedChart: List<DynoSpeedChartPoint>,
        powerCurve: List<DynoPowerPoint>,
    ): String {
        val root = JSONObject()
        root.put("peakPs", peakPs)
        root.put("maxKmh", maxKmh)
        root.put("confidence", confidence)
        root.put("speedChart", encodeSpeedChart(speedChart))
        root.put("powerChart", encodePowerChart(powerCurve))
        return root.toString()
    }

    fun encodeVibrationResult(
        result: VibrationAnalysisResult,
        rmsChart: List<VibrationRmsChartPoint>,
    ): String {
        val root = JSONObject()
        root.put("rms", result.rms)
        root.put("dominantHz", result.dominantFrequencyHz)
        root.put("noiseFloor", result.noiseFloor)
        root.put("hasSignificantVibration", result.hasSignificantVibration)
        root.put("dominantMagnitude", result.dominantMagnitude)
        root.put("durationMs", result.durationMs)
        root.put("rmsChart", encodeRmsChart(rmsChart))
        root.put("spectrum", encodeSpectrum(result.spectrum))
        return root.toString()
    }

    fun parseDynoCharts(resultJson: String): DynoSessionCharts? {
        if (resultJson.isBlank()) return null
        return runCatching {
            val data = JSONObject(resultJson)
            val speedChart = decodeSpeedChart(data.optJSONArray("speedChart"))
            val powerChart = decodePowerChart(data.optJSONArray("powerChart"))
            if (speedChart.size < 2 && powerChart.size < 2) return null
            DynoSessionCharts(
                speedChart = speedChart,
                powerChart = powerChart,
            )
        }.getOrNull()
    }

    fun parseVibrationCharts(resultJson: String): VibrationSessionCharts? {
        if (resultJson.isBlank()) return null
        return runCatching {
            val data = JSONObject(resultJson)
            val rmsChart = decodeRmsChart(data.optJSONArray("rmsChart"))
            val spectrum = decodeSpectrum(data.optJSONArray("spectrum"))
            val result = VibrationAnalysisResult(
                spectrum = spectrum,
                rms = data.optDouble("rms", 0.0),
                dominantFrequencyHz = data.optDouble("dominantHz", 0.0),
                dominantMagnitude = data.optDouble("dominantMagnitude", 0.0),
                durationMs = data.optLong("durationMs", 0L),
                noiseFloor = data.optDouble("noiseFloor", 0.0),
                hasSignificantVibration = data.optBoolean("hasSignificantVibration", false),
            )
            if (rmsChart.size < 2 && spectrum.size < 2) return null
            VibrationSessionCharts(
                rmsChart = rmsChart,
                spectrum = spectrum,
                result = result,
            )
        }.getOrNull()
    }

    private fun encodeSpeedChart(points: List<DynoSpeedChartPoint>): JSONArray {
        val array = JSONArray()
        downsample(points.size, MAX_CHART_POINTS).forEach { index ->
            val point = points[index]
            array.put(
                JSONObject()
                    .put("t", point.elapsedMs)
                    .put("v", point.speedKmh),
            )
        }
        return array
    }

    private fun encodePowerChart(points: List<DynoPowerPoint>): JSONArray {
        val array = JSONArray()
        downsample(points.size, MAX_CHART_POINTS).forEach { index ->
            val point = points[index]
            array.put(
                JSONObject()
                    .put("s", point.speedKmh)
                    .put("p", point.powerPs),
            )
        }
        return array
    }

    private fun encodeRmsChart(points: List<VibrationRmsChartPoint>): JSONArray {
        val array = JSONArray()
        downsample(points.size, MAX_CHART_POINTS).forEach { index ->
            val point = points[index]
            array.put(
                JSONObject()
                    .put("t", point.elapsedMs)
                    .put("v", point.rms),
            )
        }
        return array
    }

    private fun encodeSpectrum(bins: List<SpectrumBin>): JSONArray {
        val array = JSONArray()
        bins.forEach { bin ->
            array.put(
                JSONObject()
                    .put("f", bin.frequencyHz)
                    .put("m", bin.magnitude),
            )
        }
        return array
    }

    private fun decodeSpeedChart(array: JSONArray?): List<DynoSpeedChartPoint> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    DynoSpeedChartPoint(
                        elapsedMs = item.optLong("t"),
                        speedKmh = item.optDouble("v"),
                    ),
                )
            }
        }
    }

    private fun decodePowerChart(array: JSONArray?): List<DynoPowerPoint> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    DynoPowerPoint(
                        speedKmh = item.optDouble("s"),
                        powerPs = item.optDouble("p"),
                        accelMs2 = 0.0,
                    ),
                )
            }
        }
    }

    private fun decodeRmsChart(array: JSONArray?): List<VibrationRmsChartPoint> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    VibrationRmsChartPoint(
                        elapsedMs = item.optLong("t"),
                        rms = item.optDouble("v"),
                    ),
                )
            }
        }
    }

    private fun decodeSpectrum(array: JSONArray?): List<SpectrumBin> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    SpectrumBin(
                        frequencyHz = item.optDouble("f"),
                        magnitude = item.optDouble("m"),
                    ),
                )
            }
        }
    }

    private fun downsample(size: Int, maxPoints: Int): List<Int> {
        if (size <= 0) return emptyList()
        if (size <= maxPoints) return (0 until size).toList()
        return (0 until maxPoints).map { index ->
            ((index * size.toDouble()) / maxPoints).toInt().coerceIn(0, size - 1)
        }
    }
}
