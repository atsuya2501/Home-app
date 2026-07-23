package com.example.minimallauncher

import android.app.Application
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minimallauncher.data.AllowListStore
import com.example.minimallauncher.data.AppInfo
import com.example.minimallauncher.data.AppRepository
import com.example.minimallauncher.data.HomeItem
import com.example.minimallauncher.data.ReasonLogEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ホーム画面と設定画面で共有する状態とロジックを持つ ViewModel。
 *
 * - allInstalledApps : 端末の全アプリ（設定画面の一覧用）
 * - allowedPackages  : 許可したアプリのパッケージ名（ホーム画面に表示する対象）
 * - categories       : パッケージ名 → グループ名（未設定は「その他」扱い）
 *
 * 状態は Compose の State（mutableStateOf）で持つので、変更すると画面が自動で再描画される。
 */
class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val allowListStore = AllowListStore(application)

    var allInstalledApps by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    var allowedPackages by mutableStateOf<Set<String>>(emptySet())
        private set

    var categories by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    /** 下段ドックに固定するアプリのパッケージ名。 */
    var dockPackages by mutableStateOf<Set<String>>(emptySet())
        private set

    /** 下段ドック項目の並び順（HomeItem.key の並び）。 */
    var dockOrderedKeys by mutableStateOf<List<String>>(emptyList())
        private set

    /** ホーム項目の並び順（HomeItem.key の並び）。ユーザーがドラッグで決める。 */
    var orderedKeys by mutableStateOf<List<String>>(emptyList())
        private set

    /** グループ内でのアプリの並び順（グループ名 → パッケージ名の並び）。 */
    var groupItemOrder by mutableStateOf<Map<String, List<String>>>(emptyMap())
        private set

    /** 起動理由入力を必須にするアプリのパッケージ名。 */
    var frictionPackages by mutableStateOf<Set<String>>(emptySet())
        private set

    /** 起動待機（数秒のクールダウン）を課すアプリのパッケージ名。 */
    var delayPackages by mutableStateOf<Set<String>>(emptySet())
        private set

    /** 起動理由ログ（新しい順）。 */
    var reasonLog by mutableStateOf<List<ReasonLogEntry>>(emptyList())
        private set

    /** 起動理由ゲートダイアログの表示対象。null のときは非表示。 */
    var pendingLaunch by mutableStateOf<AppInfo?>(null)
        private set

    /** まだアプリ一覧の読み込みが終わっていないか（初回ロード中の表示用）。 */
    var isLoading by mutableStateOf(true)
        private set

    init {
        // 保存済みの許可リスト・グループ・ドック・並び順・摩擦設定・待機設定・ログは即座に読める
        allowedPackages = allowListStore.getAllowed()
        categories = allowListStore.getCategories()
        dockPackages = allowListStore.getDock()
        dockOrderedKeys = allowListStore.getDockOrder()
        orderedKeys = allowListStore.getOrder()
        groupItemOrder = allowListStore.getGroupOrder()
        frictionPackages = allowListStore.getFriction()
        delayPackages = allowListStore.getDelay()
        reasonLog = allowListStore.getReasonLog().sortedByDescending { it.timestamp }
        loadApps()
    }

    /** アプリ一覧の再読み込みが実行中か（同時に走らせないためのガード）。 */
    private var isReloading = false

    /**
     * 端末のアプリ一覧を（重い処理なので）バックグラウンドで読み込む。
     *
     * @param showLoading 初回ロードのようにスピナーを見せたいときは true。
     *   画面復帰時の静かなリフレッシュでは false にして、設定画面のスピナー点滅を防ぐ。
     */
    fun loadApps(showLoading: Boolean = true) {
        if (isReloading) return
        isReloading = true
        viewModelScope.launch {
            if (showLoading) isLoading = true
            try {
                val apps = withContext(Dispatchers.IO) { repository.loadInstalledApps() }
                allInstalledApps = apps
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // 一時的なPackageManagerの失敗では既存一覧を残し、次回onResumeで再試行する。
            } finally {
                isLoading = false
                isReloading = false
            }
        }
    }

    // 以下の一覧は元の状態（allInstalledApps 等）から導出される。
    // 再コンポーズのたびに毎回計算し直さないよう derivedStateOf でキャッシュし、
    // 元の状態が変わったときだけ再計算する（ドラッグ中の毎フレーム再計算を防ぐ）。

    /** 許可済みアプリすべて（アプリ名順）。 */
    val allowedApps: List<AppInfo> by derivedStateOf {
        allInstalledApps.filter { it.packageName in allowedPackages }
    }

    /** 下段ドックに固定されたアプリ（アプリ名順）。 */
    val dockApps: List<AppInfo> by derivedStateOf {
        allowedApps.filter { it.packageName in dockPackages }
    }

    /** グリッド（フォルダ含む）に並べる、ドック以外の許可済みアプリ。 */
    private val gridApps: List<AppInfo> by derivedStateOf {
        allowedApps.filter { it.packageName !in dockPackages }
    }

    /**
     * ホーム画面用に、（ドック以外の）許可済みアプリをグループごとにまとめたもの。
     * 並び順は「名前付きグループ（名前順）」→「その他」を最後、の順。
     */
    val groupedAllowedApps: List<Pair<String, List<AppInfo>>> by derivedStateOf {
        val groups = gridApps.groupBy { categories[it.packageName] ?: UNCATEGORIZED }
        val namedInOrder = groups.keys
            .filter { it != UNCATEGORIZED }
            .sorted()
        buildList {
            for (name in namedInOrder) {
                add(name to orderWithinGroup(name, groups.getValue(name)))
            }
            groups[UNCATEGORIZED]?.let { add(UNCATEGORIZED to it) }
        }
    }

    /**
     * ホーム画面に並べる項目（フォルダ＋個別アプリ）を、保存済みの並び順で返す。
     * 並び順に無い項目（新しく追加されたアプリ等）は自然順のまま末尾に付く。
     */
    val homeItems: List<HomeItem> by derivedStateOf {
        // 自然順：名前付きグループ（フォルダ）→ その他アプリ（個別）
        val natural = buildList {
            for ((name, apps) in groupedAllowedApps) {
                if (name == UNCATEGORIZED) {
                    apps.forEach { add(HomeItem.AppItem(it)) }
                } else {
                    add(HomeItem.Folder(name, apps))
                }
            }
        }
        // 保存済み並び順を適用（未登録は末尾。sortedBy は安定ソートなので自然順を保つ）
        val orderIndex = orderedKeys.withIndex().associate { (i, key) -> key to i }
        natural.sortedBy { orderIndex[it.key] ?: Int.MAX_VALUE }
    }

    /** ドラッグでの並べ替え結果を反映して保存する。 */
    fun moveHomeItem(fromIndex: Int, toIndex: Int) {
        val keys = homeItems.map { it.key }.toMutableList()
        if (fromIndex !in keys.indices || toIndex !in keys.indices) return
        keys.add(toIndex, keys.removeAt(fromIndex))
        orderedKeys = keys
        allowListStore.setOrder(keys)
    }

    /**
     * 下段ドックに並べる項目。ドック内のアプリも、名前付きグループはフォルダにまとめる。
     * 保存済みの並び順にない項目は、自然順のまま末尾に追加する。
     */
    val dockItems: List<HomeItem> by derivedStateOf {
        val groups = dockApps.groupBy { categories[it.packageName] ?: UNCATEGORIZED }
        val named = groups.keys.filter { it != UNCATEGORIZED }.sorted()
        val natural = buildList {
            for (name in named) add(HomeItem.Folder(name, orderWithinGroup(name, groups.getValue(name))))
            groups[UNCATEGORIZED]?.forEach { add(HomeItem.AppItem(it)) }
        }
        val orderIndex = dockOrderedKeys.withIndex().associate { (i, key) -> key to i }
        natural.sortedBy { orderIndex[it.key] ?: Int.MAX_VALUE }
    }

    /** 下段ドック内のドラッグ並べ替え結果を反映して保存する。 */
    fun moveDockItem(fromIndex: Int, toIndex: Int) {
        val keys = dockItems.map { it.key }.toMutableList()
        if (fromIndex !in keys.indices || toIndex !in keys.indices) return
        keys.add(toIndex, keys.removeAt(fromIndex))
        dockOrderedKeys = keys
        allowListStore.setDockOrder(keys)
    }

    /** 既存のグループ名一覧（グループ選択ダイアログの選択肢用）。 */
    val existingCategories: List<String> by derivedStateOf {
        categories.values.filter { it != UNCATEGORIZED }.toSortedSet().toList()
    }

    /** 保存済みのグループ内並び順を適用する（並び順に無いアプリは自然順のまま末尾）。 */
    private fun orderWithinGroup(category: String, apps: List<AppInfo>): List<AppInfo> {
        val order = groupItemOrder[category] ?: return apps
        val index = order.withIndex().associate { (i, pkg) -> pkg to i }
        return apps.sortedBy { index[it.packageName] ?: Int.MAX_VALUE }
    }

    /** フォルダ内でのドラッグ並べ替え結果を反映して保存する。 */
    fun moveGroupItem(category: String, apps: List<AppInfo>, fromIndex: Int, toIndex: Int) {
        if (fromIndex !in apps.indices || toIndex !in apps.indices) return
        val keys = apps.map { it.packageName }.toMutableList()
        keys.add(toIndex, keys.removeAt(fromIndex))
        val updated = groupItemOrder.toMutableMap()
        updated[category] = keys
        groupItemOrder = updated
        allowListStore.setGroupOrder(updated)
    }

    /** あるアプリの現在のグループ名（未設定なら「その他」）。 */
    fun categoryOf(packageName: String): String =
        categories[packageName] ?: UNCATEGORIZED

    /** あるアプリの許可状態を切り替えて、即座に保存する。外す場合はグループ割り当ても消す。 */
    fun setAllowed(packageName: String, allowed: Boolean) {
        val updated = allowedPackages.toMutableSet().apply {
            if (allowed) add(packageName) else remove(packageName)
        }
        allowedPackages = updated
        allowListStore.setAllowed(updated)

        if (!allowed) {
            setCategory(packageName, UNCATEGORIZED)
            setDock(packageName, false)
        }
    }

    /** あるアプリを下段ドックに固定/解除して保存する。 */
    fun setDock(packageName: String, inDock: Boolean) {
        val updated = dockPackages.toMutableSet().apply {
            if (inDock) add(packageName) else remove(packageName)
        }
        dockPackages = updated
        allowListStore.setDock(updated)
    }

    /** ホーム項目（アプリ/フォルダ）をホームから外す。フォルダは中の全アプリを外す。 */
    fun removeFromHome(item: HomeItem) {
        when (item) {
            is HomeItem.AppItem -> setAllowed(item.app.packageName, false)
            is HomeItem.Folder -> item.apps.forEach { setAllowed(it.packageName, false) }
        }
    }

    /** あるアプリのグループを設定して保存する。「その他」または空なら割り当てを削除。 */
    fun setCategory(packageName: String, category: String) {
        val trimmed = category.trim()
        val updated = categories.toMutableMap().apply {
            if (trimmed.isEmpty() || trimmed == UNCATEGORIZED) {
                remove(packageName)
            } else {
                put(packageName, trimmed)
            }
        }
        categories = updated
        allowListStore.setCategories(updated)
    }

    /**
     * グループ名を変更する。そのグループに属する全アプリをまとめて付け替える。
     * 新しい名前が空 or「その他」なら、そのグループを解散（各アプリを未設定に）する。
     */
    fun renameCategory(oldName: String, newName: String) {
        if (oldName == UNCATEGORIZED) return // 「その他」は固定なので変更しない
        val trimmed = newName.trim()
        val updated = categories.toMutableMap()
        val targetPackages = updated.filterValues { it == oldName }.keys.toList()
        for (pkg in targetPackages) {
            if (trimmed.isEmpty() || trimmed == UNCATEGORIZED) {
                updated.remove(pkg)
            } else {
                updated[pkg] = trimmed
            }
        }
        categories = updated
        allowListStore.setCategories(updated)

        // グループ名はフォルダの一意キーにも使うため、保存済みの各並び順も同時に移行する。
        val oldFolderKey = "folder:$oldName"
        val newFolderKey = trimmed
            .takeIf { it.isNotEmpty() && it != UNCATEGORIZED }
            ?.let { "folder:$it" }

        orderedKeys = migrateFolderKey(orderedKeys, oldFolderKey, newFolderKey)
        allowListStore.setOrder(orderedKeys)
        dockOrderedKeys = migrateFolderKey(dockOrderedKeys, oldFolderKey, newFolderKey)
        allowListStore.setDockOrder(dockOrderedKeys)

        val migratedGroupOrder = groupItemOrder.toMutableMap()
        val oldOrder = migratedGroupOrder.remove(oldName).orEmpty()
        if (newFolderKey != null && oldOrder.isNotEmpty()) {
            migratedGroupOrder[trimmed] =
                (migratedGroupOrder[trimmed].orEmpty() + oldOrder).distinct()
        }
        groupItemOrder = migratedGroupOrder
        allowListStore.setGroupOrder(migratedGroupOrder)
    }

    /** フォルダ名変更・解散時に古いキーを置換し、重複と不要キーを除く。 */
    private fun migrateFolderKey(
        keys: List<String>,
        oldKey: String,
        newKey: String?,
    ): List<String> = keys.mapNotNull { key ->
        if (key == oldKey) newKey else key
    }.distinct()

    /** あるアプリの起動理由入力を必須にするか切り替えて、即座に保存する。 */
    fun setFriction(packageName: String, requireReason: Boolean) {
        val updated = frictionPackages.toMutableSet().apply {
            if (requireReason) add(packageName) else remove(packageName)
        }
        frictionPackages = updated
        allowListStore.setFriction(updated)
    }

    /** あるアプリに起動待機（数秒のクールダウン）を課すか切り替えて、即座に保存する。 */
    fun setDelayGate(packageName: String, enabled: Boolean) {
        val updated = delayPackages.toMutableSet().apply {
            if (enabled) add(packageName) else remove(packageName)
        }
        delayPackages = updated
        allowListStore.setDelay(updated)
    }

    /**
     * ホームからのアプリ起動窓口。理由入力または起動待機の対象アプリはゲートダイアログを挟み、
     * どちらでもないアプリは即座に起動する（遅延ゼロ）。
     */
    fun requestLaunch(packageName: String) {
        if (packageName in frictionPackages || packageName in delayPackages) {
            val app = allInstalledApps.find { it.packageName == packageName }
            if (app != null) pendingLaunch = app else launchApp(packageName)
        } else {
            launchApp(packageName)
        }
    }

    /**
     * ゲートダイアログを確定して実際に起動する。
     * 理由が必要なアプリ（friction）は理由をログに残し、YouTube は検索経由で開く。
     * 待機のみ（friction ではない）のアプリは reason が空で渡ってくるので、
     * 空理由のゴミログを残さないよう、ログには記録せずそのまま起動する。
     */
    fun confirmLaunch(reason: String) {
        val app = pendingLaunch ?: return

        if (app.packageName !in frictionPackages) {
            // 待機のみのアプリ：ログを残さず起動する
            launchApp(app.packageName)
            pendingLaunch = null
            return
        }

        val trimmedReason = reason.trim()
        if (trimmedReason.isEmpty()) return
        // 古いログは切り捨てて、保存・読み込みが際限なく重くならないようにする
        val updated = (listOf(
            ReasonLogEntry(
                packageName = app.packageName,
                label = app.label,
                reason = trimmedReason,
                timestamp = System.currentTimeMillis(),
            )
        ) + reasonLog).take(MAX_REASON_LOG_ENTRIES)
        reasonLog = updated
        allowListStore.setReasonLog(updated)

        // YouTube はホームフィード（Shorts の誘惑）を経由させず、
        // 入力した理由をそのまま検索して結果画面から開始する
        val openedViaSearch = app.packageName == AppRepository.YOUTUBE_PACKAGE &&
            repository.launchYouTubeSearch(trimmedReason)
        if (!openedViaSearch) {
            launchApp(app.packageName)
        }
        pendingLaunch = null
    }

    /** ゲートダイアログをキャンセルする（起動しない）。 */
    fun cancelLaunch() {
        pendingLaunch = null
    }

    /** アプリを起動する。UI からは requestLaunch 経由で呼ぶこと。 */
    fun launchApp(packageName: String) {
        repository.launchApp(packageName)
    }

    companion object {
        /** グループ未設定のアプリをまとめる既定グループ名。 */
        const val UNCATEGORIZED = "その他"

        /** 起動待機ゲートでカウントダウンする秒数。 */
        const val LAUNCH_DELAY_SECONDS = 5

        /** 起動理由ログの保持件数の上限。 */
        private const val MAX_REASON_LOG_ENTRIES = 500
    }
}
