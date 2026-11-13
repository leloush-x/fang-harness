# CHECKPOINTS — Fang Harness upgrade to match Pipkin

This file is the working memory for the upgrade of fang-harness (an older fork) to the current Pipkin.
Re-read before every step. Tick items only after CLI green, APK builds, and all tests pass.

## Current task
Step 0 — plan written. Next: Upgrade 1 (Latest versions only).

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
- [x] Cloned Pipkin reference and Termx (main) for comparison.
- [x] Wrote initial CHECKPOINTS.md and AGENT.md.

## Upgrades (in order)

### Upgrade 1 — Latest versions only (DO FIRST)
- [ ] Update `@earendil-works/pi-coding-agent` and `@earendil-works/pi-ai` to latest (1.5.2) in `host/package.json` and `app/src/main/assets/pi/package.json`.
- [ ] Update other host deps to latest working: oxlint 1.93.0, vitest 5.1.0, @types/node 26.6.6, typebox 1.4.7, undici 8.12.4, defuddle 0.19.6, @modelcontextprotocol/sdk 1.32.2.
- [ ] Node.js: use latest (22.19+ minimum, target latest LTS/24+); update `nodeVersionAtLeastMinimum` and `engines.node`.
- [ ] Remove all old pinned versions everywhere (scripts, configs, lockfiles) — regenerate lockfiles.
- [ ] Home screen / setup must show installed version and keep on latest; remove old-version option.
- [ ] Latest-working rule: if newest breaks build/tests/CLI, use newest that works; document why here and in AGENT.md.
- Verify: CLI green, APK builds, all tests pass.
- Commit, tick, log in AGENT.md.

### Upgrade 2 — Host core additions (pipkin → fang, rebranded)
- [ ] Port `host/src/install-lock.ts`, `host/src/pid-lock.ts`, `host/src/model-list.ts`, `host/src/string-enum.ts`.
- [ ] Port `host/src/pipkin-extensions.ts` → `host/src/fang-extensions.ts` (merge with existing).
- [ ] Port `host/src/pi-extensions/pipkin-channel.ts` → `host/src/pi-extensions/fang-channel.ts` (merge).
- [ ] Port `host/src/pi-extensions/string-enum.ts`.
- [ ] Port host modes: `local-models-sync.ts`, `remote-connect.ts`, `remote-folder.ts`, `remote-hosts.ts`, `remote-install.ts`, `subagent-sessions.ts`, `subscription-models.ts`.
- [ ] Port `host/src/subagent-folders.ts`, `host/src/termux-session.ts` (adapt: fang uses Wolfi, not Termux — keep only generic parts).
- [ ] Merge pipkin host changes into differing host files (catalogue, channel, config-lock, detect, env, folder-search, main, mode, models-json, prompt, runtime, scope, tar, all modes/*, framing, connection, params, etc.).
- [ ] Port host tests: emulator-fixtures, helper-daemon, helper-protocol, local-models-sync, pipkin-background-records, pipkin-background, pipkin-remote, pipkin-subagents, pipkin-todo, remote-*, sshd, subagents-harness, subscription-models.
- Verify, commit, tick, log.

### Upgrade 3 — Subagents
- [ ] Port `app/.../agent/LiveSubagent.kt`, `StepFigure.kt`, `StepStream.kt`, `SubagentRuns.kt`.
- [ ] Port `app/.../data/PendingReload.kt`.
- [ ] Port strings_subagents.xml and related string changes.
- [ ] Port UI changes for subagents (in chat/agent screens).
- [ ] Port host `subagent-sessions` mode and tests.
- Verify, commit, tick, log.

### Upgrade 4 — Remote chat (SSH hosts)
- [ ] Port `app/.../agent/RemoteOutages.kt`, `data/RemoteHosts.kt`, `data/RemotePasswords.kt`.
- [ ] Port host modes remote-* and tests.
- [ ] Port UI/screens for remote hosts.
- [ ] Port docs/remote-hosts.md.
- [ ] Port strings_remote.xml.
- Verify, commit, tick, log.

### Upgrade 5 — Local models (LiteRT-LM, llama.cpp, OpenCL)
- [ ] Port AIDL files, cpp/llama, cpp/opencl, CMakeLists changes.
- [ ] Port `app/litertlm-rules.pro`, license files.
- [ ] Port strings_local_models.xml, strings_local_runtime.xml.
- [ ] Port docs/local-models.md, privacy.md updates.
- [ ] Port host mode local-models-sync.
- Verify, commit, tick, log.

### Upgrade 6 — Subscriptions / model defaults
- [ ] Port `subscription-models` mode and tests, `subscription-save` updates.
- [ ] Merge model-defaults changes.
- Verify, commit, tick, log.

### Upgrade 7 — Termx terminal core (replace old Wolfi terminal)
- [ ] Copy Termx core: `FakeProcfs.kt`, `StorageAccess.kt`, `StorageHealthScreen.kt`, `StoragePreferences.kt`, `WorkspaceTransfer.kt`, `AndroidDnsResolver.kt`.
- [ ] Copy Termx `ProotCommand.kt` (with snapshot binds) → adapt into fang's WolfiRuntime.
- [ ] Copy Termx `BindTable` logic into WolfiRuntime.command().
- [ ] Copy Termx proc snapshot logic (bind_if_blocked) into syntheticProcFiles.
- [ ] Fix SN1 permission error (/proc/stat permission denied) via snapshot binds + FakeProcfs.
- [ ] Fix SCOM permission error via T1 storage tier model (bind shared storage only when granted).
- [ ] Copy proc config (FakeProcfs) so environment behaves like a real device.
- [ ] Port Termx tests: BindTableTest, FakeProcfsTest, StorageAccessTest.
- [ ] Keep Ghostty terminal widget (libghostty-android); only swap runtime core.
- Verify, commit, tick, log.

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
Upgrade 1 — Latest versions only.
