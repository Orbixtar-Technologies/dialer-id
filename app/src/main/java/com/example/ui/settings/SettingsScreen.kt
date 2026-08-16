package com.example.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import com.example.service.sip.RegistrationStatus
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.common.AppTextField
import com.example.ui.theme.Ink950
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.glassBorder
import com.example.ui.theme.onSuccessContainer
import com.example.ui.theme.onWarningContainer
import com.example.ui.theme.success
import com.example.ui.theme.successContainer
import com.example.ui.theme.warning
import com.example.ui.theme.warningContainer
import kotlinx.coroutines.delay

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

    var showSignOutDialog by remember { mutableStateOf(false) }

    // Auto-dismiss toast messages after 3 seconds
    LaunchedEffect(uiState.toastMessage) {
        if (uiState.toastMessage != null) {
            delay(3000)
            viewModel.clearToastMessage()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
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
                        .background(MaterialTheme.colorScheme.successContainer)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.success,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(start = 14.dp, top = 4.dp, end = 4.dp, bottom = 4.dp)
                ) {
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
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSuccessContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.toastMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSuccessContainer,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = viewModel::clearToastMessage,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.settings_toast_dismiss),
                                tint = MaterialTheme.colorScheme.onSuccessContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Realtime True Identity Profile Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.glassBorder,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .testTag("realtime_profile_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                                    // Fixed brand gradient: dark enough for white
                                    // initials in either theme.
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!userProfile.photoUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = userProfile.photoUrl,
                                    contentDescription = stringResource(R.string.settings_profile_photo),
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
                                        color = MaterialTheme.colorScheme.onPrimary,
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
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (userProfile.isVerified) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = stringResource(
                                            R.string.settings_verified_identity
                                        ),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Text(
                                text = userProfile.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                                        .background(
                                            if (userProfile.isCloudSynced) {
                                                MaterialTheme.colorScheme.success
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (userProfile.isCloudSynced) {
                                        stringResource(R.string.settings_status_synced)
                                    } else {
                                        stringResource(R.string.settings_status_session)
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (userProfile.isCloudSynced) {
                                        MaterialTheme.colorScheme.onSuccessContainer
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Edit Profile Button
                        IconButton(
                            onClick = viewModel::showEditProfileDialog,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .testTag("edit_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.settings_edit_profile),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Identity Details Grid (Org, Role, Phone, UID)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileMetaRow(
                            icon = Icons.Default.Business,
                            label = stringResource(R.string.settings_meta_organization),
                            value = userProfile.organization
                        )
                        ProfileMetaRow(
                            icon = Icons.Default.Security,
                            label = stringResource(R.string.settings_meta_role),
                            value = userProfile.accountRole
                        )
                        if (userProfile.phoneNumber.isNotEmpty()) {
                            ProfileMetaRow(
                                icon = Icons.Default.Phone,
                                label = stringResource(R.string.settings_meta_phone),
                                value = userProfile.phoneNumber
                            )
                        }
                        // UID with Copy Button
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
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(
                                        R.string.settings_uid,
                                        userProfile.uid.take(12)
                                    ),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.copyUidToClipboard(userProfile.uid) }
                                    .heightIn(min = 48.dp)
                                    .padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = stringResource(R.string.settings_copy_uid),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.settings_copy),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
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
                            label = stringResource(R.string.settings_stat_calls),
                            value = "${userProfile.callsCount}"
                        )
                        StatTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Timer,
                            label = stringResource(R.string.settings_stat_minutes),
                            value = stringResource(
                                R.string.settings_stat_minutes_value,
                                userProfile.totalMinutes
                            )
                        )
                        StatTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.CloudDone,
                            label = stringResource(R.string.settings_stat_sync),
                            value = if (userProfile.isCloudSynced) {
                                stringResource(R.string.settings_stat_sync_active)
                            } else {
                                stringResource(R.string.settings_stat_sync_local)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Audio & Calling Engine Section
            SectionHeader(text = stringResource(R.string.settings_section_audio))
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.glassBorder,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    SettingToggleRow(
                        icon = Icons.Default.GraphicEq,
                        title = stringResource(R.string.settings_hd_audio_title),
                        subtitle = stringResource(R.string.settings_hd_audio_subtitle),
                        checked = uiState.hdAudioQuality,
                        onCheckedChange = viewModel::toggleHdAudio
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SettingToggleRow(
                        icon = Icons.Default.Headset,
                        title = stringResource(R.string.settings_noise_title),
                        subtitle = stringResource(R.string.settings_noise_subtitle),
                        checked = uiState.noiseReduction,
                        onCheckedChange = viewModel::toggleNoiseReduction
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SettingToggleRow(
                        icon = Icons.Default.VolumeUp,
                        title = stringResource(R.string.settings_speaker_title),
                        subtitle = stringResource(R.string.settings_speaker_subtitle),
                        checked = uiState.autoSpeakerphone,
                        onCheckedChange = viewModel::toggleAutoSpeakerphone
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // VoIP Audio Codec (G.711 PCMA / PCMU) Section
            SectionHeader(text = stringResource(R.string.settings_section_codec))
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.glassBorder,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_codec_hint),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val selectedCodec = userProfile.preferredCodec

                    // Option 1: Auto Dual Stack
                    CodecOptionCard(
                        title = stringResource(R.string.settings_codec_auto_title),
                        subtitle = stringResource(R.string.settings_codec_auto_subtitle),
                        isSelected = selectedCodec == "G711_AUTO" || selectedCodec.isEmpty(),
                        badge = stringResource(R.string.settings_codec_auto_badge),
                        onClick = { viewModel.setPreferredCodec("G711_AUTO") }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 2: G.711u (PCMU)
                    CodecOptionCard(
                        title = stringResource(R.string.settings_codec_pcmu_title),
                        subtitle = stringResource(R.string.settings_codec_pcmu_subtitle),
                        isSelected = selectedCodec == "G711U",
                        badge = stringResource(R.string.settings_codec_pcmu_badge),
                        onClick = { viewModel.setPreferredCodec("G711U") }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 3: G.711a (PCMA)
                    CodecOptionCard(
                        title = stringResource(R.string.settings_codec_pcma_title),
                        subtitle = stringResource(R.string.settings_codec_pcma_subtitle),
                        isSelected = selectedCodec == "G711A",
                        badge = stringResource(R.string.settings_codec_pcma_badge),
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
                SectionHeader(
                    text = stringResource(R.string.settings_section_sip),
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = viewModel::showEditSipDialog,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("configure_sip_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.settings_edit_sip_trunk),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.settings_configure_trunk),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.glassBorder,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val notConfigured = stringResource(R.string.settings_not_configured)
                    val noValue = stringResource(R.string.settings_value_none)
                    val dynamicCid = stringResource(R.string.settings_cid_dynamic)
                    val host = sip?.host?.takeIf { it.isNotEmpty() }
                    val username = sip?.username?.takeIf { it.isNotEmpty() }

                    InfoStatusRow(
                        icon = Icons.Default.Phone,
                        title = stringResource(R.string.settings_sip_host),
                        badge = host ?: notConfigured,
                        tone = if (host == null) BadgeTone.Error else BadgeTone.Success
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoStatusRow(
                        icon = Icons.Default.AccountCircle,
                        title = stringResource(R.string.settings_sip_username),
                        badge = username ?: notConfigured,
                        tone = if (username == null) BadgeTone.Error else BadgeTone.Success
                    )
                    if (sip?.needsPassword() == true || regState.needsPassword) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.settings_sip_password_required),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("sip_password_required_notice")
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoStatusRow(
                        icon = Icons.Default.PhoneAndroid,
                        title = stringResource(R.string.settings_sip_port_device),
                        badge = stringResource(
                            R.string.settings_sip_port_device_value,
                            sip?.port ?: 5060,
                            sip?.deviceId?.takeIf { it.isNotEmpty() } ?: noValue
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoStatusRow(
                        icon = Icons.Default.Call,
                        title = stringResource(R.string.settings_selected_cid),
                        badge = userProfile.selectedCallerId.takeIf { it.isNotEmpty() }
                            ?: dynamicCid
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = viewModel::showEditSipDialog,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.settings_test_configure_sip),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                SectionHeader(
                    text = stringResource(R.string.settings_section_register),
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = viewModel::refreshRegistration,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("refresh_sip_registration_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(
                            R.string.settings_refresh_registration
                        ),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.settings_refresh_lease),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.glassBorder,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Status Badge Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            val dotColor = when {
                                regState.needsPassword -> MaterialTheme.colorScheme.error
                                regState.status == RegistrationStatus.REGISTERED ->
                                    MaterialTheme.colorScheme.success
                                regState.status == RegistrationStatus.REGISTERING ||
                                    regState.status == RegistrationStatus.UNREGISTERING ->
                                    MaterialTheme.colorScheme.primary
                                regState.status == RegistrationStatus.AUTHENTICATING ||
                                    regState.status == RegistrationStatus.RETRYING ->
                                    MaterialTheme.colorScheme.warning
                                regState.status == RegistrationStatus.FAILED ->
                                    MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.outline
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(dotColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = regState.formattedStatus,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        val isRegistrationFailure = regState.needsPassword ||
                            regState.status == RegistrationStatus.FAILED
                        val isRetryOrAuth = regState.status == RegistrationStatus.AUTHENTICATING ||
                            regState.status == RegistrationStatus.RETRYING
                        val badgeBg = when {
                            isRegistrationFailure -> MaterialTheme.colorScheme.errorContainer
                            regState.status == RegistrationStatus.REGISTERED ->
                                MaterialTheme.colorScheme.successContainer
                            isRetryOrAuth -> MaterialTheme.colorScheme.warningContainer
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                        val badgeText = when {
                            regState.needsPassword ->
                                stringResource(R.string.settings_badge_password_required)
                            regState.status == RegistrationStatus.REGISTERED ->
                                stringResource(R.string.settings_badge_registered)
                            regState.status == RegistrationStatus.AUTHENTICATING ->
                                stringResource(R.string.settings_badge_authenticating)
                            regState.status == RegistrationStatus.REGISTERING ->
                                stringResource(R.string.settings_badge_registering)
                            regState.status == RegistrationStatus.RETRYING && regState.retryAfterSeconds > 0 ->
                                stringResource(R.string.settings_badge_retrying, regState.retryAfterSeconds)
                            regState.status == RegistrationStatus.RETRYING ->
                                stringResource(R.string.settings_badge_retrying_short)
                            regState.status == RegistrationStatus.FAILED && regState.statusCode > 0 ->
                                stringResource(R.string.settings_badge_error, regState.statusCode)
                            regState.status == RegistrationStatus.FAILED ->
                                stringResource(R.string.settings_badge_auth_failed)
                            regState.status == RegistrationStatus.EXPIRED ->
                                stringResource(R.string.settings_badge_expired)
                            else -> stringResource(R.string.settings_badge_standby)
                        }
                        val badgeColor = when {
                            isRegistrationFailure -> MaterialTheme.colorScheme.onErrorContainer
                            regState.status == RegistrationStatus.REGISTERED ->
                                MaterialTheme.colorScheme.onSuccessContainer
                            isRetryOrAuth -> MaterialTheme.colorScheme.onWarningContainer
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeBg
                        ) {
                            Text(
                                text = badgeText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = badgeColor,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val hasDigestAccount = regState.username.isNotBlank() &&
                        regState.host.isNotBlank()
                    InfoStatusRow(
                        icon = Icons.Default.VpnKey,
                        title = stringResource(R.string.settings_digest_account),
                        badge = if (hasDigestAccount) {
                            stringResource(
                                R.string.settings_digest_account_value,
                                regState.username,
                                regState.host
                            )
                        } else {
                            stringResource(R.string.settings_not_configured)
                        },
                        tone = if (hasDigestAccount) BadgeTone.Success else BadgeTone.Error
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    InfoStatusRow(
                        icon = Icons.Default.HourglassTop,
                        title = stringResource(R.string.settings_registration_expiry),
                        badge = if (regState.isRegistered) {
                            stringResource(
                                R.string.settings_expiry_active,
                                regState.secondsRemaining,
                                regState.expiresSeconds
                            )
                        } else {
                            stringResource(
                                R.string.settings_expiry_requested,
                                regState.expiresSeconds
                            )
                        },
                        tone = if (regState.isRegistered) BadgeTone.Success else BadgeTone.Neutral
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    InfoStatusRow(
                        icon = Icons.Default.NetworkCheck,
                        title = stringResource(R.string.settings_keepalive),
                        badge = if (regState.isKeepAliveActive) {
                            stringResource(
                                R.string.settings_keepalive_active,
                                regState.keepAlivePingsSent
                            )
                        } else {
                            stringResource(R.string.settings_badge_standby)
                        },
                        tone = if (regState.isKeepAliveActive) {
                            BadgeTone.Success
                        } else {
                            BadgeTone.Neutral
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    InfoStatusRow(
                        icon = Icons.Default.SignalCellularAlt,
                        title = stringResource(R.string.settings_server_banner),
                        badge = regState.serverBanner.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.settings_value_none)
                    )

                    regState.lastError?.let { error ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.settings_notice, error),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
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
                                .height(48.dp)
                                .testTag("refresh_lease_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.settings_refresh_lease),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = viewModel::forceReRegister,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("reauth_sip_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.settings_reauthenticate),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                SectionHeader(
                    text = stringResource(R.string.settings_section_sdp),
                    modifier = Modifier.weight(1f)
                )
                sdpDump?.let { dump ->
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = stringResource(R.string.settings_sdp_captured, dump.statusCode),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            maxLines = 1
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.glassBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .testTag("sdp_diagnostics_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val currentDump = sdpDump
                    if (currentDump != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(
                                        R.string.settings_sdp_dump_title,
                                        currentDump.attemptNumber
                                    ),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(
                                        R.string.settings_sdp_dump_meta,
                                        currentDump.formattedTime,
                                        currentDump.cSeq
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = viewModel::copySdpDumpToClipboard,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = stringResource(R.string.settings_copy_sdp),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                // Log console: deliberately a fixed dark terminal
                                // in both themes so the dump reads as raw output.
                                .background(Ink950)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = currentDump.formattedReport,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                ),
                                color = TerminalGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = viewModel::copySdpDumpToClipboard,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.settings_copy_sdp_report),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        InfoStatusRow(
                            icon = Icons.Default.GraphicEq,
                            title = stringResource(R.string.settings_sdp_profile),
                            badge = stringResource(R.string.settings_sdp_profile_value)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        InfoStatusRow(
                            icon = Icons.Default.Security,
                            title = stringResource(R.string.settings_srtp),
                            badge = stringResource(R.string.settings_srtp_value)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        InfoStatusRow(
                            icon = Icons.Default.Code,
                            title = stringResource(R.string.settings_codec_policy),
                            badge = stringResource(R.string.settings_codec_policy_value)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.settings_sdp_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Realtime Sync & Telecom Security Section
            SectionHeader(text = stringResource(R.string.settings_section_integration))
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.glassBorder,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoStatusRow(
                        icon = Icons.Default.CloudDone,
                        title = stringResource(R.string.settings_rtdb_endpoint),
                        badge = stringResource(
                            R.string.settings_rtdb_endpoint_value,
                            userProfile.uid.take(8)
                        ),
                        tone = if (userProfile.isCloudSynced) {
                            BadgeTone.Success
                        } else {
                            BadgeTone.Neutral
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoStatusRow(
                        icon = Icons.Default.PhoneAndroid,
                        title = stringResource(R.string.settings_telecom),
                        badge = stringResource(R.string.settings_telecom_value),
                        tone = BadgeTone.Success
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoStatusRow(
                        icon = Icons.Default.Lock,
                        title = stringResource(R.string.settings_encryption),
                        badge = stringResource(R.string.settings_encryption_value)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoStatusRow(
                        icon = Icons.Default.Security,
                        title = stringResource(R.string.settings_privacy),
                        badge = stringResource(R.string.settings_privacy_value)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Logout Button
            Button(
                onClick = { showSignOutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("settings_logout_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.settings_sign_out_operator),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        if (showSignOutDialog) {
            AlertDialog(
                onDismissRequest = { showSignOutDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
                title = {
                    Text(
                        text = stringResource(R.string.settings_sign_out_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.settings_sign_out_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSignOutDialog = false
                            onLogout()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.testTag("confirm_sign_out_button")
                    ) {
                        Text(
                            text = stringResource(R.string.action_sign_out),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutDialog = false }) {
                        Text(text = stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        // Edit Profile Realtime Dialog
        if (uiState.isEditProfileDialogVisible) {
            AlertDialog(
                onDismissRequest = viewModel::hideEditProfileDialog,
                title = {
                    Text(
                        text = stringResource(R.string.settings_edit_profile_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .imePadding()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_edit_profile_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        AppTextField(
                            value = uiState.editDisplayName,
                            onValueChange = viewModel::onEditDisplayNameChanged,
                            label = stringResource(R.string.settings_field_display_name),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_display_name_input")
                        )

                        AppTextField(
                            value = uiState.editOrganization,
                            onValueChange = viewModel::onEditOrganizationChanged,
                            label = stringResource(R.string.settings_field_organization),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_organization_input")
                        )

                        AppTextField(
                            value = uiState.editAccountRole,
                            onValueChange = viewModel::onEditAccountRoleChanged,
                            label = stringResource(R.string.settings_field_role),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_role_input")
                        )

                        AppTextField(
                            value = uiState.editPhoneNumber,
                            onValueChange = viewModel::onEditPhoneNumberChanged,
                            label = stringResource(R.string.settings_field_phone),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { viewModel.saveProfile() }
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_phone_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = viewModel::saveProfile,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("save_profile_button")
                    ) {
                        Text(
                            text = stringResource(R.string.settings_save_sync),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = viewModel::hideEditProfileDialog,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = stringResource(R.string.action_cancel))
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
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
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.settings_sip_dialog_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .imePadding()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_sip_dialog_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Live SIP Connection Test Card / Status Banner
                        if (uiState.isTestingSip) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.settings_sip_testing),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        } else if (uiState.sipTestResult != null) {
                            val res = uiState.sipTestResult!!
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (res.isSuccess) {
                                            MaterialTheme.colorScheme.successContainer
                                        } else {
                                            MaterialTheme.colorScheme.errorContainer
                                        }
                                    )
                                    .padding(12.dp)
                            ) {
                                val resultColor = if (res.isSuccess) {
                                    MaterialTheme.colorScheme.onSuccessContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                }
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (res.isSuccess) {
                                                Icons.Default.CheckCircle
                                            } else {
                                                Icons.Default.Close
                                            },
                                            contentDescription = null,
                                            tint = resultColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (res.isSuccess) {
                                                stringResource(R.string.settings_sip_test_success)
                                            } else {
                                                stringResource(R.string.settings_sip_test_notice)
                                            },
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = resultColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(
                                            R.string.settings_sip_test_detail,
                                            res.message,
                                            res.latencyMs
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = resultColor
                                    )
                                    if (res.serverBanner.isNotBlank()) {
                                        Text(
                                            text = stringResource(
                                                R.string.settings_sip_test_server,
                                                res.serverBanner
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = resultColor
                                        )
                                    }
                                }
                            }
                        }

                        AppTextField(
                            value = uiState.editSipHost,
                            onValueChange = viewModel::onEditSipHostChanged,
                            label = stringResource(R.string.settings_field_sip_host),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_sip_host_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppTextField(
                                value = uiState.editSipPort,
                                onValueChange = viewModel::onEditSipPortChanged,
                                label = stringResource(R.string.settings_field_sip_port),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("edit_sip_port_input")
                            )

                            AppTextField(
                                value = uiState.editSipDeviceId,
                                onValueChange = viewModel::onEditSipDeviceIdChanged,
                                label = stringResource(R.string.settings_field_sip_device),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("edit_sip_device_input")
                            )
                        }

                        AppTextField(
                            value = uiState.editSipUsername,
                            onValueChange = viewModel::onEditSipUsernameChanged,
                            label = stringResource(R.string.settings_field_sip_username),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_sip_user_input")
                        )

                        var isSipPasswordVisible by remember { mutableStateOf(false) }
                        AppTextField(
                            value = uiState.editSipPassword,
                            onValueChange = viewModel::onEditSipPasswordChanged,
                            label = stringResource(R.string.settings_field_sip_password),
                            supportingText = if (
                                userProfile.sipConfig?.needsPassword() == true ||
                                uiState.editSipPassword.isBlank()
                            ) {
                                stringResource(R.string.settings_sip_password_support)
                            } else {
                                null
                            },
                            visualTransformation = if (isSipPasswordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { isSipPasswordVisible = !isSipPasswordVisible }
                                ) {
                                    Icon(
                                        imageVector = if (isSipPasswordVisible) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                        contentDescription = if (isSipPasswordVisible) {
                                            stringResource(R.string.auth_password_hide)
                                        } else {
                                            stringResource(R.string.auth_password_show)
                                        }
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_sip_pass_input")
                        )

                        AppTextField(
                            value = uiState.editSipCallerId,
                            onValueChange = viewModel::onEditSipCallerIdChanged,
                            label = stringResource(R.string.settings_field_sip_cid),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { viewModel.saveSipConfig() }
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_sip_cid_input")
                        )

                        OutlinedButton(
                            onClick = viewModel::testSipConnection,
                            enabled = !uiState.isTestingSip,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("test_sip_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isTestingSip) {
                                    stringResource(R.string.settings_test_sip_testing)
                                } else {
                                    stringResource(R.string.settings_test_sip_probe)
                                },
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = viewModel::saveSipConfig,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("save_sip_button")
                    ) {
                        Text(
                            text = stringResource(R.string.settings_save_trunk),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = viewModel::hideEditSipDialog,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = stringResource(R.string.action_cancel))
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Section label above each settings card group. */
@Composable
private fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
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
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
            .heightIn(min = 48.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedBorderColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

/**
 * Tone of an [InfoStatusRow] badge. Diagnostics rows used to render every value
 * on a green success chip, which read as "healthy" even for `Not configured`.
 */
private enum class BadgeTone { Neutral, Success, Error }

@Composable
private fun InfoStatusRow(
    icon: ImageVector,
    title: String,
    badge: String,
    tone: BadgeTone = BadgeTone.Neutral
) {
    val badgeBackground: Color
    val badgeContent: Color
    when (tone) {
        BadgeTone.Success -> {
            badgeBackground = MaterialTheme.colorScheme.successContainer
            badgeContent = MaterialTheme.colorScheme.onSuccessContainer
        }
        BadgeTone.Error -> {
            badgeBackground = MaterialTheme.colorScheme.errorContainer
            badgeContent = MaterialTheme.colorScheme.onErrorContainer
        }
        BadgeTone.Neutral -> {
            badgeBackground = MaterialTheme.colorScheme.surfaceVariant
            badgeContent = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

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
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .clip(RoundedCornerShape(8.dp))
                .background(badgeBackground)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = badgeContent,
                fontSize = 10.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
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
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
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
                        selectedColor = MaterialTheme.colorScheme.primary,
                        unselectedColor = MaterialTheme.colorScheme.outline
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        }
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}

