package com.peek.app.integrations

import com.peek.app.core.notifications.NotificationSnapshot
import com.peek.app.data.model.PeekConversation
import com.peek.app.data.model.PeekMessage

/**
 * Initial Messenger parser.
 *
 * Prefers structured `MessagingStyle` data. Falls back to plain title/text only for
 * non-summary notifications that look like a single incoming message.
 *
 * Messenger's notification format varies by app version, Android version, language and the
 * user's notification settings. Validate every change against real captured structures
 * (see the in-app diagnostics panel) before claiming compatibility.
 */
class MessengerAdapter : MessagingAdapter {

    override val appLabel: String = "Messenger"

    override fun supports(packageName: String): Boolean = packageName in PACKAGES

    override fun parse(notification: NotificationSnapshot): PeekConversation? {
        if (!supports(notification.packageName)) return null
        if (notification.isOngoing) return null
        if (notification.category in EXCLUDED_CATEGORIES) return null

        val conversationId = ConversationIds.conversationId(notification)
        val structured = notification.messages.isNotEmpty()

        // A bundle summary aggregates several chats ("3 new messages"). Its children carry
        // the real content, so only accept a summary when it has structured messages.
        if (notification.isGroupSummary && !structured) return null

        val isGroup = notification.isGroupConversation
            ?: (notification.conversationTitle != null &&
                notification.messages.mapNotNull { it.sender }.distinct().size > 1)

        val messages = if (structured) {
            structuredMessages(notification, conversationId, isGroup)
        } else {
            fallbackMessage(notification, conversationId)
        }
        if (messages.isEmpty()) return null

        val title = conversationTitle(notification, messages) ?: return null

        return PeekConversation(
            id = conversationId,
            packageName = notification.packageName,
            appLabel = appLabel,
            title = title,
            messages = messages,
            updatedAt = maxOf(notification.postTime, messages.maxOf { it.timestamp }),
            isGroupConversation = isGroup,
        )
    }

    private fun structuredMessages(
        notification: NotificationSnapshot,
        conversationId: String,
        isGroup: Boolean,
    ): List<PeekMessage> = notification.messages.mapNotNull { message ->
        val text = message.text?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        val sender = if (message.isFromSelf) {
            notification.selfDisplayName?.takeIf { it.isNotBlank() } ?: SELF_LABEL
        } else {
            message.sender?.trim()?.takeIf { it.isNotEmpty() }
        }
        val timestamp = if (message.timestamp > 0) message.timestamp else notification.postTime
        PeekMessage(
            id = ConversationIds.messageId(conversationId, sender, text, timestamp),
            conversationId = conversationId,
            packageName = notification.packageName,
            sender = sender,
            text = text,
            timestamp = timestamp,
            isGroupConversation = isGroup,
            isFromSelf = message.isFromSelf,
        )
    }.sortedBy { it.timestamp }

    private fun fallbackMessage(
        notification: NotificationSnapshot,
        conversationId: String,
    ): List<PeekMessage> {
        val sender = notification.title?.trim()?.takeIf { it.isNotEmpty() } ?: return emptyList()
        val text = (notification.bigText ?: notification.text)
            ?.trim()?.takeIf { it.isNotEmpty() } ?: return emptyList()
        return listOf(
            PeekMessage(
                id = ConversationIds.messageId(conversationId, sender, text, notification.postTime),
                conversationId = conversationId,
                packageName = notification.packageName,
                sender = sender,
                text = text,
                timestamp = notification.postTime,
                isGroupConversation = false,
            )
        )
    }

    private fun conversationTitle(
        notification: NotificationSnapshot,
        messages: List<PeekMessage>,
    ): String? =
        notification.conversationTitle?.trim()?.takeIf { it.isNotEmpty() }
            ?: notification.title?.trim()?.takeIf { it.isNotEmpty() }
            ?: messages.lastOrNull { !it.isFromSelf && it.sender != null }?.sender
            ?: appLabel

    companion object {
        const val MESSENGER_PACKAGE = "com.facebook.orca"
        val PACKAGES = setOf(MESSENGER_PACKAGE)

        const val SELF_LABEL = "You"

        /**
         * Categories that are never chat messages (calls, uploads, system status, ...).
         * Values mirror `android.app.Notification.CATEGORY_*`.
         */
        val EXCLUDED_CATEGORIES = setOf(
            "call", "service", "progress", "sys", "transport", "alarm", "err",
            "status", "navigation", "stopwatch", "location_sharing", "missed_call",
        )
    }
}
