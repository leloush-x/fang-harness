package io.leloush.fang.ui.components

/**
 * Closes half-written inline markers at the tail of a still-streaming reply, so `**bold`, `` `code `` and
 * `[label](https://…` don't flash as literal text before the closing delimiter lands. A trailing run with
 * no content after it is hidden instead. Only the streaming caller (the one without a parse [MarkdownCache])
 * uses this; finished text is parsed exactly as written.
 */
internal fun healStreamingTail(text: String): String {
  val trimmed = text.trimEnd()
  if (trimmed.isEmpty()) return text
  val suffix = text.substring(trimmed.length)
  val label = partialLink(trimmed)
  if (label != null) return label + suffix

  val state = ScanState(cut = trimmed.length)
  var index = 0
  while (index < trimmed.length) {
    val char = trimmed[index]
    if (char == '`' || char == '*' || char == '_' || char == '~') {
      index = scanDelimiter(trimmed, index, state)
    } else {
      index++
    }
  }
  val open = (state.emphasis.reversed() + listOfNotNull(state.code)).filter { it.start < state.cut }
  val closing = buildString { open.forEach { run -> repeat(run.length) { append(run.char) } } }
  val healed = trimmed.substring(0, state.cut) + closing + suffix
  return if (healed == text) text else healed
}

/** Mutable state accumulated while scanning inline delimiters left to right. */
private data class ScanState(
  var code: MarkdownRun? = null,
  val emphasis: ArrayDeque<MarkdownRun> = ArrayDeque(),
  var cut: Int = 0,
)

/** Processes the delimiter run at [index] and returns the index just past it. */
private fun scanDelimiter(text: String, index: Int, state: ScanState): Int {
  val char = text[index]
  var length = 1
  while (index + length < text.length && text[index + length] == char) length++
  val run = MarkdownRun(char, length, index, index + length)
  val bare = run.after == text.length
  if (char == '`') {
    val currentCode = state.code
    when {
      currentCode != null && currentCode.length == length -> state.code = null
      bare -> state.cut = run.start
      else -> state.code = run
    }
  } else {
    val closes = index > 0 && !text[index - 1].isWhitespace()
    when {
      closes && state.emphasis.lastOrNull()?.char == char -> {
        val open = state.emphasis.removeLast()
        if (open.length > length) state.emphasis.addLast(open.copy(length = open.length - length))
      }

      bare -> state.cut = run.start

      !text[run.after].isWhitespace() -> state.emphasis.addLast(run)
    }
  }
  return index + length
}

/** The text with a link's still-arriving destination closed, or null when no link is open at the end. */
private fun partialLink(text: String): String? {
  val open = text.lastIndexOf('[')
  if (open < 0 || (open > 0 && text[open - 1] == '\\')) return null
  val close = text.indexOf(']', open + 1)
  if (close < 0 || text.getOrNull(close + 1) != '(') return null
  if (')' in text.substring(close + 2)) return null
  // Keep the prose and the partial destination, closing the link so it renders while it streams.
  return "$text)"
}

/** One run of an inline delimiter: its character, length, and offsets before and after it. */
private data class MarkdownRun(val char: Char, val length: Int, val start: Int, val after: Int)
