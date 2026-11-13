# CHECKPOINTS — upgrading fang-harness to current pipkin

What this file is: my working memory. Current task, what is done, what is next, and every blocker.
I re-read it before every step and update it after every step. `AGENT.md` is the permanent record
(architecture + changelog); this file is the running plan.

## Reference points

| What | Where |
|---|---|
| My work repo | `github.com/leloush-x/fang-harness`, branch **`debug`** (based on the pre-existing `origin/debug` = `main` + the OpenRouter/provider commit `ab935a4`; the user asked to create `debug`, that work already lives there, so I continue on it) |
| Reference app | `github.com/jeph/pipkin`, `dev` == `main` == **`9bf5767`** (cloned at `/work/ref/pipkin`) |
| Terminal source | `github.com/leloush-x/termx` @ `4017d3f` (v0.8.13, cloned at `/work/termx`) |
| Old Wolfi terminal (do NOT use) | `github.com/leloush-x/wolfi-terminal` (`com.rk.terminal`, cloned at `/work/wolfi-terminal`) — only its `RootfsExtractor` logic is already adapted in `wolfi-core/.../RootfsExtractor.kt` |
| Fang on main | `53e81d1`, 61 commits, forked 2026-10-21 |

Gap measured by rename-and-diff (fang tree, `fang`→`pipkin` renamed, vs pipkin):
**326 files only in pipkin, 18 only in fang, 221 differing.** The fang-only files are the Wolfi
runtime parts (`wolfi-core/`, `host/src/session.ts`, `host/src/synthetic-proc.ts`), Fang's own
CI workflows (`ci.yml`, `watch-actions.yml`), licenses (`proot.txt`, `wolfi-terminal.txt`),
four icons, and the site screenshots. Everything else is pipkin having moved ahead.

Architecture fact that shapes the whole port: **pipkin runs its host inside Termux** (a separate
app) and needs the Play-helper AIDL + `termux-session.ts` + `TermuxBuild.kt` to talk to it.
**Fang Harness has no Termux**: the host is a child of the app, started by `WolfiRuntime` through
the bundled PRoot (`libproot.so` from `wolfi-core/`). So pipkin's Termux handling is never ported;
its feature behavior is, adapted to the Wolfi launch path. The terminal's runtime parts (proot
command, fake `/proc`, DNS, storage binds, permissions) follow **TermX** instead, per the user.

## Rules for every upgrade

1. Work only on `debug`. `main` is untouched until every upgrade is green.
2. Per upgrade: implement → host checks green locally (`cd host && pnpm run check`, with the
   pnpm shim noted in AGENTS.md — the fast inner loop) → **commit and push `debug`** → GitHub
   Actions is the build gate (per the user: APKs build in Actions, not locally): `ci.yml` runs
   `./gradlew check :app:assembleDebug` plus the Wolfi-runtime-in-APK check (`lib/arm64-v8a` and
   `lib/x86_64` `libproot.so`/`libloader.so`, `assets/fang-host.mjs`) and must be green → tick
   this box → append to the AGENT.md changelog → only then start the next upgrade. Never move
   on red.
3. Rename everything ported: `io.jeph.pipkin`→`io.leloush.fang`, `Pipkin`→`Fang Harness`,
   `PIPKIN_`→`FANG_`, `pipkin-`→`fang-`, wire names `pipkin_*`→`fang_*`, bundle
   `pipkin-host.mjs`→`fang-host.mjs`.
4. Keep the Fork's identity: Wolfi OS runtime, rootfs from `leloush-x/wolfi-os-rootfs`,
   `wolfi-core` PRoot module, `FANG_*` env, fang site domain and branding.
5. Copy nothing under GPL/LGPL/AGPL in. License files in `app/src/main/assets/licenses/` and
   `site/public/licenses.txt` updated whenever bundled code changes.
6. "Latest versions only" rule: if a newest version breaks build/tests/CLI, use the newest
   version that works and record the why here and in AGENT.md.

---

## [ ] Upgrade 1 — Latest versions only

The pi coding CLI, Node.js, and every package/tool in the install path is always the newest
version; the home screen shows what is installed and keeps it there, with no old-version option.

- **pi runtime** (`app/src/main/assets/pi/package.json` + lock): 1.3.0 on main / 1.4.0 on debug
  → **1.5.2** (latest on npm, engines `node >=22.19.0`). Regenerate the lockfile with npm.
- **host deps** (`host/package.json` + `pnpm-lock.yaml`): `@earendil-works/pi-ai` and
  `pi-coding-agent` 1.3.0→**1.5.2**; devDeps to latest (oxlint, vitest, esbuild,
  `@types/node`, typescript latest-working — see Latest-working log).
- **Node.js in the runtime**: `WolfiRuntime.ensureNode` installs `nodejs-24` → change to
  `nodejs-latest` (Wolfi's rolling Node package) and keep the minimum check matching pi's
  engines (`>=22.19.0`). The runtime itself already runs on Node 24.
- **Toolchain**: Gradle wrapper 9.8.1→latest, AGP 9.4.0→**9.5.0**, Kotlin 2.4.20→**2.4.21**,
  `detekt`/`ktlint` plugins to latest, Compose BOM 2026.09.00→latest,
  `libghostty-android(-compose)` snapshot→newest published, `gradle.properties` jvmargs
  →4g (as pipkin). Verify each against the registry at work time.
- **Site** (`site/package.json` + lock): deps to latest.
- **Home screen shows latest, no old option**: the sidebar footer already says
  "Fang Harness · Pi" — it must show the **installed** Pi and Node versions (via the host's
  detect/utility path), and the setup/ensure flow must **always upgrade to the pinned-latest**
  (never offer a version picker, never skip a newer installed-into-APK version). Check Settings
  for any version-control UI and remove an old-version option if one exists.
- Verify: host checks green, `./gradlew check` green, APK builds, all tests pass.
- Commit, tick, log.

### Latest-working log (fill-in as versions break)

| Component | Wanted | Used | Why |
|---|---|---|---|
| (none yet) | | | |

## [ ] Upgrade 2 — Host core and protocol sync

Port pipkin's `host/` core to fang, keeping the Wolfi launch model:

- Core: `main.ts`, `env.ts`, `channel.ts`, `mode.ts`, `detect.ts`, `config-lock.ts`,
  `framing.ts`, `models-json.ts`, `model-def.ts`, `scope.ts`, `tar.ts`, `prompt.ts`,
  `catalogue.ts`, `runtime.ts`, `session-file.ts`, `pipkin-extensions.ts`→`fang-extensions.ts`.
- New locks: `install-lock.ts`, `pid-lock.ts` (used by ensure and the install modes).
- New behaviors absorbed by the core (npm-cancel fixes, "reinstall Pi when its install is
  unfinished or missing files", config-lock semantics, i18n objects on host messages).
- **Keep fang-specific**: `session.ts` (login-shell env capture) and `synthetic-proc.ts`
  (re-homed under the TermX proc model in upgrade 9), plus fang-only modes
  (`backups.ts`, `default-model.ts`, `model-defaults.ts`, `provider-remove.ts`,
  `session-messages.ts`) and their tests, re-wired to the new core.
- `host/build.ts`: keep producing `fang-host.mjs`; license notices in the bundle; bundle the
  fang extensions.
- Tests: port pipkin's test suite (harness, global-setup fixture, new tests), rename
  `pipkin-*`→`fang-*`, keep fang's extra tests (`folder-search`, `synthetic-proc` — moved with
  the proc model).
- `app/.../host/` Kotlin side: `HostProtocol`, `HostClient`, `HostText` (host i18n keys),
  `HostLauncher` — whatever the new core requires.

Verify + commit + tick + log.

## [ ] Upgrade 3 — pi-extensions sync

- `fang-background/` (background tasks extension): port pipkin's current version.
- `fang-todo/`: port.
- `fang-web-access/`: fold in the newest upstream pi-web-access release per its `UPSTREAM.md`
  (many new providers: cloudflare-search, deepseek, field-extract, linkup, ninerouter,
  searchapi, tabstack, …, plus rate-limit and fix files), keeping Fang's local changes listed
  in `UPSTREAM.md`.
- `string-enum.ts` helper (port from pipkin).
- Extension wiring in `fang-extensions.ts`, `BundledExtension.kt`, Settings toggles,
  `update-extensions` mode.

Verify + commit + tick + log.

## [ ] Upgrade 4 — Theme rework: Cobalt, Manrope, launcher icons

Port pipkin's visual rework so everything later in this plan is built in the current theme:

- `Cobalt` palette as default + Porcelain/Clay/Obsidian/Stone theme set
  (`FangColors`, `values/colors.xml`, `values-night/colors.xml`, `themes.xml`).
- **Manrope** font (`manrope.ttf`, `codes` left-to-right/tabular rules) + JetBrains Mono kept.
- Per-theme **launcher icons** (`ic_launcher_{clay,cobalt,obsidian,stone}`, mipmaps,
  `launcher_icon.xml`, theme-picked icon in the launcher).
- UI rework as it landed in pipkin: Cobalt header/dialogs/sheets/snackbars/controls → Cobalt
  sidebar/workspace tiles/tabs sheet → chat stream, composer, request surfaces → explorer, file
  view, diff cards → terminal key bar, settings, archive, onboarding, sign-in.
- Sidebar footer: "Fang Harness · Pi <version>" (folded into upgrade 1's version display).
- New icons needed: `ic_lucide_{external_link,list_checks,log_out,server,smartphone,wifi_off}`
  (those are used by later features too).

Verify + commit + tick + log.

## [ ] Upgrade 5 — Providers, subscriptions and the model list

- `host/src/runtime.ts`: fang's debug branch already added OpenRouter (API key,
  `openai-completions` base) — merge with pipkin's newer model: `subscription-models.ts` mode,
  `model-list.ts` (provider `/models` lists, `hiddenUntilPicked`), so OpenRouter/Z.ai/Kimi keys
  expose a model list the user picks from.
- `ic_brand_openrouter.xml` (pipkin's asset, canonical), provider logos and `Subscription.kt`
  rows, `ProviderEditor`/`ModelEditor` screens synced with pipkin, `provider-models`/
  `provider-save`/`subscription-save` modes synced, `docs/providers.md` adapted.
- Unit tests for the model-list behavior.

Verify + commit + tick + log.

## [ ] Upgrade 6 — Subagents

- Host: `subagent-folders.ts`, `modes/subagent-sessions.ts`, and the "give subagents the app's
  system prompt before their agent's" behavior (`prompt.ts`).
- App: subagents UI (sheets/screens pipkin added — discovered by diffing `ui/` during work),
  `strings_subagents.xml`.
- Tests: `pipkin-subagents.test.ts`→`fang-subagents.test.ts` + `subagents-harness.ts`.

Verify + commit + tick + log.

## [ ] Upgrade 7 — Local models (LiteRT-LM + llama.cpp + OpenCL)

The biggest pipkin feature fang lacks entirely.

- Native: `app/src/main/cpp/llama/` (llama-cpp JNI, pinned source archives via the new
  `PinnedSources` gradle task), `app/src/main/cpp/opencl/` (loader + probe), `CMakeLists.txt`.
- `app/litertlm-rules.pro` (R8 rules), the LiteRT-LM Gradle dependency, AIDL
  (`IInferenceListener`, `IInferenceProcess`), the separate inference process + service
  (`AndroidManifest.xml`).
- Host: `modes/local-models-sync.ts`, `model-list.ts` interplay.
- App UI: local-models screens (manage/download/use on-device models),
  `strings_local_models.xml`, `strings_local_runtime.xml`.
- Docs: `docs/local-models.md` (adapted). Licenses: llama-cpp, litert-lm, kleidiai,
  cpp-httplib, miniaudio, nlohmann-json, opencl-headers, stb-image, rotate-bits, manrope,
  markdown-composer.
- **Requires the NDK** in Actions (`ndk;30.0.16248370` + `cmake;4.1.2` in `ci.yml`); the native
  sources must compile for both `arm64-v8a` and `x86_64`.

Verify + commit + tick + log.

## [ ] Upgrade 8 — Remote hosts

- Host: `src/remote/` package, modes `remote-connect`, `remote-folder`, `remote-hosts`,
  `remote-install`; host-password sealing that doesn't crash when the Keystore can't seal
  (pipkin commit `cb38e68`); "check a remote chat's host while it's on screen" + lost-host banner
  (`4d2433d`).
- App: remote UI (host list, connect flow, password storage), `strings_remote.xml`,
  the server/smartphone/wifi-off icons.
- Tests: `remote-*.test.ts` suite + `sshd.ts` fixture + `scripts/ssh-test-host.sh`.
- Docs: `docs/remote-hosts.md` (adapted).

Verify + commit + tick + log.

## [ ] Upgrade 9 — Terminal on TermX (the inner core terminal)

Goal: the terminal in Fang Harness runs on TermX's runtime core. Not the old Wolfi terminal
(`wolfi-terminal`, only its extractor remains, already adapted), and not pipkin's Termux
handling (never ported).

- **proot command** (`WolfiRuntime.command()` → a TermX-style `ProotCommand`): storage-tier
  binds (`T0` private only → bind shared storage only when All-files access is active),
  hardening `require`s (regular file, link count, path containment) copied from TermX.
- **proc config** (the "behaves like a real device" part): TermX's `FakeProcfs` sysdata set —
  `stat, vmstat, meminfo, uptime, loadavg, cpuinfo, version` — bound **only where the real
  file is blocked** (TermX's `bind_if_blocked`), replacing fang's current static four-file
  synthetic `/proc`. Port `host/src/synthetic-proc.ts` semantics into it (the host still
  rewrites the files while it runs; keep the rewrite, change the bind model).
- **DNS**: TermX's `AndroidDnsResolver` — write the device's DNS servers to a private resolver
  file and bind it at `/etc/resolv.conf` (fixes network tools that ignore Android DNS).
- **Permission errors incl. SN1 and SCOM**: fang's current `SHARED_MOUNTS` unconditionally
  binds `/sdcard`, `/storage`, `/data`, `/dev`, `/proc`, `/sys`. Without All-files access the
  `/sdcard`-family binds fail with permission errors; with it, `/data`-family binds are
  meaningless and some OEMs refuse them — these unconditional binds are the SN1/SCOM-class
  permission errors the user reported. Fix = TermX's tiered, point-of-need permission model:
  request `MANAGE_EXTERNAL_STORAGE` only when needed, with a one-line reason and a
  Storage-Health-style status (which tier is active, what is bound), translate EACCES into that
  one line. Record each old error signature and its fix in AGENT.md as I verify them.
- **Session behavior**: TermX's foreground service with session notifications (Open / New
  session / Stop all) where fang's terminal sessions need to keep living — evaluate against
  fang's existing `AgentService`/`KeepAlive` and port only what's needed for correct
  background behavior (record the decision in AGENT.md).
- **Input**: TermX's tap-hold Ctrl/Alt/Shift one-shot-modifier behavior and IME composition
  handling for the terminal key bar (fang's key bar is pipkin's older one — `TerminalKeyBar`
  was part of upgrade 4's rework; fold TermX's input fixes in without duplicating state).
- Keep: `libghostty-android` rendering, the host `terminal` mode (synced in upgrade 2), and
  `wolfi-core` (builds `libproot.so`, PRoot 5.1.0 — source-identical to TermX's PRoot,
  verified by tree diff).
- Cleanup: `licenses/wolfi-terminal.txt` stays only if the adapted `RootfsExtractor` stays
  (it does — update its header to name both sources if I also adopt TermX's extractor
  improvements).

Verify + commit + tick + log.

## [ ] Upgrade 10 — Docs and site

- `docs/`: port `local-models.md`, `remote-hosts.md`, `privacy.md` (privacy is new for fang —
  Google-Play-relevant, updated for the Wolfi/TermX reality and dated); adapt
  `background.md`, `get-started.md`, `configure.md`, `extensions.md`, `providers.md`,
  `acknowledgements.md`, `index.md` (every "Termux" sentence → Wolfi OS + TermX core).
- `site/`: theme menu (Porcelain/Obsidian/Clay/Cobalt/Stone + System, picked-theme icon in the
  title and launcher tile), phone-demo parity (foldable steps, widgets tab, placeholder),
  `LauncherTile`, `DocsSiteTitle`, `DocsThemeProvider`, `theme.css`/`theme-init.js`,
  per-theme favicons, Manrope woff2, `licenses.txt`, screenshots (fang has one set — map them
  to all theme variants, note it).
- README/AGENTS/SECURITY parity (adapted).

Verify (site `pnpm run check`) + commit + tick + log.

## [ ] Upgrade 11 — Release pipeline parity and final gate

- `.github/workflows/release.yml`: bring to pipkin's current shape (release-numbered after the
  latest tag, `check`+`assembleRelease` in `build` with no secrets, key-holding `sign` that
  verifies the signed APK, `publish`), keeping Fang's signing variables and the
  "no key → fail" behavior fang already added.
- `scripts/sign-release-apk.sh` parity (pipkin's current checks: version, signature, 16 KB
  alignment, the runtime the app needs).
- Keep fang-only `ci.yml` (the push gate) and `watch-actions.yml`. Skip pipkin's
  `sign-play-bundle.sh`/`upload-play-bundle.sh` — Fang publishes GitHub releases only (note in
  AGENT.md).
- Final: every upgrade ticked; host checks green locally; the last `debug` push green in
  Actions (check + APK); AGENT.md and this file complete and current; **merge `debug` into
  `main` and push** — that push triggers `release.yml` (signed APK published as the next
  `v0.1.N`); watch the release run to green too.

---

## Status

- **Current task:** (updated after every step)
- **Next:**
- **Blockers:**
- **Log:** (one line per commit)