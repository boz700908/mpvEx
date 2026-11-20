package app.marlboroadvance.mpvex.accessibility

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import java.lang.ref.WeakReference

/**
 * Immutable view of the accessibility preferences this announcer needs.
 *
 * Kept as a plain data class so the announcer never touches preferences directly and can be
 * reasoned about (and unit-tested) without an Android preference store.
 */
data class SubtitleAnnounceConfig(
  val enabled: Boolean,
  val announceSecondary: Boolean,
  val interruptOnNewLine: Boolean,
  val onlyWhenSubtitlesVisible: Boolean,
) {
  companion object {
    val Disabled =
      SubtitleAnnounceConfig(
        enabled = false,
        announceSecondary = false,
        interruptOnNewLine = false,
        onlyWhenSubtitlesVisible = true,
      )
  }
}

/**
 * Speaks the current subtitle line through accessibility events.
 *
 * Text comes from mpv's `sub-text` / `secondary-sub-text` properties: mpv resolves the cue and
 * strips its formatting, so video files, audio files with an embedded subtitle track and external
 * subtitle files all arrive here the same way. This class only decides *when* and *how* to speak,
 * which is where subtitle announcement differs from ordinary UI narration:
 *
 * - the same line is never announced twice,
 * - a new line replaces the previous one instead of queueing behind it (otherwise the reader
 *   drifts further and further behind the picture),
 * - karaoke / typewriter subtitles re-emit one growing line many times per second, so a line that
 *   merely extends the previous one is debounced into a single announcement.
 *
 * Public methods must be called on the main thread.
 */
class SubtitleAnnouncer(
  context: Context,
  private val config: () -> SubtitleAnnounceConfig,
) {
  private val appContext: Context = context.applicationContext

  private val accessibilityManager: AccessibilityManager? =
    appContext.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager

  private val handler = Handler(Looper.getMainLooper())

  private var hostView = WeakReference<View>(null)

  private var primary = ""
  private var secondary = ""
  private var subtitlesVisible = true

  /** Last line handed to the reader, used for de-duplication and for growth detection. */
  private var lastSpokenLine = ""
  private var lastSpokenAt = 0L
  private var pending: Runnable? = null

  /**
   * True when a screen reader is currently attached to the device.
   */
  val isReaderActive: Boolean
    get() = accessibilityManager?.isEnabled == true

  /**
   * Hosts accessibility events. Announcements are sent from this view so they carry a source the
   * reader can attribute to the app; with no view attached (background audio playback) events are
   * sent straight to the accessibility service instead.
   */
  fun attach(view: View) {
    hostView = WeakReference(view)
  }

  fun detach() {
    hostView = WeakReference(null)
    cancelPending()
  }

  /** Called when mpv's `sub-text` changes. */
  fun onPrimaryText(text: String?) {
    primary = text.orEmpty()
    flushIfNeeded()
  }

  /** Called when mpv's `secondary-sub-text` changes (dual subtitles). */
  fun onSecondaryText(text: String?) {
    secondary = text.orEmpty()
    flushIfNeeded()
  }

  /** Called when mpv's `sub-visibility` changes. */
  fun onSubtitleVisibilityChanged(visible: Boolean) {
    subtitlesVisible = visible
    if (!visible) cancelPending()
  }

  /** Forgets the previous line, e.g. when a file is loaded, so its first cue speaks again. */
  fun reset() {
    primary = ""
    secondary = ""
    lastSpokenLine = ""
    cancelPending()
  }

  private fun flushIfNeeded() {
    val cfg = config()
    if (!cfg.enabled) {
      lastSpokenLine = ""
      return
    }
    if (cfg.onlyWhenSubtitlesVisible && !subtitlesVisible) return

    val line = composeLine(cfg)
    if (line.isEmpty()) {
      lastSpokenLine = ""
      return
    }
    if (line == lastSpokenLine) return

    val growing = lastSpokenLine.isNotEmpty() && line.startsWith(lastSpokenLine)
    val tooSoon = SystemClock.uptimeMillis() - lastSpokenAt < MIN_GAP_MS
    lastSpokenLine = line

    if (growing || tooSoon) schedule(line) else speak(line)
  }

  private fun composeLine(cfg: SubtitleAnnounceConfig): String {
    val main = normalize(primary)
    if (!cfg.announceSecondary) return main

    val second = normalize(secondary)
    return when {
      main.isEmpty() -> second
      second.isEmpty() || second == main -> main
      else -> "$main$SECONDARY_SEPARATOR$second"
    }
  }

  private fun schedule(line: String) {
    cancelPending()
    val runnable = Runnable { speak(line) }
    pending = runnable
    handler.postDelayed(runnable, DEBOUNCE_MS)
  }

  private fun speak(line: String) {
    pending = null
    val cfg = config()
    if (!cfg.enabled) return

    val manager = accessibilityManager ?: return
    if (!manager.isEnabled) return

    lastSpokenAt = SystemClock.uptimeMillis()
    if (cfg.interruptOnNewLine) runCatching { manager.interrupt() }

    val view = hostView.get()
    if (view != null && view.isAttachedToWindow && view.isShown) {
      view.announceForAccessibility(line)
    } else {
      sendGlobalAnnouncement(manager, line)
    }
  }

  /**
   * Announces without a source view. Used while the player UI is gone (background audio playback);
   * readers accept a package-level announcement, it is only less reliably attributed.
   */
  private fun sendGlobalAnnouncement(
    manager: AccessibilityManager,
    line: String,
  ) {
    @Suppress("DEPRECATION")
    val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT)
    event.packageName = appContext.packageName
    event.className = "android.widget.TextView"
    event.text.add(line)
    runCatching { manager.sendAccessibilityEvent(event) }
  }

  private fun cancelPending() {
    pending?.let(handler::removeCallbacks)
    pending = null
  }

  private fun normalize(raw: String): String {
    if (raw.isBlank()) return ""
    // mpv already strips ASS/SSA tags; what is left is the visible text with its line breaks.
    return WHITESPACE.replace(raw, " ").trim()
  }

  private companion object {
    /** Collapses the line breaks and padding a subtitle cue usually carries. */
    val WHITESPACE = Regex("\\s+")

    /** Separator between the primary and secondary line of a dual-subtitle cue. */
    const val SECONDARY_SEPARATOR = "；"

    /** Wait this long before speaking a line that is still growing (karaoke / typewriter). */
    const val DEBOUNCE_MS = 400L

    /** Never speak two different lines closer together than this. */
    const val MIN_GAP_MS = 250L
  }
}
