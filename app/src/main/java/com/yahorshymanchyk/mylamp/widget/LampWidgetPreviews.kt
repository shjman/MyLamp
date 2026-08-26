package com.yahorshymanchyk.mylamp.widget

import androidx.compose.runtime.Composable
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview

// IDE-only previews (androidx.glance:glance-preview + glance-appwidget-preview) — render in
// Android Studio's Preview pane without a build/install cycle. Dimensions match the real
// AppWidgetProviderInfo minWidth/minHeight declared in res/xml/lamp_widget_*_info.xml. Kept in a
// separate file so LampWidgetContent.kt doesn't hit detekt's TooManyFunctions threshold.

@OptIn(ExperimentalGlancePreviewApi::class)
@Composable
@Preview(widthDp = 110, heightDp = 110)
private fun LampWidgetCardTwoByTwoOnPreview() {
    LampWidgetCard(state = LampWidgetUiState(isOn = true, brightnessPct = 50), variant = WidgetVariant.TwoByTwo)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Composable
@Preview(widthDp = 110, heightDp = 110)
private fun LampWidgetCardTwoByTwoOffPreview() {
    LampWidgetCard(state = LampWidgetUiState(isOn = false, brightnessPct = 50), variant = WidgetVariant.TwoByTwo)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Composable
@Preview(widthDp = 180, heightDp = 110)
private fun LampWidgetCardThreeByTwoOnPreview() {
    LampWidgetCard(state = LampWidgetUiState(isOn = true, brightnessPct = 50), variant = WidgetVariant.ThreeByTwo)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Composable
@Preview(widthDp = 180, heightDp = 110)
private fun LampWidgetCardThreeByTwoOffPreview() {
    LampWidgetCard(state = LampWidgetUiState(isOn = false, brightnessPct = 50), variant = WidgetVariant.ThreeByTwo)
}
