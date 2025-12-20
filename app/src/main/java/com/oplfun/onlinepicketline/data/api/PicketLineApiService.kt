package com.onlinepicketline.onlinepicketline.data.api

import com.onlinepicketline.onlinepicketline.data.model.LaborDisputeResponse
import retrofit2.http.GET

/**
 * API service for communicating with the Online Picket Line API
 */
interface PicketLineApiService {
    
    /**
     * Fetches the unified blocklist and action resources from the new API
     * @param format Optional format (json, list, hosts, extension)
     * @param includeInactive Optional, include inactive employers
     */
    @GET("api/blocklist?format=json")
    suspend fun getBlocklist(): BlocklistApiResponse
}
