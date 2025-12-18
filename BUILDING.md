# Building the Online Picket Line Android App

## Prerequisites

Before building the application, ensure you have the following installed:

1. **Android Studio**: Download from [developer.android.com](https://developer.android.com/studio)
   - Version: Arctic Fox (2020.3.1) or later
   
2. **Java Development Kit (JDK)**: 
   - Version: JDK 8 or higher (JDK 11 recommended)
   - Verify installation: `java -version`

3. **Android SDK**:
   - Minimum SDK: API 24 (Android 7.0)
   - Target SDK: API 34 (Android 14)
   - Build Tools: 34.0.0 or later

## Setup Steps

### 1. Clone the Repository

```bash
git clone https://github.com/oplfun/opl-for-android.git
cd opl-for-android
```

### 2. Configure Android SDK Path

Create a `local.properties` file in the project root if it doesn't exist:

```properties
sdk.dir=/path/to/your/android/sdk
```

**Common SDK locations:**
- **Linux**: `~/Android/Sdk` or `/usr/local/lib/android/sdk`
- **macOS**: `~/Library/Android/sdk`
- **Windows**: `C:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk`

### 3. Sync Gradle Dependencies

Open the project in Android Studio and let it sync Gradle dependencies automatically, or run:

```bash
./gradlew build --refresh-dependencies
```

## Building the App

### Using Android Studio

1. Open Android Studio
2. Click **File → Open** and select the project directory
3. Wait for Gradle sync to complete
4. Click **Build → Make Project** (or press `Ctrl+F9` / `Cmd+F9`)

### Using Command Line

#### Build Debug APK
```bash
./gradlew assembleDebug
```

The APK will be created at:
```
app/build/outputs/apk/debug/app-debug.apk
```

#### Build Release APK
```bash
./gradlew assembleRelease
```

The release APK will be at:
```
app/build/outputs/apk/release/app-release.apk
```

**Note:** Release builds require signing configuration in `app/build.gradle.kts`

#### Run All Tests
```bash
./gradlew test
```

#### Run Specific Test
```bash
./gradlew test --tests "com.oplfun.onlinepicketline.LaborDisputeTest"
```

## Installing the App

### On a Physical Device

1. Enable USB debugging on your Android device:
   - Go to **Settings → About Phone**
   - Tap **Build Number** 7 times to enable Developer Options
   - Go to **Settings → Developer Options**
   - Enable **USB Debugging**

2. Connect device via USB

3. Install the app:
```bash
./gradlew installDebug
```

Or manually install the APK:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### On an Emulator

1. Create an AVD (Android Virtual Device) in Android Studio:
   - **Tools → AVD Manager → Create Virtual Device**
   - Choose a device (e.g., Pixel 6)
   - Select system image (API 24 or higher)
   - Click **Finish**

2. Start the emulator

3. Install the app:
```bash
./gradlew installDebug
```

## Running the App

After installation, you can run the app:

### From Android Studio
Click the **Run** button or press `Shift+F10`

### From Command Line
```bash
./gradlew installDebug
adb shell am start -n com.oplfun.onlinepicketline/.MainActivity
```

## Troubleshooting

### Build Fails with "SDK location not found"

Create or update `local.properties` with the correct SDK path:
```properties
sdk.dir=/path/to/android/sdk
```

### Gradle Sync Fails

1. Clear Gradle cache:
```bash
./gradlew clean
rm -rf ~/.gradle/caches/
```

2. Re-sync:
```bash
./gradlew build --refresh-dependencies
```

### Dependencies Download Issues

If you're behind a proxy or firewall:

1. Add to `gradle.properties`:
```properties
systemProp.http.proxyHost=proxy.company.com
systemProp.http.proxyPort=8080
systemProp.https.proxyHost=proxy.company.com
systemProp.https.proxyPort=8080
```

### Build Tools Not Found

Open Android Studio SDK Manager and ensure you have:
- Android SDK Build-Tools 34.0.0 or later
- Android SDK Platform 34
- Android SDK Platform-Tools

## Project Structure Overview

```
opl-for-android/
├── app/
│   ├── build.gradle.kts          # App module build configuration
│   ├── proguard-rules.pro        # ProGuard rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/oplfun/onlinepicketline/
│       │   │   ├── MainActivity.kt
│       │   │   ├── SettingsActivity.kt
│       │   │   ├── data/         # Data layer (API, models, repository)
│       │   │   ├── vpn/          # VPN service
│       │   │   └── ui/           # UI components
│       │   └── res/              # Resources (layouts, strings, etc.)
│       └── test/                 # Unit tests
├── build.gradle.kts              # Root build configuration
├── settings.gradle.kts           # Project settings
├── gradle.properties             # Gradle properties
└── local.properties              # Local SDK path (not in git)
```

## Development Tips

### Enable Instant Run
In Android Studio: **File → Settings → Build, Execution, Deployment → Instant Run**

### View Logs
```bash
adb logcat | grep "PicketLine"
```

### Debug the VPN Service
```bash
adb logcat | grep "PicketLineVpnService"
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Android CI

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'
        
    - name: Setup Android SDK
      uses: android-actions/setup-android@v2
      
    - name: Build with Gradle
      run: ./gradlew build
      
    - name: Run tests
      run: ./gradlew test
```

## Additional Resources

- [Android Developer Documentation](https://developer.android.com/docs)
- [Gradle Build Tool](https://gradle.org/)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [VPN Service Guide](https://developer.android.com/guide/topics/connectivity/vpn)

## Support

For build issues:
1. Check the [Issues](https://github.com/oplfun/opl-for-android/issues) page
2. Create a new issue with:
   - Your OS and Android Studio version
   - Full error output
   - Steps to reproduce

## License

See the LICENSE file in the repository root.
