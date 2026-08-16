package com.example.ui.dialer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.service.ActiveCallInfo
import com.example.service.CallPhase
import com.example.ui.theme.onErrorContainer
import com.example.ui.theme.onSuccessContainer
import com.example.ui.theme.onWarningContainer
import com.example.ui.theme.successContainer
import com.example.ui.theme.warningContainer

@Composable
fun ActiveCallScreen(
    callInfo: ActiveCallInfo,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onSendDtmf: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDtmfPad by remember { mutableStateOf(false) }
    val isTestCall = callInfo.destinationNumber == "3200" || callInfo.destinationNumber == "444"
    val isActive = callInfo.phase == CallPhase.ACTIVE || callInfo.phase == CallPhase.CONNECTED
    val isHold = callInfo.phase == CallPhase.ON_HOLD
    val isEnded = callInfo.phase == CallPhase.ENDED || callInfo.phase == CallPhase.ENDING

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("active_call_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header: call status pill & identity
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    val pillBg = when {
                        isActive -> MaterialTheme.colorScheme.successContainer
                        isHold -> MaterialTheme.colorScheme.warningContainer
                        isEnded -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.primaryContainer
                    }
                    val pillContent = when {
                        isActive -> MaterialTheme.colorScheme.onSuccessContainer
                        isHold -> MaterialTheme.colorScheme.onWarningContainer
                        isEnded -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    }
                    val pillIcon: ImageVector = when {
                        callInfo.isEncrypted && isActive -> Icons.Default.Lock
                        isActive -> Icons.Default.CheckCircle
                        isHold -> Icons.Default.HourglassTop
                        isEnded -> Icons.Default.CallEnd
                        callInfo.phase == CallPhase.RINGING || callInfo.phase == CallPhase.EARLY_MEDIA -> Icons.Default.GraphicEq
                        else -> Icons.Default.Sync
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(pillBg)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = pillIcon,
                            contentDescription = null,
                            tint = pillContent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = callInfo.displayStatus,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = pillContent,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val emoji = when {
                            isTestCall -> "🛠️"
                            isActive -> "📞"
                            isHold -> "⏸️"
                            isEnded -> "⏹️"
                            callInfo.phase == CallPhase.RINGING || callInfo.phase == CallPhase.EARLY_MEDIA -> "🔔"
                            else -> "📡"
                        }
                        Text(
                            text = emoji,
                            fontSize = 40.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = callInfo.destinationNumber,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = if (isTestCall) {
                            if (callInfo.destinationNumber == "3200") {
                                "Test Audio Tones Line"
                            } else {
                                "Line Health Check Diagnostics"
                            }
                        } else {
                            if (callInfo.isEncrypted) {
                                "${callInfo.countryName} • Secured SRTP Line"
                            } else {
                                "${callInfo.countryName} • Standard RTP Line"
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Transmitting ID:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = callInfo.callerIdUsed,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = when {
                            callInfo.phase == CallPhase.ACTIVE -> callInfo.formattedDuration
                            callInfo.phase == CallPhase.ON_HOLD -> "${callInfo.formattedDuration} (Hold)"
                            callInfo.phase == CallPhase.ENDED && callInfo.durationSeconds > 0 -> callInfo.formattedDuration
                            else -> "--:--"
                        },
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (isActive) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                // Middle: live SIP telemetry for an active call
                if (isActive || isHold) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "SIP Live Telemetry",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = callInfo.sipHost,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                DiagnosticItem(
                                    label = "Codec",
                                    value = callInfo.audioCodec.substringBefore(" ").take(7)
                                )
                                DiagnosticItem(label = "Latency", value = "${callInfo.latencyMs} ms")
                                DiagnosticItem(label = "TX Pkts", value = "${callInfo.packetsSent}")
                                DiagnosticItem(label = "RX Pkts", value = "${callInfo.packetsReceived}")
                            }
                        }
                    }
                }

                if (callInfo.dtmfLog.isNotEmpty()) {
                    Text(
                        text = "DTMF Tones: ${callInfo.dtmfLog}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Bottom: action controls
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InCallActionButton(
                            icon = if (callInfo.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            label = if (callInfo.isMuted) "Muted" else "Mute",
                            isActive = callInfo.isMuted,
                            activeColor = MaterialTheme.colorScheme.error,
                            activeContentColor = MaterialTheme.colorScheme.onError,
                            onClick = onToggleMute,
                            testTag = "incall_mute_button"
                        )

                        InCallActionButton(
                            icon = Icons.Default.Dialpad,
                            label = "Keypad",
                            isActive = showDtmfPad,
                            activeColor = MaterialTheme.colorScheme.primary,
                            activeContentColor = MaterialTheme.colorScheme.onPrimary,
                            onClick = { showDtmfPad = !showDtmfPad },
                            testTag = "incall_dtmf_toggle"
                        )

                        InCallActionButton(
                            icon = if (callInfo.isSpeakerOn) {
                                Icons.Default.VolumeUp
                            } else {
                                Icons.Default.VolumeDown
                            },
                            label = if (callInfo.isSpeakerOn) "Speaker" else "Earpiece",
                            isActive = callInfo.isSpeakerOn,
                            activeColor = MaterialTheme.colorScheme.primary,
                            activeContentColor = MaterialTheme.colorScheme.onPrimary,
                            onClick = onToggleSpeaker,
                            testTag = "incall_speaker_button"
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                            .clickable { onEndCall() }
                            .testTag("incall_end_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // DTMF Keypad Overlay
            AnimatedVisibility(
                visible = showDtmfPad,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "In-Call DTMF Touch Keypad",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { showDtmfPad = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.action_close),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val dtmfKeys = listOf(
                            listOf('1', '2', '3'),
                            listOf('4', '5', '6'),
                            listOf('7', '8', '9'),
                            listOf('*', '0', '#')
                        )

                        for (row in dtmfKeys) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (key in row) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { onSendDtmf(key) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key.toString(),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InCallActionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    activeContentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(if (isActive) activeColor else MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    1.dp,
                    if (isActive) activeColor else MaterialTheme.colorScheme.outlineVariant,
                    CircleShape
                )
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) activeContentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
