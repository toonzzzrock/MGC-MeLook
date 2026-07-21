package com.example.mgc_keyboard.statscore

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Persistent breadcrumb log surviving process death (unlike logcat, which drops/rotates).
 * Written to app-private storage so it's readable after a crash via:
 *   adb exec-out run-as com.example.mgc_keyboard cat files/mgc_diag.log
 *
 * One shared file, one lock — every writer (IME process threads, coroutines) must go
 * through [log] rather than opening the file itself, per the no-shared-unlocked-file rule.
 */
object DiagLog {
    private const val FILE_NAME = "mgc_diag.log"
    private const val MAX_BYTES = 512 * 1024L
    private val timeFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
    private val lock = Any()

    @Volatile private var file: File? = null

    fun init(context: Context) {
        if (file != null) return
        synchronized(lock) {
            if (file == null) {
                file = File(context.applicationContext.filesDir, FILE_NAME)
            }
        }
    }

    fun d(tag: String, message: String) = write("D", tag, message)

    fun e(tag: String, message: String, error: Throwable? = null) {
        val full = if (error != null) "$message :: ${error.javaClass.name}: ${error.message}\n${error.stackTraceToString()}" else message
        write("E", tag, full)
    }

    private fun write(level: String, tag: String, message: String) {
        val target = file ?: return
        val line = "${timeFormat.format(Date())} $level/$tag(${Thread.currentThread().name}): $message\n"
        synchronized(lock) {
            try {
                if (target.exists() && target.length() > MAX_BYTES) target.delete()
                target.appendText(line)
            } catch (_: Exception) {
                // Logging must never crash the caller.
            }
        }
    }
}
