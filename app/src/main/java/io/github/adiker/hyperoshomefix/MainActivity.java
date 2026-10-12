/* SPDX-License-Identifier: Apache-2.0 */
package io.github.adiker.hyperoshomefix;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.view.WindowInsets;

/** Private local settings, caller-checked IPC, and last-observed launcher diagnostics. */
public final class MainActivity extends Activity {
    private TextView status;
    private Switch homeFixControl, diagnosticsControl;
    // Hooked only in the module's own app to distinguish loading the class from delivering callbacks.
    public static String frameworkProbe() { return "No framework callback observed in settings app"; }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        final SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
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
        homeFixControl = addSwitch(layout, settings, "enabled", getString(R.string.module_enabled), true);
        diagnosticsControl = addSwitch(layout, settings, "diagnostics", getString(R.string.diagnostics_enabled), false);
        TextView help = new TextView(this);
        help.setText(getString(R.string.settings_help, Compatibility.VERSION));
        layout.addView(help);
        status = new TextView(this);
        status.setTextIsSelectable(true);
        layout.addView(status);
        Button refresh = new Button(this);
        refresh.setText(R.string.refresh_status);
        refresh.setOnClickListener(view -> refreshStatus());
        layout.addView(refresh);
        Button copy = new Button(this);
        copy.setText(R.string.copy_status);
        copy.setOnClickListener(view -> {
            ClipboardManager clipboard = getSystemService(ClipboardManager.class);
            if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText(
                    getString(R.string.runtime_status_title), status.getText()));
        });
        layout.addView(copy);
        ScrollView scroll = new ScrollView(this);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        scroll.addView(layout);
        setContentView(scroll);
    }

    @Override protected void onResume() {
        super.onResume();
        refreshStatus();
    }
    private void refreshStatus() {
        if (status == null) return;
        Bundle report = getContentResolver().call(ModuleChannel.URI, "status", null, null);
        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
        homeFixControl.setChecked(settings.getBoolean("enabled", false));
        diagnosticsControl.setChecked(settings.getBoolean("diagnostics", false));
        String code = report == null ? "" : report.getString("code", "");
        String text = getString(R.string.runtime_status_format, BuildConfig.VERSION_CODE,
                frameworkProbe(), settings.getBoolean("initialized", false),
                settings.getBoolean("enabled", false), settings.getBoolean("diagnostics", false),
                code.isEmpty() ? getString(R.string.no_launcher_report) : code,
                report == null ? "" : report.getString("detail", ""),
                report == null ? 0 : report.getInt("build"),
                report == null ? 0 : report.getInt("pid"),
                report == null ? "" : report.getString("event", ""));
        if (report != null && report.getLong("reportedAt") > 0) {
            text += getString(R.string.profile_status_format,
                    java.text.DateFormat.getDateTimeInstance().format(new java.util.Date(report.getLong("reportedAt"))),
                    report.getInt("sdk"), report.getString("device", ""),
                    report.getString("version", ""), report.getLong("versionCode"),
                    report.getString("digest", ""), report.getString("error", ""),
                    report.getBoolean("firstApplication"));
        }
        status.setText(text);
    }

    private Switch addSwitch(LinearLayout layout, SharedPreferences prefs, String key,
                           String label, boolean defaultValue) {
        Switch control = new Switch(this);
        control.setText(label);
        control.setChecked(prefs.getBoolean(key, defaultValue));
        control.setOnCheckedChangeListener((button, checked) -> prefs.edit().putBoolean(key, checked).apply());
        layout.addView(control);
        return control;
    }
}
