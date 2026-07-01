package com.example.minimallauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.minimallauncher.LauncherViewModel
import com.example.minimallauncher.data.AppInfo
import com.example.minimallauncher.data.HomeItem
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

/**
 * ホーム画面。許可されたアプリを表示する。
 * - 名前付きグループ（例:「買い物」）は、アイコン1個分の「フォルダ」にまとめる（タップで開く）
 * - グループ未設定（その他）のアプリは、個別アイコンで直接並べる
 *
 * @param onOpenSettings 設定画面を開くときに呼ぶ
 */
@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    onOpenSettings: () -> Unit,
) {
    val apps = viewModel.allowedApps
    // 開いているフォルダ。null のときはフォルダを開いていない。
    var openFolder by remember { mutableStateOf<HomeItem.Folder?>(null) }
    // 名前変更中のグループ。null のときはリネームダイアログ非表示。
    var renameTarget by remember { mutableStateOf<String?>(null) }
    // 編集モード。ON中は各アイコンに×が出て、ホームから外せる。
    var editMode by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    // 背景の何もない部分を長押しすると設定画面へ
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = { onOpenSettings() })
                    }
            ) {
            if (apps.isEmpty()) {
                EmptyHome(
                    modifier = Modifier.align(Alignment.Center),
                    onOpenSettings = onOpenSettings,
                )
            } else {
                val gridState = rememberLazyGridState()
                // ドラッグ並べ替えの状態。onMove で並び順を保存する。
                val reorderState = rememberReorderableLazyGridState(gridState) { from, to ->
                    viewModel.moveHomeItem(from.index, to.index)
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp, end = 12.dp, top = 72.dp, bottom = 48.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    items(viewModel.homeItems, key = { it.key }) { homeItem ->
                        ReorderableItem(reorderState, key = homeItem.key) { isDragging ->
                            // ドラッグ中は少し拡大して「持ち上げた」感を出す
                            val scale = if (isDragging) 1.1f else 1f
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // アイコンを長押しするとドラッグ開始（背景長押しは設定のまま）
                                    .longPressDraggableHandle()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                            ) {
                                EditableItem(
                                    editMode = editMode,
                                    onRemove = { viewModel.removeFromHome(homeItem) },
                                ) {
                                    when (homeItem) {
                                        is HomeItem.Folder -> FolderTile(
                                            name = homeItem.name,
                                            apps = homeItem.apps,
                                            onClick = { if (!editMode) openFolder = homeItem },
                                        )
                                        is HomeItem.AppItem -> AppGridItem(
                                            app = homeItem.app,
                                            onClick = {
                                                if (!editMode) {
                                                    viewModel.launchApp(homeItem.app.packageName)
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 右上の小さな設定ボタン（長押しに気づかない場合の保険）。
            // システムのステータスバーと重ならないよう statusBarsPadding を入れ、
            // 画面下の戻る/ホームボタンとは干渉しない上部に配置する。
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "設定",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
            }

            // 編集モードの切り替えボタン（左上）。ON中は各アイコンに×が出る。
            if (apps.isNotEmpty()) {
                IconButton(
                    onClick = { editMode = !editMode },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(8.dp),
                ) {
                    Icon(
                        imageVector = if (editMode) Icons.Filled.Done else Icons.Filled.Edit,
                        contentDescription = if (editMode) "編集を終了" else "編集",
                        tint = MaterialTheme.colorScheme.onBackground
                            .copy(alpha = if (editMode) 1f else 0.6f),
                    )
                }
            }
            }

            // 下段ドック（固定アプリがあるときだけ表示）。ドック内もフォルダにまとまる。
            val dockItems = viewModel.dockItems
            if (dockItems.isNotEmpty()) {
                DockBar(
                    items = dockItems,
                    onLaunch = { viewModel.launchApp(it) },
                    onOpenFolder = { openFolder = it },
                    editMode = editMode,
                    onRemove = { viewModel.removeFromHome(it) },
                )
            }
        }
    }

    // フォルダを開いたときの中身表示（グリッド・ドックどちらのフォルダでも同じ）
    openFolder?.let { folder ->
        FolderDialog(
            name = folder.name,
            apps = folder.apps,
            onDismiss = { openFolder = null },
            onLaunch = { packageName ->
                viewModel.launchApp(packageName)
                openFolder = null
            },
            onRename = { renameTarget = folder.name },
        )
    }

    // フォルダ名の変更ダイアログ
    renameTarget?.let { oldName ->
        RenameDialog(
            currentName = oldName,
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                viewModel.renameCategory(oldName, newName)
                renameTarget = null
                // リネーム後はいったんフォルダを閉じる（見たいときは再タップ）
                openFolder = null
            },
        )
    }
}

/** 1アプリ分のアイコン＋名前。タップで起動。 */
@Composable
private fun AppGridItem(
    app: AppInfo,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = app.label,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = app.label,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * グループをまとめた「フォルダ」タイル。アプリ1個分の大きさ。
 * アイコン部分には中身のアプリを最大4つ、2x2 でプレビュー表示する。
 */
@Composable
private fun FolderTile(
    name: String,
    apps: List<AppInfo>,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f))
                .padding(7.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    MiniIcon(apps.getOrNull(0))
                    MiniIcon(apps.getOrNull(1))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    MiniIcon(apps.getOrNull(2))
                    MiniIcon(apps.getOrNull(3))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = name,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** フォルダのプレビュー用の小さいアイコン（中身が足りない枠は空白）。 */
@Composable
private fun MiniIcon(app: AppInfo?) {
    if (app != null) {
        Image(
            bitmap = app.icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
    } else {
        Spacer(Modifier.size(18.dp))
    }
}

/** フォルダを開いたときに中身のアプリを一覧表示するダイアログ。 */
@Composable
private fun FolderDialog(
    name: String,
    apps: List<AppInfo>,
    onDismiss: () -> Unit,
    onLaunch: (String) -> Unit,
    onRename: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                // タイトル行：グループ名 ＋ 名前変更（鉛筆）ボタン
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = name,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onRename) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "グループ名を変更",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                // 4列ずつの行に分けて並べる（フォルダ内は数が少ない想定）
                apps.chunked(4).forEach { rowApps ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowApps.forEach { app ->
                            Box(modifier = Modifier.weight(1f)) {
                                AppGridItem(
                                    app = app,
                                    onClick = { onLaunch(app.packageName) },
                                )
                            }
                        }
                        // 端数の空きセルを詰めて左寄せの見た目を保つ
                        repeat(4 - rowApps.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/** 画面下部に固定表示するドック。指定したアプリを常に最下段に並べる。 */
@Composable
private fun DockBar(
    items: List<HomeItem>,
    onLaunch: (String) -> Unit,
    onOpenFolder: (HomeItem.Folder) -> Unit,
    editMode: Boolean,
    onRemove: (HomeItem) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            items.forEach { item ->
                Box(modifier = Modifier.width(76.dp)) {
                    EditableItem(
                        editMode = editMode,
                        onRemove = { onRemove(item) },
                    ) {
                        when (item) {
                            is HomeItem.Folder -> FolderTile(
                                name = item.name,
                                apps = item.apps,
                                onClick = { if (!editMode) onOpenFolder(item) },
                            )
                            is HomeItem.AppItem -> AppGridItem(
                                app = item.app,
                                onClick = { if (!editMode) onLaunch(item.app.packageName) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 編集モード中は左上に「×」バッジを重ねて表示し、タップでホームから外せるようにするラッパー。
 * 通常時は中身をそのまま表示するだけ。
 */
@Composable
private fun EditableItem(
    editMode: Boolean,
    onRemove: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box {
        content()
        if (editMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "ホームから外す",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/** フォルダ（グループ）名を変更するダイアログ。 */
@Composable
private fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember(currentName) { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("グループ名を変更") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("グループ名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = { onConfirm(text) },
            ) { Text("変更") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}

/** 許可アプリが1つも無いときの案内表示。 */
@Composable
private fun EmptyHome(
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onOpenSettings)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "表示するアプリがありません",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "画面を長押し、または右上の設定ボタンから\n表示するアプリを選んでください",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}
