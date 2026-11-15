package com.example.chromebridge

import android.app.Activity
import android.content.*
import android.content.pm.CrossProfileApps
import android.os.Bundle
import android.widget.*
import kotlin.concurrent.thread

class LinkCatcherActivity : Activity() {
    private var busy = false
    private var retryOnReturn = false
    private var recoveryLayout: LinearLayout? = null
    private val url get() = intent?.dataString.orEmpty()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BridgeProtocol.validUrl(url)) { finish(); return }
        if (savedInstanceState != null) showRecovery("Forwarding was interrupted. The link may already be open; check before retrying.")
        else forward()
    }
    private fun forward() {
        if (busy) return
        busy = true
        recoveryLayout?.let { layout ->
            for (i in 0 until layout.childCount) if (layout.getChildAt(i) is Button) layout.getChildAt(i).isEnabled = false
        }
        val key = getSharedPreferences("bridge", MODE_PRIVATE).getString("key", "").orEmpty()
        thread(name = "bridge-send") {
            val result = BridgeClient.send(url, key)
            runOnUiThread {
                busy = false
                if (isDestroyed || isFinishing) return@runOnUiThread
                if (result.ok) {
                    getSharedPreferences("bridge", MODE_PRIVATE).edit().putLong("last_success", System.currentTimeMillis()).apply()
                    finish()
                } else showRecovery(result.message)
            }
        }
    }
    override fun onResume() {
        super.onResume()
        if (retryOnReturn) { retryOnReturn = false; forward() }
    }
    private fun showRecovery(message: String) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = (24 * resources.displayMetrics.density).toInt()
            setPadding(p, p, p, p)
            setBackgroundColor(0xff16324f.toInt())
        }
        recoveryLayout = layout
        fun label(value: String) { layout.addView(TextView(this).apply { text = value; setTextColor(0xffffffff.toInt()); textSize = 17f; setPadding(0, 12, 0, 12) }) }
        fun button(value: String, action: () -> Unit) { layout.addView(Button(this).apply { text = value; setOnClickListener { action() } }) }
        label("Chrome Bridge needs a hand")
        label(message)
        label("Your link is still here. Open the Personal listener, then return here to retry automatically. If profile switching is unavailable, share to Personal → Chrome Bridge instead. Your profile policy may block either option.")
        button("Retry forwarding") {
            if (!busy) { (layout.getChildAt(1) as TextView).setText(R.string.retrying); forward() }
        }
        button("Open Personal listener") {
            try {
                val profiles = getSystemService(CrossProfileApps::class.java)
                val target = profiles.targetUserProfiles.singleOrNull()
                if (target == null) Toast.makeText(this, "Personal app is not available for profile switching. Try Share below.", Toast.LENGTH_LONG).show()
                else {
                    profiles.startMainActivity(ComponentName(this, MainActivity::class.java), target)
                    retryOnReturn = true
                }
            } catch (_: RuntimeException) {
                Toast.makeText(this, "Android blocked profile switching. Try Share below or open Personal Chrome Bridge manually.", Toast.LENGTH_LONG).show()
            }
        }
        button("Share through Personal…") {
            try {
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url), "Choose Personal → Chrome Bridge"))
            } catch (_: ActivityNotFoundException) { Toast.makeText(this, "No share handler available. Open Personal Chrome Bridge manually.", Toast.LENGTH_LONG).show() }
        }
        button("Copy link") {
            getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Link", url))
            Toast.makeText(this, "Link copied", Toast.LENGTH_SHORT).show()
        }
        button("Cancel") { finish() }
        setContentView(ScrollView(this).apply { addView(layout) })
    }
}
