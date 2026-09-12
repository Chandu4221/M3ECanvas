package dev.chandradsl.m3ecanvas.util

/**
 * Diagnostic logger for M3E Canvas.
 * Outputs formatted messages with tags to standard streams, ensuring failures
 * and warnings are observable rather than silently swallowed.
 */
object AppLogger {
    var isEnabled: Boolean = true

    fun info(tag: String, message: String) {
        if (isEnabled) {
            println("[$tag] INFO: $message")
        }
    }

    fun warn(tag: String, message: String, throwable: Throwable? = null) {
        if (isEnabled) {
            println("[$tag] WARN: $message")
            throwable?.let { t ->
                println("[$tag] Cause: ${t.message}")
            }
        }
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        if (isEnabled) {
            println("[$tag] ERROR: $message")
            throwable?.let { t ->
                println("[$tag] Stacktrace:")
                t.printStackTrace()
            }
        }
    }
}
