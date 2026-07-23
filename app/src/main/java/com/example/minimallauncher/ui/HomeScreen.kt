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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.minimallauncher.LauncherViewModel
import com.example.minimallauncher.data.AppInfo
import com.example.minimallauncher.data.AppRepository
import com.example.minimallauncher.data.HomeItem
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import sh.calvin.reorderable.rememberReorderableLazyListState

/** ホーム画面のアプリ名・グループ名の文字色。壁紙が透けて見えるため、明るい壁紙でも読めるよう固定の暗色にしている。 */
private val HomeLabelColor = Color(0xFF1C1C1C)

/**
 * フォルダを開いたときのダイアログの背景色。
 * 中のアプリ名は壁紙対策の暗色（HomeLabelColor）で固定しているため、
 * ダイアログ自体はテーマの暗い surface ではなく明るい色にして文字を見えるようにする。
 */
private val FolderDialogBackground = Color(0xFFF2F2F2)
private val FolderDialogOnBackground = Color(0xFF1C1C1C)

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
    // 開いているフォルダ（グループ名, ドック由来か）。null のときはフォルダを開いていない。
    // 名前だけを保持し、中身は毎回 homeItems/dockItems から引き直すことで、
    // フォルダ内でドラッグ並べ替えした結果を開いたまま即座に反映できるようにする。
    var openFolder by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    // 名前変更中のグループ。null のときはリネームダイアログ非表示。
    var renameTarget by remember { mutableStateOf<String?>(null) }
    // 編集モード。ON中は各アイコンに×が出て、ホームから外せる。
    // アイコンを長押し（＝ドラッグ開始）すると自動でONになり、背景タップでOFFに戻る。
    var editMode by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        // ホーム画面だけは透明にして、端末の壁紙（Activity側で表示設定済み）を透かす。
        // 設定・履歴画面は別途 Scaffold の不透明な背景色を使うので影響しない。
        color = Color.Transparent,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    // 背景の何もない部分：長押しで設定画面へ、タップで編集モードを終了
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { editMode = false },
                            onLongPress = { onOpenSettings() },
                        )
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
                                    // アイコンを長押しするとドラッグ開始（背景長押しは設定のまま）。
                                    // ドラッグが始まったら編集モードに入り、各アイコンに×を出す。
                                    // 終了は背景タップ。
                                    .longPressDraggableHandle(
                                        onDragStarted = { editMode = true },
                                    )
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
                                            onClick = { if (!editMode) openFolder = homeItem.name to false },
                                        )
                                        is HomeItem.AppItem -> AppGridItem(
                                            app = homeItem.app,
                                            onClick = {
                                                if (!editMode) {
                                                    viewModel.requestLaunch(homeItem.app.packageName)
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
                    tint = HomeLabelColor.copy(alpha = 0.7f),
                )
            }

            }

            // 下段ドック（固定アプリがあるときだけ表示）。ドック内もフォルダにまとまる。
            val dockItems = viewModel.dockItems
            if (dockItems.isNotEmpty()) {
                DockBar(
                    items = dockItems,
                    onLaunch = { viewModel.requestLaunch(it) },
                    onOpenFolder = { openFolder = it.name to true },
                    onReorder = viewModel::moveDockItem,
                    onDragStarted = { editMode = true },
                    editMode = editMode,
                    onRemove = { viewModel.removeFromHome(it) },
                )
            }
        }
    }

    // フォルダを開いたときの中身表示（グリッド・ドックどちらのフォルダでも同じ）。
    // homeItems/dockItems から名前で引き直すので、フォルダ内で並べ替えた直後の
    // 順序もそのまま（ダイアログを開いたまま）反映される。
    openFolder?.let { (name, isDock) ->
        val liveApps = (if (isDock) viewModel.dockItems else viewModel.homeItems)
            .filterIsInstance<HomeItem.Folder>()
            .find { it.name == name }
            ?.apps
        if (liveApps != null) {
            FolderDialog(
                name = name,
                apps = liveApps,
                onDismiss = { openFolder = null },
                onLaunch = { packageName ->
                    viewModel.requestLaunch(packageName)
                    openFolder = null
                },
                onRename = { renameTarget = name },
                onReorder = { from, to -> viewModel.moveGroupItem(name, liveApps, from, to) },
            )
        }
        // liveApps が null（最後の1個が外れる等でフォルダ自体が消えた）ときは何も表示しない。
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

    // 起動ゲート（理由入力または起動待機の対象アプリをタップしたときだけ表示）
    viewModel.pendingLaunch?.let { app ->
        ReasonGateDialog(
            app = app,
            // 理由が必要なアプリ（friction）のときだけ理由入力欄を出す
            requireReason = app.packageName in viewModel.frictionPackages,
            // 起動待機（delay）の対象なら規定秒数だけカウントダウンする（対象外は0）
            delaySeconds = if (app.packageName in viewModel.delayPackages) {
                LauncherViewModel.LAUNCH_DELAY_SECONDS
            } else {
                0
            },
            onDismiss = { viewModel.cancelLaunch() },
            onConfirm = { reason -> viewModel.confirmLaunch(reason) },
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
            color = HomeLabelColor,
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
            // 壁紙が透けて見えるようになった分、明るい壁紙でも読めるよう
            // テーマ色（明るいグレー）ではなく固定の暗い色にしている
            color = HomeLabelColor,
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

/**
 * フォルダを開いたときに中身のアプリを一覧表示するダイアログ。
 * ホーム画面のグリッドと同じ要領で長押しドラッグによる並べ替えができる。
 */
@Composable
private fun FolderDialog(
    name: String,
    apps: List<AppInfo>,
    onDismiss: () -> Unit,
    onLaunch: (String) -> Unit,
    onRename: () -> Unit,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = FolderDialogBackground,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .padding(20.dp),
            ) {
                // タイトル行：グループ名 ＋ 名前変更（鉛筆）ボタン
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = name,
                        color = FolderDialogOnBackground,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onRename) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "グループ名を変更",
                            tint = FolderDialogOnBackground.copy(alpha = 0.7f),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                val gridState = rememberLazyGridState()
                val reorderState = rememberReorderableLazyGridState(gridState) { from, to ->
                    onReorder(from.index, to.index)
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    state = gridState,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        ReorderableItem(reorderState, key = app.packageName) { isDragging ->
                            val scale = if (isDragging) 1.1f else 1f
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .longPressDraggableHandle()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                            ) {
                                AppGridItem(
                                    app = app,
                                    onClick = { onLaunch(app.packageName) },
                                )
                            }
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
    onReorder: (Int, Int) -> Unit,
    onDragStarted: () -> Unit,
    editMode: Boolean,
    onRemove: (HomeItem) -> Unit,
) {
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        onReorder(from.index, to.index)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f))
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            lazyItems(items, key = { it.key }) { item ->
                ReorderableItem(reorderState, key = item.key) { isDragging ->
                    val scale = if (isDragging) 1.1f else 1f
                    Box(
                        modifier = Modifier
                            .width(76.dp)
                            .longPressDraggableHandle(onDragStarted = { onDragStarted() })
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            },
                    ) {
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

/**
 * 起動ゲートのダイアログ。二つの摩擦を扱う。
 * - requireReason = true : 理由入力欄を出し、理由を書かないと開けない（従来の理由ゲート）
 * - delaySeconds  > 0    : 表示から delaySeconds 秒はカウントダウンし、その間は開けない（起動待機ゲート）
 * 両方が有効なアプリは、理由入力とカウントダウンの両方が同時に効く。
 *
 * @param requireReason 理由入力を必須にするか
 * @param delaySeconds  「開く」を押せるようになるまでの待機秒数（0 なら待機なし）
 */
@Composable
private fun ReasonGateDialog(
    app: AppInfo,
    requireReason: Boolean,
    delaySeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var reason by remember(app.packageName) { mutableStateOf("") }
    val isYouTube = app.packageName == AppRepository.YOUTUBE_PACKAGE
    // ダイアログ表示と同時にテキスト入力へフォーカスし、キーボードを自動で開く。
    val focusRequester = remember { FocusRequester() }

    // 起動待機の残り秒数。表示時から1秒ごとに減らし、0になったら「開く」を解禁する。
    var remainingSeconds by remember(app.packageName) { mutableIntStateOf(delaySeconds) }
    if (delaySeconds > 0) {
        LaunchedEffect(app.packageName) {
            while (remainingSeconds > 0) {
                kotlinx.coroutines.delay(1000)
                remainingSeconds -= 1
            }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    )
                    // 入力欄があるときだけ自動フォーカスする（欄が無いときは requestFocus しない）
                    LaunchedEffect(Unit) { focusRequester.requestFocus() }
                } else {
                    Text(
                        text = "衝動的に開いていませんか？少し待ってから開けます。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (isYouTube && requireReason) {
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
                // 待機が終わり、かつ（理由不要 または 理由入力済み）のときだけ押せる
                enabled = remainingSeconds == 0 && (!requireReason || reason.isNotBlank()),
                onClick = { onConfirm(reason) },
            ) {
                Text(
                    if (remainingSeconds > 0) "開く（あと $remainingSeconds 秒）"
                    else "開く"
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
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
            color = HomeLabelColor,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "画面を長押し、または右上の設定ボタンから\n表示するアプリを選んでください",
            color = HomeLabelColor.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}
