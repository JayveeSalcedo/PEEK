package com.peek.app.core.permissions

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.peek.app.BuildConfig

/** Status of the Lab-only accessibility service (present only in the "lab" build). */
object LabAccess {
    private const val SERVICE_CLASS = "com.peek.app.lab.PeekAccessibilityService"

    val isLabBuild: Boolean get() = BuildConfig.LAB

    fun isServiceEnabled(context: Context): Boolean {
        if (!isLabBuild) return false
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val ours = ComponentName(context.packageName, SERVICE_CLASS)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == ours }
    }
}
