package com.peek.app.core.shortcuts

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.peek.app.core.diagnostics.PeekLog
import com.peek.app.data.model.PeekConversation
import com.peek.app.ui.components.initialsOf

/**
 * Trigger 1: recent conversations as dynamic launcher shortcuts. Long-press PEEK's icon on the
 * home screen, tap a chat, and the preview opens without Messenger.
 *
 * Shortcut labels show chat titles on the home screen, so they follow the cache: cleared at
 * process start (the in-memory cache is empty then), when the cache is cleared, and when PEEK
 * is disabled.
 */
class ConversationShortcuts(context: Context) {
    private val appContext = context.applicationContext
    private var published: Set<String> = emptySet()

    fun publish(conversations: List<PeekConversation>) {
        try {
            val max = ShortcutManagerCompat.getMaxShortcutCountPerActivity(appContext)
                .coerceIn(1, MAX_SHORTCUTS)
            val wanted = conversations.take(max)
            val wantedIds = wanted.map { shortcutId(it.id) }.toSet()

            val stale = published - wantedIds
            if (stale.isNotEmpty()) ShortcutManagerCompat.removeDynamicShortcuts(appContext, stale.toList())

            // pushDynamicShortcut is not rate-limited; push least recent first so the most
            // recent chat ends up nearest the icon.
            wanted.withIndex().reversed().forEach { (rank, conversation) ->
                ShortcutManagerCompat.pushDynamicShortcut(appContext, build(conversation, rank))
            }
            published = wantedIds
        } catch (e: RuntimeException) {
            PeekLog.w("shortcut publish failed", e)
        }
    }

    fun clear() {
        try {
            ShortcutManagerCompat.removeAllDynamicShortcuts(appContext)
        } catch (e: RuntimeException) {
            PeekLog.w("shortcut clear failed", e)
        }
        published = emptySet()
    }

    private fun build(conversation: PeekConversation, rank: Int): ShortcutInfoCompat =
        ShortcutInfoCompat.Builder(appContext, shortcutId(conversation.id))
            .setShortLabel(conversation.title.take(24).ifBlank { conversation.appLabel })
            .setLongLabel("Peek at ${conversation.title}".take(48))
            .setIcon(IconCompat.createWithAdaptiveBitmap(monogramBitmap(conversation.title)))
            .setIntent(PeekLauncherActivity.intent(appContext, conversation.id))
            .setRank(rank)
            .build()

    /** Contacts-style monogram, drawn for an adaptive icon (content kept in the safe zone). */
    private fun monogramBitmap(title: String): Bitmap {
        val size = 216 // 108dp adaptive canvas at 2x
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val background = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, size.toFloat(), 0xFFA5ABB9.toInt(), 0xFF858994.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), background)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = size * 0.30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val y = size / 2f - (text.descent() + text.ascent()) / 2f
        canvas.drawText(initialsOf(title), size / 2f, y, text)
        return bitmap
    }

    private fun shortcutId(conversationId: String) = "chat:$conversationId"

    private companion object {
        const val MAX_SHORTCUTS = 4
    }
}
