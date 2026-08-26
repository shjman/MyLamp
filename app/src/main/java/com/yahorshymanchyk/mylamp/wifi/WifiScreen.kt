package com.yahorshymanchyk.mylamp.wifi

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val DEFAULT_BRIGHTNESS_PERCENT = 50f

@Composable
fun WifiScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isOn by remember { mutableStateOf(false) }
    var brightness by remember { mutableFloatStateOf(DEFAULT_BRIGHTNESS_PERCENT) }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(onClick = onBack) { Text("← Назад") }
        Text("WiFi (прямой MQTT)", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Заглушка — состояние только локальное, к брокеру на Pi ещё не подключена.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Включена")
            Spacer(Modifier.width(8.dp))
            Switch(checked = isOn, onCheckedChange = { isOn = it })
        }
        Spacer(Modifier.height(16.dp))
        Text("Яркость: ${brightness.toInt()}%")
        Slider(
            value = brightness,
            onValueChange = { brightness = it },
            valueRange = 0f..100f,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
