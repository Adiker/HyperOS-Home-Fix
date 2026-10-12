# Changelog

## 0.1.0 — unreleased

### Experimental build 5 — 2026-10-12

- Keep a successfully completed HOME gesture pending until the launcher gains focus; inherit the original gesture deadline and preserve all cancellation guards.
- Add regression tests for late focus, expiration and cancellation before focus. Physical build 4 traces showed animation completion preceding focus by 43 ms and app-to-HOME still leaving the drawer open.
- Refresh settings switches from stored values on resume/diagnostic refresh and keep the settings layout clear of Android 16 system bars.

### Experimental build 4 — 2026-10-12

- Observe the verified concrete `Launcher.onCreate(Bundle)` entry instead of its `BaseLauncher` super-call, after the native activity initialization has returned.
- Attempt the first bootstrap report before reading switches; a failed optional report no longer aborts initialization. Settings access remains mandatory and fail-closed.
- Keep native HOME methods and exact compatibility guards unchanged. The build 3 phone failure is unresolved; successful entry-class loading and the settings-app probe do not prove launcher callbacks or IPC access.

### Experimental build 3 — 2026-10-12

- Add a settings-screen callback probe and last-observed launcher status, profile/hash, module build and optional last event, with refresh/copy actions.
- Replace cross-process `XSharedPreferences` reads with asynchronous caller-checked settings IPC; preserve fail-closed switches, request expiration and cancellation.
- Defer launcher initialization before the first user unlock to keep private settings access out of Direct Boot.
- Report compatibility rejection and initialization failure independently of the event-logging toggle.
- Use exact package/process isolation rather than treating framework `isFirstApplication` bookkeeping as a compatibility check.
- Require non-null hook registrations before reporting successful installation. Device failure remains under investigation; this is not a verified stable release.

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
