package com.example.ui.dialer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.ActiveCallInfo
import com.example.service.CallPhase
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Rose500
import com.example.ui.theme.RoyalBlue50
import com.example.ui.theme.RoyalBlue600
import com.example.ui.theme.RoyalBlue800
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

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

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("active_call_screen"),
        color = Slate50
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header: call status
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (callInfo.phase == CallPhase.ACTIVE) Emerald50 else RoyalBlue50)
                            .border(
                                1.dp,
                                if (callInfo.phase == CallPhase.ACTIVE) Emerald500.copy(alpha = 0.4f) else RoyalBlue600.copy(alpha = 0.3f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Call status",
                            tint = if (callInfo.phase == CallPhase.ACTIVE) Emerald600 else RoyalBlue800,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (callInfo.phase) {
                                CallPhase.CONNECTING -> "Connecting Line..."
                                CallPhase.RINGING -> "Ringing Destination..."
                                CallPhase.ACTIVE -> "HD Line Connected"
                                CallPhase.ENDED -> callInfo.endReason
                                else -> "Outbound Call"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (callInfo.phase == CallPhase.ACTIVE) Emerald600 else RoyalBlue800
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Destination Avatar / Icon
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .shadow(8.dp, CircleShape, spotColor = RoyalBlue600.copy(alpha = 0.2f))
                            .clip(CircleShape)
                            .background(PureWhite)
                            .border(2.dp, Slate200, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isTestCall) "🛠️" else "📞",
                            fontSize = 40.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Destination Number
                    Text(
                        text = callInfo.destinationNumber,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = Slate900,
                        textAlign = TextAlign.Center
                    )

                    // Country / Call Type
                    Text(
                        text = if (isTestCall) {
                            if (callInfo.destinationNumber == "3200") "Test Audio Tones Line" else "Line Health Check Diagnostics"
                        } else {
                            if (callInfo.isEncrypted) {
                                "${callInfo.countryName} • SRTP"
                            } else {
                                "${callInfo.countryName} • Standard RTP"
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate500,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Outbound Caller ID transmitted
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Slate100)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Transmitting ID:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = callInfo.callerIdUsed,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Slate900
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Live Timer
                    Text(
                        text = if (callInfo.phase == CallPhase.ACTIVE) callInfo.formattedDuration else "--:--",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (callInfo.phase == CallPhase.ACTIVE) Slate900 else Slate400
                    )
                }

                // Middle: Diagnostics Card for Health Check, Test calls, or active SIP call telemetry
                if (callInfo.phase == CallPhase.ACTIVE) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Slate200, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Speed, contentDescription = "Speed", tint = Emerald500, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "SIP Live Telemetry",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Slate900
                                    )
                                }
                                Text(
                                    text = "${callInfo.sipHost} (SIP/2.0)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = RoyalBlue600
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                DiagnosticItem(label = "Codec", value = callInfo.audioCodec.substringBefore(" ").take(7))
                                DiagnosticItem(label = "Latency", value = "${callInfo.latencyMs} ms")
                                DiagnosticItem(label = "TX Pkts", value = "${callInfo.packetsSent}")
                                DiagnosticItem(label = "RX Pkts", value = "${callInfo.packetsReceived}")
                            }
                        }
                    }
                }

                // In-Call DTMF Log if any typed
                if (callInfo.dtmfLog.isNotEmpty()) {
                    Text(
                        text = "DTMF Tones: ${callInfo.dtmfLog}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = RoyalBlue600
                    )
                }

                // Bottom: Action Controls
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Control Buttons Row (Mute, DTMF, Speaker)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 28.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InCallActionButton(
                            icon = if (callInfo.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            label = if (callInfo.isMuted) "Muted" else "Mute",
                            isActive = callInfo.isMuted,
                            activeColor = Rose500,
                            onClick = onToggleMute,
                            testTag = "incall_mute_button"
                        )

                        InCallActionButton(
                            icon = Icons.Default.Dialpad,
                            label = "Keypad",
                            isActive = showDtmfPad,
                            activeColor = RoyalBlue600,
                            onClick = { showDtmfPad = !showDtmfPad },
                            testTag = "incall_dtmf_toggle"
                        )

                        InCallActionButton(
                            icon = if (callInfo.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                            label = if (callInfo.isSpeakerOn) "Speaker" else "Earpiece",
                            isActive = callInfo.isSpeakerOn,
                            activeColor = RoyalBlue600,
                            onClick = onToggleSpeaker,
                            testTag = "incall_speaker_button"
                        )
                    }

                    // Red End Call Button
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(8.dp, CircleShape, spotColor = Rose500.copy(alpha = 0.4f))
                            .clip(CircleShape)
                            .background(Rose500)
                            .clickable { onEndCall() }
                            .testTag("incall_end_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = PureWhite,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // DTMF Keypad Overlay Modal
            AnimatedVisibility(
                visible = showDtmfPad,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.dp, Slate200, RoundedCornerShape(24.dp))
                        .shadow(12.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite)
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
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Slate900
                            )
                            IconButton(onClick = { showDtmfPad = false }) {
                                Text("✕", fontWeight = FontWeight.Bold, color = Slate500, fontSize = 18.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Compact DTMF Grid
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
                                            .size(56.dp)
                                            .padding(4.dp)
                                            .clip(CircleShape)
                                            .background(Slate100)
                                            .clickable { onSendDtmf(key) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key.toString(),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Slate900
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Slate400(): Color = Color(0xFF94A3B8)

@Composable
private fun DiagnosticItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Slate500)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ),
            color = Slate900
        )
    }
}

@Composable
private fun InCallActionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(if (isActive) activeColor else Slate100)
                .border(
                    1.dp,
                    if (isActive) activeColor else Slate200,
                    CircleShape
                )
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) PureWhite else Slate800,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isActive) activeColor else Slate700
        )
    }
}
