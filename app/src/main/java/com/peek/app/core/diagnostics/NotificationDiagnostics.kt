package com.peek.app.core.diagnostics

import com.peek.app.core.notifications.NotificationSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Structural description of a received notification. Contains no titles, names or message
 * text — only shapes, counts and key names — so it is safe to show and log.
 */
data class NotificationDiagnostic(
    val receivedAt: Long,
    val packageName: String,
    val category: String?,
    val hasMessagingStyle: Boolean,
    val messageCount: Int,
    val messagesWithText: Int,
    val hasShortcutId: Boolean,
    val hasLocusId: Boolean,
    val hasTag: Boolean,
    val hasGroupKey: Boolean,
    val isGroupSummary: Boolean,
    val isGroupConversation: Boolean?,
    val hasTitle: Boolean,
    val hasText: Boolean,
    val extrasKeys: List<String>,
    val outcome: Outcome,
) {
    enum class Outcome { STORED, REJECTED, IGNORED_DISABLED, ERROR }

    /** One-line, content-free summary suitable for logcat. */
    fun toLogLine(): String =
        "pkg=$packageName outcome=$outcome category=$category messagingStyle=$hasMessagingStyle " +
            "messages=$messageCount/$messagesWithText shortcut=$hasShortcutId locus=$hasLocusId " +
            "tag=$hasTag group=$hasGroupKey summary=$isGroupSummary groupConv=$isGroupConversation " +
            "title=$hasTitle text=$hasText keys=${extrasKeys.size}"

    companion object {
        fun from(snapshot: NotificationSnapshot, receivedAt: Long, outcome: Outcome) =
            NotificationDiagnostic(
                receivedAt = receivedAt,
                packageName = snapshot.packageName,
                category = snapshot.category,
                hasMessagingStyle = snapshot.hasMessagingStyle,
                messageCount = snapshot.messages.size,
                messagesWithText = snapshot.messages.count { !it.text.isNullOrBlank() },
                hasShortcutId = !snapshot.shortcutId.isNullOrBlank(),
                hasLocusId = !snapshot.locusId.isNullOrBlank(),
                hasTag = !snapshot.tag.isNullOrBlank(),
                hasGroupKey = !snapshot.groupKey.isNullOrBlank(),
                isGroupSummary = snapshot.isGroupSummary,
                isGroupConversation = snapshot.isGroupConversation,
                hasTitle = !snapshot.title.isNullOrBlank(),
                hasText = !snapshot.text.isNullOrBlank() || !snapshot.bigText.isNullOrBlank(),
                extrasKeys = snapshot.extrasKeys.sorted(),
                outcome = outcome,
            )
    }
}

/** Small in-memory ring buffer of [NotificationDiagnostic]s for the in-app inspector. */
class NotificationDiagnostics(private val capacity: Int = 25) {
    private val state = MutableStateFlow<List<NotificationDiagnostic>>(emptyList())
    val records: StateFlow<List<NotificationDiagnostic>> = state.asStateFlow()

    @Synchronized
    fun record(diagnostic: NotificationDiagnostic) {
        state.value = (listOf(diagnostic) + state.value).take(capacity)
    }

    @Synchronized
    fun clear() {
        state.value = emptyList()
    }
}
