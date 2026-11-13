package com.homingsms.app.ui

import app.cash.turbine.test
import com.homingsms.app.feature.about.AboutDefaults
import com.homingsms.app.feature.about.ComponentLicense
import com.homingsms.app.feature.about.LicenseKind
import com.homingsms.app.feature.about.OpenSourceComponent
import com.homingsms.app.ui.about.AboutLibrariesParser
import com.homingsms.app.ui.about.AboutViewModel
import com.homingsms.app.ui.about.BuildInfo
import com.homingsms.app.ui.connect.PairLinkInbox
import com.homingsms.app.ui.navigation.AppViewModel
import com.homingsms.app.ui.navigation.ConnectRoute
import com.homingsms.app.ui.navigation.HomeRoute
import com.homingsms.app.ui.navigation.OnboardingRoute
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AboutAndAppViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val sample = """
        {
          "libraries": [
            {"uniqueId": "androidx.compose.ui:ui", "artifactVersion": "1.9.0", "name": "Compose UI",
             "organization": {"name": "The Android Open Source Project"}, "licenses": ["Apache-2.0"]},
            {"uniqueId": "androidx.compose.ui:ui-graphics", "artifactVersion": "1.9.0", "name": "Compose Graphics",
             "organization": {"name": "The Android Open Source Project"}, "licenses": ["Apache-2.0"]},
            {"uniqueId": "org.slf4j:slf4j-api", "artifactVersion": "2.0.17", "name": "SLF4J API Module",
             "developers": [{"name": "Ceki Gulcu"}], "licenses": ["MIT"]},
            {"uniqueId": "com.google.android.gms:play-services-base", "artifactVersion": "18.0.1",
             "name": "play-services-base", "licenses": ["ASDKL"]},
            {"uniqueId": "androidx.camera:camera-core", "artifactVersion": "1.5.3", "name": "Camera Core",
             "organization": {"name": "The Android Open Source Project"}, "licenses": ["Apache-2.0", "bsd"]}
          ],
          "licenses": {
            "Apache-2.0": {"name": "The Apache License, Version 2.0", "spdxId": "Apache-2.0", "url": "https://apache.org"},
            "MIT": {"name": "MIT", "spdxId": "MIT", "url": "https://mit"},
            "ASDKL": {"name": "Android Software Development Kit License", "spdxId": "ASDKL", "url": "https://sdk"},
            "bsd": {"name": "BSD License", "url": "https://bsd"}
          }
        }
    """.trimIndent()

    @Test
    fun parserGroupsArtifactsAndKeepsEveryLicense() {
        val components = AboutLibrariesParser.parse(sample)
        val byName = components.groupBy { it.name }

        assertEquals(ComponentLicense.Bundled(LicenseKind.Apache2), byName.getValue("androidx.compose.ui").single().license)
        assertNull(byName.getValue("androidx.compose.ui").single().version)
        assertEquals(ComponentLicense.Bundled(LicenseKind.Mit), byName.getValue("SLF4J API Module").single().license)
        assertEquals("Ceki Gulcu", byName.getValue("SLF4J API Module").single().holder)
        val sdk = byName.getValue("play-services-base").single()
        assertEquals("com.google.android.gms", sdk.holder)
        assertEquals(ComponentLicense.External("Android Software Development Kit License", null, "https://sdk"), sdk.license)
        assertEquals(2, byName.getValue("Camera Core").size)
        assertEquals(components.sortedBy { it.name.lowercase() }.map { it.name }, components.map { it.name })
    }

    @Test
    fun aboutListsHomingGeneratedLibrariesAndBundledNotices() {
        val generated = listOf(OpenSourceComponent("Ktor", "JetBrains", LicenseKind.Apache2))
        val viewModel = AboutViewModel(FakeLicenseSource(generated), BuildInfo("1.0.0", 7L, "release"))

        val state = viewModel.state.value
        assertEquals("1.0.0", state.versionName)
        assertEquals(7L, state.versionCode)
        assertEquals("release", state.buildType)
        assertEquals(listOf(AboutDefaults.homing) + generated + AboutDefaults.bundledAssets, state.components)
        assertTrue(state.components.any { it.license == ComponentLicense.Bundled(LicenseKind.AospNotice) })
        assertTrue(state.components.any { it.license == ComponentLicense.Bundled(LicenseKind.OflAspekta) })
    }

    @Test
    fun startRouteDependsOnOnboarding() {
        val firstRun = FakeAppStatus()
        assertEquals(OnboardingRoute, AppViewModel(firstRun, PairLinkInbox()).startRoute)

        val done = FakeAppStatus(firstRun.settings.value.let { it.copy(general = it.general.copy(onboardingCompleted = true)) })
        assertEquals(HomeRoute, AppViewModel(done, PairLinkInbox()).startRoute)
    }

    @Test
    fun pairLinksRouteToConnectOnlyAfterOnboarding() = runTest {
        val links = PairLinkInbox()
        val done = FakeAppStatus().apply {
            settings.value = settings.value.copy(general = settings.value.general.copy(onboardingCompleted = true))
        }
        val viewModel = AppViewModel(done, links)
        viewModel.routes.test {
            viewModel.onLink("https://example.test/pair?code=ABCD")
            viewModel.onLink("homing://pair?server=https://cloud.example.test&code=ABCD12")
            assertEquals(ConnectRoute, awaitItem())
        }
        assertEquals("homing://pair?server=https://cloud.example.test&code=ABCD12", links.links.value)

        val firstRunLinks = PairLinkInbox()
        val firstRun = AppViewModel(FakeAppStatus(), firstRunLinks)
        firstRun.routes.test {
            firstRun.onLink("homing://pair?server=https://cloud.example.test&code=ABCD12")
            expectNoEvents()
        }
        assertEquals("homing://pair?server=https://cloud.example.test&code=ABCD12", firstRunLinks.links.value)
    }
}
