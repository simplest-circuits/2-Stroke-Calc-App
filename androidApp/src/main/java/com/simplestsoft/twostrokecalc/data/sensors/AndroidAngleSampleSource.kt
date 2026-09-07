package com.simplestsoft.twostrokecalc.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class AngleSample(
    val degrees: Float,
    val reliability: Float,
)

@Singleton
class AndroidAngleSampleSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun start(): Flow<AngleSample> = callbackFlow {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensor == null) {
            close(IllegalStateException("No gravity sensor or accelerometer available"))
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values.getOrElse(0) { 0f }
                val y = event.values.getOrElse(1) { 0f }
                val z = event.values.getOrElse(2) { 0f }
                val planarGravity = sqrt(x * x + y * y)
                val totalGravity = sqrt(planarGravity * planarGravity + z * z).coerceAtLeast(0.001f)
                val clockwiseFromTop = Math.toDegrees(atan2(-x, y).toDouble()).toFloat()
                val normalized = (clockwiseFromTop + 360f) % 360f

                trySend(
                    AngleSample(
                        degrees = normalized,
                        reliability = (planarGravity / totalGravity).coerceIn(0f, 1f),
                    ),
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
