package io.leloush.fang.ui.chat

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.graphics.ImageBitmap
import io.leloush.fang.agent.ImagePart
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A paste at or above this many characters becomes a Pasted Text card; shorter text stays in the field. The exact
 * upstream threshold was not recoverable, so this is a sensible one: short snippets read better inline, while a log
 * or code block is easier to inspect as a card and read in full there.
 */
internal const val PASTED_TEXT_MIN_LENGTH = 300

/** Something picked for the next prompt. */
internal sealed interface Attachment {
  /** Sent inside Pi's `prompt`: the JPEG and its composer-tray thumbnail. */
  class Image(val part: ImagePart, val thumbnail: ImageBitmap) : Attachment

  /** Copied into Wolfi OS on send; the prompt lists its path for Pi's tools. */
  class File(val name: String, val bytes: ByteArray) : Attachment

  /** A long paste, shown as a card; the whole text is sent to Pi with the prompt. */
  class Text(val text: String) : Attachment
}

/** The picked file would push the message past [ChatFiles.MAX_TOTAL_BYTES]. */
internal class AttachmentsTooLarge : IOException()

internal object ChatFiles {
  /** All files of one message travel in a single host request line, which the host caps at 16 MiB. */
  const val MAX_TOTAL_BYTES = 10L * 1024 * 1024

  private const val BUFFER_BYTES = 64 * 1024

  /**
   * Reads a picked document right away: the picker's read grant does not outlive the activity.
   * Throws [AttachmentsTooLarge] past [budget] bytes; null when the provider refuses access.
   */
  suspend fun attach(context: Context, uri: Uri, budget: Long): Attachment.File? = withContext(Dispatchers.IO) {
    try {
      val resolver = context.contentResolver
      val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (column >= 0 && cursor.moveToFirst()) cursor.getString(column) else null
      }
      val bytes = resolver.openInputStream(uri)?.use { input ->
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_BYTES)
        while (true) {
          val read = input.read(buffer)
          if (read < 0) break
          if (out.size() + read > budget) throw AttachmentsTooLarge()
          out.write(buffer, 0, read)
        }
        out.toByteArray()
      } ?: return@withContext null
      Attachment.File(name?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment ?: "file", bytes)
    } catch (error: AttachmentsTooLarge) {
      throw error
    } catch (_: IOException) {
      null
    } catch (_: SecurityException) {
      null
    }
  }
}
