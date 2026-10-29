# Android TV, Google TV and Fire TV launcher

Targets Android 8.0+ with a remote and landscape display. The same APK resolves installed provider variants across TV platforms. Home replacement depends on the firmware’s default-home controls; installing the APK alone does not guarantee Home-button takeover. See [compatibility](COMPATIBILITY.md) for validation status.


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

A local 12-title editorial collection for Netflix, Prime Video and YouTube with mood, approximate runtime, original-language and format filters. Only enabled services with installed apps are eligible. English, Tamil and French receive initial priority; preferred languages are adjustable.

The local ranking engine learns story themes from explicit feedback, lowers recently shown titles, varies the order by day and mixes language/mood between alternatives when possible. It does not contact an AI service or read your streaming history.

- **Balanced:** your preferences, recent suggestions and some daily variety.
- **Easy evening:** excludes titles tagged intense and favors lighter, shorter options. This is not a parental-rating filter.
- **Discovery:** boosts languages outside your preferred list while preserving explicit filters.
- **French practice:** French content, with a beginner-focused lesson prioritized; other French titles may have advanced dialogue.
- **Saved picks:** only titles you saved for another evening.

Feedback distinguishes watched, loved, not-my-taste, more-like-this, save, and a 12-hour skip. A skip does not train a dislike. Undo reverses the last feedback in the current picker session. Reset taste feedback and clear suggestion history are separate controls. An empty result never silently relaxes filters; Browse all picks explicitly clears filters/mode while keeping your services and feedback.

The ranking is transparent heuristics over editorial tags, not a model trained on your accounts. The 12-item catalogue remains the main limit on variety.

Canadian listings were reviewed on 28 October 2026. They do not refresh automatically. Title availability, runtime, audio and subtitle options can change. Movie posters are not bundled in this public version; metadata source URLs remain in `res/raw/picks.json`.

Netflix and YouTube use title/video links. Netflix accepted a tested link during development, but its destination could not be visually verified. Prime links did not reliably reach a title, so the UI explicitly opens Prime and tells you the title to search. Playback is controlled by the provider. This is not cross-service autoplay or access to your viewing history.

The public build intentionally omits network weather and any fixed personal location. The upper-left label identifies the offline build. It uses the device timezone for its date and clock.
