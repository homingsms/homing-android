package com.homingsms.app.feature.connect

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import com.homingsms.app.core.testing.HomingTestContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConnectScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun secretDialogShowsFullKeyOnceAndAcknowledges() {
        var saved = 0
        composeRule.setContent {
            HomingTestContent { ConnectScreen(ConnectSamples.secretOnce, ConnectActions(onSecretSaved = { saved++ })) }
        }
        composeRule.onNode(isDialog()).assertIsDisplayed()
        composeRule.onNodeWithText("hk_live_9c2e1f7a4b8d6e3c5a0b9f1d").assertIsDisplayed()
        composeRule.onNodeWithText("This is the only time", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertDoesNotExist()
        composeRule.onNodeWithText("I've saved it").performClick()
        assertEquals(1, saved)
    }

    @Test
    fun secretDialogIgnoresBackPress() {
        var saved = 0
        composeRule.setContent {
            HomingTestContent { ConnectScreen(ConnectSamples.secretOnce, ConnectActions(onSecretSaved = { saved++ })) }
        }
        Espresso.pressBack()
        composeRule.onNode(isDialog()).assertIsDisplayed()
        assertEquals(0, saved)
    }

    @Test
    fun secretCanBeHidden() {
        composeRule.setContent { HomingTestContent { ConnectScreen(ConnectSamples.secretOnce, ConnectActions()) } }
        composeRule.onNodeWithContentDescription("Hide API key").performClick()
        composeRule.onNodeWithText("hk_live_9c2e1f7a4b8d6e3c5a0b9f1d").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("API key, hidden").assertIsDisplayed()
    }

    @Test
    fun createKeyDialogReportsNameAndScopes() {
        val names = mutableListOf<String>()
        val scopes = mutableListOf<ApiScope>()
        var created = 0
        val state = ConnectSamples.connected.copy(createKey = CreateKeyDraft())
        composeRule.setContent {
            HomingTestContent {
                ConnectScreen(
                    state,
                    ConnectActions(
                        onCreateKeyNameChange = { names += it },
                        onCreateKeyScopeToggle = { scopes += it },
                        onCreateKeyConfirm = { created++ }
                    )
                )
            }
        }
        composeRule.onNodeWithText("Create").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("Name").performTextInput("Billing")
        composeRule.onNodeWithText("inbox:read").performScrollTo().performClick()
        assertEquals(listOf("Billing"), names)
        assertEquals(listOf(ApiScope.InboxRead), scopes)
        assertEquals(0, created)
    }

    @Test
    fun adminScopeImpliesAll() {
        val state = ConnectSamples.connected.copy(createKey = CreateKeyDraft(name = "Ops", scopes = setOf(ApiScope.Admin)))
        composeRule.setContent { HomingTestContent { ConnectScreen(state, ConnectActions()) } }
        composeRule.onNodeWithText("webhooks:manage").performScrollTo()
        composeRule.onNode(hasText("webhooks:manage")).assertIsOn().assertIsNotEnabled()
    }

    @Test
    fun revokeRequiresConfirmation() {
        val revoked = mutableListOf<String>()
        composeRule.setContent {
            HomingTestContent { ConnectScreen(ConnectSamples.connected, ConnectActions(onRevokeKey = { revoked += it })) }
        }
        composeRule.onNodeWithContentDescription("Revoke Support inbox").performScrollTo().performClick()
        composeRule.onNodeWithText("Revoke Support inbox?").assertIsDisplayed()
        assertTrue(revoked.isEmpty())
        composeRule.onNode(hasText("Revoke") and hasAnyAncestor(isDialog())).performClick()
        assertEquals(listOf("key_2"), revoked)
    }

    @Test
    fun localServerShowsBaseUrlAndCurl() {
        composeRule.setContent { HomingTestContent { ConnectScreen(ConnectSamples.connected, ConnectActions()) } }
        composeRule.onNodeWithText("http://192.168.1.20:8080/api/v1").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("QR code for http://192.168.1.20:8080/api/v1").assertExists()
        composeRule.onNodeWithText("curl -X POST http://192.168.1.20:8080/api/v1/messages", substring = true).assertExists()
    }

    @Test
    fun pairingCodeSubmitsAndShowsError() {
        var submitted = 0
        composeRule.setContent {
            HomingTestContent {
                ConnectScreen(ConnectSamples.notPaired, ConnectActions(pairing = PairingActions(onSubmitPairingCode = { submitted++ })))
            }
        }
        composeRule.onNodeWithText("That code expired. Create a new one in the console.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Pair").performScrollTo().performClick()
        assertEquals(1, submitted)
    }

    @Test
    fun unpairRequiresConfirmation() {
        var unpaired = 0
        composeRule.setContent {
            HomingTestContent {
                ConnectScreen(ConnectSamples.connected, ConnectActions(pairing = PairingActions(onUnpair = { unpaired++ })))
            }
        }
        composeRule.onNodeWithText("Unpair").performScrollTo().performClick()
        composeRule.onNodeWithText("Unpair this phone?").assertIsDisplayed()
        assertEquals(0, unpaired)
        composeRule.onNode(hasText("Unpair") and hasAnyAncestor(isDialog())).performClick()
        assertEquals(1, unpaired)
    }

    @Test
    fun deniedCameraOffersSettingsAndTypingFallback() {
        var settings = 0
        var closed = 0
        composeRule.setContent {
            HomingTestContent {
                QrScannerContent(
                    permission = CameraPermissionState.Denied,
                    onRequestPermission = {},
                    onOpenSettings = { settings++ },
                    onClose = { closed++ }
                )
            }
        }
        composeRule.onNodeWithText("Camera access is off").assertIsDisplayed()
        composeRule.onNodeWithText("Open settings").performClick()
        composeRule.onNodeWithText("Type the code instead").performClick()
        assertEquals(1, settings)
        assertEquals(1, closed)
    }
}
