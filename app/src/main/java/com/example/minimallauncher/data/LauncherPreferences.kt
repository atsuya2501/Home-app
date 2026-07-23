package com.example.minimallauncher.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/** ランチャーの設定・配置・起動理由ログをSharedPreferencesへ保存する窓口。 */
class LauncherPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAllowed(): Set<String> = readStringSet(KEY_ALLOWED)
    fun setAllowed(packages: Set<String>) = writeStringSet(KEY_ALLOWED, packages)

    fun getCategories(): Map<String, String> = readStringMap(KEY_CATEGORIES)
    fun setCategories(categories: Map<String, String>) =
        writeJson(KEY_CATEGORIES, categories.toJsonObject())

    fun getDock(): Set<String> = readStringSet(KEY_DOCK)
    fun setDock(packages: Set<String>) = writeStringSet(KEY_DOCK, packages)

    fun getDockOrder(): List<String> = readStringList(KEY_DOCK_ORDER)
    fun setDockOrder(keys: List<String>) = writeStringList(KEY_DOCK_ORDER, keys)

    fun getOrder(): List<String> = readStringList(KEY_ORDER)
    fun setOrder(keys: List<String>) = writeStringList(KEY_ORDER, keys)

    fun getFriction(): Set<String> = readStringSet(KEY_FRICTION)
    fun setFriction(packages: Set<String>) = writeStringSet(KEY_FRICTION, packages)

    /** 未保存時だけInstagramを待機対象にする。空集合を保存した場合はその選択を尊重する。 */
    fun getDelay(): Set<String> = prefs.getStringSet(KEY_DELAY, null)?.toSet()
        ?: setOf(DEFAULT_DELAY_PACKAGE)

    fun setDelay(packages: Set<String>) = writeStringSet(KEY_DELAY, packages)

    fun getGroupOrder(): Map<String, List<String>> = readStringListMap(KEY_GROUP_ORDER)
    fun setGroupOrder(order: Map<String, List<String>>) =
        writeJson(KEY_GROUP_ORDER, order.toListMapJson())

    fun getHomeLabelMode(): HomeLabelMode = runCatching {
        HomeLabelMode.valueOf(prefs.getString(KEY_HOME_LABEL_MODE, HomeLabelMode.AUTO.name).orEmpty())
    }.getOrDefault(HomeLabelMode.AUTO)

    fun setHomeLabelMode(mode: HomeLabelMode) {
        prefs.edit { putString(KEY_HOME_LABEL_MODE, mode.name) }
    }

    fun getReasonLog(): List<ReasonLogEntry> {
        val raw = prefs.getString(KEY_REASON_LOG, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val entry = array.getJSONObject(index)
                    // pkg/tsは旧バージョンとの互換性のために読む。
                    add(
                        ReasonLogEntry(
                            packageName = entry.optString("packageName", entry.optString("pkg")),
                            label = entry.getString("label"),
                            reason = entry.getString("reason"),
                            timestamp = if (entry.has("timestamp")) {
                                entry.getLong("timestamp")
                            } else {
                                entry.getLong("ts")
                            },
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun setReasonLog(entries: List<ReasonLogEntry>) {
        writeJson(KEY_REASON_LOG, entries.toReasonLogJson())
    }

    fun exportBackup(): String = LauncherBackupCodec.encode(snapshot())

    /** JSON全体の検証に成功した場合だけ、全設定を1回の編集で置き換える。 */
    fun importBackup(raw: String): Boolean {
        val backup = runCatching { LauncherBackupCodec.decode(raw) }.getOrNull() ?: return false
        restore(backup)
        return true
    }

    private fun snapshot(): LauncherBackup = LauncherBackup(
        allowedPackages = getAllowed(),
        categories = getCategories(),
        dockPackages = getDock(),
        dockOrder = getDockOrder(),
        homeOrder = getOrder(),
        frictionPackages = getFriction(),
        delayPackages = getDelay(),
        reasonLog = getReasonLog(),
        groupItemOrder = getGroupOrder(),
        homeLabelMode = getHomeLabelMode(),
    )

    private fun restore(backup: LauncherBackup) {
        prefs.edit {
            putStringSet(KEY_ALLOWED, backup.allowedPackages)
            putString(KEY_CATEGORIES, backup.categories.toJsonObject().toString())
            putStringSet(KEY_DOCK, backup.dockPackages)
            putString(KEY_DOCK_ORDER, backup.dockOrder.toJsonArray().toString())
            putString(KEY_ORDER, backup.homeOrder.toJsonArray().toString())
            putStringSet(KEY_FRICTION, backup.frictionPackages)
            putStringSet(KEY_DELAY, backup.delayPackages)
            putString(KEY_REASON_LOG, backup.reasonLog.toReasonLogJson().toString())
            putString(KEY_GROUP_ORDER, backup.groupItemOrder.toListMapJson().toString())
            putString(KEY_HOME_LABEL_MODE, backup.homeLabelMode.name)
        }
    }

    private fun readStringSet(key: String): Set<String> =
        prefs.getStringSet(key, emptySet())?.toSet() ?: emptySet()

    private fun writeStringSet(key: String, values: Set<String>) {
        prefs.edit { putStringSet(key, values) }
    }

    private fun readStringList(key: String): List<String> = readJson(key) {
        JSONArray(it).toStringList()
    } ?: emptyList()

    private fun writeStringList(key: String, values: List<String>) {
        writeJson(key, values.toJsonArray())
    }

    private fun readStringMap(key: String): Map<String, String> = readJson(key) {
        JSONObject(it).toStringMap()
    } ?: emptyMap()

    private fun readStringListMap(key: String): Map<String, List<String>> = readJson(key) {
        JSONObject(it).toStringListMap()
    } ?: emptyMap()

    private fun <T> readJson(key: String, decode: (String) -> T): T? {
        val raw = prefs.getString(key, null) ?: return null
        return runCatching { decode(raw) }.getOrNull()
    }

    private fun writeJson(key: String, value: Any) {
        prefs.edit { putString(key, value.toString()) }
    }

    companion object {
        private const val PREFS_NAME = "launcher_prefs"
        private const val DEFAULT_DELAY_PACKAGE = "com.instagram.android"
        private const val KEY_ALLOWED = "allowed_packages"
        private const val KEY_CATEGORIES = "app_categories"
        private const val KEY_DOCK = "dock_packages"
        private const val KEY_DOCK_ORDER = "dock_order"
        private const val KEY_ORDER = "home_order"
        private const val KEY_FRICTION = "friction_packages"
        private const val KEY_DELAY = "delay_packages"
        private const val KEY_REASON_LOG = "reason_log"
        private const val KEY_GROUP_ORDER = "group_item_order"
        private const val KEY_HOME_LABEL_MODE = "home_label_mode"
    }
}

private fun List<ReasonLogEntry>.toReasonLogJson(): JSONArray = JSONArray().also { array ->
    forEach { entry ->
        array.put(JSONObject().apply {
            put("packageName", entry.packageName)
            put("label", entry.label)
            put("reason", entry.reason)
            put("timestamp", entry.timestamp)
        })
    }
}
