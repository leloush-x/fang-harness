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

## Upgrades (in order)

### Upgrade 1 — Latest versions only (DO FIRST)
- [x] Update `@earendil-works/pi-coding-agent` and `@earendil-works/pi-ai` to latest (1.5.2) in `host/package.json` and `app/src/main/assets/pi/package.json`.
- [x] Update other host deps to latest working: oxlint 1.93.0, vitest 5.1.0, @types/node 26.6.6, typebox 1.4.7, undici 8.12.4, defuddle 0.19.6, @modelcontextprotocol/sdk 1.32.2.
- [x] Node.js: use latest (>=24); updated `nodeVersionAtLeastMinimum` and `engines.node`, runtime installs latest `nodejs`.
- [x] Remove old pinned versions in host/pi deps and configs touched; lockfiles regenerated.
- [x] Home screen/setup uses bundled expected Pi version (latest); no old-version option.
- [x] Latest-working rule: no breaks found so far (host lint/build OK locally).
- [ ] Verify: CLI green, APK builds, all tests pass (pending CI).
- [~] Commit done; tick fully after verification, log in AGENT.md.

### Upgrade 2 — Host core additions (pipkin → fang, rebranded)
- [~] Port `host/src/install-lock.ts`, `host/src/pid-lock.ts`, `host/src/model-list.ts`, `host/src/pi-extensions/string-enum.ts`; `host/src/string-enum.ts` N/A in pipkin root.
- [~] Port `host/src/pipkin-extensions.ts` → `host/src/fang-extensions.ts` and `pipkin-channel.ts` → `fang-channel.ts` (non-remote parity parts merged/updated; remote-extension handling still pending careful merge).
- [~] Port host modes: `subagent-sessions.ts`, `subscription-models.ts` added; `local-models-sync.ts` and `remote-*` modes skipped per scope.
- [~] Port `host/src/subagent-folders.ts`; `host/src/termux-session.ts` skipped (Wolfi runtime, not Termux).
- [~] Merged pipkin host changes into many differing in-scope files (catalogue/channel/config-lock/connection/detect/env/folder-search/mode/model-def/model-list/models-json/runtime/scope/session-file/tar, several modes, fang-channel, most fang-web-access modules + new providers). `ensure` merged with install-lock/completion-marker improvements (no remote). `sessions`/`utility` merged with in-scope parity (subagent counts, unified utility protocol/ops) excluding remote/local-models ops. Restored Wolfi-specific `env` behavior (`FANG_HOME`, comments) after parity port. Remote-dependent files still pending careful non-remote merge: main/prompt/fang-extensions, browse/files/git/rpc/save-attachments/terminal, fang-background, fang-subagents/runs.
- [~] Port host tests: aligned `scope`, `channel`, `provider-save`, `subscription`, `login` tests with pipkin parity; removed obsolete `folder-search.test.ts`; added `subscription-models.test.ts` and `harness.runOp`; existing fang-named background/subagents/todo tests already match pipkin rebranded. Remote/local-models/helper/ssh tests intentionally skipped.
- [~] Host build+lint/typecheck green locally; multiple fixes pushed to debug. CI runs repeatedly cancelled by newer pushes; latest run pending/needs verification for green check/tests/APK.

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
- [ ] Port `subscription-models` mode and tests, `subscription-save` updates.
- [ ] Merge model-defaults changes.
- Verify, commit, tick, log.

### Upgrade 7 — Termx terminal core (replace old Wolfi terminal)
- [~] Copy Termx core: `FakeProcfs.kt` integrated; other UI/storage components pending as needed.
- [x] Adapted Termx proc snapshot logic (bind_if_blocked) into WolfiRuntime.syntheticProcFiles.
- [x] Fix SN1 permission error (/proc/stat permission denied) via FakeProcfs + bind_if_blocked proc binds.
- [x] Fix SCOM permission error: shared storage binds (/sdcard,/storage) gated on T1 permission (All-files/legacy).
- [x] Copy proc config (FakeProcfs) so environment behaves like a real device.
- [ ] Port Termx tests: BindTableTest, FakeProcfsTest, StorageAccessTest (adapted for wolfi-core).
- [x] Keep Ghostty terminal widget (libghostty-android); runtime core updated.
- [ ] Verify: CLI green, APK builds, all tests pass (pending CI).
- Commit, tick, log.

### Upgrade 8 — UI, strings, icons, fonts, site
- [~] Ported `app/src/main/res/xml/data_extraction_rules.xml`; values-night/manrope/most drawables already present or not safely portable without branding conflicts.
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
- Continue Upgrade 2: carefully merge non-remote pipkin changes into remaining host files (prompt stays Wolfi-specific; merge non-remote parts of utility/sessions/terminal/save-attachments/browse/files/git/rpc, fang-extensions, fang-background, fang-subagents/runs) and port applicable host tests.
- Upgrade 3/6: port subagents (app agent/UI/data/tests + host wiring) and subscription/model-defaults integration, excluding remote/local-models.
- Upgrade 7: add/adapt Termx tests (BindTable/FakeProcfs/StorageAccess) for wolfi-core where feasible.
- Upgrade 8/9: merge UI strings/resources/fonts/icons, site, build config, scripts, docs (non-remote/local-models), workflows; rebrand and keep Wolfi runtime.
- After each coherent step: host build+lint green, commit, push debug, verify CI green + APK/tests, then proceed. Final: push debug to main.
