package com.itantra.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itantra.app.ui.theme.Dimens
import com.itantra.app.ui.theme.iTantraError
import com.itantra.app.ui.theme.iTantraSuccess
import com.itantra.app.ui.theme.iTantraWarning
import com.itantra.app.ui.theme.iTantraWhite
import kotlinx.coroutines.delay

/**
 * Emergency alert button with smooth hold progress animation,
 * tactile haptic response, and animated broadcast confirmation.
 */
@Composable
fun EmergencyButton(
    onEmergencyActivated: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isHolding by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var showAlertSentDialog by remember { mutableStateOf(false) }
    var showTapHint by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current

    // Animate progress while holding down (2.0 seconds = 2000 ms)
    LaunchedEffect(isHolding) {
        if (isHolding) {
            val startTime = System.currentTimeMillis()
            val totalDuration = 2000f
            while (isHolding && holdProgress < 1f) {
                val elapsed = System.currentTimeMillis() - startTime
                holdProgress = (elapsed / totalDuration).coerceIn(0f, 1f)
                if (holdProgress >= 1f) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEmergencyActivated()
                    showAlertSentDialog = true
                    isHolding = false
                    holdProgress = 0f
                    break
                }
                delay(16) // ~60fps
            }
        } else {
            holdProgress = 0f
        }
    }

    // Dismiss tap hint after 2 seconds
    LaunchedEffect(showTapHint) {
        if (showTapHint) {
            delay(2500)
            showTapHint = false
        }
    }

    val cardScale by animateFloatAsState(
        targetValue = if (isHolding) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "emergencyScale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isHolding) iTantraError else iTantraWarning,
        animationSpec = tween(250),
        label = "emergencyBgColor"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .scale(cardScale)
                .clip(RoundedCornerShape(16.dp))
                .pointerInput(enabled) {
                    detectTapGestures(
                        onPress = {
                            isHolding = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            tryAwaitRelease()
                            if (holdProgress < 0.95f && isHolding) {
                                showTapHint = true
                            }
                            isHolding = false
                            holdProgress = 0f
                        },
                        onTap = {
                            showTapHint = true
                        }
                    )
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isHolding) 8.dp else 3.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Main content
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = Dimens.cardPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isHolding) Icons.Default.Campaign else Icons.Default.Warning,
                        contentDescription = "Emergency Alert",
                        tint = iTantraWhite,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isHolding) "KEEP HOLDING TO BROADCAST…" else "Emergency Alert",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = iTantraWhite,
                        fontSize = 16.sp
                    )
                    if (!isHolding) {
                        Text(
                            text = " — Hold 2s",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = iTantraWhite.copy(alpha = 0.9f),
                            fontSize = 13.sp
                        )
                    }
                }

                // Progress line indicator at bottom of card
                if (isHolding) {
                    LinearProgressIndicator(
                        progress = { holdProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .align(Alignment.BottomCenter),
                        color = iTantraWhite,
                        trackColor = Color.Transparent
                    )
                }
            }
        }

        // Tap hint message
        AnimatedVisibility(
            visible = showTapHint,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Text(
                text = "⚠️ Press and hold for 2 seconds to broadcast emergency",
                style = MaterialTheme.typography.labelSmall,
                color = iTantraWarning,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }

    // Confirmation Alert Modal when Emergency is Triggered
    if (showAlertSentDialog) {
        AlertDialog(
            onDismissRequest = { showAlertSentDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(iTantraError.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = iTantraError,
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Emergency Alert Sent",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = iTantraError
                )
            },
            text = {
                Text(
                    text = "Your emergency alert has been sent to nearby devices.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = { showAlertSentDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = iTantraError),
                    shape = RoundedCornerShape(Dimens.radiusRound)
                ) {
                    Text(text = "Dismiss", color = iTantraWhite, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
