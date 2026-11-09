package com.peek.app.core.permissions

import android.content.Context
import com.peek.app.core.notifications.NotificationAccessChecker
import com.peek.app.core.overlay.OverlayPermissionChecker

data class PermissionStatus(
    val notificationAccess: Boolean,
    val overlay: Boolean,
    val labAccessibility: Boolean = false,
) {
    val allGranted: Boolean get() = notificationAccess && overlay

    companion object {
        fun read(context: Context) = PermissionStatus(
            notificationAccess = NotificationAccessChecker.isGranted(context),
            overlay = OverlayPermissionChecker.isGranted(context),
            labAccessibility = LabAccess.isServiceEnabled(context),
        )
    }
}
