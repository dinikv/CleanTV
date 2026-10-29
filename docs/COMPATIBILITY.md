# Compatibility and evidence

| Component | Status |
| --- | --- |
| Fire TV model AFTKRT, build RS8185.3891N | The listed packages were disabled and their states read back during development. |
| Other Fire OS builds / models | Read-only audit permitted. Plan/apply blocked. |
| Other Android TVs | Audit may work where ADB and package information are available. No supported mutation profile. |
| Roku, Apple TV, Samsung Tizen, LG webOS | Not supported by this ADB toolkit. |

The initial profile comes from one device session. It is **not** broad testing across devices, regions or firmware. Full playback and reboot persistence were not tested after the final logging-component change. Some earlier cleanup checks confirmed app navigation; do not infer full service compatibility from this.

The generic CLI passes 20 synthetic transport tests. Its live read-only audit could not complete because the reference device disconnected; no generic CLI mutations have been run on a real TV. The public launcher is compile/signature checked; it uses a distinct package from the development launcher and needs its own on-device validation before a stable release. No stable binary release is provided yet.

## Adding support

Submit a proposed profile change with a sanitized model/build, package purpose, original state, UID/shared-UID analysis, expected feature loss and test results. Include streaming playback, remote navigation, Settings and reboot behavior. Use synthetic dumps in tests; never attach a complete real-device dump.

A successful `pm disable-user` command alone is not sufficient evidence. Packages and responsibilities change between firmware versions. Protected components should remain protected; do not propose bypasses as routine cleanup.
