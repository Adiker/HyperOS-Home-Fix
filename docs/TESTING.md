# Verification

## Offline

Run `./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug assembleRelease lintRelease` with JDK 17 and SDK 35.0.0. An unsigned release build checks release compilation; it is not an installable published release. Unit tests cover strict identity rejection, actual file hashing, HOME classification, bounded request cancellation/expiry and modern gesture cancellation/end ordering.

Run `python3 tools/verify_launcher.py /path/to/PocoLauncher.apk` for an exact-hash DEX declaration check. Xiaomi APKs and REA decompilation results remain local and ignored. Packaged-module checks verify legacy metadata/scope/entry point and that no Xposed implementation or vendor code was bundled. APK identity, signature and alignment are checked with Android build tools. REA inspects the built module's manifest and selected methods as a separate shipped artifact.

Results recorded for this initial implementation are in [VALIDATION.md](VALIDATION.md). GitHub CI must run after authorized repository publication; local results do not count as a GitHub Actions run.

## Physical device — all pending

Record exact firmware build, launcher version/hash, Android SDK, device codename, root/framework versions, navigation mode and commit/APK digest. Test the same commit and build intended for release. Prepare a framework recovery path first.

| Scenario | Expected result | Status |
| --- | --- | --- |
| HOME in drawer root, category, search and work profile | Drawer closes and configured default page appears | Pending |
| HOME on every desktop page, including a non-first configured default | Configured default page appears | Pending |
| HOME with folder, shortcut menu, search menu, edit mode or widget overlay | Native cleanup and default page; no crash | Pending |
| HOME from another app launched from drawer/folder/other page | Native HyperOS app-to-home animation completes, then default page | Pending |
| Three-button navigation and full-screen gestures | Same intended HOME result | Pending |
| Rapid/repeated HOME, animation scale 0/0.5/1/2, interrupted animation | No recursion, stale correction, flash or crash | Pending |
| Back from app/drawer/folder; predictive Back | Native behavior unchanged | Pending |
| Recents open/close, task selection, horizontal Quick Switch | Native behavior unchanged; no surprise default-page reset | Pending |
| Home from overview/feed, split-screen, floating window, PiP and keyguard | Native handling remains usable; document coverage limits | Pending |
| Disable module switch; diagnostics on/off | No correction when disabled, no module log messages when off | Pending |
| Different APK/version, Android SDK or device | No correction hooks installed | Pending |
| Launcher restart, cold launch, rotation and process recreation | No crash or retained activity/transaction | Pending |
| Other applications and launcher auxiliary processes | No module state manipulation | Pending |

Compare module disabled/enabled on the same firmware and launcher. Capture a screen recording for animation comparisons and redact framework logs. Test immediate Back/Recents/Quick Switch during HOME settling. If any mandatory behavior fails, do not attest verification or tag `v0.1.0`.

## Release gate

`v0.1.0` requires a successful CI build for the exact commit, all applicable physical-device checks, owner approval, and configured signing secrets. Store the tested tag/commit in the protected release environment only after the tests actually ran. Neither static REA analysis nor unit tests establish hardware compatibility.
