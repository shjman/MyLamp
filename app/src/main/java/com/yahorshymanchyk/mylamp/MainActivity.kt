package com.yahorshymanchyk.mylamp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.yahorshymanchyk.mylamp.ble.BleScreen
import com.yahorshymanchyk.mylamp.rest.RestScreen
import com.yahorshymanchyk.mylamp.ui.theme.MyLampTheme
import com.yahorshymanchyk.mylamp.wifi.WifiScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyLampTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MyLampApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

private enum class Screen {
    Home,
    Wifi,
    Rest,
    Ble,
}

private const val LOCAL_NETWORK_PERMISSION_MIN_SDK = 37

@Composable
private fun MyLampApp(modifier: Modifier = Modifier) {
    var screen by remember { mutableStateOf(Screen.Home) }
    val context = LocalContext.current

    // android.permission.ACCESS_LOCAL_NETWORK (dangerous) — without it, socket connections to
    // addresses like 192.168.1.x silently hang until timeout instead of an explicit refusal
    // (found via investigation on 2026-08-23, see CLAUDE.md/smarthome.md). Needed by all three
    // screens (WiFi/REST/BLE), so it's requested once at app startup.
    val localNetworkPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(Unit) {
        // ACCESS_LOCAL_NETWORK only exists starting API 37 (minSdk here is 28) — devices below
        // that don't have this restriction at all, so skip the check/request entirely there.
        if (Build.VERSION.SDK_INT >= LOCAL_NETWORK_PERMISSION_MIN_SDK) {
            val hasLocalNetworkPermission =
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_LOCAL_NETWORK) ==
                    PackageManager.PERMISSION_GRANTED
            if (!hasLocalNetworkPermission) {
                localNetworkPermissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
            }
        }
    }

    BackHandler(enabled = screen != Screen.Home) {
        screen = Screen.Home
    }

    when (screen) {
        Screen.Home ->
            HomeScreen(
                modifier = modifier,
                onSelectWifi = { screen = Screen.Wifi },
                onSelectRest = { screen = Screen.Rest },
                onSelectBle = { screen = Screen.Ble },
            )
        Screen.Wifi -> WifiScreen(modifier = modifier, onBack = { screen = Screen.Home })
        Screen.Rest -> RestScreen(modifier = modifier, onBack = { screen = Screen.Home })
        Screen.Ble -> BleScreen(modifier = modifier, onBack = { screen = Screen.Home })
    }
}

@Composable
private fun HomeScreen(
    onSelectWifi: () -> Unit,
    onSelectRest: () -> Unit,
    onSelectBle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("MyLamp", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = onSelectWifi, modifier = Modifier.fillMaxWidth()) {
            Text("WiFi (MQTT)")
        }
        Button(onClick = onSelectRest, modifier = Modifier.fillMaxWidth()) {
            Text("REST (локальная сеть)")
        }
        Button(onClick = onSelectBle, modifier = Modifier.fillMaxWidth()) {
            Text("BLE (к Малине)")
        }
    }
}
