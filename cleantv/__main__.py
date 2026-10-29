"""Standard-library-only ADB workflow. No root, shell scripts or arbitrary plan commands."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parent.parent
PROFILE = json.loads((ROOT / "profiles/fire-tv-tested.json").read_text())
PACKAGES = {p for g in PROFILE["groups"].values() for p in g["packages"]}
PRIVACY_KEYS = ["settingsApp:deviceUsageData:value", "settingsApp:appUsageData:value",
                "advertisingIdApp:interestBasedAds:value", "USAGE_METRICS_UPLOAD_ENABLED"]


def stamp():
    return datetime.now(timezone.utc).isoformat()


def save(path, obj, exclusive=False):
    """Persist a private journal before any device mutation; fsync its data."""
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    if exclusive:
        fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        with os.fdopen(fd, "w") as f:
            json.dump(obj, f, indent=2)
            f.write("\n")
            f.flush()
            os.fsync(f.fileno())
    else:
        temp = path.with_name(path.name + ".tmp")
        fd = os.open(temp, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
        with os.fdopen(fd, "w") as f:
            json.dump(obj, f, indent=2)
            f.write("\n")
            f.flush()
            os.fsync(f.fileno())
        os.replace(temp, path)


class ADB:
    def __init__(self, executable, serial):
        self.executable, self.serial = executable, serial

    def run(self, *args):
        result = subprocess.run([self.executable, "-s", self.serial, *args],
                                capture_output=True, text=True, timeout=30)
        if result.returncode:
            raise RuntimeError("ADB failed: " + result.stderr.strip()[:400])
        if re.search(r"SecurityException|Error:|Exception occurred|Permission Denial", result.stdout):
            raise RuntimeError("Device refused the command: " + result.stdout[:400])
        return result.stdout.strip()

    def shell(self, *args):
        return self.run("shell", *args)

    def identity(self):
        if self.run("get-state") != "device":
            raise RuntimeError("Device is not ready. Approve debugging on your TV.")
        get = lambda key: self.shell("getprop", key)
        serial = get("ro.serialno") or get("ro.boot.serialno")
        if not serial:
            raise RuntimeError("Cannot obtain a stable device identity; refusing to create a writable plan.")
        return {"model": get("ro.product.model"), "build": get("ro.build.display.id"),
                "fingerprint": get("ro.build.fingerprint"),
                "device_hash": hashlib.sha256(serial.encode()).hexdigest()}

    def package(self, name):
        if name not in PACKAGES:
            raise ValueError("Package is outside the reviewed allowlist")
        return parse_package(self.shell("dumpsys", "package", name), name)


def parse_package(text, name):
    if not re.search(r"Package \[" + re.escape(name) + r"\]", text):
        return {"present": False}
    # Only the target package block, not shared-user/package-query sections.
    block = text.split("Package [" + name + "]", 1)[1].split("\nQueries:", 1)[0]
    uid = re.search(r"\buserId=(\d+)", block)
    state = re.search(r"User 0:.*?\benabled=(\d+)", block)
    installed = re.search(r"User 0:.*?\binstalled=(true|false)", block)
    version = re.search(r"\bversionCode=(\d+)", block)
    return {"present": True, "installed": bool(installed and installed[1] == "true"),
            "uid": int(uid[1]) if uid else None,
            "shared_uid": bool(re.search(r"\bsharedUser=", block)),
            "enabled": int(state[1]) if state else None,
            "version": version[1] if version else None}


def safe_state(state):
    return (state.get("present") and state.get("installed") and
            state.get("uid") is not None and state["uid"] >= 10000 and
            not state.get("shared_uid") and state.get("version") is not None and
            state.get("enabled") in (0, 1, 2, 3, 4))


def detect_platform(properties, features):
    """Classify TV families without using the result as cleanup authorization."""
    manufacturer = properties.get("manufacturer", "").lower()
    model = properties.get("model", "").upper()
    if manufacturer == "amazon" or model.startswith("AFT"):
        return "fire-tv"
    if "android.software.leanback" in features or "android.hardware.type.television" in features:
        return "android-tv"
    return "unknown-android"


def audit(adb):
    identity = adb.identity()
    properties = {name: adb.shell("getprop", prop) for name, prop in {
        "manufacturer": "ro.product.manufacturer", "brand": "ro.product.brand",
        "model": "ro.product.model", "android_version": "ro.build.version.release",
        "sdk": "ro.build.version.sdk"}.items()}
    features = adb.shell("pm", "list", "features")
    platform = detect_platform(properties, features)
    # A generic inventory supports review on every vendor; never becomes an allowlist.
    inventory = sorted(set(re.findall(r"^package:([A-Za-z0-9_.]+)$",
                                     adb.shell("pm", "list", "packages"), re.M)))
    supported = any(all(identity.get(k) == v for k, v in model.items())
                    for model in PROFILE["tested"])
    packages = {name: adb.package(name) for name in sorted(PACKAGES)} if supported else {}
    privacy = ({key: adb.shell("settings", "get", "secure", key) for key in PRIVACY_KEYS}
               if platform == "fire-tv" else {})
    return {"schema": 1, "kind": "audit", "created": stamp(), "device": identity,
            "platform": platform, "properties": properties, "inventory": inventory,
            "profile": PROFILE["id"] if supported else None,
            "cleanup_supported": supported, "packages": packages, "privacy": privacy,
            "note": "Local snapshot. Inventory is not a debloat list. No claim of zero telemetry."}


def make_plan(report, groups):
    if report.get("kind") != "audit" or report.get("schema") != 1:
        raise ValueError("Expected a CleanTV schema-1 audit")
    identity = report["device"]
    if not any(all(identity.get(k) == v for k, v in model.items()) for model in PROFILE["tested"]):
        raise ValueError("Untested model/build: audit only. See docs/COMPATIBILITY.md before contributing a profile.")
    actions, skipped = [], []
    for group in groups:
        if group not in PROFILE["groups"]:
            raise ValueError("Unknown group: " + group)
        for name in PROFILE["groups"][group]["packages"]:
            state = report["packages"].get(name, {})
            if not safe_state(state) or state["enabled"] not in (0, 1):
                skipped.append({"package": name, "reason": "Absent, already disabled, shared UID, or incomplete evidence"})
                continue
            actions.append({"package": name, "before": state, "after": 3,
                            "group": group, "impact": PROFILE["groups"][group]["impact"]})
    return {"schema": 1, "kind": "plan", "created": stamp(), "device": identity,
            "profile": PROFILE["id"], "actions": actions, "skipped": skipped}


def validate(document, kind):
    if document.get("schema") != 1 or document.get("kind") != kind:
        raise ValueError("Unexpected file format")
    identity = document.get("device", {})
    if not all(identity.get(k) for k in ("model", "build", "fingerprint", "device_hash")):
        raise ValueError("Missing device identity")
    if not any(all(identity.get(k) == v for k, v in model.items()) for model in PROFILE["tested"]):
        raise ValueError("Unsupported device/build in file")
    seen = set()
    for action in document["actions"]:
        name = action["package"]
        group = action.get("group")
        if (name not in PACKAGES or name in seen or group not in PROFILE["groups"] or
                name not in PROFILE["groups"][group]["packages"] or action.get("after") != 3 or
                not safe_state(action["before"]) or action["before"]["enabled"] not in (0, 1)):
            raise ValueError("Unsafe or unexpected action in file")
        seen.add(name)


def verify_device(adb, document):
    if adb.identity() != document["device"]:
        raise ValueError("Device or firmware differs from the saved audit. Re-audit before proceeding.")


def apply(adb, plan, journal_path):
    validate(plan, "plan")
    verify_device(adb, plan)
    # Preflight every action before making the first change.
    for action in plan["actions"]:
        if adb.package(action["package"]) != action["before"]:
            raise ValueError("Package changed since audit: " + action["package"])
    journal = dict(plan, kind="journal", started=stamp())
    journal["actions"] = [dict(a, status="pending") for a in plan["actions"]]
    save(journal_path, journal, exclusive=True)
    for action in journal["actions"]:
        action["status"] = "attempting"
        save(journal_path, journal)
        try:
            adb.shell("pm", "disable-user", "--user", "0", action["package"])
            if adb.package(action["package"])["enabled"] != 3:
                raise RuntimeError("Disable did not persist")
            action["status"] = "verified"
            print("Disabled:", action["package"])
        except Exception as error:
            action["status"] = "uncertain"
            action["error"] = str(error)
            save(journal_path, journal)
            raise RuntimeError("Stopped. Inspect the journal and use restore for attempted changes.") from error
        save(journal_path, journal)
    journal["finished"] = stamp()
    save(journal_path, journal)


def restore(adb, journal, journal_path):
    validate(journal, "journal")
    verify_device(adb, journal)
    for action in reversed(journal["actions"]):
        if action.get("status") not in ("attempting", "uncertain", "verified", "restoring"):
            continue
        current = adb.package(action["package"])
        before = action["before"]
        if current == before:
            action["status"] = "restored"
            save(journal_path, journal)
            continue
        expected = dict(before, enabled=3)
        if current != expected:
            raise ValueError("Package changed outside CleanTV; restore stopped: " + action["package"])
        action["status"] = "restoring"
        save(journal_path, journal)
        command = "default-state" if before["enabled"] == 0 else "enable"
        adb.shell("pm", command, "--user", "0", action["package"])
        if adb.package(action["package"]) != before:
            raise RuntimeError("Restore verification failed; journal retained")
        action["status"] = "restored"
        save(journal_path, journal)
        print("Restored:", action["package"])


def main():
    parser = argparse.ArgumentParser(description="CleanTV: read-only audit, reviewed plan, reversible package cleanup")
    parser.add_argument("--adb", default=os.environ.get("ADB", "adb"))
    parser.add_argument("--serial", help="Exact authorized ADB device/IP:port. Never auto-selects a TV.")
    sub = parser.add_subparsers(dest="command", required=True)
    a = sub.add_parser("audit"); a.add_argument("--out", required=True)
    p = sub.add_parser("plan"); p.add_argument("--audit", required=True); p.add_argument("--groups", required=True); p.add_argument("--out", required=True)
    a = sub.add_parser("apply"); a.add_argument("--plan", required=True); a.add_argument("--journal", required=True); a.add_argument("--yes", action="store_true")
    a = sub.add_parser("restore"); a.add_argument("--journal", required=True); a.add_argument("--yes", action="store_true")
    args = parser.parse_args()
    if args.command != "plan" and not args.serial:
        parser.error("--serial is required for device operations")
    adb = ADB(args.adb, args.serial)
    if args.command == "audit":
        save(args.out, audit(adb), exclusive=True)
        print("Read-only audit saved:", args.out)
    elif args.command == "plan":
        plan = make_plan(json.loads(Path(args.audit).read_text()), args.groups.split(","))
        validate(plan, "plan")
        save(args.out, plan, exclusive=True)
        for a in plan["actions"]:
            print(a["package"], "—", a["impact"])
        print(len(plan["actions"]), "proposed changes. Nothing applied.")
    else:
        if not args.yes:
            parser.error("Review the saved plan/journal first; --yes acknowledges those changes")
        if args.command == "apply":
            apply(adb, json.loads(Path(args.plan).read_text()), args.journal)
        else:
            restore(adb, json.loads(Path(args.journal).read_text()), args.journal)


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, RuntimeError, KeyError, TypeError, subprocess.TimeoutExpired) as error:
        print("CleanTV:", error, file=sys.stderr)
        sys.exit(1)
