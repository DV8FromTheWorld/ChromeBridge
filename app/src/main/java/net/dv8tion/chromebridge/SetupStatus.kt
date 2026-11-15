package net.dv8tion.chromebridge

/** Labels intentionally separate observable facts from unverified setup claims. */
object SetupStatus {
    data class Snapshot(
        val work: Boolean, val overlay: Boolean, val notifications: Boolean,
        val listening: Boolean, val autoRestart: Boolean, val backgroundRestricted: Boolean,
        val dozeExempt: Boolean, val companionAvailable: Boolean?, val keySaved: Boolean,
        val lastVerified: String?, val browserRoleHeld: Boolean, val chromeInstalled: Boolean
    )
    fun lines(s: Snapshot): List<String> = buildList {
        fun state(ok: Boolean, yes: String, no: String) { add(if (ok) "✓ $yes" else "○ $no") }
        if (s.work) {
            state(s.browserRoleHeld, "Chrome Bridge is this profile's browser", "Choose Chrome Bridge as Work browser")
            state(s.keySaved, "Pairing code saved locally", "Pairing code missing")
            add(if (s.lastVerified == null) "○ Pairing not yet verified: open a Work link" else "✓ Last acknowledged link: ${s.lastVerified}")
            add("• Saved code alone does not prove that Personal matches or is online.")
        } else {
            state(s.overlay, "Display over other apps allowed", "Display over other apps not allowed")
            state(s.notifications, "App and listener notifications allowed", "Notifications disabled or listener channel blocked")
            state(s.listening, "Listener socket is active now", "Listener is not listening now")
            state(s.autoRestart, "Automatic restart enabled", "Automatic restart disabled")
            state(!s.backgroundRestricted, "Android reports no background restriction", "Android reports background restrictions")
            add(if (s.dozeExempt) "✓ Doze battery-optimization exemption reported" else "• Doze battery-optimization exemption not reported")
            add("• Unrestricted vs Optimized cannot be reliably read; verify in Battery settings.")
            state(s.chromeInstalled, "Personal Chrome installed and enabled", "Personal Chrome missing or disabled")
            if (s.browserRoleHeld) add("⚠ Chrome Bridge is your Personal browser; switch Personal default to Chrome.")
            state(s.keySaved, "Pairing code ready", "Pairing code missing")
            add(if (s.lastVerified == null) "○ Pairing not yet tested: open a link from Work" else "✓ Authenticated link received: ${s.lastVerified}")
            add("• A past link verifies that moment, not current Work setup or visible Chrome launch.")
        }
        add(when (s.companionAvailable) {
            true -> "✓ Same app available in another profile"
            false -> "○ Companion unavailable: missing, paused, hidden or restricted"
            null -> "• Other-profile availability could not be checked"
        })
        add("• Other-profile permissions and browser defaults are not readable here.")
    }
}
