package com.itantra.app.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.Color
import com.itantra.app.voice.tts.VoiceGender
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.itantra.app.core.permissions.PermissionManager
import com.itantra.app.domain.model.CommunicationMode
import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.ui.components.ConnectionCard
import com.itantra.app.ui.components.EmergencyButton
import com.itantra.app.ui.components.ITantraTopBar
import com.itantra.app.ui.components.LanguageChipSelector
import com.itantra.app.ui.components.ModeSelector
import com.itantra.app.ui.components.VoiceButton
import com.itantra.app.ui.theme.Dimens
import com.itantra.app.ui.theme.iTantraError
import com.itantra.app.ui.theme.iTantraListening
import com.itantra.app.ui.theme.iTantraPrimary
import com.itantra.app.ui.theme.iTantraSuccess
import com.itantra.app.ui.theme.iTantraTextSecondary
import com.itantra.app.ui.theme.iTantraWarning
import com.itantra.app.ui.theme.iTantraWhite

/**
 * Home Screen — main application screen with live bidirectional communication.
 *
 * Layout priority (top to bottom):
 *  1. Connection status card (tap to scan or chat)
 *  2. Language selector (horizontal chips)
 *  3. Mode selector (Walkie / Phone)
 *  4. Voice button (PTT hold-to-talk or Phone hands-free)
 *  5. Live transcript display
 *  6. Emergency button
 */
@Composable
fun HomeScreen(
    onNavigateToScan: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCommunication: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val connectedDeviceName by viewModel.connectedDeviceName.collectAsStateWithLifecycle()
    val connectionType by viewModel.connectionType.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val communicationMode by viewModel.communicationMode.collectAsStateWithLifecycle()
    val voiceButtonState by viewModel.voiceButtonState.collectAsStateWithLifecycle()
    val lastSentText by viewModel.lastSentText.collectAsStateWithLifecycle()
    val lastReceivedText by viewModel.lastReceivedText.collectAsStateWithLifecycle()
    val emergencyAlertReceived by viewModel.emergencyAlertReceived.collectAsStateWithLifecycle()
    val isAlertAudioPlaying by viewModel.isAlertAudioPlaying.collectAsStateWithLifecycle()
    val lastReceivedLatencyMs by viewModel.lastReceivedLatencyMs.collectAsStateWithLifecycle()
    val lastReceivedPacketBytes by viewModel.lastReceivedPacketBytes.collectAsStateWithLifecycle()
    val connectionRequiredNotice by viewModel.connectionRequiredNotice.collectAsStateWithLifecycle()
    val liveTranscribingText by viewModel.liveTranscribingText.collectAsStateWithLifecycle()
    val speechNotice by viewModel.speechNotice.collectAsStateWithLifecycle()
    val voiceGender by viewModel.voiceGender.collectAsStateWithLifecycle()

    var hasMicPermission by remember {
        mutableStateOf(PermissionManager.isMicrophoneGranted(context))
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
    }

    // Emergency Alert Dialog popup if received from peer (Strictly non-interruptible per PS 26173)
    if (emergencyAlertReceived != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isAlertAudioPlaying) {
                    viewModel.dismissEmergencyAlert()
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = !isAlertAudioPlaying,
                dismissOnClickOutside = !isAlertAudioPlaying
            ),
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = iTantraError,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "EMERGENCY ALERT RECEIVED",
                    color = iTantraError,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = emergencyAlertReceived ?: "Distress alert received from connected peer.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    if (isAlertAudioPlaying) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "🔊 Non-Interruptible Alert Playing (Max Volume)",
                            style = MaterialTheme.typography.labelSmall,
                            color = iTantraError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissEmergencyAlert() },
                    enabled = !isAlertAudioPlaying,
                    colors = ButtonDefaults.buttonColors(containerColor = iTantraError)
                ) {
                    Text(
                        text = if (isAlertAudioPlaying) "Announcing Alert..." else "Acknowledge & Dismiss",
                        color = iTantraWhite
                    )
                }
            }
        )
    }

    // Connection Required Dialog popup
    if (connectionRequiredNotice != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissConnectionRequiredNotice() },
            icon = {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = "Connection Required",
                    tint = iTantraPrimary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Connection Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = connectionRequiredNotice ?: "Pehle doosre phone se connection establish karein. Wi-Fi Direct ya Bluetooth connect hone ke baad hi baat hogi.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissConnectionRequiredNotice()
                        onNavigateToScan()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = iTantraPrimary)
                ) {
                    Text("Scan & Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissConnectionRequiredNotice() }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            ITantraTopBar(
                title = "iTantra",
                showSettingsButton = true,
                onSettingsClick = onNavigateToSettings
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(Dimens.spacingLg))

            // ── 1. Connection Status Card ──
            ConnectionCard(
                connectionState = connectionState,
                connectedDeviceName = connectedDeviceName,
                connectionType = connectionType,
                onTapToScan = {
                    if (connectionState == ConnectionState.CONNECTED) {
                        onNavigateToCommunication()
                    } else {
                        onNavigateToScan()
                    }
                },
                onDisconnect = viewModel::disconnect,
                modifier = Modifier.padding(horizontal = Dimens.screenPaddingHorizontal)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── 2. Language Selector ──
            LanguageChipSelector(
                languages = viewModel.supportedLanguages,
                selectedLanguage = selectedLanguage,
                onLanguageSelected = viewModel::selectLanguage,
                onAllClicked = onNavigateToLanguage
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── 3. Mode Selector ──
            Text(
                text = "Mode (संवाद के प्रकार)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(horizontal = Dimens.screenPaddingHorizontal)
            )

            Spacer(modifier = Modifier.height(Dimens.spacingMd))

            ModeSelector(
                selectedMode = communicationMode,
                onModeSelected = viewModel::selectMode
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Voice Tone Selector (Boy / Girl) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.screenPaddingHorizontal),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Speaker Voice (आवाज)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isBoy = voiceGender == VoiceGender.BOY
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isBoy) iTantraPrimary else Color.Transparent)
                            .clickable { viewModel.setVoiceGender(VoiceGender.BOY) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "👦 Boy",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isBoy) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val isGirl = voiceGender == VoiceGender.GIRL
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isGirl) iTantraPrimary else Color.Transparent)
                            .clickable { viewModel.setVoiceGender(VoiceGender.GIRL) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "👧 Girl",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isGirl) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── 4. Main Voice Button ──
            VoiceButton(
                state = voiceButtonState,
                isWalkieMode = communicationMode == CommunicationMode.WALKIE,
                onPressStart = {
                    if (!hasMicPermission) {
                        micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    } else {
                        viewModel.onVoicePressStart()
                    }
                },
                onPressEnd = {
                    if (hasMicPermission) {
                        viewModel.onVoicePressEnd()
                    }
                },
                onTap = {
                    if (!hasMicPermission) {
                        micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    } else {
                        viewModel.onVoiceTap()
                    }
                }
            )

            // ── Live Speech Feedback & Waiting Cards ──
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.screenPaddingHorizontal),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Live Transcribing State (Sender Phone)
                if (!liveTranscribingText.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Dimens.radiusMd),
                        colors = CardDefaults.cardColors(
                            containerColor = iTantraPrimary.copy(alpha = 0.08f)
                        ),
                        border = BorderStroke(1.2.dp, iTantraPrimary.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = iTantraPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🎙️ Transcribing Voice...",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = iTantraPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = liveTranscribingText!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // 2. Speech Notice (e.g. silence / no speech detected)
                if (!speechNotice.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Dimens.radiusMd),
                        colors = CardDefaults.cardColors(
                            containerColor = iTantraWarning.copy(alpha = 0.12f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = iTantraWarning,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = speechNotice!!,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // 3. Last Sent Voice (Sender Phone)
                lastSentText?.let { sent ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Dimens.radiusMd),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🗣️ Sent Voice Message",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = iTantraPrimary
                                )
                                IconButton(
                                    onClick = { viewModel.replayAudio(sent) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Replay Voice",
                                        tint = iTantraPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = sent,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // 4. Receiver Phone: Waiting vs Received Message
                if (connectionState == ConnectionState.CONNECTED) {
                    if (lastReceivedText.isNullOrBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Dimens.radiusMd),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(iTantraPrimary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "⏳", fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Waiting for voice message...",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "When ${connectedDeviceName?.ifEmpty { "peer" } ?: "peer"} speaks, voice will automatically play out loud here.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = iTantraTextSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Dimens.radiusMd),
                            colors = CardDefaults.cardColors(
                                containerColor = iTantraSuccess.copy(alpha = 0.08f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📥 Received Voice (Audio Played)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = iTantraSuccess
                                    )
                                    IconButton(
                                        onClick = { viewModel.replayAudio(lastReceivedText!!) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Replay Voice",
                                            tint = iTantraSuccess,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = lastReceivedText!!,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── 5. Emergency Button ──
            EmergencyButton(
                onEmergencyActivated = viewModel::onEmergencyActivated,
                enabled = true,
                modifier = Modifier.padding(horizontal = Dimens.screenPaddingHorizontal)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
