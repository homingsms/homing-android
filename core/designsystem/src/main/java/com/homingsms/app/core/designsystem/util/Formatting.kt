package com.homingsms.app.core.designsystem.util

import android.text.format.DateFormat
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.homingsms.app.core.designsystem.R
import com.homingsms.app.core.designsystem.theme.LocalHomingClock
import java.text.NumberFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val JUST_NOW_MILLIS = 60_000L

@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales.get(0)

@Composable
@ReadOnlyComposable
fun formatCount(value: Long): String = NumberFormat.getIntegerInstance(currentLocale()).format(value)

@Composable
@ReadOnlyComposable
fun formatCount(value: Int): String = formatCount(value.toLong())

@Composable
@ReadOnlyComposable
fun formatPercent(fraction: Float): String = NumberFormat.getPercentInstance(currentLocale()).format(fraction.toDouble())

@Composable
@ReadOnlyComposable
fun simLabel(slot: Int): String = stringResource(R.string.homing_sim_label, slot + 1)

@Composable
fun formatRelativeTime(epochMillis: Long?): String {
    if (epochMillis == null) return stringResource(R.string.homing_time_never)
    val now = LocalHomingClock.current()
    if (now - epochMillis in 0 until JUST_NOW_MILLIS) return stringResource(R.string.homing_time_just_now)
    return DateUtils.getRelativeTimeSpanString(
        epochMillis,
        now,
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
}

@Composable
fun formatClockTime(epochMillis: Long): String {
    val context = LocalContext.current
    return DateFormat.getTimeFormat(context).format(Date(epochMillis))
}

@Composable
fun formatDateTime(epochMillis: Long): String {
    val context = LocalContext.current
    val now = LocalHomingClock.current()
    val flags = if (sameDay(epochMillis, now)) {
        DateUtils.FORMAT_SHOW_TIME
    } else {
        DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH
    }
    return DateUtils.formatDateTime(context, epochMillis, flags)
}

@Composable
fun formatFullDateTime(epochMillis: Long): String {
    val context = LocalContext.current
    val flags = DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or
        DateUtils.FORMAT_ABBREV_MONTH
    return DateUtils.formatDateTime(context, epochMillis, flags)
}

@Composable
fun formatBytes(bytes: Long): String = Formatter.formatShortFileSize(LocalContext.current, bytes)

private fun sameDay(first: Long, second: Long): Boolean {
    val a = Calendar.getInstance().apply { timeInMillis = first }
    val b = Calendar.getInstance().apply { timeInMillis = second }
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}
