# Android Ambulance MVP

This module contains the first Android MVP for the ambulance service.

## Current scope

- Public ambulance booking only
- No login required
- Sends booking requests to the backend `POST /api/v1/bookings`
- Uses `ONLINE_MOBILE` as the booking channel
- Required fields only:
  - contact name
  - contact phone
  - pickup address
  - destination address
  - ambulance type

## Environment setup

The app is configured with product flavors:

- `local` → default base URL `http://192.168.0.18:8080/` (Laptop on Wi-Fi) or http://10.0.2.2:8080/
- `render` → placeholder Render URL for future deployment

You can override these URLs with Gradle properties:

- `API_BASE_URL_LOCAL`
- `API_BASE_URL_RENDER`

Example `gradle.properties` entries:

```properties
API_BASE_URL_LOCAL=http://192.168.0.18:8080/ or http://10.0.2.2:8080/
API_BASE_URL_RENDER=https://your-render-service.onrender.com/
```

## Google Maps pickup selection

The pickup selector uses the Google Maps SDK for Android. Map taps select a
marker; **Confirm** saves its coordinates to the booking. **Reset** restores the
previous selection, and **Cancel** or dismissing the sheet discards changes.
The address must still be entered manually; this does not add Places search,
reverse geocoding, or GPS location access.

1. In Google Cloud, enable **Maps SDK for Android** and billing for your project.
2. Create an API key restricted to Maps SDK for Android and your Android app's
   package name and signing certificate SHA-1. Configure both
   `com.ambulanceservice.mobile.local` (local flavor) and
   `com.ambulanceservice.mobile` (render flavor), with the appropriate debug or
   release certificate.
3. Set `GOOGLE_MAPS_API_KEY` in your user-level
   `%USERPROFILE%\.gradle\gradle.properties` (not the tracked project file), or
   supply it as an environment variable. The Gradle property takes precedence.
4. Rebuild and install the app on a device/emulator with Google Play services.

```powershell
$env:GOOGLE_MAPS_API_KEY = "your-restricted-android-maps-key"
.\gradlew.bat :app:assembleLocalDebug
```

The key is packaged into the app's manifest, so Android application/API
restrictions are essential. Do not commit keys. Without a configured key,
the app shows a configuration message and keeps manual address entry available.
If the map stays blank, check connectivity, billing, API restrictions, and the
installed app's package/SHA-1 restrictions in Google Cloud.

## Local backend notes

### Physical Android phone over Wi-Fi

The local flavor targets at `http://192.168.0.18:8080/`.
Connect the phone and laptop to the same Wi-Fi and configure the backend with:

```properties
server.port=8080
server.address=0.0.0.0
```

Allow incoming backend connections in the laptop firewall. If using Docker, publish
port 8080. Confirm the phone can open `http://192.168.0.18:8080/` in its browser.
Guest Wi-Fi/client isolation or VPN routing may prevent LAN access.

Build the local debug variant with the command below, then transfer
`app\build\outputs\apk\local\debug\app-local-debug.apk` to the phone and open it
to install. Allow installation from that trusted file source if Android prompts.
No USB debugging or special phone network settings are required.

The local-flavor resource `app\src\local\res\xml\network_security_config.xml`
allows HTTP to this laptop IP, localhost, and the emulator host. The Render
flavor's network policy is unchanged. If the laptop's IP changes, update both
`API_BASE_URL_LOCAL` and the local HTTP allowlist, then rebuild and reinstall.
Keep the trailing slash on the base URL. Use HTTP only on a trusted development
network; do not expose this backend publicly.

### Android emulator

To reach a backend on the emulator's host machine, override the local base URL:

```powershell
.\gradlew.bat :app:assembleLocalDebug "-PAPI_BASE_URL_LOCAL=http://10.0.2.2:8080/"
```

## Run locally

Build the local debug variant:

```powershell
Set-Location "D:\test\ambulance-platform\mobile-android"
.\gradlew.bat :app:assembleLocalDebug
```

Run unit tests:

```powershell
Set-Location "D:\test\ambulance-platform\mobile-android"
.\gradlew.bat :app:testLocalDebugUnitTest
```

## Next suggested steps

- GPS pickup assistance
- booking status / tracking by ID
- user registration and login
- driver and dispatcher modules
