package com.onlinepicketline.opl.data.repository

import com.onlinepicketline.opl.data.model.BlocklistData
import com.onlinepicketline.opl.data.model.BlocklistEntry
import org.junit.Assert.*
import org.junit.Test

class OplRepositoryTest {

    // Test the URL matching logic extracted from the repository

    @Test
    fun `extractHost removes www prefix`() {
        assertEquals("example.com", extractHost("https://www.example.com/path"))
        assertEquals("example.com", extractHost("http://example.com"))
        assertEquals("sub.example.com", extractHost("https://sub.example.com"))
    }

    @Test
    fun `extractHost handles bare domains`() {
        assertEquals("example.com", extractHost("example.com"))
        assertEquals("example.com", extractHost("www.example.com"))
    }

    @Test
    fun `extractHost returns null for invalid URLs`() {
        assertNull(extractHost(""))
        assertNull(extractHost("not a url at all !!!"))
    }

    @Test
    fun `findBlockedUrl matches exact domain`() {
        val entries = listOf(
            BlocklistEntry("https://starbucks.com", "Starbucks", "emp-1", "strike", "act-1"),
            BlocklistEntry("https://amazon.com", "Amazon", "emp-2", "boycott", "act-2")
        )

        val result = findBlockedUrlInList("starbucks.com", entries)
        assertNotNull(result)
        assertEquals("Starbucks", result?.employer)
    }

    @Test
    fun `findBlockedUrl matches subdomain`() {
        val entries = listOf(
            BlocklistEntry("https://starbucks.com", "Starbucks", "emp-1", "strike", "act-1")
        )

        val result = findBlockedUrlInList("store.starbucks.com", entries)
        assertNotNull(result)
        assertEquals("Starbucks", result?.employer)
    }

    @Test
    fun `findBlockedUrl does not match partial domains`() {
        val entries = listOf(
            BlocklistEntry("https://example.com", "Example", "emp-1", "strike", "act-1")
        )

        val result = findBlockedUrlInList("notexample.com", entries)
        assertNull(result)
    }

    @Test
    fun `findBlockedUrl returns null for unmatched domains`() {
        val entries = listOf(
            BlocklistEntry("https://starbucks.com", "Starbucks", "emp-1", "strike", "act-1")
        )

        val result = findBlockedUrlInList("google.com", entries)
        assertNull(result)
    }

    @Test
    fun `findBlockedUrl is case insensitive`() {
        val entries = listOf(
            BlocklistEntry("https://Starbucks.com", "Starbucks", "emp-1", "strike", "act-1")
        )

        val result = findBlockedUrlInList("STARBUCKS.COM", entries)
        assertNotNull(result)
    }

    @Test
    fun `ApiException stores code and message`() {
        val exception = ApiException(403, "Forbidden")
        assertEquals(403, exception.code)
        assertEquals("Forbidden", exception.message)
    }

    // ---- Helper functions that replicate repository logic for unit testing ----

    private fun extractHost(url: String): String? {
        return try {
            java.net.URI(if (url.contains("://")) url else "https://$url")
                .host?.removePrefix("www.")?.lowercase()
        } catch (_: Exception) { null }
    }

    private fun findBlockedUrlInList(url: String, entries: List<BlocklistEntry>): BlocklistEntry? {
        val host = extractHost(url) ?: return null
        return entries.find { entry ->
            val entryHost = extractHost(entry.url)
            entryHost != null && (host == entryHost || host.endsWith(".$entryHost"))
        }
    }
}
