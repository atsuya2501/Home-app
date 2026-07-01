package com.example.minimallauncher.data

/**
 * 起動理由ゲートで記録した1件のログ。
 *
 * @param packageName 起動したアプリのパッケージ名
 * @param label       記録時点でのアプリ表示名（アンインストール後も表示できるよう保持）
 * @param reason      ユーザーが入力した起動理由
 * @param timestamp   記録時刻（epoch ミリ秒）
 */
data class ReasonLogEntry(
    val packageName: String,
    val label: String,
    val reason: String,
    val timestamp: Long,
)
