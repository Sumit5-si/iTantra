package com.itantra.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itantra.app.domain.model.CommunicationMode
import com.itantra.app.ui.theme.Dimens
import com.itantra.app.ui.theme.iTantraPrimary
import com.itantra.app.ui.theme.iTantraWhite

/**
 * Communication mode selector: WALKIE | PHONE
 * Two cards side by side with smooth animated feedback on tap and selection.
 */
@Composable
fun ModeSelector(
    selectedMode: CommunicationMode,
    onModeSelected: (CommunicationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screenPaddingHorizontal),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
    ) {
        ModeCard(
            label = "Walkie Talkie",
            subLabel = "Hold to Talk",
            icon = Icons.Default.SettingsInputAntenna,
            isSelected = selectedMode == CommunicationMode.WALKIE,
            onSelect = { onModeSelected(CommunicationMode.WALKIE) },
            modifier = Modifier.weight(1f)
        )
        ModeCard(
            label = "Phone",
            subLabel = "Hands-free VAD",
            icon = Icons.Default.Phone,
            isSelected = selectedMode == CommunicationMode.PHONE,
            onSelect = { onModeSelected(CommunicationMode.PHONE) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ModeCard(
    label: String,
    subLabel: String,
    icon: ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val scaleAnim by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.95f
            isSelected -> 1.02f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
        label = "modeScale"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) iTantraPrimary else MaterialTheme.colorScheme.surface,
        animationSpec = tween(280),
        label = "modeBgColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) iTantraWhite else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(280),
        label = "modeContentColor"
    )

    val subTextColor by animateColorAsState(
        targetValue = if (isSelected) iTantraWhite.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(280),
        label = "modeSubColor"
    )

    val elevationAnim by animateDpAsState(
        targetValue = if (isSelected) 4.dp else 0.dp,
        animationSpec = tween(280),
        label = "modeElevation"
    )

    Card(
        modifier = modifier
            .scale(scaleAnim)
            .clip(RoundedCornerShape(18.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onSelect()
                    }
                )
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(
            1.5.dp,
            if (isSelected) iTantraPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevationAnim)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) iTantraWhite.copy(alpha = 0.20f)
                        else iTantraPrimary.copy(alpha = 0.08f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = subTextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
