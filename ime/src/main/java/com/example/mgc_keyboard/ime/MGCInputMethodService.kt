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
 * Symbols switch:
 *   123 key → switch to symbols layer (isAlphaMode = false)
 *   ABC key → switch back to alpha layer (isAlphaMode = true)
 */
package com.example.mgc_keyboard.ime

import android.inputmethodservice.InputMethodService
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
        // Reset to alpha layout and clear shift on each new input field
        keyboardView.isAlphaMode = true
        keyboardView.shiftState  = MGCKeyboardView.ShiftState.NONE
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
                ic.commitText("\n", 1)
                commitAndClearBuffer()
            }

            KeyCodes.SPACE, KeyCodes.PERIOD -> {
                ic.commitText(primaryCode.toChar().toString(), 1)
                commitAndClearBuffer()
            }

            KeyCodes.MODE_SYMBOLS -> {
                keyboardView.isAlphaMode = false
                keyboardView.shiftState  = MGCKeyboardView.ShiftState.NONE
            }

            KeyCodes.MODE_ALPHABET -> {
                keyboardView.isAlphaMode = true
            }

            KeyCodes.EMOJI -> {
                // TODO: emoji panel — no-op for now
            }

            else -> {
                if (primaryCode > 0) {
                    var ch = primaryCode.toChar()
                    if (ch.isLetter() && keyboardView.shiftState != MGCKeyboardView.ShiftState.NONE) {
                        ch = ch.uppercaseChar()
                    }
                    ic.commitText(ch.toString(), 1)
                    wordBuffer.append(ch)

                    // One-shot shift: reset after single letter is committed
                    if (keyboardView.shiftState == MGCKeyboardView.ShiftState.SHIFTED) {
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
