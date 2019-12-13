package com.desperado.apkreduce.convertwebp.util

/**
 * @author haozhou
 */

object Logger {
    private var gradleLogger: org.gradle.api.logging.Logger? = null

    fun initialize(gradleLogger: org.gradle.api.logging.Logger) {
        Logger.gradleLogger = gradleLogger
    }

    fun i(s: String) {
        if (gradleLogger != null) {
            gradleLogger!!.lifecycle(String.format("WebPAndroidPlugin: %s", s))
        }
    }
}
