package com.peek.app.core.overlay

import android.content.Context
import android.provider.Settings

object OverlayPermissionChecker {
    fun isGranted(context: Context): Boolean = Settings.canDrawOverlays(context)
}
