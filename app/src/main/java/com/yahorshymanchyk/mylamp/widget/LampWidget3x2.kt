package com.yahorshymanchyk.mylamp.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState

class LampWidget3x2Widget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            val prefs = currentState<Preferences>()
            LampWidgetCard(state = LampWidgetUiState.from(prefs), variant = WidgetVariant.ThreeByTwo)
        }
    }
}

class LampWidget3x2Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LampWidget3x2Widget()
}
