package com.itantra.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itantra.app.R
import com.itantra.app.ui.theme.Dimens
import com.itantra.app.ui.theme.iTantraLightBlue
import com.itantra.app.ui.theme.iTantraPrimary
import com.itantra.app.ui.theme.iTantraSuccess
import com.itantra.app.ui.theme.iTantraWhite

/**
 * iTantra branded top app bar.
 *
 * Home variant: shows app logo mark + "iTantra" wordmark + OFFLINE pill badge + settings gear.
 * Screen variant: shows back arrow + title.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ITantraTopBar(
    title: String = "iTantra",
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    showSettingsButton: Boolean = false,
    onSettingsClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    val isHomeTitle = title == "iTantra"

    CenterAlignedTopAppBar(
        title = {
            if (isHomeTitle) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_itantra_logo),
                        contentDescription = "iTantra Logo",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy((-4).dp)
                    ) {
                        Text(
                            text = "iTantra",
                            style = MaterialTheme.typography.titleLarge.copy(lineHeight = 22.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = iTantraPrimary
                        )
                        Text(
                            text = "OFFLINE NEURAL RADIO",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = iTantraLightBlue,
                            letterSpacing = 1.sp
                        )
                    }
                }
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        },
        actions = {
            actions()
            if (showSettingsButton) {
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .padding(end = Dimens.spacingXs)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = iTantraPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background
        )
    )
}
