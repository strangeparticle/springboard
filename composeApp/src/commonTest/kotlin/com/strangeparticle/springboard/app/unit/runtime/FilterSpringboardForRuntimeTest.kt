package com.strangeparticle.springboard.app.unit.runtime

import com.strangeparticle.springboard.app.domain.factory.SpringboardFactory
import com.strangeparticle.springboard.app.domain.model.CommandActivator
import com.strangeparticle.springboard.app.domain.model.Coordinate
import com.strangeparticle.springboard.app.runtime.filterSpringboardForRuntime
import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.shared.TestFixtureJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FilterSpringboardForRuntimeTest {

    @Test
    fun `wasm hides command activators in filtered springboard`() {
        val full = SpringboardFactory.fromJson(TestFixtureJson.COMMAND_ACTIVATOR, "/test/commands.json")

        val filtered = filterSpringboardForRuntime(full, RuntimeEnvironment.WASM)

        val coordinate = Coordinate("dev", "app1", "res1")
        assertEquals(1, full.activators.filterIsInstance<CommandActivator>().size)
        assertNotNull(full.indexes.activatorByCoordinate[coordinate])
        assertEquals(0, filtered.activators.filterIsInstance<CommandActivator>().size)
        assertNull(filtered.indexes.activatorByCoordinate[coordinate])
    }

    @Test
    fun `desktop keeps command activators in filtered springboard`() {
        val full = SpringboardFactory.fromJson(TestFixtureJson.COMMAND_ACTIVATOR, "/test/commands.json")

        val filtered = filterSpringboardForRuntime(full, RuntimeEnvironment.DesktopOsx)

        val coordinate = Coordinate("dev", "app1", "res1")
        assertNotNull(filtered.indexes.activatorByCoordinate[coordinate])
        assertEquals(1, filtered.activators.filterIsInstance<CommandActivator>().size)
    }

    @Test
    fun `filtering does not mutate original springboard`() {
        val full = SpringboardFactory.fromJson(TestFixtureJson.COMMAND_ACTIVATOR, "/test/commands.json")

        filterSpringboardForRuntime(full, RuntimeEnvironment.WASM)

        val coordinate = Coordinate("dev", "app1", "res1")
        assertEquals(1, full.activators.filterIsInstance<CommandActivator>().size)
        assertNotNull(full.indexes.activatorByCoordinate[coordinate])
    }
}
