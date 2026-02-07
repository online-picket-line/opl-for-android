# Online Picket Line for Android

Native Android application that helps workers stand in solidarity with active labor disputes. The app uses local VPN technology to notify users when accessing companies with active strikes, provides GPS-based proximity alerts for nearby picket lines, and allows users to submit strike reports and GPS location snapshots.

## Features

- **VPN-Based Traffic Filtering**: Uses Android VPN Service to intercept DNS queries locally and notify users when accessing sites of employers with active labor disputes
- **GPS Strike Proximity Alerts**: Monitors your location and alerts you when within 100 miles of an active picket line
- **GPS Snapshot**: Submit your GPS coordinates to augment strike location data
- **Strike Submission**: Report new labor actions through a built-in submission wizard
- **Hash-Based Caching**: Efficient data syncing using SHA-256 content hashes and HTTP 304 responses
- **Privacy First**: All traffic filtering happens locally on-device — no browsing data leaves the app

## Requirements

- Android 8.0 (API level 26) or higher
- Internet connection for fetching strike data
- VPN permission (requested at runtime)
- Location permission (optional, for GPS features)
- API key from your OPL administrator

## Building

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK with compileSdk 35

### Build Steps

```bash
git clone https://github.com/oplfun/opl-for-android.git
cd opl-for-android

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run tests
./gradlew test
```

### Configuration

The API base URL defaults to `https://onlinepicketline.com`. To override for development, set the `API_BASE_URL` build config field in `app/build.gradle.kts`.

## Architecture

```
com.onlinepicketline.opl/
├── data/
│   ├── api/           # Retrofit API service + OkHttp client
│   ├── model/         # Data models (API request/response types)
│   └── repository/    # Repository with hash-based caching
├── ui/                # Activities (Main, Settings, GPS Snapshot, Submit Strike, Block Page)
├── vpn/               # VPN service with DNS interception
├── util/              # SecureStorage (EncryptedSharedPreferences), LocationUtils
└── receiver/          # BootReceiver for auto-restart
```

### Key Technologies

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 2.1.0 |
| HTTP Client | Retrofit 2.11.0 + OkHttp 4.12.0 |
| Secure Storage | EncryptedSharedPreferences |
| Location | Google Play Services Location 21.3.0 |
| UI | Material Design 3 + ViewBinding |
| Async | Kotlin Coroutines 1.9.0 |
| Background | WorkManager 2.10.0 |

## API Integration

The app communicates with the OPL Mobile API using an `X-API-Key` header. Keys use the format `opl_` + 64 hex characters (68 chars total).

### Endpoints

| Method | Endpoint | Scope | Description |
|--------|----------|-------|-------------|
| GET | `/api/mobile/data` | `read:mobile` | Combined blocklist + geofences |
| GET | `/api/mobile/active-strikes` | `read:mobile` | List of active strikes |
| POST | `/api/mobile/gps-snapshot` | `write:gps-snapshot` | Submit GPS location snapshot |
| POST | `/api/mobile/submit-strike` | `write:submit-strike` | Submit new strike report |
| POST | `/api/mobile/geocode` | `read:mobile` | Forward geocoding |
| POST | `/api/mobile/reverse-geocode` | `read:mobile` | Reverse geocoding |

## Usage

1. **API Key Setup**: Enter your API key on first launch
2. **Enable VPN**: Tap the VPN toggle to start monitoring traffic
3. **Browse Normally**: You'll get notified when accessing an employer with an active dispute
4. **Check Nearby Strikes**: The dashboard shows strikes within your configured radius
5. **Submit Data**: Use the GPS Snapshot or Submit Strike forms to contribute data

## Testing

```bash
# Unit tests
./gradlew test

# Specific test class
./gradlew test --tests "com.onlinepicketline.opl.data.model.ModelsTest"
```

Tests cover:
- Data model serialization/deserialization
- DNS packet parsing in VPN service
- URL matching logic in repository
- Distance calculations

## Security

- API keys stored in EncryptedSharedPreferences (AES-256-GCM + RSA)
- All network traffic uses HTTPS
- No browsing history or DNS queries stored or transmitted
- VPN runs entirely locally — no remote tunnel
- API key format validated before use (`opl_` prefix, 68 chars)

## License

GPL-3.0 — see [LICENSE](LICENSE)
