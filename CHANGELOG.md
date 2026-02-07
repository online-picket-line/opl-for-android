# Changelog

All notable changes to the Online Picket Line Android app will be documented in this file.

This project follows [Semantic Versioning](https://semver.org/).

## [1.0.0] - 2025-01-01

### Added
- Complete native Android app with VPN-based DNS traffic filtering
- Local VPN service that intercepts DNS queries and checks against employer blocklist
- GPS proximity alerts for active picket lines within configurable radius (default 100 miles)
- GPS Snapshot feature for submitting location data to augment strike information
- Strike submission wizard for reporting new labor actions
- Dashboard with VPN toggle, active strike statistics, and nearby geofence cards
- Block page with solidarity message, employer info, and allow/block user actions
- Settings activity with notification, auto-block, and GPS toggles
- API key authentication via EncryptedSharedPreferences
- Hash-based caching (SHA-256) with HTTP 304 support for efficient data sync
- Geocoding and reverse geocoding via OPL Mobile API
- Boot receiver for auto-restarting VPN after device restart
- Material Design 3 UI with dark theme for block pages
- Unit tests for models, DNS parsing, URL matching, and distance calculations
- CI/CD workflow with lint, test, security (CodeQL), build, and release jobs
- Google Play Store submission guide

### Technical Details
- Kotlin 2.1.0, compileSdk 35, minSdk 26, targetSdk 35
- Retrofit 2.11.0 + OkHttp 4.12.0 for networking
- Google Play Services Location 21.3.0
- EncryptedSharedPreferences for secure API key storage
- Coroutines 1.9.0 for async operations
- WorkManager 2.10.0 for background data sync
- Package: com.onlinepicketline.opl
