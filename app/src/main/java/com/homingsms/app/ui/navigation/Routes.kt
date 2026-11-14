package com.homingsms.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute

@Serializable
data object OnboardingRoute : AppRoute

@Serializable
data object TermsRoute : AppRoute

@Serializable
data object HomeRoute : AppRoute

@Serializable
data object MessagesRoute : AppRoute

@Serializable
data class MessageDetailRoute(val id: String) : AppRoute

@Serializable
data object InboxRoute : AppRoute

@Serializable
data class InboxDetailRoute(val id: String) : AppRoute

@Serializable
data object ConnectRoute : AppRoute

@Serializable
data object WebhooksRoute : AppRoute

@Serializable
data class WebhookEditorRoute(val id: String? = null) : AppRoute

@Serializable
data class WebhookDetailRoute(val id: String) : AppRoute

@Serializable
data object SettingsRoute : AppRoute

@Serializable
data object AboutRoute : AppRoute
