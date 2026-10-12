# Verification

## Offline

Run `./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug assembleRelease lintRelease` with JDK 17, SDK platform 36 and build-tools 35.0.0. An unsigned release build checks release compilation; it is not an installable published release. Unit tests cover strict identity rejection, actual file hashing, HOME classification, bounded request cancellation/expiry, modern gesture cancellation/end ordering, and IPC caller authorization. JVM tests do not execute Android Binder or prove provider accessibility on the phone.

Run `python3 tools/verify_launcher.py /path/to/PocoLauncher.apk` for an exact-hash DEX declaration check. Xiaomi APKs and REA decompilation results remain local and ignored. Packaged-module checks verify legacy metadata/scope/entry point and that no Xposed implementation or vendor code was bundled. APK identity, signature and alignment are checked with Android build tools. REA inspects the built module's manifest and selected methods as a separate shipped artifact.

Results recorded for this initial implementation are in [VALIDATION.md](VALIDATION.md). GitHub CI must run after authorized repository publication; local results do not count as a GitHub Actions run.

## Physical device — current candidate verification incomplete

Build 5 passed the previously failing app-from-drawer HOME path, including a disabled/enabled comparison on the same phone and APK. Drawer search, a different desktop page and basic Back/Recents/Quick Switch checks also passed. Results in [VALIDATION.md](VALIDATION.md) are bounded ADB-driven physical checks, not a full device attestation. The matrix remains incomplete, including interruption during pending HOME and visual animation-quality comparisons.

Record exact firmware build, launcher version/hash, Android SDK, device codename, root/framework versions, navigation mode and commit/APK digest. Test the same commit and build intended for release. Prepare a framework recovery path first.

| Scenario | Expected result | Status |
| --- | --- | --- |
| HOME in drawer root, category, search and work profile | Drawer closes and configured default page appears | Partial: root/search passed; category/work profile pending |
| HOME on every desktop page, including a non-first configured default | Configured default page appears | Partial: one adjacent page passed; all pages/changed default pending |
| HOME with folder, shortcut menu, search menu, edit mode or widget overlay | Native cleanup and default page; no crash | Pending |
| HOME from another app launched from drawer/folder/other page | Native HyperOS app-to-home animation completes, then default page | Partial: one drawer-launched app passed; folder/other page and visual-quality comparison pending |
| Three-button navigation and full-screen gestures | Same intended HOME result | Pending |
| Rapid/repeated HOME, animation scale 0/0.5/1/2, interrupted animation | No recursion, stale correction, flash or crash | Pending |
| Back from app/drawer/folder; predictive Back | Native behavior unchanged | Partial: injected Back from one app returned to drawer; gesture/predictive Back and folder pending |
| Recents open/close, task selection, horizontal Quick Switch | Native behavior unchanged; no surprise default-page reset | Partial: overview/Back retained non-default page and one Quick Switch returned to previous app; task selection/interruption pending |
| Home from overview/feed, split-screen, floating window, PiP and keyguard | Native handling remains usable; document coverage limits | Pending |
| Disable module switch; diagnostics on/off | No correction when disabled, no module log messages when off | Partial: disabled/enabled app-to-HOME comparison passed; diagnostics-off pending |
| Refresh/copy diagnostics after launcher restart | Current build/timestamp, framework probe and launcher stage visible; stale reports identified | Partial: reopening settings showed READY/current build 5/PID and correction event; copy-button check pending |
| Settings provider unavailable or response delayed beyond HOME deadline | HOME left native; no crash or stale correction | Pending |
| Different APK/version, Android SDK or device | No correction hooks installed | Pending |
| Launcher restart, cold launch, rotation and process recreation | No crash or retained activity/transaction | Partial: launcher-only restart loaded build 5 and completed checks; rotation/recreation pending |
| Cold reboot with PIN/password; first unlock | Initialization deferred while locked, then current activity validated after unlock | Pending |
| Other applications and launcher auxiliary processes | No module state manipulation | Pending |

Compare module disabled/enabled on the same firmware and launcher. Capture a screen recording for animation comparisons and redact framework logs. Test immediate Back/Recents/Quick Switch during HOME settling. If any mandatory behavior fails, do not attest verification or tag `v0.1.0`.

For the current experimental build, open settings once, enable diagnostics, restart the launcher through the framework/reboot, perform one HOME in the drawer, then reopen settings and choose **Refresh diagnostics** and **Copy diagnostics**. Report the entire copied text. A settings-app callback proves only that process's callback; `READY` proves reported launcher hook registration, not successful HOME behavior. A missing launcher report can mean bootstrap or IPC failure. Do not infer the failing branch from an absent log alone.

## Release gate

`v0.1.0` requires a successful CI build for the exact commit, all applicable physical-device checks, owner approval, and configured signing secrets. Store the tested tag/commit in the protected release environment only after the tests actually ran. Neither static REA analysis nor unit tests establish hardware compatibility.
