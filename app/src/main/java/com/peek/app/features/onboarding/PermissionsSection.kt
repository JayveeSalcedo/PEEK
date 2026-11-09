package com.peek.app.features.onboarding

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.peek.app.core.permissions.PermissionStatus
import com.peek.app.features.SectionCard
import com.peek.app.features.SectionTitle

/**
 * Explains each permission before sending the user to Android Settings (spec §4).
 * Both stay revocable from here; PEEK keeps working with whatever remains granted.
 */
@Composable
fun PermissionsSection(
    status: PermissionStatus,
    listenerConnected: Boolean,
    onOpenNotificationAccess: () -> Unit,
    onOpenOverlay: () -> Unit,
    onOpenAppDetails: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(if (status.allGranted) "Permissions" else "Set up PEEK")
        PermissionCard(
            title = "Notification access",
            explanation = "PEEK reads supported notification content so it can display " +
                "message previews. It does not access private app databases, and messages " +
                "stay on this device.",
            granted = status.notificationAccess,
            detail = when {
                !status.notificationAccess -> null
                listenerConnected -> "Listening for Messenger notifications."
                else -> "Granted. Waiting for Android to connect the listener…"
            },
            onClick = onOpenNotificationAccess,
        )
        if (!status.notificationAccess && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Text(
                "Option greyed out? Installed outside Google Play, Android may block this " +
                    "setting. Open App info → ⋮ → \"Allow restricted settings\", then try again.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onOpenAppDetails) { Text("Open App info") }
        }
        PermissionCard(
            title = "Display over other apps",
            explanation = "PEEK uses a floating window to show a preview above the current " +
                "application. It never blocks the app underneath.",
            granted = status.overlay,
            detail = null,
            onClick = onOpenOverlay,
        )
    }
}

@Composable
private fun PermissionCard(
    title: String,
    explanation: String,
    granted: Boolean,
    detail: String?,
    onClick: () -> Unit,
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            AssistChip(
                onClick = onClick,
                label = { Text(if (granted) "Allowed" else "Not allowed") },
                colors = AssistChipDefaults.assistChipColors(
                    labelColor = if (granted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                ),
            )
        }
        Text(
            explanation,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (detail != null) {
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        if (granted) {
            OutlinedButton(onClick = onClick) { Text("Manage in Settings") }
        } else {
            Button(onClick = onClick) { Text("Allow in Settings") }
        }
    }
}
