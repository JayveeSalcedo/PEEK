package com.peek.app.core.notifications

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.peek.app.PeekApplication
import com.peek.app.core.diagnostics.NotificationDiagnostic
import com.peek.app.core.diagnostics.NotificationDiagnostic.Outcome
import com.peek.app.core.diagnostics.PeekLog

/**
 * Receives notifications after the user grants notification access, hands supported ones to
 * the matching [com.peek.app.integrations.MessagingAdapter], and caches the result in memory.
 */
class PeekNotificationService : NotificationListenerService() {

    private val container get() = (application as PeekApplication).container

    override fun onListenerConnected() {
        container.listenerConnected.value = true
        PeekLog.d("listener connected")
        // Pick up conversations that were already in the shade when access was granted
        // or when the service restarted (e.g. after a reboot).
        val active = runCatching { activeNotifications }.getOrNull().orEmpty()
        active.sortedBy { it.postTime }.forEach(::handle)
    }

    override fun onListenerDisconnected() {
        container.listenerConnected.value = false
        PeekLog.d("listener disconnected; requesting rebind")
        runCatching {
            requestRebind(ComponentName(this, PeekNotificationService::class.java))
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        handle(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // The notification left the shade (read, dismissed, or replaced). Keep the cached
        // conversation: it is pruned by the retention policy, not by notification state.
    }

    private fun handle(sbn: StatusBarNotification) {
        val adapter = container.adapters.firstOrNull { it.supports(sbn.packageName) } ?: return
        val now = System.currentTimeMillis()
        val snapshot = try {
            NotificationParser.snapshot(sbn)
        } catch (e: Exception) {
            PeekLog.w("snapshot failed for ${sbn.packageName}", e)
            return
        }

        val outcome = if (!container.isPeekEnabled) {
            Outcome.IGNORED_DISABLED
        } else {
            try {
                val conversation = adapter.parse(snapshot)
                if (conversation == null) {
                    Outcome.REJECTED
                } else {
                    container.repository.upsert(conversation)
                    Outcome.STORED
                }
            } catch (e: Exception) {
                PeekLog.w("parse failed for ${sbn.packageName}", e)
                Outcome.ERROR
            }
        }

        val diagnostic = NotificationDiagnostic.from(snapshot, now, outcome)
        container.diagnostics.record(diagnostic)
        PeekLog.d(diagnostic.toLogLine())
    }
}
