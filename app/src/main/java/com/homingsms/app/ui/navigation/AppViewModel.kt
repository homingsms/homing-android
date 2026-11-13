package com.homingsms.app.ui.navigation

import androidx.lifecycle.ViewModel
import com.homingsms.app.ui.connect.PairLinkInbox
import com.homingsms.app.ui.data.AppStatus
import com.homingsms.core.model.PairingLink
import java.net.URI
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class AppViewModel(private val status: AppStatus, private val links: PairLinkInbox) : ViewModel() {
    private val navigation = Channel<AppRoute>(Channel.BUFFERED)

    val startRoute: AppRoute = if (status.settings.value.general.onboardingCompleted) HomeRoute else OnboardingRoute

    val routes: Flow<AppRoute> = navigation.receiveAsFlow()

    fun onLink(link: String?) {
        if (link == null || !isPairLink(link)) return
        links.offer(link)
        if (status.settings.value.general.onboardingCompleted) navigation.trySend(ConnectRoute)
    }

    companion object {
        fun isPairLink(link: String): Boolean {
            val uri = runCatching { URI(link.trim()) }.getOrNull() ?: return false
            return uri.scheme.equals(PairingLink.SCHEME, ignoreCase = true) && uri.host.equals(PairingLink.HOST, ignoreCase = true)
        }
    }
}
