plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Kotlin 2.x では Compose 用のコンパイラプラグインを別途有効化する必要がある
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.minimallauncher"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.minimallauncher"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            // 個人利用なので難読化・最適化は無効のままにしておく
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        // Jetpack Compose を有効化
        compose = true
    }
}

dependencies {
    // Compose BOM: Compose関連ライブラリのバージョンをまとめて管理する仕組み
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose 本体（バージョンは BOM が決めるので記載しない）
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // ホーム画面のドラッグ＆ドロップ並べ替え用（LazyVerticalGrid 対応）
    implementation("sh.calvin.reorderable:reorderable:3.1.0")

    testImplementation("junit:junit:4.13.2")
    // Android標準のorg.jsonと同じAPIをローカル単体テストでも動かすためのテスト専用実装
    testImplementation("org.json:json:20240303")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
