package com.example.minimallauncher.ui

import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import com.example.minimallauncher.data.HomeLabelMode

val LocalHomeLabelColor = compositionLocalOf { Color.White }

/** 壁紙が暗ければ白、明るければ黒を選び、手動設定があればそちらを優先する。 */
@Composable
fun rememberHomeLabelColor(mode: HomeLabelMode): Color {
    val context = LocalContext.current
    val manager = remember(context) { WallpaperManager.getInstance(context) }

    fun supportsDarkText(): Boolean = runCatching {
        val primary = manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.primaryColor
            ?: return@runCatching false
        ColorUtils.calculateLuminance(primary.toArgb()) >= 0.5
    }.getOrDefault(false)

    var darkTextSupported by remember(manager) {
        mutableStateOf(
            supportsDarkText()
        )
    }

    DisposableEffect(manager) {
        val listener = WallpaperManager.OnColorsChangedListener { _, which ->
            if (which and WallpaperManager.FLAG_SYSTEM != 0) {
                darkTextSupported = supportsDarkText()
            }
        }
        manager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
        onDispose { manager.removeOnColorsChangedListener(listener) }
    }

    return when (mode) {
        HomeLabelMode.AUTO -> if (darkTextSupported) Color(0xFF1C1C1C) else Color.White
        HomeLabelMode.LIGHT -> Color.White
        HomeLabelMode.DARK -> Color(0xFF1C1C1C)
    }
}

/** ホーム上部の時刻・電波・電池表示を、アプリ名と同じ白黒へ切り替える。 */
@Composable
fun ApplyHomeStatusBarStyle(color: Color) {
    val activity = LocalContext.current.findActivity() ?: return
    val view = LocalView.current
    val useDarkIcons = color.luminance() < 0.5f

    DisposableEffect(activity, view, useDarkIcons) {
        WindowCompat.getInsetsController(activity.window, view)
            .isAppearanceLightStatusBars = useDarkIcons

        onDispose {
            // 設定画面は暗い背景なので、ホームを離れたら白い表示へ戻す。
            WindowCompat.getInsetsController(activity.window, view)
                .isAppearanceLightStatusBars = false
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
