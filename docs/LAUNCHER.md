# CleanTV launcher

An optional native Android launcher, separate from the cleanup CLI. Package: `org.cleantv.launcher`. Display name: **CleanTV**. This is an early source release.

## Build

Requires a JDK supporting `javac --release 8` (JDK 17 recommended), Android SDK Platform 35, Build Tools 35.0.0, and `zip`. The script works in a POSIX shell on macOS/Linux; Windows users can use WSL with those dependencies.

```sh
export ANDROID_HOME=/path/to/your/android-sdk
sh launcher/build.sh
```

Output: `launcher/build/CleanTV.apk`. The build generates an ignored **local development key**, with a public development password. Keep it for your own in-place updates; never publish the private key or use it as a production-release key. Future builds signed with another key cannot replace your installed app directly.

The four wallpaper JPEGs are copied without re-encoding and stored uncompressed in the APK. Their original dimensions and JPEG image data are preserved; metadata in the public copies has been sanitized.

## Install and open

```sh
adb -s TV_IP:5555 install -r launcher/build/CleanTV.apk
adb -s TV_IP:5555 shell am start -n org.cleantv.launcher/.HomeActivity
```

This does not change the stock Home-button behavior. Fire OS may ignore third-party default launchers. No accessibility service is included or enabled, and no stock launcher is disabled.

## Remote controls

| Control | Action |
| --- | --- |
| Left / Right | Move through apps. |
| Down | Open the app grid. |
| Up from the top row | Open Pick for tonight. |
| Select | Open an app. |
| Hold Select | Arrange apps. |
| Menu | Settings, wallpaper, screensaver and information. |
| Back | Return through the grid; from the top row, enter the screensaver. |

The wallpaper rotates every 60 seconds while the launcher is foreground. Idle screensaver animation starts after approximately 60 seconds. Wake with a navigation key before using another shortcut.

## Pick for tonight

A local 12-title editorial collection for Netflix, Prime Video and YouTube with mood, approximate runtime, original-language and format filters. English, Tamil and French rank ahead of other languages by default; preferences and feedback are adjustable and local.

Canadian listings were reviewed on 28 October 2026. They do not refresh automatically. Title availability, runtime, audio and subtitle options can change. Movie posters are not bundled in this public version; metadata source URLs remain in `res/raw/picks.json`.

Netflix and YouTube use title/video links. Netflix accepted a tested link during development, but its destination could not be visually verified. Prime links did not reliably reach a title, so the UI explicitly opens Prime and tells you the title to search. Playback is controlled by the provider. This is not cross-service autoplay or access to your viewing history.

The public build intentionally omits network weather and any fixed personal location. The upper-left label identifies the offline build. It uses the device timezone for its date and clock.
