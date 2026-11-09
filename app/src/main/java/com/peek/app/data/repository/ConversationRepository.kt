package com.peek.app.data.repository

import com.peek.app.data.model.PeekConversation
import kotlinx.coroutines.flow.StateFlow

interface ConversationRepository {
    /** Conversations ordered by most recent activity first. */
    val conversations: StateFlow<List<PeekConversation>>

    /** Merges [conversation] into any existing conversation with the same ID. */
    fun upsert(conversation: PeekConversation)

    fun get(id: String): PeekConversation?

    fun latest(): PeekConversation?

    /** Deletes every cached conversation. */
    fun clear()
}
