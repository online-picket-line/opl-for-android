package com.onlinepicketline.opl.data.model

import org.junit.Assert.*
import org.junit.Test

class ModelsTest {

    @Test
    fun `BlockedRequest defaults to PENDING action`() {
        val request = BlockedRequest(
            url = "example.com",
            employer = "Acme Corp",
            actionType = "strike"
        )
        assertEquals(UserAction.PENDING, request.userAction)
        assertTrue(request.timestamp > 0)
        assertNull(request.appPackage)
    }

    @Test
    fun `BlocklistEntry stores all fields`() {
        val entry = BlocklistEntry(
            url = "https://example.com",
            employer = "Acme Corp",
            employerId = "emp-1",
            actionType = "strike",
            actionId = "act-1"
        )
        assertEquals("https://example.com", entry.url)
        assertEquals("Acme Corp", entry.employer)
        assertEquals("emp-1", entry.employerId)
    }

    @Test
    fun `GpsSnapshotRequest with optional fields`() {
        val withOptional = GpsSnapshotRequest(
            actionId = "act-1",
            latitude = 40.7128,
            longitude = -74.0060,
            address = "New York, NY",
            notes = "Test note"
        )
        assertEquals("act-1", withOptional.actionId)
        assertEquals(40.7128, withOptional.latitude, 0.0001)
        assertEquals("New York, NY", withOptional.address)

        val withoutOptional = GpsSnapshotRequest(
            actionId = "act-2",
            latitude = 34.0522,
            longitude = -118.2437
        )
        assertNull(withoutOptional.address)
        assertNull(withoutOptional.notes)
    }

    @Test
    fun `StrikeSubmissionRequest default values`() {
        val request = StrikeSubmissionRequest(
            employer = EmployerSubmission(name = "Acme Corp"),
            action = ActionSubmission(
                organization = "Workers United",
                location = "New York, NY",
                startDate = "2026-01-01",
                description = "Test strike"
            )
        )
        assertEquals("strike", request.action.actionType)
        assertEquals(30, request.action.durationDays)
        assertNull(request.action.coordinates)
        assertNull(request.employer.industry)
    }

    @Test
    fun `GeocodeRequest can use address or components`() {
        val byAddress = GeocodeRequest(address = "123 Main St, New York, NY")
        assertEquals("123 Main St, New York, NY", byAddress.address)
        assertNull(byAddress.city)

        val byComponents = GeocodeRequest(
            city = "New York",
            state = "NY",
            zipCode = "10001",
            street = "123 Main St"
        )
        assertNull(byComponents.address)
        assertEquals("New York", byComponents.city)
    }

    @Test
    fun `ActiveStrike stores all required fields`() {
        val strike = ActiveStrike(
            id = "act-1",
            organization = "Local 123",
            actionType = "strike",
            location = "Chicago, IL",
            employerName = "Acme Corp",
            employerId = "emp-1",
            startDate = "2026-01-15",
            endDate = "2026-02-15",
            description = "Test action"
        )
        assertEquals("act-1", strike.id)
        assertEquals("Local 123", strike.organization)
    }

    @Test
    fun `Geofence stores coordinates and radius`() {
        val geofence = Geofence(
            id = "action-1",
            type = "action",
            actionId = "act-1",
            employerId = "emp-1",
            employerName = "Acme Corp",
            actionType = "strike",
            organization = "Local 123",
            location = "New York, NY",
            coordinates = Coordinates(40.7128, -74.0060),
            distance = 500,
            notificationRadius = 100,
            startDate = "2026-01-01",
            endDate = "2026-02-01",
            description = "Test geofence",
            demands = null,
            moreInfoUrl = null
        )
        assertEquals(40.7128, geofence.coordinates.lat, 0.0001)
        assertEquals(-74.0060, geofence.coordinates.lng, 0.0001)
        assertEquals(100, geofence.notificationRadius)
        assertNull(geofence.locationName)
    }

    @Test
    fun `MobileDataResponse CachedRegion has threshold`() {
        val region = CachedRegion(
            center = Coordinates(40.0, -74.0),
            radiusMeters = 160934,
            refreshThresholdMeters = 128747
        )
        assertEquals(160934, region.radiusMeters)
        assertTrue(region.refreshThresholdMeters < region.radiusMeters)
    }

    @Test
    fun `UserAction enum has correct values`() {
        assertEquals(3, UserAction.values().size)
        assertNotNull(UserAction.valueOf("PENDING"))
        assertNotNull(UserAction.valueOf("BLOCKED"))
        assertNotNull(UserAction.valueOf("ALLOWED"))
    }
}
