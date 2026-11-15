package com.example.chromebridge

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.CrossProfileApps
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.widget.*
import java.util.UUID

class MainActivity : Activity() {
    private lateinit var statusView: TextView
    private val statusHandler = Handler(Looper.getMainLooper())
    private val statusRefresh = object : Runnable {
        override fun run() { refreshStatus(); statusHandler.postDelayed(this, 1000) }
    }
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
        label("Live setup status • this profile")
        statusView = TextView(this).apply { textSize = 15f; setPadding(0, 8, 0, 20) }
        layout.addView(statusView)
        if (work) {
            label("Paste the pairing code from Personal Chrome Bridge. Links stay on this device, but forwarding intentionally moves the link from Work into Personal Chrome.")
            val code = EditText(this).apply { hint = "Pairing code"; setSingleLine(); setText(prefs.getString("key", "")) }
            layout.addView(code)
            button("Save pairing code") {
                val value = code.text.toString().trim().lowercase()
                if (!value.matches(Regex("[0-9a-f]{32}"))) toast("Enter the full 32-character pairing code.")
                else {
                    val changed = prefs.getString("key", "") != value
                    prefs.edit().putString("key", value).apply()
                    if (changed) prefs.edit().remove("last_success").apply()
                    refreshStatus()
                    toast("Code saved. Open a Work link to verify pairing.")
                }
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
                else try {
                    startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
                } catch (_: ActivityNotFoundException) { toast("Open Android App info → Notifications manually.") }
                  catch (_: SecurityException) { toast("Android blocked notification settings. Open App info manually.") }
            }
            button("3. Start listener") {
                if (!Settings.canDrawOverlays(this)) toast("Grant Display over other apps first.")
                else if (BridgeState.start(this)) toast("Listener enabled, including boot recovery. Look for its notification.")
                else toast("Android blocked startup. Reopen the app and try again.")
            }
            button("Stop listener and automatic restart") { BridgeState.stop(this); toast("Stopped. Automatic restart is disabled until you start it again.") }
            label("Recommended: choose Unrestricted in this app's battery settings (sometimes under Allow background usage). It can improve listener reliability, but may use more battery and cannot override Force stop or device policy. You make the change in Android Settings.")
            button("4. Open battery settings — choose Unrestricted") {
                val route = BatterySettingsRoute.open { candidate ->
                    try {
                        val settingsIntent = Intent(candidate.action)
                        if (candidate.packageUri) settingsIntent.data = Uri.parse("package:$packageName")
                        startActivity(settingsIntent)
                        true
                    } catch (_: ActivityNotFoundException) { false }
                      catch (_: SecurityException) { false }
                }
                when (route?.action) {
                    BatterySettingsRoute.candidates[0].action -> toast("Choose Unrestricted or allow background usage if offered. No setting was changed automatically.")
                    BatterySettingsRoute.candidates[1].action -> toast("Open Battery or App battery usage, then choose Unrestricted if offered.")
                    BatterySettingsRoute.candidates[2].action -> toast("Go to Apps → Chrome Bridge → Battery → Unrestricted if offered.")
                    else -> toast("Open Android Settings → Apps → Chrome Bridge → Battery manually.")
                }
            }
            label("Start enables recovery after reboot (after unlock), app updates and ordinary process kills. Stop disables recovery. Force-stop and Restricted battery mode can block automatic recovery: reopen this Personal app or use Work's recovery/share flow. For best reliability, set battery usage to Unrestricted. Only loopback networking is used; device policy/VPN can still block forwarding.")
        }
        setContentView(ScrollView(this).apply { addView(layout) })
    }
    override fun onResume() {
        super.onResume()
        if (!BridgeState.isWork(this) && BridgeState.enabled(this)) BridgeState.start(this)
        statusHandler.removeCallbacks(statusRefresh)
        statusHandler.post(statusRefresh)
    }
    override fun onPause() {
        statusHandler.removeCallbacks(statusRefresh)
        super.onPause()
    }
    private fun refreshStatus() {
        if (!::statusView.isInitialized) return
        val work = BridgeState.isWork(this)
        val prefs = getSharedPreferences("bridge", MODE_PRIVATE)
        val notifications = getSystemService(NotificationManager::class.java)
        val channel = notifications.getNotificationChannel("bridge")
        val companion = try { getSystemService(CrossProfileApps::class.java).targetUserProfiles.isNotEmpty() } catch (_: RuntimeException) { null }
        val chrome = if (work) false else try { packageManager.getApplicationInfo("com.android.chrome", 0).enabled } catch (_: PackageManager.NameNotFoundException) { false }
        val timestamp = prefs.getLong(if (work) "last_success" else "last_received", 0)
        val verified = if (timestamp > 0) java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(java.util.Date(timestamp)) else null
        statusView.text = SetupStatus.lines(SetupStatus.Snapshot(
            work = work,
            overlay = Settings.canDrawOverlays(this),
            notifications = notifications.areNotificationsEnabled() && channel?.importance != NotificationManager.IMPORTANCE_NONE,
            listening = BridgeService.isListening,
            autoRestart = BridgeState.enabled(this),
            backgroundRestricted = getSystemService(ActivityManager::class.java).isBackgroundRestricted,
            dozeExempt = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName),
            companionAvailable = companion,
            keySaved = prefs.getString("key", "").orEmpty().matches(Regex("[0-9a-f]{32}")),
            lastVerified = verified,
            browserRoleHeld = getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_BROWSER),
            chromeInstalled = chrome
        )).joinToString("\n")
    }
}
