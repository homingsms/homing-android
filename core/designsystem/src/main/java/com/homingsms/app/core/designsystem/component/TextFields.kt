package com.homingsms.app.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.VisualTransformation
import com.homingsms.app.core.designsystem.theme.HomingColors
import com.homingsms.app.core.designsystem.theme.HomingMotion
import com.homingsms.app.core.designsystem.theme.HomingRadius
import com.homingsms.app.core.designsystem.theme.HomingTheme

private const val FOCUS_TINT = 0.65f

internal val HomingFieldShape = RoundedCornerShape(HomingRadius.lg)

internal fun HomingColors.fieldFocused(): Color = lerp(muted, brandSoft, FOCUS_TINT)

@Composable
fun HomingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    errorText: String? = null,
    singleLine: Boolean = true,
    mono: Boolean = false,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val colors = HomingTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val isError = errorText != null
    val support = errorText ?: supportingText
    val labelColor by animateColorAsState(
        targetValue = when {
            isError -> colors.danger
            focused -> colors.brand
            else -> colors.mutedForeground
        },
        animationSpec = tween(HomingMotion.FAST_MILLIS),
        label = "fieldLabel"
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.xs)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
            modifier = Modifier.clearAndSetSemantics {}
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    text = AnnotatedString(label)
                    if (errorText != null) error(errorText)
                },
            placeholder = placeholder?.let { { Text(it, style = MaterialTheme.typography.bodyLarge) } },
            isError = isError,
            singleLine = singleLine,
            enabled = enabled,
            textStyle = if (mono) HomingTheme.text.mono else MaterialTheme.typography.bodyLarge,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            interactionSource = interaction,
            shape = HomingFieldShape,
            colors = homingFieldColors()
        )
        if (support != null) {
            Text(
                text = support,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) colors.danger else colors.mutedForeground
            )
        }
    }
}

@Composable
internal fun homingFieldColors() = HomingTheme.colors.let { colors ->
    TextFieldDefaults.colors(
        focusedContainerColor = colors.fieldFocused(),
        unfocusedContainerColor = colors.muted,
        disabledContainerColor = colors.muted.copy(alpha = 0.6f),
        errorContainerColor = colors.dangerSoft,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        errorIndicatorColor = Color.Transparent,
        focusedTextColor = colors.foreground,
        unfocusedTextColor = colors.foreground,
        disabledTextColor = colors.mutedForeground,
        errorTextColor = colors.foreground,
        focusedPlaceholderColor = colors.mutedForeground,
        unfocusedPlaceholderColor = colors.mutedForeground,
        errorPlaceholderColor = colors.mutedForeground,
        focusedLeadingIconColor = colors.brand,
        unfocusedLeadingIconColor = colors.mutedForeground,
        errorLeadingIconColor = colors.danger,
        focusedTrailingIconColor = colors.mutedForeground,
        unfocusedTrailingIconColor = colors.mutedForeground,
        errorTrailingIconColor = colors.danger,
        cursorColor = colors.brand,
        errorCursorColor = colors.danger
    )
}
