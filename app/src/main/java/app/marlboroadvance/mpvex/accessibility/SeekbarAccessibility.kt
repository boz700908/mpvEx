package app.marlboroadvance.mpvex.accessibility

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import kotlin.math.roundToInt

/**
 * Turns a hand-drawn seek bar into a real slider for accessibility services.
 *
 * The player's seek bar is a Canvas with its own tap and drag handling, so to a screen reader it is
 * an inert rectangle: nothing to focus, nothing to adjust, no way to know where playback is. This
 * modifier publishes the playback position as a progress node and exposes the three things a
 * listener actually needs — set an absolute position, jump forward, jump back.
 *
 * No extra pointer handling is added on purpose: the visual drag handling stays exactly as it is,
 * and a screen reader drives the bar through its adjustment gestures and custom actions instead.
 *
 * [onSeek] takes seconds and [onSeekFinished] commits the seek, matching what the drag handler in
 * the seek bar already does.
 */
fun Modifier.seekbarAccessibility(
  position: Float,
  duration: Float,
  onSeek: (Float) -> Unit,
  onSeekFinished: () -> Unit,
  stepSeconds: Int = 10,
): Modifier {
  if (duration <= 0f) return this

  val clamped = position.coerceIn(0f, duration)

  // Playback position arrives several times a second. Publishing it raw makes the semantics change
  // at that rate, and every change makes the accessibility service re-read the tree — with a screen
  // reader running the player turns into a stutter. A whole second of precision is all a listener
  // needs from a slider, and it cuts the traffic by the update rate.
  val progress = (clamped / PROGRESS_GRANULARITY_SECONDS).toInt() * PROGRESS_GRANULARITY_SECONDS

  val seekTo: (Float) -> Unit = { seconds ->
    onSeek(seconds.coerceIn(0f, duration))
    onSeekFinished()
  }

  return this.semantics {
    contentDescription = "Playback position"
    progressBarRangeInfo =
      ProgressBarRangeInfo(
        current = progress.toFloat(),
        range = 0f..duration,
        steps = (duration / stepSeconds).roundToInt().coerceAtLeast(0),
      )
    setProgress { target ->
      seekTo(target)
      true
    }
    customActions =
      listOf(
        CustomAccessibilityAction("Forward $stepSeconds seconds") {
          seekTo(clamped + stepSeconds)
          true
        },
        CustomAccessibilityAction("Back $stepSeconds seconds") {
          seekTo(clamped - stepSeconds)
          true
        },
        CustomAccessibilityAction("Jump to start") {
          seekTo(0f)
          true
        },
      )
  }
}

/** Only whole seconds are published to accessibility services; see the note in the modifier. */
private const val PROGRESS_GRANULARITY_SECONDS = 1f
