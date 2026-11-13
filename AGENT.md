# AGENT.md — Fang Harness

Architecture notes and changelog for the Fang Harness upgrade to match Pipkin.
Companion to AGENTS.md (agent operating manual) and CHECKPOINTS.md (task plan).

## Project identity
- Repo: github.com/leloush-x/fang-harness
- Package: `io.leloush.fang` (debug: `io.leloush.fang.debug`)
- Reference: github.com/jeph/pipkin (latest)
- Terminal core: github.com/leloush-x/termx (main branch), clone at `/root/core/termx`

## Architecture

### Runtime model
Fang Harness is self-contained: it ships Wolfi OS (rootfs) and PRoot (libproot.so / libloader.so) inside the APK
and runs the agent host inside that runtime. No external Termux app is required.

- App-private runtime lives under `files/wolfi/`: `rootfs/`, `runtime/` (host + pinned Pi), `pi/` (Pi config/sessions).
- `WolfiRuntime` (wolfi-core) downloads/unpacks Wolfi, installs Node.js + tools, writes the host bundle, and builds the PRoot command.
- The host is a Node program (`host.mjs`) shipped as an APK asset; `HostLauncher` starts it as a child through PRoot.
- Connect-back protocol: app opens a loopback listener, host dials in with a single-use 32-byte token (constant-time compare). Wire format is hand-rolled JSONL (`host/src/framing.ts`), not readline.
- Message types are `fang_`-prefixed and must agree between `app/.../host/HostProtocol.kt` and `host/src/main.ts`.

### Modules
- `app/` — Jetpack Compose UI, ViewModels, data, host client, terminal, syntax.
- `wolfi-core/` — PRoot runtime + Wolfi OS management (native cpp + Kotlin).
- `host/` — TypeScript host (esbuild bundle shipped as `fang-host.mjs`), with bundled Pi extensions under `host/src/pi-extensions/`.
- `config/` — detekt config.
- `site/` — Astro docs site.
- `scripts/` — release signing, emulator LLM helper.

### Terminal (Termx core)
The terminal uses Termx's core parts, NOT the old Wolfi terminal and NOT Pipkin's Termux handling:
- `FakeProcfs` — generates fake /proc/stat, /proc/meminfo, etc. so htop/top work (Android denies these to non-system apps).
- `StorageAccess` — T0/T1/T2 storage tier model (no permission / All-files / SAF).
- `BindTable` — builds PRoot binds; only binds shared storage when T1 is granted.
- `ProotCommand` (Termx) — PRoot launch with snapshot binds (bind_if_blocked: bind fake proc files only when real ones are unreadable).
- `AndroidDnsResolver` — writes Android's DNS servers to a private resolver file bound at /etc/resolv.conf.
- Ghostty (libghostty-android) remains the terminal widget.

Permission fixes:
- SN1 (/proc/stat permission denied) — fixed via snapshot binds + FakeProcfs.
- SCOM (storage permission) — fixed via T1 storage tier model (bind shared storage only when granted).

Proc config: FakeProcfs generates /proc/* content so the environment behaves like a real device.

### Build & test
- JDK 17+, Android SDK API 37, NDK 30.0.16248370, CMake 4.1.2.
- Node.js latest (22.19+ min) + pnpm.
- `./gradlew check` — ktlint + detekt + Android lint + unit tests + host check.
- `cd host && pnpm run check` — typecheck + lint + test.
- `./gradlew :app:assembleDebug` — debug APK.
- This machine cannot run heavy checks (no Android SDK, tsc panics on arm64). Verify via CI.

### Conventions
- Conventional Commits.
- All user-visible text from string resources (strings_*.xml); host messages carry i18n {key, args}.
- No @Suppress / baselines / ignore comments to silence checks.
- Third-party licenses tracked in app/src/main/assets/licenses/ and site/public/licenses.txt.
- Product name "Fang" is load-bearing (package, JNI, host bundle, wire protocol, runtime paths, Pi manifest).

## Changelog

### Pre-upgrade (baseline)
- fang-harness was an older fork of Pipkin, self-contained on Wolfi OS, with the old Wolfi terminal.

### Upgrade 1 — Latest versions only
(pending)

### Upgrade 2+ (pending)
(to be filled as upgrades land)
