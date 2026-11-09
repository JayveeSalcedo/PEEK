package com.peek.app.integrations

import com.peek.app.data.model.PeekConversation
import com.peek.app.ui.components.initialsOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatTitleMatcherTest {

    private fun conversation(id: String, title: String) = PeekConversation(
        id = id, packageName = "pkg", appLabel = "App", title = title,
        messages = emptyList(), updatedAt = 0,
    )

    private val alex = conversation("1", "Alex")
    private val alexMorgan = conversation("2", "Alex Morgan")
    private val weekend = conversation("3", "Weekend plans")
    private val all = listOf(alex, alexMorgan, weekend)

    @Test
    fun `exact row text wins`() {
        assertEquals(alex, ChatTitleMatcher.bestMatch(listOf("Alex", "See you there", "2m"), all))
    }

    @Test
    fun `combined row description matches the most specific title`() {
        assertEquals(
            alexMorgan,
            ChatTitleMatcher.bestMatch(listOf("Alex Morgan, Perfect. See you there, 1 min"), all),
        )
    }

    @Test
    fun `matching ignores case and extra whitespace`() {
        assertEquals(weekend, ChatTitleMatcher.bestMatch(listOf("  weekend   PLANS "), all))
    }

    @Test
    fun `no match when nothing captured fits`() {
        assertNull(ChatTitleMatcher.bestMatch(listOf("Jordan"), all))
        assertNull(ChatTitleMatcher.bestMatch(emptyList(), all))
        assertNull(ChatTitleMatcher.bestMatch(listOf("Alex"), emptyList()))
    }

    @Test
    fun `monogram initials`() {
        assertEquals("WP", initialsOf("Weekend plans"))
        assertEquals("A", initialsOf("Alex"))
        assertEquals("AM", initialsOf("Alex Morgan Smith"))
        assertEquals("•", initialsOf("🎉"))
    }
}
