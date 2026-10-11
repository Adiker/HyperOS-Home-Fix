# Research and evidence ledger

## Target and method

Analysis date: 2026-10-11. The supplied APK was inspected statically with REA 6.4.0 and the caller-supplied JADX headless MCP 0.7.1 provider (one worker, full JDK 27). The engine JAR digest was verified against its upstream release checksum. Build/test verification used a separate full JDK 17. The APK was never executed by the analysis provider.

Observed package: `com.mi.android.globallauncher`; version `RELEASE-7.50.06.2592-09011644`; version code `750062592`; APK SHA-256 `aba8d486b2116aba1e32143e5d2a2598460965bc5b1aba3db60714a432b11d23`. REA reported 7,011 classes and 4,356 resources. Its signature verification status was **not performed**; runtime compatibility uses the exact artifact hash, not a claim about Xiaomi's signing certificate.

REA exposes parsed display signatures and reports exact DEX descriptors as unknown. Therefore `tools/verify_launcher.py` independently checks actual DEX class-data definitions and prototypes for all 33 required methods/fields. Reflection also validates declaring class, parameters, return types and field types before any correction hook is installed. No signatures were selected from an older MIUI version.

## Static observations

Evidence references identify complete locally retained REA records. Vendor decompilation/records are intentionally not distributed; the ledger contains only identity, signatures and original analytical summaries.

| Question | Observation / design implication | REA Evidence |
| --- | --- | --- |
| Package identity | Exact supplied version, class inventory and manifest | `ev_23ccad9c1268d760d8b05c6352a0b8629beed07fc14b6a92b4714682b2d75224` |
| Launcher architecture | Relevant handlers are declared in `BaseLauncher`; `Launcher` is the concrete activity subclass | `ev_c5de01e029bc2079555a603a8ef8865bd449af70552168ded29659f9b486a4a4`, `ev_6082481a723c4c1e58519e8dad795b74d3e43cc155b3399b514183b6af6ab317` |
| Native HOME intent | `onNewIntent(Intent)` has state-dependent early exits; drawer may delegate to its own Back/search handling, and focused/native filtering affects default-page handling | `ev_cd953b3bc2289317e4ca96d14a3ee769c31de9baf55c1c165061cd2f28793861` |
| Drawer closure | Private `hideAppView()V` uses the native state manager to return from ALL_APPS to NORMAL | `ev_2d2f6ff3f6bc2b8faeddfc7eb16102aaa892c6dd924b365af48f75861498f228` |
| Overlay/folder cleanup | Native `onNewIntentWhenAlreadyOnHome()V` handles tips, search/shortcut menus, folders and edit mode; some cleanup paths intentionally return before changing pages | `ev_e70ad88b232dae4ffed5c1fa5656b1e70ff12600811c4dc30379c540659ec985` |
| Default page | `scrollToDefault()` merely requests focus. `Workspace.moveToDefaultScreen(Z)V` reads the configured default screen and snaps/sets it | `ev_73a844877f1c0c046f32ba1667f948d4ba19b4823143507db9d00cfab0921130`, `ev_6d297996586b8b1c43ba1203f28834338aeb135837beab393bbf3daf9895fc70` |
| AllApps | Current classes include `BaseAllAppsContainerView`; its `onNewIntent()` delegates to Back, which can consume an intent while clearing an intermediate drawer state | `ev_c75b3f15a5f1e4eedf09a0a7eab025d98cb5d0db42ee836183f86cbe3705cd9a`, `ev_d45d10aaf5b82c89e9ea672736486702ca41c8571dc93eef120e060508350ec5` |
| App-to-HOME start | `commonAnimStartAppToHome(BaseLauncher)` schedules native HOME callbacks on the main executor | `ev_f129765a5f73b75df95bfa19489b110ae5d332b3b7d9271aa2531321eb7c8083` |
| Legacy completion | `finishAppToHome(ZZZ)V` passes its successful-home boolean into `commonFinishAppToHome(Z)V`; animation completion later clears `mIsAnimatingToLauncher` | `ev_3ad065c4349d74a34fbd6ef3173de0f196bd22261fd74a37ee133e12e2963356`, `ev_cc32588934df86ed4f86675e6e4310dbc7e46bb50a90ee019b6c4810a80e8335`, `ev_83e5d2840b3354295b8e49d4401ecc7aebbfe8acb352e3458596acb711201712` |
| Modern completion/cancellation | The app-to-HOME listener has separate surface and animation completion. Its cancellation handler invokes both callbacks too, so completion alone does not prove a successful gesture | `ev_0712cd1fe8bef77ea90e9afc11dcf4b49f67a9d9f18d343eb28fe7ea0f2f7f42`, `ev_564f4442f2f51d4ee1c9e2b4360f9d574a95f89204b08001c138a0ebd8437e44` |
| Native animation completion | `notifyMiuiAnimationEnd()` posts native back-home cleanup; `onEnterAnimationComplete()` retains native rotation cleanup | `ev_788fbaf7f3a7b8d136a05039c8625788d0fbfdc36fe94bd33d85a2c3934477eb`, `ev_418f67c74436a97cd913b5e5f28f7c8d8bfa077aef532b7a0d75b4aca06c419e` |
| Back isolation | Verified native `BaseLauncher.onBackPressed()V` handles drawer, menus, folders and widgets. Observer discards pending module work and preserves that body | `ev_87efe83310b8231d9aecbd5543716fc605948b1b8bd3d420182f034877392e79` |
| Quick Switch classification | Native `isQuickSwitchMode()` reads mutable navigation mode; eligibility must be captured at the original callback, before main-thread dispatch | `ev_e9209e03246c5de71c388869d1e4c92c7616e8afd410073c29f75c351603ab0e` |

These observations justify the hook design but do not prove actual HOME delivery, physical gesture timing or animation quality. Static incoming-reference recovery cannot exclude reflection/dynamic behavior. The exact profile and conservative cancellation deliberately leave unsupported paths native.

## Upstream research

- [Original CustoMIUIzer historical mirror, commit ce0d6f2](https://github.com/TTTT55/custoMIUIzer/blob/ce0d6f2353fbfc4c9c98cf3ffb1a9f1d1b2a1432/app/src/main/java/name/mikanoshi/customiuizer/mods/Launcher.java#L1488-L1497) invokes `hideAppView()` when launching from drawer items. Its [package guard](https://github.com/TTTT55/custoMIUIzer/blob/ce0d6f2353fbfc4c9c98cf3ffb1a9f1d1b2a1432/app/src/main/java/name/mikanoshi/customiuizer/MainModule.java#L377-L399) historically supported POCO. This is app-launch behavior, not a verified HyperOS 3 HOME fix. The original source server was unavailable; the identified mirror was used.
- [Maintained CustoMIUIzer fork, commit dfd3a4d](https://github.com/MonwF/customiuizer/blob/dfd3a4de5201d058357c692250e35b1cb74edfb6/app/src/main/java/name/monwf/customiuizer/mods/Launcher.java#L1178-L1207) retains drawer auto-close hooks. Its scope/version documentation does not establish support for this POCO APK.
- [HyperCeiler folder adaptation, commit 5e46860](https://github.com/ReChronoRain/HyperCeiler/blob/5e4686069dd7ab1f3697e256d5fc7d68fb73e317/library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/folder/FolderAutoClose.kt#L30-L53) confirms OS3's `BaseLauncher`/folder-manager migration. This project uses the supplied APK's native HOME handler instead of copying that app-launch hook.
- [HyperCeiler AllApps rendering hooks](https://github.com/ReChronoRain/HyperCeiler/blob/5e4686069dd7ab1f3697e256d5fc7d68fb73e317/library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/drawer/AllAppsContainerViewBlur.kt#L40-L77) concern blur and inflation/resume. They are unnecessary for this HOME correction. [Issue 572](https://github.com/ReChronoRain/HyperCeiler/issues/572) reports a historical drawer/gesture crash with blur enabled; it motivates animation regression testing without proving a crash in the current target.
- [Vector legacy preference handling, commit ddeed8c](https://github.com/JingMatrix/Vector/blob/ddeed8ca1ffedfaba7f4341b911089906862d5c4/legacy/src/main/java/org/matrix/vector/legacy/LegacyDelegateImpl.java#L113-L143) and [XSharedPreferences](https://github.com/JingMatrix/Vector/blob/ddeed8ca1ffedfaba7f4341b911089906862d5c4/legacy/src/main/java/de/robv/android/xposed/XSharedPreferences.java#L155-L187) show API 93/shared-preference redirection. Actual access still requires testing on the installed framework.
- [ZuiRecentsFix, commit 4651e6e](https://github.com/miner7222/ZuiRecentsFix/blob/4651e6e64496a0c93a8077725b6b5b09c590d17e/app/src/main/java/io/github/miner7222/fixrecents/LauncherRecentsHooks.kt) illustrates why lifecycle-only HOME resets can interact with Recents. It targets another vendor and supplies no Xiaomi hook signatures.

The bounded search covered CustoMIUIzer launcher hooks and 139 HyperCeiler HOME rule files. No current exact HOME-to-default-page solution was found within that scope. This is not a claim that no such module exists elsewhere.

CustoMIUIzer's GPL and HyperCeiler's AGPL code was not copied. The Apache-2.0 implementation was written independently from API observations of the supplied artifact.

## Review result and remaining unknowns

REA-assisted review identified stale lifecycle/animation transactions, duplicate native cleanup, cancellation callback ambiguity and mutable gesture-state classification. The final implementation invalidates owner tokens and queued starts, preserves native Back, snapshots eligibility at the event and uses a recursion guard. Final static review found no remaining actionable defect.

Still unknown: physical-device behavior, framework/SELinux settings sharing, actual callback thread ordering, every firmware-specific overlay path and visual animation continuity. See [the pending device checklist](TESTING.md); release attestation must not be filled from these static findings.
