package com.peek.app.core.triggers

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.peek.app.PeekApplication
import com.peek.app.core.diagnostics.PeekLog
import com.peek.app.core.shortcuts.PeekLauncherActivity

/** Trigger 2: Quick Settings tile that peeks at the most recent conversation. */
class PeekTileService : TileService() {

    private val container get() = (application as PeekApplication).container

    override fun onStartListening() {
        val tile = qsTile ?: return
        val count = container.repository.conversations.value.size
        tile.state = if (count > 0 && container.isPeekEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "PEEK"
        tile.subtitle = when {
            !container.isPeekEnabled -> "Off"
            count == 0 -> "No chats"
            count == 1 -> "1 chat"
            else -> "$count chats"
        }
        tile.updateTile()
    }

    override fun onClick() {
        if (isLocked) unlockAndRun { peek() } else peek()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun peek() {
        val intent = PeekLauncherActivity.intent(this)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(
                    PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                )
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        } catch (e: RuntimeException) {
            PeekLog.w("tile launch failed", e)
        }
    }

    companion object {
        /** Ask the system to refresh the tile's label/state. */
        fun requestUpdate(context: Context) {
            try {
                requestListeningState(context, ComponentName(context, PeekTileService::class.java))
            } catch (e: RuntimeException) {
                PeekLog.w("tile refresh failed", e)
            }
        }
    }
}
