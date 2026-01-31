package com.ericswpark.homebox_qrdroid.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val homeboxServerUrlKey = stringPreferencesKey("homebox_server_url")
    private val trimQrCodeQuietZoneKey = booleanPreferencesKey("trim_qr_code_quiet_zone")

    val homeboxServerUrl: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[homeboxServerUrlKey]
    }

    val trimQrCodeQuietZone: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[trimQrCodeQuietZoneKey] ?: false
    }

    suspend fun setHomeboxServerUrl(url: String) {
        context.dataStore.edit { settings ->
            settings[homeboxServerUrlKey] = url
        }
    }

    suspend fun setTrimQrCodeQuietZone(enabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[trimQrCodeQuietZoneKey] = enabled
        }
    }
}
