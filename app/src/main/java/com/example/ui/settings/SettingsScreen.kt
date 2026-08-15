package com.example.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.VpnKey
import com.example.service.sip.RegistrationStatus
import com.example.service.sip.SipRegistrationState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Rose50
import com.example.ui.theme.Rose500
import com.example.ui.theme.Rose600
import com.example.ui.theme.RoyalBlue50
import com.example.ui.theme.RoyalBlue600
import com.example.ui.theme.RoyalBlue700
import com.example.ui.theme.RoyalBlue800
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val regState by viewModel.registrationState.collectAsState()
    val sdpDump by viewModel.sdpDiagnosticDump.collectAsState()

    // Auto-dismiss toast messages after 3 seconds
    LaunchedEffect(uiState.toastMessage) {
        if (uiState.toastMessage != null) {
            delay(3000)
            viewModel.clearToastMessage()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Slate50
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Toast Message Notification Banner
            AnimatedVisibility(
                visible = uiState.toastMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Emerald50)
                        .border(1.dp, Emerald500.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Emerald600,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.toastMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Emerald600
                            )
                        }
                        IconButton(
                            onClick = viewModel::clearToastMessage,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Emerald600,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Realtime True Identity Profile Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(20.dp))
                    .shadow(3.dp, RoundedCornerShape(20.dp), spotColor = RoyalBlue600.copy(alpha = 0.1f))
                    .testTag("realtime_profile_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                    // Profile Header (Avatar + Name + Role + Verified Badge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Avatar with Initials Fallback
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(RoyalBlue600, RoyalBlue800)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!userProfile.photoUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = userProfile.photoUrl,
                                    contentDescription = "User Avatar",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            } else {
                                val initials = userProfile.displayName
                                    .split(" ")
                                    .filter { it.isNotBlank() }
                                    .take(2)
                                    .mapNotNull { it.firstOrNull()?.uppercase() }
                                    .joinToString("")
                                    .ifEmpty { "OP" }

                                Text(
                                    text = initials,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PureWhite,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = userProfile.displayName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.3).sp
                                    ),
                                    color = Slate900
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (userProfile.isVerified) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = "Verified Identity",
                                        tint = RoyalBlue600,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Text(
                                text = userProfile.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )

                            // Status / Presence Pill
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (userProfile.isCloudSynced) Emerald500 else RoyalBlue600)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (userProfile.isCloudSynced) "Firebase RTDB • Live Synced" else "Secure Session • Active",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (userProfile.isCloudSynced) Emerald600 else RoyalBlue700,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Edit Profile Button
                        IconButton(
                            onClick = viewModel::showEditProfileDialog,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(RoyalBlue50)
                                .size(36.dp)
                                .testTag("edit_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = RoyalBlue600,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Identity Details Grid (Org, Role, Phone, UID)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Slate50)
                            .border(1.dp, Slate200, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileMetaRow(
                            icon = Icons.Default.Business,
                            label = "Organization",
                            value = userProfile.organization
                        )
                        ProfileMetaRow(
                            icon = Icons.Default.Security,
                            label = "Account Role",
                            value = userProfile.accountRole
                        )
                        if (userProfile.phoneNumber.isNotEmpty()) {
                            ProfileMetaRow(
                                icon = Icons.Default.Phone,
                                label = "Phone Line",
                                value = userProfile.phoneNumber
                            )
                        }
                        // UID with Copy Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "UID: ${userProfile.uid.take(12)}...",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Slate500
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { viewModel.copyUidToClipboard(userProfile.uid) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy UID",
                                    tint = RoyalBlue600,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Copy",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalBlue600,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Realtime Lifetime Usage Statistics Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Call,
                            label = "Calls Dialed",
                            value = "${userProfile.callsCount}"
                        )
                        StatTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Timer,
                            label = "Call Minutes",
                            value = "${userProfile.totalMinutes}m"
                        )
                        StatTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.CloudDone,
                            label = "RTDB Sync",
                            value = if (userProfile.isCloudSynced) "Active" else "Local"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Audio & Calling Engine Section
            Text(
                text = "Audio & Calling Engine",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Slate700
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    SettingToggleRow(
                        icon = Icons.Default.GraphicEq,
                        title = "HD Audio Quality",
                        subtitle = "High-fidelity wideband audio pipeline",
                        checked = uiState.hdAudioQuality,
                        onCheckedChange = viewModel::toggleHdAudio
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SettingToggleRow(
                        icon = Icons.Default.Headset,
                        title = "Acoustic Noise Reduction",
                        subtitle = "Suppresses background line noise & echo",
                        checked = uiState.noiseReduction,
                        onCheckedChange = viewModel::toggleNoiseReduction
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SettingToggleRow(
                        icon = Icons.Default.VolumeUp,
                        title = "Auto Speakerphone",
                        subtitle = "Route audio automatically to speaker",
                        checked = uiState.autoSpeakerphone,
                        onCheckedChange = viewModel::toggleAutoSpeakerphone
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // VoIP Audio Codec (G.711 PCMA / PCMU) Section
            Text(
                text = "Telephony Codec (ITU-T G.711)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Slate700
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Preferred RTP Payload Encoding",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Slate500
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val selectedCodec = userProfile.preferredCodec

                    // Option 1: Auto Dual Stack
                    CodecOptionCard(
                        title = "Auto Negotiate (Dual Stack)",
                        subtitle = "SDP offers PCMU (0) & PCMA (8) with dynamic matching",
                        isSelected = selectedCodec == "G711_AUTO" || selectedCodec.isEmpty(),
                        badge = "Recommended",
                        onClick = { viewModel.setPreferredCodec("G711_AUTO") }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 2: G.711u (PCMU)
                    CodecOptionCard(
                        title = "G.711u (PCMU / µ-law)",
                        subtitle = "Standard in North America & Japan • 64 kbps, 8kHz",
                        isSelected = selectedCodec == "G711U",
                        badge = "Payload 0",
                        onClick = { viewModel.setPreferredCodec("G711U") }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 3: G.711a (PCMA)
                    CodecOptionCard(
                        title = "G.711a (PCMA / A-law)",
                        subtitle = "Standard in Europe & International • 64 kbps, 8kHz",
                        isSelected = selectedCodec == "G711A",
                        badge = "Payload 8",
                        onClick = { viewModel.setPreferredCodec("G711A") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SIP Trunk Configuration Section
            val sip = userProfile.sipConfig
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SIP Trunk Configuration",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Slate700
                )
                TextButton(
                    onClick = viewModel::showEditSipDialog,
                    modifier = Modifier.testTag("configure_sip_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit SIP Trunk",
                        tint = RoyalBlue600,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Configure Trunk",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = RoyalBlue600
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoStatusRow(
                        icon = Icons.Default.Phone,
                        title = "SIP Host",
                        badge = sip?.host?.ifEmpty { "Not configured" } ?: "Not configured"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoStatusRow(
                        icon = Icons.Default.AccountCircle,
                        title = "SIP Username",
                        badge = sip?.username?.ifEmpty { "Not configured" } ?: "Not configured"
                    )
                    if (sip?.needsPassword() == true || regState.needsPassword) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "SIP password required. Sign in so credentials can load from Firebase, or enter them in Configure Trunk.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Rose500,
                            modifier = Modifier.testTag("sip_password_required_notice")
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoStatusRow(
                        icon = Icons.Default.PhoneAndroid,
                        title = "SIP Port / Device ID",
                        badge = "${sip?.port ?: 5060} • ID ${sip?.deviceId?.ifEmpty { "—" } ?: "—"}"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoStatusRow(
                        icon = Icons.Default.Call,
                        title = "Selected Outbound CID",
                        badge = userProfile.selectedCallerId.ifEmpty { "Dynamic" }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = viewModel::showEditSipDialog,
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalBlue600)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Test SIP Connection",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Test & Configure SIP Connection",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SIP Register Service & Keep-Alive Lifecycle Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SIP Register & Keep-Alive Service",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Slate700
                )
                TextButton(
                    onClick = viewModel::refreshRegistration,
                    modifier = Modifier.testTag("refresh_sip_registration_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh SIP Registration",
                        tint = RoyalBlue600,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Refresh Lease",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = RoyalBlue600
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Status Badge Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val dotColor = when {
                                regState.needsPassword -> Rose500
                                regState.status == RegistrationStatus.REGISTERED -> Emerald500
                                regState.status == RegistrationStatus.REGISTERING ||
                                    regState.status == RegistrationStatus.UNREGISTERING -> RoyalBlue600
                                regState.status == RegistrationStatus.EXPIRED -> Slate500
                                regState.status == RegistrationStatus.FAILED -> Rose500
                                else -> Slate400
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(dotColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = regState.formattedStatus,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Slate800
                            )
                        }

                        val badgeBg = when {
                            regState.needsPassword || regState.status == RegistrationStatus.FAILED -> Rose50
                            regState.status == RegistrationStatus.REGISTERED -> Emerald50
                            else -> RoyalBlue50
                        }
                        val badgeText = when {
                            regState.needsPassword -> "Password required"
                            regState.status == RegistrationStatus.REGISTERED -> "200 OK Active"
                            regState.status == RegistrationStatus.REGISTERING -> "Challenging 401..."
                            regState.status == RegistrationStatus.FAILED && regState.statusCode > 0 ->
                                "Error ${regState.statusCode}"
                            regState.status == RegistrationStatus.FAILED -> "Auth failed"
                            regState.status == RegistrationStatus.EXPIRED -> "Expired"
                            else -> "Standby"
                        }
                        val badgeColor = when {
                            regState.needsPassword || regState.status == RegistrationStatus.FAILED -> Rose500
                            regState.status == RegistrationStatus.REGISTERED -> Emerald600
                            else -> RoyalBlue700
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeBg
                        ) {
                            Text(
                                text = badgeText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = badgeColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    InfoStatusRow(
                        icon = Icons.Default.VpnKey,
                        title = "SIP Digest Account",
                        badge = if (regState.username.isNotBlank() && regState.host.isNotBlank()) {
                            "${regState.username}@${regState.host}"
                        } else {
                            "Not configured"
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    InfoStatusRow(
                        icon = Icons.Default.HourglassTop,
                        title = "Registration Expiry",
                        badge = if (regState.isRegistered) {
                            "${regState.secondsRemaining}s remaining (${regState.expiresSeconds}s lease)"
                        } else {
                            "${regState.expiresSeconds}s (Requested)"
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    InfoStatusRow(
                        icon = Icons.Default.NetworkCheck,
                        title = "NAT Pinhole Keep-Alive",
                        badge = if (regState.isKeepAliveActive) {
                            "25s Ping Active (#${regState.keepAlivePingsSent})"
                        } else {
                            "Standby"
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    InfoStatusRow(
                        icon = Icons.Default.SignalCellularAlt,
                        title = "Gateway / Server Banner",
                        badge = regState.serverBanner.ifEmpty { "—" }
                    )

                    if (regState.lastError != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Notice: ${regState.lastError}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Rose500
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = viewModel::refreshRegistration,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("refresh_lease_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue600)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Registration",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Refresh Lease",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        OutlinedButton(
                            onClick = viewModel::forceReRegister,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("reauth_sip_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Re-Authenticate",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Re-Authenticate",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SIP SDP Diagnostics & Media Inspector Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SIP SDP Media Diagnostics",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Slate700
                )
                if (sdpDump != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Rose50
                    ) {
                        Text(
                            text = "SIP ${sdpDump?.statusCode ?: 488} Captured",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Rose500
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(16.dp))
                    .testTag("sdp_diagnostics_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val currentDump = sdpDump
                    if (currentDump != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Latest Trunk Error Dump (#${currentDump.attemptNumber})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Rose600
                                )
                                Text(
                                    text = "Captured at ${currentDump.formattedTime} • CSeq ${currentDump.cSeq}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate500
                                )
                            }
                            IconButton(
                                onClick = viewModel::copySdpDumpToClipboard,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy SDP Dump",
                                    tint = RoyalBlue600,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Slate900)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = currentDump.formattedReport,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                ),
                                color = Emerald500
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = viewModel::copySdpDumpToClipboard,
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue600)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Diagnostic Report",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Copy Full SDP Diagnostic Report",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    } else {
                        InfoStatusRow(
                            icon = Icons.Default.GraphicEq,
                            title = "SDP Profile Status",
                            badge = "RFC 4566 G.711 RTP/AVP (Plain)"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        InfoStatusRow(
                            icon = Icons.Default.Security,
                            title = "SRTP Requirement",
                            badge = "Disabled (Mandatory SRTP Off)"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        InfoStatusRow(
                            icon = Icons.Default.Code,
                            title = "Codec Offer Policy",
                            badge = "G.711u / G.711a (G.729 Excluded)"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "If an outbound call triggers SIP 488 (Not Acceptable Here) or 415, the complete SDP offer and server headers will be dumped here and to Logcat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Realtime Sync & Telecom Security Section
            Text(
                text = "System Integration & Security",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Slate700
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoStatusRow(
                        icon = Icons.Default.CloudDone,
                        title = "Realtime DB Sync Endpoint",
                        badge = "users/${userProfile.uid.take(8)}..."
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoStatusRow(
                        icon = Icons.Default.PhoneAndroid,
                        title = "Android Telecom Service",
                        badge = "Self-Managed Active"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoStatusRow(
                        icon = Icons.Default.Lock,
                        title = "Encryption Standard",
                        badge = "SRTP offered (not guaranteed)"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoStatusRow(
                        icon = Icons.Default.Security,
                        title = "Network Privacy",
                        badge = "Zero Retention Policy"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Logout Button
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("settings_logout_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Rose50,
                    contentColor = Rose500
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Sign Out",
                    tint = Rose500,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sign Out Operator",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Rose500
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        // Edit Profile Realtime Dialog
        if (uiState.isEditProfileDialogVisible) {
            AlertDialog(
                onDismissRequest = viewModel::hideEditProfileDialog,
                title = {
                    Text(
                        text = "Edit Operator Profile",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Slate900
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Updates are instantly synchronized to Firebase Realtime Database.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )

                        OutlinedTextField(
                            value = uiState.editDisplayName,
                            onValueChange = viewModel::onEditDisplayNameChanged,
                            label = { Text("Display Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_display_name_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RoyalBlue600,
                                unfocusedBorderColor = Slate300
                            )
                        )

                        OutlinedTextField(
                            value = uiState.editOrganization,
                            onValueChange = viewModel::onEditOrganizationChanged,
                            label = { Text("Organization / Agency") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_organization_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RoyalBlue600,
                                unfocusedBorderColor = Slate300
                            )
                        )

                        OutlinedTextField(
                            value = uiState.editAccountRole,
                            onValueChange = viewModel::onEditAccountRoleChanged,
                            label = { Text("Account Role") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_role_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RoyalBlue600,
                                unfocusedBorderColor = Slate300
                            )
                        )

                        OutlinedTextField(
                            value = uiState.editPhoneNumber,
                            onValueChange = viewModel::onEditPhoneNumberChanged,
                            label = { Text("Direct Phone Line") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_phone_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RoyalBlue600,
                                unfocusedBorderColor = Slate300
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = viewModel::saveProfile,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalBlue600,
                            contentColor = PureWhite
                        ),
                        modifier = Modifier.testTag("save_profile_button")
                    ) {
                        Text("Save & Sync", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = viewModel::hideEditProfileDialog,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", color = Slate500)
                    }
                },
                containerColor = PureWhite,
                shape = RoundedCornerShape(18.dp)
            )
        }

        // Edit SIP Trunk Configuration Dialog
        if (uiState.isEditSipDialogVisible) {
            AlertDialog(
                onDismissRequest = viewModel::hideEditSipDialog,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = RoyalBlue600,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Configure SIP Trunk",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Slate900
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Standard RFC 3261 SIP UDP & RTP configuration with MD5 Digest Authentication.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )

                        // Live SIP Connection Test Card / Status Banner
                        if (uiState.isTestingSip) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(RoyalBlue50)
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Testing",
                                        tint = RoyalBlue600,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Probing SIP Server & measuring latency...",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = RoyalBlue700
                                    )
                                }
                            }
                        } else if (uiState.sipTestResult != null) {
                            val res = uiState.sipTestResult!!
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (res.isSuccess) Emerald50 else Rose50)
                                    .border(1.dp, if (res.isSuccess) Emerald500.copy(alpha = 0.4f) else Rose500.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (res.isSuccess) Icons.Default.CheckCircle else Icons.Default.Close,
                                            contentDescription = null,
                                            tint = if (res.isSuccess) Emerald600 else Rose500,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (res.isSuccess) "SIP Connection Verified" else "SIP Connection Notice",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (res.isSuccess) Emerald600 else Rose500
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${res.message} • ${res.latencyMs}ms latency",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate700
                                    )
                                    if (res.serverBanner.isNotBlank()) {
                                        Text(
                                            text = "Server: ${res.serverBanner}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Slate500
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = uiState.editSipHost,
                            onValueChange = viewModel::onEditSipHostChanged,
                            label = { Text("SIP Host / Server") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_sip_host_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RoyalBlue600,
                                unfocusedBorderColor = Slate300
                            )
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = uiState.editSipPort,
                                onValueChange = viewModel::onEditSipPortChanged,
                                label = { Text("Port") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("edit_sip_port_input"),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RoyalBlue600,
                                    unfocusedBorderColor = Slate300
                                )
                            )

                            OutlinedTextField(
                                value = uiState.editSipDeviceId,
                                onValueChange = viewModel::onEditSipDeviceIdChanged,
                                label = { Text("Device ID") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("edit_sip_device_input"),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RoyalBlue600,
                                    unfocusedBorderColor = Slate300
                                )
                            )
                        }

                        OutlinedTextField(
                            value = uiState.editSipUsername,
                            onValueChange = viewModel::onEditSipUsernameChanged,
                            label = { Text("SIP Username / Ext") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_sip_user_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RoyalBlue600,
                                unfocusedBorderColor = Slate300
                            )
                        )

                        OutlinedTextField(
                            value = uiState.editSipPassword,
                            onValueChange = viewModel::onEditSipPasswordChanged,
                            label = { Text("SIP Secret / Password") },
                            supportingText = {
                                if (userProfile.sipConfig?.needsPassword() == true || uiState.editSipPassword.isBlank()) {
                                    Text("Required. Stored only on this device after save.")
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_sip_pass_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RoyalBlue600,
                                unfocusedBorderColor = Slate300
                            )
                        )

                        OutlinedTextField(
                            value = uiState.editSipCallerId,
                            onValueChange = viewModel::onEditSipCallerIdChanged,
                            label = { Text("Default Outbound CID (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_sip_cid_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RoyalBlue600,
                                unfocusedBorderColor = Slate300
                            )
                        )

                        OutlinedButton(
                            onClick = viewModel::testSipConnection,
                            enabled = !uiState.isTestingSip,
                            modifier = Modifier.fillMaxWidth().height(42.dp).testTag("test_sip_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalBlue600)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Test SIP Connection",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isTestingSip) "Testing Connection..." else "Test Live SIP Server Probe",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = viewModel::saveSipConfig,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalBlue600,
                            contentColor = PureWhite
                        ),
                        modifier = Modifier.testTag("save_sip_button")
                    ) {
                        Text("Save Trunk & Sync", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = viewModel::hideEditSipDialog,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", color = Slate500)
                    }
                },
                containerColor = PureWhite,
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}

@Composable
private fun ProfileMetaRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Slate400,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Slate500
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = Slate800
        )
    }
}

@Composable
private fun StatTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Slate50)
            .border(1.dp, Slate200, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = RoyalBlue600,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500,
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = Slate900
            )
        }
    }
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = RoyalBlue600,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Slate900
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PureWhite,
                checkedTrackColor = RoyalBlue600
            )
        )
    }
}

@Composable
private fun InfoStatusRow(
    icon: ImageVector,
    title: String,
    badge: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Slate500,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Slate700
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Emerald50)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = Emerald600,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun CodecOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    badge: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) RoyalBlue600 else Slate200,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) RoyalBlue50.copy(alpha = 0.5f) else Slate50.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = RoyalBlue600,
                        unselectedColor = Slate400
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) RoyalBlue700 else Slate900
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) RoyalBlue600 else Slate200)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = if (isSelected) PureWhite else Slate600,
                    fontSize = 10.sp
                )
            }
        }
    }
}

