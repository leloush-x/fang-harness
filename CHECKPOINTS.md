# CHECKPOINTS — Fang Harness upgrade to match Pipkin

This file is the working memory for the upgrade of fang-harness (an older fork) to the current Pipkin.
Re-read before every step. Tick items only after CLI green, APK builds, and all tests pass.

## Current task
Porting pipkin features/structure into fang (host core, subagents, differing merges), skipping remote/local-models. Termx terminal core partially integrated.

## Branch
`debug` (all work here; do not touch `main` until done and green).

## Repositories
- Fang Harness: github.com/leloush-x/fang-harness (this repo), package `io.leloush.fang`
- Reference: github.com/jeph/pipkin (latest), package `io.jeph.pipkin`
- Termx (terminal core): github.com/leloush-x/termx, clone at `/root/core/termx` (main branch). Never touch `/root/termx`.

## Status legend
- [ ] not started
- [~] in progress
- [x] done (verified)

## Done
- [x] Created `debug` branch.
- [x] Cloned Pipkin reference and Termx (main) at `/root/core/termx`.
- [x] Wrote initial CHECKPOINTS.md and AGENT.md.
- [x] Upgrade 1 (implementation): Pi 1.5.2, host deps latest, lockfiles regenerated, Node >=24/latest in runtime.
- [x] Upgrade 7 (partial): Termx FakeProcfs + bind_if_blocked /proc (SN1), T1-gated shared storage binds (SCOM) in WolfiRuntime.
- [x] Copied/rebranded selected pipkin-only files (remote/local-models skipped).
- [x] Latest debug CI green at `30f643a` (host check/typecheck/lint/tests, Android check, debug APK).

## Upgrades (in order)

### Upgrade 1 — Latest versions only (DO FIRST)
- [x] Update `@earendil-works/pi-coding-agent` and `@earendil-works/pi-ai` to latest (1.5.2) in `host/package.json` and `app/src/main/assets/pi/package.json`.
- [x] Update other host deps to latest working: oxlint 1.93.0, vitest 5.1.0, @types/node 26.6.6, typebox 1.4.7, undici 8.12.4, defuddle 0.19.6, @modelcontextprotocol/sdk 1.32.2.
- [x] Node.js: use latest (>=24); updated `nodeVersionAtLeastMinimum` and `engines.node`, runtime installs latest `nodejs`.
- [x] Remove old pinned versions in host/pi deps and configs touched; lockfiles regenerated.
- [x] Home screen/setup uses bundled expected Pi version (latest); no old-version option.
- [x] Latest-working rule: no breaks found so far (host lint/build OK locally).
- [x] Verify: CLI green, APK builds, all tests pass on CI for `30f643a`.
- [~] Keep Upgrade 1 ticked as implemented/verified at latest green point; continue overall upgrade work.

### Upgrade 2 — Host core additions (pipkin → fang, rebranded)
- [x] Ported `install-lock.ts`, `pid-lock.ts`, `subagent-folders.ts`, `pi-extensions/string-enum.ts`, `modes/subagent-sessions.ts`, `modes/subscription-models.ts`; merged `fang-channel.ts` and kept `fang-extensions.ts` on Wolfi/fang behavior (no remote extension). `string-enum.ts` root and `termux-session.ts` not applicable/skipped.
- [~] Merged pipkin host changes into many in-scope files (catalogue/channel/config-lock/connection/detect/env/folder-search/model/model-list/models-json/runtime/scope/session-file/tar, selected modes, most fang-web-access modules + new providers). `ensure` merged with install-lock/completion-marker improvements (no remote). `sessions`/`utility` merged with in-scope parity (subagent counts, unified utility protocol/ops) excluding remote/local-models ops. Restored Wolfi-specific `env` behavior (`FANG_HOME`, comments) after parity port.
- [x] Reviewed remote-mixed files (`main.ts`, `fang-extensions.ts`, `modes/browse,files,git,rpc,save-attachments,terminal`, `pi-extensions/fang-background/{index,tasks}.ts`, `pi-extensions/fang-subagents/runs.ts`): pipkin differences are remote/local-model support; no safe non-remote merges applied, Wolfi/local behavior retained. `prompt.ts` remains Wolfi-specific.
- [~] Port host tests: aligned `scope`, `channel`, `provider-save`, `subscription`, `login`, `package-install`, `update-extensions` tests with pipkin parity where applicable; removed obsolete `folder-search.test.ts`; added `subscription-models.test.ts` and `harness.runOp`; existing fang-named background/subagents/todo tests already match pipkin rebranded. Remote/local-models/helper/ssh tests intentionally skipped.
- [x] Host build+lint/typecheck green locally and CI green at latest verified commit (`4f14f08`). Non-remote host parity substantially merged; remaining gaps tracked in other upgrades.

### Upgrade 3 — Subagents
- [ ] Port `app/.../agent/LiveSubagent.kt`, `StepFigure.kt`, `StepStream.kt`, `SubagentRuns.kt`.
- [ ] Port `app/.../data/PendingReload.kt`.
- [ ] Port strings_subagents.xml and related string changes.
- [ ] Port UI changes for subagents (in chat/agent screens).
- [ ] Port host `subagent-sessions` mode and tests.
- Verify, commit, tick, log.

### Upgrade 4 — Remote chat (SSH hosts) — SKIPPED (user: not needed)
- [x] Skipped per user instruction. No remote chat feature.

### Upgrade 5 — Local models (LiteRT-LM, llama.cpp, OpenCL) — SKIPPED (user: not needed)
- [x] Skipped per user instruction. No local models.

### Upgrade 6 — Subscriptions / model defaults
- [x] Ported `subscription-models` mode and tests, `subscription-save` updates (host parity merged earlier).
- [~] Merged model-defaults host changes where applicable; added app `subscriptionModels` host/client API. Full pipkin-style subscription/model-selection UI integration not yet ported (fang UI differs significantly).
- Verify, commit, tick, log.

### Upgrade 7 — Termx terminal core (replace old Wolfi terminal)
- [~] Copy Termx core: `FakeProcfs.kt` integrated; other UI/storage components pending as needed.
- [x] Adapted Termx proc snapshot logic (bind_if_blocked) into WolfiRuntime.syntheticProcFiles.
- [x] Fix SN1 permission error (/proc/stat permission denied) via FakeProcfs + bind_if_blocked proc binds.
- [x] Fix SCOM permission error: shared storage binds (/sdcard,/storage) gated on T1 permission (All-files/legacy).
- [x] Copy proc config (FakeProcfs) so environment behaves like a real device.
- [~] Port Termx tests: `FakeProcfsTest` added for wolfi-core (adapted) and verified on CI; BindTable/StorageAccess tests not directly portable because wolfi-core inlines bind/storage logic in `WolfiRuntime` (no separate classes like Termx).
- [x] Keep Ghostty terminal widget (libghostty-android); runtime core updated.
- [x] Verify: CLI/host checks and APK/tests green on CI at `4f14f08`.
- Commit, tick, log.

### Upgrade 8 — UI, strings, icons, fonts, site
- [~] Updated `data_extraction_rules.xml` for Wolfi runtime, improved day/night themes (window background/accent); values-night colors/manrope/drawables already at parity. Network security config for local models intentionally not added.
- [~] Merged non-remote missing string resources from pipkin into in-scope `strings_*.xml` (archive/chat/connection/host/inbox/onboarding/settings/shell); existing strings and remote/local-model strings need further careful parity review.
- [~] Updated site components/theme/scripts/pages/assets/licenses (rebranded, skipped remote/local-model-heavy astro config/content areas); docs kept fang-specific to avoid reintroducing skipped features.
- [ ] Complete UI parity (icons/drawables/launcher, full strings merge, values-night/colors/launcher icon as needed) and app screen changes; verify, commit, tick, log.

### Upgrade 9 — Build config, scripts, docs, plans
- [~] Ported `scripts/sign-play-bundle.sh`, `scripts/upload-play-bundle.sh` (skipped ssh-test-host). Attempted AGP/Kotlin bump reverted due CI JDK/lint toolchain mismatch; build config merges still pending.
- [ ] Merge build.gradle.kts, settings.gradle.kts, gradle.properties, gradle/wrapper, CMakeLists, AndroidManifest, syntax.c (preserve fang/Wolfi, exclude remote/local-model deps).
- [ ] Port non-remote docs/acknowledgements/privacy updates and plans; merge workflows (`ci.yml`/`release.yml`) as appropriate.
- Verify, commit, tick, log.

### Upgrade 10 — Final verification and merge
- [ ] Run full check (CI), APK builds, all tests pass.
- [ ] Update AGENT.md changelog with all upgrades.
- [ ] Push debug to main (per fang AGENTS.md: `git push origin HEAD:main`).
- [ ] Verify merge-base, remove worktree/branch, pull main.

## Blockers
(none yet)

## Next
- Implement Upgrade 3 subagent parity (host/app wiring, agent/timeline models, UI/screens, resources/tests) excluding remote/local-models; verify CI green.
- Complete Upgrade 6 subscription/model-defaults app/UI integration as needed.
- Upgrade 8/9: merge remaining UI strings/resources/icons/fonts/app screens, build config, scripts, docs (non-remote/local-models), workflows; keep Wolfi runtime and fang branding.
- After each coherent step: host build+lint green locally when applicable, commit, push debug, verify CI green + APK/tests, then proceed. Final: push debug to main.
