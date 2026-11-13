package com.homingsms.app.feature.about

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.homingsms.app.core.testing.HomingTestContent
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AboutScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsVersionAndOpensFontLicense() {
        composeRule.setContent { HomingTestContent { AboutScreen(SampleAbout, AboutActions()) } }
        composeRule.onNodeWithText("Version 1.0.0 (1), debug build").assertIsDisplayed()
        composeRule.onNodeWithText("Aspekta").performScrollTo().performClick()
        composeRule.onNodeWithText("SIL OPEN FONT LICENSE", substring = true).assertExists()
    }

    @Test
    fun aospNoticeNamesOriginalCopyrightHolders() {
        composeRule.setContent { HomingTestContent { AboutScreen(SampleAbout, AboutActions()) } }
        composeRule.onNodeWithText("MMS PDU encoding").performScrollTo().performClick()
        composeRule.onNodeWithText("Copyright (C) 2007-2008 The Android Open Source Project", substring = true).assertExists()
    }

    @Test
    fun everyComponentHasReadableLicenseText() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        LicenseKind.entries.forEach { kind ->
            val text = context.resources.openRawResource(kind.textRes).bufferedReader().use { it.readText() }
            assertTrue(kind.name, text.length > 200)
        }
    }
}
