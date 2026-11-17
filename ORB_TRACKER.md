# Fang Harness — Orb / UI-UX overhaul tracker (worktree /root/fang-orb, branch feature/orb-ux)

Start: 0c5b1df (v0.1.27). Kit: /root/storage/shared/Download/fang-harness/rea-tech/ (replicate/SPEC.md, TECH.md, ASK_QUESTION.md, PASTE_AND_MENU.md, shaders/*.agls, OpenCode.zip).

## Stage 0 — foundation (Agent A, disjoint new files only)
- [ ] 8 raw AGSL shaders in app/src/main/res/raw (7 extracted + Fang's cells reconstruction shader_07)
- [ ] OrbShader.kt: RuntimeShader, child binding, uniforms, remember helper, API 33 guard
- [ ] assets/licenses/opencode-agls.txt attribution
- [ ] Review return, compile sanity by inspection (no Gradle locally)
- [ ] RISK: opencode-agls.txt says upstream license unknown and "do not redistribute" — confirm with user before any release containing these shaders

## Stage 1 — identity (me after A)
- [ ] FangOrb composable: mood state, cyan/green, 3.8s breath, dot grid, Fresnel, dither, glow; Canvas-only frame reads
- [ ] API 29-32 fallback keeps SimmeringPot pulse (no blank UI)
- [ ] FangTheme colours -> shader tints (light/dark/AMOLED)
- [ ] SimmeringPot call-site classification (36 sites): orb for working state, pot for generic progress
- [ ] LocalPotsSimmer freeze in MainScreen around covered panels
- [ ] Dynamic placing: docked/floating/parked + gaze; settings in Preferences

## Stage 2 — chat states (Agent D after stage 1)
- [ ] Working glyph on messages/turn footer/subagents; green completion; copy handoff
- [ ] Subagent capsules/popups on SubagentRuns
- [ ] Bubbles and send interaction from kit
- [ ] Link long-press Copy/Share/Open

## Stage 3 — ask card fold (Agent B after stage 1)
- [ ] In-composer foldable/expandable AskUserCard per ASK_QUESTION.md; folded bar like Todos/Background; no duplicated content logic
- [ ] Answered summary with checked option; failed answer with Retry
- [ ] Animations; strings in resources; answers via existing channel path, never chat messages

## Stage 4 — paste + menu (Agent C after B; shares strings_chat.xml, so serialized)
- [ ] Pasted Text attachment card, full text, OpenPastedText, pasteError/empty
- [ ] Copy/quote menu family; quote keeps code fenced; skip OpenCode-only items

## Verify / ship
- [ ] Local host checks (if host changed) + Kotlin static review (line length, detekt limits, imports)
- [ ] Push debug -> CI green, fix every failure
- [ ] Ask before main -> release -> APK size/SHA-256/signature verify
- [ ] Device check: user confirms orb visually (no emulator here)

## Coordination rules
- One agent owns a file set; others must not touch it. No git add/commit by agents. I integrate.
- CI after each stage: never stack unverified Kotlin changes.
- No new @Suppress, no baselines, no string literals in UI, no new dependency without license check.
- Supermemory unauthenticated; tracker is local only.
