package com.oplfun.onlinepicketline.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.oplfun.onlinepicketline.data.api.ApiClient
import com.oplfun.onlinepicketline.data.api.PicketLineApiService
import com.oplfun.onlinepicketline.data.model.LaborDispute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for managing labor dispute data
 */
class DisputeRepository(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "dispute_prefs", Context.MODE_PRIVATE
    )
    
    private val apiService: PicketLineApiService by lazy {
        val baseUrl = prefs.getString(KEY_API_BASE_URL, DEFAULT_API_URL) ?: DEFAULT_API_URL
        ApiClient.createApiService(baseUrl)
    }
    
    // In-memory cache of disputes
    private var cachedDisputes: List<LaborDispute> = emptyList()
    private var lastFetchTime: Long = 0
    
    // Set of domains that user has chosen to always allow
    private val allowedDomains = mutableSetOf<String>()
    
    companion object {
        private const val TAG = "DisputeRepository"
        private const val DEFAULT_API_URL = "https://api.onlinepicketline.org/"
        private const val KEY_API_BASE_URL = "api_base_url"
        private const val KEY_ALLOWED_DOMAINS = "allowed_domains"
        private const val CACHE_DURATION_MS = 3600000L // 1 hour
        
        @Volatile
        private var INSTANCE: DisputeRepository? = null
        
        fun getInstance(context: Context): DisputeRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DisputeRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    init {
        loadAllowedDomains()
    }
    
    /**
     * Fetches the latest list of companies under labor disputes
     */
    suspend fun fetchDisputes(forceRefresh: Boolean = false): Result<List<LaborDispute>> {
        return withContext(Dispatchers.IO) {
            try {
                // Return cached data if still valid
                if (!forceRefresh && isCacheValid()) {
                    return@withContext Result.success(cachedDisputes)
                }
                
                // Try primary endpoint
                val response = try {
                    apiService.getDisputedCompanies()
                } catch (e: Exception) {
                    Log.w(TAG, "Primary endpoint failed, trying alternative", e)
                    // Try alternative endpoint
                    apiService.getDisputesJson()
                }
                
                cachedDisputes = response.disputes.filter { it.status == "active" }
                lastFetchTime = System.currentTimeMillis()
                
                Log.d(TAG, "Fetched ${cachedDisputes.size} active disputes")
                Result.success(cachedDisputes)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch disputes", e)
                // Return cached data if available, even if expired
                if (cachedDisputes.isNotEmpty()) {
                    Result.success(cachedDisputes)
                } else {
                    Result.failure(e)
                }
            }
        }
    }
    
    /**
     * Checks if a domain matches any disputed company
     */
    fun findDisputeForDomain(domain: String): LaborDispute? {
        if (isAllowedDomain(domain)) {
            return null
        }
        
        return cachedDisputes.firstOrNull { dispute ->
            dispute.matchesDomain(domain)
        }
    }
    
    /**
     * Gets all cached disputes
     */
    fun getCachedDisputes(): List<LaborDispute> = cachedDisputes
    
    /**
     * Marks a domain as allowed (user chose to ignore the block)
     */
    fun allowDomain(domain: String) {
        allowedDomains.add(domain.lowercase())
        saveAllowedDomains()
    }
    
    /**
     * Removes a domain from the allowed list
     */
    fun removeAllowedDomain(domain: String) {
        allowedDomains.remove(domain.lowercase())
        saveAllowedDomains()
    }
    
    /**
     * Checks if a domain is in the allowed list
     */
    fun isAllowedDomain(domain: String): Boolean {
        return allowedDomains.contains(domain.lowercase())
    }
    
    /**
     * Gets all allowed domains
     */
    fun getAllowedDomains(): Set<String> = allowedDomains.toSet()
    
    /**
     * Clears all allowed domains
     */
    fun clearAllowedDomains() {
        allowedDomains.clear()
        saveAllowedDomains()
    }
    
    /**
     * Sets the API base URL
     */
    fun setApiBaseUrl(url: String) {
        prefs.edit().putString(KEY_API_BASE_URL, url).apply()
    }
    
    /**
     * Gets the current API base URL
     */
    fun getApiBaseUrl(): String {
        return prefs.getString(KEY_API_BASE_URL, DEFAULT_API_URL) ?: DEFAULT_API_URL
    }
    
    private fun isCacheValid(): Boolean {
        return cachedDisputes.isNotEmpty() &&
                (System.currentTimeMillis() - lastFetchTime) < CACHE_DURATION_MS
    }
    
    private fun loadAllowedDomains() {
        val domainsString = prefs.getString(KEY_ALLOWED_DOMAINS, "") ?: ""
        if (domainsString.isNotEmpty()) {
            allowedDomains.addAll(domainsString.split(","))
        }
    }
    
    private fun saveAllowedDomains() {
        val domainsString = allowedDomains.joinToString(",")
        prefs.edit().putString(KEY_ALLOWED_DOMAINS, domainsString).apply()
    }
}
