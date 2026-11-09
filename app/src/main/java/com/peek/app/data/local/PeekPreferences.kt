package com.peek.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.peekDataStore by preferencesDataStore(name = "peek_settings")

/** User settings only. Message content is never written to DataStore. */
class PeekPreferences(context: Context) {
    private val dataStore = context.applicationContext.peekDataStore

    val peekEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_ENABLED] ?: true }

    suspend fun setPeekEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_ENABLED] = enabled }
    }

    private companion object {
        val KEY_ENABLED = booleanPreferencesKey("peek_enabled")
    }
}
