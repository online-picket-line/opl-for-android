import com.google.gson.annotations.SerializedName

/**
 * Data model for the new unified blocklist API response
 */
data class BlocklistApiResponse(
    @SerializedName("version")
    val version: String?,
    @SerializedName("generatedAt")
    val generatedAt: String?,
    @SerializedName("totalUrls")
    val totalUrls: Int?,
    @SerializedName("employers")
    val employers: List<Employer>?,
    @SerializedName("blocklist")
    val blocklist: List<BlocklistEntry>?,
    @SerializedName("actionResources")
    val actionResources: ActionResources?
)

data class Employer(
    @SerializedName("id")
    val id: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("urlCount")
    val urlCount: Int?
)

data class BlocklistEntry(
    @SerializedName("url")
    val url: String?,
    @SerializedName("employer")
    val employer: String?,
    @SerializedName("employerId")
    val employerId: String?,
    @SerializedName("label")
    val label: String?,
    @SerializedName("category")
    val category: String?,
    @SerializedName("reason")
    val reason: String?
)

data class ActionResources(
    @SerializedName("totalActions")
    val totalActions: Int?,
    @SerializedName("totalResources")
    val totalResources: Int?,
    @SerializedName("actions")
    val actions: List<Action>?,
    @SerializedName("resources")
    val resources: List<ActionResource>?
)

data class Action(
    @SerializedName("id")
    val id: String?,
    @SerializedName("organization")
    val organization: String?,
    @SerializedName("actionType")
    val actionType: String?,
    @SerializedName("status")
    val status: String?,
    @SerializedName("resourceCount")
    val resourceCount: Int?
)

data class ActionResource(
    @SerializedName("actionId")
    val actionId: String?,
    @SerializedName("actionType")
    val actionType: String?,
    @SerializedName("organization")
    val organization: String?,
    @SerializedName("status")
    val status: String?,
    @SerializedName("url")
    val url: String?,
    @SerializedName("label")
    val label: String?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("startDate")
    val startDate: String?,
    @SerializedName("endDate")
    val endDate: String?,
    @SerializedName("location")
    val location: String?
)
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
