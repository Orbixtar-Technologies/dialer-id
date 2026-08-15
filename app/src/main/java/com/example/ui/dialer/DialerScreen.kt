package com.example.ui.dialer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.service.sip.RegistrationStatus
import com.example.ui.theme.Amber50
import com.example.ui.theme.Amber500
import com.example.ui.theme.Amber600
import com.example.ui.theme.Emerald500
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Rose500
import com.example.ui.theme.RoyalBlue600
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900

@Composable
fun DialerScreen(
    viewModel: DialerViewModel,
    onNavigateToDeposit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val regState by viewModel.registrationState.collectAsState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        viewModel.onScreenOpened()
    }

    val hasZeroBalance = userProfile.creditBalance <= 0.0
    val isTestNumber = uiState.inputNumber == "3200" || uiState.inputNumber == "444"
    val isCallAllowed = (!hasZeroBalance || isTestNumber) && regState.isRegistered

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Slate50
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dotColor = when {
                    regState.needsPassword -> Rose500
                    regState.status == RegistrationStatus.REGISTERED -> Emerald500
                    regState.status == RegistrationStatus.REGISTERING -> RoyalBlue600
                    regState.status == RegistrationStatus.FAILED -> Rose500
                    else -> Slate400
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(dotColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        regState.isRegistered -> stringResource(R.string.dialer_status_registered)
                        regState.needsPassword -> stringResource(R.string.dialer_status_password_required)
                        regState.status == RegistrationStatus.FAILED &&
                            regState.statusCode > 0 -> stringResource(
                            R.string.dialer_status_failed_code,
                            regState.statusCode
                        )
                        regState.status == RegistrationStatus.FAILED ->
                            stringResource(
                                R.string.dialer_status_failed,
                                regState.lastError ?: stringResource(R.string.dialer_registration_failed)
                            )
                        else -> stringResource(R.string.dialer_status_not_registered)
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (regState.isRegistered) Slate700 else Slate500
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = PureWhite
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 22.dp)
                ) {
                    Text(
                        text = if (uiState.formattedDisplay.isEmpty()) {
                            stringResource(R.string.dialer_enter_number)
                        } else {
                            uiState.formattedDisplay
                        },
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.8.sp
                        ),
                        color = if (uiState.formattedDisplay.isEmpty()) Slate400 else Slate900,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 44.dp)
                            .testTag("dialer_display_text")
                    )

                    if (uiState.inputNumber.isNotEmpty()) {
                        @OptIn(ExperimentalFoundationApi::class)
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(40.dp)
                                .clip(CircleShape)
                                .combinedClickable(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.onBackspace()
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.onClearAll()
                                    }
                                )
                                .testTag("dialer_backspace_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = stringResource(R.string.dialer_backspace),
                                tint = Slate700,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val keypadRows = listOf(
                    listOf(KeypadData('1', ""), KeypadData('2', "ABC"), KeypadData('3', "DEF")),
                    listOf(KeypadData('4', "GHI"), KeypadData('5', "JKL"), KeypadData('6', "MNO")),
                    listOf(KeypadData('7', "PQRS"), KeypadData('8', "TUV"), KeypadData('9', "WXYZ")),
                    listOf(KeypadData('*', ""), KeypadData('0', "+"), KeypadData('#', ""))
                )

                for (row in keypadRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (item in row) {
                            KeypadKey(
                                digit = item.digit,
                                subText = item.subText,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.appendDigit(item.digit)
                                },
                                onLongClick = {
                                    if (item.digit == '0') {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.appendDigit('+')
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            AnimatedVisibility(
                visible = uiState.showZeroBalanceWarning || (hasZeroBalance && !isTestNumber && uiState.inputNumber.isNotEmpty()),
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .border(1.dp, Amber500.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Amber50),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Zero Balance Warning",
                            tint = Amber600,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Zero Balance Guard Active",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Amber600
                            )
                            Text(
                                text = "Please add credit before placing a standard call.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate700
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = onNavigateToDeposit,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Amber600,
                                contentColor = PureWhite
                            ),
                            contentPadding = ButtonDefaults.TextButtonContentPadding
                        ) {
                            Text("Top Up", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.placeCall()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(
                        elevation = if (isCallAllowed && uiState.inputNumber.isNotEmpty()) 6.dp else 0.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = Emerald500.copy(alpha = 0.4f)
                    )
                    .testTag("dialer_call_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCallAllowed) Emerald500 else Slate300,
                    contentColor = PureWhite,
                    disabledContainerColor = Slate200,
                    disabledContentColor = Slate400
                ),
                enabled = uiState.inputNumber.isNotEmpty() && isCallAllowed
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isCallAllowed) Icons.Default.Call else Icons.Default.Lock,
                        contentDescription = stringResource(R.string.dialer_place_call),
                        tint = PureWhite,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = when {
                            !regState.isRegistered -> stringResource(R.string.dialer_not_registered)
                            !isCallAllowed -> stringResource(R.string.dialer_top_up_to_call)
                            else -> stringResource(R.string.dialer_start_call)
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = PureWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private data class KeypadData(val digit: Char, val subText: String)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeypadKey(
    digit: Char,
    subText: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .shadow(2.dp, CircleShape, spotColor = Slate300)
            .clip(CircleShape)
            .background(PureWhite)
            .border(1.dp, Slate200, CircleShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("keypad_digit_$digit"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = digit.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                ),
                color = Slate900
            )
            if (subText.isNotEmpty()) {
                Text(
                    text = subText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 9.sp
                    ),
                    color = Slate400
                )
            }
        }
    }
}
