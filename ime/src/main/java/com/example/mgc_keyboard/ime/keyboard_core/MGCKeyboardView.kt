/*
 * Custom keyboard view derived from AnySoftKeyboard's AnyKeyboardViewBase.
 * Copyright (c) 2013 Menny Even-Danan — Apache License 2.0
 * https://github.com/AnySoftKeyboard/AnySoftKeyboard
 *
 * Key changes from upstream AnyKeyboardViewBase / AnyKeyboardView:
 *  - No addon/theme system — simple hardcoded light palette matching the Figma mock
 *  - No RxJava — pure Kotlin with Handler for repeat events
 *  - No mini/popup keyboards
 *  - No gesture-typing or swipe recognition
 *  - Proximity key detection retained (nearest-key logic ported from
 *    ProximityKeyDetector.java and adapted to Kotlin)
 *  - Shift-state rendering (NONE / SHIFTED / CAPS_LOCK) handled in onDraw
 *
 * Sections:
 *   1. Public interface + state
 *   2. Paint / drawing constants
 *   3. onMeasure / onSizeChanged — layout keys
 *   4. onDraw — renders every key
 *   5. onTouchEvent — dispatches to key-press / key-release / key-fire
 *   6. findNearestKey — proximity detection (ported from ProximityKeyDetector)
 *   7. Repeat-key handler (backspace held)
 */
package com.example.mgc_keyboard.ime.keyboard_core

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

/**
 * A self-contained custom [View] that renders a list of [Key] objects and
 * dispatches key events to an [OnKeyboardActionListener].
 *
 * The view is display-size-independent: keys are rebuilt from scratch in
 * [onSizeChanged] so rotating the device or changing font size always
 * produces a correctly sized layout.
 */
class MGCKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // ── 1. Public interface ──────────────────────────────────────────────────

    /** Mirrors the shape of AnySoftKeyboard's OnKeyboardActionListener. */
    interface OnKeyboardActionListener {
        fun onKey(primaryCode: Int)
        fun onPress(primaryCode: Int) {}
        fun onRelease(primaryCode: Int) {}
    }

    /**
     * Shift state, analogous to the three-state shift in AnySoftKeyboard:
     * NONE → SHIFTED (single-shot) → CAPS_LOCK → NONE.
     *
     * The IME service cycles through these on tap/double-tap of the Shift key.
     * Setting this property triggers a full redraw.
     */
    enum class ShiftState { NONE, SHIFTED, CAPS_LOCK }

    var shiftState: ShiftState = ShiftState.NONE
        set(value) {
            field = value
            invalidate()
        }

    // Whether we're currently showing QWERTY (true) or Symbols (false).
    // The IME service sets this; changing it triggers a layout rebuild.
    var isAlphaMode: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                rebuildKeysForCurrentSize()
                invalidate()
            }
        }

    private var listener: OnKeyboardActionListener? = null
    private var allKeys: List<Key> = emptyList()

    fun setOnKeyboardActionListener(l: OnKeyboardActionListener) {
        listener = l
    }

    /**
     * Extra blank space reserved at the very bottom of the view, below the key rows.
     * On 3-button-nav devices the system draws its own "switch keyboard" affordance
     * docked at the bottom of the IME window; without this reserved strip the last
     * key row renders flush to the edge and gets covered by it. Set from the IME
     * service using the navigation-bar window inset.
     */
    var bottomInsetPx: Int = 0
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
                rebuildKeysForCurrentSize()
                invalidate()
            }
        }

    // ── 2. Paint / drawing constants ─────────────────────────────────────────

    // Light Figma-style palette: white keys on a soft gray board
    private val colorBoard      = Color.parseColor("#D1D5DB")  // keyboard surround
    private val colorKeyFace    = Color.WHITE                  // regular letter key
    private val colorKeySpecial = Color.parseColor("#AEB3BC")  // shift, del, 123, return
    private val colorKeyPressed = Color.parseColor("#C2C8D0")  // any key while pressed
    private val colorKeyShadow  = Color.parseColor("#8E96A5")  // bottom-edge shadow line
    private val colorText       = Color.parseColor("#1A1A1A")  // label text
    private val colorTextShift  = Color.parseColor("#2563EB")  // shift-active label tint

    private val paintFill   = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val paintText   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = colorText
        typeface  = Typeface.DEFAULT
        textAlign = Paint.Align.CENTER
    }
    private val tmpRect = RectF()

    // ── 3. Measure / size-change ─────────────────────────────────────────────

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Let the IME container decide width; enforce a fixed 4-row height plus
        // whatever bottom inset is reserved for the system's keyboard-switch affordance.
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val rowHeightPx = (ROW_HEIGHT_DP * resources.displayMetrics.density + 0.5f).toInt()
        val h = rowHeightPx * 4 + bottomInsetPx
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildKeysForCurrentSize()
    }

    private fun rebuildKeysForCurrentSize() {
        if (width == 0 || height == 0) return
        val usableHeight = (height - bottomInsetPx).coerceAtLeast(1)
        allKeys = if (isAlphaMode) {
            KeyboardLayout.buildQwerty(width, usableHeight)
        } else {
            KeyboardLayout.buildSymbols(width, usableHeight)
        }
    }

    // ── 4. Drawing ───────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(colorBoard)

        val density   = resources.displayMetrics.density
        val metrics   = resources.displayMetrics
        val cornerR   = KEY_CORNER_DP * density
        val margin    = KEY_MARGIN_DP * density
        val shadowH   = KEY_SHADOW_DP * density

        for (key in allKeys) {
            val left   = key.x.toFloat() + margin
            val top    = key.y.toFloat() + margin
            val right  = (key.x + key.width).toFloat() - margin
            val bottom = (key.y + key.height).toFloat() - margin

            // Shadow stripe at the bottom of every key
            paintFill.color = colorKeyShadow
            tmpRect.set(left, bottom - shadowH, right, bottom)
            canvas.drawRoundRect(tmpRect, cornerR, cornerR, paintFill)

            // Key face (slightly higher than shadow, giving a raised-key illusion)
            paintFill.color = when {
                key === pressedKey -> colorKeyPressed
                key.isSpecial      -> colorKeySpecial
                else               -> colorKeyFace
            }
            tmpRect.set(left, top, right, bottom - shadowH)
            canvas.drawRoundRect(tmpRect, cornerR, cornerR, paintFill)

            // Label
            val label = displayLabel(key)
            if (label.isEmpty()) continue

            val spValue = when {
                label.length >= 5 -> TEXT_SMALL_SP
                label.length >= 3 -> TEXT_MEDIUM_SP
                else              -> TEXT_LARGE_SP
            }
            val textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue, metrics)
            paintText.textSize = textSize
            paintText.color = when {
                key.code == KeyCodes.SHIFT && shiftState != ShiftState.NONE -> colorTextShift
                else -> colorText
            }

            val cy = top + (bottom - shadowH - top) / 2f -
                     (paintText.ascent() + paintText.descent()) / 2f
            val cx = (left + right) / 2f
            canvas.drawText(label, cx, cy, paintText)
        }
    }

    /**
     * Returns the text to display on [key], applying shift transformation for
     * single-letter labels. Non-letter labels (shift arrow, ⌫, "space", digits,
     * symbols) are returned as-is.
     *
     * Inspired by AnyKeyboardViewBase's key-label rendering logic.
     */
    private fun displayLabel(key: Key): String {
        val label = key.label
        if (label.length == 1 && label[0].isLetter()) {
            return if (shiftState != ShiftState.NONE) label.uppercase() else label.lowercase()
        }
        return label
    }

    // ── 5. Touch handling ────────────────────────────────────────────────────

    private var pressedKey: Key? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> handleDown(event.x, event.y)
            MotionEvent.ACTION_MOVE -> handleMove(event.x, event.y)
            MotionEvent.ACTION_UP   -> handleUp(event.x, event.y)
            MotionEvent.ACTION_CANCEL -> handleCancel()
        }
        return true
    }

    private fun handleDown(x: Float, y: Float) {
        val key = findNearestKey(x, y) ?: return
        pressedKey = key
        invalidate()
        listener?.onPress(key.code)
        if (key.isRepeatable) {
            repeatKey = key
            handler.postDelayed(repeatRunnable, REPEAT_INITIAL_DELAY_MS)
        }
    }

    private fun handleMove(x: Float, y: Float) {
        val key = findNearestKey(x, y)
        if (key !== pressedKey) {
            pressedKey?.let { listener?.onRelease(it.code) }
            pressedKey = key
            invalidate()
            // Cancel repeat if the finger slid off the repeatable key
            if (repeatKey != null && key !== repeatKey) {
                handler.removeCallbacks(repeatRunnable)
                repeatKey = null
            }
            key?.let { listener?.onPress(it.code) }
        }
    }

    private fun handleUp(x: Float, y: Float) {
        cancelRepeat()
        val key = pressedKey
        pressedKey = null
        invalidate()
        if (key != null) {
            listener?.onKey(key.code)
            listener?.onRelease(key.code)
        }
    }

    private fun handleCancel() {
        cancelRepeat()
        pressedKey?.let { listener?.onRelease(it.code) }
        pressedKey = null
        invalidate()
    }

    // ── 6. Proximity key detection ───────────────────────────────────────────
    //
    // Ported from AnySoftKeyboard's ProximityKeyDetector.java.
    // Strategy:
    //   1. If the touch point falls exactly inside a key's bounds → return it.
    //   2. Otherwise find the key whose centre is nearest, weighting y-distance
    //      more heavily so keys on other rows are strongly de-preferred.
    //      This avoids "row-bleeding" for letters whose rows are vertically close.

    private fun findNearestKey(x: Float, y: Float): Key? {
        if (allKeys.isEmpty()) return null

        // 1. Exact hit
        for (key in allKeys) {
            if (x >= key.x && x < key.x + key.width &&
                y >= key.y && y < key.y + key.height) {
                return key
            }
        }

        // 2. Proximity: weighted Euclidean distance to key centre
        var best: Key? = null
        var bestDist = Float.MAX_VALUE
        for (key in allKeys) {
            val cx = key.x + key.width  / 2f
            val cy = key.y + key.height / 2f
            val dx = x - cx
            val dy = (y - cy) * Y_WEIGHT   // up-weight vertical distance
            val dist = dx * dx + dy * dy
            if (dist < bestDist) {
                bestDist = dist
                best = key
            }
        }
        return best
    }

    // ── 7. Repeat-key handler ────────────────────────────────────────────────

    private val handler = Handler(Looper.getMainLooper())
    private var repeatKey: Key? = null

    private val repeatRunnable = object : Runnable {
        override fun run() {
            val key = repeatKey ?: return
            listener?.onKey(key.code)
            handler.postDelayed(this, REPEAT_INTERVAL_MS)
        }
    }

    private fun cancelRepeat() {
        handler.removeCallbacks(repeatRunnable)
        repeatKey = null
    }

    // ── Constants ────────────────────────────────────────────────────────────

    companion object {
        private const val ROW_HEIGHT_DP      = 52f   // matches Figma row height
        private const val KEY_MARGIN_DP      = 3f
        private const val KEY_CORNER_DP      = 6f
        private const val KEY_SHADOW_DP      = 1.5f

        private const val TEXT_LARGE_SP      = 18f
        private const val TEXT_MEDIUM_SP     = 13f
        private const val TEXT_SMALL_SP      = 11f

        private const val Y_WEIGHT           = 2.5f  // from ASK ProximityKeyDetector heuristic

        private const val REPEAT_INITIAL_DELAY_MS = 400L
        private const val REPEAT_INTERVAL_MS      =  50L
    }
}
