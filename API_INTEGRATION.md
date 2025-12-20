# API Integration Guide

This guide explains how to integrate the Online Picket Line Android app with the backend API.

## API Requirements

The app expects a RESTful API that provides information about companies currently involved in labor disputes.

### Base URL

The default base URL is configured as:
```
https://api.onlinepicketline.org/
```

Users can change this in the app's Settings screen.

### Endpoints

#### GET /api/disputes

Returns the current list of active labor disputes.

**Response Format:**
```json
{
  "disputes": [
    {
      "id": "string",
      "company_name": "string",
      "domain": "string",
      "domains": ["string"],
      "dispute_type": "string",
      "description": "string",
      "start_date": "string (ISO 8601)",
      "union": "string",
      "status": "string",
      "more_info_url": "string"
    }
  ],
  "last_updated": "string (ISO 8601)"
}
```

#### Alternative: GET /disputes.json

If the primary endpoint is unavailable, the app will try this alternative endpoint with the same response format.

## Field Specifications

### Required Fields

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| id | string | Unique identifier for the dispute | "dispute-001" |
| company_name | string | Official name of the company | "Example Corporation" |
| domain | string | Primary domain to block | "example.com" |
| dispute_type | string | Type of labor dispute | "Strike", "Lockout", "Unfair Labor Practice" |
| description | string | Description of the dispute and context | "Workers striking for better wages..." |
| start_date | string | ISO 8601 date when dispute started | "2024-01-15" or "2024-01-15T10:30:00Z" |
| status | string | Current status (only "active" disputes are used) | "active", "resolved", "suspended" |

### Optional Fields

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| domains | array[string] | Additional domains to block | ["www.example.com", "shop.example.com"] |
| union | string | Union or organization involved | "Workers United Local 123" |
| more_info_url | string | URL for additional information | "https://union.org/example-dispute" |
| last_updated | string | ISO 8601 timestamp of last update | "2024-01-20T15:45:00Z" |

## Domain Matching

The app performs domain matching as follows:

### Exact Match
```
dispute.domain = "example.com"
matches: "example.com"
```

### Subdomain Match
```
dispute.domain = "example.com"
matches: "www.example.com", "shop.example.com", "api.example.com"
```

### Multiple Domains
```
dispute.domain = "example.com"
dispute.domains = ["example.org", "example.net"]
matches: "example.com", "example.org", "example.net" and all their subdomains
```

### Case Insensitive
All domain matching is case-insensitive.

## Status Filtering

Only disputes with `status = "active"` are used for filtering. This allows the API to maintain historical records while only enforcing current disputes.

**Status Values:**
- `"active"`: Currently blocking traffic
- `"resolved"`: Dispute has been resolved
- `"suspended"`: Temporarily suspended
- `"pending"`: Not yet active

## API Response Examples

### Minimal Valid Response
```json
{
  "disputes": [
    {
      "id": "001",
      "company_name": "Test Corp",
      "domain": "testcorp.com",
      "dispute_type": "Strike",
      "description": "Workers on strike",
      "start_date": "2024-01-15",
      "status": "active"
    }
  ]
}
```

### Complete Response
```json
{
  "disputes": [
    {
      "id": "dispute-001",
      "company_name": "Example Corporation",
      "domain": "example.com",
      "domains": [
        "www.example.com",
        "shop.example.com",
        "api.example.com"
      ],
      "dispute_type": "Strike",
      "description": "Workers are striking for better wages and improved working conditions. Negotiations have been ongoing for 6 months without resolution.",
      "start_date": "2024-01-15T09:00:00Z",
      "union": "Workers United Local 123",
      "status": "active",
      "more_info_url": "https://workersunited.org/example-strike"
    },
    {
      "id": "dispute-002",
      "company_name": "Tech Mega Corp",
      "domain": "techmega.com",
      "domains": [
        "www.techmega.com",
        "store.techmega.com"
      ],
      "dispute_type": "Unfair Labor Practice",
      "description": "The company has engaged in anti-union activities and retaliation against organizing workers.",
      "start_date": "2024-02-01",
      "union": "Tech Workers Alliance",
      "status": "active",
      "more_info_url": "https://techworkersalliance.org/techmega"
    }
  ],
  "last_updated": "2024-03-15T10:30:00Z"
}
```

### Empty Response (No Active Disputes)
```json
{
  "disputes": [],
  "last_updated": "2024-03-15T10:30:00Z"
}
```

## Error Handling

The app handles various error scenarios:

### Network Errors
- Connection timeout
- DNS resolution failure
- Server unreachable

**Behavior:** Uses cached data if available, shows error message to user.

### HTTP Errors

#### 404 Not Found
**Behavior:** Tries alternative endpoint (`/disputes.json`), then falls back to cache.

#### 500 Server Error
**Behavior:** Uses cached data, retries on next refresh.

#### 401/403 Authentication Error
**Behavior:** Shows error to user, uses cached data.

### Response Validation

The app validates:
- JSON structure is correct
- Required fields are present
- Field types match expectations
- Domain names are valid

**Invalid Response Behavior:** Discards invalid entries, uses valid ones, logs errors.

## Caching Strategy

### Cache Duration
Default: 1 hour (3600 seconds)

### Cache Behavior
1. On first launch: Fetch from API
2. On subsequent launches:
   - If cache < 1 hour old: Use cache
   - If cache > 1 hour old: Fetch from API
3. Manual refresh: Always fetch from API
4. Network error: Use cache regardless of age

### Cache Storage
- Location: SharedPreferences + in-memory
- Persistence: Survives app restarts
- Cleared: When user clears app data

## Rate Limiting

The app automatically rate limits API requests:

- Maximum 1 request per minute
- Manual refresh: Not rate limited
- Automatic background refresh: Rate limited

**Recommendation:** API should support reasonable request rates from mobile clients.

## Authentication (Optional)

If your API requires authentication:

### API Key
Add header to requests:
```
Authorization: Bearer YOUR_API_KEY
```

**Implementation:** Modify `ApiClient.kt` to add interceptor:

```kotlin
private fun createOkHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $API_KEY")
                .build()
            chain.proceed(request)
        }
        .build()
}
```

### OAuth 2.0
For OAuth, implement token refresh logic in the API client.

## Testing Your API

### Using curl
```bash
curl -X GET https://api.onlinepicketline.org/api/disputes \
  -H "Accept: application/json"
```

### Using Postman
1. Create GET request to `/api/disputes`
2. Check response matches schema
3. Verify all required fields present

### Using the App
1. Install the app
2. Go to Settings
3. Set API URL to your endpoint
4. Return to main screen
5. Tap "Refresh Disputes"
6. Check logs for any errors

## Performance Recommendations

### Response Size
- Keep response under 1MB
- Consider pagination for large datasets
- Only include active disputes

### Response Time
- Target: < 2 seconds
- Maximum: < 5 seconds
- Use CDN for static data

### Compression
Enable gzip compression:
```
Content-Encoding: gzip
```

### Caching Headers
Set appropriate cache headers:
```
Cache-Control: public, max-age=3600
ETag: "dispute-list-hash"
```

## API Versioning

Recommended versioning strategy:

### URL Versioning
```
https://api.onlinepicketline.org/v1/api/disputes
```

### Header Versioning
```
Accept: application/vnd.onlinepicketline.v1+json
```

## Monitoring & Analytics

### Metrics to Track
- Request count per client
- Response time (p50, p95, p99)
- Error rate
- Cache hit rate

### Logging
Log important events:
- API requests
- Validation errors
- Dispute updates

## Security Considerations

### HTTPS Required
All API endpoints must use HTTPS.

### Data Validation
- Sanitize all inputs
- Validate domain names
- Check for malicious content

### Privacy
- Don't require user identification
- Minimize data collection
- No tracking of individual requests

## Support

### API Issues
If users report API problems:
1. Check API status
2. Verify response format
3. Check server logs
4. Test with curl/Postman

### Reporting Problems
Users can report issues at:
https://github.com/online-picket-line/opl-for-android/issues

## Example Implementation

### Simple Express.js API

```javascript
const express = require('express');
const app = express();

app.get('/api/disputes', (req, res) => {
  res.json({
    disputes: [
      {
        id: "001",
        company_name: "Example Corp",
        domain: "example.com",
        dispute_type: "Strike",
        description: "Workers on strike",
        start_date: "2024-01-15",
        status: "active"
      }
    ],
    last_updated: new Date().toISOString()
  });
});

app.listen(3000);
```

### Python Flask API

```python
from flask import Flask, jsonify
from datetime import datetime

app = Flask(__name__)

@app.route('/api/disputes')
def get_disputes():
    return jsonify({
        'disputes': [
            {
                'id': '001',
                'company_name': 'Example Corp',
                'domain': 'example.com',
                'dispute_type': 'Strike',
                'description': 'Workers on strike',
                'start_date': '2024-01-15',
                'status': 'active'
            }
        ],
        'last_updated': datetime.utcnow().isoformat()
    })

if __name__ == '__main__':
    app.run(port=3000)
```

## FAQ

**Q: Can I host the API on any platform?**
A: Yes, as long as it's accessible via HTTPS and returns the correct JSON format.

**Q: How often should the data be updated?**
A: Update as needed, but consider the 1-hour cache on the client side.

**Q: Can I add custom fields?**
A: Yes, the app ignores unknown fields. Document any extensions.

**Q: What if my API is temporarily down?**
A: The app will use cached data and retry later.

**Q: Do I need to implement authentication?**
A: Not required, but you can add it if needed.
