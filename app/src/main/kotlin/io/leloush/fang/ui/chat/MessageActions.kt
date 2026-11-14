package io.leloush.fang.ui.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.leloush.fang.R
import io.leloush.fang.agent.RpcError
import io.leloush.fang.agent.TimelineItem
import kotlinx.coroutines.launch

/**
 * Edit / delete / resend actions for a sent user message. Kept out of [ChatController] so that class stays under the
 * function-count limit; it reaches the controller's public helpers ([ChatController.launchRpc], [ChatController.show],
 * [ChatController.setDraftText], [ChatController.process]).
 */
internal class MessageActions(private val controller: ChatController) {
  var pendingDelete by mutableStateOf<TimelineItem.User?>(null)
    private set

  fun requestDelete(item: TimelineItem.User) { pendingDelete = item }

  fun cancelDelete() { pendingDelete = null }

  fun confirmDelete() {
    val item = pendingDelete ?: return
    pendingDelete = null
    delete(item)
  }

  fun edit(item: TimelineItem.User) {
    val index = userMessageIndex(item)
    controller.launchRpc {
      val entryId = controller.process.userEntryId(index, item.text)
      if (entryId == null) { controller.show(R.string.chat_message_action_failed); return@launchRpc }
      val text = controller.process.fork(entryId)
      if (text != null) {
        controller.setDraftText(text)
        controller.show(R.string.chat_message_edited)
      } else {
        controller.show(R.string.chat_message_action_failed)
      }
    }
  }

  private fun delete(item: TimelineItem.User) {
    val index = userMessageIndex(item)
    controller.launchRpc {
      val entryId = controller.process.userEntryId(index, item.text)
      if (entryId == null) { controller.show(R.string.chat_message_action_failed); return@launchRpc }
      val result = controller.process.fork(entryId)
      if (result == null) controller.show(R.string.chat_message_action_failed) else controller.show(R.string.chat_message_deleted)
    }
  }

  fun resend(item: TimelineItem.User) {
    if (controller.process.state.value.starting) { controller.show(R.string.chat_message_action_failed); return }
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

  private fun userMessageIndex(item: TimelineItem.User): Int =
    controller.process.timeline.state.value.items.filterIsInstance<TimelineItem.User>().indexOf(item)
}
