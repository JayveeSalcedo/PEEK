package com.peek.app.core.triggers

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.peek.app.core.overlay.OverlayPermissionChecker
import com.peek.app.core.overlay.PeekOverlayController
import com.peek.app.data.model.PeekConversation
import com.peek.app.data.model.SampleConversation
import com.peek.app.data.repository.ConversationRepository

/** P0 trigger: buttons on PEEK's own test screen. */
class InAppPreviewTrigger(
    context: Context,
    private val repository: ConversationRepository,
    private val overlay: PeekOverlayController,
) : PreviewTrigger {

    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null

    override fun activate(conversationId: String?, delayMillis: Long): TriggerResult =
        schedule(delayMillis) { resolve(conversationId) }

    /** Shows the built-in sample conversation; useful before any real notification arrives. */
    fun activateSample(delayMillis: Long = 0): TriggerResult =
        schedule(delayMillis) { SampleConversation.create() }

    override fun cancel() {
        pending?.let { handler.removeCallbacks(it) }
        pending = null
    }

    private fun schedule(delayMillis: Long, source: () -> PeekConversation?): TriggerResult {
        if (!OverlayPermissionChecker.isGranted(appContext)) return TriggerResult.NO_OVERLAY_PERMISSION
        if (source() == null) return TriggerResult.NO_CONVERSATION
        cancel()
        if (delayMillis <= 0) {
            return if (showNow(source)) TriggerResult.SHOWN else TriggerResult.NO_OVERLAY_PERMISSION
        }
        val runnable = Runnable {
            pending = null
            showNow(source)
        }
        pending = runnable
        handler.postDelayed(runnable, delayMillis)
        return TriggerResult.SCHEDULED
    }

    // Re-resolve at show time so a delayed preview includes messages that arrived meanwhile.
    private fun showNow(source: () -> PeekConversation?): Boolean {
        val conversation = source() ?: return false
        return overlay.show(conversation)
    }

    private fun resolve(conversationId: String?): PeekConversation? =
        if (conversationId == null) repository.latest() else repository.get(conversationId)
}
