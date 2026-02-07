package com.onlinepicketline.opl.util

import android.location.Location
import com.onlinepicketline.opl.data.model.Coordinates
import com.onlinepicketline.opl.data.model.Geofence

/**
 * Utility functions for distance calculations and geofence checking.
 */
object LocationUtils {

    /**
     * Calculate distance in meters between two points using Android's Location API.
     */
    fun distanceBetween(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
    }

    /**
     * Check which geofences the user is currently inside.
     * Returns geofences sorted by distance (closest first).
     */
    fun checkGeofences(
        latitude: Double,
        longitude: Double,
        geofences: List<Geofence>
    ): List<GeofenceHit> {
        return geofences.mapNotNull { geofence ->
            val distance = distanceBetween(
                latitude, longitude,
                geofence.coordinates.lat, geofence.coordinates.lng
            )
            if (distance <= geofence.notificationRadius) {
                GeofenceHit(geofence, distance)
            } else null
        }.sortedBy { it.distance }
    }

    /**
     * Check if user has moved far enough from cached region center
     * to warrant a new API data fetch.
     */
    fun shouldRefreshData(
        currentLat: Double,
        currentLng: Double,
        cachedCenter: Coordinates,
        refreshThresholdMeters: Int
    ): Boolean {
        val distance = distanceBetween(
            currentLat, currentLng,
            cachedCenter.lat, cachedCenter.lng
        )
        return distance > refreshThresholdMeters
    }
}

data class GeofenceHit(
    val geofence: Geofence,
    val distance: Float
)
