# Changelog

## 0.1.0 — unreleased

### Experimental build 2 — 2026-10-12

- Fix rejection of POCO's native home-to-home HOME intents carrying `ignore_bring_to_front=true`; the previous filter could leave drawer/desktop HOME unchanged even with active hooks.
- Add opt-in bootstrap and intent-filter diagnostic messages, including the module build code.
- Add regression tests for native HOME flags and non-HOME/special-intent isolation. The correction still requires physical-device validation.

### Initial experimental build

- Initial independently written Java module targeting one analyzed POCO Launcher APK.
- HOME intent correction through native drawer/overlay handling and default-page navigation.
- Completion-aware launcher gesture handling with cancellation and Quick Switch exclusions.
- Exact package/process, Android/device, version and APK SHA-256 allowlist; reflective preflight and process circuit breaker.
- Local enable/diagnostics switches with framework shared preferences.
- Gradle wrapper, offline tests, CI debug build, protected signed-release workflow and device verification checklist.

Physical POCO F8 Ultra behavior and animation preservation remain unverified. This entry does not represent a published release.
