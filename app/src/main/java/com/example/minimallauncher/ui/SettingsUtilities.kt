package com.example.minimallauncher.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.minimallauncher.LauncherViewModel
import com.example.minimallauncher.data.HomeLabelMode
import java.time.LocalDate

/** 設定バックアップとホーム文字色をまとめた設定画面上部の操作欄。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsUtilities(viewModel: LauncherViewModel) {
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("設定のバックアップ")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    exportLauncher.launch("minimal-launcher-${LocalDate.now()}.json")
                }
            ) {
                Text("保存")
            }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "text/json", "text/plain")) }
            ) {
                Text("復元")
            }
        }

        Text("ホームの文字色")
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
}

private val HomeLabelMode.displayName: String
    get() = when (this) {
        HomeLabelMode.AUTO -> "自動"
        HomeLabelMode.LIGHT -> "白"
        HomeLabelMode.DARK -> "黒"
    }
