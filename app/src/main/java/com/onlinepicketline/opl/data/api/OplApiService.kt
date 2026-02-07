package com.onlinepicketline.opl.data.api

import com.onlinepicketline.opl.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit interface for the OPL Mobile API.
 * All endpoints require X-API-Key header (added by interceptor).
 */
interface OplApiService {

    /**
     * Get combined blocklist + geofence data.
     * Returns 304 Not Modified when hash matches.
     */
    @GET("mobile/data")
    suspend fun getMobileData(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("radius") radius: Int? = null,
        @Query("hash") hash: String? = null,
        @Query("includeInactive") includeInactive: Boolean? = null
    ): Response<MobileDataResponse>

    /**
     * Get list of active strikes for GPS snapshot selection.
     */
    @GET("mobile/active-strikes")
    suspend fun getActiveStrikes(): Response<ActiveStrikesResponse>

    /**
     * Submit a GPS snapshot for an active strike.
     */
    @POST("mobile/gps-snapshot")
    suspend fun submitGpsSnapshot(
        @Body request: GpsSnapshotRequest
    ): Response<GpsSnapshotResponse>

    /**
     * Submit a new strike action from the mobile app.
     */
    @POST("mobile/submit-strike")
    suspend fun submitStrike(
        @Body request: StrikeSubmissionRequest
    ): Response<StrikeSubmissionResponse>

    /**
     * Forward geocode: address → coordinates.
     */
    @POST("mobile/geocode")
    suspend fun geocode(
        @Body request: GeocodeRequest
    ): Response<GeocodeResponse>

    /**
     * Reverse geocode: coordinates → address.
     */
    @POST("mobile/reverse-geocode")
    suspend fun reverseGeocode(
        @Body request: ReverseGeocodeRequest
    ): Response<ReverseGeocodeResponse>
}
