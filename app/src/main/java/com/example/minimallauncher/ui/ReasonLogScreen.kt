package com.example.minimallauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.minimallauncher.LauncherViewModel
import com.example.minimallauncher.data.ReasonLogEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 起動理由ゲートで記録したログを日別にまとめて表示する画面。
 *
 * リスト最上部に直近7日のアプリ別回数サマリーを添え、以降は新しい日から順に
 * 日付ヘッダー付きでエントリを並べる。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReasonLogScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) { viewModel.removeExpiredReasonLogs() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("起動理由の履歴") },
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
        val entries = viewModel.reasonLog
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Text(
                    text = "まだ記録がありません",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        } else {
            // 再コンポーズごとの再集計を避けるため、サマリーと日別グループはログが
            // 変わったときだけ計算し直してキャッシュする。
            val summary = remember(entries) { buildRecentSummary(entries) }
            val groups = remember(entries) { buildDayGroups(entries) }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                // 直近7日のアプリ別回数サマリー（対象期間に記録があるときだけ表示）。
                if (summary.isNotEmpty()) {
                    item(key = "summary") {
                        RecentSummary(summary)
                        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                    }
                }

                // 新しい日から順に、日付ヘッダー＋その日のエントリを並べる。
                groups.forEach { group ->
                    item(key = "header_${group.date}") {
                        DayHeader(group)
                    }
                    items(
                        items = group.entries,
                        key = { "${it.timestamp}_${it.packageName}" },
                    ) { entry ->
                        ReasonLogRow(entry)
                        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                    }
                }
            }
        }
    }
}

/** 直近7日のアプリ別回数サマリー（小見出し＋上位アプリの一行表記）。 */
@Composable
private fun RecentSummary(summary: List<AppCount>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "直近7日の記録",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = summary.joinToString(" / ") { "${it.label} ${it.count}回" },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** 日付ヘッダー（「今日・3件」のように日付表記とその日の件数を並べる）。 */
@Composable
private fun DayHeader(group: DayGroup) {
    Text(
        text = "${group.title}・${group.entries.size}件",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** ログ1件分の行（アプリ名・理由・時刻）。 */
@Composable
private fun ReasonLogRow(entry: ReasonLogEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = entry.label,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = entry.reason,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = formatTimestamp(entry.timestamp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

/** アプリ別の集計結果（サマリー表示用）。 */
private data class AppCount(
    val label: String,
    val count: Int,
)

/** 1日分のエントリ群と、その日の見出しラベル。 */
private data class DayGroup(
    val date: LocalDate,
    val title: String,
    val entries: List<ReasonLogEntry>,
)

/**
 * 「今日を含む直近7日間」のエントリをアプリ（label）別に集計し、多い順に上位5件を返す。
 * 対象期間に記録が無ければ空リストを返す。
 */
private fun buildRecentSummary(entries: List<ReasonLogEntry>): List<AppCount> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    // 今日を含む直近7日間なので、6日前の 00:00 以降が対象。
    val since = today.minusDays(6)
    return entries
        .filter { !entryDate(it, zone).isBefore(since) }
        .groupingBy { it.label }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(5)
        .map { AppCount(it.key, it.value) }
}

/**
 * エントリを端末ローカルタイムゾーンの日付でグループ化し、新しい日から順に返す。
 * 入力（reasonLog）は新しい順なので、日付キーの初出順＝新しい日順になる。
 */
private fun buildDayGroups(entries: List<ReasonLogEntry>): List<DayGroup> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    return entries
        .groupBy { entryDate(it, zone) }
        .map { (date, dayEntries) -> DayGroup(date, formatDayHeader(date, today), dayEntries) }
}

/** エントリの timestamp を端末ローカルタイムゾーンの LocalDate に変換する。 */
private fun entryDate(entry: ReasonLogEntry, zone: ZoneId): LocalDate =
    Instant.ofEpochMilli(entry.timestamp).atZone(zone).toLocalDate()

/**
 * 日付ヘッダーの表記を作る。今日／昨日は専用ラベル、それ以外は「7月10日（金）」の形式。
 * 今年でなければ「2025年12月31日（水）」のように年も付ける。
 */
private fun formatDayHeader(date: LocalDate, today: LocalDate): String =
    when (date) {
        today -> "今日"
        today.minusDays(1) -> "昨日"
        else -> {
            val pattern = if (date.year == today.year) "M月d日（E）" else "yyyy年M月d日（E）"
            date.format(DateTimeFormatter.ofPattern(pattern, Locale.JAPANESE))
        }
    }

/** epoch ミリ秒を「HH:mm」形式の端末ローカル時刻表記に変換する。 */
private fun formatTimestamp(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val zoned = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
    return formatter.format(zoned)
}
