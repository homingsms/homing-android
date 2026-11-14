package com.homingsms.core.model

object LegalTerms {
    const val CURRENT_VERSION = "2026-11-14"
}

enum class LegalDocument(val path: String) {
    Terms("legal/terms"),
    Privacy("legal/privacy"),
    AcceptableUse("legal/acceptable-use");

    fun url(baseUrl: String): String = baseUrl.trimEnd('/') + "/" + path
}
