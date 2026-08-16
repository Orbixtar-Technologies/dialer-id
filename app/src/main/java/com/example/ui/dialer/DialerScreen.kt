package com.example.ui.dialer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CallerIdItem
import com.example.data.model.Contact
import com.example.service.sip.RegistrationStatus
import com.example.service.sip.SipRegistrationState
import com.example.ui.common.CountryUtils
import com.example.ui.contacts.ContactActionsSheet
import com.example.ui.contacts.ContactAvatar
import com.example.ui.contacts.ContactsPermissionNotice
import com.example.ui.contacts.ContactsPermissionStatus
import com.example.ui.contacts.ContactsViewModel
import com.example.ui.contacts.rememberContactsPermissionState
import com.example.ui.theme.onSuccess
import com.example.ui.theme.onSuccessContainer
import com.example.ui.theme.onWarning
import com.example.ui.theme.onWarningContainer
import com.example.ui.theme.success
import com.example.ui.theme.warning
import com.example.ui.theme.warningContainer

@Composable
fun DialerScreen(
    viewModel: DialerViewModel,
    contactsViewModel: ContactsViewModel,
    onNavigateToDeposit: () -> Unit,
    onNavigateToCallerIds: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToContacts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val regState by viewModel.registrationState.collectAsState()
    val callerIds by viewModel.callerIds.collectAsState()
    val recentDestinations by viewModel.recentDestinations.collectAsState()
    val contactMatches by contactsViewModel.dialerSuggestions.collectAsState()
    val favoriteContacts by contactsViewModel.favorites.collectAsState()
    val haptic = LocalHapticFeedback.current

    val contactsPermission = rememberContactsPermissionState(
        onGrantedChanged = { granted -> contactsViewModel.onPermissionResult(granted) }
    )

    var showCallerIdSheet by remember { mutableStateOf(false) }
    var contactSheetTarget by remember { mutableStateOf<Contact?>(null) }

    LaunchedEffect(Unit) {
        viewModel.onScreenOpened()
    }

    LaunchedEffect(uiState.inputNumber) {
        contactsViewModel.onDialerInputChanged(uiState.inputNumber)
    }

    val hasZeroBalance = userProfile.creditBalance <= 0.0
    val isTestNumber = uiState.inputNumber == "3200" || uiState.inputNumber == "444"
    val isCallAllowed = (!hasZeroBalance || isTestNumber) && regState.isRegistered
    val hasSipCredentials = userProfile.sipConfig?.hasUsableCredentials() == true

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Flexible upper area: status, caller ID selector and quick dial.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                RegistrationStatusPill(
                    regState = regState,
                    showRetry = hasSipCredentials && !regState.isRegistered,
                    onRetry = { viewModel.retryRegistration() }
                )

                CallerIdSelector(
                    selectedNumber = userProfile.selectedCallerId,
                    selectedLabel = callerIds
                        .firstOrNull { it.phoneNumber == userProfile.selectedCallerId }
                        ?.label
                        .orEmpty(),
                    onClick = {
                        if (callerIds.isEmpty()) onNavigateToCallerIds() else showCallerIdSheet = true
                    }
                )

                DialerContactsSection(
                    permissionStatus = contactsPermission.status,
                    dialedDigits = uiState.inputNumber,
                    matches = contactMatches,
                    favorites = favoriteContacts,
                    isCallEnabled = isCallAllowed,
                    onRequestPermission = { contactsPermission.request() },
                    onOpenSettings = { contactsPermission.openAppSettings() },
                    onViewAll = onNavigateToContacts,
                    onSelectContact = { contact ->
                        val number = contact.primaryNumber
                        if (contact.hasMultipleNumbers || number == null) {
                            contactSheetTarget = contact
                        } else {
                            viewModel.onNumberChanged(number.dialNumber)
                        }
                    },
                    onCallContact = { contact ->
                        val number = contact.primaryNumber
                        if (contact.hasMultipleNumbers || number == null) {
                            contactSheetTarget = contact
                        } else {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.placeCallTo(number.dialNumber)
                        }
                    }
                )

                QuickDialSection(
                    recents = recentDestinations,
                    onSelect = { viewModel.onNumberChanged(it.number) },
                    onViewAll = onNavigateToHistory
                )

                Spacer(modifier = Modifier.height(4.dp))
            }

            // Bottom-anchored dial area.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                NumberDisplay(formattedNumber = uiState.formattedDisplay)

                Spacer(modifier = Modifier.height(6.dp))

                Keypad(
                    onDigit = { digit ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.appendDigit(digit)
                    },
                    onPlusLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.appendDigit('+')
                    },
                    onBackspace = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.onBackspace()
                    },
                    onClearAll = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.onClearAll()
                    }
                )

                AnimatedVisibility(
                    visible = uiState.showZeroBalanceWarning ||
                        (hasZeroBalance && !isTestNumber && uiState.inputNumber.isNotEmpty()),
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    ZeroBalanceBanner(onTopUp = onNavigateToDeposit)
                }

                Spacer(modifier = Modifier.height(14.dp))

                CallActionButton(
                    isCallAllowed = isCallAllowed,
                    isRegistered = regState.isRegistered,
                    hasNumber = uiState.inputNumber.isNotEmpty(),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.placeCall()
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    if (showCallerIdSheet) {
        CallerIdSheet(
            callerIds = callerIds,
            selectedNumber = userProfile.selectedCallerId,
            onSelect = { item ->
                viewModel.selectCallerId(item)
                showCallerIdSheet = false
            },
            onManage = {
                showCallerIdSheet = false
                onNavigateToCallerIds()
            },
            onDismiss = { showCallerIdSheet = false }
        )
    }

    contactSheetTarget?.let { contact ->
        ContactActionsSheet(
            contact = contact,
            isCallEnabled = isCallAllowed,
            onCall = { number ->
                contactSheetTarget = null
                viewModel.placeCallTo(number)
            },
            onFillDialer = { number ->
                contactSheetTarget = null
                viewModel.onNumberChanged(number)
            },
            onDismiss = { contactSheetTarget = null }
        )
    }
}

/**
 * Address book presence on the dialer.
 *
 * While digits are being typed this is a smart-dial match list; with an empty
 * input it falls back to starred contacts, and to a plain entry point when the
 * user has starred nobody. Everything here is real `ContactsContract` data.
 */
@Composable
private fun DialerContactsSection(
    permissionStatus: ContactsPermissionStatus,
    dialedDigits: String,
    matches: List<Contact>,
    favorites: List<Contact>,
    isCallEnabled: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onViewAll: () -> Unit,
    onSelectContact: (Contact) -> Unit,
    onCallContact: (Contact) -> Unit
) {
    val isGranted = permissionStatus == ContactsPermissionStatus.GRANTED
    val isSmartDialing =
        dialedDigits.trimStart('+').length >= ContactsViewModel.MIN_SMART_DIAL_DIGITS

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isGranted && !isSmartDialing && favorites.isNotEmpty()) {
                    stringResource(R.string.dialer_contacts_favorites)
                } else {
                    stringResource(R.string.dialer_contacts_title)
                },
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isGranted) {
                TextButton(
                    onClick = onViewAll,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("dialer_contacts_view_all")
                ) {
                    Text(
                        text = stringResource(R.string.dialer_contacts_view_all),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        when {
            !isGranted -> ContactsPermissionNotice(
                status = permissionStatus,
                onRequest = onRequestPermission,
                onOpenSettings = onOpenSettings,
                compact = true
            )

            isSmartDialing && matches.isNotEmpty() -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dialer_contact_matches"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // A plain Column keeps this inside the scrollable upper area:
                // a nested lazy list would fight the parent vertical scroll.
                matches.forEach { contact ->
                    ContactMatchRow(
                        contact = contact,
                        isCallEnabled = isCallEnabled,
                        onClick = { onSelectContact(contact) },
                        onCall = { onCallContact(contact) }
                    )
                }
            }

            isSmartDialing -> Text(
                text = stringResource(R.string.dialer_contacts_no_matches),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp, top = 2.dp, bottom = 2.dp)
            )

            favorites.isNotEmpty() -> LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dialer_favorites_row"),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(favorites, key = { it.id }) { contact ->
                    FavoriteContactCard(
                        contact = contact,
                        onClick = { onSelectContact(contact) }
                    )
                }
            }

            else -> BrowseContactsCard(onClick = onViewAll)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactMatchRow(
    contact: Contact,
    isCallEnabled: Boolean,
    onClick: () -> Unit,
    onCall: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dialer_contact_match_${contact.id}"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatar(
                name = contact.displayName,
                photoUri = contact.photoThumbnailUri ?: contact.photoUri,
                size = 36.dp
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = contact.primaryNumber?.displayNumber.orEmpty(),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onCall,
                enabled = isCallEnabled,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCallEnabled) {
                            MaterialTheme.colorScheme.success
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .testTag("dialer_contact_call_${contact.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = stringResource(R.string.contacts_call),
                    tint = if (isCallEnabled) {
                        MaterialTheme.colorScheme.onSuccess
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoriteContactCard(
    contact: Contact,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(80.dp)
            .testTag("dialer_favorite_${contact.id}"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ContactAvatar(
                name = contact.displayName,
                photoUri = contact.photoThumbnailUri ?: contact.photoUri,
                size = 38.dp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = contact.displayName,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowseContactsCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dialer_contacts_browse"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Contacts,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.dialer_contacts_browse_title),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.dialer_contacts_browse_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RegistrationStatusPill(
    regState: SipRegistrationState,
    showRetry: Boolean,
    onRetry: () -> Unit
) {
    // Text and dot share one accent so the pill reads as a single status, and
    // the accent is always a role that clears AA on the screen background.
    val dot = when {
        regState.needsPassword -> MaterialTheme.colorScheme.error
        regState.status == RegistrationStatus.REGISTERED -> MaterialTheme.colorScheme.success
        regState.status == RegistrationStatus.REGISTERING -> MaterialTheme.colorScheme.primary
        regState.status == RegistrationStatus.AUTHENTICATING || regState.status == RegistrationStatus.RETRYING ->
            MaterialTheme.colorScheme.warning
        regState.status == RegistrationStatus.FAILED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    // The label needs a darker tone than the dot: the dot only has to clear 3:1
    // as a graphic, the 12sp label has to clear 4.5:1 on the tinted pill.
    val accent = when {
        regState.status == RegistrationStatus.REGISTERED -> MaterialTheme.colorScheme.onSuccessContainer
        regState.status == RegistrationStatus.AUTHENTICATING || regState.status == RegistrationStatus.RETRYING ->
            MaterialTheme.colorScheme.onWarningContainer
        else -> dot
    }
    val statusText = when {
        regState.isRegistered -> stringResource(R.string.dialer_status_registered)
        regState.needsPassword -> stringResource(R.string.dialer_status_password_required)
        regState.status == RegistrationStatus.AUTHENTICATING ->
            stringResource(R.string.dialer_status_authenticating)
        regState.status == RegistrationStatus.REGISTERING ->
            stringResource(R.string.dialer_status_registering)
        regState.status == RegistrationStatus.RETRYING && regState.retryAfterSeconds > 0 ->
            stringResource(R.string.dialer_status_retrying_in, regState.retryAfterSeconds)
        regState.status == RegistrationStatus.RETRYING ->
            stringResource(R.string.dialer_status_retrying)
        regState.status == RegistrationStatus.UNREGISTERING ->
            stringResource(R.string.dialer_status_unregistering)
        regState.status == RegistrationStatus.FAILED && regState.statusCode > 0 ->
            stringResource(R.string.dialer_status_failed_code, regState.statusCode)
        regState.status == RegistrationStatus.FAILED -> stringResource(
            R.string.dialer_status_failed,
            regState.lastError ?: stringResource(R.string.dialer_registration_failed)
        )
        else -> stringResource(R.string.dialer_status_not_registered)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = dot.copy(alpha = 0.12f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(dot, CircleShape)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("dialer_status_text")
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (showRetry) {
            TextButton(
                onClick = onRetry,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("dialer_retry_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.dialer_retry_registration),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallerIdSelector(
    selectedNumber: String,
    selectedLabel: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dialer_caller_id_chip"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Badge,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.dialer_caller_id_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (selectedNumber.isEmpty()) {
                    Text(
                        text = stringResource(R.string.dialer_caller_id_none),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = selectedNumber,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (selectedLabel.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.UnfoldMore,
                contentDescription = stringResource(R.string.dialer_caller_id_change),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallerIdSheet(
    callerIds: List<CallerIdItem>,
    selectedNumber: String,
    onSelect: (CallerIdItem) -> Unit,
    onManage: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.dialer_caller_id_sheet_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.dialer_caller_id_sheet_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            callerIds.forEach { item ->
                val isSelected = item.phoneNumber == selectedNumber
                Surface(
                    onClick = { onSelect(item) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.phoneNumber,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (item.isVerified) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = stringResource(R.string.dialer_caller_id_verified),
                                        tint = MaterialTheme.colorScheme.success,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            TextButton(
                onClick = onManage,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.dialer_caller_id_manage),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun QuickDialSection(
    recents: List<RecentDestination>,
    onSelect: (RecentDestination) -> Unit,
    onViewAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.dialer_recents_title),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            if (recents.isNotEmpty()) {
                TextButton(
                    onClick = onViewAll,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("dialer_recents_view_all")
                ) {
                    Text(
                        text = stringResource(R.string.dialer_recents_view_all),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (recents.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.dialer_recents_empty),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.dialer_recents_empty_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dialer_recents_row"),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(recents, key = { it.number }) { item ->
                    QuickDialCard(item = item, onClick = { onSelect(item) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickDialCard(
    item: RecentDestination,
    onClick: () -> Unit
) {
    val formattedNumber = remember(item.number) { CountryUtils.formatPhoneNumber(item.number) }

    Surface(
        onClick = onClick,
        modifier = Modifier.width(140.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CallMade,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formattedNumber,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = relativeTimeLabel(item.lastCalledAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    if (item.callCount > 1) {
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.dialer_recents_repeat_count, item.callCount),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun relativeTimeLabel(timestamp: Long): String {
    val elapsedMinutes = remember(timestamp) {
        ((System.currentTimeMillis() - timestamp).coerceAtLeast(0L)) / 60_000L
    }
    return when {
        elapsedMinutes < 1L -> stringResource(R.string.dialer_recent_just_now)
        elapsedMinutes < 60L -> stringResource(R.string.dialer_recent_minutes_ago, elapsedMinutes.toInt())
        elapsedMinutes < 1_440L ->
            stringResource(R.string.dialer_recent_hours_ago, (elapsedMinutes / 60L).toInt())
        else -> stringResource(R.string.dialer_recent_days_ago, (elapsedMinutes / 1_440L).toInt())
    }
}

@Composable
private fun NumberDisplay(formattedNumber: String) {
    val isEmpty = formattedNumber.isEmpty()
    val fontSize = when {
        formattedNumber.length <= 12 -> 34.sp
        formattedNumber.length <= 17 -> 28.sp
        else -> 22.sp
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 66.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isEmpty) stringResource(R.string.dialer_enter_number) else formattedNumber,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = if (isEmpty) 22.sp else fontSize,
                letterSpacing = 0.8.sp
            ),
            color = if (isEmpty) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("dialer_display_text")
        )
    }
}

@Composable
private fun Keypad(
    onDigit: (Char) -> Unit,
    onPlusLongPress: () -> Unit,
    onBackspace: () -> Unit,
    onClearAll: () -> Unit
) {
    val rows = listOf(
        listOf(KeypadEntry('1', ""), KeypadEntry('2', "ABC"), KeypadEntry('3', "DEF")),
        listOf(KeypadEntry('4', "GHI"), KeypadEntry('5', "JKL"), KeypadEntry('6', "MNO")),
        listOf(KeypadEntry('7', "PQRS"), KeypadEntry('8', "TUV"), KeypadEntry('9', "WXYZ"))
    )

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val gap = 14.dp
        val keySize = ((maxWidth - gap * 2) / 3).coerceAtMost(74.dp)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(gap),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally)
                ) {
                    row.forEach { entry ->
                        KeypadKey(
                            label = entry.digit.toString(),
                            subLabel = entry.subText,
                            size = keySize,
                            testTag = "keypad_digit_${entry.digit}",
                            onClick = { onDigit(entry.digit) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally)
            ) {
                KeypadKey(
                    label = "+",
                    subLabel = "",
                    size = keySize,
                    testTag = "keypad_digit_+",
                    onClick = { onDigit('+') }
                )
                KeypadKey(
                    label = "0",
                    subLabel = "+",
                    size = keySize,
                    testTag = "keypad_digit_0",
                    onClick = { onDigit('0') },
                    onLongClick = onPlusLongPress
                )
                KeypadKey(
                    label = "",
                    subLabel = "",
                    size = keySize,
                    testTag = "dialer_backspace_button",
                    icon = Icons.AutoMirrored.Filled.Backspace,
                    iconDescription = stringResource(R.string.dialer_backspace),
                    onClick = onBackspace,
                    onLongClick = onClearAll
                )
            }
        }
    }
}

private data class KeypadEntry(val digit: Char, val subText: String)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeypadKey(
    label: String,
    subLabel: String,
    size: Dp,
    testTag: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    icon: ImageVector? = null,
    iconDescription: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        label = "keypad_key_scale"
    )

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = iconDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 27.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subLabel.isNotEmpty()) {
                    Text(
                        text = subLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 9.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ZeroBalanceBanner(onTopUp: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.warningContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.warning.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onWarningContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.dialer_zero_balance_title),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onWarningContainer
                )
                Text(
                    text = stringResource(R.string.dialer_zero_balance_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onWarningContainer
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Button(
                onClick = onTopUp,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.warning,
                    contentColor = MaterialTheme.colorScheme.onWarning
                ),
                contentPadding = ButtonDefaults.TextButtonContentPadding
            ) {
                Text(
                    text = stringResource(R.string.dialer_zero_balance_action),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CallActionButton(
    isCallAllowed: Boolean,
    isRegistered: Boolean,
    hasNumber: Boolean,
    onClick: () -> Unit
) {
    val isEnabled = hasNumber && isCallAllowed

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(
                elevation = if (isEnabled) 8.dp else 0.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = MaterialTheme.colorScheme.success.copy(alpha = 0.45f)
            )
            .testTag("dialer_call_button"),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.success,
            contentColor = MaterialTheme.colorScheme.onSuccess,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        enabled = isEnabled
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isCallAllowed) Icons.Default.Call else Icons.Default.Lock,
                contentDescription = stringResource(R.string.dialer_place_call),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = when {
                    !isRegistered -> stringResource(R.string.dialer_not_registered)
                    !isCallAllowed -> stringResource(R.string.dialer_top_up_to_call)
                    else -> stringResource(R.string.dialer_start_call)
                },
                // 18sp bold keeps white-on-green in WCAG large-text territory,
                // where the 3:1 floor applies instead of 4.5:1.
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
