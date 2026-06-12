package com.strangeparticle.luther.client.provider

/** A selectable option surfaced by [LutherProviderCatalog]: [valueId] is the persisted/used value,
 *  [displayLabel] is shown to the user. Replaces the old Springboard `DropDownOption` at the seam. */
internal data class Choice(val valueId: String, val displayLabel: String)
