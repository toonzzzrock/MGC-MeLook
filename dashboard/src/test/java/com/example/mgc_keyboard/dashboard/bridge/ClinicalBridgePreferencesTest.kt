package com.example.mgc_keyboard.dashboard.bridge

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ClinicalBridgePreferencesTest {

    private val prefs = ClinicalBridgePreferences(ApplicationProvider.getApplicationContext())

    @Test
    fun `default state is disabled with no registration`() = runTest {
        val state = prefs.state.first()

        assertEquals(false, state.enabled)
        assertEquals("", state.serverUrl)
        assertNull(state.deviceToken)
        assertNull(state.pairingCode)
        assertNull(state.lastSyncAtMillis)
    }

    @Test
    fun `setEnabled and setServerUrl round-trip, trimming trailing slash`() = runTest {
        prefs.setEnabled(true)
        prefs.setServerUrl("https://bridge.example.com/")

        val state = prefs.state.first()
        assertEquals(true, state.enabled)
        assertEquals("https://bridge.example.com", state.serverUrl)
    }

    @Test
    fun `setRegistration stores token and pairing code`() = runTest {
        prefs.setRegistration(deviceToken = "tok-123", pairingCode = "ABC123")

        val state = prefs.state.first()
        assertEquals("tok-123", state.deviceToken)
        assertEquals("ABC123", state.pairingCode)
    }

    @Test
    fun `clearRegistration removes token, pairing code and last sync`() = runTest {
        prefs.setRegistration(deviceToken = "tok-123", pairingCode = "ABC123")
        prefs.setLastSyncNow()

        prefs.clearRegistration()

        val state = prefs.state.first()
        assertNull(state.deviceToken)
        assertNull(state.pairingCode)
        assertNull(state.lastSyncAtMillis)
    }
}
