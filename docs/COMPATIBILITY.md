# Compatibility and evidence

| Component | Status |
| --- | --- |
| Fire TV model AFTKRT, build RS8185.3891N | The listed packages were disabled and their states read back during development. |
| Other Fire OS builds / models | Read-only audit permitted. Plan/apply blocked. |
| Android TV / Google TV (Sony, TCL, Hisense, Philips, Xiaomi, NVIDIA and others) | Generic read-only inventory and platform detection implemented. Launcher targets Android 8.0+ with Android TV provider variants. No vendor-specific cleanup profile or hardware validation yet. |
| Roku, Apple TV, Samsung Tizen, LG webOS | Not supported by this ADB toolkit. |

The initial profile comes from one device session. It is **not** broad testing across devices, regions or firmware. Full playback and reboot persistence were not tested after the final logging-component change. Some earlier cleanup checks confirmed app navigation; do not infer full service compatibility from this.

The generic CLI passes 24 synthetic workflow/platform tests. Its live read-only audit could not complete because the reference device disconnected; no generic CLI mutations have been run on a real TV. The public launcher is compile/signature checked; it uses a distinct package from the development launcher and needs its own on-device validation before a stable release. No stable binary release is provided yet.

## Adding support

Submit a proposed profile change with a sanitized model/build, package purpose, original state, UID/shared-UID analysis, expected feature loss and test results. Include streaming playback, remote navigation, Settings and reboot behavior. Use synthetic dumps in tests; never attach a complete real-device dump.

A successful `pm disable-user` command alone is not sufficient evidence. Packages and responsibilities change between firmware versions. Protected components should remain protected; do not propose bypasses as routine cleanup.

## Platform support and limits

Google TV is a home-screen experience built on Android TV; both use the Android launcher and ADB paths here. Android-based TV boxes without TV features are classified as `unknown-android`; that classification never grants cleanup access. Roku, Tizen and webOS are different operating systems even when the TV brand also sells Android TVs.

The launcher discovers installed launchable apps rather than requiring an Amazon catalogue. YouTube, Prime Video and Apple TV resolve their installed Android TV or Fire TV package variant. Settings uses the system Settings intent. App availability and title deep links depend on the installed provider version and region.

Install with `adb -s TV_SERIAL install launcher/build/CleanTV.apk`. Use the system default-app/home-app selector if offered. If unavailable, open CleanTV from the app list. Firmware may prevent changing Home; no stock launcher is disabled and no accessibility service is automatically enabled. Menu opens CleanTV settings. Validate on each device before describing it as tested.

Contributions should add synthetic audit fixtures and real-device results for each model/build. Broad launcher compatibility does not imply a universal package-removal list. Current audit identity requires a readable hardware serial; devices hiding it fail with an explicit error rather than creating a writable plan.
