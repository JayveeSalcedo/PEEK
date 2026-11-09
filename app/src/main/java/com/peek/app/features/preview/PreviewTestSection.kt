package com.peek.app.features.preview

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peek.app.data.model.PeekConversation
import com.peek.app.features.SectionCard
import com.peek.app.features.SectionTitle

/** The P0 in-app trigger (spec §13): pick a captured conversation and peek at it. */
@Composable
fun PreviewTestSection(
    conversations: List<PeekConversation>,
    overlayGranted: Boolean,
    notificationAccessGranted: Boolean,
    peekEnabled: Boolean,
    onPeek: (String) -> Unit,
    onPeekDelayed: (String) -> Unit,
    onPeekSample: () -> Unit,
    onPeekSampleDelayed: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Preview")
        SectionCard {
            Text(
                "Captured conversations",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            when {
                !peekEnabled -> Hint("PEEK is turned off. Turn it on in Settings below.")
                !notificationAccessGranted -> Hint("Allow notification access to capture Messenger conversations.")
                conversations.isEmpty() -> Hint(
                    "Nothing yet. Ask someone to message you on Messenger — the conversation " +
                        "appears here as soon as its notification arrives."
                )
            }
            conversations.forEachIndexed { index, conversation ->
                if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                ConversationRow(
                    conversation = conversation,
                    overlayGranted = overlayGranted,
                    onPeek = { onPeek(conversation.id) },
                    onPeekDelayed = { onPeekDelayed(conversation.id) },
                )
            }
        }
        SectionCard {
            Text(
                "Sample preview",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Hint("A made-up conversation for checking the floating card without Messenger.")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onPeekSample, enabled = overlayGranted) {
                    Text("Peek")
                }
                OutlinedButton(onClick = onPeekSampleDelayed, enabled = overlayGranted) {
                    Text("Peek in 5 s")
                }
            }
        }
        if (!overlayGranted) {
            Hint("Previews need \"Display over other apps\".")
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: PeekConversation,
    overlayGranted: Boolean,
    onPeek: () -> Unit,
    onPeekDelayed: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    conversation.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val count = conversation.messages.size
                Text(
                    "${conversation.appLabel} · $count message${if (count == 1) "" else "s"} · " +
                        DateUtils.getRelativeTimeSpanString(conversation.updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPeek, enabled = overlayGranted) { Text("Peek") }
            OutlinedButton(onClick = onPeekDelayed, enabled = overlayGranted) { Text("Peek in 5 s") }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
