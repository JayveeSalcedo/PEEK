package com.peek.app.integrations

import com.peek.app.core.notifications.NotificationSnapshot
import java.security.MessageDigest

/** Builds stable identifiers from notification metadata. */
object ConversationIds {

    /**
     * Picks the most stable conversation identity the notification exposes:
     *
     * 1. Shortcut ID (Android's conversation identity, used for conversation notifications).
     * 2. Locus ID.
     * 3. Notification tag + ID. Apps replace a notification in place by reusing tag + ID, so
     *    this pair already means "the same thread" to the posting app.
     *
     * The app-level group key is deliberately not used: Messenger-style apps commonly share
     * one group key across every chat, which would merge unrelated conversations.
     * The result is always namespaced by package and never the package name alone.
     */
    fun conversationId(snapshot: NotificationSnapshot): String {
        val pkg = snapshot.packageName
        snapshot.shortcutId?.takeIf { it.isNotBlank() }?.let { return "$pkg/shortcut:$it" }
        snapshot.locusId?.takeIf { it.isNotBlank() }?.let { return "$pkg/locus:$it" }
        return "$pkg/notification:${snapshot.tag.orEmpty()}#${snapshot.notificationId}"
    }

    /**
     * Deterministic message ID. Hashed so that message text never appears in identifiers
     * (identifiers are more likely than content to end up in logs or crash reports).
     */
    fun messageId(
        conversationId: String,
        sender: String?,
        text: String,
        timestamp: Long,
    ): String = sha256("$conversationId\u0000${sender.orEmpty()}\u0000$timestamp\u0000$text")

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .take(32)
}
