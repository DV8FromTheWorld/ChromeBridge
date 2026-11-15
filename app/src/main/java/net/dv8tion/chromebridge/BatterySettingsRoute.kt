package net.dv8tion.chromebridge

/** Launches settings UI only. Never changes battery settings or requests an exemption. */
object BatterySettingsRoute {
    data class Route(val action: String, val packageUri: Boolean)
    // AOSP exposes this Settings activity, but its action is not a public SDK
    // constant. Treat it as best effort; manufacturers may remove or restrict it.
    val candidates = listOf(
        Route("android.settings.VIEW_ADVANCED_POWER_USAGE_DETAIL", true),
        Route("android.settings.APPLICATION_DETAILS_SETTINGS", true),
        Route("android.settings.SETTINGS", false)
    )
    fun open(tryLaunch: (Route) -> Boolean): Route? = candidates.firstOrNull(tryLaunch)
}
