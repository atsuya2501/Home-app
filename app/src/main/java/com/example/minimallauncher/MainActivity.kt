package com.example.minimallauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.minimallauncher.ui.HomeScreen
import com.example.minimallauncher.ui.ReasonLogScreen
import com.example.minimallauncher.ui.SettingsScreen
import com.example.minimallauncher.ui.theme.MinimalLauncherTheme

/**
 * アプリの入口。ホームアプリとして起動されるとこの画面が表示される。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MinimalLauncherTheme {
                LauncherApp()
            }
        }
    }
}

/** アプリ内の画面。小規模なので Navigation ライブラリは使わず、この enum で切り替える。 */
private enum class Screen { HOME, SETTINGS, HISTORY }

/**
 * ホーム・設定・履歴の3画面の切り替えを管理する。
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
            onOpenSettings = { screen = Screen.SETTINGS },
        )
        Screen.SETTINGS -> {
            // 設定画面では端末の「戻る」でホームへ戻す
            BackHandler { screen = Screen.HOME }
            SettingsScreen(
                viewModel = viewModel,
                onBack = { screen = Screen.HOME },
                onOpenHistory = { screen = Screen.HISTORY },
            )
        }
        Screen.HISTORY -> {
            // 履歴画面では端末の「戻る」で設定画面へ戻す
            BackHandler { screen = Screen.SETTINGS }
            ReasonLogScreen(
                viewModel = viewModel,
                onBack = { screen = Screen.SETTINGS },
            )
        }
    }
}
