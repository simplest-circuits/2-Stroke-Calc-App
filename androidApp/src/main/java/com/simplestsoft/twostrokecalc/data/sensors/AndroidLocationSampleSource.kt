package com.simplestsoft.twostrokecalc.data.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class LocationSample(
    val timestampMs: Long,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double,
    val accuracyM: Double,
)

@Singleton
class AndroidLocationSampleSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    fun start(dynoMode: Boolean = false): Flow<LocationSample> = callbackFlow {
        var previousLocation: Location? = null
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val speedKmh = if (dynoMode) {
                    computeDynoSpeedKmh(location, previousLocation)
                } else {
                    (location.speed * 3.6).coerceAtLeast(0.0)
                }
                previousLocation = location
                trySend(
                    LocationSample(
                        timestampMs = location.time.takeIf { it > 0 } ?: System.currentTimeMillis(),
                        latitude = location.latitude,
                        longitude = location.longitude,
                        speedKmh = speedKmh,
                        accuracyM = location.accuracy.toDouble(),
                    ),
                )
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }
        val provider = when {
            dynoMode && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                LocationManager.NETWORK_PROVIDER
            else -> null
        }
        if (provider == null) {
            close(IllegalStateException("No location provider"))
            return@callbackFlow
        }
        val minTimeMs = if (dynoMode) 0L else 200L
        locationManager.requestLocationUpdates(provider, minTimeMs, 0f, listener, Looper.getMainLooper())
        awaitClose { locationManager.removeUpdates(listener) }
    }

    /**
     * Position-derived speed reacts faster than GPS-filtered [Location.getSpeed].
     * Falls back to reported speed when movement is too small to measure reliably.
     */
    internal fun computeDynoSpeedKmh(current: Location, previous: Location?): Double {
        if (previous != null) {
            val dtMs = current.time - previous.time
            if (dtMs in 50..2000) {
                val distanceM = current.distanceTo(previous).toDouble()
                if (distanceM >= 0.25) {
                    return (distanceM / (dtMs / 1000.0) * 3.6).coerceAtLeast(0.0)
                }
            }
        }
        if (current.hasSpeed()) {
            return (current.speed * 3.6).toDouble().coerceAtLeast(0.0)
        }
        return 0.0
    }
}
