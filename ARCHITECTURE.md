# Architecture Overview

## High-Level Architecture

The Online Picket Line Android app follows clean architecture principles with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────────┐
│                        Presentation Layer                        │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐                 │
│  │ MainActivity│  │ Settings   │  │  Dispute   │                 │
│  │            │  │  Activity  │  │  Adapter   │                 │
│  └────────────┘  └────────────┘  └────────────┘                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Domain Layer                             │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              DisputeRepository                            │  │
│  │  • Fetch disputes from API                                │  │
│  │  • Cache management                                       │  │
│  │  • Domain matching logic                                  │  │
│  │  • Allowed domains management                             │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                          Data Layer                              │
│  ┌────────────────┐    ┌──────────────────┐                     │
│  │  API Service   │    │  Local Storage   │                     │
│  │  • Retrofit    │    │  • SharedPrefs   │                     │
│  │  • OkHttp      │    │  • Cache         │                     │
│  └────────────────┘    └──────────────────┘                     │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      VPN Service Layer                           │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │           PicketLineVpnService                            │  │
│  │  • Intercept network packets                              │  │
│  │  • Parse destination domains                              │  │
│  │  • Check against dispute list                             │  │
│  │  • Block/Allow traffic                                    │  │
│  │  • Send notifications                                     │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## Data Flow

### 1. Fetching Dispute Data

```
User Launches App
      │
      ▼
MainActivity.onCreate()
      │
      ▼
DisputeRepository.fetchDisputes()
      │
      ├─→ Check Cache Valid?
      │   └─→ Yes: Return Cached Data
      │
      └─→ No: API Call
          │
          ▼
      PicketLineApiService.getDisputedCompanies()
          │
          ▼
      Parse Response
          │
          ▼
      Update Cache
          │
          ▼
      Return Disputes List
          │
          ▼
      Update UI (RecyclerView)
```

### 2. Traffic Filtering Flow

```
User Starts VPN
      │
      ▼
MainActivity.startVpnService()
      │
      ▼
PicketLineVpnService.onStartCommand()
      │
      ▼
Establish VPN Interface
      │
      ▼
Start Packet Processing Loop
      │
      ├─→ Read Packet
      │   │
      │   ▼
      │   Parse IP Header
      │   │
      │   ▼
      │   Extract Destination Domain
      │   │
      │   ▼
      │   DisputeRepository.findDisputeForDomain()
      │   │
      │   ├─→ Match Found?
      │   │   │
      │   │   └─→ Yes: Block Packet
      │   │       │
      │   │       ▼
      │   │       Send Notification
      │   │       │
      │   │       ▼
      │   │       Show Alert Dialog
      │   │       │
      │   │       ▼
      │   │       User Decision:
      │   │       ├─→ Keep Blocking
      │   │       ├─→ Allow Once
      │   │       └─→ Allow Always
      │   │
      │   └─→ No: Forward Packet
      │
      └─→ Loop Back
```

### 3. User Override Flow

```
User Receives Block Notification
      │
      ▼
Taps Notification
      │
      ▼
MainActivity.showBlockDialog()
      │
      ▼
Display Dispute Details
      │
      ├─→ User Selects "Keep Blocking"
      │   └─→ Dismiss Dialog
      │
      ├─→ User Selects "Allow This Time"
      │   └─→ Continue Blocking for Session
      │
      └─→ User Selects "Always Allow"
          │
          ▼
          DisputeRepository.allowDomain()
          │
          ▼
          Save to SharedPreferences
          │
          ▼
          Future Requests Allowed
```

## Component Details

### MainActivity
**Responsibilities:**
- Display app status (VPN active/inactive)
- Show list of current disputes
- Handle VPN permission requests
- Manage start/stop VPN controls
- Navigate to settings

**Key Methods:**
- `requestVpnPermission()`: Request Android VPN permission
- `startVpnService()`: Start the VPN service
- `stopVpnService()`: Stop the VPN service
- `refreshDisputes()`: Fetch latest dispute data
- `showBlockDialog()`: Display block notification with options

### PicketLineVpnService
**Responsibilities:**
- Create and manage VPN tunnel
- Intercept all outgoing network traffic
- Parse IP packets to extract destinations
- Match domains against dispute database
- Block or forward packets based on policy
- Notify user of blocked requests

**Key Methods:**
- `startVpn()`: Establish VPN connection
- `stopVpn()`: Tear down VPN connection
- `processPackets()`: Main packet processing loop
- `checkAndBlockPacket()`: Determine if packet should be blocked
- `notifyBlock()`: Send notification for blocked domain

### DisputeRepository
**Responsibilities:**
- Manage dispute data lifecycle
- Cache dispute information
- Provide domain matching functionality
- Store and retrieve allowed domains
- Handle API communication errors

**Key Methods:**
- `fetchDisputes()`: Retrieve disputes from API
- `findDisputeForDomain()`: Check if domain matches any dispute
- `allowDomain()`: Mark domain as allowed
- `isAllowedDomain()`: Check if domain is in allow list
- `getCachedDisputes()`: Get cached dispute list

### API Client
**Responsibilities:**
- Configure HTTP client (Retrofit + OkHttp)
- Define API endpoints
- Handle network requests/responses
- Manage timeouts and retries

**Endpoints:**
- `GET /api/disputes`: Primary dispute data endpoint
- `GET /disputes.json`: Alternative endpoint

## Data Models

### LaborDispute
```kotlin
data class LaborDispute(
    id: String,              // Unique identifier
    companyName: String,     // Company name
    domain: String,          // Primary domain
    domains: List<String>?,  // Additional domains
    disputeType: String,     // Type (Strike, Lockout, etc.)
    description: String,     // Dispute description
    startDate: String,       // When it started
    union: String?,          // Union involved
    status: String,          // Must be "active"
    moreInfoUrl: String?     // More info URL
)
```

### BlockedRequest
```kotlin
data class BlockedRequest(
    domain: String,          // Blocked domain
    url: String,             // Full URL
    dispute: LaborDispute,   // Associated dispute
    timestamp: Long          // When blocked
)
```

## Security Considerations

### Privacy Protection
- **Local Processing**: All filtering happens on-device
- **No Data Collection**: No browsing history is stored or transmitted
- **Minimal Logging**: Only debug logs for troubleshooting

### VPN Security
- **Local Only**: VPN doesn't route through external servers
- **Packet Inspection**: Only destination domains are examined
- **No MITM**: App doesn't decrypt HTTPS traffic

### API Communication
- **HTTPS Only**: All API calls use encrypted connections
- **Certificate Pinning**: Can be added for enhanced security
- **Input Validation**: All API responses are validated

## Performance Optimizations

### Caching Strategy
- **Duration**: 1-hour cache lifetime
- **Automatic Refresh**: Fetch new data when cache expires
- **Background Updates**: Can refresh while VPN is active

### Memory Management
- **Efficient Data Structures**: Use Sets for O(1) lookups
- **Lazy Loading**: Initialize API client only when needed
- **Packet Buffering**: Limited buffer size to prevent OOM

### Battery Optimization
- **Efficient Packet Processing**: Minimal CPU usage per packet
- **Foreground Service**: Properly manages battery impact
- **Smart Notifications**: Debounce to prevent notification spam

## Testing Strategy

### Unit Tests
- Domain matching logic
- Data model transformations
- Repository cache management

### Integration Tests
- API communication
- Data persistence
- VPN service lifecycle

### UI Tests
- User flows (start/stop VPN)
- Settings management
- Dialog interactions

### Manual Testing
- Real network traffic filtering
- Various Android versions
- Different network conditions
- Battery impact assessment

## Future Enhancements

### Planned Features
1. **Statistics Dashboard**: Show blocked requests over time
2. **Custom Lists**: User-defined block/allow lists
3. **Scheduled Filtering**: Enable/disable on schedule
4. **Export Data**: Export blocking history

### Technical Improvements
1. **IPv6 Support**: Handle IPv6 packets
2. **DNS Filtering**: Block at DNS level for efficiency
3. **Split Tunneling**: Selective app filtering
4. **Low-Level Optimization**: Native code for packet processing

## Dependencies

### Core Libraries
- **Kotlin Coroutines**: Asynchronous operations
- **Retrofit**: REST API client
- **OkHttp**: HTTP client
- **Gson**: JSON parsing
- **Material Components**: UI components
- **AndroidX Libraries**: Core Android utilities

### Build Tools
- **Gradle**: Build automation
- **Android Gradle Plugin**: Android-specific build config
- **Kotlin Gradle Plugin**: Kotlin compilation

## Deployment

### Release Process
1. Update version in `build.gradle.kts`
2. Run tests: `./gradlew test`
3. Build release APK: `./gradlew assembleRelease`
4. Sign APK with release key
5. Test on multiple devices
6. Upload to GitHub Releases
7. Optional: Publish to F-Droid or Play Store

### Version Naming
- Format: `MAJOR.MINOR.PATCH`
- Example: `1.0.0` → first stable release
- Increment MAJOR for breaking changes
- Increment MINOR for new features
- Increment PATCH for bug fixes
