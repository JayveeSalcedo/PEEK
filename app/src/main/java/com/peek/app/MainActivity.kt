package com.peek.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peek.app.core.permissions.PermissionSettings
import com.peek.app.core.permissions.PermissionStatus
import com.peek.app.core.triggers.TriggerResult
import com.peek.app.features.onboarding.PermissionsSection
import com.peek.app.features.preview.PreviewTestSection
import com.peek.app.features.settings.DiagnosticsSection
import com.peek.app.features.settings.SettingsSection
import com.peek.app.ui.theme.PeekTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val container get() = (application as PeekApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PeekTheme {
                PeekHome(container)
            }
        }
    }
}

private const val SWITCH_APPS_DELAY_MS = 5_000L

@Composable
private fun PeekHome(container: AppContainer) {
    val context = LocalContext.current
    var permissions by remember { mutableStateOf(PermissionStatus.read(context)) }
    // Users grant/revoke access in Settings, so re-check every time PEEK returns.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissions = PermissionStatus.read(context)
    }

    val conversations by container.repository.conversations.collectAsStateWithLifecycle()
    val diagnostics by container.diagnostics.records.collectAsStateWithLifecycle()
    val listenerConnected by container.listenerConnected.collectAsStateWithLifecycle()
    val peekEnabled by container.preferences.peekEnabled.collectAsStateWithLifecycle(initialValue = true)

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun report(result: TriggerResult) {
        val message = when (result) {
            TriggerResult.SHOWN -> null
            TriggerResult.SCHEDULED -> "Preview in 5 seconds — switch to another app now."
            TriggerResult.NO_CONVERSATION -> "No conversation available yet."
            TriggerResult.NO_OVERLAY_PERMISSION -> "Allow \"Display over other apps\" first."
        }
        if (message != null) scope.launch { snackbar.showSnackbar(message) }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item { Header() }
            item {
                PermissionsSection(
                    status = permissions,
                    listenerConnected = listenerConnected,
                    onOpenNotificationAccess = { PermissionSettings.openNotificationAccess(context) },
                    onOpenOverlay = { PermissionSettings.openOverlay(context) },
                    onOpenAppDetails = { PermissionSettings.openAppDetails(context) },
                )
            }
            item {
                PreviewTestSection(
                    conversations = conversations,
                    overlayGranted = permissions.overlay,
                    notificationAccessGranted = permissions.notificationAccess,
                    peekEnabled = peekEnabled,
                    onPeek = { id -> report(container.previewTrigger.activate(id)) },
                    onPeekDelayed = { id ->
                        report(container.previewTrigger.activate(id, SWITCH_APPS_DELAY_MS))
                    },
                    onPeekSample = { report(container.previewTrigger.activateSample()) },
                    onPeekSampleDelayed = {
                        report(container.previewTrigger.activateSample(SWITCH_APPS_DELAY_MS))
                    },
                )
            }
            item {
                SettingsSection(
                    peekEnabled = peekEnabled,
                    onPeekEnabledChange = { enabled ->
                        container.appScope.launch { container.preferences.setPeekEnabled(enabled) }
                    },
                    onClearCache = {
                        container.clearCachedData()
                        scope.launch { snackbar.showSnackbar("Cached previews cleared.") }
                    },
                )
            }
            item { DiagnosticsSection(records = diagnostics) }
        }
    }
}

@Composable
private fun Header() {
    Column {
        Text(
            "PEEK",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "See the conversation. Don't open the conversation.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Prototype 0.1 · Messenger",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
