/*
 * IME service for the MGC keyboard.
 *
 * Replaces the deprecated-API MentalMelookInputMethodService. This class
 * owns a [MGCKeyboardView] (our ported/derived keyboard view) and wires up
 * the three tracking hooks that already exist in ime/tracking/:
 *
 *   BackspaceTracker.onKeyProcessed(code)   — called on every key press
 *   WordCommitListener.onWordCommitted(word) — called at every word boundary
 *                                             (space / period / enter)
 *   KeyboardStatsSink.flush()               — called periodically and on exit
 *
 * Tracking hook invocation pattern is intentionally unchanged from the
 * original MentalMelookInputMethodService so that the stats pipeline is not
 * disturbed.
 *
 * Shift-state machine:
 *   Single tap Shift   → NONE → SHIFTED (one-shot)
 *   Double-tap Shift   → SHIFTED → CAPS_LOCK
 *   Tap Shift again    → CAPS_LOCK → NONE
 *   Any letter key     → if SHIFTED, auto-resets to NONE after commit
 *
 * Layer switching (KeyboardMode: ALPHA / SYMBOLS / EMOJI / CLIPBOARD):
 *   123 key   → SYMBOLS   (has digits + a 📋 key back to CLIPBOARD)
 *   ABC key   → ALPHA
 *   😊 key    → EMOJI
 *   📋 key    → CLIPBOARD (Copy / Paste / Cut / Select all)
 *   Numeric/phone/datetime input fields open directly in SYMBOLS.
 */
package com.example.mgc_keyboard.ime

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mgc_keyboard.ime.keyboard_core.KeyCodes
import com.example.mgc_keyboard.ime.keyboard_core.MGCKeyboardView
import com.example.mgc_keyboard.ime.tracking.BackspaceTracker
import com.example.mgc_keyboard.ime.tracking.KeyboardStatsSink
import com.example.mgc_keyboard.ime.tracking.SentimentScorer
import com.example.mgc_keyboard.ime.tracking.WordCommitListener
import com.example.mgc_keyboard.alerts.AlertNotifier
import com.example.mgc_keyboard.alerts.AlertThresholdsStore
import com.example.mgc_keyboard.alerts.ThresholdMonitor
import com.example.mgc_keyboard.statscore.DiagLog
import com.example.mgc_keyboard.statscore.StatsAggregator
import com.example.mgc_keyboard.statscore.StatsDatabase
import com.example.mgc_keyboard.statscore.StatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MGCInputMethodService : InputMethodService(),
    MGCKeyboardView.OnKeyboardActionListener {

    // ── Keyboard view ────────────────────────────────────────────────────────

    private lateinit var keyboardView: MGCKeyboardView

    // ── Word accumulation buffer ─────────────────────────────────────────────

    private val wordBuffer = StringBuilder()

    // ── Tracking hooks (same objects / same invocation as original service) ──

    private val aggregator       = StatsAggregator()
    private lateinit var backspaceTracker:  BackspaceTracker
    private lateinit var wordCommitListener: WordCommitListener
    private lateinit var statsSink:          KeyboardStatsSink

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var periodicFlushJob: Job? = null

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        DiagLog.init(applicationContext)
        DiagLog.d(TAG, "onCreate")
        val repository = StatsRepository.from(StatsDatabase.getInstance(applicationContext))
        val thresholdMonitor = ThresholdMonitor(repository, AlertNotifier(applicationContext))
        val thresholdsStore = AlertThresholdsStore(applicationContext)
        backspaceTracker   = BackspaceTracker(aggregator)
        wordCommitListener = WordCommitListener(SentimentScorer(), aggregator)
        statsSink          = KeyboardStatsSink(aggregator, repository, thresholdMonitor, thresholdsStore)

        // Periodic flush identical to the original service. Tracked in periodicFlushJob and
        // cancelled in onDestroy() — previously this loop was never stopped, so every
        // onCreate() (the IME service can be recreated repeatedly, e.g. across lock/unlock
        // cycles) leaked another infinite loop, each hitting the DB every FLUSH_INTERVAL_MS.
        // The resulting pile-up of concurrent flush() calls into the same SQLCipher
        // connection was the root cause of the native crashes seen right after unlocking.
        periodicFlushJob = serviceScope.launch {
            while (isActive) {
                delay(FLUSH_INTERVAL_MS)
                statsSink.flush()
            }
        }
    }

    override fun onCreateInputView(): View {
        keyboardView = MGCKeyboardView(this)
        keyboardView.setOnKeyboardActionListener(this)
        // Reserve space for the system's own keyboard-switch affordance (3-button nav
        // devices draw it docked at the bottom of the IME window) so the last key row
        // doesn't render underneath it.
        ViewCompat.setOnApplyWindowInsetsListener(keyboardView) { view, insets ->
            val navBarBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            // The system's floating "switch keyboard" affordance can be taller than the
            // measured nav-bar inset (esp. on gesture-nav devices, where the nav-bar inset
            // itself is a thin strip but the switcher icon overlaid on it is a full touch
            // target), so always reserve at least a standard touch-target's worth of space.
            val minSwitcherPx = (MIN_SWITCHER_DP * resources.displayMetrics.density + 0.5f).toInt()
            (view as MGCKeyboardView).bottomInsetPx = maxOf(navBarBottom, minSwitcherPx)
            insets
        }
        return keyboardView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        wordBuffer.clear()
        // Reset shift on each new input field. Numeric/phone/datetime fields open straight
        // on the symbols layer (it already has a digit row) instead of forcing a 123 tap.
        val numberLike = info?.inputType?.let { it and android.text.InputType.TYPE_MASK_CLASS } in
            setOf(android.text.InputType.TYPE_CLASS_NUMBER, android.text.InputType.TYPE_CLASS_PHONE, android.text.InputType.TYPE_CLASS_DATETIME)
        keyboardView.mode = if (numberLike) MGCKeyboardView.KeyboardMode.SYMBOLS else MGCKeyboardView.KeyboardMode.ALPHA
        keyboardView.shiftState = MGCKeyboardView.ShiftState.NONE
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        DiagLog.d(TAG, "onFinishInputView finishingInput=$finishingInput")
        serviceScope.launch { statsSink.flush() }
    }

    override fun onDestroy() {
        super.onDestroy()
        DiagLog.d(TAG, "onDestroy")
        // Stop the periodic loop first so it can't race the final flush below or outlive
        // this service instance.
        periodicFlushJob?.cancel()
        serviceScope.launch { statsSink.flush() }
    }

    // ── Key event handling ───────────────────────────────────────────────────

    /**
     * Central key handler. Processing order:
     *  1. Always tell BackspaceTracker about the key (counts total + backspace presses).
     *  2. Dispatch on code to the appropriate action.
     *  3. Auto-reset shift after a letter is committed (if in one-shot SHIFTED state).
     */
    override fun onKey(primaryCode: Int) {
        // Tracking hook #1 — mirrors original MentalMelookInputMethodService.onKey
        backspaceTracker.onKeyProcessed(primaryCode)

        val ic = currentInputConnection ?: return

        when (primaryCode) {
            KeyCodes.DELETE -> {
                ic.deleteSurroundingText(1, 0)
                if (wordBuffer.isNotEmpty()) {
                    wordBuffer.deleteCharAt(wordBuffer.length - 1)
                }
            }

            KeyCodes.SHIFT -> cycleShift()

            KeyCodes.ENTER -> {
                // An editor that declares an action (Send / Search / Go / Done / Next) expects
                // Enter to fire it, not to insert a newline — committing "\n" unconditionally was
                // why "return" did nothing useful in chat and search fields. sendDefaultEditorAction
                // honours IME_FLAG_NO_ENTER_ACTION, so genuinely multi-line fields return false and
                // fall through to a real Enter key event (works in WebViews too, unlike commitText).
                // ponytail: it only bails on IME_ACTION_NONE, not IME_ACTION_UNSPECIFIED, so an
                // editor setting neither an action nor the flag gets a no-op instead of a newline.
                // Match on editorInfo.imeOptions directly if that ever shows up in practice.
                if (!sendDefaultEditorAction(true)) {
                    sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                }
                commitAndClearBuffer()
            }

            KeyCodes.SPACE, KeyCodes.PERIOD -> {
                ic.commitText(primaryCode.toChar().toString(), 1)
                commitAndClearBuffer()
            }

            KeyCodes.MODE_SYMBOLS -> {
                keyboardView.mode = MGCKeyboardView.KeyboardMode.SYMBOLS
                keyboardView.shiftState = MGCKeyboardView.ShiftState.NONE
            }

            KeyCodes.MODE_ALPHABET -> {
                keyboardView.mode = MGCKeyboardView.KeyboardMode.ALPHA
            }

            KeyCodes.MODE_CLIPBOARD -> {
                keyboardView.mode = MGCKeyboardView.KeyboardMode.CLIPBOARD
            }

            KeyCodes.EMOJI -> {
                keyboardView.mode = MGCKeyboardView.KeyboardMode.EMOJI
            }

            KeyCodes.COPY       -> ic.performContextMenuAction(android.R.id.copy)
            KeyCodes.PASTE      -> ic.performContextMenuAction(android.R.id.paste)
            KeyCodes.CUT        -> ic.performContextMenuAction(android.R.id.cut)
            KeyCodes.SELECT_ALL -> ic.performContextMenuAction(android.R.id.selectAll)

            else -> {
                if (primaryCode > 0) {
                    // Codepoints above the BMP (most emoji) don't fit in a single Char —
                    // Character.toChars() gives the correct one-or-two-char UTF-16 sequence.
                    var text = String(Character.toChars(primaryCode))
                    val isLetter = text.length == 1 && text[0].isLetter()
                    if (isLetter && keyboardView.shiftState != MGCKeyboardView.ShiftState.NONE) {
                        text = text.uppercase()
                    }
                    ic.commitText(text, 1)
                    wordBuffer.append(text)

                    // One-shot shift: reset after single letter is committed
                    if (isLetter && keyboardView.shiftState == MGCKeyboardView.ShiftState.SHIFTED) {
                        keyboardView.shiftState = MGCKeyboardView.ShiftState.NONE
                    }
                }
            }
        }
    }

    /**
     * Shift state machine:
     *   NONE       → SHIFTED    (single tap)
     *   SHIFTED    → CAPS_LOCK  (double tap / tap again while shifted)
     *   CAPS_LOCK  → NONE       (tap again to turn off)
     */
    private fun cycleShift() {
        keyboardView.shiftState = when (keyboardView.shiftState) {
            MGCKeyboardView.ShiftState.NONE      -> MGCKeyboardView.ShiftState.SHIFTED
            MGCKeyboardView.ShiftState.SHIFTED   -> MGCKeyboardView.ShiftState.CAPS_LOCK
            MGCKeyboardView.ShiftState.CAPS_LOCK -> MGCKeyboardView.ShiftState.NONE
        }
    }

    /**
     * Flush the accumulated word through the sentiment scorer and clear the buffer.
     * Tracking hook #2 — mirrors original MentalMelookInputMethodService.commitBufferedWord.
     */
    private fun commitAndClearBuffer() {
        if (wordBuffer.isEmpty()) return
        val word = wordBuffer.toString()
        wordBuffer.clear()
        // Dispatch to WordCommitListener on a background thread, same as original
        serviceScope.launch { wordCommitListener.onWordCommitted(word) }
    }

    // ── OnKeyboardActionListener stubs ───────────────────────────────────────

    override fun onPress(primaryCode: Int)   = Unit
    override fun onRelease(primaryCode: Int) = Unit

    // ── Constants ────────────────────────────────────────────────────────────

    private companion object {
        const val TAG = "MGCInputMethodService"
        const val FLUSH_INTERVAL_MS = 10_000L
        const val MIN_SWITCHER_DP = 48f
    }
}
