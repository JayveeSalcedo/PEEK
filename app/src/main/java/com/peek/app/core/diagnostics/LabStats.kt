package com.peek.app.core.diagnostics

/**
 * Lab (accessibility long-press experiment) counters. Counts only — never the text that was
 * read from the screen.
 */
data class LabStats(
    val longPressesSeen: Int = 0,
    val matched: Int = 0,
    val unmatched: Int = 0,
    val lastAt: Long? = null,
    val lastMatched: Boolean? = null,
)
