/* SPDX-License-Identifier: Apache-2.0 */
package io.github.adiker.hyperoshomefix;

import android.app.Activity;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.UserManager;
import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** Independently implemented from observations of the allowlisted APK; no vendor code. */
public final class HomeFixModule implements IXposedHookLoadPackage {
    private static final String MODULE = "io.github.adiker.hyperoshomefix";
    private Handler main;
    private final WeakHashMap<Activity, HomeRequest> requests = new WeakHashMap<>();
    private final WeakHashMap<Activity, WeakReference<Object>> gestureViews = new WeakHashMap<>();
    private final WeakHashMap<Object, ModernGesture> modernGestures = new WeakHashMap<>();
    private final WeakHashMap<Object, ModernGesture> legacyGestures = new WeakHashMap<>();
    private final List<XC_MethodHook.Unhook> hooks = new ArrayList<>();
    private final CancellationEpoch cancellations = new CancellationEpoch();
    private final ExecutorService io = Executors.newSingleThreadExecutor(task -> new Thread(task, "HomeFix-IPC"));
    private ModuleChannel channel;
    private Bundle profile = new Bundle();
    private Api api;
    private volatile boolean failed;
    private boolean checking;
    private boolean installed;
    private boolean correcting;
    private volatile boolean diagnostics;
    private boolean bootstrapRegistered;
    private BroadcastReceiver unlockReceiver;
    private WeakReference<Activity> unlockOwner;

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam load) {
        if (MODULE.equals(load.packageName) && MODULE.equals(load.processName)) {
            try {
                Class<?> settings = load.classLoader.loadClass(MODULE + ".MainActivity");
                XposedBridge.hookMethod(method(settings, "frameworkProbe", String.class), new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam p) {
                        p.setResult("Framework callback observed; firstApplication=" + load.isFirstApplication);
                    }
                });
            } catch (Throwable ignored) { /* The settings app retains its explicit negative probe. */ }
            return;
        }
        if (!Compatibility.PACKAGE.equals(load.packageName)
                || !Compatibility.PACKAGE.equals(load.processName) || bootstrapRegistered) return;
        // isFirstApplication is framework bookkeeping, not the primary-process security boundary.
        // Exact package/process, device, APK digest and declaring-member checks still apply.
        try {
            profile.putInt("build", BuildConfig.VERSION_CODE);
            profile.putInt("sdk", Build.VERSION.SDK_INT);
            profile.putString("device", Build.DEVICE);
            profile.putBoolean("firstApplication", load.isFirstApplication);
            // Observe the real activity entry point, not its potentially inlined super-call.
            Class<?> launcher = load.classLoader.loadClass("com.miui.home.launcher.Launcher");
            Method create = method(launcher, "onCreate", void.class, Bundle.class);
            XC_MethodHook.Unhook bootstrap = XposedBridge.hookMethod(create, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    if (param.hasThrowable() || checking || installed || failed) return;
                    try { initialize((Activity) param.thisObject, load.classLoader); }
                    catch (Throwable error) { fail(error); }
                }
            });
            if (bootstrap == null) throw new IllegalStateException("Bootstrap hook registration rejected");
            bootstrapRegistered = true;
        } catch (Throwable error) { fail(error); }
    }

    /** CE settings are unavailable before first unlock; do not permanently disable on that edge. */
    private void initialize(Activity activity, ClassLoader loader) {
        Context context = activity.getApplicationContext();
        UserManager users = context.getSystemService(UserManager.class);
        if (users == null) { fail(new IllegalStateException("User state unavailable")); return; }
        if (!users.isUserUnlocked()) {
            unlockOwner = new WeakReference<>(activity);
            if (unlockReceiver == null) {
                unlockReceiver = new BroadcastReceiver() {
                    @Override public void onReceive(Context ignored, Intent intent) {
                        if (!Intent.ACTION_USER_UNLOCKED.equals(intent.getAction())) return;
                        try {
                            context.unregisterReceiver(this);
                            unlockReceiver = null;
                            Activity current = unlockOwner == null ? null : unlockOwner.get();
                            unlockOwner = null;
                            if (current != null && !current.isFinishing() && !current.isDestroyed()
                                    && !checking && !installed && !failed) initialize(current, loader);
                        } catch (Throwable error) { fail(error); }
                    }
                };
                context.registerReceiver(unlockReceiver, new IntentFilter(Intent.ACTION_USER_UNLOCKED),
                        Context.RECEIVER_NOT_EXPORTED);
                // Unlock may race the first check and receiver registration.
                if (users.isUserUnlocked()) {
                    context.unregisterReceiver(unlockReceiver);
                    unlockReceiver = null;
                    unlockOwner = null;
                    initialize(activity, loader);
                }
            }
            return;
        }
        checking = true;
        main = new Handler(activity.getMainLooper());
        channel = new ModuleChannel(activity);
        // Hash the installed APK off the UI thread. No hook mutates launcher state yet.
        io.execute(() -> {
            try {
                reportState("CHECKING", "Launcher callback and concrete activity onCreate reached", profile);
                ModuleChannel.Settings settings = channel.settings();
                diagnostics = settings.diagnostics;
                log("Module build " + BuildConfig.VERSION_CODE + ": launcher bootstrap reached; settings IPC readable.");
                if (Build.VERSION.SDK_INT != 36 || !"myron".equals(Build.DEVICE)) {
                    reportState("UNSUPPORTED_DEVICE", "Android SDK/device allowlist rejected this runtime", profile);
                    failed = true;
                    return;
                }
                PackageInfo info = activity.getPackageManager().getPackageInfo(Compatibility.PACKAGE, 0);
                profile.putString("version", info.versionName);
                profile.putLong("versionCode", info.getLongVersionCode());
                String digest = info.applicationInfo == null ? "" : Compatibility.digest(new File(info.applicationInfo.sourceDir));
                profile.putString("digest", digest);
                boolean match = info.applicationInfo != null
                        && (info.applicationInfo.splitSourceDirs == null
                            || info.applicationInfo.splitSourceDirs.length == 0)
                        && Compatibility.supported(Build.VERSION.SDK_INT, Build.DEVICE,
                            info.versionName, info.getLongVersionCode(), digest);
                if (!match) {
                    reportState("UNSUPPORTED_APK", "Installed version, digest or split package differs from the analyzed APK", profile);
                    failed = true;
                    return;
                }
                main.post(() -> {
                    try {
                        api = new Api(loader);
                        install();
                        installed = true;
                        state("READY", "Exact APK validated; all launcher hooks installed");
                        log("Exact APK validated; hooks installed. Physical-device validation pending.");
                    } catch (Throwable error) { fail(error); }
                });
            } catch (Throwable error) { main.post(() -> fail(error)); }
        });
    }

    static boolean isHome(Intent intent) {
        return intent != null && HomeIntentPolicy.accepts(intent.getAction(),
                intent.hasCategory(Intent.CATEGORY_HOME), intent.hasExtra("locate_app"),
                intent.getBooleanExtra("is_from_newhome", false),
                intent.getBooleanExtra("ignore_bring_to_front", false));
    }

    private void install() throws Exception {
        hook(api.newIntent, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                try {
                    Activity a = (Activity) p.thisObject;
                    // Save the pre-native focus; do not alter Intent, arguments, result or exception.
                    boolean home = isHome((Intent) p.args[0]);
                    event(home ? "HOME observed; waiting for native handling and current settings" : "Non-HOME or special intent excluded");
                    p.setObjectExtra("homefix.home", home);
                    p.setObjectExtra("homefix.focus", a.hasWindowFocus());
                    if (!home) cancel(a);
                    else {
                        HomeRequest r = begin(a, false);
                        p.setObjectExtra("homefix.request", r);
                    }
                } catch (Throwable error) { fail(error); }
            }
            @Override protected void afterHookedMethod(MethodHookParam p) {
                try {
                    Activity a = (Activity) p.thisObject;
                    if (p.hasThrowable()) { cancel(a); return; }
                    if (!Boolean.TRUE.equals(p.getObjectExtra("homefix.home"))) return;
                    HomeRequest r = requests.get(a);
                    if (r != p.getObjectExtra("homefix.request") || r == null) return;
                    if (Boolean.TRUE.equals(p.getObjectExtra("homefix.focus"))) r.complete(now());
                    queue(a);
                } catch (Throwable error) { fail(error); }
            }
        });
        hook(api.nativeHome, after(p -> {
            HomeRequest r = requests.get((Activity) p.thisObject);
            if (r != null && r.active(now())) r.cleaned = true;
        }));
        // Native successful app-to-HOME completion, in the launcher process (not SystemUI).
        hook(api.gestureStart, after(p -> {
            Activity a = (Activity) p.args[0];
            if (a != null && p.gestureEligible
                    && cancellations.accepts(p.epoch, p.timestamp, now())) {
                legacyGestures.put(p.thisObject, new ModernGesture(a, p.timestamp));
            }
        }));
        hook(api.gestureFinish, after(p -> {
            Activity a = (Activity) api.navLauncher.get(p.thisObject);
            if (a == null) return;
            if (!Boolean.TRUE.equals(p.args[0])) { cancel(a); return; }
            ModernGesture started = legacyGestures.remove(p.thisObject);
            if (started == null || started.owner.get() != a || now() >= started.deadline) return;
            gestureViews.put(a, new WeakReference<>(p.thisObject));
            begin(a, true, started.deadline - HomeRequest.MAX_AGE_MS);
            queue(a);
        }));
        hook(api.gestureAnimating, after(p -> {
            if (Boolean.FALSE.equals(p.args[0])) {
                Activity a = (Activity) api.navLauncher.get(p.thisObject);
                if (a != null) queue(a);
            }
        }));
        hook(api.modernStart, after(p -> {
            Object nav = api.listenerNav.get(p.thisObject);
            Activity a = (Activity) api.navLauncher.get(nav);
            if (p.gestureEligible && a != null
                    && cancellations.accepts(p.epoch, p.timestamp, now())) {
                modernGestures.put(p.thisObject, new ModernGesture(a, p.timestamp));
            }
        }));
        // Cancellation invokes the native end callbacks recursively, so mark it BEFORE native code.
        hook(api.modernCancel, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                Object listener = p.thisObject;
                dispatch(() -> {
                    ModernGesture completion = modernGestures.get(listener);
                    if (completion != null) completion.state.cancel();
                    Activity a = (Activity) api.navLauncher.get(api.listenerNav.get(listener));
                    if (a != null) cancel(a);
                });
            }
        });
        hook(api.modernEnd, after(p -> modernEnd(p.thisObject, false)));
        hook(api.modernSurfaceEnd, after(p -> modernEnd(p.thisObject, true)));
        for (Method finish : new Method[]{api.animationEnd, api.enterComplete}) {
            hook(finish, after(p -> {
                Activity a = (Activity) p.thisObject;
                HomeRequest r = requests.get(a);
                if (r != null) { r.complete(now()); queue(a); }
            }));
        }
        hook(api.transitionComplete, after(p -> queue((Activity) p.thisObject)));
        hook(api.transitionStart, after(p -> {
            if (p.args[0] != api.normal && p.args[0] != api.allApps) cancel((Activity) p.thisObject);
        }));
        hook(api.windowFocus, after(p -> {
            Activity a = (Activity) p.thisObject;
            if (Boolean.TRUE.equals(p.args[0])) queue(a); else cancel(a);
        }));
        hook(api.pause, after(p -> cancel((Activity) p.thisObject)));
        hook(api.destroy, after(p -> cancel((Activity) p.thisObject)));
        hook(api.back, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                // Only discard our HOME work. Native Back arguments, body and results are untouched.
                dispatch(() -> cancel((Activity) p.thisObject));
            }
        });
        hook(Activity.class.getDeclaredMethod("onUserInteraction"), new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (api.base.isInstance(p.thisObject)) dispatch(() -> cancel((Activity) p.thisObject));
            }
        });
    }

    private static final class ModernGesture {
        final WeakReference<Activity> owner;
        final GestureCompletion state = new GestureCompletion();
        final long deadline;
        ModernGesture(Activity activity, long timestamp) {
            owner = new WeakReference<>(activity);
            deadline = timestamp + HomeRequest.MAX_AGE_MS;
        }
    }
    private void modernEnd(Object listener, boolean surface) throws Throwable {
        ModernGesture gesture = modernGestures.get(listener);
        if (gesture == null) return;
        GestureCompletion completion = gesture.state;
        if (now() >= gesture.deadline) { modernGestures.remove(listener); return; }
        if (surface) completion.surfaceEnded = true; else completion.animationEnded = true;
        if (!completion.takeCompleted()) return;
        Object nav = api.listenerNav.get(listener);
        Activity a = (Activity) api.navLauncher.get(nav);
        if (a == null || a != gesture.owner.get()) return;
        legacyGestures.remove(nav);
        // Both native callbacks have finished; no remote surface or icon animation is replaced.
        gestureViews.remove(a);
        begin(a, true, gesture.deadline - HomeRequest.MAX_AGE_MS);
        event("Successful HOME animation completed; waiting for focused correction");
        queue(a);
    }

    private final class Event {
        final Object thisObject;
        final Object[] args;
        final long epoch = cancellations.snapshot();
        final long timestamp = now();
        final boolean gestureEligible;
        Event(XC_MethodHook.MethodHookParam param) throws ReflectiveOperationException {
            thisObject = param.thisObject;
            args = param.args.clone();
            // Capture mutable native navigation classification at the event, before marshalling.
            if (param.method.equals(api.gestureStart)) {
                gestureEligible = !(boolean) api.quickSwitch.invoke(thisObject)
                        && !(boolean) api.closeElement.invoke(thisObject);
            } else if (param.method.equals(api.modernStart)) {
                gestureEligible = !api.closeToElement.getBoolean(thisObject)
                        && !(boolean) api.quickSwitch.invoke(api.listenerNav.get(thisObject));
            } else gestureEligible = false;
        }
    }
    private interface Callback { void run(Event param) throws Throwable; }
    private interface Work { void run() throws Throwable; }
    private void dispatch(Work work) {
        Runnable run = () -> {
            if (failed || correcting) return;
            try { work.run(); } catch (Throwable error) { fail(error); }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) run.run(); else main.post(run);
    }
    private XC_MethodHook after(Callback callback) {
        return new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (failed || p.hasThrowable()) return;
                try {
                    Event event = new Event(p);
                    dispatch(() -> callback.run(event));
                } catch (Throwable error) { dispatch(() -> fail(error)); }
            }
        };
    }
    private void hook(Method method, XC_MethodHook callback) {
        XC_MethodHook.Unhook registered = XposedBridge.hookMethod(method, callback);
        if (registered == null) throw new IllegalStateException("Native hook registration rejected");
        hooks.add(registered);
    }
    private HomeRequest begin(Activity a, boolean completed) {
        return begin(a, completed, now());
    }
    private HomeRequest begin(Activity a, boolean completed, long timestamp) {
        HomeRequest request = new HomeRequest();
        request.begin(timestamp, completed);
        requests.put(a, request);
        return request;
    }
    private void cancel(Activity a) {
        cancellations.invalidate();
        requests.remove(a);
        gestureViews.remove(a);
        // End/surface callbacks from an invalidated animation must never create a new HOME request.
        modernGestures.values().removeIf(gesture -> {
            if (gesture.owner.get() != a) return false;
            gesture.state.cancel();
            return true;
        });
        legacyGestures.values().removeIf(gesture -> gesture.owner.get() == a);
    }
    private static long now() { return SystemClock.uptimeMillis(); }

    private void queue(Activity a) {
        if (failed || correcting) return;
        HomeRequest expected = requests.get(a);
        if (expected == null || !expected.canRequestSettings(now(), a.hasWindowFocus())) return;
        expected.settingsRequested = true;
        WeakReference<Activity> owner = new WeakReference<>(a);
        io.execute(() -> {
            try {
                ModuleChannel.Settings settings = channel.settings();
                diagnostics = settings.diagnostics;
                main.post(() -> {
                    expected.settingsRequested = false;
                    Activity current = owner.get();
                    if (current != null && requests.get(current) == expected) correct(current, expected, settings);
                });
            } catch (Throwable error) {
                main.post(() -> {
                    Activity current = owner.get();
                    if (current != null && requests.get(current) == expected) cancel(current);
                    event("Settings IPC unavailable; this HOME request was left native");
                });
            }
        });
    }

    private void correct(Activity a, HomeRequest r, ModuleChannel.Settings settings) {
        if (failed || correcting || !r.ready(now())) return;
        try {
            if (!settings.initialized || !settings.enabled || a.isFinishing() || a.isDestroyed()) {
                event("Current settings disabled/uninitialized, or activity unavailable"); cancel(a); return;
            }
            if (!a.hasWindowFocus() || (boolean) api.locked.invoke(a)) {
                event("HOME skipped: focus lost or activity locked"); cancel(a); return;
            }
            if ((boolean) api.loading.invoke(a)) { event("HOME waiting: workspace loading"); return; }
            WeakReference<Object> navRef = gestureViews.get(a);
            Object nav = navRef == null ? null : navRef.get();
            if (nav != null && api.navAnimating.getBoolean(nav)) { event("HOME waiting: native gesture animation"); return; }
            // Keep Recents and other states native. Drawer is the only forced state transition.
            boolean normal = (boolean) api.inState.invoke(a, api.normal);
            boolean allApps = (boolean) api.inState.invoke(a, api.allApps);
            if (!normal && !allApps) { event("HOME left native: launcher state is neither NORMAL nor ALL_APPS"); cancel(a); return; }
            correcting = true;
            if (!r.cleaned) {
                r.cleaned = true;
                api.hideApps.invoke(a);
                api.nativeHome.invoke(a);
            }
            if (!(boolean) api.inState.invoke(a, api.normal)) { event("HOME waiting: native drawer state transition"); return; }
            Object workspace = api.workspace.invoke(a);
            if (workspace == null) { cancel(a); return; }
            if (!(boolean) api.defaultShowing.invoke(workspace)) api.moveDefault.invoke(workspace, true);
            cancel(a);
            event("HOME corrected using native launcher methods");
        } catch (Throwable error) { fail(error); }
        finally { correcting = false; }
    }

    private void log(String message) {
        if (!diagnostics || channel == null) return;
        io.execute(() -> {
            try {
                if (channel.settings().diagnostics) XposedBridge.log("HyperOSHomeFix: " + message);
            } catch (Throwable ignored) { /* An unreadable logging switch must stay quiet. */ }
        });
    }
    private void state(String code, String detail) {
        if (channel == null) return;
        Bundle snapshot = new Bundle(profile);
        io.execute(() -> reportState(code, detail, snapshot));
    }
    /** Worker-thread reporting is optional, including the very first bootstrap report. */
    private void reportState(String code, String detail, Bundle snapshot) {
        try { channel.state(code, detail, snapshot); }
        catch (Throwable ignored) { /* Status transport must never fail a native launcher event. */ }
    }
    private void event(String detail) {
        if (!diagnostics || channel == null) return;
        io.execute(() -> {
            try {
                diagnostics = channel.settings().diagnostics;
                if (!diagnostics) return;
                XposedBridge.log("HyperOSHomeFix: " + detail);
                channel.event(detail);
            } catch (Throwable ignored) { /* Optional diagnostics only. */ }
        });
    }
    private void fail(Throwable error) {
        failed = true;
        profile.putString("error", error.getClass().getSimpleName());
        state("ERROR", "Initialization or native hook error disabled this launcher process");
        cancellations.invalidate();
        if (Looper.myLooper() == Looper.getMainLooper()) {
            requests.clear(); gestureViews.clear(); modernGestures.clear(); legacyGestures.clear();
            for (XC_MethodHook.Unhook hook : hooks) {
                try { hook.unhook(); } catch (Throwable ignored) { /* circuit stays open */ }
            }
            hooks.clear();
        }
        // Never log Intent extras, package lists, launcher content, or raw exception messages.
        log("Disabled for this process after " + error.getClass().getSimpleName() + ".");
    }

    private static Method method(Class<?> owner, String name, Class<?> result, Class<?>... args)
            throws ReflectiveOperationException {
        Method method = owner.getDeclaredMethod(name, args);
        if (method.getReturnType() != result) throw new NoSuchMethodException(name);
        method.setAccessible(true);
        return method;
    }
    private static Field field(Class<?> owner, String name, Class<?> type) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        if (field.getType() != type) throw new NoSuchFieldException(name);
        field.setAccessible(true);
        return field;
    }

    /** Resolve every used member before registering any state-changing hook. No guessed overloads. */
    private static final class Api {
        final Method newIntent, hideApps, nativeHome, loading, locked, workspace, defaultShowing,
                moveDefault, inState, gestureFinish, gestureAnimating, animationEnd, enterComplete,
                transitionComplete, transitionStart, windowFocus, pause, destroy, back, gestureStart;
        final Class<?> base;
        final Method modernStart, modernCancel, modernEnd, modernSurfaceEnd, quickSwitch, closeElement;
        final Field navLauncher, navAnimating, listenerNav, closeToElement;
        final Object normal, allApps;
        Api(ClassLoader loader) throws ReflectiveOperationException {
            base = loader.loadClass("com.miui.home.launcher.BaseLauncher");
            Class<?> ws = loader.loadClass("com.miui.home.launcher.Workspace");
            Class<?> state = loader.loadClass("com.miui.home.launcher.LauncherState");
            Class<?> stateInterface = loader.loadClass("com.miui.home.state.api.ILauncherState");
            Class<?> nav = loader.loadClass("com.miui.home.recents.NavStubView");
            Class<?> listener = loader.loadClass("com.miui.home.recents.NavStubView$9");
            Class<?> launcher = loader.loadClass("com.miui.home.launcher.Launcher");
            if (!Activity.class.isAssignableFrom(base) || !base.isAssignableFrom(launcher))
                throw new ClassNotFoundException("Launcher hierarchy");
            newIntent = method(base, "onNewIntent", void.class, Intent.class);
            hideApps = method(base, "hideAppView", void.class);
            nativeHome = method(base, "onNewIntentWhenAlreadyOnHome", void.class);
            loading = method(base, "isWorkspaceLoading", boolean.class);
            locked = method(base, "isActivityLocked", boolean.class);
            workspace = method(base, "getWorkspace", ws);
            inState = method(base, "isInState", boolean.class, state);
            defaultShowing = method(ws, "isDefaultScreenShowing", boolean.class);
            moveDefault = method(ws, "moveToDefaultScreen", void.class, boolean.class);
            gestureFinish = method(nav, "commonFinishAppToHome", void.class, boolean.class);
            gestureStart = method(nav, "commonAnimStartAppToHome", void.class, base);
            gestureAnimating = method(nav, "setIsAnimatingToLauncher", void.class, boolean.class);
            navLauncher = field(nav, "mLauncher", base);
            navAnimating = field(nav, "mIsAnimatingToLauncher", boolean.class);
            quickSwitch = method(nav, "isQuickSwitchMode", boolean.class);
            closeElement = method(nav, "isCloseToElement", boolean.class);
            listenerNav = field(listener, "this$0", nav);
            closeToElement = field(listener, "val$isCloseToElement", boolean.class);
            modernStart = method(listener, "onAnimationStart", void.class, Object.class);
            modernCancel = method(listener, "onAnimationCancel", void.class, Object.class);
            modernEnd = method(listener, "onAnimationEnd", void.class, Object.class);
            modernSurfaceEnd = method(listener, "onSurfaceEnd", void.class, Object.class);
            animationEnd = method(base, "notifyMiuiAnimationEnd", void.class);
            enterComplete = method(base, "onEnterAnimationComplete", void.class);
            transitionComplete = method(base, "onStateTransitionComplete", void.class, stateInterface);
            transitionStart = method(base, "onStateTransitionStart", void.class, stateInterface);
            windowFocus = method(base, "onWindowFocusChanged", void.class, boolean.class);
            pause = method(base, "onPause", void.class);
            destroy = method(base, "onDestroy", void.class);
            back = method(base, "onBackPressed", void.class);
            normal = field(state, "NORMAL", state).get(null);
            allApps = field(state, "ALL_APPS", state).get(null);
            if (normal == null || allApps == null) throw new IllegalStateException("Missing states");
        }
    }
}
