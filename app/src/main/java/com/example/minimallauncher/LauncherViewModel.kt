package com.example.minimallauncher

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minimallauncher.data.AllowListStore
import com.example.minimallauncher.data.AppInfo
import com.example.minimallauncher.data.AppRepository
import com.example.minimallauncher.data.HomeItem
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

    /** ホーム項目の並び順（HomeItem.key の並び）。ユーザーがドラッグで決める。 */
    var orderedKeys by mutableStateOf<List<String>>(emptyList())
        private set

    /** まだアプリ一覧の読み込みが終わっていないか（初回ロード中の表示用）。 */
    var isLoading by mutableStateOf(true)
        private set

    init {
        // 保存済みの許可リスト・グループ・ドック・並び順は即座に読める
        allowedPackages = allowListStore.getAllowed()
        categories = allowListStore.getCategories()
        dockPackages = allowListStore.getDock()
        orderedKeys = allowListStore.getOrder()
        loadApps()
    }

    /** 端末のアプリ一覧を（重い処理なので）バックグラウンドで読み込む。 */
    fun loadApps() {
        viewModelScope.launch {
            isLoading = true
            val apps = withContext(Dispatchers.IO) { repository.loadInstalledApps() }
            allInstalledApps = apps
            isLoading = false
        }
    }

    /** 許可済みアプリすべて（アプリ名順）。 */
    val allowedApps: List<AppInfo>
        get() = allInstalledApps.filter { it.packageName in allowedPackages }

    /** 下段ドックに固定されたアプリ（アプリ名順）。 */
    val dockApps: List<AppInfo>
        get() = allowedApps.filter { it.packageName in dockPackages }

    /** グリッド（フォルダ含む）に並べる、ドック以外の許可済みアプリ。 */
    private val gridApps: List<AppInfo>
        get() = allowedApps.filter { it.packageName !in dockPackages }

    /**
     * ホーム画面用に、（ドック以外の）許可済みアプリをグループごとにまとめたもの。
     * 並び順は「名前付きグループ（名前順）」→「その他」を最後、の順。
     */
    val groupedAllowedApps: List<Pair<String, List<AppInfo>>>
        get() {
            val groups = gridApps.groupBy { categories[it.packageName] ?: UNCATEGORIZED }
            val namedInOrder = groups.keys
                .filter { it != UNCATEGORIZED }
                .sorted()
            return buildList {
                for (name in namedInOrder) {
                    add(name to groups.getValue(name))
                }
                groups[UNCATEGORIZED]?.let { add(UNCATEGORIZED to it) }
            }
        }

    /**
     * ホーム画面に並べる項目（フォルダ＋個別アプリ）を、保存済みの並び順で返す。
     * 並び順に無い項目（新しく追加されたアプリ等）は自然順のまま末尾に付く。
     */
    val homeItems: List<HomeItem>
        get() {
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
            return natural.sortedBy { orderIndex[it.key] ?: Int.MAX_VALUE }
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
     * 並び順は「名前付きグループ（名前順）」→「その他アプリ」。
     */
    val dockItems: List<HomeItem>
        get() {
            val groups = dockApps.groupBy { categories[it.packageName] ?: UNCATEGORIZED }
            val named = groups.keys.filter { it != UNCATEGORIZED }.sorted()
            return buildList {
                for (name in named) add(HomeItem.Folder(name, groups.getValue(name)))
                groups[UNCATEGORIZED]?.forEach { add(HomeItem.AppItem(it)) }
            }
        }

    /** 既存のグループ名一覧（グループ選択ダイアログの選択肢用）。 */
    val existingCategories: List<String>
        get() = categories.values.filter { it != UNCATEGORIZED }.toSortedSet().toList()

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
    }

    /** アプリを起動する。 */
    fun launchApp(packageName: String) {
        repository.launchApp(packageName)
    }

    companion object {
        /** グループ未設定のアプリをまとめる既定グループ名。 */
        const val UNCATEGORIZED = "その他"
    }
}
