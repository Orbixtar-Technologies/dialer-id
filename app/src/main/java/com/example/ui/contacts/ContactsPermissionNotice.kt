package com.example.ui.contacts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * Rationale shown instead of contact rows whenever `READ_CONTACTS` is missing.
 * A permanently denied permission routes the user to app settings rather than
 * re-triggering a dialog the system will no longer show.
 */
@Composable
fun ContactsPermissionNotice(
    status: ContactsPermissionStatus,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val isBlocked = status == ContactsPermissionStatus.PERMANENTLY_DENIED
    val action: () -> Unit = if (isBlocked) onOpenSettings else onRequest
    val actionLabel = if (isBlocked) {
        stringResource(R.string.contacts_permission_settings)
    } else {
        stringResource(R.string.contacts_permission_action)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("contacts_permission_notice"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        if (compact) {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PermissionIcon(isBlocked = isBlocked, size = 32.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isBlocked) {
                        stringResource(R.string.contacts_permission_blocked_body)
                    } else {
                        stringResource(R.string.dialer_contacts_permission_hint)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = action,
                    modifier = Modifier.testTag("contacts_permission_action")
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PermissionIcon(isBlocked = isBlocked, size = 56.dp)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = if (isBlocked) {
                        stringResource(R.string.contacts_permission_blocked_title)
                    } else {
                        stringResource(R.string.contacts_permission_title)
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isBlocked) {
                        stringResource(R.string.contacts_permission_blocked_body)
                    } else {
                        stringResource(R.string.contacts_permission_body)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = action,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("contacts_permission_action")
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionIcon(isBlocked: Boolean, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isBlocked) Icons.Default.Lock else Icons.Default.Contacts,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}
