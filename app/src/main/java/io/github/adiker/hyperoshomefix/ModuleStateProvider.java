/* SPDX-License-Identifier: Apache-2.0 */
package io.github.adiker.hyperoshomefix;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;

/** Narrow IPC: launcher reads switches/reports status; only this app reads the status. */
public final class ModuleStateProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }

    @Override public Bundle call(String method, String argument, Bundle extras) {
        int caller = Binder.getCallingUid();
        int owner = Process.myUid();
        int launcher = launcherUid();
        SharedPreferences settings = getContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        SharedPreferences status = getContext().getSharedPreferences("runtime_status", Context.MODE_PRIVATE);
        if ("settings".equals(method)) {
            if (!ProviderPolicy.mayReadSettings(caller, owner, launcher)) throw new SecurityException("Caller denied");
            Bundle answer = new Bundle();
            answer.putBoolean("initialized", settings.getBoolean("initialized", false));
            answer.putBoolean("enabled", settings.getBoolean("enabled", false));
            answer.putBoolean("diagnostics", settings.getBoolean("diagnostics", false));
            return answer;
        }
        if ("status".equals(method)) {
            if (!ProviderPolicy.mayReadStatus(caller, owner)) throw new SecurityException("Caller denied");
            Bundle answer = new Bundle();
            for (String key : new String[]{"code", "detail", "event", "device", "version", "digest", "error"})
                answer.putString(key, status.getString(key, ""));
            for (String key : new String[]{"build", "sdk", "pid"}) answer.putInt(key, status.getInt(key, 0));
            answer.putLong("versionCode", status.getLong("versionCode", 0));
            answer.putLong("reportedAt", status.getLong("reportedAt", 0));
            answer.putBoolean("firstApplication", status.getBoolean("firstApplication", false));
            return answer;
        }
        if (!ProviderPolicy.mayReport(caller, launcher)) throw new SecurityException("Caller denied");
        if (extras == null) throw new IllegalArgumentException("Missing report");
        if ("event".equals(method)) {
            if (settings.getBoolean("diagnostics", false)) status.edit()
                    .putString("event", limited(extras.getString("detail"))).apply();
            return Bundle.EMPTY;
        }
        if (!"state".equals(method)) throw new IllegalArgumentException("Unknown call");
        SharedPreferences.Editor editor = status.edit();
        // A fixed whitelist; no Intents, stack traces, paths or arbitrary Bundles are persisted.
        for (String key : new String[]{"code", "detail", "device", "version", "digest", "error"})
            editor.putString(key, limited(extras.getString(key)));
        editor.putInt("build", extras.getInt("build"));
        editor.putInt("sdk", extras.getInt("sdk"));
        editor.putInt("pid", Binder.getCallingPid());
        editor.putLong("versionCode", extras.getLong("versionCode"));
        editor.putBoolean("firstApplication", extras.getBoolean("firstApplication"));
        editor.putLong("reportedAt", System.currentTimeMillis());
        editor.putString("event", "");
        editor.apply();
        return Bundle.EMPTY;
    }
    private int launcherUid() {
        try {
            return getContext().getPackageManager().getApplicationInfo(Compatibility.PACKAGE, 0).uid;
        } catch (PackageManager.NameNotFoundException missing) { return -1; }
    }
    private static String limited(String value) {
        if (value == null) return "";
        return value.substring(0, Math.min(value.length(), 240));
    }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        throw new UnsupportedOperationException("Use authorized calls");
    }
    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String where, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String where, String[] args) { throw new UnsupportedOperationException(); }
}
