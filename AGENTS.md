# Fang Harness

## Branches and worktrees

This is a private repository with a single `main` branch: there is no upstream to contribute to and no
second trunk to promote.

Work in its own git worktree rather than in the main checkout, so a half-finished change never lands
on `main`. Create it next to the main checkout on a branch, for example
`git worktree add ../fang-<branch> -b <branch> main`, and leave the main checkout on `main`.

Before building, bring the branch up to date with `git fetch origin && git rebase origin/main`, so
every build you test is the current `main` plus your work. Resolve any conflicts, and if changes
someone else landed on `main` break yours or don't work with them, fix that in your branch before you
build.

Finished work goes to `main` directly; this project doesn't use pull requests. Rebase the branch onto
`origin/main` and run `git push origin HEAD:main` from the worktree. Never force push, to any branch,
with `--force`, `--force-with-lease`, or a `+` refspec. If the push is rejected because `main` moved,
fetch, rebase onto `origin/main` again, fix any conflicts or breakage, and push again. Worktree
branches stay local: never push them to the repository under their own names.

Once the push has landed, remove the worktree and its branch so they don't pile up on this machine.
Confirm the work is on `main` with `git fetch origin && git merge-base --is-ancestor HEAD origin/main`,
then from the main checkout run `git worktree remove ../fang-<branch>` and `git branch -D <branch>`.
Finally, bring the main checkout up to date with `git pull --ff-only origin main`.

## Testing on devices

Test changes on an Android emulator. Several agents work at once, so each worktree gets its own emulator, and agents never share one: installs, app data, and taps from one agent would otherwise overwrite or interrupt another's test. Don't install on, drive, or shut down an emulator that another worktree or project is using.

Before testing, create an emulator for the worktree, named after its branch, with the Pixel 9 hardware profile and a system image under `~/Library/Android/sdk/system-images`, for example `echo no | avdmanager create avd -n fang-<branch> -d pixel_9 -k "system-images;android-36;google_apis_playstore;arm64-v8a"` (the `echo no` answers its custom hardware profile prompt). Without `-d`, the emulator gets a 320×640 screen that Fang Harness's UI and `adb` taps don't fit. Boot it on a free even port with `emulator -avd fang-<branch> -port <port> -memory 4096`, and pass `-s emulator-<port>` to every `adb` command so it can't reach another agent's emulator or the phone. When you remove the worktree, shut the emulator down with `adb -s emulator-<port> emu kill` and delete it with `avdmanager delete avd -n fang-<branch>`.

To chat with a model on the emulator, run `scripts/emulator-llm.sh emulator-<port>` once Fang Harness Dev's setup has installed the agent, or add `--release` to sign in the release build instead. It signs Fang Harness in to the shared codex-lb provider, with the API key stored in the Mac's Keychain under `fang-codex-lb`, and makes GPT-6-Astra the default model. It always goes through Fang Harness Dev, the debuggable build, so Fang Harness Dev has to be installed and set up even for `--release`. Never print the key or copy it anywhere else.

## Release and debug builds

The debug build is Fang Harness Dev, a separate app that installs beside the release, so the user can test it on a phone that runs the release. The package suffix is set in `app/build.gradle.kts`. Both builds share one Termux, and each keeps all of its state there under its own names, from `TermuxBuild` in `app/src/debug/kotlin` and `app/src/release/kotlin` (the helper's ports through `HelperProtocol.PORTS`):

| | Release | Debug (Fang Harness Dev) |
|---|---|---|
| Package | `io.leloush.fang` | `io.leloush.fang.debug` |
| Host and pinned Pi (`FANG_ROOT`) | `~/.local/share/fang` | `~/.local/share/fang-dev` |
| Pi's config and sessions (`PI_CODING_AGENT_DIR`) | `~/.fang/pi` | `~/.fang-dev/pi` |
| Backups, background tasks | `~/.fang/backups`, `~/.fang/background` | `~/.fang-dev/backups`, `~/.fang-dev/background` |
| Play-Termux helper, its ports, boot script and command | `~/.fang/helper`, 47310–47314, `~/.termux/boot/fang-helper`, `fang-start` | `~/.fang-dev/helper`, 47320–47324, `~/.termux/boot/fang-dev-helper`, `fang-dev-start` |

Keep it that way when you add state in Termux: derive the path from `TermuxBuild.NAME`, from Pi's config folder (`PI_CODING_AGENT_DIR`, Pi's `getAgentDir()`, or the host's `env.configDir`), from `FANG_ROOT`, or from the helper's own folder, never from a fixed `~/.fang`. Anything the app launches by component, such as `am start`, takes the build's package name from `context.packageName`; the activity class stays `io.leloush.fang.MainActivity` in both. The one shared piece is the shell hook in `~/.bashrc`, `~/.zshrc` and fish's `conf.d/fang.fish`, which both builds' setups write the same way: it starts every installed helper, so a session either build opens serves both. The user-facing docs describe the release's paths; when you test or debug on an emulator, Fang Harness Dev's are the ones in use, for example `adb -s emulator-<port> shell run-as io.leloush.fang.debug`.

The user's phone is sometimes reachable over `adb`, but it's their own phone, not a test device. Ask the user before you install a build on it, run commands on it, or test on it, and do only what they agreed to. Only ever install the debug build, Fang Harness Dev (`io.leloush.fang.debug`), on the phone: the release build there is the user's own, installed from a published release and signed with the release key, so never install, replace, or uninstall `io.leloush.fang` on it or touch its data. Never change settings on the phone, whether in Android, Termux, Fang Harness, or any other app, without asking first.

## Code you touch

Leave code you change better than you found it, and limit the cleanup to that code: don't refactor whole files or unrelated parts of the codebase. Where a change touches a function or block, flatten its nesting where you can, using early returns and guards and reordering conditions so the common path reads straight down, and simplify what's there. Some nesting is unavoidable; keep it when flattening would make the code harder to follow. Rename variables, properties, and functions in that code whose names no longer match what they hold or do, or could say it more precisely.

## Checks

A task isn't done until `./gradlew check` passes. It runs ktlint, detekt, Android lint, the unit tests, and the host's type check, lint, and tests, and every release build runs it first, so a failure on `dev` blocks the next release. Fix any failure it reports, including ones in code you didn't change, rather than leaving it for later. Fix the code instead of silencing a check: don't add `@Suppress`, lint or detekt baselines, ignore comments, or configuration that turns a rule off.

## Text the user sees

Fang Harness is set up for translation, so every text the app itself shows comes from a string resource in `app/src/main/res/values/strings_*.xml`: labels, errors, notices, notifications, and snackbars alike. Text from Pi or the model is shown as Pi sends it. In Compose, use `stringResource` or `pluralStringResource`; code without a `Context` uses `AppStrings`. An exception whose message reaches the UI, like `HostFailure` or `RpcError`, resolves its message when it's read, so it's never stored in English. Give a string with placeholders a comment saying what each one holds. Format numbers, times, and dates with the locale (`NumberFormat`, `DateFormat`, `DateUtils`), not fixed patterns or English suffixes.

The Termux host writes its messages in English, and each one the app may show also carries an `i18n` object, `{ key, args }`, beside the English: `i18n` on a failure, `<field>I18n` beside a text field such as `error` or `line`. `app/src/main/kotlin/io/leloush/fang/host/HostText.kt` maps each key to a resource in `strings_host.xml` and falls back to the English for a key it doesn't know. When the host gets a new message, add its key, the mapping, and the resource together.

Debug builds include pseudo-locales. Set Fang Harness's language to English (XA) to find text that skips resources, which stays plain English while everything else turns accented, or to Arabic (XB) to check right-to-left layouts, for example with `adb -s emulator-<port> shell cmd locale set-app-locales io.leloush.fang.debug --locales en-XA`.

## Commits

Write commit messages in the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) format: `<type>(<optional scope>): <description>`, for example `feat(composer): add a context window meter next to the send button`. Use `feat` for new behavior, `fix` for bug fixes, `docs` for documentation-only changes, and `refactor`, `perf`, `test`, `build`, `ci`, or `chore` where they fit. Mark breaking changes with `!` after the type or scope and a `BREAKING CHANGE:` footer.

Commits are signed through 1Password, so commit normally first. If 1Password doesn't allow the signature, for example because the request times out or the agent returns an error, commit again without signing using `git -c commit.gpgsign=false commit`. Don't change the repository's or your global Git signing settings to get around it.

## Third-party licenses

Before adding a dependency, whether a Gradle dependency in `app/build.gradle.kts`, a host package in `host/package.json`, or a Pi runtime package in `app/src/main/assets/pi/package.json`, check its license. Don't add copyleft code (GPL, LGPL, AGPL) without asking first, since it would change how Fang Harness can be distributed.

When you copy or adapt code, icons, or other assets from another project, keep a header in the copied file naming the source, version, and license, as the Lucide icons in `app/src/main/res/drawable/ic_lucide_*.xml` do.

The APK has to carry the license of everything third-party that it ships, because Android's resource compiler and esbuild strip the comments those headers live in. Settings › About › Open-source licenses lists every `.txt` file under the APK's `licenses/` and `syntax/licenses/` assets, so when you add or update something that ships in the app, update its license file there too:

- Copied code, icons and fonts, and native libraries from Gradle dependencies: a file in `app/src/main/assets/licenses/`, starting with a line naming the source, version, license, and what Fang Harness uses it for. Apache-2.0 Android libraries are listed together in `android-libraries.txt`.
- Packages bundled into the host or a bundled extension: `host/build.ts` writes their license files and puts their notices at the top of the bundle.
- tree-sitter and the grammars: the `syntaxSources` task in `app/build.gradle.kts` copies each source's license, plus the `notices` of code inside a source that its own license doesn't cover.

The website serves its own notices in `site/public/licenses.txt`; update it when the site starts serving another project's code, fonts, or icons.

## Vendored pi-web-access

The `fang-web-access` extension in `host/src/pi-extensions/fang-web-access/` is a copy of the upstream pi-web-access package with a few Fang Harness changes. Before updating it to a new upstream release or editing it, read `UPSTREAM.md` in that folder: it lists every difference from upstream and the steps for folding in a release.
