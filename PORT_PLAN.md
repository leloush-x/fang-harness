# Port plan — selected OpenCode features (11 bundles)

## Stage 1 (running): disjoint workers
- W1 Themes + live preview (theme/*, SettingsScreen appearance, strings_oc_themes)
- W2 Ask dock upgrade (AskState/AskRequest/AskUserCard/AskAnsweredSummary/WidgetTabs, strings_ask_dock)
- W3 Markdown upgrade (FangMarkdown/CodeBox/MarkdownTable/CodeHighlighting/MarkdownCache + new, strings_markdown_oc)
- W4 Tool rows + bubbles (MessageItems/ToolPresentation/ChatClipboard, strings_tool_bubbles)
- (W5 Composer polish — next batch: Composer/ComposerChips/ComposerEdges + autocomplete)

## Stage 2 (running in parallel with Stage-1 review)
- Composer polish worker — running
- Subagent stack worker — running
- Notifications worker — running
- Bubbles selection (reply-wide selection) — needs ChatStream owner, deferred
- Stage-1 coordination reviewer — running

## Stage 3
- Catch Up (swipe deck)
- Media viewer + files (fullscreen zoom, draw-on-image, file viewers)
- Orb/glyph polish (glyph cache, shell glyph, rolling numbers, divider glow)

## Verify: 2 reviews per worker + CI gate per stage. Ask before main/release.
Voice/call, shell output sheet, devtools/What's New, drawer polish, link previews stack = NOT selected.
