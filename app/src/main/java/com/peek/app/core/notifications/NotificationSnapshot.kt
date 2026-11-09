package com.peek.app.core.notifications

/**
 * An Android-free copy of the parts of a `StatusBarNotification` that PEEK cares about.
 *
 * [NotificationParser] builds this from the platform object; adapters only ever see the
 * snapshot, which keeps per-app parsing testable on the plain JVM.
 *
 * It intentionally has no field for `PendingIntent`s, actions or remote inputs: PEEK must
 * never retain notification action tokens.
 */
data class NotificationSnapshot(
    val packageName: String,
    val postTime: Long,
    val notificationId: Int = 0,
    val tag: String? = null,
    /** `Notification.getShortcutId()` — the platform's preferred conversation identity. */
    val shortcutId: String? = null,
    /** `Notification.getLocusId()` value. */
    val locusId: String? = null,
    /** App-supplied grouping key (`Notification.getGroup()`). Often shared by all chats. */
    val groupKey: String? = null,
    val category: String? = null,
    val isGroupSummary: Boolean = false,
    val isOngoing: Boolean = false,
    val title: String? = null,
    val text: String? = null,
    val bigText: String? = null,
    /** True when the notification used `MessagingStyle`, even if it had no usable messages. */
    val hasMessagingStyle: Boolean = false,
    val conversationTitle: String? = null,
    /** Null when the notification did not say. */
    val isGroupConversation: Boolean? = null,
    val selfDisplayName: String? = null,
    val messages: List<SnapshotMessage> = emptyList(),
    /** Extras key names only (never values), for structural diagnostics. */
    val extrasKeys: List<String> = emptyList(),
)

data class SnapshotMessage(
    val sender: String?,
    val senderKey: String? = null,
    val text: String?,
    val timestamp: Long,
    val isFromSelf: Boolean = false,
)
