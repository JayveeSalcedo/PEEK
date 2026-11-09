package com.peek.app.data.model

/**
 * A conversation assembled from one or more notifications.
 *
 * [messages] are ordered oldest first and contain only what notifications exposed.
 */
data class PeekConversation(
    /** Stable per-app identifier; always namespaced by package, never the package alone. */
    val id: String,
    val packageName: String,
    /** Human-readable name of the source app, e.g. "Messenger". */
    val appLabel: String,
    val title: String,
    val messages: List<PeekMessage>,
    val updatedAt: Long,
    val isGroupConversation: Boolean = false,
)
