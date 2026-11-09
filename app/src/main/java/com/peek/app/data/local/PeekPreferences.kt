package com.peek.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.peekDataStore by preferencesDataStore(name = "peek_settings")

/** User settings only. Message content is never written to DataStore. */
class PeekPreferences(context: Context) {
    private val dataStore = context.applicationContext.peekDataStore

    val peekEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_ENABLED] ?: true }
    val edgeHandleEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_EDGE_HANDLE] ?: false }
    val edgeHandleY: Flow<Float> = dataStore.data.map { it[KEY_EDGE_HANDLE_Y] ?: 0.35f }

    suspend fun setPeekEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_ENABLED] = enabled }
    }

    suspend fun setEdgeHandleEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_EDGE_HANDLE] = enabled }
    }

    suspend fun setEdgeHandleY(fraction: Float) {
        dataStore.edit { it[KEY_EDGE_HANDLE_Y] = fraction }
    }

    private companion object {
        val KEY_ENABLED = booleanPreferencesKey("peek_enabled")
        val KEY_EDGE_HANDLE = booleanPreferencesKey("edge_handle_enabled")
        val KEY_EDGE_HANDLE_Y = floatPreferencesKey("edge_handle_y")
    }
}
