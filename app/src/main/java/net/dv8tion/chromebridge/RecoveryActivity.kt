package net.dv8tion.chromebridge

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast

/** User-selected share target; this is not a silent cross-profile permission. */
class RecoveryActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) { finish(); return }
        if (BridgeState.isWork(this)) {
            Toast.makeText(this, "Choose the Personal tab, then Chrome Bridge. This is the Work copy.", Toast.LENGTH_LONG).show()
            finish(); return
        }
        val url = if (intent.action == Intent.ACTION_SEND) intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty() else ""
        if (!BridgeProtocol.validUrl(url)) {
            Toast.makeText(this, "Share one HTTP(S) link to Chrome Bridge.", Toast.LENGTH_LONG).show()
            finish(); return
        }
        val restarted = BridgeState.enabled(this) && BridgeState.start(this)
        if (!restarted) Toast.makeText(this, "Opening link only. The listener remains disabled or needs overlay permission. Open Personal Chrome Bridge to enable it.", Toast.LENGTH_LONG).show()
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage("com.android.chrome")) }
        catch (_: RuntimeException) { Toast.makeText(this, "Personal Chrome is unavailable. Install or enable Chrome.", Toast.LENGTH_LONG).show() }
        finish()
    }
}
