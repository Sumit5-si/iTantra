package com.itantra.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.itantra.app.domain.model.ConnectionType
import com.itantra.app.domain.model.DeviceInfo
import com.itantra.app.ui.theme.Dimens
import com.itantra.app.ui.theme.iTantraLightBlue
import com.itantra.app.ui.theme.iTantraPrimary
import com.itantra.app.ui.theme.iTantraTextSecondary
import com.itantra.app.ui.theme.iTantraWhite

/**
 * Device card shown in the Scan Nearby screen for discovered devices.
 */
@Composable
fun DeviceCard(
    device: DeviceInfo,
    isConnecting: Boolean = false,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusMd),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationSm)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = "Device",
                    tint = iTantraPrimary,
                    modifier = Modifier
                        .size(36.dp)
                        .padding(end = Dimens.spacingMd)
                )
                Column {
                    Text(
                        text = device.deviceName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (device.connectionType) {
                                ConnectionType.WIFI_DIRECT -> Icons.Default.Wifi
                                ConnectionType.BLUETOOTH -> Icons.Default.Bluetooth
                            },
                            contentDescription = device.connectionType.name,
                            tint = iTantraTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = when (device.connectionType) {
                                ConnectionType.WIFI_DIRECT -> " Wi-Fi Direct"
                                ConnectionType.BLUETOOTH -> " Bluetooth"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = iTantraTextSecondary
                        )
                    }
                }
            }

            Button(
                onClick = onConnect,
                enabled = !isConnecting,
                shape = RoundedCornerShape(Dimens.radiusRound),
                colors = ButtonDefaults.buttonColors(
                    containerColor = iTantraPrimary,
                    contentColor = iTantraWhite
                )
            ) {
                Text(
                    text = if (isConnecting) "Connecting…" else "Connect",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
