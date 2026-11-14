package com.homingsms.app.ui.onboarding

import com.homingsms.app.feature.onboarding.LegalLink
import com.homingsms.app.ui.system.SystemAction
import com.homingsms.core.model.LegalDocument

object LegalLinks {
    fun document(link: LegalLink): LegalDocument = when (link) {
        LegalLink.Terms -> LegalDocument.Terms
        LegalLink.Privacy -> LegalDocument.Privacy
        LegalLink.AcceptableUse -> LegalDocument.AcceptableUse
    }

    fun open(document: LegalDocument, baseUrl: String): SystemAction = SystemAction.OpenUrl(document.url(baseUrl))

    fun open(link: LegalLink, baseUrl: String): SystemAction = open(document(link), baseUrl)
}
