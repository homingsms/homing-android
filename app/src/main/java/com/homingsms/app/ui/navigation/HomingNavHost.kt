package com.homingsms.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.homingsms.app.core.designsystem.shell.HomingAppShell
import com.homingsms.app.core.designsystem.shell.HomingTopLevelDestination
import com.homingsms.app.core.designsystem.shell.ShellDestination
import com.homingsms.app.ui.Navigator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

object TopLevel {
    val routes: Map<HomingTopLevelDestination, AppRoute> = mapOf(
        HomingTopLevelDestination.Home to HomeRoute,
        HomingTopLevelDestination.Messages to MessagesRoute,
        HomingTopLevelDestination.Inbox to InboxRoute,
        HomingTopLevelDestination.Connect to ConnectRoute,
        HomingTopLevelDestination.Settings to SettingsRoute
    )

    fun of(destination: NavDestination?): HomingTopLevelDestination? {
        destination ?: return null
        return when {
            destination.hasRoute<HomeRoute>() -> HomingTopLevelDestination.Home
            destination.hasRoute<MessagesRoute>() -> HomingTopLevelDestination.Messages
            destination.hasRoute<InboxRoute>() -> HomingTopLevelDestination.Inbox
            destination.hasRoute<ConnectRoute>() -> HomingTopLevelDestination.Connect
            destination.hasRoute<SettingsRoute>() -> HomingTopLevelDestination.Settings
            else -> null
        }
    }
}

class NavControllerNavigator(private val controller: NavHostController, private val onExit: () -> Unit = {}) : Navigator {
    override fun navigate(route: AppRoute) {
        if (route in TopLevel.routes.values) {
            controller.navigate(route) {
                popUpTo<HomeRoute> { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        } else {
            controller.navigate(route) { launchSingleTop = true }
        }
    }

    override fun back() {
        if (!controller.navigateUp()) onExit()
    }

    override fun backTo(route: AppRoute) {
        if (!controller.popBackStack(route, inclusive = false)) back()
    }

    override fun resetTo(route: AppRoute) {
        controller.navigate(route) {
            popUpTo(controller.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }
}

@Composable
fun HomingApp(
    startRoute: AppRoute,
    modifier: Modifier = Modifier,
    routes: Flow<AppRoute> = emptyFlow(),
    navController: NavHostController = rememberNavController(),
    onExit: () -> Unit = {}
) {
    val navigator = remember(navController) { NavControllerNavigator(navController, onExit) }
    LaunchedEffect(routes, navigator) { routes.collect(navigator::navigate) }
    val entry by navController.currentBackStackEntryAsState()
    val selected = TopLevel.of(entry?.destination)
    val host = remember(navController, navigator, startRoute) {
        movableContentOf { HomingGraph(navController, navigator, startRoute) }
    }
    if (selected == null) {
        host()
        return
    }
    val destinations = HomingTopLevelDestination.entries.map {
        ShellDestination(key = it.name, label = stringResource(it.labelRes), icon = it.icon, selectedIcon = it.selectedIcon)
    }
    HomingAppShell(
        destinations = destinations,
        selectedKey = selected.name,
        onDestinationSelected = { key ->
            TopLevel.routes[HomingTopLevelDestination.valueOf(key)]?.let(navigator::navigate)
        },
        modifier = modifier
    ) { host() }
}

@Composable
private fun HomingGraph(navController: NavHostController, navigator: Navigator, startRoute: AppRoute) {
    NavHost(navController = navController, startDestination = startRoute) {
        composable<OnboardingRoute> { OnboardingDestination(navigator) }
        composable<HomeRoute> { HomeDestination(navigator) }
        composable<MessagesRoute> { MessagesDestination(navigator) }
        composable<MessageDetailRoute> { MessageDetailDestination(it.toRoute<MessageDetailRoute>().id, navigator) }
        composable<InboxRoute> { InboxDestination(navigator) }
        composable<InboxDetailRoute> { InboxDetailDestination(it.toRoute<InboxDetailRoute>().id, navigator) }
        composable<ConnectRoute> { ConnectDestination(navigator) }
        composable<WebhooksRoute> { WebhooksDestination(navigator) }
        composable<WebhookEditorRoute> { WebhookEditorDestination(it.toRoute<WebhookEditorRoute>().id, navigator) }
        composable<WebhookDetailRoute> { WebhookDetailDestination(it.toRoute<WebhookDetailRoute>().id, navigator) }
        composable<SettingsRoute> { SettingsDestination(navigator) }
        composable<AboutRoute> { AboutDestination(navigator) }
    }
}
