# Architecture

The legacy Xposed entry point is `io.github.adiker.hyperoshomefix.HomeFixModule`. Module scope and runtime guards select only the primary `com.mi.android.globallauncher` process. There is no SystemUI hook, replacement activity, intent rewrite, method replacement or modification of native return values.

## Identity and initialization

The bootstrap hook runs after the analyzed `BaseLauncher.onCreate(Bundle)`. It reads package/version information and hashes the installed base APK on a worker thread. No state correction is installed until all identity checks and every required reflective member pass. Unsupported Android SDK/device identifiers bypass bootstrap entirely. Split APK installations are conservatively rejected.

The SDK 36 and `myron` checks identify the intended Android/device combination; they do not detect or certify a firmware build. The exact APK digest is the strongest static compatibility boundary. Porting requires fresh analysis, not removal of this check.

An initialized, framework-readable `settings` preference file is required. `initialized`, `enabled` and `diagnostics` are the only stored values. Legacy API 93 preference redirection permits sharing these non-sensitive switches with the launcher process. An unreadable file disables corrections. An error latches the module inactive until process restart and unhooks correction callbacks where possible.

## HOME intent path

Before `BaseLauncher.onNewIntent(Intent)`, the module recognizes `ACTION_MAIN` plus `CATEGORY_HOME`, excluding native app-location, feed-return and `ignore_bring_to_front` special intents. It records focus and creates a three-second request, then lets the complete original method run unchanged. A hook on the native already-home handler records whether cleanup already ran, preventing duplicate cleanup.

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
- Shared-preference access through the installed framework/SELinux policy.
- Drawer search/category/folder animation behavior and default-page movement during native closing animations.
- A cold launcher startup before asynchronous compatibility verification uses native behavior only.
- Overview, feed, split/floating-window and other non-normal states retain native HOME handling; their HOME-to-default-page behavior is not promised by this initial implementation.
- Rapid Back/Recents/Quick Switch and new touches while HOME completes need regression tests; static absence of method replacement does not prove absence of runtime interactions.

These are release gates, not completed device tests.
