package com.homingsms.app.core.designsystem.shell

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homingsms.app.core.designsystem.R
import com.homingsms.app.core.designsystem.component.HomingLogoMark
import com.homingsms.app.core.designsystem.icon.HomingIcons
import com.homingsms.app.core.designsystem.theme.HomingMotion
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.util.formatCount

private val RailBreakpoint = 600.dp
private val BarHeight = 64.dp
private val BarMaxWidth = 480.dp
private val BarRadius = 24.dp
private val ItemRadius = 18.dp
private val BarInnerPadding = 6.dp
private val IconSize = 22.dp

@Immutable
data class ShellDestination(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val badgeCount: Int? = null
)

enum class HomingTopLevelDestination(@StringRes val labelRes: Int, val icon: ImageVector, val selectedIcon: ImageVector) {
    Home(R.string.homing_nav_home, HomingIcons.Home, HomingIcons.HomeSolid),
    Messages(R.string.homing_nav_messages, HomingIcons.Messages, HomingIcons.MessagesSolid),
    Inbox(R.string.homing_nav_inbox, HomingIcons.Inbox, HomingIcons.InboxSolid),
    Connect(R.string.homing_nav_connect, HomingIcons.Connect, HomingIcons.ConnectSolid),
    Settings(R.string.homing_nav_settings, HomingIcons.Settings, HomingIcons.SettingsSolid)
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
        bottomBar = { FloatingNavBar(destinations, selectedKey, onDestinationSelected) }
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
private fun FloatingNavBar(destinations: List<ShellDestination>, selectedKey: String, onDestinationSelected: (String) -> Unit) {
    val colors = HomingTheme.colors
    val shape = RoundedCornerShape(BarRadius)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .padding(horizontal = HomingTheme.spacing.lg)
            .padding(top = HomingTheme.spacing.xs, bottom = HomingTheme.spacing.md)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = BarMaxWidth)
                .align(Alignment.Center)
                .shadow(
                    elevation = 18.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = if (colors.isDark) 0.4f else 0.06f),
                    spotColor = Color.Black.copy(alpha = if (colors.isDark) 0.5f else 0.14f)
                )
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.border, shape)
                .height(BarHeight)
                .padding(BarInnerPadding)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            destinations.forEach { destination ->
                BarItem(
                    destination = destination,
                    selected = destination.key == selectedKey,
                    onClick = { onDestinationSelected(destination.key) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun BarItem(destination: ShellDestination, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = HomingTheme.colors
    val spec = tween<Color>(HomingMotion.FAST_MILLIS, easing = HomingMotion.Easing)
    val container by animateColorAsState(if (selected) colors.brandSoft else Color.Transparent, spec, label = "navContainer")
    val tint by animateColorAsState(if (selected) colors.brand else colors.mutedForeground, spec, label = "navTint")
    val labelColor by animateColorAsState(if (selected) colors.foreground else colors.mutedForeground, spec, label = "navLabel")
    val shape = RoundedCornerShape(ItemRadius)
    Column(
        modifier = modifier
            .clip(shape)
            .background(container)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        DestinationIcon(destination, selected, tint)
        Spacer(Modifier.height(3.dp))
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
                        selectedIconColor = colors.brand,
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
private fun DestinationIcon(destination: ShellDestination, selected: Boolean, tint: Color = LocalContentColor.current) {
    val icon = if (selected) destination.selectedIcon else destination.icon
    val badge = destination.badgeCount
    val colors = HomingTheme.colors
    if (badge != null && badge > 0) {
        BadgedBox(
            badge = {
                Badge(containerColor = colors.brand, contentColor = colors.brandForeground) { Text(formatCount(badge)) }
            }
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(IconSize))
        }
    } else {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(IconSize))
    }
}
