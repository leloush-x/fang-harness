package io.leloush.fang.ui.media

/**
 * One page of [OcMediaViewer]. The payloads are deliberately opaque: the viewer presents what it is
 * given and never reads files or fetches anything itself.
 */
sealed interface OcMediaItem {
  /** [data] is an Android `Bitmap` or a Compose `ImageBitmap`; anything else shows a placeholder. */
  data class Image(val data: Any) : OcMediaItem

  /** A document whose [content] is shown as markdown or as source text. */
  data class Text(val title: String, val content: String) : OcMediaItem

  /** A file placeholder: [bytes] carries text when present, otherwise [uri] is shown. */
  data class File(val name: String, val bytes: ByteArray? = null, val uri: String? = null) : OcMediaItem
}
