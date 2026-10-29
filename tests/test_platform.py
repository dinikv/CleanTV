import unittest
from cleantv.__main__ import audit, detect_platform, make_plan

class InventoryADB:
    def __init__(self): self.calls = []
    def identity(self):
        return {"model":"Example TV", "build":"test", "fingerprint":"synthetic", "device_hash":"synthetic"}
    def shell(self, *args):
        self.calls.append(args)
        if args == ("pm", "list", "features"): return "feature:android.software.leanback"
        if args == ("pm", "list", "packages"): return "package:com.example.player\npackage:com.example.player\nnoise\npackage:bad;command"
        return "Example"
    def package(self, name): raise AssertionError("No Fire package checks on unknown firmware")

class PlatformTests(unittest.TestCase):
    def test_fire(self):
        self.assertEqual(detect_platform({"manufacturer":"Amazon"}, ""), "fire-tv")
    def test_android_tv(self):
        self.assertEqual(detect_platform({"manufacturer":"Example"}, "feature:android.software.leanback"), "android-tv")
    def test_unknown_not_assumed_tv(self):
        self.assertEqual(detect_platform({}, ""), "unknown-android")
    def test_generic_audit_is_read_only_and_no_fire_settings(self):
        adb = InventoryADB()
        result = audit(adb)
        self.assertEqual(result["inventory"], ["com.example.player"])
        self.assertEqual(result["platform"], "android-tv")
        self.assertIsNone(result["profile"])
        self.assertFalse(result["cleanup_supported"])
        self.assertEqual(result["privacy"], {})
        self.assertTrue(all(c[0] in ("getprop", "pm") for c in adb.calls))
        with self.assertRaises(ValueError): make_plan(result, ["diagnostics"])
