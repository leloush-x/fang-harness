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
- [~] Merged pipkin host changes into many differing in-scope files (catalogue/channel/config-lock/connection/detect/env/folder-search/mode/model-def/model-list/models-json/runtime/scope/session-file/tar, several modes, fang-channel, most fang-web-access modules + new providers). Remote-dependent files left on Wolfi/fang-safe implementation pending careful merge: main/prompt/fang-extensions, browse/ensure/files/git/rpc/save-attachments/sessions/terminal/utility, fang-background, fang-subagents/runs.
- [ ] Port host tests: emulator-fixtures, helper-daemon, helper-protocol, local-models-sync, pipkin-background-records, pipkin-background, pipkin-remote, pipkin-subagents, pipkin-todo, remote-*, sshd, subagents-harness, subscription-models (skip remote/local-models-only tests).
- [~] Host build+lint green locally (`cd host && OUT=/tmp/fh node build.ts && pnpm run lint`); full CI verification pending.

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
- [ ] Port new drawables (openrouter brand, launcher foregrounds, lucide icons).
- [ ] Port manrope font + OFL.
- [ ] Port values-night/colors.xml, launcher_icon.xml.
- [ ] Merge all strings_*.xml changes (archive, chat, connection, files, host, inbox, onboarding, settings, shell).
- [ ] Port site changes (favicons, manrope, components, theme).
- Verify, commit, tick, log.

### Upgrade 9 — Build config, scripts, docs, plans
- [ ] Merge build.gradle.kts, settings.gradle.kts, gradle.properties, gradle/wrapper, CMakeLists, AndroidManifest, syntax.c.
- [ ] Port scripts: sign-play-bundle.sh, ssh-test-host.sh, upload-play-bundle.sh.
- [ ] Port docs: local-models, privacy, remote-hosts, acknowledgements updates, etc.
- [ ] Port plans/ directory.
- [ ] Merge .github/workflows/release.yml.
- Verify, commit, tick, log.

### Upgrade 10 — Final verification and merge
- [ ] Run full check (CI), APK builds, all tests pass.
- [ ] Update AGENT.md changelog with all upgrades.
- [ ] Push debug to main (per fang AGENTS.md: `git push origin HEAD:main`).
- [ ] Verify merge-base, remove worktree/branch, pull main.

## Blockers
(none yet)

## Next
Continue Upgrade 2/3/6/8/9: merge pipkin host/app changes, port subagents/subscription features, update UI/docs/build while preserving Wolfi/Termx runtime and skipping remote/local-models.
