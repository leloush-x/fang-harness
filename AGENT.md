# AGENT.md — Fang Harness architecture and changelog

This is the permanent record: how the app is built, how it is tested, the conventions, how the
TermX-style terminal core works, and a changelog of every upgrade made in the port to current
pipkin. For the running plan (current task, next, blockers), see `CHECKPOINTS.md` — re-read it
before every step and keep it current.

## What this is

Fang Harness is an Android app (package `io.leloush.fang`, UI in Jetpack Compose, Kotlin) that
binds a terminal to the pi coding CLI. The terminal's working directory is always the pi
working directory, so pi can see everything the user sees, and vice versa (AGENTS.md: "one
agent, not two").

## Module layout

```
wolfi-core/     Kotlin + CMake: builds PRoot (5.1.0, from the `proot-5.1.0` source in
                `src/main/cpp/`) as `libproot.so` and the 32-bit `libloader.so` for
                x86-64→i686. Its CMake also copies the loader's ELF into `app/src/main/assets/elf/`,
                so the APK stays self-contained. Source tree matches TermX's PRoot exactly.
app/            The Android app.
  src/main/kotlin/io/leloush/fang/
    MainActivity.kt          Window binding, navigation between the app phases.
    ui/                      Compose UI: App.kt, ChatPane (stream/request/composer/surfaces),
                             SidebarPane (workspace tabs), SettingsPane, OnboardingPane (Pi install),
                             SigninPane, ArchivePane, TerminalKeyBar, KeyBindDialog, …
    data/                    State owners: ChatController, ChatSessionScope, ChatStore,
                             RuntimeController, RuntimePaths, TerminalSessions, GitStore,
                             ProviderCatalogue/Draft/Models, Subscription*.k, SessionHistory/Scope,
                             KeystoreStore, AppFiles, SharedStore, SharedText, Folders,
                             ThemeStore, TerminalKeys, FileText, …
    runtime/                 Android launch side: HostHandle, RuntimeStart (assembles the environment
                             for the host process), ProotShell, WolfiRootfs.
    host/                    The other half of the protocol: HostLauncher (spawns the host child),
                             HostProtocol/HostClient (frame channel to the host), HostText (messages
                             that ship with the host bundle, resolved against the host's locale).
  src/main/assets/
    pi/                      The pi runtime, pinned: `package.json` pins
                             `@earendil-works/pi-coding-agent` (currently the latest),
                             bundled `pi-extensions/` (fang-background, fang-todo, fang-web-access)
                             with `UPSTREAM.md` + `LICENSE-MIT` per extension.
    elasticsearch/           FTS5 SQL for the file-index `file.db`.
    licenses/                One file per third party bundled into the APK.
host/            The in-runtime host: TypeScript, ES2022, strict, `type: module`. Bundled by
                esbuild (`host/build.ts`) into `fang-host.mjs` inside the APK.
  src/main.ts      Host entry: the mode router, the host environment (`FANG_*` variables),
                   the channel/framing protocol, session-file locking and installs.
  src/modes/       One file per mode (terminal, ensure, files, folders, git, sessions,
                   provider-*, subscription-*, backup, browse, rpc, utility, …).
  src/fang-extensions.ts  Loads the bundled extensions into the pi instances the host starts.
  src/synthetic-proc.ts   Rewrites stat/vmstat/loadavg/uptime inside the synthetic /proc while
                   the host is live (paired with the /proc bind model in app/...WolfiRuntime).
  test/            Vitest suite + `harness.ts` (spawns the real host on a mocked runtime) and
                   `global-setup.ts` (the test fixture).
site/            The docs/marketing site: TypeScript, esbuild, no framework.
docs/            The docs the site renders (get-started, configure, providers, extensions,
                 background, acknowledgements, …).
scripts/         sign-release-apk.sh and friends; CI fixtures.
```

## How the app talks to pi

1. **Two processes in the rootfs.** The host (`fang-host.mjs`) is a child of the app process,
   started by `HostLauncher` through proot (`WolfiRuntime.command()`). It talks to the app over
   its stdio with a framing protocol (`channel.ts`↔`HostProtocol`), mode-based (`MODE_NAME` per
   file in `src/modes/`), JSON-RPC-ish.
2. **pi is a grandchild.** When the chat runs pi, the host spawns pi (`session.ts` captures the
   login shell's environment first, so what the user's profile exports is what pi sees). The
   host also owns the pi sessions on disk (jsonl), the settings file, the provider configs,
   the backup, and the `file.db` search index.
3. **The terminal is pi's working directory.** `TerminalSessions` keeps one host `terminal`
   session (a shell started through the same proot environment) per workspace tab, rendered by
   `libghostty-android`. `WolfiRuntime` builds the rootfs: the pinned Wolfi distro tarball,
   the bundled PRoot, the Node.js install (`nodejs-24`/latest), the pi install, and the
   synthetic `/proc` bind.

## Build and test

- **Local inner loop (host side):** `cd host && pnpm run check` (typecheck, lint, test). There
  is no system pnpm here, so the shim is a file that puts the pnpm from
  `npm install -g pnpm` on `PATH`:
  ```
  cd /tmp && mkdir -p bin && ln -sf "$(npm root -g)/pnpm/bin/pnpm.cjs" bin/pnpm
  ```
  then `PATH=/tmp/bin:$PATH pnpm run check` in `host/`. (Wolfi's npm needs the proxy shim for
  the registry; once packages are installed the checks run offline.)
- **The gate:** push to `debug` (or any branch but main) and GitHub Actions `ci.yml` runs
  `./gradlew check :app:assembleDebug` on `ubuntu-24.04` — ktlint, detekt, unit tests, plus
  verifying `assets/fang-host.mjs` and the `libproot.so`/`libloader.so` for `arm64-v8a` and
  `x86_64` are inside the APK. **APKs build in Actions, never locally** (decision, 2026-12-01).
- **Release:** a push to `main` runs `release.yml`: build (no secrets) → sign (key from the
  `release` environment) → publish (a GitHub release `v0.1.N`, N one higher than the latest,
  recorded in `gradle.properties` as `fang.versionName`).
- **Site:** `cd site && pnpm run check` (lint, typecheck, build).
- Gradle: the wrapper (see `gradle/wrapper/gradle-wrapper.properties`) pins the exact
  distribution sha; `gradle.properties` sets caching and the jvm heap.
- **Never run `apk add` on the user's Wolfi runtime here while fiddling with the build** —
  this very runtime is the one the build tools live on; keep it clean.

## Conventions

- **Identity:** the user names this **Fang Harness**. Every string that pipkin says as
  "Pipkin" says "Fang Harness"; packages are `io.leloush.fang` / `io.leloush.fang.host`; env
  prefix is `FANG_`; the host bundle is `fang-host.mjs`; extensions are `fang-*`.
- **Kotlin:** ktlint + detekt as configured; no star imports; Compose for all UI; state is
  owned by the `data/` classes and observed via `StateFlow`, Compose reads them.
- **Host TypeScript:** strict, ESM, every file that ships has a header comment; messages are
  static (the UI resolves them through `HostText` against the host's locale — never format a
  host message in Kotlin); no `console.log` on the happy path.
- **Latest versions only:** the pi runtime, Node (Wolfi's `nodejs-latest`), and every
  dependency in the install path are always the newest version; the home screen shows what is
  installed and the install path always upgrades to it. Where a newest version genuinely
  breaks the build/tests/CLI, the newest working version is used instead and **the reason is
  recorded in the Latest-working log of `CHECKPOINTS.md` and here in the changelog**.
- **Licences:** bundled third-party code lives with one notice file each in
  `app/src/main/assets/licenses/` and a line in `site/public/licenses.txt`. No GPL/LGPL/AGPL
  under an MIT umbrella (AGENTS.md): llama.cpp stays MIT-licensed build, OpenCL is a system
  library, pi extensions are local copies.
- **Commit:** Conventional Commit, imperative, scoped; body states the why; the user commits —
  but the user's operating model is that *I* drive the loop (implement → verify → commit →
  tick → log), so these commits are made with the user's standing instruction to do so.

## The terminal: TermX, not the old Wolfi terminal, not Termux

The inner terminal of Fang Harness runs on **TermX's runtime core** (
`github.com/leloush-x/termx`, v0.8.13 as of 2026-12-01). Nothing from the old Wolfi terminal
(`github.com/leloush-x/wolfi-terminal`, `com.rk.terminal`) beyond the already-adapted
`RootfsExtractor.kt` in `wolfi-core`; nothing from pipkin's Termux handling (the Play-helper
AIDL, `termux-session.ts`, the Termux app dependency) — Fang Harness has no Termux. The
Ghostty rendering (`libghostty-android`) and the host `terminal` mode stay; what changed is
how the proot environment under them is built.

### Proot command (TermX model)

`WolfiRuntime.command()` builds the launch the way TermX's `ProotCommand.build` does:

- Binds come from a **storage tier** (`StorageTier`): private only by default; the shared
  storage (`/sdcard`-family) is bound **only while All-files access
  (`MANAGE_EXTERNAL_STORAGE`) is active** — no more unconditional binds that fail with
  `EACCES`/permission errors on most devices.
- Hardening on every bind source (copied from TermX): the source must be a regular file
  (or directory where a directory is needed), link count 1 (or -1), and contained in the
  tree it claims to be in.
- The guest working directory is validated (absolute, no `..`, length) and created in the
  rootfs before launch; the shell is `bash` (the Wolfi rootfs's).
- The `PROOT_LOADER` env is set, `PROOT_TMP_DIR` is the app's temp dir, and the launch goes
  through `libproot.so` with `libloader.so` for 32-bit.

### The proc config (behaves like a real device)

The Wolfi distro's `/proc` is unreadable under proot on most Android kernels. TermX
handles it in two steps and so do we:

1. A **fake `/proc` (sysdata) directory** on the host side, refreshed before launch, holding
   the files TermX fakes: `stat`, `vmstat`, `meminfo`, `uptime`, `loadavg`, `cpuinfo`,
   `version`. (Fang previously shipped only four of the seven and always, even where the real
   files were readable — the extra two made `free`/`df` report nonsense.)
2. **Bind-if-blocked:** each fake file is bound over the real one **only if the real file
   cannot be read** from the app (`real.canRead() && readBytes().isNotEmpty()` check, as in
   TermX). Where the real `/proc` works (common on `userdebug` and many production kernels),
   the environment sees the real, live values — `top`, `free` and `lshw` behave. Where it is
   blocked, the app falls back to the fakes, which keep processes usable.
3. The host's `synthetic-proc.ts` still **refreshes the fake files while it is live** (it
   rewrites them from the values it can see), so a long session does not serve stale numbers.

### DNS

TermX's `AndroidDnsResolver`: read the device's configuration, write the DNS servers to a
private file, and bind that file at `/etc/resolv.conf` inside the rootfs (replacing the
distro's, which on Android points at nowhere). This is what makes `curl`, `ping -4`, and
every tool that reads the resolver file work out of the box — the old behavior was that
"network tools hang unless the user edits resolv.conf".

### Permission errors and how they were fixed

The permission errors in the old terminal (the SN1/SCOM class the user reported) came from
binding shared and system paths unconditionally:

| Old error class | Signature | Cause in the old code | Fix (TermX model) |
|---|---|---|---|
| /sdcard class | `EACCES` binding `/sdcard` into the guest | no All-files access, bind attempted anyway | tiered binds: shared paths only when `MANAGE_EXTERNAL_STORAGE` is active; the error surfaces as a one-line status ("grant All-files in Settings → Storage"), not a crash |
| /data class | bind refused / pointless on a real device | `/data` was bound into the guest unconditionally | never bound by default; only the app's own private dirs are |
| /dev class | `EACCES` on device nodes under proot | `/dev` bound even where the kernel allows it | bound only when the real one is not usable, with the TermX hardening |
| resolv.conf | network tools hang | the distro's `resolv.conf` is a stub | the AndroidDnsResolver bind (above) |

(Each signature gets a row with its device and fix as it is verified on the user's hardware;
this table is the running record. The user's own names for the errors — SN1, SCOM — are kept
as the report names in `CHECKPOINTS.md` and mapped to the classes here.)

### Session behavior

TermX keeps sessions alive with a foreground service + notification (Open / New session /
Stop all). Fang Harness keeps its own `AgentService` model; the piece we take from TermX is
the *session lifecycle semantics*, not the notification UI (decision 2026-12-01, revisited
when background behavior is verified on the user's device).

## Changelog (this port)

### Upgrade 0 — Baseline (2026-12-01)

- Plan written: `CHECKPOINTS.md` (all upgrades, dependency order, "latest versions only"
  first, the latest-working rule) and this file. Working branch: the existing `debug`
  (`main` `53e81d1` + `ab935a4` OpenRouter providers) — adopted as the branch the user asked
  for; `main` is not touched until every upgrade is green.
- Reference pinned: pipkin `9bf5767` (`dev`==`main`); TermX `4017d3f` (v0.8.13).
- Decision: APKs build in GitHub Actions (`ci.yml` on every non-main push, `release.yml` on
  main), not locally.
</content>
</invoke>