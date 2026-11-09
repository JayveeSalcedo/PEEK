package com.peek.app.integrations

import com.peek.app.core.notifications.NotificationSnapshot
import com.peek.app.data.model.PeekConversation

/**
 * Converts one app's notifications into PEEK's normalized model.
 *
 * Adapters receive a [NotificationSnapshot] (extracted from the `StatusBarNotification` by
 * `NotificationParser`) rather than the platform object, so every adapter can be unit-tested
 * against recorded notification structures without a device.
 */
interface MessagingAdapter {
    /** Human-readable app name shown in the preview header. */
    val appLabel: String

    fun supports(packageName: String): Boolean

    /** Returns null for notifications that are unsupported, empty, or not conversations. */
    fun parse(notification: NotificationSnapshot): PeekConversation?
}
