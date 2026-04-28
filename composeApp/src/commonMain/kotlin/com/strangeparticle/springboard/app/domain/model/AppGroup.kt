package com.strangeparticle.springboard.app.domain.model

/**
 * Logical grouping of apps for visual organization in the grid. Groups have an id
 * and a description; the description is currently unused in the UI and exists as
 * documentation for the springboard author.
 *
 * The grid renders columns for apps in a group adjacent to one another, in the order
 * the group was declared in the springboard. A blank separator column is rendered
 * between adjacent non-empty groups (and between the last group and any ungrouped
 * apps), giving a visual break between groups without showing the group name.
 */
data class AppGroup(val id: String, val description: String)
