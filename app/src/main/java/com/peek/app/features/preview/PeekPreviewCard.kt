package com.peek.app.features.preview

import android.text.format.DateFormat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peek.app.data.model.PeekConversation
import com.peek.app.data.model.PeekMessage
import com.peek.app.ui.theme.senderColor
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.math.abs

/**
 * Floating conversation card. Shows only what notifications exposed and says so.
 *
 * Dismiss with the close button, a horizontal swipe, a tap outside (handled by the overlay
 * window), or the accessibility "Dismiss preview" action.
 */
@Composable
fun PeekPreviewCard(
    conversation: PeekConversation,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.6f).dp
    val dismissThresholdPx = with(density) { 120.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val enter = remember { MutableTransitionState(false).apply { targetState = true } }

    AnimatedVisibility(
        visibleState = enter,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
    ) {
        Card(
            modifier = modifier
                .widthIn(max = 360.dp)
                .padding(16.dp)
                .graphicsLayer {
                    translationX = offsetX.value
                    alpha = 1f - (abs(offsetX.value) / (dismissThresholdPx * 2)).coerceIn(0f, 0.6f)
                }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch { offsetX.snapTo(offsetX.value + delta) }
                    },
                    onDragStopped = {
                        if (abs(offsetX.value) > dismissThresholdPx) {
                            onDismiss()
                        } else {
                            offsetX.animateTo(0f)
                        }
                    },
                )
                .semantics {
                    paneTitle = "PEEK preview: ${conversation.title}"
                    customActions = listOf(
                        CustomAccessibilityAction("Dismiss preview") { onDismiss(); true }
                    )
                },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        ) {
            Column(Modifier.padding(start = 18.dp, end = 6.dp, top = 10.dp, bottom = 14.dp)) {
                Header(conversation, onDismiss)
                HorizontalDivider(Modifier.padding(end = 12.dp, top = 6.dp, bottom = 4.dp))
                MessageList(
                    conversation = conversation,
                    modifier = Modifier.heightIn(max = maxHeight).padding(end = 12.dp),
                )
                Text(
                    text = "Showing messages from notifications only",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, end = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun Header(conversation: PeekConversation, onDismiss: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${conversation.appLabel} · PEEK",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = conversation.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close preview")
        }
    }
}

@Composable
private fun MessageList(conversation: PeekConversation, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    LaunchedEffect(conversation.messages.size) {
        if (conversation.messages.isNotEmpty()) {
            listState.scrollToItem(conversation.messages.lastIndex)
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(conversation.messages, key = { it.id }) { message ->
            MessageRow(message, showSender = conversation.isGroupConversation || message.isFromSelf)
        }
    }
}

@Composable
private fun MessageRow(message: PeekMessage, showSender: Boolean) {
    val context = LocalContext.current
    val time = remember(message.timestamp) {
        DateFormat.getTimeFormat(context).format(Date(message.timestamp))
    }
    val alignment = if (message.isFromSelf) Alignment.End else Alignment.Start
    Column(Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Row(verticalAlignment = Alignment.Bottom) {
            if (showSender || message.sender == null) {
                Text(
                    text = message.sender ?: "Unknown sender",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = senderColor(message.sender),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (message.isFromSelf) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
            modifier = Modifier.padding(top = 2.dp),
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}
