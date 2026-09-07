package com.simplestsoft.twostrokecalc.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class MotionSample(
    val timestampMs: Long,
    val x: Float,
    val y: Float,
    val z: Float,
)

@Singleton
class AndroidMotionSampleSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun start(sampleRateHz: Int = 100): Flow<MotionSample> = callbackFlow {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensor == null) {
            close(IllegalStateException("No accelerometer available"))
            return@callbackFlow
        }
        val delayUs = (1_000_000 / sampleRateHz.coerceAtLeast(1)).coerceAtLeast(SensorManager.SENSOR_DELAY_GAME)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(
                    MotionSample(
                        timestampMs = System.currentTimeMillis(),
                        x = event.values.getOrElse(0) { 0f },
                        y = event.values.getOrElse(1) { 0f },
                        z = event.values.getOrElse(2) { 0f },
                    ),
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, sensor, delayUs)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
