package com.strangeparticle.springboard.app.domain.model

/**
 * One slot in the grid's left-to-right column rendering. Either a real app column
 * or a blank separator column inserted between adjacent app groups (and between
 * the last group and any ungrouped apps). Separator slots are not interactive —
 * they exist purely for visual spacing.
 */
sealed class AppColumnSlot
data class AppColumn(val app: App) : AppColumnSlot()
data object SeparatorColumn : AppColumnSlot()

/**
 * Visual order of grid columns. When `appGroups` is empty, this is just one
 * AppColumn per app in declaration order — fully backwards-compatible with
 * springboards that don't use grouping. When groups are declared, apps that
 * belong to the same group are emitted adjacent to one another (in their
 * springboard-declaration order), groups are emitted in their declaration order,
 * and a SeparatorColumn is placed between adjacent non-empty groups. Apps with
 * no `appGroupId` are emitted last (after one trailing SeparatorColumn) in their
 * declaration order. Declared groups with no apps are silently skipped — they do
 * not produce orphan separators.
 */
fun Springboard.appColumnLayout(): List<AppColumnSlot> {
    if (appGroups.isEmpty()) return apps.map { AppColumn(it) }

    val groupedBlocks = appGroups
        .map { group -> apps.filter { it.appGroupId == group.id } }
        .filter { it.isNotEmpty() }

    val ungroupedApps = apps.filter { it.appGroupId == null }

    return buildList {
        groupedBlocks.forEachIndexed { index, groupApps ->
            if (index > 0) add(SeparatorColumn)
            groupApps.forEach { add(AppColumn(it)) }
        }
        if (ungroupedApps.isNotEmpty()) {
            if (groupedBlocks.isNotEmpty()) add(SeparatorColumn)
            ungroupedApps.forEach { add(AppColumn(it)) }
        }
    }
}
