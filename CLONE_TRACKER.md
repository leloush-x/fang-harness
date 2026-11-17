# OC pixel-perfect clone tracker (worktree /root/fang-clone, branch feature/oc-clone)

Base: origin/main 6bc5c3c (v0.1.28). Kit: /root/storage/shared/Download/fang-harness/oc/ (CLONE_SPEC.md, EXACT_CODE.md, shader_00..06.agls).

## Key finding
All 7 shipped shaders are already byte-identical to the new verbatim kit (only the DEX prefix/newline differs). No shader copy needed. The gap is the state machine and UI flows.

## Stage A — Orb 5 modes (Idle/Listening/Speaking/Done/Failed)
- [ ] OrbMood: replace 6 moods with the exact 5; map CONNECTING/WAITING→Idle, WORKING/SPEAKING→Speaking, LISTENING→Listening, DONE→Done, add Failed
- [ ] Exact palette from shader_03: USER_BASE cyan, AGENT_BASE green, MUTED_PLATE, MUTED_RIM red, mix plate mixB.x*0.65, shadow alpha 0.42, dither
- [ ] Exact uniform mapping table (CLONE_SPEC 1.2): mixB, rim.x/y, tintBase/Core, glow.x per mode
- [ ] Done = 0.55 s finale with glow 1.25 flash; Failed = red rim + muted plate
- [ ] Breath = 3.8 s; settle 350 ms
- [ ] Update all OrbMood call sites (FloatingWorkBadge, MorphGlyph, SubagentCapsules)
- [ ] API 29-32 fallback preserved; freeze preserved

## Stage B — Ask questions = todos-like square bulge
- [ ] Collapsed = small square/count bulge in composer top area (like WidgetTabs tab), expanded = question + choices growing out of it
- [ ] Drag down to fold; tap to expand; failed answer stays on Retry
- [ ] Device-optimized widths from tokens; no lazy list in intrinsic-measured slot
- [ ] strings from resources

## Stage C — Subagent popups + full chat entry
- [ ] Capsule quality/size optimization (Capsule width, rounded press, single 'every-running-subagent' entry)
- [ ] Tap capsule → FULL CHAT view using existing LiveSubagents.of(runId) + follow/unfollow + ChatStream + composer steering via process.steerSubagent
- [ ] Back returns to parent; can send steering while run is active (not gated on done)
- [ ] Keep detail sheet as secondary action if useful

## Stage D — Theme 28 slots + optional signal matrix/connection strip
- [ ] 28-slot ThemeVariant mapping from EXACT_CODE.md into FangColors where meaningful
- [ ] Signal matrix (shader_01/02) + connection strip (shader_04) only if safe; report if skipped

## Verify / ship
- [ ] Two reviews per stage (subagents) + local ktlint + CI gate per stage
- [ ] Ask before main/release
- [ ] Device check required for pixel-perfect claim

## Coordination
- Disjoint file ownership per worker; no agent commits; main integrates and reviews.
- No @Suppress/baselines; all user text in resources; ktlint android_studio; detekt 11 funcs/file, 60-line methods, 8 params.
