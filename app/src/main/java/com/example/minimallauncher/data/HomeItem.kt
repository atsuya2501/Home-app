package com.example.minimallauncher.data

/**
 * ホーム画面に並ぶ1マス分の項目。フォルダ（グループ）か、個別アプリのどちらか。
 * key はドラッグ並べ替えと保存に使う一意なID。
 */
sealed class HomeItem(val key: String) {
    /** 名前付きグループ。中身のアプリを持つ。 */
    class Folder(val name: String, val apps: List<AppInfo>) : HomeItem("folder:$name")

    /** グループ未設定の個別アプリ。 */
    class AppItem(val app: AppInfo) : HomeItem("app:${app.packageName}")
}
