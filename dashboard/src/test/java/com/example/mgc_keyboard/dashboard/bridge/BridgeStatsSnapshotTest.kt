package com.example.mgc_keyboard.dashboard.bridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BridgeStatsSnapshotTest {

    @Test
    fun `toJson includes all fields when present`() {
        val snapshot = BridgeStatsSnapshot(
            recordedAt = "2026-07-21T10:00:00Z",
            backspaceRate = 0.2f,
            sentimentScore = 0.6f,
            screenTimeMinutes = 45f,
            appVarietyCount = 5,
            keyPresses = 300,
            wordsScored = 60
        )

        val json = snapshot.toJson()

        assertEquals("2026-07-21T10:00:00Z", json.getString("recordedAt"))
        assertEquals(0.2, json.getDouble("backspaceRate"), 0.0001)
        assertEquals(0.6, json.getDouble("sentimentScore"), 0.0001)
        assertEquals(45.0, json.getDouble("screenTimeMinutes"), 0.0001)
        assertEquals(5, json.getInt("appVarietyCount"))
        assertEquals(300, json.getInt("keyPresses"))
        assertEquals(60, json.getInt("wordsScored"))
    }

    @Test
    fun `toJson omits null optional fields`() {
        val snapshot = BridgeStatsSnapshot(
            recordedAt = "2026-07-21T10:00:00Z",
            backspaceRate = null,
            sentimentScore = null,
            screenTimeMinutes = null,
            appVarietyCount = null,
            keyPresses = null,
            wordsScored = null
        )

        val json = snapshot.toJson()

        assertTrue(json.has("recordedAt"))
        assertFalse(json.has("backspaceRate"))
        assertFalse(json.has("sentimentScore"))
        assertFalse(json.has("screenTimeMinutes"))
        assertFalse(json.has("appVarietyCount"))
        assertFalse(json.has("keyPresses"))
        assertFalse(json.has("wordsScored"))
    }
}
