package com.example.minimallauncher.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.minimallauncher.LauncherViewModel
import com.example.minimallauncher.data.HomeItem
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

/** 許可アプリ・フォルダ・下段ドックを表示するホーム画面。 */
@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    onOpenSettings: () -> Unit,
) {
    val labelColor = rememberHomeLabelColor(viewModel.homeLabelMode)
    ApplyHomeStatusBarStyle(labelColor)
    var openFolder by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var renameTarget by remember { mutableStateOf<String?>(null) }
    var editMode by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalHomeLabelColor provides labelColor) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "設定",
                            tint = LocalHomeLabelColor.current.copy(alpha = 0.75f),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { editMode = false },
                                onLongPress = { onOpenSettings() },
                            )
                        },
                ) {
                    if (viewModel.allowedApps.isEmpty()) {
                        EmptyHome(
                            modifier = Modifier.align(Alignment.Center),
                            onOpenSettings = onOpenSettings,
                        )
                    } else {
                        HomeGrid(
                            items = viewModel.homeItems,
                            editMode = editMode,
                            onDragStarted = { editMode = true },
                            onReorder = viewModel::moveHomeItem,
                            onRemove = viewModel::removeFromHome,
                            onOpenFolder = { openFolder = it.name to false },
                            onLaunch = viewModel::requestLaunch,
                        )
                    }
                }

                if (viewModel.dockItems.isNotEmpty()) {
                    DockBar(
                        items = viewModel.dockItems,
                        onLaunch = viewModel::requestLaunch,
                        onOpenFolder = { openFolder = it.name to true },
                        onReorder = viewModel::moveDockItem,
                        onDragStarted = { editMode = true },
                        editMode = editMode,
                        onRemove = viewModel::removeFromHome,
                    )
                }
            }
        }

        openFolder?.let { (name, isDock) ->
            val source = if (isDock) viewModel.dockItems else viewModel.homeItems
            val liveApps = source
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
                    onReorder = { from, to ->
                        viewModel.moveGroupItem(name, liveApps, from, to)
                    },
                )
            }
        }

        renameTarget?.let { oldName ->
            RenameDialog(
                currentName = oldName,
                onDismiss = { renameTarget = null },
                onConfirm = { newName ->
                    viewModel.renameCategory(oldName, newName)
                    renameTarget = null
                    openFolder = null
                },
            )
        }

        viewModel.pendingLaunch?.let { app ->
            val gate = viewModel.launchGate(app.packageName)
            ReasonGateDialog(
                app = app,
                requireReason = gate.requireReason,
                delaySeconds = gate.delaySeconds,
                onDismiss = viewModel::cancelLaunch,
                onConfirm = viewModel::confirmLaunch,
            )
        }
    }
}

@Composable
private fun HomeGrid(
    items: List<HomeItem>,
    editMode: Boolean,
    onDragStarted: () -> Unit,
    onReorder: (Int, Int) -> Unit,
    onRemove: (HomeItem) -> Unit,
    onOpenFolder: (HomeItem.Folder) -> Unit,
    onLaunch: (String) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val reorderState = rememberReorderableLazyGridState(gridState) { from, to ->
        onReorder(from.index, to.index)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = 16.dp,
            bottom = 48.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(items, key = { it.key }) { item ->
            ReorderableItem(reorderState, key = item.key) { isDragging ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .longPressDraggableHandle(onDragStarted = { onDragStarted() })
                        .graphicsLayer {
                            val scale = if (isDragging) 1.1f else 1f
                            scaleX = scale
                            scaleY = scale
                        },
                ) {
                    EditableItem(editMode = editMode, onRemove = { onRemove(item) }) {
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
