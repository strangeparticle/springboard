package com.strangeparticle.springboard.app.unit.ui.brand

import com.strangeparticle.springboard.app.ui.brand.BrandRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BrandRegistryTest {

    @Test
    fun `default brand is strange particle light`() {
        assertEquals("strangeparticle-light", BrandRegistry.defaultBrand.id)
    }

    @Test
    fun `registry contains light and dark strange particle brands`() {
        val ids = BrandRegistry.entries.map { it.id }.toSet()
        assertTrue("strangeparticle-light" in ids)
        assertTrue("strangeparticle-dark" in ids)
    }

    @Test
    fun `each brand has a non-blank display name`() {
        for (entry in BrandRegistry.entries) {
            assertTrue(entry.displayName.isNotBlank(), "entry ${entry.id} has blank display name")
        }
    }

    @Test
    fun `find by id returns matching entry`() {
        val entry = BrandRegistry.find("strangeparticle-dark")
        assertEquals("strangeparticle-dark", entry.id)
    }

    @Test
    fun `find with null returns default`() {
        assertSame(BrandRegistry.defaultBrand, BrandRegistry.find(null))
    }

    @Test
    fun `find with unknown id returns default`() {
        assertSame(BrandRegistry.defaultBrand, BrandRegistry.find("nonexistent-brand"))
    }

    @Test
    fun `brand ids are unique`() {
        val ids = BrandRegistry.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
}
