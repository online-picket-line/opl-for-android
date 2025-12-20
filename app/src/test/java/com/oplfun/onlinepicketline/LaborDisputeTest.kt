package com.onlinepicketline.onlinepicketline

import com.onlinepicketline.onlinepicketline.data.model.LaborDispute
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for LaborDispute model
 */
class LaborDisputeTest {
    
    @Test
    fun testGetAllDomains_withSingleDomain() {
        val dispute = LaborDispute(
            id = "1",
            companyName = "Test Company",
            domain = "example.com",
            domains = null,
            disputeType = "Strike",
            description = "Test strike",
            startDate = "2024-01-01"
        )
        
        val allDomains = dispute.getAllDomains()
        assertEquals(1, allDomains.size)
        assertTrue(allDomains.contains("example.com"))
    }
    
    @Test
    fun testGetAllDomains_withMultipleDomains() {
        val dispute = LaborDispute(
            id = "1",
            companyName = "Test Company",
            domain = "example.com",
            domains = listOf("www.example.com", "shop.example.com"),
            disputeType = "Strike",
            description = "Test strike",
            startDate = "2024-01-01"
        )
        
        val allDomains = dispute.getAllDomains()
        assertEquals(3, allDomains.size)
        assertTrue(allDomains.contains("example.com"))
        assertTrue(allDomains.contains("www.example.com"))
        assertTrue(allDomains.contains("shop.example.com"))
    }
    
    @Test
    fun testMatchesDomain_exactMatch() {
        val dispute = LaborDispute(
            id = "1",
            companyName = "Test Company",
            domain = "example.com",
            domains = null,
            disputeType = "Strike",
            description = "Test strike",
            startDate = "2024-01-01"
        )
        
        assertTrue(dispute.matchesDomain("example.com"))
        assertTrue(dispute.matchesDomain("EXAMPLE.COM"))
        assertTrue(dispute.matchesDomain(" example.com "))
    }
    
    @Test
    fun testMatchesDomain_subdomain() {
        val dispute = LaborDispute(
            id = "1",
            companyName = "Test Company",
            domain = "example.com",
            domains = null,
            disputeType = "Strike",
            description = "Test strike",
            startDate = "2024-01-01"
        )
        
        assertTrue(dispute.matchesDomain("www.example.com"))
        assertTrue(dispute.matchesDomain("shop.example.com"))
        assertTrue(dispute.matchesDomain("api.shop.example.com"))
    }
    
    @Test
    fun testMatchesDomain_noMatch() {
        val dispute = LaborDispute(
            id = "1",
            companyName = "Test Company",
            domain = "example.com",
            domains = null,
            disputeType = "Strike",
            description = "Test strike",
            startDate = "2024-01-01"
        )
        
        assertFalse(dispute.matchesDomain("other.com"))
        assertFalse(dispute.matchesDomain("examplecom"))
        assertFalse(dispute.matchesDomain("example.org"))
    }
    
    @Test
    fun testMatchesDomain_withMultipleDomains() {
        val dispute = LaborDispute(
            id = "1",
            companyName = "Test Company",
            domain = "example.com",
            domains = listOf("example.org", "example.net"),
            disputeType = "Strike",
            description = "Test strike",
            startDate = "2024-01-01"
        )
        
        assertTrue(dispute.matchesDomain("example.com"))
        assertTrue(dispute.matchesDomain("example.org"))
        assertTrue(dispute.matchesDomain("example.net"))
        assertTrue(dispute.matchesDomain("www.example.org"))
        assertFalse(dispute.matchesDomain("other.com"))
    }
}
