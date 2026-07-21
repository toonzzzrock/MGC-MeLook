package com.example.mgc_keyboard

import android.app.Application
import com.example.mgc_keyboard.statscore.DiagLog

/** Installs a persistent-log uncaught-exception hook so app/IME-process crashes are still
 * inspectable after the fact (adb logcat's buffer wraps and drops crashes that happened
 * a while ago). See DiagLog for the file location. */
class MGCApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DiagLog.init(this)

        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            DiagLog.e("UncaughtException", "thread=${thread.name}", throwable)
            previousHandler?.uncaughtException(thread, throwable)
        }
    }
}
