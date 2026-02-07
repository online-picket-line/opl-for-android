# Google Play Store Submission Guide

This document provides step-by-step instructions for submitting the Online Picket Line Android app to the Google Play Store.

## Prerequisites

1. **Google Play Developer Account** ($25 one-time registration fee)
   - Register at https://play.google.com/console/signup
2. **Signed Release APK/AAB** (Android App Bundle preferred)
3. **App listing assets** (screenshots, icons, descriptions)

## Step 1: Create the App in Google Play Console

1. Go to https://play.google.com/console
2. Click **"Create app"**
3. Fill in the app details:
   - **App name**: `Online Picket Line`
   - **Default language**: English (United States)
   - **App or game**: App
   - **Free or paid**: Free
4. Accept the declarations and click **"Create app"**

## Step 2: Store Listing

### App Details
- **App name**: `Online Picket Line`
- **Short description** (80 chars max):
  > Support labor actions with real-time strike alerts and network awareness.
- **Full description** (4000 chars max):
  > Online Picket Line helps you stand in solidarity with workers engaged in labor actions. The app provides:
  >
  > **Strike Proximity Alerts**
  > Receive notifications when you are near an active strike, picket line, or boycott location. The app uses GPS geofencing to alert you when entering or approaching a strike zone.
  >
  > **Network-Level Awareness**
  > The app uses a local VPN service to analyze your network traffic and notify you when you are accessing services provided by employers involved in active labor disputes. You choose whether to proceed or support the action — the choice is always yours.
  >
  > **GPS Snapshot Submission**
  > Help improve strike location data by submitting GPS snapshots of picket lines and protest locations you encounter. Select an active strike and submit your current location or manually enter an address.
  >
  > **Submit New Strikes**
  > Know about a labor action that is not in our database? Submit it directly from the app. Your submission will be reviewed by moderators before publication.
  >
  > **Privacy-First Design**
  > • All network traffic analysis happens on your device — no data is sent to remote servers
  > • GPS location is only transmitted when you explicitly choose to submit a snapshot
  > • No user accounts, analytics, or tracking
  > • The VPN is local-only and does not route traffic through any proxy
  >
  > Online Picket Line is an open-source project committed to supporting workers' rights.

### Graphics Assets
- **App icon**: 512x512 PNG (32-bit, no alpha)
- **Feature graphic**: 1024x500 PNG or JPEG
- **Screenshots**: Minimum 2, maximum 8 per device type
  - Phone: 16:9 or 9:16 aspect ratio, min 320px, max 3840px
  - Tablet (7"): At least one screenshot recommended
  - Tablet (10"): At least one screenshot recommended

### Categorization
- **App category**: Social
- **Tags**: `labor`, `union`, `workers rights`, `strike`, `solidarity`

### Contact Details
- **Email**: (project contact email)
- **Website**: (project website URL)

## Step 3: Content Rating

1. Go to **Policy and programs** > **Content rating**
2. Fill out the IARC questionnaire:
   - **Violence**: None
   - **Sexual content**: None
   - **Controlled substance**: None
   - **User interaction**: Users can submit content (GPS data, strike reports)
   - **Location data**: Yes, the app accesses user location
   - **In-app purchases**: None
3. Expected rating: **Everyone** or **Everyone 10+**

## Step 4: App Content

### Privacy Policy
- Required since the app accesses location data and uses VPN
- Host the privacy policy at a public URL
- Key points to cover:
  - App uses location services for strike proximity alerts
  - Location data is processed on-device; only sent with explicit user action
  - VPN service is local-only; no traffic leaves the device via the VPN
  - No personal data is collected, stored, or shared
  - No analytics or third-party tracking SDKs

### Data Safety
Fill out the data safety form:
- **Data collected**:
  - Location: Approximate and precise (optional, with user consent)
- **Data shared**: None
- **Data handling**:
  - Location data is processed on-device
  - GPS snapshots are transmitted only with explicit user action
  - Data is not linked to user identity
- **Security practices**:
  - Data in transit encrypted (HTTPS/TLS)
  - Data deletion: No persistent user data stored

### VPN Permission Declaration
Since the app uses `VpnService`, you must declare its use:
1. Go to **Policy and programs** > **App content**
2. Under **VPN service**, declare:
   - The VPN is used for local network traffic analysis only
   - No traffic is routed to external servers
   - The VPN does not collect, log, or transmit user browsing data
   - Purpose: To identify network requests to employers involved in labor disputes

**Important**: Google requires a VPN permission declaration form. Fill out:
- **VPN usage purpose**: Content filtering and awareness
- **Does the VPN route traffic to an external server?**: No
- **Does the VPN modify, redirect, or block traffic?**: Yes (blocks access to specific domains with user consent)
- **Does the VPN log or store user traffic data?**: No

## Step 5: Release Management

### Create a Release
1. Go to **Release** > **Production** (or start with **Internal testing**)
2. Click **"Create new release"**
3. Upload the signed AAB (Android App Bundle)
4. Add release notes:
   > **What's New**
   > - Initial release
   > - Real-time strike proximity alerts with GPS geofencing
   > - Network traffic awareness via local VPN
   > - GPS snapshot submission for strike location data
   > - Submit new labor action reports
5. Click **"Review release"** then **"Start rollout"**

### Recommended Release Strategy
1. **Internal testing** → Small team validation
2. **Closed testing** → Beta testers via email list
3. **Open testing** → Public beta
4. **Production** → Full release

## Step 6: App Signing

### Google Play App Signing (Recommended)
1. Let Google manage your app signing key
2. Upload your upload key for signing builds
3. This enables key rotation and recovery

### Manual Signing
```bash
# Generate a keystore
keytool -genkey -v -keystore release.keystore \
  -alias opl-android -keyalg RSA -keysize 2048 -validity 10000

# Sign the AAB
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore release.keystore app-release.aab opl-android
```

## Step 7: Review Checklist

Before submitting for review:

- [ ] Store listing complete (name, descriptions, screenshots, icon)
- [ ] Content rating questionnaire completed
- [ ] Privacy policy URL set and accessible
- [ ] Data safety form completed
- [ ] VPN permission declaration submitted
- [ ] Target API level meets Google Play requirements (currently API 34)
- [ ] App tested on multiple device sizes
- [ ] Release notes written
- [ ] App signing configured
- [ ] No crashes or ANRs in testing

## Ongoing Maintenance

- **Policy updates**: Google Play policies change regularly; review quarterly
- **Target API level**: Must target latest API level within one year of release
- **Security updates**: Address any flagged vulnerabilities promptly
- **User reviews**: Monitor and respond to user feedback
