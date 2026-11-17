package com.example.eschedule.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Tiny on-device error log shown under Settings → Developer → Error log.
 *
 * Keeps only the most recent [MAX_LINES] lines (older ones are dropped), so the file can never grow
 * without bound. Uncaught crashes are recorded automatically; other code calls [e] for failures
 * that are handled but worth knowing about.
 */
object AppLog {
    const val MAX_LINES = 1000

    private val lock = Any()
    private var file: File? = null
    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private val _lines = MutableStateFlow<List<String>>(emptyList())

    /** Oldest first, newest last. */
    val lines: StateFlow<List<String>> = _lines

    fun init(context: Context) {
        synchronized(lock) {
            val f = File(context.applicationContext.filesDir, "error.log")
            file = f
            _lines.value = if (f.exists()) f.readLines().takeLast(MAX_LINES) else emptyList()
        }

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            e("Crash", "Uncaught exception on thread ${thread.name}", throwable)
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** Records an error line (plus the first lines of the stack trace, when there is one). */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        val time = LocalDateTime.now().format(stamp)
        val newLines = buildList {
            add("$time  [$tag] $message")
            if (throwable != null) {
                Log.getStackTraceString(throwable).lineSequence()
                    .filter { it.isNotBlank() }
                    .take(12)
                    .forEach { add("    $it") }
            }
        }
        synchronized(lock) {
            val updated = (_lines.value + newLines).takeLast(MAX_LINES)
            _lines.value = updated
            runCatching { file?.writeText(updated.joinToString("\n")) }
        }
    }

    fun clear() {
        synchronized(lock) {
            _lines.value = emptyList()
            runCatching { file?.delete() }
        }
    }
}
