package com.homingsms.core.model

data class ApiKeyInfo(
    val id: String,
    val name: String,
    val prefix: String,
    val scopes: Set<Scope>,
    val createdAt: Long,
    val lastUsedAt: Long?
)

data class CreatedApiKey(val info: ApiKeyInfo, val secret: String)

data class ApiKeyPrincipal(val keyId: String, val scopes: Set<Scope>)
