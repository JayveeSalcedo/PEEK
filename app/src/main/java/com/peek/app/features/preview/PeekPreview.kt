package com.peek.app.features.preview

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peek.app.data.model.PeekConversation
import com.peek.app.data.model.PeekMessage
import com.peek.app.ui.components.Monogram
import com.peek.app.ui.theme.LocalPeekColors

const val NOTIFICATION_ONLY_NOTE = "Showing messages from notifications only"

/**
 * The full-screen peek, modelled on iOS context-menu previews: the screen behind blurs and
 * dims, the conversation springs up as a card, and a compact action menu sits beneath it.
 *
 * Tapping the backdrop, the Close action or Back dismisses with a reverse animation; [onClosed]
 * fires once the exit animation has finished so the window can be removed.
 */
@Composable
fun PeekOverlayContent(
    conversation: PeekConversation,
    closing: Boolean,
    onOpenApp: (() -> Unit)?,
    onCloseRequested: () -> Unit,
    onClosed: () -> Unit,
) {
    val colors = LocalPeekColors.current
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(Unit) { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
    LaunchedEffect(closing) { if (closing) state.targetState = false }
    LaunchedEffect(state.currentState, state.isIdle) {
        if (state.isIdle && !state.currentState && !state.targetState) onClosed()
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visibleState = state,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(200)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(colors.scrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = "Close preview",
                        onClick = onCloseRequested,
                    )
            )
        }
        AnimatedVisibility(
            visibleState = state,
            modifier = Modifier.align(Alignment.Center).safeDrawingPadding(),
            enter = fadeIn(tween(160)) + scaleIn(
                animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
                initialScale = 0.86f,
            ),
            exit = fadeOut(tween(160)) + scaleOut(tween(180), targetScale = 0.92f),
        ) {
            PeekPreview(conversation, onOpenApp = onOpenApp, onClose = onCloseRequested)
        }
    }
}

/** Preview card plus its action menu, without backdrop or animation. */
@Composable
fun PeekPreview(
    conversation: PeekConversation,
    onOpenApp: (() -> Unit)?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPeekColors.current
    val maxTranscriptHeight = (LocalConfiguration.current.screenHeightDp * 0.5f).dp
    Column(
        modifier.widthIn(max = 400.dp).padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.cell,
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    paneTitle = "Preview of ${conversation.title}"
                    customActions = listOf(CustomAccessibilityAction("Close preview") { onClose(); true })
                },
        ) {
            Column {
                Header(conversation)
                HorizontalDivider(thickness = 0.5.dp, color = colors.separator)
                Transcript(conversation, Modifier.heightIn(max = maxTranscriptHeight))
                Text(
                    NOTIFICATION_ONLY_NOTE,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.tertiaryLabel,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        ActionMenu(Modifier.align(Alignment.Start)) {
            if (onOpenApp != null) {
                MenuRow("Open ${conversation.appLabel}", Icons.AutoMirrored.Filled.ArrowForward, onOpenApp)
                HorizontalDivider(thickness = 0.5.dp, color = colors.separator)
            }
            MenuRow("Close", Icons.Default.Close, onClose)
        }
    }
}

@Composable
private fun Header(conversation: PeekConversation) {
    val colors = LocalPeekColors.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Monogram(conversation.title, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                conversation.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            val count = conversation.messages.size
            Text(
                "${conversation.appLabel} · $count message${if (count == 1) "" else "s"}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.secondaryLabel,
            )
        }
    }
}

private sealed interface TranscriptItem {
    val key: String
    data class Time(override val key: String, val timestamp: Long) : TranscriptItem
    data class Bubble(override val key: String, val message: PeekMessage, val showSender: Boolean) : TranscriptItem
}

private const val TIME_GAP_MILLIS = 15 * 60 * 1000L

private fun transcriptItems(conversation: PeekConversation): List<TranscriptItem> {
    val items = mutableListOf<TranscriptItem>()
    var previous: PeekMessage? = null
    for (message in conversation.messages) {
        val gap = previous == null || message.timestamp - previous.timestamp > TIME_GAP_MILLIS
        if (gap) items += TranscriptItem.Time("t-${message.id}", message.timestamp)
        val senderChanged = gap || previous?.sender != message.sender || previous?.isFromSelf != message.isFromSelf
        items += TranscriptItem.Bubble(
            key = message.id,
            message = message,
            showSender = conversation.isGroupConversation && !message.isFromSelf && senderChanged,
        )
        previous = message
    }
    return items
}

@Composable
private fun Transcript(conversation: PeekConversation, modifier: Modifier = Modifier) {
    val items = remember(conversation) { transcriptItems(conversation) }
    val listState = rememberLazyListState()
    LaunchedEffect(items.size) {
        if (items.isNotEmpty()) listState.scrollToItem(items.lastIndex)
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        items(items, key = { it.key }) { item ->
            when (item) {
                is TranscriptItem.Time -> TimeLabel(item.timestamp)
                is TranscriptItem.Bubble -> MessageBubble(item.message, item.showSender)
            }
        }
    }
}

@Composable
private fun TimeLabel(timestamp: Long) {
    val context = LocalContext.current
    val label = remember(timestamp) { formatTranscriptTime(context, timestamp) }
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = LocalPeekColors.current.secondaryLabel,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp),
    )
}

private fun formatTranscriptTime(context: Context, timestamp: Long): String {
    val time = DateUtils.formatDateTime(context, timestamp, DateUtils.FORMAT_SHOW_TIME)
    return if (DateUtils.isToday(timestamp)) {
        "Today $time"
    } else {
        DateUtils.formatDateTime(
            context, timestamp,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_SHOW_TIME,
        )
    }
}

@Composable
private fun MessageBubble(message: PeekMessage, showSender: Boolean) {
    val colors = LocalPeekColors.current
    val outgoing = message.isFromSelf
    Column(
        Modifier.fillMaxWidth().padding(top = if (showSender) 6.dp else 0.dp),
        horizontalAlignment = if (outgoing) Alignment.End else Alignment.Start,
    ) {
        if (showSender) {
            Text(
                message.sender ?: "Unknown",
                style = MaterialTheme.typography.labelMedium,
                color = colors.secondaryLabel,
                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp),
            )
        }
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (outgoing) colors.outgoingBubble else colors.incomingBubble,
            modifier = Modifier.widthIn(max = 270.dp),
        ) {
            Text(
                message.text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (outgoing) Color.White else colors.label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
private fun ActionMenu(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = LocalPeekColors.current.menu,
        shadowElevation = 16.dp,
        modifier = modifier.width(250.dp),
    ) {
        Column { content() }
    }
}

@Composable
private fun MenuRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    val colors = LocalPeekColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (pressed) colors.cellPressed else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.label, modifier = Modifier.weight(1f))
        Icon(icon, contentDescription = null, tint = colors.label, modifier = Modifier.size(20.dp))
    }
}
