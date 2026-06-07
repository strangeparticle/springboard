package com.strangeparticle.springboard.app.ui.brand

import com.strangeparticle.springboard.app.ui.brand.brands.strangeparticle.strangeParticleDarkBrand
import com.strangeparticle.springboard.app.ui.brand.brands.strangeparticle.strangeParticleLightBrand

/**
 * Explicit list of brands that are selectable at runtime. This is deliberately
 * an explicit list rather than an auto-discovery scan so that forks can register
 * only the brands they want to expose to their users.
 *
 * The first entry is the fallback default used when no setting override is
 * present and when a lookup by id fails.
 */
object BrandRegistry {

    val entries: List<BrandRegistryEntry> = listOf(
        BrandRegistryEntry(
            id = "strangeparticle-light",
            displayName = "Strange Particle (Light)",
            produceBrand = { strangeParticleLightBrand() },
        ),
        BrandRegistryEntry(
            id = "strangeparticle-dark",
            displayName = "Strange Particle (Dark)",
            produceBrand = { strangeParticleDarkBrand() },
        ),
    )

    // Set the default brand here
    val defaultBrand: BrandRegistryEntry = find("strangeparticle-light")

    /**
     * Returns the brand entry for the given id, falling back to [defaultBrand] when
     * the id is null or does not correspond to a registered brand.
     */
    fun find(id: String?): BrandRegistryEntry =
        entries.firstOrNull { it.id == id } ?: defaultBrand
}
