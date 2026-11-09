package com.peek.app.data.repository

import com.peek.app.data.model.PeekConversation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-memory cache of conversations. Nothing is written to disk: the cache disappears
 * when the process dies, when the user disables PEEK, or when entries pass [retentionMillis].
 *
 * Notifications are the only source, and `MessagingStyle` notifications usually repeat recent
 * messages, so merging de-duplicates by message ID.
 */
class InMemoryConversationRepository(
    private val clock: () -> Long = System::currentTimeMillis,
    private val retentionMillis: Long = DEFAULT_RETENTION_MILLIS,
    private val maxMessagesPerConversation: Int = 50,
    private val maxConversations: Int = 30,
) : ConversationRepository {

    private val lock = Any()
    private val byId = LinkedHashMap<String, PeekConversation>()
    private val state = MutableStateFlow<List<PeekConversation>>(emptyList())

    override val conversations: StateFlow<List<PeekConversation>> = state.asStateFlow()

    override fun upsert(conversation: PeekConversation) {
        synchronized(lock) {
            val existing = byId[conversation.id]
            byId[conversation.id] = if (existing == null) {
                conversation.copy(
                    messages = conversation.messages
                        .distinctBy { it.id }
                        .sortedBy { it.timestamp }
                        .takeLast(maxMessagesPerConversation)
                )
            } else {
                merge(existing, conversation)
            }
            pruneLocked()
        }
    }

    override fun get(id: String): PeekConversation? = synchronized(lock) {
        pruneLocked()
        byId[id]
    }

    override fun latest(): PeekConversation? = synchronized(lock) {
        pruneLocked()
        state.value.firstOrNull()
    }

    override fun clear() {
        synchronized(lock) {
            byId.clear()
            publishLocked()
        }
    }

    private fun merge(existing: PeekConversation, incoming: PeekConversation): PeekConversation {
        val newer = incoming.updatedAt >= existing.updatedAt
        val messages = (existing.messages + incoming.messages)
            .distinctBy { it.id }
            .sortedBy { it.timestamp }
            .takeLast(maxMessagesPerConversation)
        return existing.copy(
            title = if (newer) incoming.title else existing.title,
            isGroupConversation = if (newer) incoming.isGroupConversation else existing.isGroupConversation,
            messages = messages,
            updatedAt = maxOf(existing.updatedAt, incoming.updatedAt),
        )
    }

    private fun pruneLocked() {
        val cutoff = clock() - retentionMillis
        byId.values.removeAll { it.updatedAt < cutoff }
        if (byId.size > maxConversations) {
            byId.values.sortedBy { it.updatedAt }
                .take(byId.size - maxConversations)
                .forEach { byId.remove(it.id) }
        }
        publishLocked()
    }

    private fun publishLocked() {
        state.value = byId.values.sortedByDescending { it.updatedAt }
    }

    companion object {
        const val DEFAULT_RETENTION_MILLIS: Long = 24L * 60 * 60 * 1000
    }
}
