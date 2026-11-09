package com.peek.app.integrations

import com.peek.app.core.notifications.NotificationSnapshot
import com.peek.app.core.notifications.SnapshotMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessengerAdapterTest {

    private val adapter = MessengerAdapter()
    private val pkg = MessengerAdapter.MESSENGER_PACKAGE

    private fun snapshot(
        messages: List<SnapshotMessage> = emptyList(),
        shortcutId: String? = "thread-1",
        title: String? = "Alex",
        text: String? = null,
        conversationTitle: String? = null,
        isGroupConversation: Boolean? = null,
        category: String? = "msg",
        isGroupSummary: Boolean = false,
        isOngoing: Boolean = false,
        tag: String? = null,
        id: Int = 1,
        packageName: String = pkg,
    ) = NotificationSnapshot(
        packageName = packageName,
        postTime = 10_000,
        notificationId = id,
        tag = tag,
        shortcutId = shortcutId,
        category = category,
        isGroupSummary = isGroupSummary,
        isOngoing = isOngoing,
        title = title,
        text = text,
        hasMessagingStyle = messages.isNotEmpty(),
        conversationTitle = conversationTitle,
        isGroupConversation = isGroupConversation,
        messages = messages,
    )

    @Test
    fun `supports Messenger only`() {
        assertTrue(adapter.supports(pkg))
        assertFalse(adapter.supports("com.whatsapp"))
        assertNull(adapter.parse(snapshot(packageName = "com.whatsapp", text = "hi")))
    }

    @Test
    fun `single message notification`() {
        val result = adapter.parse(
            snapshot(messages = listOf(SnapshotMessage("Alex", text = "Hello", timestamp = 5_000)))
        )
        assertNotNull(result)
        result!!
        assertEquals("Alex", result.title)
        assertEquals("$pkg/shortcut:thread-1", result.id)
        assertEquals(1, result.messages.size)
        assertEquals("Hello", result.messages[0].text)
        assertEquals("Alex", result.messages[0].sender)
        assertEquals(5_000, result.messages[0].timestamp)
        assertEquals(10_000, result.updatedAt)
        assertFalse(result.isGroupConversation)
    }

    @Test
    fun `multiple messages from one sender are kept in order`() {
        val result = adapter.parse(
            snapshot(
                messages = listOf(
                    SnapshotMessage("Alex", text = "second", timestamp = 2),
                    SnapshotMessage("Alex", text = "first", timestamp = 1),
                    SnapshotMessage("Alex", text = "third", timestamp = 3),
                )
            )
        )!!
        assertEquals(listOf("first", "second", "third"), result.messages.map { it.text })
    }

    @Test
    fun `group conversation uses conversation title and sender names`() {
        val result = adapter.parse(
            snapshot(
                conversationTitle = "Weekend plans",
                isGroupConversation = true,
                messages = listOf(
                    SnapshotMessage("Alex", text = "Saturday?", timestamp = 1),
                    SnapshotMessage("Sam", text = "Yes", timestamp = 2),
                ),
            )
        )!!
        assertEquals("Weekend plans", result.title)
        assertTrue(result.isGroupConversation)
        assertTrue(result.messages.all { it.isGroupConversation })
        assertEquals(listOf("Alex", "Sam"), result.messages.map { it.sender })
    }

    @Test
    fun `group inferred from multiple senders when flag missing`() {
        val result = adapter.parse(
            snapshot(
                conversationTitle = "Team",
                messages = listOf(
                    SnapshotMessage("Alex", text = "a", timestamp = 1),
                    SnapshotMessage("Sam", text = "b", timestamp = 2),
                ),
            )
        )!!
        assertTrue(result.isGroupConversation)
    }

    @Test
    fun `own replies are labelled as self`() {
        val result = adapter.parse(
            snapshot(
                messages = listOf(
                    SnapshotMessage("Alex", text = "hi", timestamp = 1),
                    SnapshotMessage(null, text = "hey!", timestamp = 2, isFromSelf = true),
                )
            )
        )!!
        val mine = result.messages.last()
        assertTrue(mine.isFromSelf)
        assertEquals(MessengerAdapter.SELF_LABEL, mine.sender)
    }

    @Test
    fun `messages without text are dropped and empty notifications rejected`() {
        val partial = adapter.parse(
            snapshot(
                messages = listOf(
                    SnapshotMessage("Alex", text = null, timestamp = 1),
                    SnapshotMessage("Alex", text = "  ", timestamp = 2),
                    SnapshotMessage("Alex", text = "real", timestamp = 3),
                )
            )
        )!!
        assertEquals(listOf("real"), partial.messages.map { it.text })

        assertNull(adapter.parse(snapshot(messages = listOf(SnapshotMessage("Alex", text = null, timestamp = 1)))))
        assertNull(adapter.parse(snapshot(title = "Alex", text = null)))
        assertNull(adapter.parse(snapshot(title = null, text = "orphan text")))
    }

    @Test
    fun `falls back to title and text without MessagingStyle`() {
        val result = adapter.parse(snapshot(title = "Alex", text = "Plain text message"))!!
        assertEquals("Alex", result.title)
        assertEquals("Plain text message", result.messages.single().text)
        assertEquals("Alex", result.messages.single().sender)
        assertEquals(10_000, result.messages.single().timestamp)
    }

    @Test
    fun `non-message categories and ongoing notifications are rejected`() {
        val msg = listOf(SnapshotMessage("Alex", text = "hi", timestamp = 1))
        assertNull(adapter.parse(snapshot(messages = msg, category = "call")))
        assertNull(adapter.parse(snapshot(messages = msg, category = "progress")))
        assertNull(adapter.parse(snapshot(messages = msg, isOngoing = true)))
        assertNotNull(adapter.parse(snapshot(messages = msg, category = null)))
    }

    @Test
    fun `bundle summary without messages is rejected`() {
        assertNull(adapter.parse(snapshot(isGroupSummary = true, title = "Messenger", text = "3 new messages")))
        val withMessages = adapter.parse(
            snapshot(isGroupSummary = true, messages = listOf(SnapshotMessage("Alex", text = "hi", timestamp = 1)))
        )
        assertNotNull(withMessages)
    }

    @Test
    fun `conversation ids distinguish chats and never equal the package`() {
        val a = adapter.parse(snapshot(shortcutId = "t1", text = "x"))!!
        val b = adapter.parse(snapshot(shortcutId = "t2", text = "x"))!!
        assertNotEquals(a.id, b.id)

        val c = adapter.parse(snapshot(shortcutId = null, tag = "chat-9", id = 4, text = "x"))!!
        val d = adapter.parse(snapshot(shortcutId = null, tag = "chat-9", id = 5, text = "x"))!!
        assertNotEquals(c.id, d.id)
        listOf(a, b, c, d).forEach { assertNotEquals(pkg, it.id) }
    }

    @Test
    fun `message ids are stable and do not contain message text`() {
        val s = snapshot(messages = listOf(SnapshotMessage("Alex", text = "secret words", timestamp = 1)))
        val first = adapter.parse(s)!!.messages.single().id
        val second = adapter.parse(s)!!.messages.single().id
        assertEquals(first, second)
        assertFalse(first.contains("secret"))
    }
}
