package com.onlinepicketline.opl.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.onlinepicketline.opl.data.api.ApiClient
import com.onlinepicketline.opl.data.model.*

/**
 * Repository for all OPL mobile API data access.
 * Manages local caching (hash-based 304) and coordinates API calls.
 */
class OplRepository(private val context: Context) {

    private val api by lazy { ApiClient.getInstance(context) }
    private val gson = Gson()
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("opl_cache", Context.MODE_PRIVATE)
    }

    // ---- Mobile Data (blocklist + geofences) ----

    /**
     * Fetch mobile data with hash-based caching.
     * Returns null when data hasn't changed (304).
     */
    suspend fun getMobileData(
        latitude: Double,
        longitude: Double,
        radius: Int? = null
    ): Result<MobileDataResponse?> = runCatching {
        val lastHash = prefs.getString(KEY_DATA_HASH, null)
        val response = api.getMobileData(latitude, longitude, radius, lastHash)

        when (response.code()) {
            304 -> null // Data unchanged
            200 -> {
                val data = response.body()!!
                val newHash = response.headers()["X-Content-Hash"]
                // Cache the hash and data
                prefs.edit()
                    .putString(KEY_DATA_HASH, newHash)
                    .putString(KEY_CACHED_DATA, gson.toJson(data))
                    .putLong(KEY_LAST_FETCH, System.currentTimeMillis())
                    .apply()
                data
            }
            else -> throw ApiException(
                response.code(),
                parseError(response.errorBody()?.string())
            )
        }
    }

    /** Get the last cached mobile data */
    fun getCachedData(): MobileDataResponse? {
        val json = prefs.getString(KEY_CACHED_DATA, null) ?: return null
        return try {
            gson.fromJson(json, MobileDataResponse::class.java)
        } catch (_: Exception) { null }
    }

    /** Get last fetch timestamp */
    fun getLastFetchTime(): Long = prefs.getLong(KEY_LAST_FETCH, 0)

    /** Get the cached region center coordinates */
    fun getCachedRegionCenter(): Coordinates? {
        return getCachedData()?.cachedRegion?.center
    }

    /**
     * Check if a URL is in the blocklist.
     * Returns the matching BlocklistEntry or null.
     */
    fun findBlockedUrl(url: String): BlocklistEntry? {
        val data = getCachedData() ?: return null
        val host = extractHost(url) ?: return null
        return data.blocklist.urls.find { entry ->
            val entryHost = extractHost(entry.url)
            entryHost != null && (host == entryHost || host.endsWith(".$entryHost"))
        }
    }

    // ---- Active Strikes ----

    suspend fun getActiveStrikes(): Result<List<ActiveStrike>> = runCatching {
        val response = api.getActiveStrikes()
        if (response.isSuccessful) {
            response.body()?.strikes ?: emptyList()
        } else {
            throw ApiException(response.code(), parseError(response.errorBody()?.string()))
        }
    }

    // ---- GPS Snapshot ----

    suspend fun submitGpsSnapshot(request: GpsSnapshotRequest): Result<GpsSnapshotResponse> =
        runCatching {
            val response = api.submitGpsSnapshot(request)
            if (response.isSuccessful) {
                response.body()!!
            } else {
                throw ApiException(response.code(), parseError(response.errorBody()?.string()))
            }
        }

    // ---- Strike Submission ----

    suspend fun submitStrike(request: StrikeSubmissionRequest): Result<StrikeSubmissionResponse> =
        runCatching {
            val response = api.submitStrike(request)
            if (response.isSuccessful) {
                response.body()!!
            } else {
                throw ApiException(response.code(), parseError(response.errorBody()?.string()))
            }
        }

    // ---- Geocoding ----

    suspend fun geocode(address: String): Result<GeocodeResult> = runCatching {
        val response = api.geocode(GeocodeRequest(address = address))
        if (response.isSuccessful) {
            response.body()?.result ?: throw ApiException(404, "No results found")
        } else {
            throw ApiException(response.code(), parseError(response.errorBody()?.string()))
        }
    }

    suspend fun reverseGeocode(lat: Double, lng: Double): Result<ReverseGeocodeResult> =
        runCatching {
            val response = api.reverseGeocode(ReverseGeocodeRequest(lat, lng))
            if (response.isSuccessful) {
                response.body()?.result ?: throw ApiException(404, "No results found")
            } else {
                throw ApiException(response.code(), parseError(response.errorBody()?.string()))
            }
        }

    // ---- Cache Management ----

    fun clearCache() {
        prefs.edit().clear().apply()
    }

    // ---- Helpers ----

    private fun extractHost(url: String): String? {
        return try {
            java.net.URI(if (url.contains("://")) url else "https://$url")
                .host?.removePrefix("www.")?.lowercase()
        } catch (_: Exception) { null }
    }

    private fun parseError(body: String?): String {
        if (body == null) return "Unknown error"
        return try {
            gson.fromJson(body, ApiError::class.java).error
        } catch (_: Exception) {
            body.take(200)
        }
    }

    companion object {
        private const val KEY_DATA_HASH = "data_hash"
        private const val KEY_CACHED_DATA = "cached_data"
        private const val KEY_LAST_FETCH = "last_fetch"
    }
}

class ApiException(val code: Int, message: String) : Exception(message)
