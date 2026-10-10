package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Represents the real-time device battery state formatted for senior users
 * and screen-reader announcements to reduce low-battery anxiety.
 */
data class DeviceBatteryState(
    val percentage: Int,
    val isCharging: Boolean,
    val powerSourceLabel: String,
    val statusBadgeText: String,
    val reassuranceSubtitle: String,
    val spokenSummary: String
) {
    val isLowBattery: Boolean
        get() = percentage <= 20 && !isCharging

    val isModerateBattery: Boolean
        get() = percentage in 21..45 && !isCharging
}

object BatteryStatusHelper {

    /**
     * Reads current battery telemetry from Android's sticky [Intent.ACTION_BATTERY_CHANGED]
     * broadcast and [BatteryManager] properties.
     */
    fun readCurrentBatteryState(context: Context): DeviceBatteryState {
        return try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryIntent = context.applicationContext.registerReceiver(null, intentFilter)
            parseBatteryIntent(context, batteryIntent)
        } catch (_: Exception) {
            buildBatteryState(percentage = 85, isCharging = false, powerSource = "On Battery")
        }
    }

    fun parseBatteryIntent(context: Context, intent: Intent?): DeviceBatteryState {
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        val calculatedPercent = if (level >= 0 && scale > 0) {
            ((level.toFloat() / scale.toFloat()) * 100f).toInt().coerceIn(0, 100)
        } else {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val propCapacity = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            if (propCapacity in 0..100) propCapacity else 85
        }

        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL ||
            plugged > 0

        val powerSource = when {
            status == BatteryManager.BATTERY_STATUS_FULL -> "Fully Charged"
            plugged == BatteryManager.BATTERY_PLUGGED_AC -> "Charging (Wall Outlet)"
            plugged == BatteryManager.BATTERY_PLUGGED_USB -> "Charging (USB)"
            plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Charging"
            isCharging -> "Charging"
            else -> "On Battery"
        }

        return buildBatteryState(
            percentage = calculatedPercent,
            isCharging = isCharging,
            powerSource = powerSource
        )
    }

    fun buildBatteryState(
        percentage: Int,
        isCharging: Boolean,
        powerSource: String
    ): DeviceBatteryState {
        val clamped = percentage.coerceIn(0, 100)

        val statusBadgeText = when {
            isCharging && clamped >= 98 -> "FULL • PLUGGED IN"
            isCharging -> "CHARGING NOW"
            clamped <= 20 -> "LOW BATTERY"
            clamped <= 45 -> "MODERATE CHARGE"
            else -> "BATTERY HEALTHY"
        }

        val reassuranceSubtitle = when {
            isCharging && clamped >= 98 -> "Fully charged and connected to power"
            isCharging -> "$powerSource • Safe and charging steadily"
            clamped >= 55 -> "Plenty of power for the rest of today"
            clamped in 35..54 -> "Good charge • Plug in tonight at bedtime"
            clamped in 21..34 -> "Moderate battery • Consider charging soon"
            else -> "Low battery — Please plug in your charger now"
        }

        val spokenSummary = when {
            isCharging ->
                "Your phone battery is at $clamped percent and is currently $powerSource. You have plenty of power."
            clamped <= 20 ->
                "Battery Alert: Your phone battery is low at $clamped percent. Please plug in your charger soon so your phone stays on."
            clamped <= 45 ->
                "Your phone battery is at $clamped percent. You have moderate charge remaining; consider plugging in your charger later today."
            else ->
                "Your phone battery is healthy at $clamped percent. You have plenty of power for today."
        }

        return DeviceBatteryState(
            percentage = clamped,
            isCharging = isCharging,
            powerSourceLabel = powerSource,
            statusBadgeText = statusBadgeText,
            reassuranceSubtitle = reassuranceSubtitle,
            spokenSummary = spokenSummary
        )
    }
}

/**
 * Observes live battery level and charging state changes via a [BroadcastReceiver].
 */
@Composable
fun rememberDeviceBatteryState(): State<DeviceBatteryState> {
    val context = LocalContext.current
    val batteryState = remember {
        mutableStateOf(BatteryStatusHelper.readCurrentBatteryState(context))
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (ctx != null) {
                    batteryState.value = if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                        BatteryStatusHelper.parseBatteryIntent(ctx, intent)
                    } else {
                        BatteryStatusHelper.readCurrentBatteryState(ctx)
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
        }
        try {
            val initialIntent = context.registerReceiver(receiver, filter)
            if (initialIntent != null) {
                batteryState.value = BatteryStatusHelper.parseBatteryIntent(context, initialIntent)
            }
        } catch (_: Exception) {
        }

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
    }

    return batteryState
}
