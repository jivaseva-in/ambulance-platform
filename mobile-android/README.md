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

