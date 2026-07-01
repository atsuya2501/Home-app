package com.example.minimallauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.minimallauncher.LauncherViewModel
import com.example.minimallauncher.data.AppInfo

/**
 * 設定画面。インストール済み全アプリをチェックボックス付きで表示し、
 * 許可リストへの追加/削除、および許可したアプリのグループ割り当てを行う。
 * 削除（チェックを外す）時のみ確認ダイアログを出す。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    // 削除確認の対象アプリ。null のときはダイアログ非表示。
    var pendingRemoval by remember { mutableStateOf<AppInfo?>(null) }
    // グループ編集の対象アプリ。null のときはダイアログ非表示。
    var categoryEditTarget by remember { mutableStateOf<AppInfo?>(null) }
    // アプリ検索のキーワード。
    var query by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("表示するアプリを選択") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = "起動理由の履歴",
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
        ) {
            // 検索バー：入力すると一覧を即フィルタする
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("アプリを検索") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "クリア")
                        }
                    }
                },
                singleLine = true,
            )

            if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            } else {
                val keyword = query.trim()
                val shownApps = if (keyword.isEmpty()) {
                    viewModel.allInstalledApps
                } else {
                    viewModel.allInstalledApps.filter {
                        it.label.contains(keyword, ignoreCase = true) ||
                            it.packageName.contains(keyword, ignoreCase = true)
                    }
                }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(shownApps, key = { it.packageName }) { app ->
                        val checked = app.packageName in viewModel.allowedPackages
                        AppCheckRow(
                            app = app,
                            checked = checked,
                            category = viewModel.categoryOf(app.packageName),
                            inDock = app.packageName in viewModel.dockPackages,
                            requireReason = app.packageName in viewModel.frictionPackages,
                            onToggle = { wantChecked ->
                                if (wantChecked) {
                                    // 追加はそのまま即時反映
                                    viewModel.setAllowed(app.packageName, true)
                                } else {
                                    // 削除は確認ダイアログを挟む
                                    pendingRemoval = app
                                }
                            },
                            onEditCategory = { categoryEditTarget = app },
                            onToggleDock = {
                                val nowInDock = app.packageName in viewModel.dockPackages
                                viewModel.setDock(app.packageName, !nowInDock)
                            },
                            onToggleFriction = {
                                val now = app.packageName in viewModel.frictionPackages
                                viewModel.setFriction(app.packageName, !now)
                            },
                        )
                    }
                }
            }
        }
    }

    // 削除確認ダイアログ
    pendingRemoval?.let { app ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("ホーム画面から外す") },
            text = { Text("「${app.label}」をホーム画面の表示から外しますか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setAllowed(app.packageName, false)
                        pendingRemoval = null
                    }
                ) { Text("外す") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) { Text("キャンセル") }
            },
        )
    }

    // グループ選択ダイアログ
    categoryEditTarget?.let { app ->
        CategoryDialog(
            app = app,
            current = viewModel.categoryOf(app.packageName),
            existing = viewModel.existingCategories,
            onDismiss = { categoryEditTarget = null },
            onSelect = { chosen ->
                viewModel.setCategory(app.packageName, chosen)
                categoryEditTarget = null
            },
        )
    }
}

/**
 * アプリ1件分の行（アイコン・名前・チェックボックス）。行全体タップでも切り替え可能。
 * 許可済みのアプリには、グループ名のチップ（タップで変更）を表示する。
 */
@Composable
private fun AppCheckRow(
    app: AppInfo,
    checked: Boolean,
    category: String,
    inDock: Boolean,
    requireReason: Boolean,
    onToggle: (Boolean) -> Unit,
    onEditCategory: () -> Unit,
    onToggleDock: () -> Unit,
    onToggleFriction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = app.label,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (checked) {
                // 許可済みのときだけ「グループ変更」と「下段に固定」のチップを出す
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "グループ: $category  ›",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.clickable(onClick = onEditCategory),
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = if (inDock) "下段に固定 ●" else "下段に固定 ○",
                        color = if (inDock) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.clickable(onClick = onToggleDock),
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = if (requireReason) "理由が必要 ●" else "理由が必要 ○",
                        color = if (requireReason) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.clickable(onClick = onToggleFriction),
                    )
                }
            } else {
                Text(
                    text = app.packageName,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle(it) },
        )
    }
}

/** グループを選ぶダイアログ。既存グループから選択、または新規作成できる。 */
@Composable
private fun CategoryDialog(
    app: AppInfo,
    current: String,
    existing: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    var newName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("「${app.label}」のグループ") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 「その他」= グループなし
                CategoryOption(
                    label = "その他（グループなし）",
                    selected = current == LauncherViewModel.UNCATEGORIZED,
                    onClick = { onSelect(LauncherViewModel.UNCATEGORIZED) },
                )
                // 既存のグループ
                existing.forEach { cat ->
                    CategoryOption(
                        label = cat,
                        selected = cat == current,
                        onClick = { onSelect(cat) },
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("新しいグループを作成") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = newName.isNotBlank(),
                onClick = { onSelect(newName) },
            ) { Text("作成して割り当て") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("閉じる") }
        },
    )
}

/** グループ選択ダイアログの1項目（ラジオボタン付き）。 */
@Composable
private fun CategoryOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
