package com.example.ui.contacts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Contact
import com.example.ui.common.AppTextField
import com.example.ui.common.SignalEmptyState
import com.example.ui.theme.DialerElevation
import com.example.ui.theme.glassBorder
import com.example.ui.theme.isLightScheme
import com.example.ui.theme.onSuccess
import com.example.ui.theme.success

/**
 * Full address book browser backed by `ContactsContract`.
 *
 * @param isCallEnabled mirrors the dialer gate (SIP registered and funded) so a
 *   call is never offered when the underlying call path would refuse it.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel,
    isCallEnabled: Boolean,
    onCallNumber: (String) -> Unit,
    onFillDialer: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val permissionState = rememberContactsPermissionState(
        onGrantedChanged = { granted -> viewModel.onPermissionResult(granted) }
    )

    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    // Opening the screen is the "first use" of contacts, so ask once here and
    // never again if the user says no.
    LaunchedEffect(permissionState.status, permissionState.hasAsked) {
        if (permissionState.status == ContactsPermissionStatus.DENIED && !permissionState.hasAsked) {
            permissionState.request()
        }
    }

    val sections = remember(uiState.contacts) {
        uiState.contacts.groupBy { it.sectionKey }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("contacts_screen"),
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("contacts_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.contacts_back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.contacts_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AppTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                placeholder = stringResource(R.string.contacts_search_hint),
                leadingIcon = Icons.Default.Search,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.contacts_search_clear)
                            )
                        }
                    }
                },
                enabled = permissionState.isGranted,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contacts_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                !permissionState.isGranted -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ContactsPermissionNotice(
                            status = permissionState.status,
                            onRequest = { permissionState.request() },
                            onOpenSettings = { permissionState.openAppSettings() }
                        )
                    }
                }

                uiState.isLoading && uiState.contacts.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.contacts_loading),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                uiState.contacts.isEmpty() -> {
                    val isSearching = uiState.query.isNotBlank()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 60.dp)
                            .testTag("contacts_empty_state"),
                        contentAlignment = Alignment.Center
                    ) {
                        SignalEmptyState(
                            icon = if (isSearching) Icons.Default.SearchOff else Icons.Default.Contacts,
                            title = stringResource(
                                if (isSearching) R.string.contacts_no_results_title
                                else R.string.contacts_empty_title
                            ),
                            body = stringResource(
                                if (isSearching) R.string.contacts_no_results_body
                                else R.string.contacts_empty_body
                            )
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        state = rememberLazyListState(),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("contacts_list"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sections.forEach { (letter, contacts) ->
                            stickyHeader(key = "section_$letter") {
                                SectionHeader(letter = letter)
                            }
                            items(contacts, key = { it.id }) { contact ->
                                ContactRow(
                                    contact = contact,
                                    isCallEnabled = isCallEnabled,
                                    onClick = { selectedContact = contact },
                                    onCall = {
                                        val number = contact.primaryNumber
                                        if (contact.hasMultipleNumbers || number == null) {
                                            selectedContact = contact
                                        } else {
                                            onCallNumber(number.dialNumber)
                                        }
                                    }
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    selectedContact?.let { contact ->
        ContactActionsSheet(
            contact = contact,
            isCallEnabled = isCallEnabled,
            onCall = { number ->
                selectedContact = null
                onCallNumber(number)
            },
            onFillDialer = { number ->
                selectedContact = null
                onFillDialer(number)
            },
            onDismiss = { selectedContact = null }
        )
    }
}

@Composable
private fun SectionHeader(letter: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactRow(
    contact: Contact,
    isCallEnabled: Boolean,
    onClick: () -> Unit,
    onCall: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("contact_row_${contact.id}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (MaterialTheme.colorScheme.isLightScheme) DialerElevation.card else 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.glassBorder)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatar(
                name = contact.displayName,
                photoUri = contact.photoThumbnailUri ?: contact.photoUri,
                size = 44.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = contact.primaryNumber?.displayNumber.orEmpty(),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (contact.hasMultipleNumbers) {
                    Text(
                        text = stringResource(R.string.contacts_numbers_count, contact.numbers.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(
                onClick = onCall,
                enabled = isCallEnabled,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCallEnabled) {
                            MaterialTheme.colorScheme.success
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .testTag("contact_call_${contact.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = stringResource(R.string.contacts_call),
                    tint = if (isCallEnabled) {
                        MaterialTheme.colorScheme.onSuccess
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
