package com.example.chromebridge

import org.junit.Assert.*
import org.junit.Test

class SetupStatusTest {
    private fun snapshot() = SetupStatus.Snapshot(false, true, true, false, true, false, false, true, true, null, false, true)
    @Test fun enabledRestartDoesNotClaimActiveListener() {
        val lines = SetupStatus.lines(snapshot())
        assertTrue(lines.contains("○ Listener is not listening now"))
        assertTrue(lines.contains("✓ Automatic restart enabled"))
    }
    @Test fun notRestrictedDoesNotClaimUnrestricted() {
        val lines = SetupStatus.lines(snapshot())
        assertTrue(lines.contains("✓ Android reports no background restriction"))
        assertFalse(lines.any { it.startsWith("✓") && it.contains("Unrestricted") })
        assertTrue(lines.any { it.contains("cannot be reliably read") })
    }
    @Test fun savedKeyDoesNotClaimPairingVerified() {
        val lines = SetupStatus.lines(snapshot().copy(work = true))
        assertTrue(lines.contains("✓ Pairing code saved locally"))
        assertTrue(lines.any { it.contains("Pairing not yet verified") })
    }
    @Test fun deniedPermissionsAndUnknownCompanionStayExplicit() {
        val lines = SetupStatus.lines(snapshot().copy(overlay = false, notifications = false, companionAvailable = null))
        assertTrue(lines.contains("○ Display over other apps not allowed"))
        assertTrue(lines.contains("○ Notifications disabled or listener channel blocked"))
        assertTrue(lines.contains("• Other-profile availability could not be checked"))
    }
    @Test fun receivedTimestampIsHistoricalNotCurrentProof() {
        val lines = SetupStatus.lines(snapshot().copy(lastVerified = "today"))
        assertTrue(lines.contains("✓ Authenticated link received: today"))
        assertTrue(lines.any { it.contains("not current Work setup") })
    }
}
