package com.homingsms.app.ui.onboarding

import androidx.lifecycle.viewModelScope
import com.homingsms.app.BuildConfig
import com.homingsms.app.feature.onboarding.UpdatedTermsActions
import com.homingsms.app.feature.onboarding.UpdatedTermsUiState
import com.homingsms.app.ui.EffectViewModel
import com.homingsms.app.ui.attempt
import com.homingsms.app.ui.connect.PairLinkInbox
import com.homingsms.app.ui.data.GatewayControl
import com.homingsms.app.ui.navigation.ConnectRoute
import com.homingsms.app.ui.navigation.HomeRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UpdatedTermsViewModel(
    private val control: GatewayControl,
    private val links: PairLinkInbox,
    private val legalBaseUrl: String = BuildConfig.LEGAL_BASE_URL,
    private val clock: () -> Long = System::currentTimeMillis
) : EffectViewModel() {
    private val mutableState = MutableStateFlow(UpdatedTermsUiState())
    private var saving = false

    val state: StateFlow<UpdatedTermsUiState> = mutableState.asStateFlow()

    val actions = UpdatedTermsActions(
        onAcceptedChange = { accepted -> mutableState.update { it.copy(accepted = accepted) } },
        onOpenLegal = { link -> system(LegalLinks.open(link, legalBaseUrl)) },
        onAccept = ::accept
    )

    private fun accept() {
        if (!mutableState.value.accepted || saving) return
        saving = true
        val now = clock()
        viewModelScope.launch {
            val saved = attempt { control.updateSettings { it.copy(legal = it.legal.accept(now)) } }.isSuccess
            saving = false
            if (!saved) return@launch
            resetTo(HomeRoute)
            if (links.links.value != null) navigate(ConnectRoute)
        }
    }
}
