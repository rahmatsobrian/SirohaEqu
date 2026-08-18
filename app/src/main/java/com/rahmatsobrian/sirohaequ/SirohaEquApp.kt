package com.rahmatsobrian.sirohaequ

import android.app.Application
import com.rahmatsobrian.sirohaequ.logging.AppLogger

/**
 * Application entry point.
 *
 * Only responsibility here: wire up the process-wide uncaught exception
 * handler so a crash anywhere in the app is captured by [AppLogger] with
 * full diagnostic context *before* the process dies, instead of only
 * appearing in logcat.
 */
class SirohaEquApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Best-effort synchronous log write. This must not itself throw,
            // or we lose the original crash information entirely.
            try {
                AppLogger.logCrashBlocking(
                    tag = "UncaughtException",
                    message = "Uncaught exception on thread ${thread.name}",
                    throwable = throwable
                )
            } catch (_: Throwable) {
                // Swallow: logging must never mask the original crash.
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
