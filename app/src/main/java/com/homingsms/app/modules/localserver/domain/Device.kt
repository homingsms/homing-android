@file:UseSerializers(DateSerializer::class)

package com.homingsms.app.modules.localserver.domain

import com.homingsms.app.serialization.DateSerializer
import java.util.Date
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

/**
 * Represents a device known to the local server.
 *
 * Wire contract: [publicKey] and [keyVersion] are nullable, are omitted from
 * the response when null, and are always emitted together.
 *
 * JSON key order is deliberately not part of this contract: objects are
 * unordered. The two key fields are declared last only because they were
 * added last.
 *
 * `deletedAt` is intentionally absent: it is omitted for every listed device,
 * so including it would not change the response.
 */
@Serializable
data class Device(
    val id: String,
    val name: String,
    val createdAt: Date,
    val updatedAt: Date,
    val lastSeen: Date,
    val simCards: List<SimCard> = emptyList(),
    val publicKey: String? = null,
    val keyVersion: Int? = null,
)
