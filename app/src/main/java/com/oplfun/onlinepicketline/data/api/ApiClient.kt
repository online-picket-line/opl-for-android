package com.oplfun.onlinepicketline.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Factory for creating Retrofit API client instances
 */
object ApiClient {
    
    private const val DEFAULT_BASE_URL = "https://api.onlinepicketline.org/"
    
    /**
     * Creates an OkHttp client with logging
     */
    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * Creates the Retrofit instance
     */
    private fun createRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    /**
     * Creates the API service with custom base URL
     */
    fun createApiService(baseUrl: String = DEFAULT_BASE_URL): PicketLineApiService {
        return createRetrofit(baseUrl).create(PicketLineApiService::class.java)
    }
}
