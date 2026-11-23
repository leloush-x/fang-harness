package io.leloush.fang.ui.catchup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * One session in the Catch Up deck. Kept separate from Fang's chat models so the deck is pure UI:
 * the app wires the callbacks to its own stores.
 */
data class CatchUpSession(
  val id: String,
  val title: String,
  val subtitle: String? = null,
  val needsInput: Boolean = false,
)

/** What happened to a card, kept so Undo can call the inverse callback. */
internal enum class CatchUpDisposition { MarkRead, KeepUnread }

internal data class CatchUpHandled(val session: CatchUpSession, val disposition: CatchUpDisposition)

/**
 * The deck's queue and Undo history. [sessions] is snapshotted once: from then on the deck owns the
 * queue, and the callbacks are the only channel back to the app (no persistence lives here).
 */
internal class CatchUpDeckState(
  sessions: List<CatchUpSession>,
  private val onMarkRead: (CatchUpSession) -> Unit,
  private val onKeepUnread: (CatchUpSession) -> Unit,
  private val onMarkAll: () -> Unit,
) {
  val queue = mutableStateListOf<CatchUpSession>().apply { addAll(sessions) }
  var handling by mutableStateOf<CatchUpHandled?>(null)
    private set
  var handled by mutableStateOf(emptyList<CatchUpHandled>())
    private set

  /** True once a card is gone and no flight is running. */
  val canUndo: Boolean get() = handling == null && handled.isNotEmpty()

  /**
   * Starts the front card's exit flight and fires its callback. Returns the disposition so the UI
   * can fly the card the right way; a null means there was no card to dismiss.
   */
  fun dismiss(disposition: CatchUpDisposition): CatchUpDisposition? {
    if (handling != null) return null
    val session = queue.firstOrNull() ?: return null
    val record = CatchUpHandled(session, disposition)
    handling = record
    notify(record, inverse = false)
    return disposition
  }

  /** Commits the in-flight card, recording it for Undo. */
  fun settle() {
    val record = handling ?: return
    val index = queue.indexOfFirst { it.id == record.session.id }
    if (index >= 0) queue.removeAt(index)
    handled = handled + record
    handling = null
  }

  /** Puts the last handled card back at the front and fires the inverse callback. */
  fun undo(): Boolean {
    if (handling != null) return false
    val record = handled.lastOrNull() ?: return false
    handled = handled.dropLast(1)
    if (queue.none { it.id == record.session.id }) queue.add(0, record.session)
    notify(record, inverse = true)
    return true
  }

  /** Hides every remaining card and records them all for Undo. */
  fun markAll(): Boolean {
    if (handling != null || queue.isEmpty()) return false
    val records = queue.map { CatchUpHandled(it, CatchUpDisposition.MarkRead) }
    handled = handled + records
    queue.clear()
    onMarkAll()
    return true
  }

  /** A read card is undone by keeping it unread, and the other way around. */
  private fun notify(record: CatchUpHandled, inverse: Boolean) {
    val read = record.disposition == CatchUpDisposition.MarkRead
    if (read != inverse) onMarkRead(record.session) else onKeepUnread(record.session)
  }
}
