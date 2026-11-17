package io.leloush.fang.agent

import org.json.JSONArray
import org.json.JSONObject

/** One option of an [AskQuestion], as the `ask` request describes it. */
internal data class AskOption(val title: String, val description: String?)

/** One question of an [AskRequest], as the `ask` request describes it. */
internal data class AskQuestion(
  val question: String,
  val context: String?,
  val options: List<AskOption>,
  val allowMultiple: Boolean,
  val allowFreeform: Boolean,
)

/**
 * One `ask` request from an extension on Fang Harness's channel: a batch of [questions] the user answers in one card.
 * A message without an id or without questions is malformed and dropped before it reaches the chat.
 */
internal data class AskRequest(
  val id: String,
  val heading: String?,
  val questions: List<AskQuestion>,
  val allowComment: Boolean,
  val timeout: Long?,
) {
  companion object {
    /** Parses a channel `ask` message, or null when it isn't a request this app can show. */
    fun from(data: JSONObject): AskRequest? {
      val id = data.optString("id").takeIf { it.isNotEmpty() } ?: return null
      val array = data.optJSONArray("questions") ?: return null
      val questions = array.mapObjects(::question)
      if (questions.isEmpty()) return null
      return AskRequest(
        id = id,
        heading = data.textOrNull("heading"),
        questions = questions,
        allowComment = data.optBoolean("allowComment"),
        timeout = data.optLong("timeout").takeIf { it > 0 },
      )
    }

    private fun question(json: JSONObject): AskQuestion? {
      val text = json.optString("question").ifEmpty { return null }
      return AskQuestion(
        question = text,
        context = json.textOrNull("context"),
        options = json.optJSONArray("options").mapObjects(::option),
        allowMultiple = json.optBoolean("allowMultiple"),
        allowFreeform = json.optBoolean("allowFreeform"),
      )
    }

    /** Skips elements that aren't objects, so a malformed message is ignored instead of throwing in the reader. */
    private fun <T> JSONArray?.mapObjects(parse: (JSONObject) -> T?): List<T> =
      if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it)?.let(parse) }

    private fun option(json: JSONObject): AskOption? {
      val title = json.optString("title").ifEmpty { return null }
      return AskOption(title, json.textOrNull("description"))
    }

    private fun JSONObject.textOrNull(name: String): String? =
      if (isNull(name)) null else optString(name).takeIf { it.isNotEmpty() }
  }
}
