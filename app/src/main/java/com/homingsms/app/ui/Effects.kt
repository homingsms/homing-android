package com.homingsms.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.ViewModel
import com.homingsms.app.ui.navigation.AppRoute
import com.homingsms.app.ui.system.PermissionOutcome
import com.homingsms.app.ui.system.SystemAction
import com.homingsms.app.ui.system.rememberSystemActionLauncher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

sealed interface UiEffect {
    data class System(val action: SystemAction) : UiEffect

    data class Navigate(val route: AppRoute) : UiEffect

    data object Back : UiEffect

    data class BackTo(val route: AppRoute) : UiEffect

    data class ResetTo(val route: AppRoute) : UiEffect
}

interface Navigator {
    fun navigate(route: AppRoute)

    fun back()

    fun backTo(route: AppRoute)

    fun resetTo(route: AppRoute)
}

abstract class EffectViewModel : ViewModel() {
    private val channel = Channel<UiEffect>(Channel.BUFFERED)

    val effects: Flow<UiEffect> = channel.receiveAsFlow()

    protected fun emit(effect: UiEffect) {
        channel.trySend(effect)
    }

    protected fun navigate(route: AppRoute) = emit(UiEffect.Navigate(route))

    protected fun system(action: SystemAction) = emit(UiEffect.System(action))

    protected fun back() = emit(UiEffect.Back)

    protected fun backTo(route: AppRoute) = emit(UiEffect.BackTo(route))

    protected fun resetTo(route: AppRoute) = emit(UiEffect.ResetTo(route))
}

@Composable
fun EffectHandler(effects: Flow<UiEffect>, navigator: Navigator, onPermissionsResult: (List<PermissionOutcome>) -> Unit = {}) {
    val launch = rememberSystemActionLauncher(onPermissionsResult)
    val currentNavigator = rememberUpdatedState(navigator)
    val currentLaunch = rememberUpdatedState(launch)
    LaunchedEffect(effects) {
        effects.collect { effect ->
            when (effect) {
                is UiEffect.System -> currentLaunch.value(effect.action)
                is UiEffect.Navigate -> currentNavigator.value.navigate(effect.route)
                UiEffect.Back -> currentNavigator.value.back()
                is UiEffect.BackTo -> currentNavigator.value.backTo(effect.route)
                is UiEffect.ResetTo -> currentNavigator.value.resetTo(effect.route)
            }
        }
    }
}
