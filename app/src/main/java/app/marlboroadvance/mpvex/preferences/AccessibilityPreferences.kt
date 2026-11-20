package app.marlboroadvance.mpvex.preferences

import app.marlboroadvance.mpvex.preferences.preference.PreferenceStore

/**
 * Preferences for the screen reader layer.
 *
 * These only control what the player *announces*; they never change what mpv renders on screen.
 */
class AccessibilityPreferences(
  preferenceStore: PreferenceStore,
) {
  /**
   * Speak the current subtitle line through accessibility events.
   *
   * Off by default: it only makes sense while a screen reader is running, and it is intrusive
   * for everyone else.
   */
  val announceSubtitles = preferenceStore.getBoolean("a11y_announce_subtitles", false)

  /** Also speak the secondary subtitle track, for dual-subtitle releases. */
  val announceSecondarySubtitles =
    preferenceStore.getBoolean("a11y_announce_secondary_subtitles", true)

  /**
   * Replace the line being spoken instead of queueing behind it. Keeps the reader in sync with the
   * picture; turn off only if the reader's own queueing is preferred.
   */
  val interruptOnNewSubtitle = preferenceStore.getBoolean("a11y_interrupt_on_new_subtitle", true)

  /**
   * Stay quiet while subtitles are hidden. mpv keeps the current cue available even when it is not
   * drawn, so without this the player would read lines the sighted user cannot see.
   */
  val announceOnlyWhenSubtitlesVisible =
    preferenceStore.getBoolean("a11y_announce_only_when_visible", true)
}
