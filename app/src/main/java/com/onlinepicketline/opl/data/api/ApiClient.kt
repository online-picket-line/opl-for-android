package com.onlinepicketline.opl.data.api

import android.content.Context
import com.onlinepicketline.opl.BuildConfig
import com.onlinepicketline.opl.util.SecureStorage
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton API client. Injects the API key header on every request.
 */
object ApiClient {

    private const val CONNECT_TIMEOUT = 15L
    private const val READ_TIMEOUT = 30L

    @Volatile
    private var apiService: OplApiService? = null

    fun getInstance(context: Context): OplApiService {
        return apiService ?: synchronized(this) {
            apiService ?: createApiService(context).also { apiService = it }
        }
    }

    /**
     * Force recreation of the API client (e.g. after API key change).
     */
    fun reset() {
        synchronized(this) { apiService = null }
    }

    private fun createApiService(context: Context): OplApiService {
        val apiKeyInterceptor = Interceptor { chain ->
            val apiKey = SecureStorage.getApiKey(context)
            val request = chain.request().newBuilder().apply {
                if (apiKey != null) {
                    addHeader("X-API-Key", apiKey)
                }
                addHeader("User-Agent", "OPL-Android/${BuildConfig.VERSION_NAME}")
                addHeader("Accept", "application/json")
            }.build()
            chain.proceed(request)
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(apiKeyInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OplApiService::class.java)
    }
}
