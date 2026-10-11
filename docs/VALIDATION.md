# Initial local validation — 2026-10-11

This report describes local checks before repository publication. No GitHub Actions run, physical-device verification or stable release occurred.

| Check | Result |
| --- | --- |
| Exact supplied APK identity + 33 required DEX declarations | PASS |
| JDK 17 / Gradle 8.11.1 / AGP 8.9.2 / SDK 35.0.0 debug compilation | PASS |
| Release compilation (unsigned verification build) | PASS |
| Unit tests | 19 passed; 0 failures/errors |
| Debug and release Android lint, warnings as errors | PASS |
| Actual APK manifest, Xposed API 93 metadata, single POCO scope and entry | PASS |
| Actual module DEX has no bundled Xposed or Xiaomi implementation | PASS |
| Debug APK signature + 16 KiB alignment check | PASS |
| Release signing configuration with disposable key outside repository | PASS; temporary key deleted; no production key created |
| Signed test APK signature/alignment/package/non-debuggable identity | PASS |
| XML/YAML/workflow shell syntax, pinned wrapper checksum | PASS |
| REA-assisted static code review after fixes | No remaining actionable findings |
| Physical POCO F8 Ultra gestures/animations/settings access | NOT RUN; no device attached through ADB |
| GitHub CI artifact | PENDING publication approval |
| `v0.1.0` release | NOT CREATED; requires CI + device verification + approval |

Final local command:

```sh
./gradlew --no-daemon testDebugUnitTest assembleDebug lintDebug assembleRelease lintRelease
python3 tools/verify_launcher.py /path/to/PocoLauncher.apk
python3 tools/verify_module.py app/build/outputs/apk/debug/app-debug.apk /path/to/build-tools/35.0.0/aapt
```

The local debug APK SHA-256 is `84a84f57559bd3abdf7750ca8e5e1a97b9a797eec3be2ec76ad3f71cafcd5071`. Debug keys and build metadata can differ on another machine; this is a local artifact identity, not a reproducible-build guarantee.

REA inspected this final APK as a separate shipped artifact: package/manifest evidence `ev_89e911ab1068ca8db9a519b3493186a1990f5fc8c6ed76722920e03706431361`, compiled package guard/entry-method evidence `ev_1a5cc79bc0139a6d977cbf82fd164980e1927cfe878d00e98bd69e4411f74f89`. The module declares no Android permissions. REA performed static inspection; signing verification was separately performed by Android `apksigner`.

Tests cover exact compatibility rejection, file digest correctness, MAIN/HOME classification, native special-intent exclusion, completion-dependent HOME requests, expiry/cancellation, both orders of modern surface/animation completion, cancellation between callbacks and rejection of queued starts after a cancellation generation changes. They do not simulate real launcher hooks or Android animation scheduling.
