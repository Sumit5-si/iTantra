package com.itantra.app.core.logging

import android.util.Log

/**
 * Controlled logging system for iTantra.
 *
 * SECURITY: NEVER log encryption keys, session secrets, message plaintext,
 * or raw audio data. Violations compromise the security architecture.
 *
 * In production builds, DEBUG-level logs are suppressed.
 */
object ITantraLogger {

    private const val TAG_PREFIX = "iTantra"
    var isDebugEnabled: Boolean = true // Set false for production

    fun d(tag: String, message: String) {
        if (isDebugEnabled) {
            Log.d("$TAG_PREFIX.$tag", message)
        }
    }

    fun i(tag: String, message: String) {
        Log.i("$TAG_PREFIX.$tag", message)
    }

    fun w(tag: String, message: String) {
        Log.w("$TAG_PREFIX.$tag", message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e("$TAG_PREFIX.$tag", message, throwable)
        } else {
            Log.e("$TAG_PREFIX.$tag", message)
        }
    }
}
