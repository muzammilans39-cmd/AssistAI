package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * High-contrast, accessible animated audio waveform component that provides visual and
 * tactile confirmation whenever the microphone is active and capturing voice or call audio.
 */
@Composable
fun AnimatedVoiceWaveform(
    isActive: Boolean,
    amplitudeLevel: Float,
    statusTitle: String = "MICROPHONE ACTIVE • CAPTURING AUDIO",
    subtitleText: String = "Speak clearly into your device microphone",
    elapsedSeconds: Int? = null,
    alwaysVisible: Boolean = false,
    onTapWaveform: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    // Trigger tactile confirmation via View haptics when recording state transitions or peaks
    LaunchedEffect(isActive) {
        if (isActive) {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        }
    }

    val visible = isActive || alwaysVisible

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "waveformTransition")

        val phaseShift by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phaseShift"
        )

        val secondaryPhase by infiniteTransition.animateFloat(
            initialValue = (2f * PI).toFloat(),
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1350, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "secondaryPhase"
        )

        val indicatorPulse by infiniteTransition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 550),
                repeatMode = RepeatMode.Reverse
            ),
            label = "indicatorPulse"
        )

        val smoothedAmplitude by animateFloatAsState(
            targetValue = if (isActive) amplitudeLevel.coerceIn(0.18f, 1.0f) else 0.08f,
            animationSpec = tween(durationMillis = 110),
            label = "smoothedAmplitude"
        )

        val primaryColor = MaterialTheme.colorScheme.primary
        val activeAccentColor = if (isActive) MaterialTheme.colorScheme.error else primaryColor
        val secondaryWaveColor = MaterialTheme.colorScheme.secondary
        val containerColor = if (isActive) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
        val contentColor = if (isActive) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        val formattedTimer = elapsedSeconds?.let { sec ->
            val mins = sec / 60
            val remSec = sec % 60
            "%02d:%02d".format(mins, remSec)
        }

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = BorderStroke(
                width = if (isActive) 3.dp else 2.dp,
                color = activeAccentColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 6.dp else 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onTapWaveform != null) {
                        Modifier.clickable {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                            onTapWaveform()
                        }
                    } else {
                        Modifier
                    }
                )
                .testTag("animated_voice_waveform")
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = if (isActive) {
                        "$statusTitle. $subtitleText. Audio waveform active."
                    } else {
                        "Microphone waveform idle. Tap microphone to begin speaking."
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Status Row: Pulsing Recording Dot + Status Label + Timer / Tactile Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(28.dp)
                        ) {
                            if (isActive) {
                                Box(
                                    modifier = Modifier
                                        .size((26 * indicatorPulse).dp)
                                        .clip(CircleShape)
                                        .background(activeAccentColor.copy(alpha = 0.35f))
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(activeAccentColor)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isActive) statusTitle else "MICROPHONE STANDBY",
                            style = MaterialTheme.typography.labelLarge,
                            color = contentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (formattedTimer != null && isActive) {
                            Surface(
                                color = activeAccentColor,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = formattedTimer,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, activeAccentColor)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isActive) Icons.Default.Vibration else Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = activeAccentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isActive) "Haptic + Live Audio" else "Ready",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // High-Contrast Canvas Waveform Visualizer (uses DrawScope.size.width & size.height)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("waveform_canvas_container"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(72.dp)) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val centerY = canvasHeight / 2f

                        // 1. Subtle background oscilloscope wave path
                        val wavePath = Path()
                        val points = 60
                        for (i in 0..points) {
                            val x = (i.toFloat() / points) * canvasWidth
                            val normalizedX = (i.toFloat() / points) * 2f * PI.toFloat()
                            val envelope = sin((i.toFloat() / points) * PI.toFloat())
                            val waveOffset = if (isActive) {
                                sin(normalizedX * 2.5f + secondaryPhase) * (canvasHeight * 0.36f) * smoothedAmplitude * envelope
                            } else {
                                sin(normalizedX * 1.5f) * (canvasHeight * 0.05f)
                            }
                            val y = centerY + waveOffset
                            if (i == 0) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
                        }
                        drawPath(
                            path = wavePath,
                            color = secondaryWaveColor.copy(alpha = if (isActive) 0.45f else 0.2f),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // 2. High-contrast vertical equalizer bars across the width
                        val barCount = 23
                        val totalSpacingRatio = 0.42f
                        val barWidth = (canvasWidth * (1f - totalSpacingRatio)) / barCount
                        val gapWidth = (canvasWidth * totalSpacingRatio) / (barCount - 1)
                        val centerIndex = barCount / 2f

                        for (i in 0 until barCount) {
                            val distFromCenter = abs(i - centerIndex) / centerIndex
                            val centerWeight = (1f - (distFromCenter * 0.65f)).coerceIn(0.25f, 1f)

                            val harmonic = abs(sin(phaseShift + i * 0.55f)) * 0.65f +
                                abs(sin(secondaryPhase + i * 0.9f)) * 0.35f

                            val heightFraction = if (isActive) {
                                (0.16f + (smoothedAmplitude * centerWeight * (0.35f + 0.65f * harmonic)))
                                    .coerceIn(0.16f, 0.96f)
                            } else {
                                (0.12f + 0.06f * centerWeight).coerceIn(0.12f, 0.22f)
                            }

                            val barHeight = canvasHeight * heightFraction
                            val topY = centerY - (barHeight / 2f)
                            val leftX = i * (barWidth + gapWidth)

                            val barBrush = Brush.verticalGradient(
                                colors = if (isActive) {
                                    listOf(
                                        activeAccentColor,
                                        primaryColor,
                                        activeAccentColor
                                    )
                                } else {
                                    listOf(
                                        primaryColor.copy(alpha = 0.45f),
                                        primaryColor.copy(alpha = 0.45f)
                                    )
                                },
                                startY = topY,
                                endY = topY + barHeight
                            )

                            drawRoundRect(
                                brush = barBrush,
                                topLeft = Offset(leftX, topY),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Caption Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (isActive) {
                        val dbPercent = (smoothedAmplitude * 100).toInt().coerceIn(15, 100)
                        Text(
                            text = "Level: $dbPercent%",
                            style = MaterialTheme.typography.labelMedium,
                            color = contentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
