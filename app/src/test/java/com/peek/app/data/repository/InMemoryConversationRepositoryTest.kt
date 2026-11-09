package com.peek.app.data.repository

import com.peek.app.data.model.PeekConversation
import com.peek.app.data.model.PeekMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InMemoryConversationRepositoryTest {

    private var now = 1_000_000L
    private val repo = InMemoryConversationRepository(
        clock = { now },
        retentionMillis = 60_000,
        maxMessagesPerConversation = 3,
        maxConversations = 2,
    )

    private fun message(conversationId: String, id: String, ts: Long) = PeekMessage(
        id = id, conversationId = conversationId, packageName = "pkg", sender = "A",
        text = "t$id", timestamp = ts, isGroupConversation = false,
    )

    private fun conversation(id: String, title: String, updatedAt: Long, vararg messages: PeekMessage) =
        PeekConversation(
            id = id, packageName = "pkg", appLabel = "App", title = title,
            messages = messages.toList(), updatedAt = updatedAt,
        )

    @Test
    fun `update merges and de-duplicates repeated notification messages`() {
        repo.upsert(conversation("c1", "Old", now, message("c1", "1", now - 2), message("c1", "2", now - 1)))
        repo.upsert(conversation("c1", "New", now + 1, message("c1", "2", now - 1), message("c1", "3", now)))

        val merged = repo.get("c1")!!
        assertEquals(listOf("1", "2", "3"), merged.messages.map { it.id })
        assertEquals("New", merged.title)
        assertEquals(now + 1, merged.updatedAt)
    }

    @Test
    fun `older update does not overwrite newer title`() {
        repo.upsert(conversation("c1", "New", now, message("c1", "2", now)))
        repo.upsert(conversation("c1", "Old", now - 10, message("c1", "1", now - 10)))
        assertEquals("New", repo.get("c1")!!.title)
        assertEquals(listOf("1", "2"), repo.get("c1")!!.messages.map { it.id })
    }

    @Test
    fun `message count is capped to the most recent`() {
        repo.upsert(conversation("c1", "T", now, *(1..5).map { message("c1", "$it", now + it) }.toTypedArray()))
        assertEquals(listOf("3", "4", "5"), repo.get("c1")!!.messages.map { it.id })
    }

    @Test
    fun `several conversations are kept apart and ordered by recency`() {
        repo.upsert(conversation("c1", "One", now - 5, message("c1", "1", now - 5)))
        repo.upsert(conversation("c2", "Two", now, message("c2", "1", now)))
        assertEquals(listOf("c2", "c1"), repo.conversations.value.map { it.id })
        assertEquals("c2", repo.latest()!!.id)
    }

    @Test
    fun `oldest conversation evicted past the limit`() {
        repo.upsert(conversation("c1", "1", now - 3))
        repo.upsert(conversation("c2", "2", now - 2))
        repo.upsert(conversation("c3", "3", now - 1))
        assertEquals(listOf("c3", "c2"), repo.conversations.value.map { it.id })
    }

    @Test
    fun `entries expire after the retention period`() {
        repo.upsert(conversation("c1", "1", now, message("c1", "1", now)))
        now += 60_001
        assertNull(repo.get("c1"))
        assertEquals(emptyList<PeekConversation>(), repo.conversations.value)
    }

    @Test
    fun `clear removes everything`() {
        repo.upsert(conversation("c1", "1", now))
        repo.clear()
        assertNull(repo.latest())
        assertEquals(0, repo.conversations.value.size)
    }
}
