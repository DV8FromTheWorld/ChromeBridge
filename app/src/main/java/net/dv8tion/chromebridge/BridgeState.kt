package net.dv8tion.chromebridge

import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.provider.Settings

/** Persistent user intent, not a claim that the service is currently running. */
object BridgeState {
    const val ENABLED = "listener_enabled"
    fun isWork(context: Context) = context.getSystemService(UserManager::class.java).isManagedProfile
    fun enabled(context: Context) = context.getSharedPreferences("bridge", Context.MODE_PRIVATE).getBoolean(ENABLED, false)
    fun setEnabled(context: Context, value: Boolean) {
        context.getSharedPreferences("bridge", Context.MODE_PRIVATE).edit().putBoolean(ENABLED, value).apply()
    }
    fun start(context: Context): Boolean {
        if (isWork(context) || !Settings.canDrawOverlays(context)) return false
        setEnabled(context, true)
        return try {
            context.startForegroundService(Intent(context, BridgeService::class.java))
            true
        } catch (_: RuntimeException) { false }
    }
    fun stop(context: Context) {
        setEnabled(context, false)
        context.stopService(Intent(context, BridgeService::class.java))
    }
}
