package com.example.minimallauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.minimallauncher.data.HomeItem
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** 画面下部に固定表示する、横スクロール・ドラッグ並べ替え対応のドック。 */
@Composable
fun DockBar(
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
        HorizontalDivider(color = LocalHomeLabelColor.current.copy(alpha = 0.18f))
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            items(items, key = { it.key }) { item ->
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
}
