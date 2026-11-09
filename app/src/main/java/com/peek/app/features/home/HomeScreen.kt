package com.peek.app.features.home

import android.os.Build
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peek.app.AppContainer
import com.peek.app.core.diagnostics.NotificationDiagnostic
import com.peek.app.core.permissions.LabAccess
import com.peek.app.core.permissions.PermissionSettings
import com.peek.app.core.permissions.PermissionStatus
import com.peek.app.core.triggers.TileRequests
import com.peek.app.core.triggers.TriggerResult
import com.peek.app.data.model.PeekConversation
import com.peek.app.ui.components.GroupedRow
import com.peek.app.ui.components.GroupedSection
import com.peek.app.ui.components.IconTile
import com.peek.app.ui.components.LargeTitle
import com.peek.app.ui.components.Monogram
import com.peek.app.ui.components.PeekSwitch
import com.peek.app.ui.components.RowValue
import com.peek.app.ui.theme.LocalPeekColors
import kotlinx.coroutines.launch
import java.util.Date

private const val SWITCH_APPS_DELAY_MS = 5_000L

@Composable
fun HomeScreen(container: AppContainer) {
    val context = LocalContext.current
    val colors = LocalPeekColors.current
    val haptics = LocalHapticFeedback.current

    var permissions by remember { mutableStateOf(PermissionStatus.read(context)) }
    // Users grant/revoke access in Settings, so re-check every time PEEK returns.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissions = PermissionStatus.read(context)
    }

    val conversations by container.repository.conversations.collectAsStateWithLifecycle()
    val diagnostics by container.diagnostics.records.collectAsStateWithLifecycle()
    val listenerConnected by container.listenerConnected.collectAsStateWithLifecycle()
    val labStats by container.labStats.collectAsStateWithLifecycle()
    val peekEnabled by container.preferences.peekEnabled.collectAsStateWithLifecycle(initialValue = true)
    val edgeHandle by container.preferences.edgeHandleEnabled.collectAsStateWithLifecycle(initialValue = false)
    var showDiagnostics by rememberSaveable { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun toast(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }
    fun report(result: TriggerResult) {
        when (result) {
            TriggerResult.SHOWN -> Unit
            TriggerResult.SCHEDULED -> toast("Peeking in 5 seconds — switch to another app.")
            TriggerResult.NO_CONVERSATION -> toast("No conversation available yet.")
            TriggerResult.NO_OVERLAY_PERMISSION -> toast("Allow “Display over other apps” first.")
        }
    }

    Scaffold(
        containerColor = colors.groupedBackground,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(colors.groupedBackground),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 32.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            item {
                LargeTitle(
                    title = if (LabAccess.isLabBuild) "PEEK Lab" else "PEEK",
                    subtitle = "See the conversation. Don’t open the conversation.",
                )
            }

            // Permissions — each explained before the user is sent to Settings (spec §4).
            item {
                GroupedSection(
                    header = if (permissions.allGranted) "Access" else "Set up",
                    footer = "PEEK reads supported notification content to build previews, and " +
                        "draws the preview above the app you’re using. It never reads app " +
                        "databases, and messages stay on this device.",
                ) {
                    GroupedRow(
                        title = "Notification access",
                        subtitle = when {
                            !permissions.notificationAccess -> "Needed to see Messenger messages"
                            listenerConnected -> "Listening for Messenger"
                            else -> "Waiting for Android to connect…"
                        },
                        leading = { Glyph(Icons.Default.Notifications, colors.red) },
                        trailing = { OnOff(permissions.notificationAccess) },
                        showChevron = true,
                        onClick = { PermissionSettings.openNotificationAccess(context) },
                    )
                    GroupedRow(
                        title = "Display over other apps",
                        subtitle = if (permissions.overlay) null else "Needed to show previews",
                        leading = { Glyph(Icons.Default.Menu, colors.tint) },
                        trailing = { OnOff(permissions.overlay) },
                        showChevron = true,
                        showDivider = !permissions.notificationAccess && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                        onClick = { PermissionSettings.openOverlay(context) },
                    )
                    if (!permissions.notificationAccess && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        GroupedRow(
                            title = "Switch greyed out?",
                            subtitle = "App info → ⋮ → Allow restricted settings",
                            titleColor = colors.tint,
                            showDivider = false,
                            onClick = { PermissionSettings.openAppDetails(context) },
                        )
                    }
                }
            }

            item {
                ConversationsSection(
                    conversations = conversations,
                    enabled = peekEnabled,
                    notificationAccess = permissions.notificationAccess,
                    onPeek = { report(container.previewTrigger.activate(it)) },
                    onLongPeek = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        report(container.previewTrigger.activate(it))
                    },
                    onPeekLater = { report(container.previewTrigger.activate(it, SWITCH_APPS_DELAY_MS)) },
                )
            }

            // The ways to peek from outside PEEK.
            item {
                GroupedSection(
                    header = "Ways to peek",
                    footer = "Long-press PEEK’s icon on your home screen to see recent chats. " +
                        "The edge handle sits on the right edge of every app: swipe it inward " +
                        "to peek, or drag it up and down to move it.",
                ) {
                    GroupedRow(
                        title = "Home screen icon",
                        subtitle = if (conversations.isEmpty()) "Recent chats appear here" else "${minOf(conversations.size, 4)} chats on long-press",
                        leading = { Glyph(Icons.Default.Home, colors.green) },
                    )
                    GroupedRow(
                        title = "Quick Settings tile",
                        subtitle = if (TileRequests.canRequest) null else "Edit Quick Settings and drag PEEK in",
                        leading = { Glyph(Icons.Default.Settings, Color(0xFF8E8E93)) },
                        trailing = { if (TileRequests.canRequest) RowValue("Add", colors.tint) },
                        onClick = if (TileRequests.canRequest) {
                            { TileRequests.requestAdd(context) { added -> if (added) toast("PEEK tile added.") } }
                        } else null,
                    )
                    GroupedRow(
                        title = "Edge handle",
                        leading = { Glyph(Icons.Default.Face, Color(0xFFFF9500)) },
                        trailing = {
                            PeekSwitch(
                                checked = edgeHandle,
                                onCheckedChange = { on ->
                                    if (on && !permissions.overlay) {
                                        toast("Allow “Display over other apps” first.")
                                    } else {
                                        container.appScope.launch { container.preferences.setEdgeHandleEnabled(on) }
                                    }
                                },
                            )
                        },
                        showDivider = LabAccess.isLabBuild,
                    )
                    if (LabAccess.isLabBuild) {
                        GroupedRow(
                            title = "Long-press in Messenger",
                            subtitle = "Experimental · Accessibility",
                            leading = { Glyph(Icons.Default.Star, Color(0xFFAF52DE)) },
                            trailing = { OnOff(permissions.labAccessibility) },
                            showChevron = true,
                            showDivider = false,
                            onClick = { PermissionSettings.openAccessibility(context) },
                        )
                    }
                }
            }

            if (LabAccess.isLabBuild) {
                item {
                    GroupedSection(
                        header = "Lab results",
                        footer = "Lab builds test whether long-pressing a chat inside Messenger can " +
                            "be detected. This uses an Accessibility Service, which Google Play " +
                            "doesn’t allow for this purpose, so it will never ship in the store build. " +
                            "Only counts are recorded — never what’s on screen.",
                    ) {
                        GroupedRow(title = "Long-presses seen", trailing = { RowValue("${labStats.longPressesSeen}") })
                        GroupedRow(title = "Matched a chat", trailing = { RowValue("${labStats.matched}") })
                        GroupedRow(title = "No match", trailing = { RowValue("${labStats.unmatched}") }, showDivider = false)
                    }
                }
            }

            item {
                GroupedSection(
                    header = "Try it",
                    footer = "A made-up conversation for checking the preview without Messenger.",
                ) {
                    GroupedRow(
                        title = "Sample preview",
                        titleColor = colors.tint,
                        onClick = { report(container.previewTrigger.activateSample()) },
                    )
                    GroupedRow(
                        title = "Sample preview in 5 seconds",
                        titleColor = colors.tint,
                        showDivider = false,
                        onClick = { report(container.previewTrigger.activateSample(SWITCH_APPS_DELAY_MS)) },
                    )
                }
            }

            item {
                GroupedSection(
                    header = "Privacy",
                    footer = "Previews live in memory only — never written to storage or sent " +
                        "anywhere — and expire after 24 hours or when PEEK stops. Turning PEEK " +
                        "off clears them, along with home screen shortcuts.",
                ) {
                    GroupedRow(
                        title = "PEEK",
                        leading = { Glyph(Icons.Default.Lock, colors.tint) },
                        trailing = {
                            PeekSwitch(peekEnabled) { enabled ->
                                container.appScope.launch { container.preferences.setPeekEnabled(enabled) }
                            }
                        },
                    )
                    GroupedRow(
                        title = "Clear cached previews",
                        titleColor = colors.red,
                        leading = { Glyph(Icons.Default.Delete, colors.red) },
                        showDivider = false,
                        onClick = {
                            container.clearCachedData()
                            toast("Cached previews cleared.")
                        },
                    )
                }
            }

            item {
                GroupedSection(
                    header = "Diagnostics",
                    footer = "Structure of recent Messenger notifications, for compatibility " +
                        "testing. No names or message text are recorded.",
                ) {
                    GroupedRow(
                        title = "Notification structure",
                        leading = { Glyph(Icons.Default.Info, Color(0xFF8E8E93)) },
                        trailing = { RowValue("${diagnostics.size}") },
                        showChevron = !showDiagnostics,
                        showDivider = showDiagnostics && diagnostics.isNotEmpty(),
                        onClick = { showDiagnostics = !showDiagnostics },
                    )
                    if (showDiagnostics) {
                        diagnostics.forEachIndexed { index, record ->
                            DiagnosticRow(record, showDivider = index < diagnostics.lastIndex)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationsSection(
    conversations: List<PeekConversation>,
    enabled: Boolean,
    notificationAccess: Boolean,
    onPeek: (String) -> Unit,
    onLongPeek: (String) -> Unit,
    onPeekLater: (String) -> Unit,
) {
    val colors = LocalPeekColors.current
    val footer = when {
        !enabled -> "PEEK is off. Turn it on under Privacy."
        !notificationAccess -> "Allow notification access to capture Messenger conversations."
        conversations.isEmpty() -> "Nothing yet. Chats appear here as soon as a Messenger notification arrives — no need to open Messenger."
        else -> "Tap or press and hold a chat to peek. Use 5s to switch apps first."
    }
    GroupedSection(header = "Conversations", footer = footer) {
        if (conversations.isEmpty()) {
            GroupedRow(
                title = "No conversations",
                titleColor = colors.secondaryLabel,
                showDivider = false,
            )
        }
        conversations.forEachIndexed { index, conversation ->
            val count = conversation.messages.size
            GroupedRow(
                title = conversation.title,
                subtitle = "${conversation.appLabel} · $count message${if (count == 1) "" else "s"} · " +
                    DateUtils.getRelativeTimeSpanString(conversation.updatedAt),
                leading = { Monogram(conversation.title, size = 40.dp) },
                trailing = {
                    TextButton(onClick = { onPeekLater(conversation.id) }) {
                        Text("5s", color = colors.tint, style = MaterialTheme.typography.bodyLarge)
                    }
                },
                showDivider = index < conversations.lastIndex,
                onClickLabel = "Peek",
                onClick = { onPeek(conversation.id) },
                onLongClick = { onLongPeek(conversation.id) },
            )
        }
    }
}

@Composable
private fun DiagnosticRow(record: NotificationDiagnostic, showDivider: Boolean) {
    val context = LocalContext.current
    GroupedRow(
        title = "${DateFormat.getTimeFormat(context).format(Date(record.receivedAt))} · ${record.outcome.name.lowercase()}",
        subtitle = buildString {
            append("style=${if (record.hasMessagingStyle) "messaging" else "plain"} ")
            append("msgs=${record.messagesWithText}/${record.messageCount} ")
            append("shortcut=${record.hasShortcutId.yn()} locus=${record.hasLocusId.yn()} ")
            append("tag=${record.hasTag.yn()} summary=${record.isGroupSummary.yn()} ")
            append("group=${record.isGroupConversation?.yn() ?: "?"} cat=${record.category ?: "-"}")
        },
        showDivider = showDivider,
    )
}

private fun Boolean.yn() = if (this) "y" else "n"

@Composable
private fun OnOff(on: Boolean) {
    RowValue(if (on) "On" else "Off")
}

@Composable
private fun Glyph(icon: ImageVector, background: Color) {
    IconTile(background) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}
