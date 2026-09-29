package com.itantra.app.ui.language

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.itantra.app.domain.model.Language
import com.itantra.app.ui.components.ITantraTopBar
import com.itantra.app.ui.theme.Dimens
import com.itantra.app.ui.theme.iTantraPrimary
import com.itantra.app.ui.theme.iTantraTextSecondary
import com.itantra.app.ui.theme.iTantraWhite

/**
 * Full language selection screen with vertical list and radio buttons.
 *
 * From wireframe:
 *  - Each row: language name + native script name + radio indicator
 *  - "Done" button at bottom
 */
@Composable
fun LanguageScreen(
    onNavigateBack: () -> Unit,
    viewModel: LanguageViewModel = viewModel()
) {
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            ITantraTopBar(
                title = "Select Language",
                showBackButton = true,
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(viewModel.supportedLanguages) { language ->
                    LanguageRow(
                        language = language,
                        isSelected = language == selectedLanguage,
                        onClick = { viewModel.selectLanguage(language) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
            }

            // Done button
            Button(
                onClick = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.screenPaddingHorizontal)
                    .padding(bottom = Dimens.spacingXl)
                    .height(Dimens.buttonHeight),
                shape = RoundedCornerShape(Dimens.radiusMd),
                colors = ButtonDefaults.buttonColors(
                    containerColor = iTantraPrimary,
                    contentColor = iTantraWhite
                )
            ) {
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun LanguageRow(
    language: Language,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = Dimens.screenPaddingHorizontal,
                vertical = Dimens.spacingMd
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = language.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) iTantraPrimary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = language.nativeName,
                style = MaterialTheme.typography.bodySmall,
                color = iTantraTextSecondary
            )
        }

        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = iTantraPrimary,
                unselectedColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}
