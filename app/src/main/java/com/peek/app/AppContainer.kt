package com.peek.app

import android.content.Context
import com.peek.app.core.diagnostics.LabStats
import com.peek.app.core.diagnostics.NotificationDiagnostics
import com.peek.app.core.overlay.EdgeHandleController
import com.peek.app.core.overlay.PeekOverlayController
import com.peek.app.core.shortcuts.ConversationShortcuts
import com.peek.app.core.triggers.InAppPreviewTrigger
import com.peek.app.core.triggers.PeekTileService
import com.peek.app.data.local.PeekPreferences
import com.peek.app.data.repository.ConversationRepository
import com.peek.app.data.repository.InMemoryConversationRepository
import com.peek.app.integrations.MessagingAdapter
import com.peek.app.integrations.MessengerAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Manual dependency container (spec §3: manual DI initially). One per process. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val adapters: List<MessagingAdapter> = listOf(MessengerAdapter())
    val repository: ConversationRepository = InMemoryConversationRepository()
    val diagnostics = NotificationDiagnostics()
    val preferences = PeekPreferences(appContext)
    val overlayController = PeekOverlayController(appContext)
    val previewTrigger = InAppPreviewTrigger(appContext, repository, overlayController)
    val shortcuts = ConversationShortcuts(appContext)
    val edgeHandle = EdgeHandleController(
        appContext,
        onActivate = { previewTrigger.activate(null) },
        onMoved = { y -> appScope.launch { preferences.setEdgeHandleY(y) } },
    )

    /** Whether the system currently has the listener bound. */
    val listenerConnected = MutableStateFlow(false)

    /** Lab build only: counters from the accessibility long-press experiment. */
    val labStats = MutableStateFlow(LabStats())

    /** Mirror of the persisted setting, readable synchronously from the listener thread. */
    @Volatile
    var isPeekEnabled: Boolean = true
        private set

    private var edgeHandleWanted = false
    private var edgeHandleY = 0.35f

    init {
        // The cache starts empty in a new process, so shortcuts from a previous one are stale.
        shortcuts.clear()

        appScope.launch {
            preferences.peekEnabled.collect { enabled ->
                isPeekEnabled = enabled
                if (!enabled) clearCachedData()
                PeekTileService.requestUpdate(appContext)
            }
        }
        appScope.launch {
            repository.conversations
                .map { list -> list.take(4).map { it.id to it.title } }
                .distinctUntilChanged()
                .collect {
                    shortcuts.publish(repository.conversations.value)
                    PeekTileService.requestUpdate(appContext)
                }
        }
        appScope.launch {
            combine(
                preferences.peekEnabled,
                preferences.edgeHandleEnabled,
                preferences.edgeHandleY,
            ) { enabled, handle, y -> (enabled && handle) to y }
                .collect { (wanted, y) ->
                    edgeHandleWanted = wanted
                    edgeHandleY = y
                    refreshEdgeHandle()
                }
        }
    }

    /** Re-evaluates the edge handle, e.g. after the overlay permission changed. */
    fun refreshEdgeHandle() {
        if (edgeHandleWanted) edgeHandle.show(edgeHandleY) else edgeHandle.hide()
    }

    /** Removes every cached preview, shortcut and diagnostic record and closes any preview. */
    fun clearCachedData() {
        previewTrigger.cancel()
        overlayController.dismissNow()
        repository.clear()
        diagnostics.clear()
        shortcuts.clear()
        labStats.value = LabStats()
    }
}
