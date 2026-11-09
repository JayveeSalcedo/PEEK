package com.peek.app.data.model

/**
 * Synthetic conversation for testing the overlay before any real notification has arrived.
 * Clearly labelled so it is never mistaken for captured data.
 */
object SampleConversation {
    const val PACKAGE = "com.peek.app.sample"

    fun create(now: Long = System.currentTimeMillis()): PeekConversation {
        val id = "$PACKAGE/sample"
        fun msg(n: Int, sender: String?, text: String, minutesAgo: Long, self: Boolean = false) =
            PeekMessage(
                id = "sample-$n",
                conversationId = id,
                packageName = PACKAGE,
                sender = sender,
                text = text,
                timestamp = now - minutesAgo * 60_000,
                isGroupConversation = true,
                isFromSelf = self,
            )
        return PeekConversation(
            id = id,
            packageName = PACKAGE,
            appLabel = "Sample",
            title = "Weekend plans",
            isGroupConversation = true,
            updatedAt = now,
            messages = listOf(
                msg(1, "Alex", "Are we still on for Saturday?", 12),
                msg(2, "You", "Yes! 10am at the usual place.", 9, self = true),
                msg(3, "Sam", "I'll bring snacks", 4),
                msg(4, "Alex", "Perfect. See you there 👋", 1),
            ),
        )
    }
}
