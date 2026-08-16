package com.example.ui.callerid

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CallerIdItem
import com.example.ui.common.AppFormError
import com.example.ui.common.AppTextField
import com.example.ui.theme.onSuccessContainer
import com.example.ui.theme.success

@Composable
fun CallerIdScreen(
    viewModel: CallerIdViewModel,
    modifier: Modifier = Modifier
) {
    val callerIds by viewModel.callerIds.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var pendingDeletion by remember { mutableStateOf<CallerIdItem?>(null) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Outbound Identity",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "The selected caller ID is sent as your outgoing identity when you dial.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Caller IDs (${callerIds.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (callerIds.isNotEmpty()) {
                            Text(
                                text = "Tap star to set primary",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (callerIds.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                                .testTag("caller_id_empty_state"),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Badge,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No outbound caller IDs",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Add a number you control to send it as your caller ID.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(callerIds, key = { it.id }) { item ->
                        CallerIdCard(
                            item = item,
                            onSetPrimary = { viewModel.setPrimary(item) },
                            onDelete = { pendingDeletion = item }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(88.dp))
                }
            }

            FloatingActionButton(
                onClick = { viewModel.showAddDialog(true) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .testTag("add_caller_id_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Caller ID",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }

    if (uiState.showAddDialog) {
        AddCallerIdDialog(
            phoneNumber = uiState.inputPhoneNumber,
            label = uiState.inputLabel,
            isPrimary = uiState.isPrimaryToggle,
            errorMessage = uiState.errorMessage,
            onPhoneNumberChange = viewModel::onPhoneNumberChanged,
            onLabelChange = viewModel::onLabelChanged,
            onPrimaryChange = viewModel::onPrimaryToggleChanged,
            onConfirm = viewModel::addCallerId,
            onDismiss = { viewModel.showAddDialog(false) }
        )
    }

    pendingDeletion?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDeletion = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.testTag("caller_id_delete_dialog"),
            title = {
                Text(
                    text = stringResource(R.string.caller_id_delete_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.caller_id_delete_body, item.phoneNumber),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCallerId(item)
                        pendingDeletion = null
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_caller_id")
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletion = null }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun AddCallerIdDialog(
    phoneNumber: String,
    label: String,
    isPrimary: Boolean,
    errorMessage: String?,
    onPhoneNumberChange: (String) -> Unit,
    onLabelChange: (String) -> Unit,
    onPrimaryChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Add Outbound Caller ID",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            // The dialog window is resized by the IME, so the field list scrolls
            // rather than pushing the Save button off screen.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
            ) {
                Text(
                    text = "Enter the number in E.164 format, e.g. +15551234567.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                AppTextField(
                    value = phoneNumber,
                    onValueChange = onPhoneNumberChange,
                    label = "Phone Number",
                    placeholder = "+15551234567",
                    leadingIcon = Icons.Default.Phone,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_caller_id_number")
                )

                Spacer(modifier = Modifier.height(12.dp))

                AppTextField(
                    value = label,
                    onValueChange = onLabelChange,
                    label = "Label / Department",
                    placeholder = "e.g. Office Direct Line",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_caller_id_label")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Set as primary identity",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isPrimary,
                        onCheckedChange = onPrimaryChange
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AppFormError(
                        message = errorMessage,
                        modifier = Modifier.testTag("caller_id_error_text")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("confirm_add_caller_id")
            ) {
                Text("Save Identity", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun CallerIdCard(
    item: CallerIdItem,
    onSetPrimary: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            if (item.isPrimary) 2.dp else 1.dp,
            if (item.isPrimary) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onSetPrimary,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("caller_id_primary_${item.id}")
                ) {
                    Icon(
                        imageVector = if (item.isPrimary) {
                            Icons.Filled.Star
                        } else {
                            Icons.Outlined.StarBorder
                        },
                        contentDescription = "Set Primary",
                        tint = if (item.isPrimary) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (item.isPrimary) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "PRIMARY",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = item.phoneNumber,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (item.host.isNotEmpty() || item.username.isNotEmpty()) {
                        val account = buildString {
                            if (item.username.isNotEmpty()) append("${item.username}@")
                            append(item.host.ifEmpty { "VoIP Trunk" })
                        }
                        Text(
                            text = "SIP: $account",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (item.isVerified) {
                                        MaterialTheme.colorScheme.success
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (item.isVerified) "Verified Line" else "Unverified",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.isVerified) {
                                MaterialTheme.colorScheme.onSuccessContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1
                        )
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("caller_id_delete_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
