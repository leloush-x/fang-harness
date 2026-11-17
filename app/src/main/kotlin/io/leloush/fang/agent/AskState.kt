package io.leloush.fang.agent

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * The one `ask` request a bundled extension waits on. Held beside [AgentProcess] rather than in it: the process is
 * already at detekt's class-size limit, and the request is independent of Pi's RPC calls.
 */
internal class AskState(
  private val send: suspend (String, JSONObject) -> Unit,
) {
  private val requestState = MutableStateFlow<AskRequest?>(null)

  /** The request the chat shows, or null when no extension waits on the user's answers. */
  val request: StateFlow<AskRequest?> = requestState.asStateFlow()

  /** Accepts a channel `ask` message when it is a request this app can show. */
  fun onMessage(data: JSONObject) {
    AskRequest.from(data)?.let { requestState.value = it }
  }

  /** Sends the answers and drops the card, so a late answer cannot be sent twice. */
  suspend fun sendAnswer(data: JSONObject) {
    requestState.value = null
    send(TOPIC, data)
  }

  /** Called when the channel dies: the extension and its question are gone with it. */
  fun clear() {
    requestState.value = null
  }

  private companion object {
    const val TOPIC = "ask"
  }
}
