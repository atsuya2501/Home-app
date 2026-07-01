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

/**
 * ホーム画面と設定画面の切り替えを管理する。
 * 小規模なので Navigation ライブラリは使わず、表示フラグで切り替える。
 */
@Composable
private fun LauncherApp(
    viewModel: LauncherViewModel = viewModel(),
) {
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        // 設定画面では端末の「戻る」でホームへ戻す
        BackHandler { showSettings = false }
        SettingsScreen(
            viewModel = viewModel,
            onBack = { showSettings = false },
        )
    } else {
        HomeScreen(
            viewModel = viewModel,
            onOpenSettings = { showSettings = true },
        )
    }
}
