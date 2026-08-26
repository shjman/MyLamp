package com.yahorshymanchyk.mylamp.widget

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

// Default brightness/step convention mirrors RestScreen.kt (0..100 range, 50% default) — we
// reuse it instead of inventing a new one for the widget.
const val DEFAULT_BRIGHTNESS_PCT = 50
const val BRIGHTNESS_STEP = 10
const val MAX_BRIGHTNESS_PCT = 100

// The "−" button never drives brightness all the way to 0 while the lamp is on — HomeAssistant's
// brightness_pct=0 reads as "off", which would silently desync the widget's ON status from the
// lamp's actual state. 1% keeps it lit at the dimmest step instead.
const val MIN_BRIGHTNESS_PCT_WHILE_ON = 1

val isOnKey = booleanPreferencesKey("is_on")
val brightnessPctKey = intPreferencesKey("brightness_pct")

// Per-glanceId UI state read from the default Glance PreferencesGlanceStateDefinition. The
// widget is optimistic: values are written only after a successful REST call, never synced
// back from Home Assistant.
data class LampWidgetUiState(
    val isOn: Boolean,
    val brightnessPct: Int,
) {
    companion object {
        fun from(prefs: Preferences): LampWidgetUiState =
            LampWidgetUiState(
                isOn = prefs[isOnKey] ?: false,
                brightnessPct = prefs[brightnessPctKey] ?: DEFAULT_BRIGHTNESS_PCT,
            )
    }
}
