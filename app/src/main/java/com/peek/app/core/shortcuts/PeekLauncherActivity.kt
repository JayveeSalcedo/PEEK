package com.peek.app.core.shortcuts

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.peek.app.MainActivity
import com.peek.app.PeekApplication
import com.peek.app.core.triggers.TriggerResult

/**
 * Invisible trampoline used by launcher shortcuts and the Quick Settings tile: shows the
 * requested (or latest) conversation as an overlay and finishes immediately, so whatever
 * was on screen stays underneath the preview.
 */
class PeekLauncherActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as PeekApplication).container
        val conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        when (container.previewTrigger.activate(conversationId)) {
            TriggerResult.SHOWN, TriggerResult.SCHEDULED -> Unit
            TriggerResult.NO_CONVERSATION -> Toast.makeText(
                this,
                if (conversationId == null) "No conversations to peek at yet" else "This chat is no longer available",
                Toast.LENGTH_SHORT,
            ).show()
            TriggerResult.NO_OVERLAY_PERMISSION -> startActivity(
                Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    companion object {
        private const val EXTRA_CONVERSATION_ID = "com.peek.app.extra.CONVERSATION_ID"
        private const val ACTION_PEEK = "com.peek.app.action.PEEK"

        /** [conversationId] null means "the most recent conversation". */
        fun intent(context: Context, conversationId: String? = null): Intent =
            Intent(context, PeekLauncherActivity::class.java)
                .setAction(ACTION_PEEK)
                .apply { if (conversationId != null) putExtra(EXTRA_CONVERSATION_ID, conversationId) }
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
    }
}
