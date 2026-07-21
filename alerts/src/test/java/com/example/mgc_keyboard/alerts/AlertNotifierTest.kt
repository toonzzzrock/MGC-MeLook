package com.example.mgc_keyboard.alerts

import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AlertNotifierTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val notifier = AlertNotifier(context)

    @Test
    fun `creates the wellness check-ins notification channel on init`() {
        val channel = manager.getNotificationChannel("wellness_checkins")

        assertEquals("Wellness check-ins", channel.name)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channel.importance)
    }

    @Test
    fun `notifyBackspaceSpike posts notification id 1`() {
        notifier.notifyBackspaceSpike(0.5f)

        val notification = shadowOf(manager).getNotification(1)
        assertEquals("Your weekly summary is ready", notification.extras.getString(android.app.Notification.EXTRA_TITLE))
    }

    @Test
    fun `notifySentimentDip posts notification id 2`() {
        notifier.notifySentimentDip(3)

        val notification = shadowOf(manager).getNotification(2)
        assertEquals("Worth a look at your trends", notification.extras.getString(android.app.Notification.EXTRA_TITLE))
    }

    @Test
    fun `notifyUsageAccessRevoked posts notification id 3`() {
        notifier.notifyUsageAccessRevoked()

        val notification = shadowOf(manager).getNotification(3)
        assertEquals(
            "Usage tracking paused",
            notification.extras.getString(android.app.Notification.EXTRA_TITLE)
        )
    }

    @Test
    fun `notifyWithdrawalSignal posts a reason-specific message for app_diversity_drop`() {
        notifier.notifyWithdrawalSignal("app_diversity_drop")

        val notification = shadowOf(manager).getNotification(4)
        assertEquals(
            "You've been using fewer apps than usual lately — take a look at your trends when you have a minute.",
            notification.extras.getString(android.app.Notification.EXTRA_TEXT)
        )
    }

    @Test
    fun `notifyWithdrawalSignal posts a reason-specific message for inactivity_increase`() {
        notifier.notifyWithdrawalSignal("inactivity_increase")

        val notification = shadowOf(manager).getNotification(4)
        assertEquals(
            "Your phone has been quieter than usual lately — take a look at your trends when you have a minute.",
            notification.extras.getString(android.app.Notification.EXTRA_TEXT)
        )
    }

    @Test
    fun `notifyWithdrawalSignal falls back to a generic message for an unknown reason`() {
        notifier.notifyWithdrawalSignal("something_else")

        val notification = shadowOf(manager).getNotification(4)
        assertEquals(
            "Something shifted in your daily patterns — take a look at your trends when you have a minute.",
            notification.extras.getString(android.app.Notification.EXTRA_TEXT)
        )
    }

    @Test
    fun `no notification is posted before any notify call`() {
        assertNull(shadowOf(manager).getNotification(1))
    }
}
