package com.peek.app.core.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat

object NotificationAccessChecker {
    fun isGranted(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
}
