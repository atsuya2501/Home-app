package com.example.minimallauncher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * ホーム画面に表示する「許可リスト」と、各アプリの「グループ（カテゴリ）」を
 * SharedPreferences に永続化する。
 * - 許可リスト  : 許可したアプリのパッケージ名の集合
 * - グループ    : パッケージ名 → グループ名 のマップ（未設定のアプリは持たない）
 */
class AllowListStore(context: Context) {

    private val prefs =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 許可済みパッケージ名の集合を取得する。 */
    fun getAllowed(): Set<String> {
        // getStringSet が返す集合は変更してはいけないため、コピーして返す
        return prefs.getStringSet(KEY_ALLOWED, emptySet())?.toSet() ?: emptySet()
    }

    /** 許可リストを丸ごと保存する。 */
    fun setAllowed(packages: Set<String>) {
        prefs.edit()
            .putStringSet(KEY_ALLOWED, packages)
            .apply()
    }

    /** パッケージ名 → グループ名 のマップを取得する。 */
    fun getCategories(): Map<String, String> {
        val raw = prefs.getString(KEY_CATEGORIES, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            buildMap {
                for (key in obj.keys()) {
                    put(key, obj.getString(key))
                }
            }
        } catch (e: Exception) {
            // 壊れたデータが入っていても落とさず空扱いにする
            emptyMap()
        }
    }

    /** グループ割り当てを丸ごと保存する。 */
    fun setCategories(categories: Map<String, String>) {
        val obj = JSONObject()
        for ((pkg, category) in categories) {
            obj.put(pkg, category)
        }
        prefs.edit()
            .putString(KEY_CATEGORIES, obj.toString())
            .apply()
    }

    /** 下段ドックに固定するアプリのパッケージ名の集合を取得する。 */
    fun getDock(): Set<String> {
        return prefs.getStringSet(KEY_DOCK, emptySet())?.toSet() ?: emptySet()
    }

    /** 下段ドックの内容を保存する。 */
    fun setDock(packages: Set<String>) {
        prefs.edit()
            .putStringSet(KEY_DOCK, packages)
            .apply()
    }

    /** ホーム項目の並び順（HomeItem.key の配列）を取得する。 */
    fun getOrder(): List<String> {
        val raw = prefs.getString(KEY_ORDER, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    add(arr.getString(i))
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** ホーム項目の並び順を保存する。 */
    fun setOrder(keys: List<String>) {
        val arr = JSONArray()
        for (key in keys) {
            arr.put(key)
        }
        prefs.edit()
            .putString(KEY_ORDER, arr.toString())
            .apply()
    }

    /** 起動理由入力を必須にするアプリのパッケージ名の集合を取得する。 */
    fun getFriction(): Set<String> {
        return prefs.getStringSet(KEY_FRICTION, emptySet())?.toSet() ?: emptySet()
    }

    /** 起動理由入力を必須にするアプリの集合を保存する。 */
    fun setFriction(packages: Set<String>) {
        prefs.edit()
            .putStringSet(KEY_FRICTION, packages)
            .apply()
    }

    /** 起動理由ログを取得する（保存されている順のまま返す）。 */
    fun getReasonLog(): List<ReasonLogEntry> {
        val raw = prefs.getString(KEY_REASON_LOG, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        ReasonLogEntry(
                            packageName = obj.getString("pkg"),
                            label = obj.getString("label"),
                            reason = obj.getString("reason"),
                            timestamp = obj.getLong("ts"),
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // 壊れたデータが入っていても落とさず空扱いにする
            emptyList()
        }
    }

    /** 起動理由ログを丸ごと保存する。 */
    fun setReasonLog(entries: List<ReasonLogEntry>) {
        val arr = JSONArray()
        for (entry in entries) {
            val obj = JSONObject()
            obj.put("pkg", entry.packageName)
            obj.put("label", entry.label)
            obj.put("reason", entry.reason)
            obj.put("ts", entry.timestamp)
            arr.put(obj)
        }
        prefs.edit()
            .putString(KEY_REASON_LOG, arr.toString())
            .apply()
    }

    /** グループ内でのアプリの並び順（グループ名 → パッケージ名の配列）を取得する。 */
    fun getGroupOrder(): Map<String, List<String>> {
        val raw = prefs.getString(KEY_GROUP_ORDER, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            buildMap {
                for (key in obj.keys()) {
                    val arr = obj.getJSONArray(key)
                    put(key, buildList { for (i in 0 until arr.length()) add(arr.getString(i)) })
                }
            }
        } catch (e: Exception) {
            // 壊れたデータが入っていても落とさず空扱いにする
            emptyMap()
        }
    }

    /** グループ内の並び順を丸ごと保存する。 */
    fun setGroupOrder(order: Map<String, List<String>>) {
        val obj = JSONObject()
        for ((category, packages) in order) {
            val arr = JSONArray()
            packages.forEach { arr.put(it) }
            obj.put(category, arr)
        }
        prefs.edit()
            .putString(KEY_GROUP_ORDER, obj.toString())
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "launcher_prefs"
        private const val KEY_ALLOWED = "allowed_packages"
        private const val KEY_CATEGORIES = "app_categories"
        private const val KEY_DOCK = "dock_packages"
        private const val KEY_ORDER = "home_order"
        private const val KEY_FRICTION = "friction_packages"
        private const val KEY_REASON_LOG = "reason_log"
        private const val KEY_GROUP_ORDER = "group_item_order"
    }
}
