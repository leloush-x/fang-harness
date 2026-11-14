package io.leloush.fang.agent

import org.json.JSONArray
import org.json.JSONObject

/**
 * The user messages of the session's current branch, oldest first: each one's Pi entry id, the text as the chat shows
 * it, and the time Pi stamped it with. The branch is the chain of entries from the session's leaf up to its root, so
 * a message the user forked away from is not in it, and a message that a compaction folded into a summary is still
 * there.
 */
internal suspend fun AgentProcess.branchUserMessages(): List<BranchUserMessage> {
  val data = call("get_entries")
  val entries = data.optJSONArray("entries") ?: return emptyList()
  val byId = HashMap<String, JSONObject>()
  for (index in 0 until entries.length()) {
    val entry = entries.optJSONObject(index) ?: continue
    val id = entry.optString("id")
    if (id.isNotEmpty()) byId[id] = entry
  }
  val chain = ArrayDeque<String>()
  var id = data.optString("leafId")
  while (id.isNotEmpty()) {
    val entry = byId[id] ?: break
    chain.addFirst(id)
    id = entry.optString("parentId")
  }
  return chain.mapNotNull { entryId ->
    val entry = byId.getValue(entryId)
    if (entry.optString("type") != "message") return@mapNotNull null
    val message = entry.optJSONObject("message") ?: return@mapNotNull null
    if (message.optString("role") != "user") return@mapNotNull null
    val text = typedText(messageText(message))
    if (text.isEmpty()) null else BranchUserMessage(entryId, text, message.optLong("timestamp"))
  }
}

/** One user message of the session's branch: [entryId] names it to Pi, [text] is what the chat shows, [timestamp] when it was sent. */
internal data class BranchUserMessage(val entryId: String, val text: String, val timestamp: Long)

/**
 * Pi's entry id of the user message the chat shows as [item], or null when the chat's own messages don't name it.
 *
 * The chat shows the messages of the branch the session is on, and Pi keeps that branch's messages as the entries from
 * its leaf back to its root. Pi stamps each message with the time it was sent, which tells two messages the user sent
 * the same text apart. Without that stamp, the two lists are lined up from the newest end, where they agree even after
 * a compaction dropped the oldest messages from the chat, and the text tells them apart.
 */
internal suspend fun AgentProcess.userEntryId(item: TimelineItem.User): String? {
  val branch = branchUserMessages()
  if (branch.isEmpty()) return null
  val shown = timeline.state.value.items.filterIsInstance<TimelineItem.User>()
  return entryIdOf(branch, shown, item)
}

/** The entry id of the message the chat shows as [item] among [branch], or null when none of them is it. */
private fun entryIdOf(
  branch: List<BranchUserMessage>,
  shown: List<TimelineItem.User>,
  item: TimelineItem.User,
): String? {
  val stamped = branch.lastOrNull { it.timestamp == item.timestamp && it.text == item.text }
  if (stamped != null) return stamped.entryId
  val index = shown.indexOfLast { it.text == item.text }
  if (index < 0) return null
  val lined = branch.getOrNull(branch.size - (shown.size - index))
  if (lined != null && lined.text == item.text) return lined.entryId
  val same = branch.filter { it.text == item.text }
  val after = shown.drop(index + 1).count { it.text == item.text }
  return same.getOrNull(same.size - after - 1)?.entryId
}

/** The text of a Pi message, which is either a string or an array of blocks. */
private fun messageText(message: JSONObject): String = when (val content = message.opt("content")) {
  is JSONArray -> content.text()
  is String -> content
  else -> ""
}
