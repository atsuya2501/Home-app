package com.example.minimallauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * このランチャーは常にダーク基調で使うため、システムのライト/ダーク設定に関わらず
 * ダーク配色で固定する。
 *
 * darkColorScheme() を土台にすることで、ここで明示していない色役割
 * （ダイアログ背景の surfaceContainer 系など）にも暗い既定値が入る。
 * 以前はライトモード時にダイアログ背景が白くなり、明るい文字が見えなくなっていた。
 */
private val DarkColors = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF00344F),
    background = Color(0xFF121212),
    surface = Color(0xFF1A1A1A),
    onBackground = Color(0xFFEDEDED),
    onSurface = Color(0xFFEDEDED),
    onSurfaceVariant = Color(0xFFCFCFCF),
)

/**
 * アプリ全体に色・タイポグラフィを適用するテーマ（常にダーク）。
 */
@Composable
fun MinimalLauncherTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
