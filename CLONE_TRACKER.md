# OC pixel-perfect clone tracker (worktree /root/fang-clone, branch feature/oc-clone)

Base: origin/main 6bc5c3c (v0.1.28). Kit: /root/storage/shared/Download/fang-harness/oc/ (CLONE_SPEC.md, EXACT_CODE.md, shader_00..06.agls).

## Key findings
- All 7 shipped shaders are byte-identical to the new verbatim kit (only DEX prefix/newline differed). No shader copy needed.
- "Pixel-perfect" is not provable from the artifacts: mode→uniform mapping is RECONSTRUCTED; JADX stopped at 33%, so exact theme RGBA values and Compose bodies were never recovered. Behaviorally faithful, visual parity needs a device A/B check.

## Stage A — Orb 5 modes — DONE (in 695ceff, CI pending)
- [x] OrbMood exactly IDLE/LISTENING/SPEAKING/DONE/FAILED; all call sites updated
- [x] Exact uniform table (mixBX/Y, rimX/Y, tintBase/Core, glow.x), palette constants, 3.8s breath
- [x] Done: 0.55s finale, glow 1.25→0.60, one-shot via finaleSpent (no re-flash on unfreeze)
- [x] Failed: mixB=(1,1), red MUTED_RIM, glow 0 (review fix), muted plate
- [x] Done keeps all-green dots (userTint→agent on DONE, review fix)
- [x] API<33 pot fallback, LocalOrbsFrozen freeze, ambient size preserved

## Stage B — Ask square bulge — DONE (in 695ceff)
- [x] Collapsed 44dp square bulge at composer top with count, accent/failure tint
- [x] Tap expands question card via AnimatedVisibility spring; drag-down/chevron folds
- [x] Same question content reused; answered summary + failed Retry reachable
- [x] No lazy list/BoxWithConstraints in ChatStack intrinsic-measured dialog slot; contentMaxWidth + 280dp cap
- [x] Paging/skip/timeout/freeform preserved; answers via channel only

## Stage C — Subagent full chat — DONE (in 695ceff)
- [x] Capsule tap → full overlay chat: LiveSubagents.of/follow/unfollow, ChatStream transcript, per-run ChatView
- [x] Steering composer → steerSubagent; failure now surfaced (signature returns Boolean, review fix)
- [x] BackHandler + Back button, LIFO-exclusive with parent; finished run keeps transcript + notice
- [x] of(runId) Flow remembered to avoid collection restart (review fix)
- [x] ChatContent refactored to ChatMainBody to stay under detekt 60-line/8-param limits
- [ ] LOW left: if follow finds no run, "starting…" shows forever (no receipt signal in protocol to drive a failure UI)

## Stage D — theme 28 slots / signal matrix — SKIPPED this batch
- Optional, adds risk with no device to verify; theme RGBA values were not recoverable anyway.

## Verify / ship
- [x] 3 workers + 1 judgment + 2 review agents; all findings fixed
- [x] Local ktlint exit 0 on all 14 changed Kotlin files; function/param/line metrics pass
- [ ] CI 44063143911 pending
- [ ] Ask before main/release; device A/B needed for any pixel-perfect claim

## Coordination
- Disjoint files per worker; no agent commits; no @Suppress/baselines; all text in resources.
