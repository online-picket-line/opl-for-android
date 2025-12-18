package com.oplfun.onlinepicketline.data.api

import com.oplfun.onlinepicketline.data.model.LaborDisputeResponse
import retrofit2.http.GET

/**
 * API service for communicating with the Online Picket Line API
 */
interface PicketLineApiService {
    
    /**
     * Fetches the list of companies currently under labor disputes
     */
    @GET("api/disputes")
    suspend fun getDisputedCompanies(): LaborDisputeResponse
    
    /**
     * Alternative endpoint - can be adjusted based on actual API structure
     */
    @GET("disputes.json")
    suspend fun getDisputesJson(): LaborDisputeResponse
}
