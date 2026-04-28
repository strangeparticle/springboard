package com.strangeparticle.springboard.app.ui.gridnav

import com.strangeparticle.springboard.app.domain.model.ALL_ENVS_DISPLAY_NAME
import com.strangeparticle.springboard.app.domain.model.ALL_ENVS_ENVIRONMENT_ID
import com.strangeparticle.springboard.app.domain.model.Resource
import com.strangeparticle.springboard.app.domain.model.Springboard
import com.strangeparticle.springboard.app.domain.model.allEnvsResources
import com.strangeparticle.springboard.app.domain.model.envSpecificResources
import com.strangeparticle.springboard.app.domain.model.hasAnyAllEnvsActivators

/**
 * Vertical region of the grid. The grid renders one block per section: a heading row
 * followed by the section's resource rows. When more than one section is rendered, a
 * blank spacer row separates them.
 *
 * `sectionId` identifies the section for test tags and is "ALL" for the all-envs section
 * or the configured environment id for the env section. `environmentId` is the id used
 * to look up activators in the springboard index for that section's cells.
 *
 * `isPrimaryHeading` is true for the section whose heading occupies the
 * `gridHeaderHeight` slot at the very top of the grid (alongside the rotated app
 * column headers). When the all-envs section is present it is the primary heading;
 * otherwise the env section is.
 */
data class GridNavSection(
    val sectionId: String,
    val headingText: String,
    val environmentId: String,
    val resources: List<Resource>,
    val isPrimaryHeading: Boolean,
)

/**
 * Builds the ordered list of grid sections to render for the given springboard and
 * selected environment. All-envs section comes first (when present); env section second.
 * When `selectedEnvironmentId` is null (no environment chosen), only the all-envs
 * section is emitted.
 */
fun buildGridNavSections(
    springboard: Springboard,
    selectedEnvironmentId: String?,
): List<GridNavSection> {
    val hasAllEnvsSection = springboard.hasAnyAllEnvsActivators()

    return buildList {
        if (hasAllEnvsSection) {
            add(
                GridNavSection(
                    sectionId = ALL_ENVS_ENVIRONMENT_ID,
                    headingText = ALL_ENVS_DISPLAY_NAME,
                    environmentId = ALL_ENVS_ENVIRONMENT_ID,
                    resources = springboard.allEnvsResources(),
                    isPrimaryHeading = true,
                )
            )
        }
        if (selectedEnvironmentId != null) {
            val envName = springboard.environments.firstOrNull { it.id == selectedEnvironmentId }?.name
                ?: selectedEnvironmentId
            add(
                GridNavSection(
                    sectionId = selectedEnvironmentId,
                    headingText = "$envName Environment",
                    environmentId = selectedEnvironmentId,
                    resources = springboard.envSpecificResources(),
                    isPrimaryHeading = !hasAllEnvsSection,
                )
            )
        }
    }
}
