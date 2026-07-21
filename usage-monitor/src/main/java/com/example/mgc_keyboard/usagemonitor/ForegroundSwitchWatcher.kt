package com.example.mgc_keyboard.usagemonitor

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process

data class UsageWindow(
    val switches: Int,
    val screenTimeMillis: Long,
    val distinctAppCount: Int
)

class ForegroundSwitchWatcher(private val context: Context) {

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Only aggregate COUNTS are returned; packageName never leaves this function (US2-1/US2-2). */
    fun windowSince(timestampMillis: Long): UsageWindow {
        if (!hasUsageAccess()) return UsageWindow(0, 0, 0)

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(timestampMillis, now)

        var switches = 0
        var lastPackage: String? = null
        var lastForegroundAt = 0L
        var screenTimeMillis = 0L
        val distinctApps = HashSet<String>()

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    if (lastPackage != null && lastPackage != event.packageName) switches++
                    lastPackage = event.packageName
                    lastForegroundAt = event.timeStamp
                    distinctApps.add(event.packageName)
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    if (lastForegroundAt > 0) {
                        screenTimeMillis += (event.timeStamp - lastForegroundAt).coerceAtLeast(0)
                        lastForegroundAt = 0L
                    }
                }
            }
        }
        if (lastForegroundAt > 0) {
            screenTimeMillis += (now - lastForegroundAt).coerceAtLeast(0)
        }

        return UsageWindow(switches, screenTimeMillis, distinctApps.size)
    }
}
