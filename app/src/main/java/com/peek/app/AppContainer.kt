package com.peek.app

import android.content.Context
import com.peek.app.core.diagnostics.NotificationDiagnostics
import com.peek.app.core.overlay.PeekOverlayController
import com.peek.app.core.triggers.InAppPreviewTrigger
import com.peek.app.data.local.PeekPreferences
import com.peek.app.data.repository.ConversationRepository
import com.peek.app.data.repository.InMemoryConversationRepository
import com.peek.app.integrations.MessagingAdapter
import com.peek.app.integrations.MessengerAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
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

    /** Whether the system currently has the listener bound. */
    val listenerConnected = MutableStateFlow(false)

    /** Mirror of the persisted setting, readable synchronously from the listener thread. */
    @Volatile
    var isPeekEnabled: Boolean = true
        private set

    init {
        appScope.launch {
            preferences.peekEnabled.collect { enabled ->
                isPeekEnabled = enabled
                if (!enabled) clearCachedData()
            }
        }
    }

    /** Removes every cached preview and diagnostic record and closes any visible preview. */
    fun clearCachedData() {
        previewTrigger.cancel()
        overlayController.dismiss()
        repository.clear()
        diagnostics.clear()
    }
}
