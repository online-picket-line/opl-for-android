package com.onlinepicketline.opl.data.model

import com.google.gson.annotations.SerializedName

/**
 * Root response from GET /api/mobile/data
 */
data class MobileDataResponse(
    val version: String,
    val cachedRegion: CachedRegion,
    val suggestedRefreshInterval: Long,
    val geofences: GeofenceCollection,
    val blocklist: BlocklistData,
    val generatedAt: String
)

data class CachedRegion(
    val center: Coordinates,
    val radiusMeters: Int,
    val refreshThresholdMeters: Int
)

data class Coordinates(
    val lat: Double,
    val lng: Double
)

data class GeofenceCollection(
    val total: Int,
    val byEmployer: List<EmployerGeofences>,
    val all: List<Geofence>
)

data class EmployerGeofences(
    val employerId: String,
    val employerName: String,
    val geofences: List<Geofence>
)

data class Geofence(
    val id: String,
    val type: String, // "action", "picket-site", "employer-location"
    val actionId: String,
    val employerId: String,
    val employerName: String,
    val actionType: String,
    val organization: String?,
    val location: String?,
    val coordinates: Coordinates,
    val distance: Int,
    val notificationRadius: Int,
    val startDate: String?,
    val endDate: String?,
    val description: String?,
    val demands: String?,
    val moreInfoUrl: String?,
    val locationName: String? = null,
    val locationType: String? = null
)

data class BlocklistData(
    val totalUrls: Int,
    val totalEmployers: Int,
    val urls: List<BlocklistEntry>
)

data class BlocklistEntry(
    val url: String,
    val employer: String,
    val employerId: String,
    val actionType: String,
    val actionId: String
)

/**
 * Response from GET /api/mobile/active-strikes
 */
data class ActiveStrikesResponse(
    val success: Boolean,
    val count: Int,
    val generatedAt: String,
    val strikes: List<ActiveStrike>
)

data class ActiveStrike(
    val id: String,
    val organization: String?,
    val actionType: String,
    val location: String?,
    val employerName: String,
    val employerId: String,
    val startDate: String?,
    val endDate: String?,
    val description: String?
)

/**
 * POST /api/mobile/gps-snapshot request & response
 */
data class GpsSnapshotRequest(
    val actionId: String,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val notes: String? = null
)

data class GpsSnapshotResponse(
    val success: Boolean,
    val message: String,
    val id: String
)

/**
 * POST /api/mobile/submit-strike request & response
 */
data class StrikeSubmissionRequest(
    val employer: EmployerSubmission,
    val action: ActionSubmission
)

data class EmployerSubmission(
    val name: String,
    val industry: String? = null,
    val website: String? = null
)

data class ActionSubmission(
    val organization: String,
    val actionType: String = "strike",
    val location: String,
    val startDate: String,
    val durationDays: Int = 30,
    val description: String,
    val demands: String? = null,
    val contactInfo: String? = null,
    val learnMoreUrl: String? = null,
    val coordinates: GpsCoordinates? = null
)

data class GpsCoordinates(
    val latitude: Double,
    val longitude: Double
)

data class StrikeSubmissionResponse(
    val success: Boolean,
    val message: String,
    val id: String
)

/**
 * POST /api/mobile/geocode request & response
 */
data class GeocodeRequest(
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val zipCode: String? = null,
    val street: String? = null
)

data class GeocodeResult(
    val latitude: Double,
    val longitude: Double,
    val displayName: String?,
    val confidence: Double? = null,
    val source: String? = null
)

data class GeocodeResponse(
    val success: Boolean,
    val input: String?,
    val result: GeocodeResult?
)

/**
 * POST /api/mobile/reverse-geocode request & response
 */
data class ReverseGeocodeRequest(
    val latitude: Double,
    val longitude: Double
)

data class ReverseGeocodeResponse(
    val success: Boolean,
    val input: GpsCoordinates?,
    val result: ReverseGeocodeResult?
)

data class ReverseGeocodeResult(
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val displayName: String? = null
)

/**
 * Generic API error response
 */
data class ApiError(
    val error: String
)

/**
 * Local tracking of blocked URL access attempts
 */
data class BlockedRequest(
    val url: String,
    val employer: String,
    val actionType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val appPackage: String? = null,
    val userAction: UserAction = UserAction.PENDING
)

enum class UserAction {
    PENDING,
    BLOCKED,
    ALLOWED
}
