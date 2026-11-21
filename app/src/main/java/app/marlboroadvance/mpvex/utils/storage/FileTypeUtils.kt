package app.marlboroadvance.mpvex.utils.storage

import java.io.File
import java.util.Locale

/**
 * File Type Utilities
 * Handles video and audio file detection and MIME type resolution.
 */
object FileTypeUtils {

  val VIDEO_EXTENSIONS: Set<String> = setOf(
    "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp", "3g2",
    "mpg", "mpeg", "m2v", "ogv", "ts", "mts", "m2ts", "vob", "divx", "xvid",
    "f4v", "rm", "rmvb", "asf"
  )

  /**
   * Audio extensions mpv plays natively.
   *
   * The app was written as a video browser and hid everything else, which also hid the one case
   * that matters for a listener: an audio file carrying its own subtitle track.
   */
  val AUDIO_EXTENSIONS: Set<String> = setOf(
    "mp3", "flac", "m4a", "m4b", "aac", "opus", "ogg", "oga", "wav", "wma",
    "ape", "alac", "mka", "ac3", "dts", "aiff", "aif", "amr", "tta", "wv",
    "spx", "mp2", "m3u", "m3u8", "pls"
  )

  /** Everything the player can open from the browser. */
  val MEDIA_EXTENSIONS: Set<String> = VIDEO_EXTENSIONS + AUDIO_EXTENSIONS

  /**
   * Checks if a file is a video based on extension
   */
  fun isVideoFile(file: File): Boolean {
    val extension = file.extension.lowercase(Locale.getDefault())
    return VIDEO_EXTENSIONS.contains(extension)
  }

  /**
   * Checks if a file is audio based on extension
   */
  fun isAudioFile(file: File): Boolean {
    val extension = file.extension.lowercase(Locale.getDefault())
    return AUDIO_EXTENSIONS.contains(extension)
  }

  /**
   * Checks if a file is something the player can play, video or audio.
   */
  fun isMediaFile(file: File): Boolean {
    val extension = file.extension.lowercase(Locale.getDefault())
    return MEDIA_EXTENSIONS.contains(extension)
  }

  /**
   * Checks if an extension represents a video file
   */
  fun isVideoExtension(extension: String): Boolean {
    return VIDEO_EXTENSIONS.contains(extension.lowercase(Locale.getDefault()))
  }

  /**
   * Gets MIME type from file extension
   */
  fun getMimeTypeFromExtension(extension: String): String =
    when (extension.lowercase(Locale.getDefault())) {
      "mp4" -> "video/mp4"
      "mkv" -> "video/x-matroska"
      "avi" -> "video/x-msvideo"
      "mov" -> "video/quicktime"
      "webm" -> "video/webm"
      "flv" -> "video/x-flv"
      "wmv" -> "video/x-ms-wmv"
      "m4v" -> "video/x-m4v"
      "3gp" -> "video/3gpp"
      "mpg", "mpeg" -> "video/mpeg"
      "mp3" -> "audio/mpeg"
      "flac" -> "audio/flac"
      "m4a" -> "audio/mp4"
      "m4b" -> "audio/mp4"
      "aac" -> "audio/aac"
      "opus" -> "audio/opus"
      "ogg", "oga" -> "audio/ogg"
      "wav" -> "audio/wav"
      "wma" -> "audio/x-ms-wma"
      "ape" -> "audio/x-ape"
      "mka" -> "audio/x-matroska"
      "ac3" -> "audio/ac3"
      "dts" -> "audio/vnd.dts"
      "aiff", "aif" -> "audio/x-aiff"
      "amr" -> "audio/amr"
      "tta" -> "audio/x-tta"
      "wv" -> "audio/x-wavpack"
      "m3u", "m3u8" -> "audio/x-mpegurl"
      "pls" -> "audio/x-scpls"
      else -> "video/*"
    }
}
