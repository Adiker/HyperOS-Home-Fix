# Architecture

The legacy Xposed entry point is `io.github.adiker.hyperoshomefix.HomeFixModule`. State-changing guards select only the primary `com.mi.android.globallauncher` process. The module's own settings app additionally has a harmless static callback probe. There is no SystemUI hook, replacement launcher activity, intent rewrite or modification of native launcher return values.

## Identity and initialization

Build 4's bootstrap hook runs after the analyzed concrete `Launcher.onCreate(Bundle)`. This avoids relying on interception of its superclass call and observes the completed native activity initialization/injection. It reads package/version information and hashes the installed base APK on a worker thread. No state correction is installed until all identity checks and every required reflective member pass. Unsupported Android SDK/device identifiers are reported without installing correction hooks. Split APK installations are conservatively rejected. No lifecycle callback itself resets the desktop.

The SDK 36 and `myron` checks identify the intended Android/device combination; they do not detect or certify a firmware build. The exact APK digest is the strongest static compatibility boundary. `isFirstApplication` is recorded as framework metadata and no longer vetoes a callback that already matches the exact primary package/process. Porting requires fresh analysis, not removal of the compatibility checks.

Build 3 keeps `initialized`, `enabled` and `diagnostics` in the settings app's private preferences. A narrowly exported `ModuleStateProvider` authenticates the Binder UID against the actual module/launcher UID. The launcher may read those switches and report status, but cannot write settings or read status. Only the module can read status. General queries, mutations and file access are unsupported; package visibility names only POCO Launcher. This removes the dependency on reading another process's preference file. Framework redirection of the module's own private storage is harmless because both the UI and provider run in the same app.

The provider retains one last initialization report with public build/version/hash metadata and, only while diagnostic logging is enabled, one last fixed event message. No desktop content, arbitrary Intent extras, stack traces, APK paths or other app lists are retained. These are timestamped last observations, not live-process attestations. Diagnostic log emission refreshes the logging permission through IPC before writing.

Settings/status IPC and APK hashing run on a single worker. A ready HOME request reserves one settings lookup; the main-thread response must still match the same live request, then checks initialized/enabled before any native state call. Focus/input/lifecycle cancellation and expiry invalidate delayed replies. Temporary settings IPC failure leaves the current HOME native; a native hook error opens the process circuit breaker. All hook registrations must return a non-null registration before the provider reports `READY`.

Build 5 retains a request after proven gesture completion even if the launcher has not gained focus yet. Queueing settings/correction requires focus; the later positive focus callback queues the same request. Waiting inherits the gesture's original deadline and cannot revive a request invalidated by input, negative focus, pause, Back or cancellation. Completion remains mandatory; an ordinary resume/focus callback cannot create a HOME request.

The worker attempts the first `CHECKING` report before reading switches. Every status-report failure is optional and cannot abort initialization. Settings-read failure still disables initialization; if the same IPC cannot report the error, the UI can still show no report. A missing report is not proof of a particular failing branch.

Because the launcher is Direct Boot aware, bootstrap defers private settings IPC until the first user unlock. An application-context receiver listens only for the protected system unlock action and retains a weak reference to the latest launcher activity. The initial check/registration race is rechecked; a destroyed activity is not initialized. Private settings remain credential-encrypted.

## HOME intent path

Before `BaseLauncher.onNewIntent(Intent)`, the module recognizes `ACTION_MAIN` plus `CATEGORY_HOME`, excluding native app-location and feed-return special intents. POCO's `ignore_bring_to_front` flag remains accepted: its native home-to-home gesture sets that flag along with `filter_flag`. It records focus and creates a three-second request, then lets the complete original method run unchanged. A hook on the native already-home handler records whether cleanup already ran, preventing duplicate cleanup.

Already-focused HOME can complete after the original handler. HOME arriving from another app waits for `notifyMiuiAnimationEnd()` or `onEnterAnimationComplete()`. Completion and focus callbacks can finish an existing request but cannot independently create one. If a completion event is absent, the request expires and no forced reset occurs. No fixed delay claims to represent animation completion.

## Gesture paths

- Legacy/native `commonAnimStartAppToHome(BaseLauncher)` creates a bounded token only for an eligible app-to-HOME animation. Quick Switch and close-to-element paths are excluded. `NavStubView.commonFinishAppToHome(boolean)` requires that still-valid token and successful completion (`true`). It reads `mLauncher` and waits while `mIsAnimatingToLauncher` remains true. `setIsAnimatingToLauncher(false)` schedules the pending request after native work.
- The newer app-to-HOME animation uses the exact DEX listener `NavStubView$9`. Its start identifies eligible animations. Native close-to-element/status-bar paths and Quick Switch are excluded. Both `onAnimationEnd(Object)` and `onSurfaceEnd(Object)` must finish before a request can be created. `onAnimationCancel(Object)` marks the transaction cancelled **before** the original method invokes both end callbacks recursively.

These classes live inside POCO Launcher. Hooking its gesture completion listeners does not mean hooking SystemUI. The anonymous listener identity is intentionally tied to the one APK hash; it must never be reused speculatively for another build.

## Correction and lifecycle

All request state and correction runs on the main thread. Queued work checks activity ownership, current request identity, expiration, module settings, window focus, activity/workspace locks and gesture animation state. Only `NORMAL` and `ALL_APPS` are eligible; other native states are left alone.

Correction invokes `hideAppView()` for the drawer and, when native cleanup has not already run, `onNewIntentWhenAlreadyOnHome()` for folders, search/shortcut menus and edit mode. It then invokes `Workspace.moveToDefaultScreen(true)` only when the native state is normal and the default screen is not already showing. If a drawer transition is still running, `onStateTransitionComplete(ILauncherState)` retries the bounded request. Focus loss, pause, destruction, excluded intents, Back, user interaction, unrelated state entry and cancelled animations invalidate pending work and in-flight gesture tokens. A cancellation-only observer on the verified `BaseLauncher.onBackPressed()` lets the original Back execute unchanged. `Activity.onUserInteraction()` is filtered to BaseLauncher instances in the already-scoped process. A recursion guard prevents callback-driven reentry while native cleanup is running.

## Limits requiring a phone

- Callback coverage/timing for three-button navigation, full-screen gestures, interrupted remote animations and disabled system animations.
- Provider IPC and caller UID validation through the installed framework/SELinux policy.
- Drawer search/category/folder animation behavior and default-page movement during native closing animations.
- A cold launcher startup before asynchronous compatibility verification uses native behavior only.
- Overview, feed, split/floating-window and other non-normal states retain native HOME handling; their HOME-to-default-page behavior is not promised by this initial implementation.
- Rapid Back/Recents/Quick Switch and new touches while HOME completes need regression tests; static absence of method replacement does not prove absence of runtime interactions.

These are release gates, not completed device tests.
