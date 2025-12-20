package com.onlinepicketline.onlinepicketline.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.onlinepicketline.onlinepicketline.data.api.ApiClient
import com.onlinepicketline.onlinepicketline.data.api.PicketLineApiService
import com.onlinepicketline.onlinepicketline.data.model.LaborDispute
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
    private var cachedBlocklist: List<BlocklistEntry> = emptyList()
    private var cachedEmployers: List<Employer> = emptyList()
    private var cachedActionResources: ActionResources? = null
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
    suspend fun fetchBlocklist(forceRefresh: Boolean = false): Result<BlocklistApiResponse> {
        return withContext(Dispatchers.IO) {
            try {
                if (!forceRefresh && isCacheValid()) {
                    return@withContext Result.success(
                        BlocklistApiResponse(
                            version = null,
                            generatedAt = null,
                            totalUrls = cachedBlocklist.size,
                            employers = cachedEmployers,
                            blocklist = cachedBlocklist,
                            actionResources = cachedActionResources
                        )
                    )
                }
                val response = apiService.getBlocklist()
                cachedBlocklist = response.blocklist ?: emptyList()
                cachedEmployers = response.employers ?: emptyList()
                cachedActionResources = response.actionResources
                lastFetchTime = System.currentTimeMillis()
                Log.d(TAG, "Fetched ${cachedBlocklist.size} blocklist entries")
                Result.success(response)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch blocklist", e)
                if (cachedBlocklist.isNotEmpty()) {
                    Result.success(
                        BlocklistApiResponse(
                            version = null,
                            generatedAt = null,
                            totalUrls = cachedBlocklist.size,
                            employers = cachedEmployers,
                            blocklist = cachedBlocklist,
                            actionResources = cachedActionResources
                        )
                    )
                } else {
                    Result.failure(e)
                }
            }
        }
    }
    
    /**
     * Checks if a domain matches any disputed company
     */
    fun findBlocklistEntryForDomain(domain: String): BlocklistEntry? {
        if (isAllowedDomain(domain)) {
            return null
        }
        return cachedBlocklist.firstOrNull { entry ->
            entry.url?.contains(domain, ignoreCase = true) == true
        }
    }
    
    /**
     * Gets all cached disputes
     */
    fun getCachedBlocklist(): List<BlocklistEntry> = cachedBlocklist
    fun getCachedEmployers(): List<Employer> = cachedEmployers
    fun getCachedActionResources(): ActionResources? = cachedActionResources
    
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
