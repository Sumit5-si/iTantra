package com.itantra.app.ui.scan

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.itantra.app.core.permissions.PermissionManager
import com.itantra.app.ui.components.DeviceCard
import com.itantra.app.ui.components.ITantraTopBar
import com.itantra.app.ui.theme.Dimens
import com.itantra.app.ui.theme.iTantraLightBlue
import com.itantra.app.ui.theme.iTantraPrimary
import com.itantra.app.ui.theme.iTantraTextSecondary
import com.itantra.app.ui.theme.iTantraWhite

/**
 * Scan Nearby Screen — device discovery with radar animation and 1-minute scan timer.
 *
 * From wireframe:
 *  - Title: "Scan Nearby"
 *  - Concentric circle radar animation during scanning
 *  - 1-Minute countdown timer for peer discovery
 *  - Discovered device list below
 *  - Rescan / Stop buttons
 */
@Composable
fun ScanNearbyScreen(
    onDeviceConnected: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ScanViewModel = viewModel()
) {
    val context = LocalContext.current
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    val connectingDeviceId by viewModel.connectingDeviceId.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val scanTimerSeconds by viewModel.scanTimerSeconds.collectAsStateWithLifecycle()

    var hasPermission by remember {
        mutableStateOf(PermissionManager.isDiscoveryGranted(context))
    }
    var isRadioOn by remember {
        mutableStateOf(viewModel.isWifiEnabled() || viewModel.isBluetoothEnabled())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val granted = PermissionManager.isDiscoveryGranted(context)
        hasPermission = granted
        if (granted) {
            isRadioOn = viewModel.isWifiEnabled() || viewModel.isBluetoothEnabled()
            viewModel.startScan()
        }
    }

    val wifiSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isRadioOn = viewModel.isWifiEnabled() || viewModel.isBluetoothEnabled()
        if (isRadioOn && hasPermission) {
            viewModel.startScan()
        }
    }

    // Auto-start scanning when screen opens (requesting permission if needed)
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(PermissionManager.getDiscoveryPermissions())
        } else {
            isRadioOn = viewModel.isWifiEnabled() || viewModel.isBluetoothEnabled()
            viewModel.startScan()
        }
    }

    // Auto-navigate when device connects successfully
    LaunchedEffect(scanState) {
        if (scanState == ScanState.CONNECTED) {
            onDeviceConnected()
        }
    }

    Scaffold(
        topBar = {
            ITantraTopBar(
                title = "Scan Nearby",
                showBackButton = true,
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(Dimens.spacingXl))

            // ── Radar Animation ──
            Box(
                modifier = Modifier.size(Dimens.scanRadarSize),
                contentAlignment = Alignment.Center
            ) {
                if (scanState == ScanState.SCANNING && hasPermission && isRadioOn) {
                    RadarAnimation()
                }

                // Center icon
                Text(
                    text = when {
                        !hasPermission -> "🔒"
                        !isRadioOn -> "📶"
                        scanState == ScanState.SCANNING -> "📡"
                        scanState == ScanState.DEVICES_FOUND -> "✅"
                        scanState == ScanState.NO_DEVICES -> "📵"
                        scanState == ScanState.FAILED -> "❌"
                        else -> "📡"
                    },
                    style = MaterialTheme.typography.headlineLarge
                )
            }

            // ── 1-Minute Scan Timer Badge ──
            if (scanState == ScanState.SCANNING && hasPermission && isRadioOn) {
                Spacer(modifier = Modifier.height(Dimens.spacingSm))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Dimens.radiusRound))
                        .background(iTantraPrimary.copy(alpha = 0.08f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = iTantraPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val minutes = scanTimerSeconds / 60
                        val seconds = scanTimerSeconds % 60
                        Text(
                            text = String.format("%02d:%02d remaining (1 min scan)", minutes, seconds),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = iTantraPrimary,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }

            // ── Status Text ──
            Text(
                text = when {
                    !hasPermission -> "Nearby device permission is required to discover peers"
                    !isRadioOn -> "Wi-Fi or Bluetooth is turned off. Please turn on Wi-Fi or Bluetooth to scan"
                    scanState == ScanState.IDLE -> "Tap Rescan to search"
                    scanState == ScanState.SCANNING -> "Searching for nearby iTantra devices…"
                    scanState == ScanState.DEVICES_FOUND -> "${discoveredDevices.size} Device(s) Found"
                    scanState == ScanState.NO_DEVICES -> "No Devices Found (1 min scan finished)"
                    scanState == ScanState.CONNECTING -> "Connecting to peer…"
                    scanState == ScanState.CONNECTED -> "Connected!"
                    scanState == ScanState.FAILED -> errorMessage ?: "Connection Failed"
                    else -> "Tap Rescan to search"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    !hasPermission || !isRadioOn || scanState == ScanState.FAILED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onBackground
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Dimens.spacingLg, vertical = Dimens.spacingMd)
            )

            // ── Helper Action Buttons for Permission / Wi-Fi ──
            if (!hasPermission) {
                Button(
                    onClick = {
                        permissionLauncher.launch(PermissionManager.getDiscoveryPermissions())
                    },
                    shape = RoundedCornerShape(Dimens.radiusMd),
                    colors = ButtonDefaults.buttonColors(containerColor = iTantraPrimary)
                ) {
                    Text("Grant Permission", color = iTantraWhite)
                }
                Spacer(modifier = Modifier.height(Dimens.spacingMd))
            } else if (!isRadioOn) {
                Button(
                    onClick = {
                        wifiSettingsLauncher.launch(Intent(Settings.ACTION_WIFI_SETTINGS))
                    },
                    shape = RoundedCornerShape(Dimens.radiusMd),
                    colors = ButtonDefaults.buttonColors(containerColor = iTantraPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = iTantraWhite,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Turn on Wi-Fi", color = iTantraWhite)
                }
                Spacer(modifier = Modifier.height(Dimens.spacingMd))
            }

            // ── Device List ──
            if (discoveredDevices.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.screenPaddingHorizontal),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                ) {
                    items(discoveredDevices, key = { it.deviceId }) { device ->
                        DeviceCard(
                            device = device,
                            isConnecting = connectingDeviceId == device.deviceId,
                            onConnect = { viewModel.connectToDevice(device) }
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))

                if (scanState == ScanState.NO_DEVICES) {
                    Text(
                        text = "Make sure both devices have iTantra open\nand are on the same local Wi-Fi / Direct channel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = iTantraTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = Dimens.spacingXxl)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // ── Bottom Buttons ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.screenPaddingHorizontal)
                    .padding(bottom = Dimens.spacingXl),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
            ) {
                OutlinedButton(
                    onClick = {
                        if (!hasPermission) {
                            permissionLauncher.launch(PermissionManager.getDiscoveryPermissions())
                        } else {
                            isRadioOn = viewModel.isWifiEnabled() || viewModel.isBluetoothEnabled()
                            viewModel.startScan()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(Dimens.radiusMd),
                    enabled = scanState != ScanState.CONNECTING
                ) {
                    Text(if (scanState == ScanState.SCANNING) "Rescan (${scanTimerSeconds}s)" else "Rescan (1 min)")
                }

                Button(
                    onClick = {
                        viewModel.stopScan()
                        if (scanState != ScanState.CONNECTED) onNavigateBack()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(Dimens.radiusMd),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = iTantraPrimary,
                        contentColor = iTantraWhite
                    )
                ) {
                    Text("Stop")
                }
            }
        }
    }
}

/**
 * Concentric circle pulse animation for the scanning radar.
 */
@Composable
private fun RadarAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    val ring1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1"
    )
    val ring2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2"
    )
    val ring3 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring3"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = this.center
        val maxRadius = size.minDimension / 2

        listOf(ring1, ring2, ring3).forEach { scale ->
            drawCircle(
                color = iTantraLightBlue.copy(alpha = 1f - scale),
                radius = maxRadius * scale,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
