package com.strangeparticle.springboard.app.ui.keynav

import androidx.compose.ui.input.key.*

const val KeyNavNoneOptionId = "NONE"
const val KeyNavNoneOptionLabel = "None"

enum class KeyNavDropDown {
    APP,
    RESOURCE,
    ENVIRONMENT,
}

fun determineNextFocusDropDownForTabKeypress(current: KeyNavDropDown, isShiftPressed: Boolean): KeyNavDropDown {
    return when (current) {
        KeyNavDropDown.APP -> if (isShiftPressed) KeyNavDropDown.ENVIRONMENT else KeyNavDropDown.RESOURCE
        KeyNavDropDown.RESOURCE -> if (isShiftPressed) KeyNavDropDown.APP else KeyNavDropDown.ENVIRONMENT
        KeyNavDropDown.ENVIRONMENT -> if (isShiftPressed) KeyNavDropDown.RESOURCE else KeyNavDropDown.APP
    }
}

fun appendTypeaheadBuffer(currentBuffer: String, inputChar: Char?): String {
    if (inputChar == null) return currentBuffer
    val normalized = inputChar.lowercaseChar()
    if (!normalized.isLetterOrDigit()) return currentBuffer
    return currentBuffer + normalized
}

fun findTypeaheadMatchId(
    buffer: String,
    items: List<Pair<String, String>>,
    enabledStates: Map<String, Boolean>,
): String? {
    if (buffer.isBlank()) return null

    val typeaheadItems = listOf(KeyNavNoneOptionId to KeyNavNoneOptionLabel) + items
    val normalizedBuffer = buffer.lowercase()

    val byId = typeaheadItems.find { (id, _) ->
        (id == KeyNavNoneOptionId || enabledStates[id] != false) &&
            id.lowercase().startsWith(normalizedBuffer)
    }
    if (byId != null) return byId.first

    val byName = typeaheadItems.find { (id, name) ->
        (id == KeyNavNoneOptionId || enabledStates[id] != false) &&
            name.lowercase().startsWith(normalizedBuffer)
    }
    return byName?.first
}

// We read the character from utf16CodePoint rather than matching on Key identity so that
// typeahead respects the user's active keyboard layout (AZERTY, Dvorak, non-Latin IMEs, etc.).
// Compose's Key enum represents the physical key position, which would give us the wrong
// character on any non-QWERTY layout; utf16CodePoint is the printable character the OS
// actually produced for that keystroke.
fun keyToTypeaheadChar(event: KeyEvent): Char? {
    val codePoint = event.utf16CodePoint
    if (codePoint <= 0) return null
    val char = codePoint.toChar().lowercaseChar()
    if (!char.isLetterOrDigit()) return null
    return char
}
