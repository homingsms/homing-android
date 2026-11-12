package com.homingsms.app.modules.settings

interface Exporter {
    fun export(): Map<String, *>
}
