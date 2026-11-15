package com.example.chromebridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Only system boot/update events, and only after the owner enabled the listener. */
class RestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in listOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)) return
        if (!BridgeState.isWork(context) && BridgeState.enabled(context)) BridgeState.start(context)
    }
}
