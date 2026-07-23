package com.example.minimallauncher.data

import org.json.JSONArray
import org.json.JSONObject

/** LauncherBackupと持ち運び可能なJSON文字列を相互変換する。 */
object LauncherBackupCodec {
    private const val SCHEMA_VERSION = 1

    fun encode(backup: LauncherBackup): String = JSONObject().apply {
        put("schemaVersion", SCHEMA_VERSION)
        put("exportedAt", System.currentTimeMillis())
        put("allowedPackages", backup.allowedPackages.toJsonArray())
        put("categories", backup.categories.toJsonObject())
        put("dockPackages", backup.dockPackages.toJsonArray())
        put("dockOrder", backup.dockOrder.toJsonArray())
        put("homeOrder", backup.homeOrder.toJsonArray())
        put("frictionPackages", backup.frictionPackages.toJsonArray())
        put("delayPackages", backup.delayPackages.toJsonArray())
        put("reasonLog", backup.reasonLog.toReasonLogJson())
        put("groupItemOrder", backup.groupItemOrder.toListMapJson())
        put("homeLabelMode", backup.homeLabelMode.name)
    }.toString(2)

    fun decode(raw: String): LauncherBackup {
        val root = JSONObject(raw)
        val version = root.getInt("schemaVersion")
        require(version == SCHEMA_VERSION) { "未対応のバックアップ形式です: $version" }

        return LauncherBackup(
            allowedPackages = root.getJSONArray("allowedPackages").toStringSet(),
            categories = root.getJSONObject("categories").toStringMap(),
            dockPackages = root.getJSONArray("dockPackages").toStringSet(),
            dockOrder = root.getJSONArray("dockOrder").toStringList(),
            homeOrder = root.getJSONArray("homeOrder").toStringList(),
            frictionPackages = root.getJSONArray("frictionPackages").toStringSet(),
            delayPackages = root.getJSONArray("delayPackages").toStringSet(),
            reasonLog = root.getJSONArray("reasonLog").toReasonLog(),
            groupItemOrder = root.getJSONObject("groupItemOrder").toStringListMap(),
            homeLabelMode = runCatching {
                HomeLabelMode.valueOf(root.optString("homeLabelMode", HomeLabelMode.AUTO.name))
            }.getOrDefault(HomeLabelMode.AUTO),
        )
    }
}

internal fun Iterable<String>.toJsonArray(): JSONArray = JSONArray().also { array ->
    forEach(array::put)
}

internal fun JSONArray.toStringList(): List<String> = buildList {
    for (index in 0 until length()) add(getString(index))
}

private fun JSONArray.toStringSet(): Set<String> = toStringList().toSet()

internal fun Map<String, String>.toJsonObject(): JSONObject = JSONObject().also { obj ->
    forEach { (key, value) -> obj.put(key, value) }
}

internal fun JSONObject.toStringMap(): Map<String, String> = buildMap {
    for (key in keys()) put(key, getString(key))
}

internal fun Map<String, List<String>>.toListMapJson(): JSONObject = JSONObject().also { obj ->
    forEach { (key, values) -> obj.put(key, values.toJsonArray()) }
}

internal fun JSONObject.toStringListMap(): Map<String, List<String>> = buildMap {
    for (key in keys()) put(key, getJSONArray(key).toStringList())
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

private fun JSONArray.toReasonLog(): List<ReasonLogEntry> = buildList {
    for (index in 0 until length()) {
        val entry = getJSONObject(index)
        add(
            ReasonLogEntry(
                packageName = entry.getString("packageName"),
                label = entry.getString("label"),
                reason = entry.getString("reason"),
                timestamp = entry.getLong("timestamp"),
            )
        )
    }
}
