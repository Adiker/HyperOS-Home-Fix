/* SPDX-License-Identifier: Apache-2.0 */
package io.github.adiker.hyperoshomefix;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

/** Worker-thread IPC to the settings app; never read another app's private files. */
final class ModuleChannel {
    static final Uri URI = Uri.parse("content://io.github.adiker.hyperoshomefix.state");
    private final Context context;
    ModuleChannel(Context context) { this.context = context.getApplicationContext(); }

    static final class Settings {
        final boolean initialized, enabled, diagnostics;
        Settings(Bundle bundle) {
            initialized = bundle.getBoolean("initialized", false);
            enabled = bundle.getBoolean("enabled", false);
            diagnostics = bundle.getBoolean("diagnostics", false);
        }
    }
    Settings settings() {
        Bundle answer = context.getContentResolver().call(URI, "settings", null, null);
        if (answer == null) throw new IllegalStateException("Settings provider unavailable");
        return new Settings(answer);
    }
    void state(String code, String detail, Bundle profile) {
        Bundle report = new Bundle(profile);
        report.putString("code", code);
        report.putString("detail", detail);
        context.getContentResolver().call(URI, "state", null, report);
    }
    void event(String detail) {
        Bundle report = new Bundle();
        report.putString("detail", detail);
        context.getContentResolver().call(URI, "event", null, report);
    }
}
