package com.example.ui.auth

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.common.AppFormError
import com.example.ui.common.AppTextField

@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sizePx = size.minDimension
        val strokeWidth = sizePx * 0.2f
        val radius = (sizePx - strokeWidth) / 2f
        val center = Offset(sizePx / 2f, sizePx / 2f)

        // Google Red (top-left to top-right)
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 200f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Google Yellow (bottom-left)
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 120f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Google Green (bottom)
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 40f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Google Blue (right arc and middle horizontal bar)
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = 320f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Blue middle bar
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(center.x, center.y),
            end = Offset(sizePx - strokeWidth / 4f, center.y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var isPasswordVisible by remember { mutableStateOf(false) }

    val submit = {
        keyboardController?.hide()
        focusManager.clearFocus()
        viewModel.submitEmailAuth()
    }

    // This screen is drawn outside the app Scaffold, so it owns its own insets:
    // safeDrawing covers the status/navigation bars and the IME, which together
    // with the scrolling column keeps the submit button reachable above an open
    // keyboard on short screens.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.auth_secure_session),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    AuthModeSelector(
                        isSignUpMode = uiState.isSignUpMode,
                        onSelect = { signUp ->
                            if (signUp != uiState.isSignUpMode) viewModel.toggleAuthMode()
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedButton(
                        onClick = {
                            if (activity != null) {
                                viewModel.signInWithGoogle(activity)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("auth_google_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        enabled = !uiState.isLoading
                    ) {
                        GoogleLogoIcon(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.auth_continue_with_google),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Text(
                            text = stringResource(R.string.auth_divider_email),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    AnimatedVisibility(
                        visible = uiState.isSignUpMode,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column {
                            AppTextField(
                                value = uiState.displayNameInput,
                                onValueChange = viewModel::onDisplayNameChanged,
                                label = stringResource(R.string.auth_name_label),
                                placeholder = stringResource(R.string.auth_name_placeholder),
                                leadingIcon = Icons.Default.Person,
                                enabled = !uiState.isLoading,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_name_input")
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    AppTextField(
                        value = uiState.emailInput,
                        onValueChange = viewModel::onEmailChanged,
                        label = stringResource(R.string.auth_email_label),
                        placeholder = stringResource(R.string.auth_email_placeholder),
                        leadingIcon = Icons.Default.Email,
                        enabled = !uiState.isLoading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    AppTextField(
                        value = uiState.passwordInput,
                        onValueChange = viewModel::onPasswordChanged,
                        label = stringResource(R.string.auth_password_label),
                        placeholder = stringResource(R.string.auth_password_placeholder),
                        leadingIcon = Icons.Default.Lock,
                        enabled = !uiState.isLoading,
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) {
                                        Icons.Default.VisibilityOff
                                    } else {
                                        Icons.Default.Visibility
                                    },
                                    contentDescription = stringResource(
                                        if (isPasswordVisible) {
                                            R.string.auth_password_hide
                                        } else {
                                            R.string.auth_password_show
                                        }
                                    )
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input")
                    )

                    uiState.errorMessage?.let { message ->
                        Spacer(modifier = Modifier.height(10.dp))
                        AppFormError(
                            message = message,
                            modifier = Modifier.testTag("auth_error_text")
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { submit() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("auth_login_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(
                                    if (uiState.isSignUpMode) {
                                        R.string.auth_create_account
                                    } else {
                                        R.string.auth_sign_in
                                    }
                                ),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.loginAsGuest()
                            onLoginSuccess()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("auth_guest_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = null,
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.auth_guest_access),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.auth_footer_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun AuthModeSelector(
    isSignUpMode: Boolean,
    onSelect: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AuthModeTab(
            label = stringResource(R.string.auth_sign_in),
            isSelected = !isSignUpMode,
            onClick = { onSelect(false) },
            modifier = Modifier.weight(1f)
        )
        AuthModeTab(
            label = stringResource(R.string.auth_create_account),
            isSelected = isSignUpMode,
            onClick = { onSelect(true) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AuthModeTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.surface
        } else {
            Color.Transparent
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1
            )
        }
    }
}
