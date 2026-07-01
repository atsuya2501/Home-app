// トップレベルのビルド設定。各プラグインのバージョンをここで一括指定し、
// 実際の適用（apply）は app モジュール側で行う。
plugins {
    id("com.android.application") version "8.9.1" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
}
