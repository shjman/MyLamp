package com.yahorshymanchyk.mylamp.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.yahorshymanchyk.mylamp.data.HomeAssistantLampRepository
import com.yahorshymanchyk.mylamp.domain.LampRepository

// "2x2" / "3x2" — identifies which GlanceAppWidget subclass to refresh after an action, since
// both widgets share the same ActionCallback classes.
const val WIDGET_VARIANT_2X2 = "2x2"
const val WIDGET_VARIANT_3X2 = "3x2"

val widgetVariantKey = ActionParameters.Key<String>("widget_variant")
val brightnessDeltaKey = ActionParameters.Key<Int>("brightness_delta")

private val lampRepository: LampRepository = HomeAssistantLampRepository()

// Toggles power on/off, calling turnOn with the currently stored brightness when switching on.
class TogglePowerAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val currentState = currentUiState(context, glanceId)
        val newIsOn = !currentState.isOn
        val success =
            if (newIsOn) {
                lampRepository.turnOn(currentState.brightnessPct).isSuccess
            } else {
                lampRepository.turnOff().isSuccess
            }
        if (success) {
            updateAppWidgetState(context, glanceId) { prefs -> prefs[isOnKey] = newIsOn }
            updateWidget(context, glanceId, parameters)
        }
    }
}

// Adjusts brightness by BRIGHTNESS_STEP (sign carried via brightnessDeltaKey). No-op while the
// lamp is off — the UI already disables the buttons, this is a functional safety net.
class AdjustBrightnessAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val currentState = currentUiState(context, glanceId)
        if (!currentState.isOn) return
        val delta = parameters[brightnessDeltaKey] ?: 0
        val newBrightnessPct =
            (currentState.brightnessPct + delta).coerceIn(MIN_BRIGHTNESS_PCT_WHILE_ON, MAX_BRIGHTNESS_PCT)
        if (lampRepository.turnOn(newBrightnessPct).isSuccess) {
            updateAppWidgetState(context, glanceId) { prefs -> prefs[brightnessPctKey] = newBrightnessPct }
            updateWidget(context, glanceId, parameters)
        }
    }
}

private suspend fun currentUiState(
    context: Context,
    glanceId: GlanceId,
): LampWidgetUiState = LampWidgetUiState.from(getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId))

private suspend fun updateWidget(
    context: Context,
    glanceId: GlanceId,
    parameters: ActionParameters,
) {
    when (parameters[widgetVariantKey]) {
        WIDGET_VARIANT_3X2 -> LampWidget3x2Widget().update(context, glanceId)
        else -> LampWidget2x2Widget().update(context, glanceId)
    }
}
