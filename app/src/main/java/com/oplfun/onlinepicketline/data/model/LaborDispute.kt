package com.onlinepicketline.onlinepicketline.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a company that is involved in a labor dispute
 */
data class LaborDispute(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("company_name")
    val companyName: String,
    
    @SerializedName("domain")
    val domain: String,
    
    @SerializedName("domains")
    val domains: List<String>? = null,
    
    @SerializedName("dispute_type")
    val disputeType: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("start_date")
    val startDate: String,
    
    @SerializedName("union")
    val union: String? = null,
    
    @SerializedName("status")
    val status: String = "active",
    
    @SerializedName("more_info_url")
    val moreInfoUrl: String? = null
) {
    /**
     * Returns all domains associated with this company
     */
    fun getAllDomains(): List<String> {
        val allDomains = mutableListOf<String>()
        allDomains.add(domain)
        domains?.let { allDomains.addAll(it) }
        return allDomains.distinct()
    }
    
    /**
     * Checks if a given domain matches this company
     */
    fun matchesDomain(testDomain: String): Boolean {
        val normalizedTestDomain = testDomain.lowercase().trim()
        return getAllDomains().any { disputeDomain ->
            val normalizedDisputeDomain = disputeDomain.lowercase().trim()
            normalizedTestDomain == normalizedDisputeDomain ||
            normalizedTestDomain.endsWith(".$normalizedDisputeDomain")
        }
    }
}

/**
 * Response from the API containing list of labor disputes
 */
data class LaborDisputeResponse(
    @SerializedName("disputes")
    val disputes: List<LaborDispute>,
    
    @SerializedName("last_updated")
    val lastUpdated: String? = null
)
