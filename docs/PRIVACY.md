# What CleanTV can—and cannot—change

## Three separate layers

1. **The home screen.** The launcher changes what you see. It does not replace Fire OS.
2. **Settings and packages.** Privacy toggles and disabling selected components reduce some collection and features. CleanTV's CLI only changes reviewed package enabled states.
3. **Network traffic.** A router/DNS filter can block identified destinations, but is not included here. A shared endpoint may handle both telemetry and essential playback or login. DNS cannot classify encrypted request contents or stop every route to a service.

## Manual privacy settings

Depending on firmware, look under Settings → Preferences → Privacy Settings. Review Device Usage Data, Collect App Usage Data and Interest-based Ads. The CLI records four relevant setting values when present; `null` means unavailable, not off. It does not write undocumented settings or change cloud-account consent.

The reference configuration returned zero for each of those three settings and `USAGE_METRICS_UPLOAD_ENABLED`. That does not mean every background request is disabled.

## Microphone evidence

A permission declaration is not proof of recording. AppOps can show allowed/ignored modes and some access history, but cannot establish what audio contained or where it went. `dumpsys audio` is only a snapshot. A standard Fire TV voice remote normally uses push-to-talk; other hardware can differ.

The Alexa group disables the observed assistant and its alerts. It does not indiscriminately revoke permissions from the system UID or all microphone-capable apps. Extra AppOps restrictions need device-specific UID and permission inspection and their own rollback record. The CLI does not automate those additional restrictions.

## Data that stays local

The audit queries an allowlisted set of packages, four privacy-setting values and model/build identity. A device serial is read to derive a SHA-256 identity hash; the raw serial is not saved. The hash is for mismatch prevention, not anonymization. Treat the entire audit and journal as private. There is no upload, account login or analytics service in the CLI.

The launcher has no Internet or microphone permission in this public build. It keeps app ordering, wallpaper selection and recommendation feedback in local Android preferences. Its streaming-app links hand control to the selected provider.

After finishing, turn ADB debugging off if you do not need it. Do not expose ADB ports to the Internet.
