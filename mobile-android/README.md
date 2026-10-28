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

- `local` → default base URL `http://10.0.2.2:8080/`
- `render` → placeholder Render URL for future deployment

You can override these URLs with Gradle properties:

- `API_BASE_URL_LOCAL`
- `API_BASE_URL_RENDER`

Example `gradle.properties` entries:

```properties
API_BASE_URL_LOCAL=http://10.0.2.2:8080/
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

For the Android emulator, use `10.0.2.2` to reach your host machine.

If your backend is running on:

- `server.port=8080`
- `server.address=127.0.0.1`

then the emulator can still usually reach it through `http://10.0.2.2:8080/`.

If you later test on a physical device, you will likely need the backend to bind to all interfaces instead of only `127.0.0.1`.

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
