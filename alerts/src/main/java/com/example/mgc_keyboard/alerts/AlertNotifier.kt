package com.example.mgc_keyboard.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/** Posts a local Notification via NotificationManagerCompat. No server/FCM involved. */
class AlertNotifier(private val context: Context) {

    init {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Wellness check-ins",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun notifyBackspaceSpike(rate: Float) {
        notify(
            id = 1,
            title = "Your weekly summary is ready",
            text = "Your typing pace changed vs your baseline — take a look when you have a minute."
        )
    }

    fun notifySentimentDip(hours: Int) {
        notify(
            id = 2,
            title = "Worth a look at your trends",
            text = "Your typing patterns have shifted recently — worth a look at your trends."
        )
    }

    fun notifyUsageAccessRevoked() {
        notify(
            id = 3,
            title = "Usage tracking paused",
            text = "Screen-time and app-diversity tracking is paused because access was turned off. Re-enable it in Settings if you'd like to resume."
        )
    }

    fun notifyWithdrawalSignal(reason: String) {
        val text = when (reason) {
            "app_diversity_drop" -> "You've been using fewer apps than usual lately — take a look at your trends when you have a minute."
            "inactivity_increase" -> "Your phone has been quieter than usual lately — take a look at your trends when you have a minute."
            else -> "Something shifted in your daily patterns — take a look at your trends when you have a minute."
        }
        notify(id = 4, title = "A gentle nudge", text = text)
    }

    private fun notify(id: Int, title: String, text: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private companion object {
        const val CHANNEL_ID = "wellness_checkins"
    }
}
