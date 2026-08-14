package com.example.minimallauncher.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class LauncherWidgetHost(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val host = AppWidgetHost(appContext, HOST_ID)
    val manager: AppWidgetManager = AppWidgetManager.getInstance(appContext)

    var appWidgetIds by mutableStateOf(loadWidgetIds())
        private set

    init {
        val validIds = appWidgetIds.filter { manager.getAppWidgetInfo(it) != null }
        appWidgetIds = validIds
        // 旧版の単一ID・座標形式も、初回起動時に複数ID形式へ移行する。
        persistIds()
    }

    fun allocateId(): Int = host.allocateAppWidgetId()

    fun accept(newId: Int) {
        if (newId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        if (newId !in appWidgetIds) appWidgetIds = appWidgetIds + newId
        persistIds()
    }

    fun reject(allocatedId: Int) {
        if (allocatedId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            runCatching { host.deleteAppWidgetId(allocatedId) }
        }
    }

    fun remove(widgetId: Int) {
        if (widgetId !in appWidgetIds) return
        appWidgetIds = appWidgetIds - widgetId
        persistIds()
        runCatching { host.deleteAppWidgetId(widgetId) }
    }

    fun move(widgetId: Int, offset: Int) {
        val from = appWidgetIds.indexOf(widgetId)
        if (from == -1) return
        val to = (from + offset).coerceIn(appWidgetIds.indices)
        if (from == to) return
        appWidgetIds = appWidgetIds.toMutableList().apply {
            add(to, removeAt(from))
        }
        persistIds()
    }

    fun createView(context: Context, widgetId: Int): AppWidgetHostView? {
        val info = manager.getAppWidgetInfo(widgetId) ?: return null
        return host.createView(context, widgetId, info).also { it.setAppWidget(widgetId, info) }
    }

    private fun loadWidgetIds(): List<Int> {
        val stored = preferences.getString(KEY_WIDGET_IDS, null)
            ?.split(',')
            ?.mapNotNull(String::toIntOrNull)
            ?.distinct()
            .orEmpty()
        if (stored.isNotEmpty()) return stored
        val legacyId = preferences.getInt(KEY_WIDGET_ID, NO_WIDGET)
        return if (legacyId == NO_WIDGET) emptyList() else listOf(legacyId)
    }

    private fun persistIds() {
        preferences.edit()
            .putString(KEY_WIDGET_IDS, appWidgetIds.joinToString(","))
            .remove(KEY_WIDGET_ID)
            .remove(KEY_POSITION_X)
            .remove(KEY_POSITION_Y)
            .apply()
    }

    companion object {
        const val NO_WIDGET = AppWidgetManager.INVALID_APPWIDGET_ID
        private const val HOST_ID = 240730
        private const val PREFS_NAME = "launcher_widget_host"
        private const val KEY_WIDGET_ID = "home_widget_id"
        private const val KEY_WIDGET_IDS = "home_widget_ids"
        private const val KEY_POSITION_X = "home_widget_x"
        private const val KEY_POSITION_Y = "home_widget_y"
    }
}
