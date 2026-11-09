package com.peek.app.integrations

import com.peek.app.data.model.PeekConversation

/**
 * Lab: matches the text of a long-pressed chat row (as exposed to accessibility services)
 * against captured conversation titles. Prefers exact matches, then rows that start with the
 * title, then rows containing it; ties go to the longest (most specific) title.
 */
object ChatTitleMatcher {

    fun bestMatch(screenTexts: List<String>, conversations: List<PeekConversation>): PeekConversation? {
        val texts = screenTexts.map(::normalize).filter { it.isNotEmpty() }
        if (texts.isEmpty()) return null
        return conversations
            .mapNotNull { conversation ->
                val title = normalize(conversation.title)
                if (title.length < 2) return@mapNotNull null
                val score = texts.maxOf { text ->
                    when {
                        text == title -> 3
                        text.startsWith(title) -> 2
                        text.contains(title) -> 1
                        else -> 0
                    }
                }
                if (score == 0) null else conversation to (score * 1000 + title.length)
            }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun normalize(value: String): String =
        value.lowercase().replace(Regex("\\s+"), " ").trim()
}
