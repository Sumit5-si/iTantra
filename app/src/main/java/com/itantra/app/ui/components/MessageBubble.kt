package com.itantra.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.itantra.app.domain.model.MessageStatus
import com.itantra.app.domain.model.Priority
import com.itantra.app.ui.theme.Dimens
import com.itantra.app.ui.theme.iTantraError
import com.itantra.app.ui.theme.iTantraLightBlue
import com.itantra.app.ui.theme.iTantraPrimary
import com.itantra.app.ui.theme.iTantraSuccess
import com.itantra.app.ui.theme.iTantraTextSecondary
import com.itantra.app.ui.theme.iTantraWarning
import com.itantra.app.ui.theme.iTantraWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Message bubble for the Communication Screen.
 *
 * iTantra-styled — NOT a WhatsApp/Telegram clone.
 * Uses rounded cards with iTantra colors and clean typography.
 */
@Composable
fun MessageBubble(
    text: String,
    timestamp: Long,
    isSent: Boolean,
    status: MessageStatus,
    priority: Priority = Priority.NORMAL,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bubbleColor = when {
        priority == Priority.EMERGENCY -> iTantraError
        isSent -> iTantraPrimary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when {
        priority == Priority.EMERGENCY -> iTantraWhite
        isSent -> iTantraWhite
        else -> MaterialTheme.colorScheme.onSurface
    }

    val alignment = if (isSent) Alignment.End else Alignment.Start

    val bubbleShape = if (isSent) {
        RoundedCornerShape(
            topStart = Dimens.messageBubbleRadius,
            topEnd = Dimens.messageBubbleRadius,
            bottomStart = Dimens.messageBubbleRadius,
            bottomEnd = 4.dp
        )
    } else {
        RoundedCornerShape(
            topStart = Dimens.messageBubbleRadius,
            topEnd = Dimens.messageBubbleRadius,
            bottomStart = 4.dp,
            bottomEnd = Dimens.messageBubbleRadius
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            shape = bubbleShape,
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationSm),
            modifier = Modifier.widthIn(max = Dimens.messageBubbleMaxWidth)
        ) {
            Column(modifier = Modifier.padding(Dimens.spacingMd)) {
                if (priority == Priority.EMERGENCY) {
                    Text(
                        text = "⚠ EMERGENCY ALERT",
                        style = MaterialTheme.typography.labelMedium,
                        color = iTantraWarning
                    )
                }

                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Dimens.spacingXs),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimestamp(timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSent || priority == Priority.EMERGENCY) {
                            iTantraWhite.copy(alpha = 0.7f)
                        } else {
                            iTantraTextSecondary
                        }
                    )

                    if (isSent) {
                        MessageStatusIcon(
                            status = status,
                            isDarkBg = true,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageStatusIcon(
    status: MessageStatus,
    isDarkBg: Boolean,
    modifier: Modifier = Modifier
) {
    val (icon, tint) = when (status) {
        MessageStatus.SENDING -> Icons.Default.Schedule to iTantraWhite.copy(alpha = 0.6f)
        MessageStatus.SENT -> Icons.Default.Check to iTantraWhite.copy(alpha = 0.8f)
        MessageStatus.DELIVERED -> Icons.Default.DoneAll to if (isDarkBg) iTantraLightBlue.copy(alpha = 0.9f) else iTantraSuccess
        MessageStatus.FAILED -> Icons.Default.ErrorOutline to iTantraError
    }

    Icon(
        imageVector = icon,
        contentDescription = "Message status: $status",
        tint = tint,
        modifier = modifier.size(14.dp)
    )
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
