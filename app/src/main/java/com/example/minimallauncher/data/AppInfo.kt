package com.example.minimallauncher.data

import androidx.compose.ui.graphics.ImageBitmap

/**
 * 端末にインストールされた1つのアプリを表すデータ。
 *
 * @param packageName アプリを一意に識別するID（例: com.android.chrome）
 * @param label       画面に表示するアプリ名
 * @param icon        Compose で描画できる形式に変換済みのアイコン画像
 */
data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap
)
