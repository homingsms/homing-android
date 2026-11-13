package com.homingsms.app.core.designsystem.shell

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.homingsms.app.core.designsystem.R
import com.homingsms.app.core.designsystem.component.HomingLogoMark
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.util.formatCount

private val RailBreakpoint = 600.dp

@Immutable
data class ShellDestination(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val badgeCount: Int? = null
)

enum class HomingTopLevelDestination(@StringRes val labelRes: Int, val icon: ImageVector, val selectedIcon: ImageVector) {
    Home(R.string.homing_nav_home, Icons.Outlined.Home, Icons.Filled.Home),
    Messages(R.string.homing_nav_messages, Icons.Outlined.Sms, Icons.Filled.Sms),
    Inbox(R.string.homing_nav_inbox, Icons.Outlined.Inbox, Icons.Filled.Inbox),
    Connect(R.string.homing_nav_connect, Icons.Outlined.Hub, Icons.Filled.Hub),
    Settings(R.string.homing_nav_settings, Icons.Outlined.Settings, Icons.Filled.Settings)
}

@Composable
fun HomingAppShell(
    destinations: List<ShellDestination>,
    selectedKey: String,
    onDestinationSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth >= RailBreakpoint) {
            RailLayout(destinations, selectedKey, onDestinationSelected, content)
        } else {
            BarLayout(destinations, selectedKey, onDestinationSelected, content)
        }
    }
}

@Composable
private fun BarLayout(
    destinations: List<ShellDestination>,
    selectedKey: String,
    onDestinationSelected: (String) -> Unit,
    content: @Composable () -> Unit
) {
    val colors = HomingTheme.colors
    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            Box {
                NavigationBar(containerColor = colors.surface, tonalElevation = 0.dp) {
                    destinations.forEach { destination ->
                        val selected = destination.key == selectedKey
                        NavigationBarItem(
                            selected = selected,
                            onClick = { onDestinationSelected(destination.key) },
                            icon = { DestinationIcon(destination, selected) },
                            label = { Text(destination.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.foreground,
                                selectedTextColor = colors.foreground,
                                indicatorColor = colors.brandSoft,
                                unselectedIconColor = colors.mutedForeground,
                                unselectedTextColor = colors.mutedForeground
                            )
                        )
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = colors.border)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            content()
        }
    }
}

@Composable
private fun RailLayout(
    destinations: List<ShellDestination>,
    selectedKey: String,
    onDestinationSelected: (String) -> Unit,
    content: @Composable () -> Unit
) {
    val colors = HomingTheme.colors
    Row(Modifier.fillMaxSize()) {
        NavigationRail(
            containerColor = colors.surface,
            header = {
                HomingLogoMark(modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(8.dp))
            },
            windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)
        ) {
            destinations.forEach { destination ->
                val selected = destination.key == selectedKey
                NavigationRailItem(
                    selected = selected,
                    onClick = { onDestinationSelected(destination.key) },
                    icon = { DestinationIcon(destination, selected) },
                    label = { Text(destination.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = colors.foreground,
                        selectedTextColor = colors.foreground,
                        indicatorColor = colors.brandSoft,
                        unselectedIconColor = colors.mutedForeground,
                        unselectedTextColor = colors.mutedForeground
                    )
                )
            }
        }
        VerticalDivider(thickness = 1.dp, color = colors.border)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))
        ) {
            content()
        }
    }
}

@Composable
private fun DestinationIcon(destination: ShellDestination, selected: Boolean) {
    val icon = if (selected) destination.selectedIcon else destination.icon
    val badge = destination.badgeCount
    if (badge != null && badge > 0) {
        BadgedBox(badge = { Badge { Text(formatCount(badge)) } }) {
            Icon(imageVector = icon, contentDescription = null)
        }
    } else {
        Icon(imageVector = icon, contentDescription = null)
    }
}
