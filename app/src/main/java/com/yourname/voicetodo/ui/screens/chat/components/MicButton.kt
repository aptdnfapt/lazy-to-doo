package com.yourname.voicetodo.ui.screens.chat.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MicButton(
    isRecording: Boolean,
    isTranscribing: Boolean,
    isProcessing: Boolean,
    amplitude: Int,
    onRecordingStart: () -> Unit,
    onRecordingStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = when {
            isRecording -> 1.1f
            isTranscribing -> 0.9f
            isProcessing -> 0.95f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 150), label = "scale"
    )

    val backgroundColor = when {
        isRecording -> MaterialTheme.colorScheme.error
        isTranscribing -> MaterialTheme.colorScheme.secondary
        isProcessing -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .size(56.dp)  // Reduced from 80dp
            .scale(scale)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = false, radius = 28.dp),
                onClick = {
                    if (isRecording) {
                        onRecordingStop()
                    } else if (!isTranscribing && !isProcessing) {
                        onRecordingStart()
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isRecording) "■" else "MIC",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

