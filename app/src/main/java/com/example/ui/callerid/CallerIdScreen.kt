package com.example.ui.callerid

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallerIdItem
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Rose500
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
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900

@Composable
fun CallerIdScreen(
    viewModel: CallerIdViewModel,
    modifier: Modifier = Modifier
) {
    val callerIds by viewModel.callerIds.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Slate50
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
                    // Header Info Banner
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, RoyalBlue600.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = RoyalBlue50),
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
                                contentDescription = "Notice",
                                tint = RoyalBlue600,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Outbound Identity Vault",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = RoyalBlue800
                                )
                                Text(
                                    text = "Selected Caller ID will be transmitted as your outgoing identity when dialing. All numbers are cryptographically routed.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate700
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
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Slate900
                        )
                        Text(
                            text = "Tap star to set primary",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500
                        )
                    }
                }

                if (callerIds.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                                .border(1.dp, Slate200, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🆔", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No Outbound Caller IDs",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Slate900
                                )
                                Text(
                                    text = "Add your verified phone number to transmit as your caller ID.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate500,
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
                            onDelete = { viewModel.deleteCallerId(item) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            // Floating Action Button to add identity
            FloatingActionButton(
                onClick = { viewModel.showAddDialog(true) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .testTag("add_caller_id_fab"),
                containerColor = RoyalBlue600,
                contentColor = PureWhite,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Caller ID", modifier = Modifier.size(28.dp))
            }

            // Add Caller ID Dialog
            if (uiState.showAddDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.showAddDialog(false) },
                    containerColor = PureWhite,
                    shape = RoundedCornerShape(20.dp),
                    title = {
                        Text(
                            text = "Add Outbound Caller ID",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Slate900
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "Enter phone number in E.164 format (e.g. +1 555 123 4567).",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500,
                                modifier = Modifier.padding(bottom = 14.dp)
                            )

                            OutlinedTextField(
                                value = uiState.inputPhoneNumber,
                                onValueChange = viewModel::onPhoneNumberChanged,
                                label = { Text("Phone Number") },
                                placeholder = { Text("+1 (800) 555-0199") },
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = Slate500)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("new_caller_id_number"),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RoyalBlue600,
                                    unfocusedBorderColor = Slate200
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = uiState.inputLabel,
                                onValueChange = viewModel::onLabelChanged,
                                label = { Text("Label / Department") },
                                placeholder = { Text("e.g. Office Direct Line") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("new_caller_id_label"),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RoyalBlue600,
                                    unfocusedBorderColor = Slate200
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Set as Primary Identity",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = Slate900
                                )
                                Switch(
                                    checked = uiState.isPrimaryToggle,
                                    onCheckedChange = viewModel::onPrimaryToggleChanged,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = PureWhite,
                                        checkedTrackColor = RoyalBlue600
                                    )
                                )
                            }

                            if (uiState.errorMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = uiState.errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Rose500
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = viewModel::addCallerId,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue600),
                            modifier = Modifier.testTag("confirm_add_caller_id")
                        ) {
                            Text("Save Identity", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.showAddDialog(false) }) {
                            Text("Cancel", color = Slate500)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CallerIdCard(
    item: CallerIdItem,
    onSetPrimary: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (item.isPrimary) RoyalBlue600.copy(alpha = 0.5f) else Slate200,
                RoundedCornerShape(16.dp)
            )
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isPrimary) RoyalBlue50.copy(alpha = 0.3f) else PureWhite
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Primary star selector
                IconButton(
                    onClick = onSetPrimary,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (item.isPrimary) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Set Primary",
                        tint = if (item.isPrimary) RoyalBlue600 else Slate400,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Slate900
                        )
                        if (item.isPrimary) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(RoyalBlue600)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "PRIMARY",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = PureWhite,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = item.phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate700
                    )

                    if (item.host.isNotEmpty() || item.username.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "SIP: ${if (item.username.isNotEmpty()) "${item.username}@" else ""}${item.host.ifEmpty { "VoIP Trunk" }}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = RoyalBlue700,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Emerald500)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (item.isVerified) "Verified Line" else "Unverified",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.isVerified) Emerald600 else Slate500
                        )
                    }
                }
            }

            // Delete action
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = Slate400,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
