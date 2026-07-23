package com.example.minimallauncher.data

/** JSONバックアップに含めるランチャー設定一式。 */
data class LauncherBackup(
    val allowedPackages: Set<String>,
    val categories: Map<String, String>,
    val dockPackages: Set<String>,
    val dockOrder: List<String>,
    val homeOrder: List<String>,
    val frictionPackages: Set<String>,
    val delayPackages: Set<String>,
    val reasonLog: List<ReasonLogEntry>,
    val groupItemOrder: Map<String, List<String>>,
    val homeLabelMode: HomeLabelMode,
)
