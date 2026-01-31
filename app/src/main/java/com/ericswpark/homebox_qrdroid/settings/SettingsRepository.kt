package com.ericswpark.homebox_qrdroid.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val homeboxServerUrlKey = stringPreferencesKey("homebox_server_url")

    val homeboxServerUrl: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[homeboxServerUrlKey]
    }

    suspend fun setHomeboxServerUrl(url: String) {
        context.dataStore.edit { settings ->
            settings[homeboxServerUrlKey] = url
        }
    }
}
