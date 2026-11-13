package com.homingsms.app.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.homingsms.app.core.designsystem.R
import com.homingsms.app.core.designsystem.theme.HomingTheme

@Composable
fun HomingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: HomingButtonStyle = HomingButtonStyle.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val colors = HomingTheme.colors
    val shape = MaterialTheme.shapes.small
    val padding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    val loadingLabel = stringResource(R.string.homing_loading)
    val buttonModifier = modifier
        .heightIn(min = HomingTheme.spacing.minTouchTarget)
        .semantics { if (loading) stateDescription = loadingLabel }
    val active = enabled && !loading
    val content: @Composable RowScope.() -> Unit = {
        ButtonContent(text = text, icon = icon, loading = loading)
    }
    when (style) {
        HomingButtonStyle.Primary -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = active,
            shape = shape,
            colors = ButtonDefaults.buttonColors(containerColor = colors.brand, contentColor = colors.brandForeground),
            contentPadding = padding,
            content = content
        )

        HomingButtonStyle.Secondary -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = active,
            shape = shape,
            colors = ButtonDefaults.buttonColors(containerColor = colors.muted, contentColor = colors.foreground),
            contentPadding = padding,
            content = content
        )

        HomingButtonStyle.Outline -> OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = active,
            shape = shape,
            border = BorderStroke(1.dp, colors.borderStrong),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.foreground),
            contentPadding = padding,
            content = content
        )

        HomingButtonStyle.Text -> TextButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = active,
            shape = shape,
            colors = ButtonDefaults.textButtonColors(contentColor = colors.brand),
            contentPadding = padding,
            content = content
        )

        HomingButtonStyle.Destructive -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = active,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.danger,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            contentPadding = padding,
            content = content
        )
    }
}

@Composable
private fun ButtonContent(text: String, icon: ImageVector?, loading: Boolean) {
    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = LocalContentColor.current
        )
        Spacer(Modifier.width(8.dp))
    } else if (icon != null) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(text = text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
}
