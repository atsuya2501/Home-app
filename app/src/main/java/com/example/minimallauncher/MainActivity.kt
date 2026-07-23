package com.example.minimallauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.minimallauncher.ui.HomeScreen
import com.example.minimallauncher.ui.MoreSettingsScreen
import com.example.minimallauncher.ui.ReasonLogScreen
import com.example.minimallauncher.ui.SettingsScreen
import com.example.minimallauncher.ui.theme.MinimalLauncherTheme

/**
 * アプリの入口。ホームアプリとして起動されるとこの画面が表示される。
 */
class MainActivity : ComponentActivity() {
    // Activity スコープの ViewModel。Compose 側の viewModel() と同一インスタンスになるので、
    // onResume からのリフレッシュと画面の表示が同じ状態を共有する。
    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MinimalLauncherTheme {
                LauncherApp(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 他アプリからホームへ戻った時などに、新規インストール/削除を一覧へ反映する。
        // スピナーは出さず静かに差し替える。
        viewModel.loadApps(showLoading = false)
    }
}

/** アプリ内の画面。小規模なので Navigation ライブラリは使わず、この enum で切り替える。 */
private enum class Screen { HOME, APP_SETTINGS, MORE_SETTINGS, HISTORY }

/**
 * ホーム・アプリ選択・その他の設定・履歴の切り替えを管理する。
 * 小規模なので Navigation ライブラリは使わず、表示状態（enum）で切り替える。
 */
@Composable
private fun LauncherApp(
    viewModel: LauncherViewModel = viewModel(),
) {
    var screen by remember { mutableStateOf(Screen.HOME) }

    when (screen) {
        Screen.HOME -> HomeScreen(
            viewModel = viewModel,
            onOpenSettings = { screen = Screen.APP_SETTINGS },
        )
        Screen.APP_SETTINGS -> {
            // アプリ選択画面では端末の「戻る」でホームへ戻す
            BackHandler { screen = Screen.HOME }
            SettingsScreen(
                viewModel = viewModel,
                onBack = { screen = Screen.HOME },
                onOpenMoreSettings = { screen = Screen.MORE_SETTINGS },
            )
        }
        Screen.MORE_SETTINGS -> {
            // その他の設定画面では端末の「戻る」でアプリ選択へ戻す
            BackHandler { screen = Screen.APP_SETTINGS }
            MoreSettingsScreen(
                viewModel = viewModel,
                onBack = { screen = Screen.APP_SETTINGS },
                onOpenHistory = { screen = Screen.HISTORY },
            )
        }
        Screen.HISTORY -> {
            // 履歴画面では端末の「戻る」でその他の設定へ戻す
            BackHandler { screen = Screen.MORE_SETTINGS }
            ReasonLogScreen(
                viewModel = viewModel,
                onBack = { screen = Screen.MORE_SETTINGS },
            )
        }
    }
}
