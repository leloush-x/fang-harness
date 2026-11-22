# OpenCode-flow → Fang port tracker (worktree /root/fang-flow, branch feature/opencode-flow)

Base: origin/main f2d8a83 (v0.1.30). Upstream source: /tmp/opencode-android-src (cloned from github.com/anomalyco/opencode-android, no LICENSE file — re-implement flows in Fang style, do not verbatim copy).

## User goal
Dissect upstream with 5 parallel agents, understand orb flow everywhere (thinking orb, subagent orb, working glyphs), then apply OpenCode's flow/animation/orbs/subagent-pill to Fang. Fix orbs everywhere in the same manner with purpose. NO voice/call features.

## Phase 1 — dissection (5 agents, read-only, running)
- [ ] A: Wisp orb visual engine (shaders/cells/palette/state/tuning/breath)
- [ ] B: agent-activity/thinking/working glyphs + completion morph
- [ ] C: subagent pills/capsules + full chat entry
- [ ] D: themes 28-slot + ask-question flow
- [ ] E: module wiring + session screen composition tree

## Phase 2 — Fang flow map (after dissection)
- [ ] Map each upstream surface to Fang's equivalent files.

## Phase 3 — apply (disjoint workers, 2 reviews + CI gate per stage)
Architecture decision: upstream has TWO orb systems. Port the small 7x7 ActivityGlyph (the "orbs everywhere") as a NEW package, switch chat/subagent indicators to it. Keep Wisp FangOrb intact (no big-orb UI in Fang). See GLYPH_CONTRACT.md.
- [ ] Worker 1: ActivityGlyph engine (new package + activity_orb.agls + tests) — running
- [ ] Worker 2: chat row wiring (MessageItems/StreamBanners/MorphGlyph) — running
- [ ] Worker 3: subagent pill upgrade (SubagentCapsules/Calls/Sheet/Text) — running
- [ ] Worker 4: judgment/risks — running
- [ ] Ask flow (after engine lands): auto-advance single choice, paste suppression, fold tuning
- [ ] Me: ChatScreen/ChatMainBody integration + strings
- [ ] 2 reviews per stage + CI gate

## Verify: local ktlint + CI + ask before main/release. Device check needed for visuals.
