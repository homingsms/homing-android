package com.homingsms.app.domain

enum class EntitySource {
    Local,
    Cloud,

    @Deprecated("Not used anymore")
    Gateway,
}
