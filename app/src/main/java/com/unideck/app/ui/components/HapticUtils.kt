package com.unideck.app.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

enum class UniDeckHapticEffect {
    CONFIRM,
    REJECT,
    TICK,
    CLICK,
    HEAVY
}

/**
 * Maps semantic haptic effects to reliable, permission-free [HapticFeedbackConstants]
 * that fire consistently across physical Android OEM devices.
 */
fun resolveHapticConstant(
    effect: UniDeckHapticEffect,
    sdkInt: Int = Build.VERSION.SDK_INT
): Int = when (effect) {
    UniDeckHapticEffect.CONFIRM -> {
        if (sdkInt >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
    }

    UniDeckHapticEffect.REJECT,
    UniDeckHapticEffect.HEAVY -> HapticFeedbackConstants.LONG_PRESS

    UniDeckHapticEffect.TICK,
    UniDeckHapticEffect.CLICK -> {
        if (sdkInt >= Build.VERSION_CODES.O_MR1) {
            HapticFeedbackConstants.KEYBOARD_TAP
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
    }
}

@Immutable
class UniDeckHaptics(private val view: View) {

    fun perform(effect: UniDeckHapticEffect) {
        val constant = resolveHapticConstant(effect)
        val performed = view.performHapticFeedback(
            constant,
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
        )
        if (!performed && constant != HapticFeedbackConstants.VIRTUAL_KEY) {
            view.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
        }
    }

    fun confirm() = perform(UniDeckHapticEffect.CONFIRM)

    fun reject() = perform(UniDeckHapticEffect.REJECT)

    fun heavy() = perform(UniDeckHapticEffect.HEAVY)

    fun tick() = perform(UniDeckHapticEffect.TICK)

    fun click() = perform(UniDeckHapticEffect.TICK)

    fun contextClick() = perform(UniDeckHapticEffect.TICK)

    fun forAttendanceStatus(@Suppress("UNUSED_PARAMETER") status: String) {
        confirm()
    }
}

@Composable
fun rememberUniDeckHaptics(): UniDeckHaptics {
    val view = LocalView.current
    return remember(view) { UniDeckHaptics(view) }
}
