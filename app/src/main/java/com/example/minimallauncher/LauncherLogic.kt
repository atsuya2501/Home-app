package com.example.minimallauncher

import com.example.minimallauncher.data.ReasonLogEntry

/** 起動前に必要な摩擦の種類。 */
data class LaunchGate(
    val requireReason: Boolean,
    val delaySeconds: Int,
) {
    val isRequired: Boolean get() = requireReason || delaySeconds > 0
}

fun launchGateFor(
    packageName: String,
    frictionPackages: Set<String>,
    delayPackages: Set<String>,
    delaySeconds: Int,
): LaunchGate = LaunchGate(
    requireReason = packageName in frictionPackages,
    delaySeconds = if (packageName in delayPackages) delaySeconds else 0,
)

/** fromからtoへ1項目移動する。範囲外なら元のリストを返す。 */
fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices || from == to) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}

/** 名前変更・解散されたフォルダキーを置換し、重複を取り除く。 */
fun migrateFolderKey(keys: List<String>, oldKey: String, newKey: String?): List<String> =
    keys.mapNotNull { key -> if (key == oldKey) newKey else key }.distinct()

/** 保持期間より古いログを削除し、新しい順・最大件数に正規化する。 */
fun pruneReasonLog(
    entries: List<ReasonLogEntry>,
    nowMillis: Long,
    retentionMillis: Long,
    maxEntries: Int,
): List<ReasonLogEntry> {
    val oldestAllowed = nowMillis - retentionMillis
    return entries
        .asSequence()
        .filter { it.timestamp >= oldestAllowed }
        .sortedByDescending { it.timestamp }
        .take(maxEntries)
        .toList()
}
