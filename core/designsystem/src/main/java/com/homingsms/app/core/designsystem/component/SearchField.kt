package com.homingsms.app.core.designsystem.component

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.homingsms.app.core.designsystem.R
import com.homingsms.app.core.designsystem.theme.HomingTheme

@Composable
fun HomingSearchField(query: String, onQueryChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    HomingTextField(
        value = query,
        onValueChange = onQueryChange,
        label = label,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = HomingTheme.colors.mutedForeground) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.homing_action_clear))
                }
            }
        } else {
            null
        }
    )
}
