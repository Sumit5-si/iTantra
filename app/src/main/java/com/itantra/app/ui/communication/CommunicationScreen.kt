package com.itantra.app.ui.communication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.ConnectionType
import com.itantra.app.domain.model.MessageStatus
import com.itantra.app.ui.components.ITantraTopBar
import com.itantra.app.ui.components.MessageBubble
import com.itantra.app.ui.theme.Dimens
import com.itantra.app.ui.theme.iTantraLightBlue
import com.itantra.app.ui.theme.iTantraPrimary
import com.itantra.app.ui.theme.iTantraSuccess
import com.itantra.app.ui.theme.iTantraTextSecondary
import com.itantra.app.ui.theme.iTantraWhite

/**
 * Communication Screen — text messaging + future voice input.
 *
 * Layout:
 *  - Top: Back + "iTantra" + connection status pill
 *  - Middle: Message list (iTantra-styled bubbles)
 *  - Bottom: Text input + mic button + send button
 *
 * NOT WhatsApp-styled — uses iTantra's design language.
 */
@Composable
fun CommunicationScreen(
    onNavigateBack: () -> Unit,
    viewModel: CommunicationViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val textInput by viewModel.textInput.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val connectedDeviceName by viewModel.connectedDeviceName.collectAsStateWithLifecycle()
    val connectionType by viewModel.connectionType.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            ITantraTopBar(
                title = "iTantra",
                showBackButton = true,
                onBackClick = onNavigateBack,
                actions = {
                    // Connection status pill
                    Card(
                        shape = RoundedCornerShape(Dimens.radiusRound),
                        colors = CardDefaults.cardColors(
                            containerColor = when (connectionState) {
                                ConnectionState.CONNECTED -> iTantraSuccess.copy(alpha = 0.2f)
                                else -> iTantraTextSecondary.copy(alpha = 0.2f)
                            }
                        ),
                        modifier = Modifier.padding(end = Dimens.spacingSm)
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = Dimens.spacingMd,
                                vertical = Dimens.spacingXs
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (connectionState == ConnectionState.CONNECTED) "Connected" else "Disconnected",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = iTantraWhite
                            )
                            Text(
                                text = when (connectionType) {
                                    ConnectionType.WIFI_DIRECT -> "Wi-Fi"
                                    ConnectionType.BLUETOOTH -> "Bluetooth"
                                    null -> "Offline"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = iTantraWhite.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // ── Message List ──
            if (messages.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "📡",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Spacer(modifier = Modifier.height(Dimens.spacingMd))
                    Text(
                        text = "Connected to $connectedDeviceName",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Send a message to start communicating",
                        style = MaterialTheme.typography.bodySmall,
                        color = iTantraTextSecondary
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.screenPaddingHorizontal),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                ) {
                    item { Spacer(modifier = Modifier.height(Dimens.spacingSm)) }

                    items(messages, key = { it.id }) { message ->
                        MessageBubble(
                            text = message.text,
                            timestamp = message.timestamp,
                            isSent = message.isSent,
                            status = message.status,
                            priority = message.priority,
                            onRetry = if (message.status == MessageStatus.FAILED) {
                                { viewModel.retryMessage(message.id) }
                            } else null
                        )
                    }

                    item { Spacer(modifier = Modifier.height(Dimens.spacingSm)) }
                }
            }

            // ── Input Bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = Dimens.screenPaddingHorizontal,
                        vertical = Dimens.spacingSm
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = viewModel::updateTextInput,
                    placeholder = {
                        Text(
                            text = "Type a message…",
                            color = iTantraTextSecondary
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(Dimens.radiusXl),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = iTantraPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    maxLines = 4
                )

                // Mic button (prepared for STT)
                IconButton(
                    onClick = { /* STT will be wired in Phase 2 */ },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = iTantraLightBlue,
                        contentColor = iTantraWhite
                    ),
                    modifier = Modifier.size(Dimens.iconButtonSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice input"
                    )
                }

                // Send button
                IconButton(
                    onClick = viewModel::sendMessage,
                    enabled = textInput.isNotBlank(),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = iTantraPrimary,
                        contentColor = iTantraWhite,
                        disabledContainerColor = iTantraPrimary.copy(alpha = 0.4f),
                        disabledContentColor = iTantraWhite.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.size(Dimens.iconButtonSize)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message"
                    )
                }
            }
        }
    }
}
