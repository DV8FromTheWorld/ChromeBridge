package com.example.chromebridge

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.widget.*
import java.util.UUID

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val work = BridgeState.isWork(this)
        packageManager.setComponentEnabledSetting(ComponentName(this, RecoveryActivity::class.java),
            if (work) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP)
        val prefs = getSharedPreferences("bridge", MODE_PRIVATE)
        if (!work && prefs.getString("key", "").isNullOrBlank()) {
            prefs.edit().putString("key", UUID.randomUUID().toString().replace("-", "")).apply()
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = (24 * resources.displayMetrics.density).toInt()
            setPadding(p, p, p, p)
        }
        fun label(text: String) { layout.addView(TextView(this).apply { this.text = text; textSize = 17f; setPadding(0, 16, 0, 16) }) }
        fun button(text: String, click: () -> Unit) { layout.addView(Button(this).apply { this.text = text; setOnClickListener { click() } }) }
        fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()
        label("Chrome Bridge • ${if (work) "Work sender" else "Personal listener"}")
        if (work) {
            label("Paste the pairing code from Personal Chrome Bridge. Links stay on this device, but forwarding intentionally moves the link from Work into Personal Chrome.")
            val code = EditText(this).apply { hint = "Pairing code"; setSingleLine(); setText(prefs.getString("key", "")) }
            layout.addView(code)
            button("Save pairing code") {
                val value = code.text.toString().trim().lowercase()
                if (!value.matches(Regex("[0-9a-f]{32}"))) toast("Enter the full 32-character pairing code.")
                else { prefs.edit().putString("key", value).apply(); toast("Paired. Now choose Chrome Bridge as the Work browser.") }
            }
            button("Choose Work default browser") {
                val role = getSystemService(RoleManager::class.java)
                if (role.isRoleAvailable(RoleManager.ROLE_BROWSER)) startActivityForResult(role.createRequestRoleIntent(RoleManager.ROLE_BROWSER), 2)
            }
        } else {
            label("Start this listener only in Personal. Clone the app into Island, then paste this pairing code there and select it as the Work default browser. Leave Chrome as your Personal browser.")
            layout.addView(TextView(this).apply { text = prefs.getString("key", ""); setTextIsSelectable(true); textSize = 16f })
            button("Copy pairing code") {
                getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Chrome Bridge pairing code", prefs.getString("key", "")))
                toast("Copied. Paste into the Work app; if clipboard crossing is blocked, enter it manually.")
            }
            button("1. Allow Display over other apps") {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
            button("2. Allow notifications") {
                if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
                else toast("Notifications are available.")
            }
            button("3. Start listener") {
                if (!Settings.canDrawOverlays(this)) toast("Grant Display over other apps first.")
                else if (BridgeState.start(this)) toast("Listener enabled, including boot recovery. Look for its notification.")
                else toast("Android blocked startup. Reopen the app and try again.")
            }
            button("Stop listener and automatic restart") { BridgeState.stop(this); toast("Stopped. Automatic restart is disabled until you start it again.") }
            button("Battery / app settings") { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) }
            label("Start enables recovery after reboot (after unlock), app updates and ordinary process kills. Stop disables recovery. Force-stop and Restricted battery mode can block automatic recovery: reopen this Personal app or use Work's recovery/share flow. For best reliability, set battery usage to Unrestricted. Only loopback networking is used; device policy/VPN can still block forwarding.")
        }
        setContentView(ScrollView(this).apply { addView(layout) })
    }
    override fun onResume() {
        super.onResume()
        if (!BridgeState.isWork(this) && BridgeState.enabled(this)) BridgeState.start(this)
    }
}
