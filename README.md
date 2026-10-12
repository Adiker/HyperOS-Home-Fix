# HyperOS Home Fix

[![Build and verify](https://github.com/Adiker/HyperOS-Home-Fix/actions/workflows/build.yml/badge.svg)](https://github.com/Adiker/HyperOS-Home-Fix/actions/workflows/build.yml)

An experimental, open-source Vector/LSPosed module for the stock POCO Launcher on Android 16 / HyperOS 3. It aims to make HOME close the app drawer and native launcher overlays, then return to the configured default desktop page.

**Status: exact APK analyzed; physical-device behavior is not yet verified. No stable release has been published.** A successful build proves compilation and offline checks, not working gestures or preserved animations on a phone.

## Compatibility

| Target | Status |
| --- | --- |
| POCO F8 Ultra (`myron`), Android 16 / HyperOS 3 | Implementation target; device tests pending |
| `com.mi.android.globallauncher`, `RELEASE-7.50.06.2592-09011644` (code `750062592`) | Analyzed using REA/JADX and exact DEX declarations |
| Other devices, Android versions, launcher versions or repacked APKs | Inactive by design |

The installed base APK must have SHA-256 `aba8d486b2116aba1e32143e5d2a2598460965bc5b1aba3db60714a432b11d23`. Split packages are rejected. The device and SDK checks are conservative allowlist checks, not proof that a particular firmware build works. No HyperOS firmware build is currently device-verified. ResuKisu/root-manager choice is outside the hook; the framework must support legacy Xposed API 93. Build 3 delivers private settings through caller-checked IPC rather than relying on cross-process preference-file access.

## Installation

1. Install the debug APK from the **Build and verify** workflow artifacts for the commit you intend to test. Debug builds are experimental; signing identities can differ between local builds and CI runs. If Android reports an incompatible signing certificate, uninstall only this module, install the new APK and initialize its settings again. Once a verified release exists, use its signed APK instead.
2. Enable **HyperOS Home Fix** in Vector/LSPosed. Select only **POCO Launcher** (`com.mi.android.globallauncher`) as scope. Do not select SystemUI or the Android framework.
3. Open the module's settings app once, enable the HOME fix, then reboot. This initializes the private settings; the module stays inactive without a successful settings response.
4. Leave **Enable HOME fix** on. Optionally enable diagnostics during testing. Settings are read on the next HOME transaction.
5. Run the [device test checklist](docs/TESTING.md). Configure the desired default page in POCO Launcher itself. If behavior is unchanged, enable logging, perform HOME, reopen the settings app and use **Refresh diagnostics** / **Copy diagnostics**.

The module does not modify Xiaomi's APK, replace the launcher, request network permission, or install a service. Back keeps its native handler; a cancellation-only observer discards pending HOME corrections when Back starts. Quick Switch and app launch are not used to close the drawer preemptively. See [architecture and limitations](docs/ARCHITECTURE.md) for HOME event filtering and native-state exceptions.

## Recovery and diagnostics

Disable the module in Vector/LSPosed and reboot. If the launcher is unusable, open the framework manager through Android Settings → Apps, a notification, or a previously prepared ADB connection. With authorized ADB, launch its manager using its actual installed component; manager package names differ between frameworks. If Android cannot boot, use your root/framework's documented safe-mode or recovery procedure; prepare and verify that procedure before enabling any experimental module. Uninstalling the module APK also removes it; do not clear POCO Launcher's data.

Diagnostic event logging is off by default. Turn it on and check framework logs for `HyperOSHomeFix:`, or copy diagnostics from the module's settings screen. The screen separates the requested switch state, a framework callback probe in the settings app, and the launcher's last reported initialization state. `READY` means hooks reported successful installation; it does not prove physical HOME behavior. `UNSUPPORTED_APK` includes the observed version/hash for comparison. `ERROR` reports an exception type. No report means the launcher has not reached the reporting bootstrap, or IPC failed; it does not identify one cause on its own. Reports include a timestamp/build/PID and can be stale after a process exits.

The provider exposes settings only to the module and the launcher UID; only the module can read diagnostic status. Other apps cannot read switches or forge reports. Native correction waits asynchronously for current initialized/enabled settings, with the same request identity and expiration checks. An incompatible APK is left untouched. A native hook error disables correction for that launcher process. Changes to framework activation require a launcher restart or reboot.

## Build

Use a full JDK 17, Android SDK platform 36 and build-tools 35.0.0:

```sh
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`. The official Gradle 8.11.1 wrapper and distribution checksums are pinned. Xiaomi's APK is not needed to compile or run unit tests. Optional read-only declaration verification, with your own local APK:

```sh
python3 tools/verify_launcher.py /path/to/PocoLauncher.apk
```

The verifier checks the exact digest and declared DEX descriptors, without decompiling or redistributing vendor code.

Every branch push/PR builds a debug APK and runs unit tests, lint and packaged-module checks. Tagged releases require signing secrets, protected approval and an attestation of physical testing for that exact commit. See [release instructions](docs/RELEASING.md). Do not create `v0.1.0` until the CI build passes and the device checklist is completed.

## Research and contributing

[REA analysis and upstream references](docs/RESEARCH.md), [offline/device test status](docs/TESTING.md), and [architecture](docs/ARCHITECTURE.md) explain the evidence and remaining gaps. Include your exact firmware, launcher version/hash, framework version and reproduction steps in bug reports. Do not upload Xiaomi APKs, decompiled source, credentials or unredacted private logs to this repository.

The implementation was written independently. CustoMIUIzer and HyperCeiler were research references; their GPL/AGPL implementations were not copied. Project source is licensed under [Apache-2.0](LICENSE). The Gradle wrapper is upstream Gradle tooling under Apache-2.0.
