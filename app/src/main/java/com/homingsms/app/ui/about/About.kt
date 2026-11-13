package com.homingsms.app.ui.about

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.homingsms.app.R
import com.homingsms.app.feature.about.AboutActions
import com.homingsms.app.feature.about.AboutDefaults
import com.homingsms.app.feature.about.AboutUiState
import com.homingsms.app.feature.about.ComponentLicense
import com.homingsms.app.feature.about.LicenseKind
import com.homingsms.app.feature.about.OpenSourceComponent
import com.homingsms.app.ui.EffectViewModel
import com.homingsms.app.ui.attempt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

data class BuildInfo(val versionName: String, val versionCode: Long, val buildType: String)

fun interface LicenseSource {
    suspend fun libraries(): List<OpenSourceComponent>
}

class RawLicenseSource(private val context: Context) : LicenseSource {
    override suspend fun libraries(): List<OpenSourceComponent> = withContext(Dispatchers.IO) {
        val json = context.resources.openRawResource(R.raw.aboutlibraries).bufferedReader().use { it.readText() }
        AboutLibrariesParser.parse(json)
    }
}

object AboutLibrariesParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): List<OpenSourceComponent> {
        val root = json.parseToJsonElement(text).jsonObject
        val licenses = (root["licenses"] as? JsonObject).orEmpty()
        val libraries = (root["libraries"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
        return libraries.flatMap { entries(it) }
            .groupBy { Triple(it.group, it.holder, it.licenseKey) }
            .map { (key, members) ->
                val single = members.singleOrNull()
                OpenSourceComponent(
                    name = single?.name ?: key.first,
                    holder = key.second,
                    license = license(key.third, licenses[key.third] as? JsonObject),
                    version = single?.version
                )
            }
            .sortedWith(compareBy({ it.name.lowercase() }, { it.holder }))
    }

    private data class Entry(val group: String, val name: String, val holder: String, val licenseKey: String?, val version: String?)

    private fun entries(library: JsonObject): List<Entry> {
        val id = library.string("uniqueId") ?: return emptyList()
        val name = library.string("name")?.takeIf { it.isNotBlank() } ?: id
        val organization = (library["organization"] as? JsonObject)?.string("name")
        val developers = (library["developers"] as? JsonArray).orEmpty()
            .mapNotNull { (it as? JsonObject)?.string("name") }
            .filter { it.isNotBlank() }
            .distinct()
        val group = id.substringBefore(':')
        val holder = organization?.takeIf { it.isNotBlank() } ?: developers.joinToString(", ").ifBlank { group }
        val keys = (library["licenses"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.content }.ifEmpty { listOf(null) }
        return keys.map { Entry(group, name, holder, it, library.string("artifactVersion")) }
    }

    private fun license(key: String?, details: JsonObject?): ComponentLicense {
        val spdx = details?.string("spdxId") ?: key
        return when (spdx?.lowercase()) {
            "apache-2.0", "apache_2_0" -> ComponentLicense.Bundled(LicenseKind.Apache2)

            "mit" -> ComponentLicense.Bundled(LicenseKind.Mit)

            else -> ComponentLicense.External(
                name = details?.string("name") ?: spdx ?: "Unknown license",
                text = details?.string("content")?.takeIf { it.isNotBlank() },
                url = details?.string("url")
            )
        }
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
}

class AboutViewModel(private val source: LicenseSource, private val build: BuildInfo) : EffectViewModel() {
    private val mutable = MutableStateFlow(
        AboutUiState(
            versionName = build.versionName,
            versionCode = build.versionCode,
            buildType = build.buildType,
            components = listOf(AboutDefaults.homing) + AboutDefaults.bundledAssets
        )
    )

    val state: StateFlow<AboutUiState> = mutable.asStateFlow()

    val actions = AboutActions(onBack = ::back)

    init {
        viewModelScope.launch {
            val libraries = attempt { source.libraries() }.getOrDefault(emptyList())
            if (libraries.isNotEmpty()) {
                mutable.update { it.copy(components = listOf(AboutDefaults.homing) + libraries + AboutDefaults.bundledAssets) }
            }
        }
    }
}
