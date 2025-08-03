package com.github.izhangzhihao.rainbow.brackets.lite.util

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger

/**
 * Centralized logging utility for Rainbow Brackets plugin.
 */
object RainbowLogger {
    private val LOG = logger<RainbowLogger>()
    
    // Convenience methods with class context
    inline fun trace(clazz: Any, message: () -> String) = getLogger(clazz).trace(message())
    inline fun debug(clazz: Any, message: () -> String) = getLogger(clazz).debug(message())
    inline fun info(clazz: Any, message: () -> String) = getLogger(clazz).info(message())
    inline fun warn(clazz: Any, message: () -> String) = getLogger(clazz).warn(message())
    inline fun warn(clazz: Any, message: String, t: Throwable) = getLogger(clazz).warn(message, t)
    inline fun error(clazz: Any, message: () -> String) = getLogger(clazz).error(message())
    inline fun error(clazz: Any, message: String, t: Throwable) = getLogger(clazz).error(message, t)
    
    // Helper to get logger for class
    fun getLogger(clazz: Any): Logger {
        val className = when (clazz) {
            is String -> clazz
            is Class<*> -> clazz.name
            else -> clazz.javaClass.name
        }
        return Logger.getInstance(className)
    }
    
    // Plugin-wide logger
    val pluginLogger: Logger get() = LOG
}
