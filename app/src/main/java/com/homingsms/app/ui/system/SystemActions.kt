package com.homingsms.app.ui.system

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.homingsms.app.feature.onboarding.DeviceMaker

sealed interface SystemAction {
    data class RequestPermissions(val permissions: List<String>) : SystemAction

    data object OpenAppSettings : SystemAction

    data object RequestBatteryExemption : SystemAction

    data object OpenBatteryOptimizationSettings : SystemAction

    data class OpenOemSettings(val maker: DeviceMaker) : SystemAction

    data object OpenNetworkSettings : SystemAction

    data object OpenBatterySaverSettings : SystemAction

    data object OpenSimSettings : SystemAction

    data class ViewFile(val uri: String, val mimeType: String) : SystemAction

    data class ShareFile(val uri: String, val mimeType: String) : SystemAction

    data class OpenUrl(val url: String) : SystemAction
}

data class PermissionOutcome(val permission: String, val granted: Boolean, val canAskAgain: Boolean)

object SystemIntents {
    fun candidates(action: SystemAction, packageName: String): List<Intent> = when (action) {
        is SystemAction.RequestPermissions -> emptyList()

        SystemAction.OpenAppSettings -> listOf(appDetails(packageName), Intent(Settings.ACTION_SETTINGS))

        SystemAction.RequestBatteryExemption -> listOf(
            batteryExemption(packageName),
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            appDetails(packageName)
        )

        SystemAction.OpenBatteryOptimizationSettings -> listOf(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            appDetails(packageName)
        )

        is SystemAction.OpenOemSettings -> oemComponents(action.maker).map { (pkg, cls) ->
            Intent().setComponent(ComponentName(pkg, cls))
        } + appDetails(packageName)

        SystemAction.OpenNetworkSettings -> listOf(
            Intent(Settings.ACTION_WIRELESS_SETTINGS),
            Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )

        SystemAction.OpenBatterySaverSettings -> listOf(
            Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS),
            Intent(Intent.ACTION_POWER_USAGE_SUMMARY),
            Intent(Settings.ACTION_SETTINGS)
        )

        SystemAction.OpenSimSettings -> listOf(
            Intent(SIM_SETTINGS),
            Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS),
            Intent(Settings.ACTION_WIRELESS_SETTINGS)
        )

        is SystemAction.ViewFile -> listOf(
            Intent(Intent.ACTION_VIEW).setDataAndType(action.uri.toUri(), action.mimeType)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
            Intent.createChooser(shareIntent(action.uri.toUri(), action.mimeType), null)
        )

        is SystemAction.ShareFile -> listOf(Intent.createChooser(shareIntent(action.uri.toUri(), action.mimeType), null))

        is SystemAction.OpenUrl -> listOf(Intent(Intent.ACTION_VIEW, action.url.toUri()).addCategory(Intent.CATEGORY_BROWSABLE))
    }

    fun oemComponents(maker: DeviceMaker): List<Pair<String, String>> = when (maker) {
        DeviceMaker.Xiaomi -> listOf(
            "com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity",
            "com.miui.powerkeeper" to "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
        )

        DeviceMaker.Samsung -> listOf(
            "com.samsung.android.lool" to "com.samsung.android.sm.battery.ui.BatteryActivity",
            "com.samsung.android.sm" to "com.samsung.android.sm.battery.ui.BatteryActivity"
        )

        DeviceMaker.Huawei -> listOf(
            "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            "com.huawei.systemmanager" to "com.huawei.systemmanager.optimize.process.ProtectActivity"
        )

        DeviceMaker.OppoRealme -> listOf(
            "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
            "com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity",
            "com.oppo.safe" to "com.oppo.safe.permission.startup.StartupAppListActivity"
        )

        DeviceMaker.TecnoInfinix -> listOf(
            "com.transsion.phonemaster" to "com.cyin.himgr.autostart.AutoStartActivity"
        )

        DeviceMaker.Other -> emptyList()
    }

    fun launch(context: Context, action: SystemAction): Boolean = candidates(action, context.packageName).any { intent ->
        try {
            if (context.findActivity() == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    @SuppressLint("BatteryLife")
    private fun batteryExemption(packageName: String): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:$packageName".toUri())

    private fun appDetails(packageName: String): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))

    private fun shareIntent(uri: Uri, mimeType: String): Intent = Intent(Intent.ACTION_SEND)
        .setType(mimeType)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    private const val SIM_SETTINGS = "android.settings.MANAGE_ALL_SIM_PROFILES_SETTINGS"
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun rememberSystemActionLauncher(onPermissionsResult: (List<PermissionOutcome>) -> Unit = {}): (SystemAction) -> Unit {
    val context = LocalContext.current
    val callback = rememberUpdatedState(onPermissionsResult)
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val activity = context.findActivity()
        val outcomes = results.map { (permission, granted) ->
            val canAsk = activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, permission) } ?: true
            PermissionOutcome(permission, granted, granted || canAsk)
        }
        callback.value(outcomes)
    }
    return remember(context, permissions) {
        { action ->
            if (action is SystemAction.RequestPermissions) {
                if (action.permissions.isNotEmpty()) permissions.launch(action.permissions.toTypedArray())
            } else {
                SystemIntents.launch(context, action)
            }
        }
    }
}
