package app.marlboroadvance.mpvex.accessibility

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Whether a screen reader is currently exploring the screen by touch (TalkBack, ShinePlus, ...).
 *
 * The player's gesture surface is a full-screen pointer input layer: two-finger pinch, drag for
 * volume/brightness, double tap to seek. That is exactly the vocabulary touch exploration uses for
 * something else — a double tap means "activate" and a drag means "move focus" — so with a screen
 * reader running, those gestures must not reach the player. Anything the gestures can do has to be
 * reachable through focusable controls instead.
 */
@Composable
fun rememberTouchExplorationEnabled(): State<Boolean> {
  val context = LocalContext.current
  val manager =
    remember(context) {
      context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    }
  val enabled = remember(manager) { mutableStateOf(manager?.isTouchExplorationEnabled == true) }

  DisposableEffect(manager) {
    val listener =
      AccessibilityManager.TouchExplorationStateChangeListener { isEnabled ->
        enabled.value = isEnabled
      }
    manager?.addTouchExplorationStateChangeListener(listener)
    onDispose { manager?.removeTouchExplorationStateChangeListener(listener) }
  }

  return enabled
}
