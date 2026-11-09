package com.peek.app.data.model

/**
 * One message as exposed by a notification. PEEK never has more than what the
 * notification carried, so this is not a full chat record.
 */
data class PeekMessage(
    val id: String,
    val conversationId: String,
    val packageName: String,
    /** Display name of the sender, or null when the notification did not expose one. */
    val sender: String?,
    val text: String,
    val timestamp: Long,
    val isGroupConversation: Boolean,
    /** True when the notification marked this message as sent by the device owner. */
    val isFromSelf: Boolean = false,
)
