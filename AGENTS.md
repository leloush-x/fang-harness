# Fang Harness

A private copy of an Android AI-coding harness. The app is Jetpack Compose; the agent runs in
Termux, driven by a TypeScript host that the app ships as an asset.

## Branches and worktrees

A private repository with a single `main` branch: no upstream to contribute to, no second trunk to
promote, no pull requests.

Work in a git worktree rather than the main checkout, so a half-finished change never lands on
`main`: `git worktree add ../fang-<branch> -b <branch> main`, leaving the main checkout on `main`.
Rebase onto `origin/main` before building, so every build you test is the current `main` plus your
work. When the work is tested, `git push origin HEAD:main` from the worktree. Never force push, to
any branch, with `--force`, `--force-with-lease`, or a `+` refspec. Worktree branches stay local.
After the push lands: `git merge-base --is-ancestor HEAD origin/main`, then
`git worktree remove ../fang-<branch>` and `git branch -D <branch>`, then
`git pull --ff-only origin main` in the main checkout.

## Commands

```sh
./gradlew check                     # ktlint + detekt + Android lint + unit tests + host check
./gradlew :app:assembleDebug        # debug APK
./gradlew :app:testDebugUnitTest --tests "io.leloush.fang.host.HelperProtocolTest"
cd host && pnpm run check           # typecheck + lint + test
cd host && pnpm vitest run test/helper-protocol.test.ts
cd site && pnpm run check
```

`./gradlew check` is the gate: it runs `:app:check` (ktlint, detekt, Android lint, unit tests) plus
`:app:hostCheck`, which shells out to `pnpm run typecheck && pnpm run lint && pnpm run test` in
`host/`. The first build needs the network: it downloads the pinned tree-sitter grammars
(`syntaxSources`) and the JDKs Gradle provisions for itself.

## The app talks to the host over a socket, not a library

The host is a separate Node program that runs **inside Termux**. The app ships it as the APK asset
`fang-host.mjs` and pushes it to Termux on demand; `HostLauncher` starts it through Termux's
`RunCommandService` (F-Droid build) or through the app's own in-Termux helper (Google Play build,
which has no `RunCommandService`).

The connection is **connect-back**: the app opens a loopback listener, the host dials in and
presents a single-use 32-byte token, compared in constant time. The wire format is hand-rolled JSONL
(`host/src/framing.ts`), not `readline` — `readline` also splits on U+2028/U+2029.

Both sides must agree on the message types, which are all `fang_`-prefixed: `fang_hello`,
`fang_request`, `fang_result`, `fang_progress`, `fang_exit`, `fang_channel`, `fang_auth_*`. The
protocol constants live in `app/src/main/kotlin/io/leloush/fang/host/HostProtocol.kt` and
`host/src/main.ts`; change one, change the other.

Environment the app sets for the host: `FANG_ROOT` (the bundle and the pinned Pi runtime),
`FANG_TOKEN` (the connect-back token), `PI_CODING_AGENT_DIR` (Pi's own config, sessions and
user-installed packages — Pi's, not ours).

## The product name is load-bearing

"Fang" appears in places that break in different ways. Renaming it means touching all of them:

| Where | Example |
|---|---|
| Package and application id | `io.leloush.fang`, `io.leloush.fang.debug` |
| JNI symbols in `app/src/main/cpp/syntax.c` | `Java_io_leloush_fang_syntax_TreeSitter_*` |
| Host bundle and extension assets | `fang-host.mjs`, `fang-extensions/fang-todo.js` |
| Wire protocol | `fang_hello`, `fang_request`, … |
| Helper HMAC labels | `fang-helper-server`, `fang-helper-client`, `fang-helper-c2s`, `fang-helper-s2c` |
| Pairing key derivation | `fang-pair-key`, and the AAD `fang-pair\|<sid>` |
| Termux state | `~/.fang`, `~/.fang-dev`, `FANG_ROOT` |
| Pi runtime manifest | `fang-pi-runtime` in `app/src/main/assets/pi/package.json` |

The traps, all of which bit during the rename:

- **JNI names must match the Kotlin package exactly.** `io.leloush.fang.syntax.TreeSitter` is
  `Java_io_leloush_fang_syntax_TreeSitter_…`. Get it wrong and the native methods fail at runtime,
  not at build time.
- **The HMAC labels are the protocol.** Changing them changes every derived MAC, so the pinned
  vectors in `host/test/helper-protocol.test.ts`, `app/src/test/kotlin/io/leloush/fang/host/HelperProtocolTest.kt`
  and `app/src/test/kotlin/io/leloush/fang/host/PlaySetupTest.kt` must be recomputed. Recompute them
  from the implementation, not by hand.
- **Resource names cannot contain spaces.** The style is `Theme.FangHarness`, not `Theme.Fang Harness`.
- **The package rename reorders imports.** ktlint wants them lexicographic; `ktlint --format` fixes
  the order.
- **The Pi runtime manifest's `name` must match `package-lock.json`'s**, or `npm ci` refuses to run.
- **`app/src/main/assets/pi/package.json` and the vendored `fang-web-access` are separate from the
  host's own `package.json`**; each has its own lockfile.

## Release and debug builds

The debug build is Fang Harness Dev, a separate app that installs beside the release, so the user
can test it on a phone that runs the release. Both builds share one Termux and keep their own state
under their own names, from `TermuxBuild` in `app/src/debug/kotlin` and `app/src/release/kotlin`
(the helper's ports come from `HelperProtocol.PORTS`):

| | Release | Debug (Fang Harness Dev) |
|---|---|---|
| Package | `io.leloush.fang` | `io.leloush.fang.debug` |
| Host and pinned Pi (`FANG_ROOT`) | `~/.local/share/fang` | `~/.local/share/fang-dev` |
| Pi's config and sessions (`PI_CODING_AGENT_DIR`) | `~/.fang/pi` | `~/.fang-dev/pi` |
| Backups, background tasks | `~/.fang/backups`, `~/.fang/background` | `~/.fang-dev/backups`, `~/.fang-dev/background` |
| Play-Termux helper, ports, boot script, command | `~/.fang/helper`, 47310–47314, `~/.termux/boot/fang-helper`, `fang-start` | `~/.fang-dev/helper`, 47320–47324, `~/.termux/boot/fang-dev-helper`, `fang-dev-start` |

Derive every Termux path from `TermuxBuild.NAME`, from Pi's config folder (`PI_CODING_AGENT_DIR`,
Pi's `getAgentDir()`, or the host's `env.configDir`), from `FANG_ROOT`, or from the helper's own
folder — never from a fixed `~/.fang`. Anything the app launches by component (`am start`) takes
the package name from `context.packageName`; the activity class is `io.leloush.fang.MainActivity` in
both builds. The one shared piece is the shell hook in `~/.bashrc`, `~/.zshrc` and fish's
`conf.d/fang.fish`, which both setups write the same way: it starts every installed helper, so a
session either build opens serves both.

`minSdk` is 29, `targetSdk` 37, ABIs `arm64-v8a` and `x86_64`. The version comes from the
gradle properties `fang.versionName` and `fang.versionCode`, which the release workflow sets.

## Signing the release

Every push to `main` publishes a signed APK as GitHub release `v0.0.N`. The workflow has three
jobs: `build` (runs `./gradlew check :app:assembleRelease` with no secrets), `sign` (holds the key),
and `publish`.

The key lives in the repository's secrets and variables, not in the repo:

- secrets `RELEASE_KEYSTORE_BASE64` (base64 PKCS12) and `RELEASE_KEYSTORE_PASSWORD`
- variables `RELEASE_KEY_ALIAS` and `RELEASE_CERT_SHA256` (the certificate digest, hex, no colons)

**A release with no key configured now fails instead of publishing an unsigned APK**, because an
unsigned APK cannot be installed at all. If you need a new key, generate a PKCS12 with `keytool`
and set all four; keep the keystore and its password somewhere durable, because losing them means
the installed app can never be updated.

`scripts/sign-release-apk.sh` checks the signed APK carries the expected certificate, and the
attestation step is skipped automatically on a private repository.

## Testing on devices

Test on an Android emulator, one per worktree, never shared: installs, app data and taps from one
agent would otherwise overwrite or interrupt another's test. Create it with the Pixel 9 hardware
profile, for example `echo no | avdmanager create avd -n fang-<branch> -d pixel_9 -k "system-images;android-36;google_apis_playstore;arm64-v8a"` (the `echo no` answers its custom-hardware-profile prompt; without `-d` the emulator gets a 320×640 screen the UI and `adb` taps don't fit). Boot it on a free even port and pass `-s emulator-<port>` to every `adb` command so it can't reach another agent's emulator or the phone. Tear down with `adb -s emulator-<port> emu kill` and `avdmanager delete avd -n fang-<branch>`.

To chat with a model on the emulator, run `scripts/emulator-llm.sh emulator-<port>` once Fang
Harness Dev's setup has installed the agent, or add `--release` to sign in the release build
instead. It reads the API key from the Mac's Keychain and writes the provider into Pi's config in
Termux. The provider URL and the Keychain service come from `FANG_LLM_BASE_URL` and
`FANG_LLM_KEYCHAIN_SERVICE`, so point them at your own endpoint. Never print the key or copy it
anywhere else.

The user's phone is sometimes reachable over `adb`, but it's their own phone, not a test device.
Ask before you install a build on it, run commands on it, or test on it, and do only what they
agreed to. Only ever install the debug build (`io.leloush.fang.debug`); the release build there is
the user's own, so never install, replace or uninstall `io.leloush.fang` on it or touch its data.
Never change settings on the phone without asking first.

## This machine cannot run the heavy checks

Verify through CI, not locally. Specifically:

- **No Android SDK**, so `./gradlew` cannot build. Push and watch the run.
- **`tsc` is TypeScript 7's native compiler (`tsgo`)**, which panics on arm64 with a `noembed`
  error, so the host's `pnpm run typecheck` cannot run here.
- **`pnpm`'s launcher has no shebang**, so Node's `execFile("pnpm", …)` fails with `ENOEXEC` —
  which is what `host/test/global-setup.ts` does to build the bundle, so `pnpm run test` fails at
  setup. Put a shim earlier on `PATH`:
  `printf '#!/bin/sh\nexec node /usr/local/lib/node_modules/pnpm/bin/pnpm.mjs "$@"\n' > /tmp/bin/pnpm`.
- **Git loses loose objects on this filesystem.** Set `git config core.fsync "committed,pack"` before
  the first commit, and check `git fsck --no-progress` afterwards; a `git gc` on a repo with lost
  objects will fail.
- `oxlint` and `node build.ts` (the host bundle) do run locally, and are a fast sanity check after
  a rename.

## Checks

A task isn't done until `./gradlew check` passes. Fix any failure it reports, including ones in code
you didn't change. Fix the code instead of silencing a check: no `@Suppress`, no lint or detekt
baselines, no ignore comments, no configuration that turns a rule off. Note that detekt's
`MaxLineLength` is off and ktlint's `max-line-length` is not enforced under the `android_studio`
code style, so long lines are not what fails the build.

## Code you touch

Leave code you change better than you found it, and limit the cleanup to that code: don't refactor
whole files or unrelated parts of the codebase. Where a change touches a function or block, flatten
its nesting where you can, using early returns and guards and reordering conditions so the common
path reads straight down. Rename variables, properties and functions whose names no longer match
what they hold.

## Text the user sees

Every text the app itself shows comes from a string resource in `app/src/main/res/values/strings_*.xml`:
labels, errors, notices, notifications and snackbars alike. Text from Pi or the model is shown as Pi
sends it. In Compose, use `stringResource` or `pluralStringResource`; code without a `Context` uses
`AppStrings`. An exception whose message reaches the UI, like `HostFailure` or `RpcError`, resolves
its message when it is read, so it's never stored in English. Give a string with placeholders a
comment saying what each one holds. Format numbers, times and dates with the locale
(`NumberFormat`, `DateFormat`, `DateUtils`).

The Termux host writes its messages in English, and each one the app may show also carries an
`i18n` object, `{ key, args }`, beside the English: `i18n` on a failure, `<field>I18n` beside a text
field such as `error` or `line`. `app/src/main/kotlin/io/leloush/fang/host/HostText.kt` maps each key
to a resource in `strings_host.xml` and falls back to the English for a key it doesn't know. When the
host gets a new message, add its key, the mapping and the resource together.

Debug builds include pseudo-locales. English (XA) finds text that skips resources, which stays plain
English while everything else turns accented; Arabic (XB) checks right-to-left layouts, for
example `adb -s emulator-<port> shell cmd locale set-app-locales io.leloush.fang.debug --locales en-XA`.

## Commits

Conventional Commits: `<type>(<optional scope>): <description>`, for example
`feat(composer): add a context window meter next to the send button`. Types: `feat`, `fix`, `docs`,
`refactor`, `perf`, `test`, `build`, `ci`, `chore`. Breaking changes get `!` and a
`BREAKING CHANGE:` footer.

Commits are signed through 1Password. If signing times out or errors, commit again with
`git -c commit.gpgsign=false commit`. Don't change the repository's or your global Git signing
settings to get around it.

## Third-party licenses

Before adding a dependency — a Gradle dependency in `app/build.gradle.kts`, a host package in
`host/package.json`, or a Pi runtime package in `app/src/main/assets/pi/package.json` — check its
license. Don't add copyleft code (GPL, LGPL, AGPL) without asking first, since it would change how
Fang Harness can be distributed.

When you copy or adapt code, icons or assets from another project, keep a header in the copied file
naming the source, version and license, as the Lucide icons in
`app/src/main/res/drawable/ic_lucide_*.xml` do.

The APK has to carry the license of everything third-party it ships, because Android's resource
compiler and esbuild strip the comments those headers live in. Settings › About › Open-source
licenses lists every `.txt` under the APK's `licenses/` and `syntax/licenses/` assets:

- Copied code, icons, fonts and native libraries from Gradle dependencies: a file in
  `app/src/main/assets/licenses/`, starting with a line naming the source, version, license and what
  Fang Harness uses it for. Apache-2.0 Android libraries are listed together in
  `android-libraries.txt`.
- Packages bundled into the host or a bundled extension: `host/build.ts` writes their license files
  and puts their notices at the top of the bundle.
- tree-sitter and the grammars: the `syntaxSources` task in `app/build.gradle.kts` copies each
  source's license, plus the `notices` of code inside a source its own license doesn't cover.

The website serves its own notices in `site/public/licenses.txt`; update it when the site starts
serving another project's code, fonts or icons.

## Vendored pi-web-access

`host/src/pi-extensions/fang-web-access/` is a copy of the upstream pi-web-access package with a
few Fang Harness changes. Before updating it to a new upstream release or editing it, read
`UPSTREAM.md` in that folder: it lists every difference from upstream and the steps for folding in a
release.
