package com.yahorshymanchyk.mylamp.rest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt

private const val DEFAULT_BRIGHTNESS_PERCENT = 50f

@Composable
fun RestScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: RestLampViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    // Local transient UI state for smooth drag without network lag
    var sliderPosition by remember { mutableFloatStateOf(DEFAULT_BRIGHTNESS_PERCENT) }

    // Sync sliderPosition from ViewModel state, but skip during Sending to avoid resetting mid-drag
    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is RestScreenUiState.Ready -> sliderPosition = s.brightnessPct.toFloat()
            is RestScreenUiState.Idle -> Unit
            is RestScreenUiState.Sending -> Unit
        }
    }

    val isOn =
        when (val s = uiState) {
            is RestScreenUiState.Ready -> s.isOn
            is RestScreenUiState.Sending -> s.isOn
            is RestScreenUiState.Idle -> false
        }

    val statusText =
        when (val s = uiState) {
            is RestScreenUiState.Ready -> s.statusMessage
            is RestScreenUiState.Sending -> "Отправка..."
            is RestScreenUiState.Idle -> "Команд ещё не отправляли"
        }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(onClick = onBack) { Text("← Назад") }
        Text("REST (локальная сеть)", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Включена")
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = isOn,
                onCheckedChange = { checked -> viewModel.onTogglePower(checked) },
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("Яркость: ${sliderPosition.toInt()}%")
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = { viewModel.onBrightnessChanged(sliderPosition.roundToInt()) },
            valueRange = 0f..100f,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Text(statusText, style = MaterialTheme.typography.bodySmall)
    }
}
