# Fang Harness — Orb / UI-UX overhaul tracker (worktree /root/fang-orb, branch feature/orb-ux)

Start: 0c5b1df (v0.1.27). Kit: /root/storage/shared/Download/fang-harness/rea-tech/ (SPEC.md, TECH.md, ASK_QUESTION.md, PASTE_AND_MENU.md, BLOBS.md, THEMES.md, shaders/*.agls, OpenCode.zip).

## Status at a glance
- `458639e` — CI **GREEN** (orb identity + foldable ask card).
- `714b401` — CI failed (3 detekt issues: function count, unused param, long method).
- `2d9be0f` — CI failed (4 ktlint issues in Composer.kt: when-branch spacing, else braces).
- `296f175` — CI **GREEN** (`44036006927`). All stages verified by CI.
- Local ktlint 1.8.0 CLI available at `/tmp/ktlint-full.jar` (matches CI config `android_studio`); run on all changed files = exit 0.
- `main` / release: still `0c5b1df` = v0.1.27. Nothing released.

## Stage 0 — foundation — DONE (user approved shader redistribution)
- [x] 8 raw AGSL shaders (7 extracted + Fang's `cells` reconstruction `shader_07.agls`)
- [x] OrbShader.kt — RuntimeShader, child binding via `setInputShader`, uniforms, API-33 guard, remember helper
- [x] assets/licenses/opencode-agls.txt attribution

## Stage 1 — orb identity — DONE (CI green in 458639e)
- [x] FangOrb: moods, cyan/green, 3.8 s breath, frame read inside Canvas only
- [x] API 29–32 fallback keeps SimmeringPot pulse
- [x] FangTheme → shader tints (blend `fangColors.accent`/`dotSuccess` into kit cyan/green)
- [x] 36 SimmeringPot sites classified; working sites → orb, generic progress keeps pot
- [x] LocalOrbsFrozen: freeze orb + pot while a side panel covers the chat
- [x] Dynamic placing: ambient orbSize scale + slow gaze drift; settings persisted in Preferences

## Stage 2 — chat states — PARTIAL (deliberate skips)
- [x] Working glyph on tool rows, turn footer, status banners (calm WORKING mood)
- [x] Copy full message + Quote in reply on user message menu (Quote appends to draft, keeps code fenced)
- [x] Opt-in floating work badge (`FloatingWorkBadge.kt`) driven by `orbFloats`
- [ ] Green completion morph / copy handoff — SKIPPED: no copy slot on working glyphs; replies already have CopyButton
- [ ] Subagent capsules/popups — SKIPPED: ui/subagents/* are empty stubs; no surface to attach to
- [ ] Message bubbles lift/rise + cached pulse — SKIPPED: needs careful ChatStream lazy-list coordination
- [ ] Link long-press Copy/Share/Open — SKIPPED: third-party markdown lib; custom link annotation risks rendering

## Stage 3 — ask card fold — DONE (CI green in 458639e)
- [x] Foldable/expandable card, folded bar like Todos/Background
- [x] Answered summary with checked options; failed answer keeps card with Retry
- [x] Animated fold/expand, strings in resources, answers via channel only
- [x] FIXED (review): send failure now propagates (no false "Answered"), in-flight guard prevents double Submit

## Stage 4 — paste + menu — DONE in code
- [x] Pasted Text card: `Attachment.Text`, 300-char threshold, full-text dialog with Copy, `deliver()` includes full text
- [x] Copy/quote menu family (new strings_chat_menu.xml)
- [x] FIXED (review): no false "Nothing to paste" snackbar for URI-only clips
- [x] Skipped OpenCode-only items

## Stage 5 — IMPLEMENT.md surfaces — IN PROGRESS (user: "go all in", skip §5 OpenCode-only)
- [x] §4 completion morph (orb→copy, 0.55 s finale / 0.34 s handoff) — `40c4e58`, CI GREEN
  - Fixed by judgment review: real `ToolCall.output` now feeds the copy button; finished rows keep their tool icon when there is no output; morph respects LocalOrbsFrozen
- [x] §2 subagent capsules UI (press/fling/fold, no navigation) — `041fb60` + `d2512c5`, CI GREEN
  - Fixed by review: fling reads consumption in `Final` pass (parent scroll no longer folds pills), width bounded for ellipsis, single-exit loop for detekt
  - NOT yet wired into chat — host needs data plumbing and must avoid ChatStream index changes
- [ ] §1 link long-press menus — **REVERTED**. Review proved Box-level long-press has no link hit-testing: long-pressing ordinary text opens the last link's menu, and it stacks a second menu over text selection. No public per-link long-press hook exists in markdown 0.46.0 / Compose (LinkInteractionListener has only onClick). Doing it correctly needs a `text`-component + `onTextLayout` rewrite plus selection suppression — not safe under "nothing breaks". Revisit only with device testing.
- [x] §3 bubble rise animations — `347cc72`, CI pending. Fixed by review: async-seeded history no longer rises (seed on first non-empty list)
- [ ] §5 OpenCode-only — SKIPPED by user (Ask OpenCode, Fork, Report a Problem, share)
- [x] Wire StreamSubagents host — `4a14cb4`, CI pending. Review found and fixed LongParameterList blocker (ChatContent back to 8 params) and stale sheet snapshot (stores id, derives live fact, closes when run leaves)
- [ ] Two reviews per stage + CI gate per stage

## Verify / ship
- [x] Independent agent 1 (mechanical): PASS on all 3 detekt fixes, no new issues, 37 string refs resolve
- [x] Independent agent 2 (correctness): found AskState false-success + double-send (fixed), Quote draft wipe (fixed), paste snackbar (fixed); 2 LOW findings left by choice (mixed-clip URI loss = pre-existing, not introduced here)
- [x] CI `44036006927` green — all stages compile, detekt/ktlint/lint/tests pass
- [x] Notification-system investigation: NO actual bug. "Misses" are by design — heads-ups suppressed while app is on screen (in-app inbox pill instead), seen-on-screen dialogs intentionally not notified later, taken-back dialogs never re-posted. Permission denial/OEM/Doze are outside app control. No code change needed.
- [ ] Ask before `main` → release → APK size/SHA-256/signature verify
- [ ] Device check: user confirms orb renders and looks right (no emulator here)

## Coordination rules
- Disjoint file ownership; no agent commits; CI between stages; no suppressions/baselines; no literals in UI.
- Agents used: foundation, orb component, glyphs, ask card, paste, menu, detekt verifier, correctness reviewer (2 passes, 1 redirected after misread).
