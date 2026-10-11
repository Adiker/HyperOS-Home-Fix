/* SPDX-License-Identifier: Apache-2.0 */
package io.github.adiker.hyperoshomefix;

import android.app.Activity;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

/** Local settings only. Vector/LSPosed provides preference sharing to the hook. */
public final class MainActivity extends Activity {
    // Framework-redirected settings contain only two switches, never private user data.
    @SuppressLint("WorldReadableFiles")
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        SharedPreferences prefs;
        try {
            // Legacy API 93 shared-preference redirection permits this mode inside the framework.
            prefs = getSharedPreferences("settings", MODE_WORLD_READABLE);
        } catch (SecurityException unavailable) {
            prefs = getSharedPreferences("settings", MODE_PRIVATE);
        }
        final SharedPreferences settings = prefs;
        if (!settings.contains("initialized")) settings.edit().putBoolean("initialized", true)
                .putBoolean("enabled", true).putBoolean("diagnostics", false).apply();
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);
        TextView title = new TextView(this);
        title.setText(R.string.settings_title);
        title.setTextSize(22);
        layout.addView(title);
        addSwitch(layout, settings, "enabled", getString(R.string.module_enabled), true);
        addSwitch(layout, settings, "diagnostics", getString(R.string.diagnostics_enabled), false);
        TextView help = new TextView(this);
        help.setText(getString(R.string.settings_help, Compatibility.VERSION));
        layout.addView(help);
        setContentView(layout);
    }

    private void addSwitch(LinearLayout layout, SharedPreferences prefs, String key,
                           String label, boolean defaultValue) {
        Switch control = new Switch(this);
        control.setText(label);
        control.setChecked(prefs.getBoolean(key, defaultValue));
        control.setOnCheckedChangeListener((button, checked) -> prefs.edit().putBoolean(key, checked).apply());
        layout.addView(control);
    }
}
