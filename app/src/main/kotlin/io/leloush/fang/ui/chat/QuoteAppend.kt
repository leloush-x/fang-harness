package io.leloush.fang.ui.chat

/** Adds a quoted copy of a user message to the draft, keeping anything already typed. */
internal fun appendQuote(controller: ChatController, text: String) {
  val quoted = text.lines().joinToString("\n") { "> $it" }
  val current = controller.draft.text.toString()
  controller.setDraftText(if (current.isBlank()) quoted else "$current\n\n$quoted")
}
