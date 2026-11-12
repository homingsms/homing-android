package com.homingsms.app.modules.localserver.domain.messages

/**
 * Sort order for GET /messages, mirroring the cloud server contract.
 */
enum class MessageSort {
    CreatedAtAsc,
    CreatedAtDesc;

    fun toDomain(): com.homingsms.app.modules.messages.data.MessageSort = when (this) {
        CreatedAtAsc -> com.homingsms.app.modules.messages.data.MessageSort.CreatedAtAsc
        CreatedAtDesc -> com.homingsms.app.modules.messages.data.MessageSort.CreatedAtDesc
    }

    companion object {
        /**
         * Parse the `sort` query parameter value.
         * null/absent -> CreatedAtDesc; "created_at" -> CreatedAtAsc;
         * "-created_at" -> CreatedAtDesc; anything else -> IllegalArgumentException.
         */
        fun parse(raw: String?): MessageSort = when (raw) {
            null -> CreatedAtDesc
            "created_at" -> CreatedAtAsc
            "-created_at" -> CreatedAtDesc
            else -> throw IllegalArgumentException("sort must be one of: created_at, -created_at")
        }
    }
}
