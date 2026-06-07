package com.strangeparticle.springboard.app.unit.ui.keynav

import com.strangeparticle.springboard.app.ui.keynav.KeyNavDropDown
import com.strangeparticle.springboard.app.ui.keynav.KeyNavNoneOptionId
import com.strangeparticle.springboard.app.ui.keynav.appendTypeaheadBuffer
import com.strangeparticle.springboard.app.ui.keynav.findTypeaheadMatchId
import com.strangeparticle.springboard.app.ui.keynav.determineNextFocusDropDownForTabKeypress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KeyNavKeyLogicTest {

    @Test
    fun `next focus field tabs forward with wrap`() {
        assertEquals(KeyNavDropDown.RESOURCE, determineNextFocusDropDownForTabKeypress(KeyNavDropDown.APP, isShiftPressed = false))
        assertEquals(KeyNavDropDown.ENVIRONMENT, determineNextFocusDropDownForTabKeypress(KeyNavDropDown.RESOURCE, isShiftPressed = false))
        assertEquals(KeyNavDropDown.APP, determineNextFocusDropDownForTabKeypress(KeyNavDropDown.ENVIRONMENT, isShiftPressed = false))
    }

    @Test
    fun `next focus field tabs backward with wrap`() {
        assertEquals(KeyNavDropDown.ENVIRONMENT, determineNextFocusDropDownForTabKeypress(KeyNavDropDown.APP, isShiftPressed = true))
        assertEquals(KeyNavDropDown.APP, determineNextFocusDropDownForTabKeypress(KeyNavDropDown.RESOURCE, isShiftPressed = true))
        assertEquals(KeyNavDropDown.RESOURCE, determineNextFocusDropDownForTabKeypress(KeyNavDropDown.ENVIRONMENT, isShiftPressed = true))
    }

    @Test
    fun `append typeahead buffer accepts lowercase alphanumeric characters`() {
        assertEquals("a", appendTypeaheadBuffer(currentBuffer = "", inputChar = 'a'))
        assertEquals("ab", appendTypeaheadBuffer(currentBuffer = "a", inputChar = 'b'))
        assertEquals("a1", appendTypeaheadBuffer(currentBuffer = "a", inputChar = '1'))
    }

    @Test
    fun `append typeahead buffer lowercases uppercase input and ignores symbols`() {
        assertEquals("n", appendTypeaheadBuffer(currentBuffer = "", inputChar = 'N'))
        assertEquals("n", appendTypeaheadBuffer(currentBuffer = "n", inputChar = '!'))
    }

    @Test
    fun `find typeahead match prefers id and includes none option`() {
        val items = listOf(
            "app1" to "App One",
            "res2" to "Logs",
        )
        val enabledStates = mapOf(
            "app1" to true,
            "res2" to true,
        )

        assertEquals(KeyNavNoneOptionId, findTypeaheadMatchId("n", items, enabledStates))
        assertEquals("res2", findTypeaheadMatchId("r", items, enabledStates))
    }

    @Test
    fun `find typeahead match falls back to name and skips disabled items`() {
        val items = listOf(
            "res1" to "Dashboard",
            "res2" to "Logs",
        )
        val enabledStates = mapOf(
            "res1" to true,
            "res2" to false,
        )

        assertEquals("res1", findTypeaheadMatchId("d", items, enabledStates))
        assertNull(findTypeaheadMatchId("l", items, enabledStates))
    }
}
