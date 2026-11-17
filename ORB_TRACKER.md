# Fang Harness — Orb / UI-UX overhaul tracker (worktree /root/fang-orb, branch feature/orb-ux)

Start: 0c5b1df (v0.1.27). Kit: /root/storage/shared/Download/fang-harness/rea-tech/ (replicate/SPEC.md, TECH.md, ASK_QUESTION.md, PASTE_AND_MENU.md, BLOBS.md, THEMES.md, shaders/*.agls, OpenCode.zip).

## Stage 0 — foundation — done, gate CLOSED (user gave permission 2026-11-17)
- [x] 8 raw AGSL shaders (7 extracted + Fang's cells reconstruction shader_07)
- [x] OrbShader.kt — RuntimeShader, child binding via `setInputShader`, uniforms, API-33 guard, remember helper
- [x] assets/licenses/opencode-agls.txt attribution (license unknown; user approved redistribution)
- [x] reviewed; compile sanity by inspection

## Stage 1 — orb identity — DONE (pending final CI integration)
- [x] FangOrb: moods, cyan/green, 3.8 s breath, dot grid, Fresnel, dither, glow; frame read inside Canvas only
- [x] API 29–32 fallback keeps SimmeringPot pulse
- [x] FangTheme → shader tints: blend fangColors.accent/dotSuccess into the kit cyan/green in `stateFor`
- [x] 36 SimmeringPot sites classified; working sites → orb, generic progress keeps pot
- [x] LocalOrbsFrozen: freeze orb + pot while a side panel covers the chat (provided in MainScreen)
- [x] Dynamic placing: ambient orbSize scale (Small/Medium/Large) + slow gaze drift; settings persisted in Preferences
- [x] Settings rows: orb size + float over chats (Appearance section)

## Stage 2 — chat states — partial (deliberate skips, see below)
- [x] Working glyph on tool rows, turn footer, status banners (calm ORB mood, no bounce)
- [x] Copy full message + Quote in reply on user message menu (Copy uses rememberCopyText; Quote prefixes `> `)
- [ ] Green completion morph / copy handoff — SKIPPED: working glyphs are in ToolRow/TurnFooter where there is no copy slot; assistant replies already have CopyButton. Doing a true morph means new animation + click plumbing in MessageItems; deferred under "nothing may break".
- [ ] Subagent capsules/popups — SKIPPED: all ui/subagents/* are empty object stubs; no card or navigation surface exists to attach to. Building one would mean inventing a screen.
- [ ] Message bubbles lift/rise + cached pulse — SKIPPED: the list is ChatStream's lazy column; animations need careful scroll/follow coordination and are not recoverable from string-level extraction.
- [ ] Link long-press Copy/Share/Open — SKIPPED: FangMarkdown uses a third-party markdown lib; a custom long-press link annotation risks breaking link rendering.

## Stage 3 — ask card fold — done
- [x] In-composer foldable/expandable card, folded bar like Todos/Background, no duplicated question UI
- [x] Answered summary with checked options; failed answer keeps card with Retry
- [x] Animated fold/expand, strings in resources, answers via channel only

## Stage 4 — paste + menu — in progress
- [ ] Pasted Text attachment card: Attachment.Text, threshold, paste→card, full-text dialog with Copy, deliver() includes full text (agent running)
- [x] Copy/quote menu family (new strings_chat_menu.xml)
- [x] Skipped OpenCode-only items (Ask OpenCode, report a problem, per-session themes)

## Verify / ship
- [x] Every stage pushed to debug, CI after each; all failures so far mechanical (imports, ktlint, wrong RuntimeShader API, unused constant, NewApi) and fixed with no suppressions
- [ ] CI 44032075863 (API-33 guard) result pending
- [ ] Integrate Stage 4 + ambient orb into ONE commit, push debug, CI green
- [ ] SECOND full review of every diff before any push to main
- [ ] Ask before main → release → APK size/SHA-256/signature verify
- [ ] Device check: user confirms orb renders and looks right (no emulator here)

## Coordination rules in force
- Disjoint file ownership; no agent commits; CI between stages; no suppressions/baselines; no literals in UI.
- Files owned during the latest parallel round: paste agent = Attachments/Composer/ChatController/strings_chat.xml/PastedTextDialog; menu agent = MessageItems/UserMessageMenu/ChatScreen/strings_chat_menu.xml; main agent = FangOrb/OrbShader/OrbAmbient/SimmeringPot/MainScreen.
