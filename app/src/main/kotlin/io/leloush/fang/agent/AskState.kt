package io.leloush.fang.agent

import io.leloush.fang.host.HostRejected
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** The answers the user sent for an [AskRequest], kept so the summary can show what was picked. */
internal data class AskAnswered(val request: AskRequest, val answers: JSONArray)

/**
 * The one `ask` request a bundled extension waits on. Held beside [AgentProcess] rather than in it: the process is
 * already at detekt's class-size limit, and the request is independent of Pi's RPC calls.
 */
internal class AskState(private val send: suspend (String, JSONObject) -> Unit) {
  private val requestState = MutableStateFlow<AskRequest?>(null)
  private val failedState = MutableStateFlow(false)
  private val answeredState = MutableStateFlow<AskAnswered?>(null)

  /** The request the chat shows, or null when no extension waits on the user's answers. */
  val request: StateFlow<AskRequest?> = requestState.asStateFlow()

  /** Whether the last answer failed to reach the extension; the request stays so it can be retried. */
  val failed: StateFlow<Boolean> = failedState.asStateFlow()

  /** The answered request, kept until dismissed so the chat can show what the user picked. */
  val answered: StateFlow<AskAnswered?> = answeredState.asStateFlow()

  /** Accepts a channel `ask` message when it is a request this app can show. */
  fun onMessage(data: JSONObject) {
    val parsed = AskRequest.from(data) ?: return
    answeredState.value = null
    failedState.value = false
    requestState.value = parsed
  }

  /** Sends the answers, then drops the card; a failed send keeps the card so the user can retry. */
  suspend fun sendAnswer(data: JSONObject) {
    val current = requestState.value ?: return
    failedState.value = false
    try {
      send(TOPIC, data)
    } catch (_: IOException) {
      // The request stays; the card offers Retry.
      failedState.value = true
      return
    } catch (_: HostRejected) {
      // The request stays; the card offers Retry.
      failedState.value = true
      return
    }
    // A timeout or Dismiss only cancels: like before folding, the card goes away with no answered summary.
    answeredState.value = if (data.optBoolean("cancelled")) {
      null
    } else {
      AskAnswered(current, data.optJSONArray("answers") ?: JSONArray())
    }
    requestState.value = null
  }

  /** Called when the channel dies: the extension and its question are gone with it. */
  fun clear() {
    requestState.value = null
    answeredState.value = null
    failedState.value = false
  }

  /** Hides the answered summary; the answer has already been sent, so this never resends it. */
  fun dismissAnswered() {
    answeredState.value = null
  }

  private companion object {
    const val TOPIC = "ask"
  }
}
