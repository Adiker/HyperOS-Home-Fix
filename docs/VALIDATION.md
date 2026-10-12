# Initial local validation — 2026-10-11

The initial local results below were recorded before repository publication. The publication/CI follow-up is recorded separately below. No physical-device verification or stable release occurred.

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
| GitHub CI artifact | See publication/CI follow-up below |
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

## Publication and CI follow-up

The owner approved publication on 2026-10-11. [The public repository](https://github.com/Adiker/HyperOS-Home-Fix) was created without a version tag or release.

The [first CI run](https://github.com/Adiker/HyperOS-Home-Fix/actions/runs/38145621392) compiled the debug APK and ran unit tests, but failed Android lint's `OldTargetApi` check for target SDK 35. The project now compiles and targets API 36 (Android 16) using AGP 8.10.1, Gradle 8.11.1, JDK 17 and build-tools 35.0.0. [Android's compatibility documentation](https://developer.android.com/build/releases/agp-8-10-0-release-notes) confirms this toolchain supports API 36.

The launcher hooks and exact compatibility allowlist are unchanged. The [corrected CI run](https://github.com/Adiker/HyperOS-Home-Fix/actions/runs/38145950568), for commit `4656dd467bdf2df023855655be5ea93a0423130a`, completed successfully. Its downloaded reports confirm 19 passing tests, zero failed/ignored tests and zero Android lint issues. Device behavior remains unverified.

Lint still treats correctness warnings as errors. Only `AndroidGradlePluginVersion`, an online suggestion to use newer Gradle/AGP releases, is disabled because the compatible toolchain is deliberately pinned. `OldTargetApi` remains enabled.

The [CI debug artifact](https://github.com/Adiker/HyperOS-Home-Fix/actions/runs/38145950568/artifacts/11705101031) was downloaded and independently checked: signature verification and 16 KiB alignment passed; legacy API 93 metadata, the single POCO scope, entry point, package and module-only DEX passed. The APK SHA-256 is `1a526fff6724d651f291555b74e030c77f73529878fcff912aae7f4275d78ec7`. The artifact ZIP digest reported by GitHub is a separate value.

REA package inspection of the actual CI APK produced evidence `ev_423139adaf33bde58e138b7b9e2699e29e4e4fb39f32c8853c786db834e45b1e`, confirming target SDK 36, version `0.1.0` and no Android permissions. CI artifacts expire after 14 days; rerun the workflow for a fresh testing APK. No version tag, production signing key or stable release was created.

## Experimental build 2 — 2026-10-12

The owner reported no visible behavior change after activation and a soft reboot. REA confirmed that POCO's native home-to-home gesture sends a MAIN/HOME intent with `ignore_bring_to_front=true`; the first implementation rejected it. Build 2 fixes this demonstrated filter bug and adds opt-in bootstrap/intent diagnostics. It keeps the same exact APK allowlist and native methods.

Local debug/release builds and lint pass; 21 tests pass with zero failures/errors/skips, including the native-flag regression and non-HOME isolation. Packaged module verification passes, with module version code 2. A real-device retest and activation logs remain required; the reported phone behavior is not considered fixed merely from these offline results. The push triggers CI for this build and publishes a testing artifact, not a stable release.

## Experimental diagnostic build 3 — 2026-10-12

The supplied Vector export confirms the recommended scope and entry-class loading in the launcher process, with Vector 2.2, Android SDK 36 and device `myron`. It contains no module diagnostic entries establishing callback/hook installation, switch access or installed APK identity. The owner reports that HOME still does not work as requested. No successful physical retest is recorded.

Build 3 replaces cross-process preference reads with authenticated asynchronous settings IPC, adds a settings-app callback probe and last-observed launcher status, records exact compatibility rejection, and requires successful hook registrations. An independent review caught a Direct Boot initialization edge and stale diagnostic-switch reads; both were corrected. The final review found no further actionable defect, with Binder/boot/device behavior still pending.

Local debug and unsigned release builds, debug/release lint and 27 JVM tests pass (zero failures/errors/skips). Exact launcher identity/33 DEX declarations and the actual module manifest/provider/scope/entry/owned-only DEX pass. Debug signing and 16 KiB ZIP alignment pass. The local APK digest is `47a57ea2638245fdef3f87cf8898cdc607332c8b400eee7a43e24e40e4a97c37`, version code 3.

REA inspected the final compiled initialization method: evidence `ev_11f823cae77efcf6f5e4a485683fb2353faec1d1c0e388569e314d00c96b1fa5` confirms the user-unlock check, protected unlock broadcast receiver, weak activity owner and settings-worker dispatch in the built artifact. This is static evidence only. The diagnostic build is experimental, with no tag or release.

The [build 3 CI run](https://github.com/Adiker/HyperOS-Home-Fix/actions/runs/38277157739), for commit `3524582c726b23a002538f3b677094dd298bfd7a`, completed successfully. Downloaded reports confirm 27 passing tests, zero failures/ignored tests and zero lint issues. The [CI APK artifact](https://github.com/Adiker/HyperOS-Home-Fix/actions/runs/38277157739/artifacts/11759292934) was independently checked for package/version code 3, provider/legacy scope/entry and module-only DEX, signature and 16 KiB alignment. Its APK SHA-256 is `da0852076e8f0792916978ee0caee8cdddf82b39b1f78b075252cea67d227708`. This artifact remains a diagnostic testing build; physical behavior is unverified and no release was created.

## Experimental build 4 — 2026-10-12

The owner reports unchanged behavior in build 3; the settings-screen callback probe is positive, but no launcher report is recorded. The new export confirms entry-class loading in the launcher process without confirming its package callback, onCreate interception or settings/status IPC. Device behavior is still failing, and no root cause is claimed.

Build 4 observes the exact concrete Launcher.onCreate declaration verified in the supplied DEX and makes the initial status report optional and earlier than settings lookup. Independent review found no further actionable defect in that patch. The native HOME correction methods and identity guards are unchanged. Local debug/unsigned-release builds, debug/release lint and 27 tests pass with zero failures/errors/skips. Exact APK identity/34 DEX declarations, packaged-module/provider checks, debug signature and 16 KiB alignment pass. Local APK SHA-256: `3d53dd1d339263c2924f888d966bd70c80fee3e99b530ea46e1c8b2d385545b7`.

REA checked the compiled entry method: `ev_842675dad89b6907d786eac5d78bb6e057f4155506c4f8feef870a01450e5d71`. This confirms the concrete class/declared-method bootstrap in the built artifact, not actual delivery on the phone. The pushed source triggers CI; a successful CI artifact and a physical retest remain required before claiming working behavior. No release/tag is created.

The [build 4 CI run](https://github.com/Adiker/HyperOS-Home-Fix/actions/runs/38280075815) completed successfully for `2c2cce99c680be7081f32900ebcabe03eaf740b4`. Downloaded reports confirm 27 tests, zero failures/ignored tests and zero lint issues. The [CI APK artifact](https://github.com/Adiker/HyperOS-Home-Fix/actions/runs/38280075815/artifacts/11761260853) passed independent package/version-code-4/provider/scope/entry/owned-only DEX, signing and 16 KiB alignment checks. Its APK SHA-256 is `6b354ebd867eb0ba844974f15eeba6142efb43c381203ab293f8383e79e930b2`. The candidate requires physical retesting and is not a stable release.

## First physical-device session — 2026-10-12

ADB connected to the owner's POCO F8 Ultra (`myron`), Android SDK 36, firmware `OS3.0.302.0.WPMEUXM`, Vector 2.2. The installed launcher and module APK digests exactly matched the analyzed launcher and build 4 CI artifact. The launcher process predated the build 4 installation. Restarting only the launcher, without clearing its data, produced build 4 bootstrap/settings IPC/READY reports in a fresh process. This resolves the current missing-report condition, without proving the cause of every earlier build's failure.

Build 4 physical checks passed for HOME in drawer root, a different desktop page, and an injected KEYCODE_HOME route. With the feature disabled, HOME from drawer search left the drawer visible; returning by HOME from an app launched by a drawer icon also left the drawer visible. The latter path still failed with the feature enabled. These observations include ADB-injected touchscreen gestures and local screenshots; they are physical-device evidence, not human manual gesture or visual-quality certification. Screenshots, app lists, device identifiers and full logs remain private.

The native trace recorded a CLOSE_TO_HOME animation with the actual NavStubView$9 listener and animation completion at 19:11:30.736, followed by launcher focus gain at 19:11:30.779. The module consumed successful completion and rejected it before creating a request whenever focus was absent. Build 5 retains the bounded completed request until focus is gained, without extending the original deadline or permitting canceled work to revive. Independent review passed. Local debug/release compilation and lint, 30 tests with zero failures/errors/skips, exact launcher/34 declaration checks, module package/provider checks, debug signing and 16 KiB alignment passed. Build 5 still requires the app-to-HOME physical retest. No stable release was created.

The [build 5 CI run](https://github.com/Adiker/HyperOS-Home-Fix/actions/runs/38283971762) completed successfully for commit `dd29df66ab35199f69ac30d13cc60b05e7612e76`. Downloaded reports confirm 30 passing tests, zero failures/ignored tests and zero lint issues. The [CI APK](https://github.com/Adiker/HyperOS-Home-Fix/actions/runs/38283971762/artifacts/11762187979) passed independent provider/legacy metadata/scope/entry/owned-only DEX, signing and 16 KiB alignment verification. APK SHA-256: `e47588455b8e6b9a9113d330d92a455d306d7904214ad3a500ced825aebd8ee8`. Its debug certificate differs from build 4, so an in-place Android update is unavailable. The owner chose manual installation before physical retesting; no module uninstall/install was performed through ADB.

## Build 5 physical retest — 2026-10-12

The owner installed and activated build 5 manually. The installed module digest matched the exact CI APK above, and the installed launcher digest matched the analyzed APK. The existing launcher still ran build 4 until a launcher-only restart loaded build 5. The new process reported readable settings IPC, exact APK validation and installed hooks. Reopening the settings app showed `READY`, reported build 5, the current launcher PID and a native correction event; both feature and diagnostic switches were enabled.

On the same POCO F8 Ultra, firmware `OS3.0.302.0.WPMEUXM`, Android SDK 36 and Vector 2.2:

| Physical check | Observed result |
| --- | --- |
| HOME from an app opened by its actual drawer icon | PASS: returned to the configured default desktop, including a repeated run |
| Disabled/enabled comparison for that same app-to-HOME path | PASS: disabled left drawer visible; enabled returned to default desktop |
| HOME from drawer root/search with entered text | PASS: drawer closed and default desktop appeared |
| HOME from an adjacent non-default desktop page | PASS: returned to the configured default desktop |
| Injected Back from a drawer-launched app | PASS: returned to the drawer instead of forcing the default page |
| Open Recents on a non-default page, then injected Back | PASS: returned to the same non-default page, with no extra module correction in the checked interval |
| Horizontal bottom-edge Quick Switch between two apps | PASS: returned to the previous app; native app-to-app trace observed, with no extra module correction in the checked interval |

The first successful app-to-HOME trace showed completion at 19:34:26.195, waiting for the native drawer transition at 19:34:26.223 and native correction at 19:34:26.457. This corroborates the completion/focus fix on the tested path. Launcher process identity remained stable throughout these checks. No launcher data was cleared, system APK modified, whole-phone reboot performed or screen-timeout setting changed. The HOME fix was left enabled after the disabled/enabled comparison.

These checks used ADB-injected input on the physical phone, screenshots and runtime diagnostics. Black/locked-screen captures and an unconfirmed edge-Back attempt were excluded from passing results. Private screenshots, UI dumps, device identifiers, app inventory and full logs remain local and ignored. Successful completion and final page state do not certify frame-by-frame preservation of animation quality. Folder/overlay coverage, changed default page, navigation-mode comparisons, interruption/stress, cold boot/first unlock and the remaining [device matrix](TESTING.md) still require verification. No tag or stable release was created.
