package com.example.minimallauncher.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

class LauncherWidgetHost(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val host = AppWidgetHost(appContext, HOST_ID)
    val manager: AppWidgetManager = AppWidgetManager.getInstance(appContext)

    var appWidgetId by mutableIntStateOf(preferences.getInt(KEY_WIDGET_ID, NO_WIDGET))
        private set

    var positionX by mutableIntStateOf(preferences.getInt(KEY_POSITION_X, 24))
        private set

    var positionY by mutableIntStateOf(preferences.getInt(KEY_POSITION_Y, 24))
        private set

    init {
        if (appWidgetId != NO_WIDGET && manager.getAppWidgetInfo(appWidgetId) == null) {
            removeCurrent()
        }
    }

    fun allocateId(): Int = host.allocateAppWidgetId()

    fun accept(newId: Int) {
        if (newId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val oldId = appWidgetId
        appWidgetId = newId
        preferences.edit().putInt(KEY_WIDGET_ID, newId).apply()
        if (oldId != NO_WIDGET && oldId != newId) {
            runCatching { host.deleteAppWidgetId(oldId) }
        }
    }

    fun reject(allocatedId: Int) {
        if (allocatedId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            runCatching { host.deleteAppWidgetId(allocatedId) }
        }
    }

    fun removeCurrent() {
        val oldId = appWidgetId
        appWidgetId = NO_WIDGET
        preferences.edit().remove(KEY_WIDGET_ID).apply()
        if (oldId != NO_WIDGET) runCatching { host.deleteAppWidgetId(oldId) }
    }

    fun updatePosition(x: Int, y: Int) {
        positionX = x.coerceAtLeast(0)
        positionY = y.coerceAtLeast(0)
    }

    fun persistPosition() {
        preferences.edit()
            .putInt(KEY_POSITION_X, positionX)
            .putInt(KEY_POSITION_Y, positionY)
            .apply()
    }

    fun createView(context: Context): AppWidgetHostView? {
        val id = appWidgetId
        if (id == NO_WIDGET) return null
        val info = manager.getAppWidgetInfo(id) ?: return null
        return host.createView(context, id, info).also { it.setAppWidget(id, info) }
    }

    companion object {
        const val NO_WIDGET = AppWidgetManager.INVALID_APPWIDGET_ID
        private const val HOST_ID = 240730
        private const val PREFS_NAME = "launcher_widget_host"
        private const val KEY_WIDGET_ID = "home_widget_id"
        private const val KEY_POSITION_X = "home_widget_x"
        private const val KEY_POSITION_Y = "home_widget_y"
    }
}
