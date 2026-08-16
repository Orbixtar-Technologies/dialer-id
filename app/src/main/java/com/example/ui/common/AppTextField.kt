package com.example.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * The single source of truth for text field colours in the app.
 *
 * `OutlinedTextFieldDefaults.colors()` only overrides what it is handed, so a
 * call site that sets nothing but the border colours inherits Material's
 * baseline for input text, label, placeholder, cursor and every error/disabled
 * tone. Those baselines are derived from the active [MaterialTheme.colorScheme],
 * which is exactly what broke on screens that paint hardcoded light containers:
 * in dark mode the text resolved to a near-white `onSurface` on a hardcoded
 * white box. Every field goes through here instead.
 */
@Composable
fun appTextFieldColors(): TextFieldColors {
    val colors = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.onSurface,
        unfocusedTextColor = colors.onSurface,
        disabledTextColor = colors.onSurfaceVariant,
        errorTextColor = colors.onSurface,
        focusedContainerColor = colors.surface,
        unfocusedContainerColor = colors.surface,
        disabledContainerColor = colors.surfaceVariant,
        errorContainerColor = colors.surface,
        cursorColor = colors.primary,
        errorCursorColor = colors.error,
        focusedBorderColor = colors.primary,
        unfocusedBorderColor = colors.outline,
        disabledBorderColor = colors.outlineVariant,
        errorBorderColor = colors.error,
        focusedLeadingIconColor = colors.primary,
        unfocusedLeadingIconColor = colors.onSurfaceVariant,
        disabledLeadingIconColor = colors.outline,
        errorLeadingIconColor = colors.error,
        focusedTrailingIconColor = colors.onSurfaceVariant,
        unfocusedTrailingIconColor = colors.onSurfaceVariant,
        disabledTrailingIconColor = colors.outline,
        errorTrailingIconColor = colors.error,
        focusedLabelColor = colors.primary,
        unfocusedLabelColor = colors.onSurfaceVariant,
        disabledLabelColor = colors.onSurfaceVariant,
        errorLabelColor = colors.error,
        focusedPlaceholderColor = colors.onSurfaceVariant,
        unfocusedPlaceholderColor = colors.onSurfaceVariant,
        disabledPlaceholderColor = colors.outline,
        errorPlaceholderColor = colors.onSurfaceVariant,
        focusedSupportingTextColor = colors.onSurfaceVariant,
        unfocusedSupportingTextColor = colors.onSurfaceVariant,
        disabledSupportingTextColor = colors.onSurfaceVariant,
        errorSupportingTextColor = colors.error
    )
}

/**
 * Outlined text field with the app's colours, shape and error presentation.
 *
 * @param errorText message shown under the field; a non-null value also puts
 *   the field into its error state, so callers never have to keep [isError] and
 *   the message in sync by hand.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    leadingIconDescription: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: String? = null,
    errorText: String? = null,
    isError: Boolean = errorText != null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        textStyle = MaterialTheme.typography.bodyLarge,
        label = label?.let { { Text(text = it, maxLines = 1) } },
        placeholder = placeholder?.let { { Text(text = it, maxLines = 1) } },
        leadingIcon = leadingIcon?.let {
            {
                Icon(imageVector = it, contentDescription = leadingIconDescription)
            }
        },
        trailingIcon = trailingIcon,
        supportingText = (errorText ?: supportingText)?.let {
            { Text(text = it, style = MaterialTheme.typography.bodySmall) }
        },
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        shape = shape,
        colors = appTextFieldColors()
    )
}

/**
 * Form error banner for failures that belong to the whole form rather than to
 * one field, e.g. a rejected sign-in.
 */
@Composable
fun AppFormError(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
