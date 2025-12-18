package com.oplfun.onlinepicketline.data.model

/**
 * Represents a blocked request decision
 */
data class BlockedRequest(
    val domain: String,
    val url: String,
    val dispute: LaborDispute,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * User's decision on a blocked request
 */
enum class UserDecision {
    BLOCK,      // Keep blocking
    ALLOW_ONCE, // Allow this one time
    ALLOW_ALWAYS // Always allow this domain
}
