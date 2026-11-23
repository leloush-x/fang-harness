package io.leloush.fang.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownCodeFence
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.MarkdownAnnotator
import com.mikepenz.markdown.model.MarkdownTypography
import com.mikepenz.markdown.model.State
import com.mikepenz.markdown.model.markdownAnimations
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.model.markdownDimens
import com.mikepenz.markdown.model.markdownPadding
import com.mikepenz.markdown.utils.getUnescapedTextInNode
import com.mikepenz.markdown.utils.resolveImageAlt
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors
import kotlinx.coroutines.flow.conflate
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes

/**
 * Assistant markdown styled to Paseo's message tokens: 15/21sp body, bordered horizontally scrolling code with a copy
 * button, checkable task items, mermaid fences as source cards, and tinted math and file paths. With a [cache], markdown
 * it has parsed before shows from the first frame, and each parse of it is kept there.
 */
@Composable
fun FangMarkdown(markdown: String, modifier: Modifier = Modifier, cache: MarkdownCache? = null) {
  val colors = fangColors
  val body = TextStyle(
    color = colors.foreground,
    fontSize = FangTokens.FontSize.content,
    lineHeight = FangTokens.LineHeight.markdown,
  )
  // CodeText is Fang Harness's one code style, so the library's code typography is ignored here.
  val codeBlock: @Composable (String, String?, TextStyle) -> Unit = { text, language, _ ->
    CodeBox(modifier = Modifier.padding(vertical = FangTokens.Spacing.s6), copyText = text) { CodeText(text, language) }
  }
  val state = rememberParsedMarkdown(markdown, cache)
  SelectionContainer(modifier = modifier) {
    Markdown(
      state = state,
      modifier = Modifier.fillMaxWidth(),
      colors =
        markdownColor(
          text = colors.foreground,
          codeBackground = colors.surface2,
          inlineCodeBackground = colors.surface2,
          dividerColor = colors.border,
        ),
      typography = typography(body),
      padding = markdownPadding(block = FangTokens.Spacing.s6, codeBlock = PaddingValues(0.dp)),
      dimens = markdownDimens(codeBackgroundCornerSize = FangTokens.Radius.md),
      components =
        markdownComponents(
          codeFence = { FangCodeFence(it, codeBlock) },
          codeBlock = { MarkdownCodeBlock(it.content, it.node, style = it.typography.code, block = codeBlock) },
          table = { MarkdownTable(it.content, it.node, it.typography.table) },
          checkbox = { FangMarkdownCheckBox(it.content, it.node) },
        ),
      // The library animates each text block's size by default, which relayouts every frame a streamed
      // block grows and leaves the chat's end moving after the text has landed.
      animations = markdownAnimations(animateTextSize = { this }),
      annotator = fangAnnotator(),
    )
  }
}

/**
 * [markdown] parsed off the main thread, one parse at a time with the latest text. Until the current text's parse
 * lands the last result stays, so a streamed reply doesn't collapse to the empty loading box between deltas. A text
 * [cache] holds needs no parse, and each result is kept in [cache]. A reply without a cache is still streaming, so its
 * tail is healed before parsing; finished text is never changed.
 */
@Composable
private fun rememberParsedMarkdown(markdown: String, cache: MarkdownCache?): State {
  var parsed by remember { mutableStateOf<State>(cache?.get(markdown) ?: State.Loading()) }
  val latest by rememberUpdatedState(markdown to cache)
  LaunchedEffect(Unit) {
    // Conflated, so text that changes faster than it parses skips to the latest instead of queueing every delta.
    snapshotFlow { latest }.conflate().collect { (text, latestCache) ->
      val shown = parsed
      val source = if (latestCache == null) healStreamingTail(text) else text
      val next = when {
        shown is State.Success && shown.content == source -> shown
        else -> latestCache?.get(source) ?: parseOffMain(source)
      }
      parsed = next
      latestCache?.put(source, next)
    }
  }
  return parsed
}

/**
 * Fang Harness has no image loader, so an image shows as Paseo shows one that failed to load: its alt text, small and
 * muted on surface2, or nothing without alt. Math keeps its source but drops the `$` wrappers and reads as code. Paths
 * mentioned in prose are tinted so they stand out from the surrounding text.
 */
@Composable
private fun fangAnnotator(): MarkdownAnnotator {
  val colors = fangColors
  val alt = SpanStyle(color = colors.foregroundMuted, background = colors.surface2, fontSize = IMAGE_ALT_FONT_SIZE)
  val math = SpanStyle(color = colors.accent, fontFamily = FontFamily.Monospace)
  val path = SpanStyle(color = colors.accent, fontFamily = FontFamily.Monospace)
  return remember(alt, math, path) {
    markdownAnnotator { content, node ->
      when (node.type) {
        MarkdownElementTypes.IMAGE -> {
          val imageAlt = node.resolveImageAlt(content)
          if (!imageAlt.isNullOrBlank()) withStyle(alt) { append(imageAlt) }
          true
        }

        GFMElementTypes.INLINE_MATH, GFMElementTypes.BLOCK_MATH -> {
          val raw = node.getTextInNode(content)
          val wrapper = if (node.type == GFMElementTypes.BLOCK_MATH) 2 else 1
          val source = if (raw.length > wrapper * 2) raw.substring(wrapper, raw.length - wrapper).trim() else raw
          withStyle(math) { append(source) }
          true
        }

        MarkdownTokenTypes.TEXT -> {
          appendStyledPaths(node.getUnescapedTextInNode(content), path)
          true
        }

        else -> false
      }
    }
  }
}

/** A mermaid fence reads as a labelled card; every other fence goes to Fang's code block. */
@Composable
private fun FangCodeFence(model: MarkdownComponentModel, codeBlock: @Composable (String, String?, TextStyle) -> Unit) {
  val language = model.node.findChildOfType(
    MarkdownTokenTypes.FENCE_LANG,
  )?.getTextInNode(model.content)?.toString()?.trim()
  if (language?.equals(MERMAID, ignoreCase = true) == true) {
    val source = fenceSource(model.content, model.node)
    if (source != null) {
      MermaidCard(source)
      return
    }
  }
  MarkdownCodeFence(model.content, model.node, style = model.typography.code, block = codeBlock)
}

/** The body of a fence, between its opening line and its closing marker. */
private fun fenceSource(content: String, node: ASTNode): String? {
  if (node.children.size < 3) return null
  val start = node.children[2].startOffset
  val minimum = if (node.children.size > 3) 3 else 2
  val end = node.children[(node.children.size - 2).coerceAtLeast(minimum)].endOffset
  return content.subSequence(start, end).toString().replaceIndent()
}

/** [text] as written, with path-like words given [style] and everything else kept plain. */
private fun AnnotatedString.Builder.appendStyledPaths(text: String, style: SpanStyle) {
  var cursor = 0
  var index = 0
  while (index < text.length) {
    if (!isPathChar(text[index])) {
      index++
      continue
    }
    var end = index
    while (end < text.length && isPathChar(text[end])) end++
    val token = text.substring(index, end)
    // shortcut: paths are tinted only; a local path has no scheme the app's URI handler can open.
    if (token.contains('/') && looksLikeFile(token)) {
      append(text, cursor, index)
      withStyle(style) { append(token) }
      cursor = end
    }
    index = end
  }
  append(text, cursor, text.length)
}

private fun isPathChar(char: Char): Boolean = char.isLetterOrDigit() || char in "/._-~"

/** A path whose last segment has a short file extension, such as `app/src/Main.kt`. */
private fun looksLikeFile(token: String): Boolean {
  val name = token.substringAfterLast('/')
  val dot = name.lastIndexOf('.')
  return dot > 0 && dot < name.length - 1 && name.length - dot <= MAX_EXTENSION && token.length <= MAX_PATH_LENGTH
}

private val IMAGE_ALT_FONT_SIZE = 10.sp

private const val MERMAID = "mermaid"

private const val MAX_EXTENSION = 6

private const val MAX_PATH_LENGTH = 200

@Composable
private fun typography(body: TextStyle): MarkdownTypography {
  val colors = fangColors
  val code = TextStyle(
    color = colors.foreground,
    fontFamily = FontFamily.Monospace,
    fontSize = FangTokens.FontSize.code,
    lineHeight = FangTokens.LineHeight.code,
  )
  return markdownTypography(
    h1 = body.copy(fontSize = FangTokens.FontSize.xxl, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    h2 = body.copy(fontSize = FangTokens.FontSize.xl, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    h3 = body.copy(fontSize = FangTokens.FontSize.lg, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    h4 = body.copy(fontWeight = FontWeight.SemiBold),
    h5 = body.copy(fontWeight = FontWeight.SemiBold),
    h6 = body.copy(fontWeight = FontWeight.SemiBold),
    text = body,
    code = code,
    inlineCode = code.copy(lineHeight = FangTokens.LineHeight.markdown),
    quote = body.copy(color = colors.foregroundMuted),
    paragraph = body,
    ordered = body,
    bullet = body,
    list = body,
    textLink = TextLinkStyles(style = SpanStyle(color = colors.accent, textDecoration = TextDecoration.Underline)),
    // The front matter table's cell type, a size under the body so more of a row fits across.
    table = body.copy(fontSize = FangTokens.FontSize.base, lineHeight = 20.sp),
    alertTitle = body.copy(fontWeight = FontWeight.SemiBold),
  )
}
