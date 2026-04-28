package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.domain.model.App
import com.strangeparticle.springboard.app.domain.model.AppColumn
import com.strangeparticle.springboard.app.domain.model.AppGroup
import com.strangeparticle.springboard.app.domain.model.Environment
import com.strangeparticle.springboard.app.domain.model.SeparatorColumn
import com.strangeparticle.springboard.app.domain.model.Springboard
import com.strangeparticle.springboard.app.domain.model.SpringboardIndexes
import com.strangeparticle.springboard.app.domain.model.appColumnLayout
import kotlin.test.Test
import kotlin.test.assertEquals

class SpringboardAppColumnLayoutTest {

    private fun makeSpringboard(
        apps: List<App>,
        appGroups: List<AppGroup> = emptyList(),
    ): Springboard = Springboard(
        name = "Test",
        environments = listOf(Environment("env1", "Env 1")),
        apps = apps,
        resources = emptyList(),
        activators = emptyList(),
        guidanceData = emptyList(),
        displayHints = null,
        indexes = SpringboardIndexes(
            activatorByCoordinate = emptyMap(),
            activatableResourcesByApp = emptyMap(),
            activatableAppsByResource = emptyMap(),
            activatableResourcesByEnvApp = emptyMap(),
        ),
        source = "test",
        lastLoadTime = 0L,
        jsonSource = "{}",
        appGroups = appGroups,
    )

    @Test
    fun `with no appGroups declared the layout is just AppColumn entries in declaration order`() {
        val springboard = makeSpringboard(
            apps = listOf(App("app1", "One"), App("app2", "Two"), App("app3", "Three")),
        )

        val layout = springboard.appColumnLayout()

        assertEquals(
            listOf(AppColumn(App("app1", "One")), AppColumn(App("app2", "Two")), AppColumn(App("app3", "Three"))),
            layout,
        )
    }

    @Test
    fun `two groups all apps grouped emit grouped apps with one separator between groups`() {
        val springboard = makeSpringboard(
            apps = listOf(
                App("app1", "One", appGroupId = "groupA"),
                App("app2", "Two", appGroupId = "groupB"),
                App("app3", "Three", appGroupId = "groupA"),
                App("app4", "Four", appGroupId = "groupB"),
            ),
            appGroups = listOf(
                AppGroup("groupA", "Group A"),
                AppGroup("groupB", "Group B"),
            ),
        )

        val layout = springboard.appColumnLayout()

        assertEquals(
            listOf(
                AppColumn(App("app1", "One", appGroupId = "groupA")),
                AppColumn(App("app3", "Three", appGroupId = "groupA")),
                SeparatorColumn,
                AppColumn(App("app2", "Two", appGroupId = "groupB")),
                AppColumn(App("app4", "Four", appGroupId = "groupB")),
            ),
            layout,
        )
    }

    @Test
    fun `ungrouped apps render after all groups with one separator before the ungrouped block`() {
        val springboard = makeSpringboard(
            apps = listOf(
                App("app1", "One", appGroupId = "groupA"),
                App("app2", "Two"),
                App("app3", "Three", appGroupId = "groupB"),
                App("app4", "Four"),
            ),
            appGroups = listOf(
                AppGroup("groupA", "Group A"),
                AppGroup("groupB", "Group B"),
            ),
        )

        val layout = springboard.appColumnLayout()

        assertEquals(
            listOf(
                AppColumn(App("app1", "One", appGroupId = "groupA")),
                SeparatorColumn,
                AppColumn(App("app3", "Three", appGroupId = "groupB")),
                SeparatorColumn,
                AppColumn(App("app2", "Two")),
                AppColumn(App("app4", "Four")),
            ),
            layout,
        )
    }

    @Test
    fun `declared group with no apps is silently skipped and produces no orphan separator`() {
        val springboard = makeSpringboard(
            apps = listOf(
                App("app1", "One", appGroupId = "groupA"),
                App("app2", "Two", appGroupId = "groupC"),
            ),
            appGroups = listOf(
                AppGroup("groupA", "Group A"),
                AppGroup("groupB", "Group B"),
                AppGroup("groupC", "Group C"),
            ),
        )

        val layout = springboard.appColumnLayout()

        assertEquals(
            listOf(
                AppColumn(App("app1", "One", appGroupId = "groupA")),
                SeparatorColumn,
                AppColumn(App("app2", "Two", appGroupId = "groupC")),
            ),
            layout,
        )
    }

    @Test
    fun `appGroups declared but no apps reference them yields no separators`() {
        val springboard = makeSpringboard(
            apps = listOf(App("app1", "One"), App("app2", "Two")),
            appGroups = listOf(AppGroup("groupA", "Group A")),
        )

        val layout = springboard.appColumnLayout()

        assertEquals(
            listOf(AppColumn(App("app1", "One")), AppColumn(App("app2", "Two"))),
            layout,
        )
    }
}
