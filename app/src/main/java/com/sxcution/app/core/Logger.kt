package com.sxcution.app.core

import android.util.Log

/**
 * Logger wrapper that hides debug traces in release builds.
 * All logging is stripped out in release builds via R8/ProGuard.
 */
object Logger {
    
    /**
     * Debug level logging - only shown in debug builds
     */
    inline fun d(tag: String, msg: () -> String) {
        Log.d(tag, msg())
    }
    
    /**
     * Info level logging - only shown in debug builds
     */
    inline fun i(tag: String, msg: () -> String) {
        Log.i(tag, msg())
    }
    
    /**
     * Warning level logging - only shown in debug builds
     */
    inline fun w(tag: String, msg: () -> String) {
        Log.w(tag, msg())
    }
    
    /**
     * Error level logging - only shown in debug builds
     */
    inline fun e(tag: String, msg: () -> String, t: Throwable? = null) {
        // Keep error logs in release builds for debugging issues
        Log.e(tag, msg(), t)
    }
}
