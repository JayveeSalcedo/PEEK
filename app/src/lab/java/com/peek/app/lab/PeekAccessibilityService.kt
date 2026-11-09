package com.peek.app.lab

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.peek.app.PeekApplication
import com.peek.app.core.diagnostics.PeekLog
import com.peek.app.integrations.ChatTitleMatcher
import com.peek.app.integrations.MessengerAdapter

/**
 * Lab experiment (trigger 4): detect a long-press on a chat row inside Messenger and show the
 * PEEK preview for it.
 *
 * How it works: Messenger reports `TYPE_VIEW_LONG_CLICKED` for the pressed row. The row's
 * visible text (chat name, last message, time) is collected from its accessibility node and
 * compared with titles PEEK already captured from notifications. If one matches, the preview
 * opens on top of Messenger's own long-press menu.
 *
 * Privacy: text read from the screen is used for the comparison and immediately discarded;
 * only counts are kept (see LabStats). The service is limited to Messenger in its config.
 */
class PeekAccessibilityService : AccessibilityService() {

    private val container get() = (application as PeekApplication).container

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_LONG_CLICKED) return
        if (event.packageName?.toString() !in MessengerAdapter.PACKAGES) return
        if (!container.isPeekEnabled) return

        val texts = buildList {
            event.text?.forEach { text -> text?.toString()?.let { add(it) } }
            event.contentDescription?.toString()?.let { add(it) }
            event.source?.let { collectTexts(it, depth = 0, out = this) }
        }
        val match = ChatTitleMatcher.bestMatch(texts, container.repository.conversations.value)

        val stats = container.labStats.value
        container.labStats.value = stats.copy(
            longPressesSeen = stats.longPressesSeen + 1,
            matched = stats.matched + if (match != null) 1 else 0,
            unmatched = stats.unmatched + if (match == null) 1 else 0,
            lastAt = System.currentTimeMillis(),
            lastMatched = match != null,
        )
        PeekLog.d("lab long-press: texts=${texts.size} matched=${match != null}")

        if (match != null) container.previewTrigger.activate(match.id)
    }

    /** Depth-limited walk of the pressed row; rows are shallow, and this bounds the work. */
    private fun collectTexts(node: AccessibilityNodeInfo, depth: Int, out: MutableList<String>) {
        node.text?.toString()?.let(out::add)
        node.contentDescription?.toString()?.let(out::add)
        if (depth >= MAX_DEPTH) return
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectTexts(child, depth + 1, out)
        }
    }

    override fun onInterrupt() = Unit

    private companion object {
        const val MAX_DEPTH = 4
    }
}
