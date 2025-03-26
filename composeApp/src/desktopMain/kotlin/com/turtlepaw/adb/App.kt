package com.turtlepaw.adb

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import com.konyaco.fluent.FluentTheme
import com.konyaco.fluent.component.*
import com.konyaco.fluent.icons.Icons
import com.konyaco.fluent.icons.regular.ArrowSync
import com.konyaco.fluent.icons.regular.PlugDisconnected
import com.konyaco.fluent.surface.Card
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.awt.Desktop
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean


// Define the connection mode enum
enum class ConnectionMode(val displayName: String) {
    CONNECT("Connect"),
    PAIR("Pair")
}

fun openWebpage(uri: URI?): Boolean {
    val desktop = if (Desktop.isDesktopSupported()) Desktop.getDesktop() else null
    if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
        try {
            desktop.browse(uri)
            return true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
    return false
}

@Composable
@Preview
fun App(state: WindowState) {
    val commandExecutor = AdbCommandExecutor()
    var ipAddress by remember {
        mutableStateOf(
            TextFieldValue(
                AppPreferences.lastConnectedIp ?: ""
            )
        )
    }
    var ipPort by remember { mutableStateOf(TextFieldValue("5555")) }
    var pairingCode by remember { mutableStateOf(TextFieldValue("")) }
    var isConnecting by remember { mutableStateOf(false) }
    var connectionResult by remember { mutableStateOf<String?>(null) }
    var devices by remember { mutableStateOf(emptyList<Pair<String, String>>()) }

    // Connection mode state (Connect or Pair)
    var selectedModeIndex by remember { mutableStateOf(0) }
    val connectionModes = ConnectionMode.values().map { it.displayName }
    val currentMode = ConnectionMode.values()[selectedModeIndex]

    // Create a dedicated executor for ADB commands
    val adbExecutor = remember { Executors.newSingleThreadExecutor() }

    // Remember if we're in the process of connecting
    val isConnectingAtomic = remember { AtomicBoolean(false) }

    LaunchedEffect(ipAddress) {
        // hook to save ip address
        AppPreferences.lastConnectedIp = ipAddress.text
    }
    val dialog = LocalContentDialog.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(state) {
        snapshotFlow { state.isMinimized }
            .onEach {
                devices = commandExecutor.getDevices().getOrThrow()
                println(devices)
            }
            .launchIn(this)
    }

    LaunchedEffect(Unit) {
        devices = commandExecutor.getDevices().getOrThrow()
        println(devices)
        scope.launch {
            val version = commandExecutor.getVersion()

            if (!AppPreferences.showAdbVersion) return@launch
            val result = dialog.show(
                size = DialogSize.Standard,
                title = "ADB Installed",
                contentText = version.getOrThrow(),
                primaryButtonText = "Ok",
                secondaryButtonText = "Don't show again"
            )

            if (result == ContentDialogButton.Secondary) {
                AppPreferences.showAdbVersion = false
            }
        }
    }

    LaunchedEffect(Unit) {
        val currentAppVersion = "1.0.0"
        val updateChecker = UpdateChecker(currentAppVersion)

        val result = updateChecker.checkForUpdate("https://api.github.com/repos/Turtlepaw/adb-manager")

        when {
            result.isUpdateAvailable -> {
                println("Update available! New version: ${result.latestVersion}")

                scope.launch {
                    val result = dialog.show(
                        size = DialogSize.Min,
                        title = "Update Available",
                        contentText = "A new version of the app is available. ${currentAppVersion} -> ${result.latestVersion}",
                        primaryButtonText = "Download",
                        secondaryButtonText = "Remind me later",
                    )

                    if (result == ContentDialogButton.Primary) {
                        openWebpage(
                            URI.create("https://github.com/Turtlepaw/adb-manager/releases/latest")
                        )
                    }
                }
            }
            result.error != null -> println("Error checking for updates: ${result.error}")
            else -> println("You have the latest version")
        }
    }

    // Watch for connection result changes
    LaunchedEffect(connectionResult) {
        connectionResult?.let { result ->
            // Reset connection result
            connectionResult = null

            // Show result in dialog
            dialog.show(
                size = DialogSize.Min,
                title = "Connection Status",
                contentText = result,
                primaryButtonText = "Ok",
            )
        }
    }

    Column(Modifier.padding(24.dp)) {
        Spacer(Modifier.height(20.dp))
        // Connection mode selector
        ComboBox(
            header = "Connection Mode",
            placeholder = "Select mode",
            selected = selectedModeIndex,
            items = connectionModes,
            onSelectionChange = { index, _ -> selectedModeIndex = index },
            //modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(14.dp))

        Row {
            TextField(
                value = ipAddress,
                onValueChange = { ipAddress = it },
                placeholder = { Text("IP Address") },
                enabled = !isConnecting
            )
            Text(":", Modifier.padding(horizontal = 5.dp), style = FluentTheme.typography.bodyLarge)
            TextField(
                value = ipPort,
                onValueChange = { ipPort = it },
                placeholder = { Text("Port") },
                enabled = !isConnecting
            )
        }

        // Show pairing code field only in PAIR mode
        if (currentMode == ConnectionMode.PAIR) {
            Spacer(Modifier.height(14.dp))
            TextField(
                value = pairingCode,
                onValueChange = { pairingCode = it },
                placeholder = { Text("Pairing Code") },
                enabled = !isConnecting,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(14.dp))

        Button(
            onClick = {
                if (!isConnectingAtomic.getAndSet(true)) {
                    // Set UI state immediately
                    isConnecting = true

                    // Execute connection in a separate thread
                    adbExecutor.execute {
                        try {
                            println("Starting ${currentMode.displayName.lowercase()} on thread: ${Thread.currentThread().name}")

                            val ip = ipAddress.text
                            val port = ipPort.text.toIntOrNull() ?: 5555

                            val result = when (currentMode) {
                                ConnectionMode.CONNECT -> {
                                    commandExecutor.connectDevice(ip, port)
                                }
                                ConnectionMode.PAIR -> {
                                    val code = pairingCode.text
                                    if (code.isBlank()) {
                                        throw IllegalArgumentException("Pairing code is required")
                                    }
                                    commandExecutor.pairDevice(ip, port, code)
                                }
                            }

                            // Return to UI thread to update state
                            scope.launch(Dispatchers.Main) {
                                println("${currentMode.displayName} succeeded")
                                connectionResult = result.getOrThrow()
                                // Only refresh devices list on connect, not on pair
                                if (currentMode == ConnectionMode.CONNECT) {
                                    devices = commandExecutor.getDevices().getOrThrow()
                                }
                                isConnecting = false
                                isConnectingAtomic.set(false)
                            }
                        } catch (e: Exception) {
                            println("${currentMode.displayName} failed: ${e.message}")

                            // Return to UI thread to update state
                            scope.launch(Dispatchers.Main) {
                                connectionResult = "Error: ${e.message ?: "Unknown error"}"
                                isConnecting = false
                                isConnectingAtomic.set(false)
                            }
                        }
                    }
                }
            },
            disabled = isConnecting
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isConnecting) {
                    ProgressRing(size = 15.dp)
                    Spacer(Modifier.width(10.dp))
                }
                Text("${currentMode.displayName}${if (isConnecting) "ing" else ""}")
            }
        }

        Spacer(
            Modifier.height(14.dp)
        )

        var isRefreshing by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        Row(
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Text("Connected Devices", style = FluentTheme.typography.subtitle)
            Button(
                onClick = {
                    coroutineScope.launch {
                        isRefreshing = true
                        delay(1000)
                        adbExecutor.execute {
                            devices = commandExecutor.getDevices().getOrThrow()
                            scope.launch(Dispatchers.Main) {
                                println("Refresh succeeded")
                                isRefreshing = false
                            }
                        }
                    }
                }
            ){
                val rotation by animateFloatAsState(
                    targetValue = if (isRefreshing) 360f else 0f,
                    animationSpec = if (isRefreshing)
                        infiniteRepeatable(
                            animation = tween(durationMillis = 1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        )
                    else
                        tween(durationMillis = 300, easing = LinearEasing),
                    label = "RotationAnimation"
                )

                Icon(
                    imageVector = Icons.Default.ArrowSync,
                    contentDescription = "Refreshing",
                    modifier = Modifier.graphicsLayer { rotationZ = rotation }
                )
            }
        }

        Spacer(
            Modifier.height(10.dp)
        )

        if (devices.isEmpty()) {
            Text("No devices connected")
        }

        LazyColumn {
            items(devices) {
                Card(
                    onClick = {},
                    disabled = true
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(15.dp)
                    ) {
                        Column {
                            Text(it.first)
                            Text(
                                text = when (it.second) {
                                    "device" -> "Device"
                                    "unauthorized" -> "Unauthorized"
                                    "offline" -> "Offline"
                                    else -> it.second.capitalize()
                                },
                                color = when (it.second) {
                                    "device" -> FluentTheme.colors.fillAccent.default
                                    "unauthorized" -> hexToColor("#f2c661")
                                    "offline" -> hexToColor("#e37d80")
                                    else -> FluentTheme.colors.text.text.primary
                                }
                            )
                        }

                        if (it.second == "device") {
                            Spacer(Modifier.width(10.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        val result = dialog.show(
                                            size = DialogSize.Min,
                                            title = "Disconnect device?",
                                            contentText = "You are about to disconnect ${it.first}",
                                            primaryButtonText = "Disconnect",
                                            secondaryButtonText = "Cancel"
                                        )

                                        if (result == ContentDialogButton.Primary) {
                                            val cmdResult = commandExecutor.executeCommand(
                                                "disconnect",
                                                it.first
                                            )
                                            if (cmdResult.isSuccess) {
                                                devices = commandExecutor.getDevices().getOrThrow()
                                            }
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Regular.PlugDisconnected,
                                    contentDescription = "PlugDisconnected"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun hexToColor(hex: String): Color {
    val cleanedHex = hex.removePrefix("#")
    val colorLong =
        cleanedHex.toLong(16) or 0x00000000FF000000 // Ensure full opacity if not specified
    return Color(colorLong)
}