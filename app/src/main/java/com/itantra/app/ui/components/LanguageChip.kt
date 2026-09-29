package com.itantra.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itantra.app.domain.model.Language
import com.itantra.app.ui.theme.Dimens
import com.itantra.app.ui.theme.iTantraLightBlue
import com.itantra.app.ui.theme.iTantraPrimary
import com.itantra.app.ui.theme.iTantraWhite

/**
 * Horizontal scrolling language chip selector for the Home Screen.
 *
 * Shows language chips with native script + English name with smooth animations.
 * "All" button opens the full language selection screen.
 */
@Composable
fun LanguageChipSelector(
    languages: List<Language>,
    selectedLanguage: Language,
    onLanguageSelected: (Language) -> Unit,
    onAllClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.screenPaddingHorizontal),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Select Language",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "(${selectedLanguage.nativeName})",
                    style = MaterialTheme.typography.bodySmall,
                    color = iTantraLightBlue,
                    fontWeight = FontWeight.Medium
                )
            }
            TextButton(onClick = onAllClicked) {
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelLarge,
                    color = iTantraLightBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screenPaddingHorizontal, vertical = Dimens.spacingXs),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            languages.forEach { language ->
                LanguageChip(
                    language = language,
                    isSelected = language == selectedLanguage,
                    onClick = { onLanguageSelected(language) }
                )
            }
        }
    }
}

@Composable
fun LanguageChip(
    language: Language,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) iTantraPrimary else MaterialTheme.colorScheme.surface,
        animationSpec = tween(250),
        label = "chipBgColor"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) iTantraWhite else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(250),
        label = "chipTextColor"
    )

    val subTextColor by animateColorAsState(
        targetValue = if (isSelected) iTantraWhite.copy(alpha = 0.8f) else Color(0xFF64748B),
        animationSpec = tween(250),
        label = "chipSubTextColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) iTantraPrimary else Color(0xFFE2E8F0),
        animationSpec = tween(250),
        label = "chipBorderColor"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.radiusRound))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Dimens.radiusRound),
        color = bgColor,
        border = BorderStroke(1.2.dp, borderColor),
        shadowElevation = if (isSelected) 3.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language.nativeName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = textColor,
                fontSize = 14.sp
            )
            if (language.nativeName != language.displayName) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = language.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = subTextColor,
                    fontSize = 11.sp
                )
            }
        }
    }
}
