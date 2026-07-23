package com.example.minimallauncher.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri

/**
 * 端末のアプリ情報を扱う窓口。
 * - インストール済みで「起動可能な」アプリ一覧の取得
 * - 指定アプリの起動
 */
class AppRepository(private val context: Context) {

    private val packageManager: PackageManager get() = context.packageManager

    /**
     * ホーム画面から起動できるアプリ（LAUNCHER を持つアプリ）を取得する。
     * アイコンは Compose 用の ImageBitmap に変換しておく。
     * このランチャー自身は一覧から除外する。
     */
    fun loadInstalledApps(): List<AppInfo> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        return packageManager.queryIntentActivities(intent, 0)
            .asSequence()
            .map { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo
                AppInfo(
                    packageName = activityInfo.packageName,
                    label = resolveInfo.loadLabel(packageManager).toString(),
                    // 一部のアイコンは intrinsic サイズが無いため、固定サイズで描画する
                    icon = resolveInfo.loadIcon(packageManager)
                        .toBitmap(width = ICON_SIZE_PX, height = ICON_SIZE_PX)
                        .asImageBitmap()
                )
            }
            .filter { it.packageName != context.packageName } // 自分自身は除外
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    /**
     * 指定パッケージのアプリを起動する。
     * @return 起動できた場合 true。起動用Intentが見つからなければ false。
     */
    fun launchApp(packageName: String): Boolean {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?: return false
        // ランチャーから別アプリを開くので新規タスクとして起動する
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return true
    }

    /**
     * YouTube を「ホームフィードではなく検索結果」で直接開く。
     * トップ画面の Shorts やおすすめ動画を経由させないための入口。
     * @return 開けた場合 true。YouTube 未インストール等で失敗したら false。
     */
    fun launchYouTubeSearch(query: String): Boolean {
        val uri = "https://www.youtube.com/results?search_query=${Uri.encode(query)}".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(YOUTUBE_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    companion object {
        private const val ICON_SIZE_PX = 144

        /** YouTube 公式アプリのパッケージ名。 */
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    }
}
