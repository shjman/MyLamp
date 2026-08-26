package com.yahorshymanchyk.mylamp.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider

// Fixed dark theme for widget — not tied to system dynamic color.
// @Suppress("RestrictedApi"): Android Studio IDE inspection false-positive on ColorProvider(Color).
// ColorProviderKt bytecode (glance 1.1.1) shows no @RestrictTo on the Color overload — only the
// adjacent @ColorRes resId overload is restricted. detekt/ktlint/Android Lint (CLI) do not flag this.
@Suppress("RestrictedApi")
internal object LampWidgetColors {
    val CardBackground = ColorProvider(color = Color(color = 0xFF1C1C1E))
    val Accent = ColorProvider(color = Color(color = 0xFFFFB300))
    val PrimaryText = ColorProvider(color = Color(color = 0xFFFFFFFF))
    val MutedText = ColorProvider(color = Color(color = 0xFF8A8A8E))
    val StepButtonSurface = ColorProvider(color = Color(color = 0xFF3A3A3C))
    val StepButtonSurfaceMuted = ColorProvider(color = Color(color = 0xFF2A2A2C))
}
