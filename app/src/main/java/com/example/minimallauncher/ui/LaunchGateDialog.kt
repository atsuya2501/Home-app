package com.example.minimallauncher.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.example.minimallauncher.data.AppInfo
import com.example.minimallauncher.data.AppRepository
import kotlinx.coroutines.delay

/** 理由入力と待機時間を組み合わせて扱う起動前ゲート。 */
@Composable
fun ReasonGateDialog(
    app: AppInfo,
    requireReason: Boolean,
    delaySeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var reason by remember(app.packageName) { mutableStateOf("") }
    var remainingSeconds by remember(app.packageName, delaySeconds) {
        mutableIntStateOf(delaySeconds)
    }
    val focusRequester = remember(app.packageName) { FocusRequester() }

    LaunchedEffect(app.packageName, delaySeconds) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds -= 1
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (requireReason) "「${app.label}」を開く理由"
                else "「${app.label}」を開きますか？"
            )
        },
        text = {
            Column {
                if (requireReason) {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("理由") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    )
                    LaunchedEffect(app.packageName) { focusRequester.requestFocus() }
                } else {
                    Text(
                        text = "衝動的に開いていませんか？少し待ってから開けます。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (app.packageName == AppRepository.YOUTUBE_PACKAGE && requireReason) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "入力した理由でYouTube内を検索し、検索結果から開きます（トップのおすすめ・ショートを飛ばします）",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = remainingSeconds == 0 && (!requireReason || reason.isNotBlank()),
                onClick = { onConfirm(reason) },
            ) {
                Text(if (remainingSeconds > 0) "開く（あと $remainingSeconds 秒）" else "開く")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
    )
}
