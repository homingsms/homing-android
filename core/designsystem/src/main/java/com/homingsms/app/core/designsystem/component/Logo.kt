package com.homingsms.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.homingsms.app.core.designsystem.R
import com.homingsms.app.core.designsystem.theme.HomingTheme

@Composable
fun HomingLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    contentDescription: String? = stringResource(R.string.homing_logo_description)
) {
    val colors = HomingTheme.colors
    val described = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 4))
            .background(colors.brand)
            .then(described),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_homing_glyph),
            contentDescription = null,
            tint = colors.brandForeground,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun HomingWordmark(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        HomingLogoMark(contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Text(text = stringResource(R.string.homing_wordmark), style = HomingTheme.text.wordmark, color = HomingTheme.colors.foreground)
    }
}
