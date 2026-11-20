package app.marlboroadvance.mpvex.ui.preferences

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.ui.theme.spacing
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.AccessibilityPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SwitchPreference
import org.koin.compose.koinInject

/**
 * Settings for everything the player says out loud.
 *
 * Kept as a top-level entry next to Player/Subtitles/Audio rather than buried inside the subtitle
 * screen: someone who needs this cannot find it by guessing which media category it belongs to.
 */
@Serializable
object AccessibilityPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val preferences = koinInject<AccessibilityPreferences>()
    val backstack = LocalBackStack.current
    val context = LocalContext.current

    val manager =
      remember(context) {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
      }
    var readerRunning by remember(manager) { mutableStateOf(manager?.isEnabled == true) }
    DisposableEffect(manager) {
      val listener = AccessibilityManager.AccessibilityStateChangeListener { readerRunning = it }
      manager?.addAccessibilityStateChangeListener(listener)
      onDispose { manager?.removeAccessibilityStateChangeListener(listener) }
    }

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = stringResource(R.string.pref_accessibility),
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.primary,
            )
          },
          navigationIcon = {
            IconButton(onClick = backstack::removeLastOrNull) {
              Icon(
                Icons.AutoMirrored.Default.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.secondary,
              )
            }
          },
        )
      },
    ) { padding ->
      ProvidePreferenceLocals {
        LazyColumn(
          modifier =
            Modifier
              .fillMaxSize()
              .padding(padding),
        ) {
          item {
            // A status line, because "I switched it on and nothing happens" is usually either no
            // reader running or no subtitle track selected.
            Text(
              text =
                stringResource(
                  if (readerRunning) {
                    R.string.a11y_reader_detected
                  } else {
                    R.string.a11y_reader_not_detected
                  },
                ),
              style = MaterialTheme.typography.bodySmall,
              color =
                if (readerRunning) {
                  MaterialTheme.colorScheme.primary
                } else {
                  MaterialTheme.colorScheme.outline
                },
              modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium, vertical = 4.dp),
            )
          }

          item {
            PreferenceSectionHeader(title = stringResource(R.string.a11y_section_title))
          }

          item {
            PreferenceCard {
              val announceSubtitles by preferences.announceSubtitles.collectAsState()
              SwitchPreference(
                value = announceSubtitles,
                onValueChange = { preferences.announceSubtitles.set(it) },
                title = { Text(stringResource(R.string.pref_a11y_announce_subtitles_title)) },
                summary = {
                  Text(
                    stringResource(R.string.pref_a11y_announce_subtitles_summary),
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()

              val announceSecondary by preferences.announceSecondarySubtitles.collectAsState()
              SwitchPreference(
                value = announceSecondary,
                onValueChange = { preferences.announceSecondarySubtitles.set(it) },
                title = { Text(stringResource(R.string.pref_a11y_announce_secondary_title)) },
                summary = {
                  Text(
                    stringResource(R.string.pref_a11y_announce_secondary_summary),
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()

              val interrupt by preferences.interruptOnNewSubtitle.collectAsState()
              SwitchPreference(
                value = interrupt,
                onValueChange = { preferences.interruptOnNewSubtitle.set(it) },
                title = { Text(stringResource(R.string.pref_a11y_interrupt_title)) },
                summary = {
                  Text(
                    stringResource(R.string.pref_a11y_interrupt_summary),
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()

              val onlyWhenVisible by preferences.announceOnlyWhenSubtitlesVisible.collectAsState()
              SwitchPreference(
                value = onlyWhenVisible,
                onValueChange = { preferences.announceOnlyWhenSubtitlesVisible.set(it) },
                title = { Text(stringResource(R.string.pref_a11y_only_when_visible_title)) },
                summary = {
                  Text(
                    stringResource(R.string.pref_a11y_only_when_visible_summary),
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )
            }
          }
        }
      }
    }
  }
}
