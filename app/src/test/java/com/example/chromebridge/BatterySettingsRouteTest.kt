package com.example.chromebridge

import org.junit.Assert.*
import org.junit.Test

class BatterySettingsRouteTest {
    @Test fun directPageWinsWithoutOpeningFallbacks() {
        var calls = 0
        val selected = BatterySettingsRoute.open { calls++; true }
        assertEquals(BatterySettingsRoute.candidates[0], selected)
        assertEquals(1, calls)
        assertTrue(selected!!.packageUri)
    }
    @Test fun unavailableDirectPageFallsBackToAppInfo() {
        val seen = mutableListOf<String>()
        val selected = BatterySettingsRoute.open { seen.add(it.action); seen.size == 2 }
        assertEquals(BatterySettingsRoute.candidates[1], selected)
        assertEquals(2, seen.size)
        assertTrue(selected!!.packageUri)
    }
    @Test fun settingsFallbackDoesNotAttachPackageUri() {
        val selected = BatterySettingsRoute.open { !it.packageUri }
        assertEquals(BatterySettingsRoute.candidates[2], selected)
    }
    @Test fun noAvailableHandlerReturnsNull() {
        var calls = 0
        assertNull(BatterySettingsRoute.open { calls++; false })
        assertEquals(3, calls)
        assertFalse(BatterySettingsRoute.candidates.any { it.action.contains("REQUEST_IGNORE") })
    }
}
