# OpenCode Android — COMPLETE feature inventory (analysis only)

Source: /tmp/opencode-android-src (github.com/anomalyco/opencode-android). 5 parallel area agents. Voice/call = names only (out of scope per user).

Counts: Session 122 | Transcript 180 | Composer ~100 | Drawer/Home 117 | Settings/Media/System/Devtools ~200 | What's New catalog 182 entries.

## A. ORBS / GLYPHS / MOTION (session + agentactivity)
- Working-line activity orb: 10 states (idle/startup/thinking/longWork/tool/streaming/waiting/handoff/done/failed), shimmer/condense/comet/helix/kick/spiral/chase/slide/ring/cross designs; 7x7 dot disc + phosphor + flash — AgentActivitySource.kt:30, ActivityGyro.kt:654
- Orb finale → copy handoff: 0.55s + 0.34s, blur/shrink into copy button, reduce-motion crossfade — AgentActivitySource.kt:206, SessionTranscriptView.kt:940
- Glyph model cache across remounts (LRU-64) — GlyphModels.kt:17
- Debug glyph lab — AgentActivitySource.kt:667
- Shell >_ glyph with output-rate laps/coast — ShellPromptGlyph.kt:21, ShellGlyph.kt:47
- Title breathing while syncing/auto-rename — header/TitleBreathing.kt:205
- Rolling numbers on counts — BackgroundWorkPanel.kt:343
- New Messages divider glow — rows/TranscriptRule.kt:93
- Arriving text fade / reveal tunables — markdown/ui/ArrivingText.kt:95, TranscriptTunables.kt:18

## B. SUBAGENTS / BACKGROUND WORK
- Group capsule with leading glyph + count, morphing panel, drag-to-capsule (56dp/550dp/s) — BackgroundWorkPanel.kt:277, BackgroundWorkMotion.kt:195
- Background rows (subagents/scripts/shells) + per-row glyph + Stop — BackgroundWorkPanel.kt:951
- Subagent push/pop page stack, predictive back, nested, draft preservation — SessionPageStack.kt:54, HomeFeature.kt:1299
- Every-running-subagent count incl. nested + >100 — HomeFeatureDrawerState.kt
- View receipt / Catch Up handshake — HomeFeatureTranscripts.kt:230
- Move to Background on steer (auto) — SessionFeatureSend.kt:609
- Shell output sheet (tail, follow, Load Earlier, Jump, Stop confirm, Share) — ShellOutputFeature.kt:27

## C. COMPOSER / ASK
- Growing multiline field, caret restore, keyboard swipe down/up, double-tap word select — ComposerTextInput.kt:538,199,213,966
- Send/stop one slot with glyph morph, steer/queue, ordered outbox, Retry/Edit/Delete failed sends — ComposerFeature.kt:67, PromptOutbox.kt:65
- Model chip + thinking arc, One Picker (favorites/recents/search/detents/level scrub) — modelpicker/OnePickerChip.kt:67
- + attachment menu (camera/photos/files/location/paste), image re-encode, file chips w/ progress, video poster — AttachmentPicker.kt:232
- Pasted Text card + viewer (wrap toggle, Copy/Share) — PastedTextViews.kt:83,173
- Autocomplete @ / / (skills/agents/commands/files/sessions, fuzzy, chips, keyboard bridge) — AutocompleteFeature.kt:44
- Ask/permission dock: grows from message box, paging, auto-advance, multiselect, Yes/No, inline validation, own-words answer, fold-to-bar (0.3/700dp/s), queue menu, review sheet, diff preview, Retry, panel fit with keyboard, prewarm — RequestDockView.kt, RequestAnswer.kt, RequestCollapse.kt:43
- Dictation: mic tap/hold, slide-up lock / left cancel, live transcription, audio ownership, + morphs to cancel — MicPressGesture.kt:180, DictationFeature.kt:25
- Quote replies / steering — ComposerQuote.kt:7

## D. TRANSCRIPT
- User bubbles: lift on menu, long fold (12→8 lines), reveal glow AGSL shader, selection toolbar, failed-send menu, session mentions — TranscriptUserBubble.kt:112, AgentMessageReveal.kt:103
- Markdown: own parser (GFM, entities, tables, task lists, code w/ incremental highlight, mermaid, math jlatexmath, PR/file cards, streaming tail heal) — OpenCodeMarkdownBlock.kt, InlineMarkdownParser.kt
- Code copy button (2s check), table scroll, blockquote/divider/headings — CodeCopyButton.kt:36
- Links: hold menu, favicons, chip/address modes, link preview stack (3 + "more"), rich GitHub/X/video previews, arrival motion, image link previews — LinkPreviewViews.kt:110
- Tool rows: Used-N group, consecutive-read merge, glyphs, output console/fold, error/decline, file/image results, subagent card, running pulse, compaction rules, model-switch rule — ToolRows.kt:159
- Grouping/corners: user groups, tool groups, tight 6dp corners, stable IDs, disclosure persistence — TranscriptEntry.kt:193, UserMessageRun.kt:38
- Reply-wide selection (handles, magnifier, autoscroll, Copy/Quote/Select All), drag payloads — selection/TranscriptReplySelection.kt:48
- Loading/empty: bubble pulse, delayed spinner (400ms), history error+retry, restore hide — TranscriptListRows.kt:139
- Scroll: reversed list, jump-to-latest, follow/browse, row-growth hold, earlier paging, borrowed drag — TranscriptList.kt:131
- Answered question rows with ✓ picks — InlineRows.kt:271

## E. SESSION CHROME / SHEETS
- Working line + retry countdown + Move to Background + connection pill + title breathing + context gauge ring — SessionTranscriptView.kt:846, SessionContextRing.kt:28
- Session sheet (tiles: Pin/Unread/Mute/Compact/Fork/Export, model/context/cost rows) — SessionSheetView.kt:220
- Model sheet / One Picker, project sheet + folder browser, queued messages sheet — SessionView.kt, DraftProjects.kt:412
- Header: backdrop gradient, title capsule, drawer-label fade, subagent back circle, header menu (Pin/Rename/Copy ID/Read/Mute/Compact/Fork/Export/Media/Info/Report/Delete) — SessionViewHeader.kt:106,490
- Export Markdown/JSON, fork opens copy, revert-to-here, staged undo/redo — SessionExport.kt:30, SessionFeatureUndo.kt:64
- Session media page (Media/Files/Links, gallery, gallery fly) — SessionMedia.kt:42
- In-app browser page: back/forward/address/share/open-in-browser/ask, link policy — BrowserView.kt:112

## F. DRAWER / HOME / NAV
- Session list: pinned+recents, hot-first, icons, indicators, TalkBack actions, pin reorder drag, mark read/unread, delete, search pill + server paging, ghost rows — DrawerListModel.kt:157
- Wide-window sidebar vs modal drawer, multi-window — HomeView.kt:115
- Catch Up deck: swipes, undo, mark-all, finish celebration, inert preview — CatchUpFeature.kt:31
- Badges: app icon count, drawer attention dot, In-App Notices capsule — HomeFeatureNotifications.kt:54
- Connection: event stream backoff, quiet grace, last-known activity, retry, server switcher, pairing — HomeConnectionFeature.kt:405
- Share sheet targets, received shares, direct share, deep links — ShareActivity.kt:43
- Drawer gestures: directional pan, predictive back, time-reveal swipe, keyboard hold — motion/DirectionalPan.kt:26

## G. SETTINGS / THEMES / NOTIFICATIONS / DEVTOOLS
- Themes: 7 curated, live transcript preview, crossfade, swatches, mode override, language screen — AppearanceSettingsView.kt:157, AppTheme.kt:24
- Motion playground, live tuning panel (drag/throw/presets/randomize/copy Kotlin/secret gestures), perf HUD, layout bounds — devtools/tuning/, DevToolsView.kt:216
- What's New: 182 shipped entries, grouped by day, animated LED symbol, "Try it" actions — WhatsNewView.kt:114
- Notifications: permission, plugin check, in-app notices picker, encrypted previews (P-256/AES-GCM), channels, app badge, Live Update — NotificationSettingsView.kt:33
- Model visibility (hide unused), sessions settings, sound effects, experimental toggles, server details/pairing — ModelVisibilitySettings.kt:52
- Fullscreen media viewer (zoom/drag-back/pinch/PiP/save/share/draw-on-image), file viewers (md/code/pdf/audio), link previews, mermaid — feature/media
- App routing: splash bloom, pairing, server list/form, welcome preview, migration — AppView.kt:155

## H. VOICE/CALL — OUT OF SCOPE (names only)
Voice calls / call tray / call history / Raise to Talk / call sounds / Wisp orb tunables+presets / floating orb / dictation Live Activity / voice server / voice screenshots / Share Screen with Agents / switch sessions by voice / dictation / etc. (full name list in the agent reports).

## NOT IN EXACT SOURCE (do not claim)
No dedicated thinking-reasoning block renderer, no inline code-diff view, no font picker.
