package com.peek.app.features.settings

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.peek.app.core.diagnostics.NotificationDiagnostic
import com.peek.app.features.SectionCard
import com.peek.app.features.SectionTitle
import java.util.Date

/**
 * Structure-only inspector for supported notifications (spec §18 step 7: "capture and inspect
 * real Messenger notification structures"). Shows no names or message text.
 */
@Composable
fun DiagnosticsSection(records: List<NotificationDiagnostic>) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Diagnostics")
        SectionCard {
            Text(
                "Structure of recent supported notifications. No message content is recorded.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (records.isEmpty()) {
                Text(
                    "No supported notifications received yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            records.forEachIndexed { index, record ->
                if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Column(Modifier.padding(top = if (index == 0) 8.dp else 0.dp)) {
                    Text(
                        "${DateFormat.getTimeFormat(context).format(Date(record.receivedAt))} · " +
                            record.outcome.name.lowercase(),
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        buildString {
                            append("category=${record.category ?: "none"}  ")
                            append("messagingStyle=${record.hasMessagingStyle}  ")
                            append("messages=${record.messagesWithText}/${record.messageCount}\n")
                            append("shortcut=${record.hasShortcutId}  locus=${record.hasLocusId}  ")
                            append("tag=${record.hasTag}  group=${record.hasGroupKey}\n")
                            append("summary=${record.isGroupSummary}  ")
                            append("groupConversation=${record.isGroupConversation ?: "unknown"}\n")
                            append("keys: ${record.extrasKeys.joinToString()}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
