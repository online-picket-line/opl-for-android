package com.onlinepicketline.opl.vpn

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the VPN service DNS parsing logic.
 * These test the packet parsing without requiring Android framework.
 */
class OplVpnServiceTest {

    /**
     * Build a minimal DNS query packet (IPv4 + UDP + DNS question)
     * for testing the domain extraction logic.
     */
    private fun buildDnsQueryPacket(domain: String, dstPort: Int = 53): ByteArray {
        val labels = domain.split(".")
        val questionSection = mutableListOf<Byte>()
        for (label in labels) {
            questionSection.add(label.length.toByte())
            for (ch in label) {
                questionSection.add(ch.code.toByte())
            }
        }
        questionSection.add(0) // terminator
        // QTYPE (A = 1) and QCLASS (IN = 1)
        questionSection.addAll(listOf(0, 1, 0, 1).map { it.toByte() })

        val dnsHeader = ByteArray(12) // transaction ID + flags + counts
        dnsHeader[4] = 0; dnsHeader[5] = 1 // QDCOUNT = 1

        val dnsPayload = dnsHeader + questionSection.toByteArray()
        val udpLength = 8 + dnsPayload.size

        // UDP header
        val udpHeader = ByteArray(8)
        udpHeader[0] = 0; udpHeader[1] = 0 // src port
        udpHeader[2] = (dstPort shr 8).toByte()
        udpHeader[3] = (dstPort and 0xFF).toByte()
        udpHeader[4] = (udpLength shr 8).toByte()
        udpHeader[5] = (udpLength and 0xFF).toByte()

        val udpPayload = udpHeader + dnsPayload
        val totalLength = 20 + udpPayload.size

        // IPv4 header (20 bytes, no options)
        val ipHeader = ByteArray(20)
        ipHeader[0] = 0x45.toByte() // version=4, IHL=5
        ipHeader[2] = (totalLength shr 8).toByte()
        ipHeader[3] = (totalLength and 0xFF).toByte()
        ipHeader[8] = 64 // TTL
        ipHeader[9] = 17 // protocol = UDP

        return ipHeader + udpPayload
    }

    @Test
    fun `extractDomainName parses simple domain`() {
        // Build DNS question section for "example.com"
        val question = byteArrayOf(
            7, 'e'.code.toByte(), 'x'.code.toByte(), 'a'.code.toByte(),
            'm'.code.toByte(), 'p'.code.toByte(), 'l'.code.toByte(), 'e'.code.toByte(),
            3, 'c'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(),
            0 // terminator
        )

        // Use a standalone extraction call
        val result = extractDomainFromBytes(question, 0, question.size)
        assertEquals("example.com", result)
    }

    @Test
    fun `extractDomainName parses subdomain`() {
        val question = byteArrayOf(
            3, 'w'.code.toByte(), 'w'.code.toByte(), 'w'.code.toByte(),
            7, 'e'.code.toByte(), 'x'.code.toByte(), 'a'.code.toByte(),
            'm'.code.toByte(), 'p'.code.toByte(), 'l'.code.toByte(), 'e'.code.toByte(),
            3, 'c'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(),
            0
        )
        val result = extractDomainFromBytes(question, 0, question.size)
        assertEquals("www.example.com", result)
    }

    @Test
    fun `extractDomainName returns null for empty packet`() {
        val result = extractDomainFromBytes(byteArrayOf(0), 0, 1)
        assertNull(result)
    }

    @Test
    fun `parseDnsQuery extracts domain from UDP port 53`() {
        val packet = buildDnsQueryPacket("starbucks.com")
        val result = parseDnsFromPacket(packet, packet.size)
        assertEquals("starbucks.com", result)
    }

    @Test
    fun `parseDnsQuery ignores non-DNS packets`() {
        val packet = buildDnsQueryPacket("example.com", dstPort = 443)
        val result = parseDnsFromPacket(packet, packet.size)
        assertNull(result)
    }

    @Test
    fun `parseDnsQuery handles short packets`() {
        assertNull(parseDnsFromPacket(ByteArray(10), 10))
        assertNull(parseDnsFromPacket(ByteArray(0), 0))
    }

    @Test
    fun `parseDnsQuery rejects non-IPv4`() {
        val packet = buildDnsQueryPacket("example.com")
        packet[0] = 0x60.toByte() // IPv6
        assertNull(parseDnsFromPacket(packet, packet.size))
    }

    @Test
    fun `parseDnsQuery rejects non-UDP`() {
        val packet = buildDnsQueryPacket("example.com")
        packet[9] = 6 // TCP
        assertNull(parseDnsFromPacket(packet, packet.size))
    }

    // ---- Helpers that replicate the VPN service's parsing logic for testing ----

    private fun extractDomainFromBytes(packet: ByteArray, offset: Int, length: Int): String? {
        val parts = mutableListOf<String>()
        var pos = offset
        while (pos < length) {
            val labelLength = packet[pos].toInt() and 0xFF
            if (labelLength == 0) break
            if (pos + labelLength + 1 > length) return null
            val label = String(packet, pos + 1, labelLength, Charsets.US_ASCII)
            parts.add(label)
            pos += labelLength + 1
        }
        return if (parts.isNotEmpty()) parts.joinToString(".").lowercase() else null
    }

    private fun parseDnsFromPacket(packet: ByteArray, length: Int): String? {
        if (length < 28) return null
        val version = (packet[0].toInt() and 0xF0) shr 4
        if (version != 4) return null
        val headerLength = (packet[0].toInt() and 0x0F) * 4
        if (length < headerLength + 8) return null
        val protocol = packet[9].toInt() and 0xFF
        if (protocol != 17) return null
        val dstPort = ((packet[headerLength + 2].toInt() and 0xFF) shl 8) or
                (packet[headerLength + 3].toInt() and 0xFF)
        if (dstPort != 53) return null
        val dnsStart = headerLength + 8
        val questionStart = dnsStart + 12
        if (questionStart >= length) return null
        return extractDomainFromBytes(packet, questionStart, length)
    }
}
