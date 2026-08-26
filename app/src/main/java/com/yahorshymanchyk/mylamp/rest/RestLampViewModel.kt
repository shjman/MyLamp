package com.yahorshymanchyk.mylamp.rest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yahorshymanchyk.mylamp.data.HomeAssistantLampRepository
import com.yahorshymanchyk.mylamp.domain.LampRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val DEFAULT_BRIGHTNESS_PCT = 50

class RestLampViewModel : ViewModel() {
    // Repository is instantiated in the body — no-arg ViewModel, works with the default viewModel()
    private val repository: LampRepository = HomeAssistantLampRepository()

    private val _uiState = MutableStateFlow<RestScreenUiState>(RestScreenUiState.Idle)
    val uiState: StateFlow<RestScreenUiState> = _uiState.asStateFlow()

    // Current values are kept separately so the Sending state can pass them through to the UI
    private var currentIsOn = false
    private var currentBrightnessPct = DEFAULT_BRIGHTNESS_PCT

    fun onTogglePower(isOn: Boolean) {
        currentIsOn = isOn
        _uiState.value = RestScreenUiState.Sending(isOn, currentBrightnessPct)
        viewModelScope.launch {
            val result = if (isOn) repository.turnOn(currentBrightnessPct) else repository.turnOff()
            _uiState.value = result.toReadyState(currentIsOn, currentBrightnessPct, isOn)
        }
    }

    fun onBrightnessChanged(brightnessPct: Int) {
        currentBrightnessPct = brightnessPct
        if (!currentIsOn) return
        _uiState.value = RestScreenUiState.Sending(currentIsOn, brightnessPct)
        viewModelScope.launch {
            val result = repository.turnOn(brightnessPct)
            _uiState.value = result.toReadyState(currentIsOn, brightnessPct, null)
        }
    }
}

// Helper function — kept separate so it doesn't bloat the ViewModel's methods
private fun Result<Unit>.toReadyState(
    isOn: Boolean,
    brightnessPct: Int,
    toggledIsOn: Boolean?, // non-null only for toggle, to build the message text
): RestScreenUiState.Ready =
    RestScreenUiState.Ready(
        isOn = isOn,
        brightnessPct = brightnessPct,
        statusMessage =
            fold(
                onSuccess = {
                    if (toggledIsOn != null) {
                        if (toggledIsOn) "OK: включено, яркость $brightnessPct%" else "OK: выключено"
                    } else {
                        "OK: яркость $brightnessPct%"
                    }
                },
                onFailure = { "Ошибка: ${it.javaClass.simpleName}: ${it.message}" },
            ),
    )
