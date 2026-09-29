package com.itantra.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itantra.app.domain.model.Language
import com.itantra.app.ui.components.ITantraTopBar
import com.itantra.app.ui.theme.Dimens
import com.itantra.app.ui.theme.iTantraLightBlue
import com.itantra.app.ui.theme.iTantraPrimary
import com.itantra.app.ui.theme.iTantraTextSecondary
import com.itantra.app.ui.theme.iTantraWhite

/**
 * About Screen — iTantra identity, version, technology, and supported languages.
 */
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            ITantraTopBar(
                title = "About iTantra",
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
                .padding(Dimens.screenPaddingHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(Dimens.spacingXxl))

            // Logo
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.itantra.app.R.drawable.ic_itantra_logo),
                contentDescription = "iTantra Logo",
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.height(Dimens.spacingLg))

            Text(
                text = "iTantra",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = iTantraPrimary
            )

            Text(
                text = "Offline Voice Walkie-Talkie & Phone",
                style = MaterialTheme.typography.bodyMedium,
                color = iTantraLightBlue,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimens.spacingXxl))

            // Version Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.radiusMd),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationSm)
            ) {
                Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                    AboutRow(label = "App Version", value = "1.0.0")
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = Dimens.spacingSm)
                    )
                    AboutRow(label = "Connection Modes", value = "Offline Wi-Fi Direct & Bluetooth")
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacingLg))

            // Supported Languages
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.radiusMd),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationSm)
            ) {
                Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                    Text(
                        text = "Supported Languages",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = iTantraPrimary
                    )
                    Spacer(modifier = Modifier.height(Dimens.spacingSm))
                    Text(
                        text = Language.entries.joinToString(" • ") { it.displayName },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacingXl))

            // Concept
            Text(
                text = "Direct device-to-device communication over local Wi-Fi and Bluetooth. No internet required. All voice processing happens on-device.",
                style = MaterialTheme.typography.bodySmall,
                color = iTantraTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Dimens.spacingLg)
            )

            Spacer(modifier = Modifier.height(Dimens.spacingXxl))
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = iTantraTextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
