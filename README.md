# PUBG Ping Master 2.0

Kotlin Android starter project for KlencodIDE, configured for Java 17, Kotlin JVM 17, Android SDK 34 and Build Tools 34.0.0.

## Version 2.0 changes
- Measures Cloudflare and Google endpoints separately.
- Keeps independent latency, average, jitter estimate and failed-attempt percentage for each endpoint.
- Jitter is calculated only from consecutive successful samples to the same endpoint.
- Includes a lightweight live latency trend chart (Cloudflare samples).
- Does not select the lowest result across endpoints or mix endpoint measurements.

## Critical limitation
Measurements use TCP connection time to public endpoints, not ICMP echo and not PUBG Mobile game-server ping. PUBG does not expose a public API for third-party apps to read its live in-game latency. The actual PUBG ping remains the in-game indicator. This app is a network diagnostic tool, not a ping reducer or VPN.

## Build
1. Extract the ZIP.
2. Open the project folder in KlencodIDE.
3. Sync Gradle.
4. Build `:app:assembleDebug`.
5. Install the generated debug APK.

The source package was generated, but has not yet been compiled in this environment. Verify with a fresh KlencodIDE build and share the complete error log if it fails.
