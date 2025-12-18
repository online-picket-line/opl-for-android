# Sample API Data for Testing

This directory contains sample mock data that can be used for testing the application when the actual API is not available.

## Mock API Response

The application expects the API to return data in the following format:

```json
{
  "disputes": [
    {
      "id": "dispute-001",
      "company_name": "Example Corporation",
      "domain": "example.com",
      "domains": ["www.example.com", "shop.example.com", "api.example.com"],
      "dispute_type": "Strike",
      "description": "Workers are striking for better wages and working conditions. The union has been negotiating for 6 months without progress.",
      "start_date": "2024-01-15",
      "union": "Workers United Local 123",
      "status": "active",
      "more_info_url": "https://workersunited.org/example-strike"
    },
    {
      "id": "dispute-002",
      "company_name": "Tech Mega Corp",
      "domain": "techmega.com",
      "domains": ["www.techmega.com", "store.techmega.com"],
      "dispute_type": "Unfair Labor Practice",
      "description": "The company has been accused of anti-union activities and retaliation against organizing workers.",
      "start_date": "2024-02-01",
      "union": "Tech Workers Alliance",
      "status": "active",
      "more_info_url": "https://techworkersalliance.org/techmega"
    },
    {
      "id": "dispute-003",
      "company_name": "Retail Giant Inc",
      "domain": "retailgiant.com",
      "domains": ["www.retailgiant.com", "online.retailgiant.com", "m.retailgiant.com"],
      "dispute_type": "Lockout",
      "description": "Management has locked out workers following failed contract negotiations over healthcare benefits.",
      "start_date": "2024-01-20",
      "union": "Retail and Warehouse Workers Union",
      "status": "active",
      "more_info_url": "https://rwwu.org/retailgiant-lockout"
    }
  ],
  "last_updated": "2024-03-15T10:30:00Z"
}
```

## Testing with Mock Data

### Option 1: Local Mock Server

You can set up a simple local HTTP server to serve this mock data:

1. Save the JSON above to a file named `disputes.json`
2. Run a simple HTTP server:

```bash
# Python 3
python -m http.server 8080

# Or Node.js with http-server
npx http-server -p 8080
```

3. In the app Settings, set the API URL to:
```
http://10.0.2.2:8080/disputes.json
```
(Note: `10.0.2.2` is the special IP for accessing localhost from Android emulator)

### Option 2: Mock API Service

Use a service like [mockapi.io](https://mockapi.io/) or [json-server](https://github.com/typicode/json-server):

1. Create a project on mockapi.io
2. Create an endpoint called `/disputes`
3. Add the mock data
4. Use the provided URL in app Settings

### Option 3: Modify Repository for Testing

For unit testing, you can modify the `DisputeRepository` to use hardcoded test data:

```kotlin
// In DisputeRepository.kt, add a test method:
fun loadMockData() {
    cachedDisputes = listOf(
        LaborDispute(
            id = "dispute-001",
            companyName = "Example Corporation",
            domain = "example.com",
            domains = listOf("www.example.com", "shop.example.com"),
            disputeType = "Strike",
            description = "Test strike description",
            startDate = "2024-01-15",
            union = "Workers United Local 123",
            status = "active"
        )
    )
    lastFetchTime = System.currentTimeMillis()
}
```

## Expected API Endpoints

The app expects the following API structure:

### Primary Endpoint
```
GET https://api.onlinepicketline.org/api/disputes
```

### Alternative Endpoint
```
GET https://api.onlinepicketline.org/disputes.json
```

Both should return the same JSON structure shown above.

## API Field Descriptions

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| disputes | Array | Yes | Array of labor dispute objects |
| last_updated | String (ISO 8601) | No | Timestamp of last database update |

### Dispute Object Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | String | Yes | Unique identifier for the dispute |
| company_name | String | Yes | Name of the company |
| domain | String | Yes | Primary domain to block |
| domains | Array[String] | No | Additional domains to block |
| dispute_type | String | Yes | Type of dispute (Strike, Lockout, etc.) |
| description | String | Yes | Description of the dispute |
| start_date | String (ISO 8601) | Yes | When the dispute started |
| union | String | No | Union or organization involved |
| status | String | Yes | Must be "active" to be included in filtering |
| more_info_url | String | No | URL for more information |

## Testing Checklist

- [ ] App loads mock data successfully
- [ ] Domains are correctly matched (exact and subdomain)
- [ ] VPN service blocks requests to disputed domains
- [ ] Notifications appear when blocks occur
- [ ] User can view dispute details
- [ ] User can allow domains temporarily or permanently
- [ ] Allowed domains are persisted across app restarts
- [ ] Cache expires and refreshes after configured time
- [ ] App handles API errors gracefully
- [ ] App works offline with cached data
