package com.yahorshymanchyk.mylamp.rest

// Idle  — screen just opened, no commands sent yet
// Sending — a command is in flight, controls are locked
// Ready  — the last command finished (successfully or with an error)
sealed interface RestScreenUiState {
    data object Idle : RestScreenUiState

    data class Sending(
        val isOn: Boolean,
        val brightnessPct: Int,
    ) : RestScreenUiState

    data class Ready(
        val isOn: Boolean,
        val brightnessPct: Int,
        val statusMessage: String,
    ) : RestScreenUiState
}
