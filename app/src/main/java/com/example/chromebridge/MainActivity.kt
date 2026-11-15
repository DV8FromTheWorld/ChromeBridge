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
import android.view.View
import android.view.WindowInsetsController
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.res.Configuration
import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import java.util.UUID

class MainActivity : Activity() {
    private lateinit var statusView: TextView
    private lateinit var headline: TextView
    private lateinit var summary: TextView
    private var overlayStatus: TextView? = null
    private var notificationStatus: TextView? = null
    private var listenerButton: Button? = null
    private var dark = false
    private val ink get() = Color.parseColor(if (dark) "#F3F5FA" else "#172338")
    private val muted get() = Color.parseColor(if (dark) "#BAC5D6" else "#52627A")
    private val accent get() = Color.parseColor(if (dark) "#A9C7FF" else "#235BC7")
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun surface(color: Int, radius: Int = 20) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun text(value: String, size: Float = 15f, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(if (bold) ink else muted)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setLineSpacing(dp(3).toFloat(), 1f)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }
    }
    private fun card(parent: LinearLayout, title: String, caption: String? = null): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(16))
            background = surface(Color.parseColor(if (dark) "#1C293C" else "#FFFFFF"))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) }
        }
        parent.addView(box); box.addView(text(title, 18f, true))
        if (caption != null) box.addView(text(caption))
        return box
    }
    private fun action(parent: LinearLayout, label: String, primary: Boolean = false, click: () -> Unit): Button {
        return Button(this).apply {
            text = label; isAllCaps = false; textSize = 15f; minHeight = dp(52)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setTextColor(if (primary) Color.parseColor(if (dark) "#102449" else "#FFFFFF") else accent)
            background = RippleDrawable(ColorStateList.valueOf(Color.parseColor("#33235BC7")), surface(if (primary) accent else Color.parseColor(if (dark) "#283A54" else "#EDF3FF"), 14), null)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }
            setOnClickListener { click() }; parent.addView(this)
        }
    }
    private fun toast(value: String) = Toast.makeText(this, value, Toast.LENGTH_LONG).show()
    private fun openAppInfo() {
        try { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) }
        catch (_: ActivityNotFoundException) { toast("Open Settings → Apps → Chrome Bridge manually.") }
        catch (_: SecurityException) { toast("Android blocked this shortcut. Open Settings → Apps → Chrome Bridge manually.") }
    }
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
        dark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        actionBar?.hide()
        val backgroundColor = Color.parseColor(if (dark) "#101A29" else "#F2F5FA")
        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor
        val lightBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        window.insetsController?.setSystemBarsAppearance(if (dark) 0 else lightBars, lightBars)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(28)); setBackgroundColor(backgroundColor)
        }
        layout.addView(text("CHROME BRIDGE", 13f, true).apply { setTextColor(accent); letterSpacing = .14f })
        layout.addView(text(if (work) "Your work links.\nYour personal browser." else "A bridge to\nyour personal Chrome.", 28f, true))
        layout.addView(text(if (work) "WORK PROFILE  →  PERSONAL" else "PERSONAL PROFILE  •  ON-DEVICE ONLY", 11f).apply { letterSpacing = .08f })
        val live = card(layout, if (work) "Work sender" else "Personal listener")
        headline = text("Checking status…", 22f, true).apply { setTextColor(accent) }; live.addView(headline)
        summary = text(""); live.addView(summary)
        if (!work) listenerButton = action(live, "Start listener", true) {
            if (BridgeState.enabled(this)) { BridgeState.stop(this); toast("Stopped. Automatic restart is off.") }
            else if (!Settings.canDrawOverlays(this)) toast("Allow Display over other apps in step 2 first.")
            else if (!BridgeState.start(this)) toast("Android blocked startup. Reopen the app and try again.")
            refreshStatus()
        }
        layout.addView(text(if (work) "Connect this profile" else "Set up once", 21f, true).apply { setPadding(0, dp(12), 0, dp(4)) })
        if (work) {
            val pairing = card(layout, "1  Pair with Personal", "Copy the code from Personal Chrome Bridge. Forwarding moves Work links into Personal Chrome, on this device.")
            val code = EditText(this).apply { hint = "32-character pairing code"; setSingleLine(); setTextColor(ink); setHintTextColor(muted); setText(prefs.getString("key", "")) }
            pairing.addView(code)
            action(pairing, "Save pairing code", true) {
                val value = code.text.toString().trim().lowercase()
                if (!value.matches(Regex("[0-9a-f]{32}"))) toast("Enter the full 32-character pairing code.")
                else {
                    val changed = prefs.getString("key", "") != value
                    prefs.edit().putString("key", value).apply()
                    if (changed) prefs.edit().remove("last_success").apply()
                    refreshStatus(); toast("Code saved. Open a Work link to verify pairing.")
                }
            }
            val browser = card(layout, "2  Route Work links", "Choose Chrome Bridge as the browser in this Work profile. Keep Chrome as your Personal default.")
            action(browser, "Choose Work browser") {
                val role = getSystemService(RoleManager::class.java)
                if (role.isRoleAvailable(RoleManager.ROLE_BROWSER)) startActivityForResult(role.createRequestRoleIntent(RoleManager.ROLE_BROWSER), 2)
            }
        } else {
            val restricted = card(layout, "1  Unlock Android settings", "Installed an APK manually? Open App info, tap ⋮, then Allow restricted settings if shown. Return here to finish setup.")
            action(restricted, "Open Chrome Bridge app info") { openAppInfo() }
            restricted.addView(text("Manual check • Android doesn’t expose this setting to the app.", 12f).apply { setPadding(0, dp(12), 0, 0) })
            val permissions = card(layout, "2  Allow the connection", "These permissions let the listener open Chrome and show its running notification.")
            overlayStatus = text(""); permissions.addView(overlayStatus)
            action(permissions, "Display over other apps") {
                try { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) }
                catch (_: ActivityNotFoundException) { openAppInfo() }
                catch (_: SecurityException) { openAppInfo() }
            }
            notificationStatus = text("").apply { setPadding(0, dp(16), 0, 0) }; permissions.addView(notificationStatus)
            action(permissions, "Notification settings") {
                if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
                else try { startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)) }
                catch (_: ActivityNotFoundException) { openAppInfo() }
                catch (_: SecurityException) { openAppInfo() }
            }
            val battery = card(layout, "3  Keep it available", "Choose Unrestricted under Battery or Allow background usage. This can improve reliability but may use more battery.")
            action(battery, "Open battery settings") {
                val route = BatterySettingsRoute.open { candidate ->
                    try {
                        val intent = Intent(candidate.action)
                        if (candidate.packageUri) intent.data = Uri.parse("package:$packageName")
                        startActivity(intent); true
                    } catch (_: ActivityNotFoundException) { false } catch (_: SecurityException) { false }
                }
                if (route == null) toast("Open Settings → Apps → Chrome Bridge → Battery manually.")
                else toast("Choose Unrestricted if offered. No setting was changed automatically.")
            }
            battery.addView(text("Manual check • Unrestricted vs Optimized can’t be verified here.", 12f).apply { setPadding(0, dp(12), 0, 0) })
            val pairing = card(layout, "4  Pair your Work copy", "Clone Chrome Bridge into Island, then paste this code in the Work app and choose it as the Work browser.")
            pairing.addView(text(prefs.getString("key", "").orEmpty(), 14f).apply { typeface = Typeface.MONOSPACE; setTextIsSelectable(true) })
            action(pairing, "Copy pairing code") {
                getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Chrome Bridge pairing code", prefs.getString("key", "")))
                toast("Copied. Paste in Work, or enter manually if clipboard crossing is blocked.")
            }
        }
        val details = card(layout, "Connection details")
        statusView = text("", 13f).apply { visibility = View.GONE }
        action(details, "Show diagnostics") {
            statusView.visibility = if (statusView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            (details.getChildAt(1) as Button).text = if (statusView.visibility == View.VISIBLE) "Hide diagnostics" else "Show diagnostics"
        }
        details.addView(statusView)
        if (!work) details.addView(text("Start enables restart after unlock, updates and ordinary process kills. Force stop or device policy can still block it. Reopen Personal or use Work’s recovery screen to reconnect. Stop disables restart.", 13f).apply { setPadding(0, dp(12), 0, 0) })
        setContentView(ScrollView(this).apply { isFillViewport = true; addView(layout) })
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
        if (::headline.isInitialized) {
            val listening = BridgeService.isListening
            headline.text = if (work) { if (verified != null) "Link acknowledged" else "Ready to pair" }
                else if (listening) "Listening" else if (BridgeState.enabled(this)) "Reconnecting" else "Not running"
            summary.text = if (work) { if (verified != null) "Last verified $verified. Open a Work link to test again." else "Save your code, then open a Work link to test the connection." }
                else if (listening) "Ready to receive Work links. Keep Chrome as your Personal browser."
                else if (BridgeState.enabled(this)) "Restart is enabled, but the socket is not listening yet. Check permissions below."
                else "Complete the steps below, then start the listener."
            listenerButton?.text = if (BridgeState.enabled(this)) "Stop listener" else "Start listener"
            overlayStatus?.text = if (Settings.canDrawOverlays(this)) "✓  Display over apps allowed" else "○  Display over apps needed"
            notificationStatus?.text = if (notifications.areNotificationsEnabled() && channel?.importance != NotificationManager.IMPORTANCE_NONE) "✓  Notifications allowed" else "○  Notifications needed"
        }
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
