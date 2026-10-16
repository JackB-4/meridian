# Meridian 1.2 — verification and delivery

Package `app.meridian`, versionCode **3**, versionName **1.2**, minSdk **28**, target/compile SDK **36**. Native Java/Android views; no third-party runtime dependencies. Final signed APK: **46,093 bytes**.

SHA-256: `104dd34290a0994769afa27c5d390e865fe7810e44fc63069b581f8ac1c68e85`.

APK Signature Scheme v3 verified by Android apksigner. Same RSA-3072 signing certificate as 1.0 and 1.1: `a2ccb2592a9ae22d84811bc0d10eaa83fbb5c22c1e82ae59f35824812f31cd34`. Only coarse/fine foreground location permissions. No INTERNET, VIBRATE, background location, storage or notification permission.

## Changes

- Adaptive regular/round icon with separate foreground/background and Android 13+ monochrome layer. Artwork stays within the adaptive safe zone.
- Equal-width data grid with one to three responsive columns, expandable row heights and page scrolling. Long-press drag and tap/accessibility Move earlier/later actions. Persistent ordering, including hidden tiles.
- One initial location-country unit resolution, locale fallback, UK mixed units, manual sticky override and legacy preference retention. A one-time system Geocoder lookup may use its provider's network backend; no app analytics or location history.
- One automatic foreground location prompt on fresh first launch. No relaunch nag after denial, no automatic upgrade nag, explicit path to App permissions for permanent denial.
- Dedicated haptic settings page with Light/Standard platform feedback, dial interval, five independent triggers, throttling and cardinal/target hysteresis. No system-preference overrides.
- Single launcher entry named Meridian Compass & Elevation, plus Compass and Elevation static shortcuts. Search indexing remains launcher-dependent.
- Settings choices stack at large text sizes or narrow widths to avoid clipped labels. Original restrained grayscale UI retained.

## Executed checks

On a Windows host, Android 16/API 36 x86_64 emulator, serial explicitly restricted to `emulator-5554`:

- **776 host assertions**: angle wrap/smoothing, coordinate formatting, four rotation-axis mappings, distance/speed conversion, freshness, preference defaults, grid columns/order normalization, regional defaults and haptic interval bounds.
- **25 basic Android regressions**: no-permission/missing-data display, preference readback, sensor rotation remapping, true-north pending, readout toggles, light/dark application and stale relative pressure.
- **42 v1.2 assertions**: reorder/hidden-tile retention, granular haptic settings, platform feedback selection/throttle/master/per-trigger controls, cardinal wrap/hysteresis and target cue, sticky units/locale/upgrade migration, shortcuts, adaptive and monochrome layers, launcher label, tile bounds and scroll reachability.
- **2 actual touch-drag assertions**: injected touchscreen down/long-press/move/up changed persisted order and rebuilt the grid.
- **20 settings-flow assertions**, repeated at **1.5 font scale**: direct selections, toggles, scroll access, Back/reopen and persistence.
- **13 grid-layout assertions each** at 320 dp narrow width, 1.5 font scale and landscape; all tiles fit horizontal bounds and the final tile is reachable by scrolling. Native tall-phone layout covered by the 42-check run.
- **11 location assertions**: ellipsoid/MSL selection and labels, vertical uncertainty, stale fix handling, true-north availability and travel course. **3 coarse-only permission assertions**: approximate label and no invented altitude.
- Saved nondefault theme/units/readout settings in installed 1.1, installed 1.2 with `adb install -r`, and passed both persistence/UI restoration assertions without clearing app data.
- Fresh uninstall/install produced the actual Android fine/coarse permission dialog on launch. Tapped Don't allow, force-stopped/relaunched: no permission dialog. Set emulator user-fixed denial flags and relaunched: no dialog. Both UI hierarchies contain zero permission-controller nodes and show the compass/Enable location state.
- APK metadata/signature checked after final build. Final size/hash above. Signing key and DPAPI-protected password excluded from deliverables.
- Actual AOSP emulator launcher queries **compass** and **elevation** each returned exactly one Meridian result. The adaptive icon was visually reviewed in its app drawer. This does not establish Pixel Launcher behavior.

Tests that needed synthetic locations live only in the separate instrumentation APK, never in the release app. The grid screenshot uses the emulator GPS fixture (-77, 38.9, ellipsoid altitude 100 m); its MSL conversion and uncertainty are emulator outputs, not real field measurements.

## Visual review and limits

Reviewed actual captures of light/dark main screens, all-data grid, General, large-font General and Haptics. Text and bounds are readable; long data grids scroll. Emulator rendering uses its OpenGL HWUI backend to avoid its earlier software redraw issue; no app or phone setting workaround is required.

No physical Pixel 10 Pro was accessed, installed on, or altered. Real heading/elevation accuracy, hardware haptic feel, live country geocoding and Pixel Launcher keyword indexing are not field-tested. Minimum-API devices are not exercised. Missing-hardware handling belongs to unchanged sensor logic previously verified on a sensorless emulator in 1.0; this run directly rechecks missing readings/permission/staleness rather than claiming a new sensorless-device run.

A timing-only test initially checked scroll visibility before the smooth scroll completed. The instrumentation now scrolls synchronously for that assertion; final runs pass. No production scrolling workaround was needed.

Emulator dimensions, font scale and rotation control were restored, then the emulator was stopped. No physical device was involved.

