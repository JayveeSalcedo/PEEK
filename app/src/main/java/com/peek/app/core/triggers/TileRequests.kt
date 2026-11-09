package com.peek.app.core.triggers

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import com.peek.app.R

/** Android 13+ can add the tile with a system prompt instead of manual shade editing. */
object TileRequests {
    val canRequest: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun requestAdd(context: Context, onResult: (added: Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val manager = context.getSystemService(StatusBarManager::class.java) ?: return
        manager.requestAddTileService(
            ComponentName(context, PeekTileService::class.java),
            "PEEK",
            Icon.createWithResource(context, R.drawable.ic_tile_peek),
            context.mainExecutor,
        ) { result ->
            onResult(
                result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ||
                    result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED
            )
        }
    }
}
