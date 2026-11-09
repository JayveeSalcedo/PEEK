package com.peek.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.peek.app.features.SectionCard
import com.peek.app.features.SectionTitle

@Composable
fun SettingsSection(
    peekEnabled: Boolean,
    onPeekEnabledChange: (Boolean) -> Unit,
    onClearCache: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Settings & privacy")
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("PEEK enabled", fontWeight = FontWeight.Medium)
                    Text(
                        "Turning PEEK off stops capturing and clears every cached preview.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = peekEnabled, onCheckedChange = onPeekEnabledChange)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Previews are kept in memory only — never written to storage or sent anywhere " +
                    "— and expire after 24 hours or when the app process stops.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onClearCache) { Text("Clear cached previews") }
        }
    }
}
