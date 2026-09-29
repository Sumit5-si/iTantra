package com.itantra.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.itantra.app.ui.components.ITantraTopBar
import com.itantra.app.ui.theme.Dimens
import com.itantra.app.ui.theme.ThemeMode
import com.itantra.app.ui.theme.iTantraLightBlue
import com.itantra.app.ui.theme.iTantraPrimary
import com.itantra.app.ui.theme.iTantraSuccess
import com.itantra.app.ui.theme.iTantraTextSecondary
import com.itantra.app.ui.theme.iTantraWarning

/**
 * Settings Screen from wireframe:
 *  - Permissions (Mic, Location, Bluetooth)
 *  - Appearance (Light / Dark / System Default)
 *  - About App
 */
@Composable
fun SettingsScreen(
    onNavigateToAbout: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            ITantraTopBar(
                title = "Settings",
                showBackButton = true,
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.screenPaddingHorizontal)
        ) {
            // ── How to Use Section ──
            SectionHeader(title = "How to Use iTantra")
            SettingsCard {
                HowToUseStep(
                    stepNumber = "1",
                    title = "Connect Nearby Devices",
                    description = "Ensure Wi-Fi is enabled on both phones. From the Home screen, tap 'Scan for Devices' or tap the Connection Card to discover and pair with a nearby user."
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                HowToUseStep(
                    stepNumber = "2",
                    title = "Select Speaking Mode",
                    description = "Choose 'Walkie-Talkie' to hold the mic while talking and release to send instantly, or 'Phone Mode' for hands-free natural conversation."
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                HowToUseStep(
                    stepNumber = "3",
                    title = "Send Voice Messages & Hear Back",
                    description = "Speak into the microphone. Your voice is transferred instantly without any internet or SIM card, and will play out loud on the connected phone in real time."
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                HowToUseStep(
                    stepNumber = "4",
                    title = "Emergency SOS Alert",
                    description = "In urgent situations, press the Emergency SOS button to ring an audible high-volume alarm on the connected device."
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                HowToUseStep(
                    stepNumber = "5",
                    title = "Disconnect & Switch User",
                    description = "Tap 'Disconnect Device' on the Home connection card anytime you want to cleanly disconnect and pair with another device."
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacingXl))

            // ── Permissions Section ──
            val openAppSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }

            SectionHeader(title = "Permissions")
            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.Mic,
                    title = "Microphone",
                    subtitle = "For voice communication",
                    onClick = openAppSettings
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                SettingsItem(
                    icon = Icons.Default.LocationOn,
                    title = "Location",
                    subtitle = "For Wi-Fi Direct discovery",
                    onClick = openAppSettings
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                SettingsItem(
                    icon = Icons.Default.Bluetooth,
                    title = "Bluetooth",
                    subtitle = "For fallback communication",
                    onClick = openAppSettings
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacingXl))

            // ── Appearance Section ──
            SectionHeader(title = "Appearance")
            SettingsCard {
                ThemeOption(
                    icon = Icons.Default.LightMode,
                    label = "Light",
                    isSelected = themeMode == ThemeMode.LIGHT,
                    onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ThemeOption(
                    icon = Icons.Default.DarkMode,
                    label = "Dark",
                    isSelected = themeMode == ThemeMode.DARK,
                    onClick = { viewModel.setThemeMode(ThemeMode.DARK) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ThemeOption(
                    icon = Icons.Default.SettingsBrightness,
                    label = "System Default",
                    isSelected = themeMode == ThemeMode.SYSTEM,
                    onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) }
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacingXl))

            // ── About Section ──
            SectionHeader(title = "About")
            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "About iTantra",
                    subtitle = "Version, technology, supported languages",
                    showArrow = true,
                    onClick = onNavigateToAbout
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacingXxl))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = iTantraPrimary,
        modifier = Modifier.padding(bottom = Dimens.spacingSm)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(Dimens.radiusMd),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationSm)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    showArrow: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(Dimens.cardPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iTantraLightBlue,
            modifier = Modifier
                .size(24.dp)
                .padding(end = Dimens.spacingSm)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = iTantraTextSecondary
                )
            }
        }
        if (showArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Navigate",
                tint = iTantraTextSecondary
            )
        }
    }
}

@Composable
private fun ThemeOption(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.cardPadding, vertical = Dimens.spacingMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) iTantraPrimary else iTantraTextSecondary,
            modifier = Modifier
                .size(24.dp)
                .padding(end = Dimens.spacingSm)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = iTantraPrimary)
        )
    }
}

@Composable
private fun HowToUseStep(
    stepNumber: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.cardPadding),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(
                    color = iTantraPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(13.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = iTantraPrimary
            )
        }
        Spacer(modifier = Modifier.width(Dimens.spacingMd))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = iTantraTextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

