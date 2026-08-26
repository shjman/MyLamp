// Multiple public top-level declarations in this file (WidgetVariant enum + LampWidgetCard function).
// detekt MatchingDeclarationName counts only class/enum/object, not functions — so it sees a single
// class. Suppressed because moving WidgetVariant to its own file is out of scope for this refactor.
@file:Suppress("MatchingDeclarationName")

package com.yahorshymanchyk.mylamp.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.yahorshymanchyk.mylamp.R

enum class WidgetVariant(
    val widgetVariantValue: String,
) {
    TwoByTwo(WIDGET_VARIANT_2X2),
    ThreeByTwo(WIDGET_VARIANT_3X2),
}

// Entry point shared by both widget sizes. 2x2 is power-only (just a big toggle, no brightness
// UI — there isn't room for comfortable +/- tap targets next to it at this width). 3x2 has the
// width for both power and brightness controls together.
@Composable
fun LampWidgetCard(
    state: LampWidgetUiState,
    variant: WidgetVariant,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(LampWidgetColors.CardBackground)
                .cornerRadius(radius = 22.dp)
                .padding(all = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WidgetHeader(isOn = state.isOn)
        Spacer(modifier = GlanceModifier.height(height = 6.dp))
        when (variant) {
            WidgetVariant.TwoByTwo -> PowerOnlyRow(isOn = state.isOn, widgetVariantValue = variant.widgetVariantValue)
            WidgetVariant.ThreeByTwo ->
                WidgetControlsRow(state = state, widgetVariantValue = variant.widgetVariantValue)
        }
    }
}

@Composable
private fun WidgetHeader(isOn: Boolean) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Cabinet",
            style = TextStyle(color = LampWidgetColors.PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Medium),
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        StatusLabel(isOn = isOn)
    }
}

// 2x2: no brightness row at all, so the power button can take the full width's worth of tap
// target instead of sharing space with anything else.
@Composable
private fun PowerOnlyRow(
    isOn: Boolean,
    widgetVariantValue: String,
) {
    Box(
        modifier = GlanceModifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        PowerButton(isOn = isOn, widgetVariantValue = widgetVariantValue, size = 64.dp, iconSize = 30.dp)
    }
}

// 3x2: power button plus the (bordered, bigger-tap-target) brightness controls in one row.
@Composable
private fun WidgetControlsRow(
    state: LampWidgetUiState,
    widgetVariantValue: String,
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PowerButton(isOn = state.isOn, widgetVariantValue = widgetVariantValue, size = 48.dp, iconSize = 24.dp)
        Spacer(modifier = GlanceModifier.width(width = 10.dp))
        BrightnessRow(
            isOn = state.isOn,
            brightnessPct = state.brightnessPct,
            widgetVariantValue = widgetVariantValue,
        )
    }
}

// Power toggling is always clickable regardless of current state (only the brightness buttons
// get functionally/visually disabled while off). Background color itself now doubles as the
// on/off indicator (amber when on, the same muted gray as a disabled brightness button when
// off) — relying on the "ON"/"OFF" header text alone was hard to read at a glance.
@Composable
private fun PowerButton(
    isOn: Boolean,
    widgetVariantValue: String,
    size: Dp,
    iconSize: Dp,
    modifier: GlanceModifier = GlanceModifier,
) {
    val backgroundColor = if (isOn) LampWidgetColors.Accent else LampWidgetColors.StepButtonSurface
    Box(
        modifier =
            modifier
                .size(size = size)
                .background(backgroundColor)
                .cornerRadius(radius = size / 2)
                .clickable(
                    actionRunCallback<TogglePowerAction>(
                        actionParametersOf(widgetVariantKey to widgetVariantValue),
                    ),
                ),
        contentAlignment = Alignment.Center,
    ) {
        // Local vector drawable — avoids font-coverage issues of Unicode power glyph (U+23FB).
        // Fill color is baked into the drawable (dark, matches the card background) — it reads
        // fine on both the amber (on) and gray (off) button backgrounds.
        Image(
            provider = ImageProvider(R.drawable.ic_widget_power),
            contentDescription = "Power",
            modifier = GlanceModifier.size(size = iconSize),
        )
    }
}

@Composable
private fun BrightnessRow(
    isOn: Boolean,
    brightnessPct: Int,
    widgetVariantValue: String,
    modifier: GlanceModifier = GlanceModifier,
) {
    Row(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrightnessStepButton(
            label = "−",
            enabled = isOn,
            delta = -BRIGHTNESS_STEP,
            widgetVariantValue = widgetVariantValue,
        )
        Spacer(modifier = GlanceModifier.width(width = 10.dp))
        Text(
            text = "$brightnessPct%",
            style =
                TextStyle(
                    color = if (isOn) LampWidgetColors.PrimaryText else LampWidgetColors.MutedText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
        )
        Spacer(modifier = GlanceModifier.width(width = 10.dp))
        BrightnessStepButton(
            label = "+",
            enabled = isOn,
            delta = BRIGHTNESS_STEP,
            widgetVariantValue = widgetVariantValue,
        )
    }
}

// Not clickable at all while disabled — functional disable, not just a dimmed look. A filled
// rounded surface (not just bare text) makes the tap target's boundary visible, sized to stay
// comfortable next to the power button on 3x2's width.
@Composable
private fun BrightnessStepButton(
    label: String,
    enabled: Boolean,
    delta: Int,
    widgetVariantValue: String,
) {
    val surfaceColor = if (enabled) LampWidgetColors.StepButtonSurface else LampWidgetColors.StepButtonSurfaceMuted
    val baseModifier =
        GlanceModifier
            .size(size = 36.dp)
            .background(surfaceColor)
            .cornerRadius(radius = 12.dp)
    val stepModifier =
        if (enabled) {
            baseModifier.clickable(
                actionRunCallback<AdjustBrightnessAction>(
                    actionParametersOf(widgetVariantKey to widgetVariantValue, brightnessDeltaKey to delta),
                ),
            )
        } else {
            baseModifier
        }
    Box(modifier = stepModifier, contentAlignment = Alignment.Center) {
        Text(
            text = label,
            style =
                TextStyle(
                    color = if (enabled) LampWidgetColors.PrimaryText else LampWidgetColors.MutedText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                ),
        )
    }
}

@Composable
private fun StatusLabel(
    isOn: Boolean,
    modifier: GlanceModifier = GlanceModifier,
) {
    Text(
        text = if (isOn) "ON" else "OFF",
        modifier = modifier,
        style =
            TextStyle(
                color = if (isOn) LampWidgetColors.Accent else LampWidgetColors.MutedText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
    )
}
