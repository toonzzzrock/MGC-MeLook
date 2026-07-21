/*
 * Hardcoded English QWERTY and Symbols keyboard layouts.
 *
 * Architectural approach inspired by AnySoftKeyboard's Keyboard/AnyKeyboard pattern:
 * the layout produces a flat list of Key objects with pixel positions baked in,
 * which the view just iterates for drawing and hit-testing.
 *
 * Differences from upstream:
 * - No XML parsing, no addon/AddOn system — layouts are defined in code
 * - No swappable languages — single hardcoded English QWERTY
 * - Row definitions use fractional weights; pixel positions are resolved
 *   once when the view knows its size
 */
package com.example.mgc_keyboard.ime.keyboard_core

/**
 * Builds [Key] lists for QWERTY and symbols layouts given the keyboard's pixel dimensions.
 *
 * Layout grid: the full keyboard width is divided into 10 "unit" columns.
 * All widths are expressed as multiples of one unit. Each row occupies
 * one quarter of the total height.
 *
 * QWERTY:
 *   Row 0 (Q..P):   10 × 1-unit keys
 *   Row 1 (A..L):    9 × 1-unit keys, 0.5-unit inset on each side
 *   Row 2 (⇧..⌫):  1.5-unit Shift + 7 × 1-unit letters + 1.5-unit Delete
 *   Row 3 (actions): 1.5-unit 123 + 1-unit emoji + 4.5-unit Space + 1-unit . + 2-unit Return
 *
 * Symbols layout reuses the same grid structure for a basic number/punctuation page.
 */
object KeyboardLayout {

    // ── Internal spec types ─────────────────────────────────────────────────

    private data class KeySpec(
        val code: Int,
        val label: String,
        val widthUnits: Float,       // width in units (total width = 10 units)
        val xOffsetUnits: Float = 0f, // extra left gap in units (used for row-1 inset)
        val isRepeatable: Boolean = false,
        val isSpecial: Boolean = false
    )

    // ── QWERTY row specs ────────────────────────────────────────────────────

    private val QWERTY_ROWS: List<List<KeySpec>> = listOf(
        // Row 0 — QWERTYUIOP
        listOf(
            KeySpec(113, "Q", 1f), KeySpec(119, "W", 1f), KeySpec(101, "E", 1f),
            KeySpec(114, "R", 1f), KeySpec(116, "T", 1f), KeySpec(121, "Y", 1f),
            KeySpec(117, "U", 1f), KeySpec(105, "I", 1f), KeySpec(111, "O", 1f),
            KeySpec(112, "P", 1f)
        ),
        // Row 1 — ASDFGHJKL (0.5-unit inset so the row is visually centred)
        listOf(
            KeySpec(97,  "A", 1f, xOffsetUnits = 0.5f),
            KeySpec(115, "S", 1f), KeySpec(100, "D", 1f), KeySpec(102, "F", 1f),
            KeySpec(103, "G", 1f), KeySpec(104, "H", 1f), KeySpec(106, "J", 1f),
            KeySpec(107, "K", 1f), KeySpec(108, "L", 1f)
        ),
        // Row 2 — Shift + ZXCVBNM + Delete
        listOf(
            KeySpec(KeyCodes.SHIFT,  "⇧", 1.5f, isSpecial = true),
            KeySpec(122, "Z", 1f), KeySpec(120, "X", 1f), KeySpec(99,  "C", 1f),
            KeySpec(118, "V", 1f), KeySpec(98,  "B", 1f), KeySpec(110, "N", 1f),
            KeySpec(109, "M", 1f),
            KeySpec(KeyCodes.DELETE, "⌫", 1.5f, isRepeatable = true, isSpecial = true)
        ),
        // Row 3 — action bar
        listOf(
            KeySpec(KeyCodes.MODE_SYMBOLS, "123",    1.5f, isSpecial = true),
            KeySpec(KeyCodes.EMOJI,        "😊",     1.0f, isSpecial = true),
            KeySpec(KeyCodes.SPACE,        "space",  4.5f),
            KeySpec(KeyCodes.PERIOD,       ".",       1.0f),
            KeySpec(KeyCodes.ENTER,        "return", 2.0f, isSpecial = true)
        )
    )

    // ── Symbols row specs ───────────────────────────────────────────────────

    private val SYMBOLS_ROWS: List<List<KeySpec>> = listOf(
        // Row 0 — digits
        listOf(
            KeySpec(49, "1", 1f), KeySpec(50,  "2", 1f), KeySpec(51, "3", 1f),
            KeySpec(52, "4", 1f), KeySpec(53,  "5", 1f), KeySpec(54, "6", 1f),
            KeySpec(55, "7", 1f), KeySpec(56,  "8", 1f), KeySpec(57, "9", 1f),
            KeySpec(48, "0", 1f)
        ),
        // Row 1 — punctuation
        listOf(
            KeySpec(33,  "!",  1f, xOffsetUnits = 0.5f),
            KeySpec(64,  "@",  1f), KeySpec(35, "#",  1f), KeySpec(36, "$",  1f),
            KeySpec(37,  "%",  1f), KeySpec(94, "^",  1f), KeySpec(38, "&",  1f),
            KeySpec(42,  "*",  1f), KeySpec(40, "(",  1f)
        ),
        // Row 2 — more symbols + delete
        listOf(
            KeySpec(KeyCodes.DELETE, "⌫",  1.5f, isRepeatable = true, isSpecial = true),
            KeySpec(41,  ")",  1f), KeySpec(45, "-",  1f), KeySpec(43, "+",  1f),
            KeySpec(61,  "=",  1f), KeySpec(47, "/",  1f), KeySpec(58, ":",  1f),
            KeySpec(59,  ";",  1f),
            KeySpec(63,  "?",  1.5f, isSpecial = false)
        ),
        // Row 3 — back to alpha + common punctuation + enter
        listOf(
            KeySpec(KeyCodes.MODE_ALPHABET, "ABC",    1.5f, isSpecial = true),
            KeySpec(34,  "\"", 1.0f),
            KeySpec(39,  "'",  1.0f),
            KeySpec(KeyCodes.SPACE, "space", 3.5f),
            KeySpec(46,  ".",  1.0f),
            KeySpec(KeyCodes.ENTER, "return", 2.0f, isSpecial = true)
        )
    )

    // ── Public builder ───────────────────────────────────────────────────────

    /** Build the QWERTY key list for a view of the given pixel [width] and [height]. */
    fun buildQwerty(width: Int, height: Int): List<Key> =
        buildKeys(QWERTY_ROWS, width, height)

    /** Build the symbols key list for a view of the given pixel [width] and [height]. */
    fun buildSymbols(width: Int, height: Int): List<Key> =
        buildKeys(SYMBOLS_ROWS, width, height)

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun buildKeys(
        rows: List<List<KeySpec>>,
        totalWidth: Int,
        totalHeight: Int
    ): List<Key> {
        val numRows = rows.size
        val rowHeight = totalHeight / numRows
        val unitWidth = totalWidth / 10f
        val keys = mutableListOf<Key>()

        rows.forEachIndexed { rowIndex, specs ->
            val rowTop = rowIndex * rowHeight
            var curX = 0f

            specs.forEachIndexed { specIndex, spec ->
                if (specIndex == 0) curX += spec.xOffsetUnits * unitWidth
                val keyWidth = (spec.widthUnits * unitWidth).toInt()
                keys += Key(
                    code = spec.code,
                    label = spec.label,
                    x = curX.toInt(),
                    y = rowTop,
                    width = keyWidth,
                    height = rowHeight,
                    isRepeatable = spec.isRepeatable,
                    isSpecial = spec.isSpecial
                )
                curX += keyWidth
            }
        }

        return keys
    }
}
