package io.leloush.fang.ui.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.leloush.fang.R
import io.leloush.fang.agent.RpcError
import io.leloush.fang.agent.TimelineItem
import io.leloush.fang.agent.fork
import io.leloush.fang.agent.userEntryId
import java.io.IOException
import kotlinx.coroutines.launch

/**
 * Edit / delete / resend actions for a sent user message. Kept out of [ChatController] so that class stays under the
 * function-count limit; it reaches the controller's public helpers ([ChatController.launchRpc], [ChatController.show],
 * [ChatController.setDraftText], [ChatController.process]).
 *
 * Pi has no command to change a message that was sent, so editing and deleting fork the chat: the session is cut
 * before that message and continues in a new one, which is what `/fork` does as well.
 */
internal class MessageActions(private val controller: ChatController) {
  /** The message whose deletion the user is asked to confirm. */
  var pendingDelete by mutableStateOf<TimelineItem.User?>(null)
    private set

  fun requestDelete(item: TimelineItem.User) {
    pendingDelete = item
  }

  fun cancelDelete() {
    pendingDelete = null
  }

  fun confirmDelete() {
    val item = pendingDelete ?: return
    pendingDelete = null
    delete(item)
  }

  /** Forks the chat to before [item] and puts its text in the composer, to edit it and send it again. */
  fun edit(item: TimelineItem.User) = controller.launchRpc {
    val entryId = entryIdOf(item)
    if (entryId == null) {
      controller.show(R.string.chat_message_action_failed)
      return@launchRpc
    }
    val text = controller.process.fork(entryId)
    if (text == null) {
      controller.show(R.string.chat_message_action_failed)
    } else {
      controller.setDraftText(text)
      controller.show(R.string.chat_message_edited)
    }
  }

  /** Forks the chat to before [item], which drops it and everything after it. */
  private fun delete(item: TimelineItem.User) = controller.launchRpc {
    val entryId = entryIdOf(item)
    if (entryId == null) {
      controller.show(R.string.chat_message_action_failed)
      return@launchRpc
    }
    val text = controller.process.fork(entryId)
    if (text ==
      null
    ) {
      controller.show(R.string.chat_message_action_failed)
    } else {
      controller.show(R.string.chat_message_deleted)
    }
  }

  /** Sends [item]'s text and images again, steering if a run is in progress. */
  fun resend(item: TimelineItem.User) {
    if (controller.process.state.value.starting) {
      controller.show(R.string.chat_message_action_failed)
      return
    }
    controller.scope.launch {
      try {
        if (controller.process.busy.value) {
          controller.process.steer(item.text, item.images)
        } else {
          controller.process.prompt(item.text, item.images)
        }
        controller.show(R.string.chat_message_resent)
      } catch (error: RpcError) {
        controller.show(error.message)
      } catch (error: IOException) {
        controller.show(error.message.orEmpty())
      }
    }
  }

  /**
   * Pi's entry id of [item], or null when the chat's own messages don't name it. The chat shows the messages of the
   * branch the session is on, and Pi keeps that branch's messages as the entries from its leaf back to its root.
   */
  private suspend fun entryIdOf(item: TimelineItem.User): String? = controller.process.userEntryId(item)
}
