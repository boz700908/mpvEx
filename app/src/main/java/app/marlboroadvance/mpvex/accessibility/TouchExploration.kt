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
 * reader running, those gestures must not reach the player.
 */
@Composable
fun rememberTouchExplorationEnabled(): State<Boolean> = rememberAccessibilityState { it.isTouchExplorationEnabled }

/**
 * Whether any accessibility service is enabled, not only one that explores by touch.
 *
 * Not every screen reader drives touch exploration: several third-party readers on Chinese ROMs
 * keep it off and work through focus alone. Gating the player's gestures on touch exploration
 * alone left the whole gesture surface live for those users, so the player also yields whenever a
 * reader is simply running.
 */
@Composable
fun rememberScreenReaderActive(): State<Boolean> = rememberAccessibilityState { it.isEnabled }

/**
 * Whether the player's own gestures must step aside for an accessibility service.
 */
@Composable
fun rememberGesturesSuspended(): State<Boolean> {
  val touchExploration = rememberTouchExplorationEnabled()
  val reader = rememberScreenReaderActive()
  return remember(touchExploration, reader) {
    object : State<Boolean> {
      override val value: Boolean
        get() = touchExploration.value || reader.value
    }
  }
}

@Composable
private fun rememberAccessibilityState(
  read: (AccessibilityManager) -> Boolean,
): State<Boolean> {
  val context = LocalContext.current
  val manager =
    remember(context) {
      context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    }
  val state = remember(manager, read) { mutableStateOf(manager?.let(read) == true) }

  DisposableEffect(manager) {
    val touchListener =
      AccessibilityManager.TouchExplorationStateChangeListener {
        state.value = manager?.let(read) == true
      }
    val stateListener =
      AccessibilityManager.AccessibilityStateChangeListener {
        state.value = manager?.let(read) == true
      }
    manager?.addTouchExplorationStateChangeListener(touchListener)
    manager?.addAccessibilityStateChangeListener(stateListener)
    onDispose {
      manager?.removeTouchExplorationStateChangeListener(touchListener)
      manager?.removeAccessibilityStateChangeListener(stateListener)
    }
  }

  return state
}
