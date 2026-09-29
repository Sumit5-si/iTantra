package com.itantra.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.ConnectionType
import com.itantra.app.ui.theme.Dimens
import com.itantra.app.ui.theme.iTantraError
import com.itantra.app.ui.theme.iTantraLightBlue
import com.itantra.app.ui.theme.iTantraPrimary
import com.itantra.app.ui.theme.iTantraSuccess
import com.itantra.app.ui.theme.iTantraTextSecondary
import com.itantra.app.ui.theme.iTantraWarning
import com.itantra.app.ui.theme.iTantraWhite

/**
 * Enhanced, enlarged connection status card on the Home Screen.
 *
 * Disconnected: prominent "No Device Connected" card with "Scan Nearby" action pill
 * Connected: large "Connected" title, peer device name, animated pulse & active link badge
 */
@Composable
fun ConnectionCard(
    connectionState: ConnectionState,
    connectedDeviceName: String? = null,
    connectionType: ConnectionType? = null,
    onTapToScan: () -> Unit,
    onDisconnect: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isConnected = connectionState == ConnectionState.CONNECTED

    val cardBorderColor by animateColorAsState(
        targetValue = when (connectionState) {
            ConnectionState.CONNECTED -> iTantraSuccess.copy(alpha = 0.45f)
            ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> iTantraWarning.copy(alpha = 0.5f)
            ConnectionState.FAILED -> iTantraError.copy(alpha = 0.4f)
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        },
        animationSpec = tween(400),
        label = "cardBorderColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isConnected) { onTapToScan() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.5.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = Dimens.spacingMd)
            ) {
                when (connectionState) {
                    ConnectionState.CONNECTED -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(iTantraSuccess)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CONNECTED & READY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = iTantraSuccess,
                                letterSpacing = 1.sp,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = connectedDeviceName?.ifEmpty { "Connected Peer" } ?: "Connected Peer",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = iTantraPrimary,
                            fontSize = 22.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (connectionType) {
                                ConnectionType.WIFI_DIRECT -> "Direct Wi-Fi P2P Socket Active"
                                ConnectionType.BLUETOOTH -> "Bluetooth RFCOMM Socket Active"
                                else -> "Local Offline Direct Socket Active"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = iTantraTextSecondary,
                            fontSize = 13.5.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(Dimens.radiusRound))
                                .background(iTantraError.copy(alpha = 0.10f))
                                .clickable { onDisconnect() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LinkOff,
                                    contentDescription = "Disconnect",
                                    tint = iTantraError,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Disconnect Device",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = iTantraError,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(iTantraWarning)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LINKING…",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = iTantraWarning,
                                letterSpacing = 1.sp,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Connecting to Peer…",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 21.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Establishing offline raw socket…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = iTantraWarning,
                            fontSize = 13.5.sp
                        )
                    }
                    ConnectionState.FAILED -> {
                        Text(
                            text = "Connection Failed",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = iTantraError,
                            fontSize = 21.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap to retry scanning nearby devices",
                            style = MaterialTheme.typography.bodyMedium,
                            color = iTantraTextSecondary,
                            fontSize = 13.5.sp
                        )
                    }
                    else -> {
                        Text(
                            text = "No Device Connected",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 21.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Direct offline P2P communication link",
                            style = MaterialTheme.typography.bodyMedium,
                            color = iTantraTextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(Dimens.radiusRound))
                                .background(iTantraLightBlue.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Scan Nearby Devices",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = iTantraLightBlue,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.WifiFind,
                                    contentDescription = null,
                                    tint = iTantraLightBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Status indicator with pulsing animation
            ConnectionStatusIndicator(
                connectionState = connectionState,
                connectionType = connectionType
            )
        }
    }
}

@Composable
fun ConnectionStatusIndicator(
    connectionState: ConnectionState,
    connectionType: ConnectionType? = null,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionState == ConnectionState.CONNECTED
    val isConnecting = connectionState == ConnectionState.CONNECTING || connectionState == ConnectionState.RECONNECTING

    val (icon, tint) = when (connectionState) {
        ConnectionState.CONNECTED -> {
            when (connectionType) {
                ConnectionType.BLUETOOTH -> Icons.Default.Bluetooth to iTantraSuccess
                else -> Icons.Default.CheckCircle to iTantraSuccess
            }
        }
        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> {
            Icons.Default.Refresh to iTantraWarning
        }
        ConnectionState.FAILED -> Icons.Default.LinkOff to iTantraError
        else -> Icons.Default.CellTower to iTantraLightBlue
    }

    // Pulse animation for active/connecting states
    val infiniteTransition = rememberInfiniteTransition(label = "indicatorPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isConnected || isConnecting) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "indicatorPulseScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        // Outer glow halo
        Box(
            modifier = Modifier
                .size(62.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f))
        )

        // Inner circle
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Status: $connectionState",
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
