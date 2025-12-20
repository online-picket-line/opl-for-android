# Online Picket Line for Android

Android phone application that filters outgoing traffic to inform users when they are accessing a company currently under a labor dispute.

## Overview

This application uses VPN technology to monitor outgoing network traffic and compares accessed domains against a database of companies involved in labor disputes. When a match is found, the app blocks the request and notifies the user, who can then choose to:
- Keep the site blocked
- Allow access this one time
- Always allow access to this domain

## Features

- **Real-time Traffic Monitoring**: Uses Android VPN Service to intercept outgoing traffic
- **Labor Dispute Database**: Fetches and caches information from the Online Picket Line API
- **User Control**: Users can override blocks and manage allowed domains
- **Notifications**: Informs users when accessing disputed companies
- **Privacy First**: All filtering happens locally on the device

## Requirements

- Android 7.0 (API level 24) or higher
- Internet connection for fetching dispute data
- VPN permission (requested at runtime)

## Building the Project

### Prerequisites

- Android Studio Arctic Fox or later
- JDK 8 or higher
- Android SDK with API level 34

### Build Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/online-picket-line/opl-for-android.git
   cd opl-for-android
   ```

2. Open the project in Android Studio

3. Sync Gradle files

4. Build the project:
   ```bash
   ./gradlew build
   ```

5. Run on device or emulator:
   ```bash
   ./gradlew installDebug
   ```

## Usage

1. **Launch the App**: Open Online Picket Line from your app drawer

2. **Start Protection**: Tap "Start Protection" and grant VPN permission when prompted

3. **Browse Normally**: The app runs in the background monitoring your traffic

4. **Handle Blocks**: When accessing a disputed company's site, you'll receive a notification with details about the labor dispute and options to proceed

5. **Manage Settings**:
   - Configure API URL in Settings
   - View and manage allowed domains
   - Refresh dispute data manually

## API Integration

The app communicates with the Online Picket Line API to fetch information about companies under labor disputes. 

### Expected API Response Format

```json
{
  "disputes": [
    {
      "id": "dispute-id",
      "company_name": "Company Name",
      "domain": "example.com",
      "domains": ["www.example.com", "shop.example.com"],
      "dispute_type": "Strike",
      "description": "Workers are on strike for better wages",
      "start_date": "2024-01-01",
      "union": "Workers Union Local 123",
      "status": "active",
      "more_info_url": "https://example.org/info"
    }
  ],
  "last_updated": "2024-01-15T12:00:00Z"
}
```

### Configuring API URL

By default, the app uses `https://api.onlinepicketline.org/` as the base URL. You can change this in the Settings screen.

## Architecture

The app follows modern Android development practices:

- **Kotlin**: Primary programming language
- **MVVM Pattern**: Separation of concerns
- **Coroutines**: Asynchronous operations
- **Retrofit**: Network communication
- **Material Design**: UI components
- **VPN Service**: Traffic interception

### Project Structure

```
app/src/main/java/com/onlinepicketline/onlinepicketline/
├── data/
│   ├── api/           # API service definitions
│   ├── model/         # Data models
│   └── repository/    # Data repository layer
├── vpn/               # VPN service implementation
├── ui/                # UI components and adapters
├── MainActivity.kt    # Main app screen
└── SettingsActivity.kt # Settings screen
```

## Permissions

The app requires the following permissions:

- `INTERNET`: To fetch dispute data from API
- `BIND_VPN_SERVICE`: To create VPN connection for traffic monitoring
- `FOREGROUND_SERVICE`: To run VPN service in foreground
- `POST_NOTIFICATIONS`: To notify users about blocked sites

## Privacy & Security

- All traffic filtering occurs locally on your device
- No browsing data is sent to external servers
- Only domain names are checked against the dispute database
- VPN connection is used solely for filtering, not routing through external servers

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

## License

This project is open source. Please check the LICENSE file for details.

## Support

For issues, questions, or contributions, please visit:
https://github.com/online-picket-line/opl-for-android

## Acknowledgments

- Built to support workers and labor movements
- Inspired by the digital picket line concept
- Uses the Online Picket Line API for dispute information
