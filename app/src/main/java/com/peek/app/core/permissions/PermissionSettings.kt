package com.peek.app.core.permissions

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.peek.app.core.diagnostics.PeekLog
import com.peek.app.core.notifications.PeekNotificationService

/** Opens the Android Settings screens where the user grants or revokes PEEK's access. */
object PermissionSettings {

    fun openNotificationAccess(context: Context) {
        val component = ComponentName(context, PeekNotificationService::class.java)
        val detail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    component.flattenToString()
                )
        } else {
            null
        }
        // Some OEM builds do not implement the detail screen; fall back to the list.
        launchFirst(
            context,
            listOfNotNull(detail, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)),
        )
    }

    fun openOverlay(context: Context) {
        launchFirst(
            context,
            listOf(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ),
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),
            ),
        )
    }

    fun openAccessibility(context: Context) {
        launchFirst(context, listOf(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
    }

    /** App info screen: on Android 13+ this is where sideloaded builds "Allow restricted settings". */
    fun openAppDetails(context: Context) {
        launchFirst(
            context,
            listOf(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                )
            ),
        )
    }

    private fun launchFirst(context: Context, intents: List<Intent>) {
        for (intent in intents) {
            try {
                if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
                // Try the next option.
            }
        }
        PeekLog.w("no settings activity available")
    }
}
