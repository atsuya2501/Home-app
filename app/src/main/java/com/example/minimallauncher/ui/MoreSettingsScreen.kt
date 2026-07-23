package com.example.minimallauncher.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.minimallauncher.LauncherViewModel
import com.example.minimallauncher.data.HomeLabelMode
import java.time.LocalDate

/** 普段は触らない表示・バックアップ・履歴をまとめた二階層目の設定画面。 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MoreSettingsScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val succeeded = runCatching {
            val output = context.contentResolver.openOutputStream(uri)
                ?: error("保存先を開けません")
            output.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(viewModel.exportBackup())
            }
        }.isSuccess
        Toast.makeText(
            context,
            if (succeeded) "バックアップを保存しました" else "バックアップの保存に失敗しました",
            Toast.LENGTH_SHORT,
        ).show()
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val succeeded = runCatching {
            val input = context.contentResolver.openInputStream(uri)
                ?: error("バックアップを開けません")
            input.bufferedReader(Charsets.UTF_8).use { reader ->
                viewModel.importBackup(reader.readText())
            }
        }.getOrDefault(false)
        Toast.makeText(
            context,
            if (succeeded) "設定を復元しました" else "このバックアップは読み込めません",
            Toast.LENGTH_SHORT,
        ).show()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("その他の設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsSection(
                title = "ホームの文字と上部表示",
                description = "アプリ名と時刻・電波・電池を、壁紙に合わせるか白または黒に固定します。",
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeLabelMode.entries.forEach { mode ->
                        FilterChip(
                            selected = viewModel.homeLabelMode == mode,
                            onClick = { viewModel.updateHomeLabelMode(mode) },
                            label = { Text(mode.displayName) },
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsSection(
                title = "設定のバックアップ",
                description = "アプリ配置やグループなどをJSONファイルに保存・復元します。",
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            exportLauncher.launch("minimal-launcher-${LocalDate.now()}.json")
                        }
                    ) {
                        Text("書き出す")
                    }
                    OutlinedButton(
                        onClick = {
                            importLauncher.launch(
                                arrayOf("application/json", "text/json", "text/plain")
                            )
                        }
                    ) {
                        Text("読み込む")
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            ListItem(
                headlineContent = { Text("起動理由の履歴") },
                supportingContent = { Text("直近30日分の記録を確認します。") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                    )
                },
                trailingContent = { Text("開く") },
                modifier = Modifier.clickable(onClick = onOpenHistory),
            )
        }
    }
}

/** 見出し・説明・操作を同じ余白で揃える。 */
@Composable
private fun SettingsSection(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        content()
    }
}

private val HomeLabelMode.displayName: String
    get() = when (this) {
        HomeLabelMode.AUTO -> "自動"
        HomeLabelMode.LIGHT -> "白"
        HomeLabelMode.DARK -> "黒"
    }
