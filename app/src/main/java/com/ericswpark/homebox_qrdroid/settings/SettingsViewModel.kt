package com.ericswpark.homebox_qrdroid.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    val homeboxServerUrl: StateFlow<String?> = settingsRepository.homeboxServerUrl.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val trimQrCodeQuietZone: StateFlow<Boolean> = settingsRepository.trimQrCodeQuietZone.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun setHomeboxServerUrl(url: String) {
        viewModelScope.launch {
            settingsRepository.setHomeboxServerUrl(url)
        }
    }

    fun setTrimQrCodeQuietZone(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setTrimQrCodeQuietZone(enabled)
        }
    }
}

class SettingsViewModelFactory(private val repository: SettingsRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
