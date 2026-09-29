package com.itantra.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itantra.app.domain.model.VoiceButtonState
import com.itantra.app.ui.theme.Dimens
import com.itantra.app.ui.theme.iTantraError
import com.itantra.app.ui.theme.iTantraLightBlue
import com.itantra.app.ui.theme.iTantraListening
import com.itantra.app.ui.theme.iTantraPrimary
import com.itantra.app.ui.theme.iTantraProcessing
import com.itantra.app.ui.theme.iTantraSending
import com.itantra.app.ui.theme.iTantraSuccess
import com.itantra.app.ui.theme.iTantraWhite

/**
 * Large circular microphone button — primary action on Home Screen.
 *
 * Smooth touch reaction with spring bounce, radiating soundwave rings,
 * and clear animated state transitions.
 *
 * Walkie mode: Press and hold to talk. Release to transmit text.
 * Phone mode: Tap to toggle continuous VAD listening.
 */
@Composable
fun VoiceButton(
    state: VoiceButtonState,
    isWalkieMode: Boolean,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val isListeningActive = state == VoiceButtonState.LISTENING || isPressed

    // Spring scale for tactile touch feedback
    val buttonScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.91f
            state == VoiceButtonState.LISTENING -> 1.05f
            else -> 1.0f
        },
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 380f),
        label = "voiceButtonScale"
    )

    val activeColor = when (state) {
        VoiceButtonState.IDLE -> if (isPressed) iTantraListening else iTantraPrimary
        VoiceButtonState.LISTENING -> iTantraListening
        VoiceButtonState.PROCESSING -> iTantraProcessing
        VoiceButtonState.SENDING -> iTantraSending
        VoiceButtonState.ERROR -> iTantraError
    }

    val backgroundColor by animateColorAsState(
        targetValue = activeColor,
        animationSpec = tween(250),
        label = "voiceButtonColor"
    )

    // Radiating soundwave rings animation
    val infiniteTransition = rememberInfiniteTransition(label = "soundwaveRipples")
    val wave1Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListeningActive) 1.42f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1Scale"
    )
    val wave1Alpha by infiniteTransition.animateFloat(
        initialValue = if (isListeningActive) 0.35f else 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1Alpha"
    )

    val wave2Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListeningActive) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, delayMillis = 350),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2Scale"
    )
    val wave2Alpha by infiniteTransition.animateFloat(
        initialValue = if (isListeningActive) 0.45f else 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, delayMillis = 350),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2Alpha"
    )

    val icon = when {
        isPressed || state == VoiceButtonState.LISTENING -> Icons.Default.GraphicEq
        state == VoiceButtonState.PROCESSING -> Icons.Default.Mic
        state == VoiceButtonState.SENDING -> Icons.AutoMirrored.Filled.Send
        state == VoiceButtonState.ERROR -> Icons.Default.MicOff
        else -> Icons.Default.Mic
    }

    val labelText = when {
        isPressed || state == VoiceButtonState.LISTENING -> {
            if (isWalkieMode) "Listening… Release to Send" else "Listening… Tap to Stop"
        }
        state == VoiceButtonState.PROCESSING -> "Transcribing…"
        state == VoiceButtonState.SENDING -> "Transcribing…"
        state == VoiceButtonState.ERROR -> "Mic Error — Tap to Retry"
        else -> if (isWalkieMode) "Hold to Talk" else "Tap to Start Speaking"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(184.dp)
        ) {
            // Radiating wave ring 1
            if (isListeningActive) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .scale(wave1Scale)
                        .alpha(wave1Alpha)
                        .clip(CircleShape)
                        .background(iTantraListening)
                )
                // Radiating wave ring 2
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .scale(wave2Scale)
                        .alpha(wave2Alpha)
                        .clip(CircleShape)
                        .background(iTantraListening)
                )
            }

            // Main central circular button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(112.dp)
                    .scale(buttonScale)
                    .shadow(
                        elevation = if (isListeningActive) 14.dp else 6.dp,
                        shape = CircleShape,
                        spotColor = activeColor
                    )
                    .clip(CircleShape)
                    .background(backgroundColor)
                    .pointerInput(isWalkieMode) {
                        if (isWalkieMode) {
                            detectTapGestures(
                                onPress = {
                                    isPressed = true
                                    onPressStart()
                                    tryAwaitRelease()
                                    isPressed = false
                                    onPressEnd()
                                }
                            )
                        } else {
                            detectTapGestures(
                                onPress = {
                                    isPressed = true
                                    tryAwaitRelease()
                                    isPressed = false
                                },
                                onTap = { onTap() }
                            )
                        }
                    }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = labelText,
                    tint = iTantraWhite,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.spacingSm))

        // Dynamic status pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(Dimens.radiusRound))
                .background(
                    if (isListeningActive) iTantraListening.copy(alpha = 0.14f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isListeningActive) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(iTantraListening)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isListeningActive) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isListeningActive) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )
            }
        }
    }
}
