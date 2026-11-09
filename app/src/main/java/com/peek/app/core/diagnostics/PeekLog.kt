package com.peek.app.core.diagnostics

import android.util.Log
import com.peek.app.BuildConfig

/**
 * The only logger PEEK uses. Callers must pass structural information only — never
 * titles, sender names or message text. Debug output is limited to debug builds.
 */
object PeekLog {
    private const val TAG = "PEEK"

    fun d(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    /** Logs the exception type only; exception messages can echo notification content. */
    fun w(message: String, error: Throwable? = null) {
        Log.w(TAG, if (error == null) message else "$message (${error.javaClass.simpleName})")
    }
}
