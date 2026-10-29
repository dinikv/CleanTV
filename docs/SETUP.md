# Install the tools

Cleanup needs only **Python and ADB** on your computer. Git is useful, but optional. Install missing tools once; reuse working installations.

| Required | Recommended source |
| --- | --- |
| Python 3.9+; choose a currently supported release for a new install | [python.org](https://www.python.org/downloads/) or your trusted OS package manager |
| Android Platform Tools, which include `adb` | [Google’s official downloads](https://developer.android.com/tools/releases/platform-tools) |
| Git, optional | [Git’s installation guide](https://git-scm.com/install/) or your trusted OS package manager |

The CLI has no third-party Python dependencies. **Do not run `pip install cleantv`**—this project runs directly from its repository. No API key, GitHub account, paid subscription or full Android SDK is needed for cleanup.

## Check before installing

macOS / Linux:

```sh
python3 --version
adb version
git --version
command -v python3
command -v adb
command -v git
```

Windows PowerShell:

```powershell
python --version
adb version
git --version
Get-Command python, adb, git -ErrorAction SilentlyContinue
```

If `python` is unavailable on Windows but `py -3 --version` works, use `py -3` in place of `python3` in the CleanTV commands. A missing optional Git installation is fine if you use a ZIP download. A version command failing means that tool needs installation or its folder needs to be added to PATH.

## macOS

**If you already use Homebrew**, run only the line for each missing tool:

```sh
brew install python
brew install --cask android-platform-tools
brew install git
```

The ADB command is documented in the [Homebrew Platform Tools cask](https://formulae.brew.sh/cask/android-platform-tools). Do not run Homebrew with `sudo`.

**Without Homebrew:** install Python from [python.org](https://www.python.org/downloads/macos/), download the Mac Platform Tools ZIP from [Google](https://developer.android.com/tools/releases/platform-tools), and extract it into a folder you control. Use the repository ZIP instead of installing Git if you prefer. You do not need to install a package manager just for CleanTV.

## Windows

1. Install a current supported Python release using the official [Python Windows downloads](https://www.python.org/downloads/windows/). Follow the official installer's PATH/command setup instructions; reopen PowerShell afterward.
2. Download **SDK Platform-Tools for Windows** from [Google](https://developer.android.com/tools/releases/platform-tools). Extract the ZIP into a folder you control, such as `C:\Tools\platform-tools`.
3. Add that extracted folder to your **user PATH** through Windows Environment Variables, or use its full path as shown below.
4. Install [Git for Windows](https://git-scm.com/install/windows) only if you want cloning and updates. Otherwise use **Code → Download ZIP** on the CleanTV GitHub page.

If PowerShell's `python` opens the Microsoft Store instead of your installed interpreter, try `py -3 --version` and use that working command. Do not change PowerShell execution policy or turn off security software for CleanTV.

## Linux

On Ubuntu/Debian, use the distribution repositories for any missing tools:

```sh
sudo apt update
sudo apt install python3 adb git
```

Leave out packages you already have, and omit `git` if using the repository ZIP. `sudo` is for the OS package manager—not for running CleanTV. On other distributions, use the equivalent packages from your distribution's official repositories rather than copying these commands blindly.

Alternatively, Google provides a standalone Linux Platform Tools download. Check its host architecture requirements; use your distribution's compatible ADB package if the vendor binary does not match your computer.

## Prefer not to change PATH?

Pass the full ADB executable path to CleanTV. Quote paths containing spaces.

macOS / Linux example (replace the example path):

```sh
python3 -m cleantv --adb "/path/to/platform-tools/adb" \
  --serial TV_IP:5555 audit --out private/audit.json
```

Windows PowerShell example:

```powershell
& "C:\Tools\platform-tools\adb.exe" connect TV_IP:5555
python -m cleantv --adb "C:\Tools\platform-tools\adb.exe" --serial TV_IP:5555 audit --out private/audit.json
```

`--adb` and `--serial` go before the subcommand (`audit`, `apply` or `restore`). Connect with the same ADB executable first. The plan command does not contact the TV.

## Verify, then connect

Re-run the version and path checks. From the CleanTV folder, verify the local CLI:

```sh
python3 -m cleantv --help
```

Use your verified Python command on Windows. Then follow [Connect in the README](../README.md#1-connect). Enable ADB debugging on your own TV and approve your computer on its screen. Never forward the ADB port through your router.

## Only if you want to build the launcher

The optional launcher additionally needs JDK 17, Android SDK Platform 35, Build Tools 35.0.0 and `zip` in a POSIX shell. Follow [the launcher build guide](LAUNCHER.md). These are **not** prerequisites for TV cleanup, and should not be installed automatically during a cleanup-only task.

## Installation rules for AI assistants

Detect OS and architecture; check first; install only what is missing. Use the official sources above or an existing trusted package manager. Explain the proposed installation and proceed within the user's authorization. Let the user handle administrator credentials and license prompts. Never paste credentials into chat, disable signature/TLS checks, pipe a remote installer into a shell, replace system Python or silently install a new package manager.

These are documented installation routes, not a claim that every command has been executed on every supported host OS.
