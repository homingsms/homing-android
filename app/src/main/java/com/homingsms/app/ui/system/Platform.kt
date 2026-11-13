package com.homingsms.app.ui.system

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.homingsms.app.feature.onboarding.DeviceMaker
import com.homingsms.app.feature.onboarding.OnboardingPermission
import com.homingsms.app.helpers.SubscriptionsHelper

interface PlatformState {
    val deviceMaker: DeviceMaker

    fun isGranted(permission: String): Boolean

    fun isIgnoringBatteryOptimizations(): Boolean

    fun isSideloaded(): Boolean

    fun ownPhoneNumber(): String?
}

object AppPermissions {
    private const val READ_PHONE_NUMBERS = "android.permission.READ_PHONE_NUMBERS"
    private const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"

    fun forOnboarding(permission: OnboardingPermission, sdkInt: Int = Build.VERSION.SDK_INT): List<String> = when (permission) {
        OnboardingPermission.SendSms -> listOf(Manifest.permission.SEND_SMS)

        OnboardingPermission.ReceiveSms -> listOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_MMS,
            Manifest.permission.RECEIVE_WAP_PUSH
        )

        OnboardingPermission.PhoneState -> listOfNotNull(
            Manifest.permission.READ_PHONE_STATE,
            READ_PHONE_NUMBERS.takeIf { sdkInt >= Build.VERSION_CODES.O }
        )

        OnboardingPermission.Notifications -> listOfNotNull(POST_NOTIFICATIONS.takeIf { sdkInt >= Build.VERSION_CODES.TIRAMISU })
    }

    fun ownerOf(permission: String): OnboardingPermission? = when (permission) {
        Manifest.permission.SEND_SMS -> OnboardingPermission.SendSms

        Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_MMS,
        Manifest.permission.RECEIVE_WAP_PUSH -> OnboardingPermission.ReceiveSms

        Manifest.permission.READ_PHONE_STATE, READ_PHONE_NUMBERS -> OnboardingPermission.PhoneState

        POST_NOTIFICATIONS -> OnboardingPermission.Notifications

        else -> null
    }

    fun makerOf(manufacturer: String, brand: String): DeviceMaker {
        val names = listOf(manufacturer.lowercase(), brand.lowercase())
        fun any(vararg candidates: String) = names.any { name -> candidates.any { name.contains(it) } }
        return when {
            any("xiaomi", "redmi", "poco") -> DeviceMaker.Xiaomi
            any("samsung") -> DeviceMaker.Samsung
            any("huawei", "honor") -> DeviceMaker.Huawei
            any("oppo", "realme", "oneplus") -> DeviceMaker.OppoRealme
            any("tecno", "infinix", "itel") -> DeviceMaker.TecnoInfinix
            else -> DeviceMaker.Other
        }
    }
}

class AndroidPlatformState(private val context: Context) : PlatformState {
    override val deviceMaker: DeviceMaker = AppPermissions.makerOf(Build.MANUFACTURER.orEmpty(), Build.BRAND.orEmpty())

    override fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    override fun isIgnoringBatteryOptimizations(): Boolean {
        val manager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return manager.isIgnoringBatteryOptimizations(context.packageName)
    }

    override fun isSideloaded(): Boolean {
        val installer = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
        return installer !in STORES
    }

    override fun ownPhoneNumber(): String? = try {
        SubscriptionsHelper.getActiveSimCards(context).firstNotNullOfOrNull { it.phoneNumber?.takeIf(String::isNotBlank) }
    } catch (_: SecurityException) {
        null
    }

    private companion object {
        val STORES = setOf("com.android.vending", "com.google.android.feedback", "com.sec.android.app.samsungapps")
    }
}
