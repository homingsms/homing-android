package com.homingsms.core.data.repository

import com.homingsms.core.data.db.ApiKeyEntity
import com.homingsms.core.data.db.ApiKeysDao
import com.homingsms.core.data.secrets.SecretStore
import com.homingsms.core.model.ApiKeyInfo
import com.homingsms.core.model.ApiKeyPrincipal
import com.homingsms.core.model.CreatedApiKey
import com.homingsms.core.model.Ids
import com.homingsms.core.model.Scope
import com.homingsms.core.model.api.ApiKeyFormat
import java.security.SecureRandom
import kotlin.io.encoding.Base64
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ApiKeyRepository(
    private val dao: ApiKeysDao,
    private val secrets: SecretStore,
    private val clock: () -> Long = System::currentTimeMillis
) {
    val keys: Flow<List<ApiKeyInfo>> = dao.observeActive().map { list -> list.map { it.toInfo() } }

    suspend fun list(): List<ApiKeyInfo> = dao.active().map { it.toInfo() }

    suspend fun create(name: String, scopes: Set<Scope>): CreatedApiKey {
        require(name.isNotBlank()) { "Name is required" }
        require(scopes.isNotEmpty()) { "At least one scope is required" }
        val secret = ApiKeyFormat.PREFIX + Ids.random(ApiKeyFormat.SECRET_LENGTH)
        val entity = ApiKeyEntity(
            id = Ids.apiKey(),
            name = name.trim().take(MAX_NAME),
            prefix = ApiKeyFormat.displayPrefix(secret),
            hash = ApiKeyFormat.hash(pepper(), secret),
            scopes = scopes.sortedBy { it.ordinal }.joinToString(",") { it.value },
            createdAt = clock(),
            lastUsedAt = null,
            revokedAt = null
        )
        dao.insert(entity)
        return CreatedApiKey(entity.toInfo(), secret)
    }

    suspend fun revoke(id: String): Boolean = dao.revoke(id, clock()) > 0

    suspend fun authenticate(key: String): ApiKeyPrincipal? {
        if (!ApiKeyFormat.looksValid(key)) return null
        val hash = ApiKeyFormat.hash(pepper(), key)
        val entity = dao.findByHash(hash) ?: return null
        if (entity.revokedAt != null || !ApiKeyFormat.matches(pepper(), key, entity.hash)) return null
        val now = clock()
        if (entity.lastUsedAt == null || now - entity.lastUsedAt > TOUCH_INTERVAL_MS) dao.touch(entity.id, now)
        return ApiKeyPrincipal(entity.id, decodeScopes(entity.scopes))
    }

    private fun pepper(): ByteArray {
        secrets.get(SecretStore.API_KEY_PEPPER)?.let { return Base64.Default.decode(it) }
        val bytes = ByteArray(PEPPER_BYTES).also { SecureRandom().nextBytes(it) }
        secrets.put(SecretStore.API_KEY_PEPPER, Base64.Default.encode(bytes))
        return bytes
    }

    private fun ApiKeyEntity.toInfo(): ApiKeyInfo = ApiKeyInfo(id, name, prefix, decodeScopes(scopes), createdAt, lastUsedAt)

    companion object {
        private const val PEPPER_BYTES = 32
        private const val MAX_NAME = 100
        private const val TOUCH_INTERVAL_MS = 60_000L

        fun decodeScopes(value: String): Set<Scope> = value.split(',').mapNotNull { Scope.fromValue(it.trim()) }.toSet()
    }
}
