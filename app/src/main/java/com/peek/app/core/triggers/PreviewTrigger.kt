package com.peek.app.core.triggers

/**
 * Something that asks PEEK to show a preview. Prototype 0.1 ships the in-app trigger only;
 * a Quick Settings tile or floating handle can implement this later without touching
 * parsing or presentation.
 */
interface PreviewTrigger {
    /**
     * Shows the conversation with [conversationId], or the most recent one when null.
     * [delayMillis] lets the tester switch to another app before the preview appears.
     */
    fun activate(conversationId: String? = null, delayMillis: Long = 0): TriggerResult

    /** Cancels a pending delayed activation. */
    fun cancel()
}

enum class TriggerResult { SHOWN, SCHEDULED, NO_CONVERSATION, NO_OVERLAY_PERMISSION }
