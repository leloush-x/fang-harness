# Fang Harness — Orb / UI-UX overhaul tracker (worktree /root/fang-orb, branch feature/orb-ux)

Start: 0c5b1df (v0.1.27). Kit: /root/storage/shared/Download/fang-harness/rea-tech/ (SPEC.md, TECH.md, ASK_QUESTION.md, PASTE_AND_MENU.md, BLOBS.md, THEMES.md, shaders/*.agls, OpenCode.zip).

## Status at a glance
- Pushed to `debug`: `458639e` — CI **GREEN** (orb identity + foldable ask card).
- Pushed to `debug`: `714b401` — CI **FAILED** (3 detekt issues, all fixed locally, not yet pushed).
- Local fixes pending push: detekt function-count / unused-param / long-method.
- `main` / release: still `0c5b1df` = v0.1.27. Nothing released.

## Stage 0 — foundation — DONE (gate closed; user approved shader redistribution)
- [x] 8 raw AGSL shaders (7 extracted + Fang's `cells` reconstruction `shader_07.agls`)
- [x] OrbShader.kt — RuntimeShader, child binding via `setInputShader`, uniforms, API-33 guard, remember helper
- [x] assets/licenses/opencode-agls.txt attribution
- [x] reviewed; compile sanity by inspection

## Stage 1 — orb identity — DONE (in `458639e`, CI green)
- [x] FangOrb: moods, cyan/green, 3.8 s breath, frame read inside Canvas only
- [x] API 29–32 fallback keeps SimmeringPot pulse
- [x] FangTheme → shader tints (blend `fangColors.accent`/`dotSuccess` into the kit cyan/green)
- [x] 36 SimmeringPot sites classified; working sites → orb, generic progress keeps pot
- [x] LocalOrbsFrozen: freeze orb + pot while a side panel covers the chat
- [x] Dynamic placing: ambient orbSize scale (Small/Medium/Large) + slow gaze drift; settings persisted
- [x] Settings rows: orb size + float-over-chats (Appearance section)

## Stage 2 — chat states — PARTIAL (deliberate skips)
- [x] Working glyph on tool rows, turn footer, status banners (calm WORKING mood, no bounce)
- [x] Copy full message + Quote in reply on user message menu
- [x] Opt-in floating work badge (`FloatingWorkBadge.kt`) driven by the `orbFloats` setting
- [ ] Green completion morph / copy handoff — SKIPPED: no copy slot on working glyphs; assistant replies already have CopyButton
- [ ] Subagent capsules/popups — SKIPPED: ui/subagents/* are empty object stubs; no surface to attach to
- [ ] Message bubbles lift/rise + cached pulse — SKIPPED: needs careful ChatStream lazy-list/follow coordination
- [ ] Link long-press Copy/Share/Open — SKIPPED: third-party markdown lib; custom link annotation risks breaking rendering

## Stage 3 — ask card fold — DONE (in `458639e`, CI green)
- [x] In-composer foldable/expandable card, folded bar like Todos/Background
- [x] Answered summary with checked options; failed answer keeps card with Retry
- [x] Animated fold/expand, strings in resources, answers via channel only

## Stage 4 — paste + menu — DONE in code, CI fixes pending
- [x] Pasted Text card: `Attachment.Text`, 300-char threshold, paste→card, full-text dialog with Copy, `deliver()` includes full text
- [x] Copy/quote menu family (new strings_chat_menu.xml)
- [x] Skipped OpenCode-only items (Ask OpenCode, report a problem, per-session themes)

## Verify / ship
- [x] CI green for `458639e` (all earlier failures mechanical: imports, ktlint, wrong RuntimeShader API, unused constant, NewApi — fixed with no suppressions)
- [ ] CI `44033669235` for `714b401` failed with 3 detekt errors — fixes applied locally:
  - [x] ChatScreen.kt 12→11 functions (inlined quote helper, extracted FloatingWorkBadge)
  - [x] PastedTextDialog.kt unused `text` param removed (+ Composer call site)
  - [x] UserMessageMenu.kt 83→<60 lines (private `ActionItem` helper)
- [ ] TWO independent agents verifying the fixes and reviewing correctness (running now)
- [ ] Apply any findings, re-run static checks, commit + push `debug`, CI green
- [ ] Ask before `main` → release → APK size/SHA-256/signature verify
- [ ] Device check: user confirms orb renders and looks right (no emulator here)

## Coordination rules
- Disjoint file ownership; no agent commits; CI between stages; no suppressions/baselines; no literals in UI.
- Latest round: paste agent = Attachments/Composer/ChatController/strings_chat.xml/PastedTextDialog; menu agent = MessageItems/UserMessageMenu/ChatScreen/strings_chat_menu.xml; main agent = FangOrb/OrbShader/OrbAmbient/SimmeringPot/MainScreen/FloatingWorkBadge.
