import copy
import json
from pathlib import Path
import tempfile
import unittest
from cleantv.__main__ import (parse_package, safe_state, make_plan, apply, restore,
                             validate, PROFILE, PACKAGES)

IDENTITY = {"model": "AFTKRT", "build": "RS8185.3891N", "fingerprint": "synthetic/test-build", "device_hash": "synthetic-device-hash"}
NAME = "com.amazon.perfcollection"
STATE = {"present": True, "installed": True, "uid": 12000, "shared_uid": False, "enabled": 0, "version": "1"}


def report():
    return {"schema": 1, "kind": "audit", "device": copy.deepcopy(IDENTITY),
            "packages": {NAME: copy.deepcopy(STATE)}}


class FakeADB:
    def __init__(self, fail=False):
        self.state = copy.deepcopy(STATE)
        self.ident = copy.deepcopy(IDENTITY)
        self.calls = []
        self.fail = fail
        self.journal = None

    def identity(self):
        return self.ident

    def package(self, name):
        return copy.deepcopy(self.state)

    def shell(self, *args):
        if self.journal:
            assert Path(self.journal).exists(), 'Journal must precede mutation'
            saved = json.loads(Path(self.journal).read_text())
            assert saved['actions'][0]['status'] in ('attempting', 'restoring')
        self.calls.append(args)
        self.state['enabled'] = {'disable-user': 3, 'enable': 1, 'default-state': 0}[args[1]]
        if self.fail:
            self.fail = False
            raise RuntimeError('Simulated lost connection after device mutation')
        return 'ok'


class WorkflowTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.path = str(Path(self.temp.name) / 'journal.json')
        self.adb = FakeADB()
        self.adb.journal = self.path
        self.plan = make_plan(report(), ['diagnostics'])

    def tearDown(self):
        self.temp.cleanup()

    def test_parse_target_user(self):
        text = 'Package [com.amazon.perfcollection] (x):\n userId=12000\n versionCode=1\n User 0: installed=true enabled=0\nQueries:\n userId=1000'
        self.assertEqual(parse_package(text, NAME), STATE)

    def test_missing_package(self):
        self.assertEqual(parse_package('Unable to find package', NAME), {'present': False})

    def test_shared_uid_excluded(self):
        s = dict(STATE, shared_uid=True)
        self.assertFalse(safe_state(s))
        r = report(); r['packages'][NAME] = s
        self.assertEqual(make_plan(r, ['diagnostics'])['actions'], [])

    def test_system_uid_excluded(self):
        self.assertFalse(safe_state(dict(STATE, uid=1000)))

    def test_unknown_state_excluded(self):
        self.assertFalse(safe_state(dict(STATE, enabled=None)))

    def test_already_disabled_skipped(self):
        r = report(); r['packages'][NAME]['enabled'] = 3
        self.assertEqual(make_plan(r, ['diagnostics'])['actions'], [])

    def test_unknown_firmware_refused(self):
        r = report(); r['device']['build'] = 'different'
        with self.assertRaises(ValueError): make_plan(r, ['diagnostics'])

    def test_unknown_group_refused(self):
        with self.assertRaises(ValueError): make_plan(report(), ['everything'])

    def test_plan_does_not_mutate(self):
        self.assertEqual(len(self.plan['actions']), 1)
        self.assertEqual(self.adb.calls, [])

    def test_package_injection_rejected(self):
        self.plan['actions'][0]['package'] = 'android; reboot'
        with self.assertRaises(ValueError): apply(self.adb, self.plan, self.path)
        self.assertEqual(self.adb.calls, [])

    def test_changed_device_refused(self):
        self.adb.ident['device_hash'] = 'other-synthetic-device'
        with self.assertRaises(ValueError): apply(self.adb, self.plan, self.path)
        self.assertEqual(self.adb.calls, [])

    def test_changed_package_refused(self):
        self.adb.state['version'] = '2'
        with self.assertRaises(ValueError): apply(self.adb, self.plan, self.path)
        self.assertEqual(self.adb.calls, [])

    def test_journal_exists_before_apply_and_exact_restore(self):
        apply(self.adb, self.plan, self.path)
        self.assertEqual(self.adb.state['enabled'], 3)
        journal = json.loads(Path(self.path).read_text())
        self.assertEqual(journal['actions'][0]['status'], 'verified')
        restore(self.adb, journal, self.path)
        self.assertEqual(self.adb.state, STATE)
        self.assertEqual(self.adb.calls[-1][1], 'default-state')

    def test_explicit_enabled_restore(self):
        self.adb.state['enabled'] = 1
        self.plan['actions'][0]['before']['enabled'] = 1
        apply(self.adb, self.plan, self.path)
        restore(self.adb, json.loads(Path(self.path).read_text()), self.path)
        self.assertEqual(self.adb.state['enabled'], 1)
        self.assertEqual(self.adb.calls[-1][1], 'enable')

    def test_existing_journal_not_overwritten(self):
        Path(self.path).write_text('existing')
        with self.assertRaises(FileExistsError): apply(self.adb, self.plan, self.path)
        self.assertEqual(self.adb.calls, [])

    def test_failure_after_mutation_is_recoverable(self):
        self.adb.fail = True
        with self.assertRaises(RuntimeError): apply(self.adb, self.plan, self.path)
        journal = json.loads(Path(self.path).read_text())
        self.assertEqual(journal['actions'][0]['status'], 'uncertain')
        restore(self.adb, journal, self.path)
        self.assertEqual(self.adb.state, STATE)

    def test_restore_does_not_override_external_change(self):
        apply(self.adb, self.plan, self.path)
        self.adb.state['enabled'] = 2
        with self.assertRaises(ValueError): restore(self.adb, json.loads(Path(self.path).read_text()), self.path)
        self.assertEqual(self.adb.state['enabled'], 2)

    def test_restore_is_idempotent(self):
        apply(self.adb, self.plan, self.path)
        restore(self.adb, json.loads(Path(self.path).read_text()), self.path)
        count = len(self.adb.calls)
        restore(self.adb, json.loads(Path(self.path).read_text()), self.path)
        self.assertEqual(len(self.adb.calls), count)

    def test_duplicate_actions_rejected(self):
        self.plan['actions'].append(copy.deepcopy(self.plan['actions'][0]))
        with self.assertRaises(ValueError): validate(self.plan, 'plan')

    def test_profile_has_no_core_components(self):
        self.assertEqual(len(PACKAGES), 18)
        for name in ('android', 'com.amazon.tv.launcher', 'com.amazon.tv.ime', 'com.netflix.ninja', 'com.amazon.firebat', 'com.amazon.firetv.youtube'):
            self.assertNotIn(name, PACKAGES)


if __name__ == '__main__':
    unittest.main()
