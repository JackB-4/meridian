<h1><img src="docs/meridian-logo-round.svg" alt="Meridian compass logo" width="48" height="48" align="absmiddle"> Meridian</h1>

A minimal, configurable native Android compass with elevation and practical navigation readouts. Designed with Pixel 10 Pro sensors in mind; supports Android 9 and later.

## Install

Download **Meridian-1.2.apk** from the [latest release](https://github.com/JackB-4/meridian/releases/latest), open it on Android, and allow installation from that file source if asked. Install over an existing Meridian copy to retain settings. The release uses the original signing identity and package `app.meridian`.

## Obtainium

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium%3A%2F%2Fapp%2F%257B%2522id%2522%253A%2522app.meridian%2522%252C%2522url%2522%253A%2522https%253A%252F%252Fgithub.com%252FJackB-4%252Fmeridian%2522%252C%2522author%2522%253A%2522JackB-4%2522%252C%2522name%2522%253A%2522Meridian%2522%257D"><img src="docs/add-to-obtainium.svg" alt="Add Meridian to Obtainium" width="176" height="40"></a>

In [Obtainium](https://github.com/ImranR98/Obtainium), choose **Add App** and enter:

```
https://github.com/JackB-4/meridian
```

Use the GitHub source with normal stable-release defaults. Each release contains one universal APK; no architecture filter, custom feed, or GitHub token is needed for this public repository. Let Obtainium install it or track the existing installation. Android may require installation permission for Obtainium. Future updates require a new published release; adding the repository does not create an automatic release schedule.

Release tags use `v1.2`-style versions. Version 1.2 has Android versionCode **3**. Future releases must increase versionCode and retain the package/signing identity. Do not uninstall an existing copy to update. Source ZIP files are not installable APKs.

See the [official Obtainium documentation](https://wiki.obtainium.imranr.dev/) for source and tracking behavior. Release discovery and APK downloads are verified; installation through Obtainium on a physical phone has not been tested.

## Features

- Smooth, wrap-safe compass heading; magnetic or location-corrected true north; heading hold and target bearing.
- Elevation with datum and uncertainty, position, horizontal accuracy, speed, travel course, pressure, relative elevation, pitch/roll, magnetic field and declination.
- Responsive one-to-three-column readout grid. Long-press drag to reorder, or tap for accessible Move earlier/later actions. Every readout can be toggled.
- Light/dark/system themes; metric, imperial and UK mixed units; decimal, DMS and DDM coordinates. Settings persist.
- Optional platform haptics with Light/Standard feedback, dial interval, and separate cardinal/target/drag/control triggers. Android system settings remain respected.
- Adaptive launcher icon with monochrome themed layer; Compass and Elevation shortcuts.

## Privacy and accuracy

No ads, analytics, account, app network permission, background service or background location. Location is requested once on first fresh launch; denial leaves the magnetic compass usable. Precise foreground location gives the best available GNSS readings. No coordinate history is saved.

Rotation-vector fusion is preferred, with geomagnetic-vector and accelerometer/magnetometer fallbacks. Display rotation is remapped correctly. Keep the phone fairly flat and away from magnets and metal. Sensor confidence is qualitative, not a guaranteed degree error. This app cannot promise survey-grade accuracy.

True north uses Android's magnetic model with the current date and a fresh location with altitude. Elevation prefers API 34+ mean-sea-level altitude, attempting Android's offline conversion when needed. Otherwise it is explicitly labeled WGS84 ellipsoid. Missing accuracy is labeled unavailable. Pressure-derived elevation is relative to a session zero and drifts with weather. Tap the relative-elevation tile to set zero. Travel course is distinct from compass heading and is withheld while stationary.

Location values expire after 30 seconds; heading after 2 seconds; pressure/field after 30 seconds. Permission, unavailable-data and calibration states are explicit. Sensors and location updates stop while the activity is paused.

Initial units use locale country provisionally, then one system Geocoder country lookup from a permitted fresh location. Its provider may use a network backend. If unavailable, mobile-network country is tried, then locale remains. US defaults to feet/mph, UK to metres/mph, others to metres/km/h. There is no travel monitor. Manual choices and existing saved units remain sticky.

## Build

Windows PowerShell, JDK 21, Android SDK 36. No Gradle or third-party runtime dependencies.

```powershell
./bootstrap.ps1
./build.ps1
```

Bootstrap downloads checksum-pinned official Microsoft/Google tools into `.tools`. The build compiles resources and Java, runs **776 host assertions**, produces DEX, and aligns `dist/Meridian-1.2-unsigned.apk`. An existing compatible tool directory can be supplied with `-ToolRoot`. The unsigned result cannot be installed until separately signed.

Release signing is intentionally outside the public build. No private key or password is included or generated by this repository. Independently signed builds cannot update an installation signed by the original release key. The published APK is the existing verified release binary, not a newly signed build.

`tests/SmokeInstrumentation.java` and its manifest contain the device test harness. It is a separate APK and never part of the release. Instrumentation must be signed with the same test identity as the app under test; use a separate emulator/test installation for independently signed builds. See [TESTING.md](TESTING.md) for previously executed checks and limitations. No physical Pixel accuracy or tactile-feel testing is claimed.

## Verify version 1.2

- Package: `app.meridian`; versionName `1.2`; versionCode `3`; minSdk `28`; targetSdk `36`.
- APK SHA-256: `104dd34290a0994769afa27c5d390e865fe7810e44fc63069b581f8ac1c68e85`.
- Signing certificate SHA-256: `a2ccb2592a9ae22d84811bc0d10eaa83fbb5c22c1e82ae59f35824812f31cd34`.

The release includes `SHA256SUMS.txt`. APK Signature Scheme v3 is verified; the same certificate is used by previous Meridian releases. SHA-256 verifies file integrity, not real-world sensor accuracy.
