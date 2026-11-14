# fang-ask-user: pi-ask-user, vendored

This folder is [pi-ask-user](https://github.com/edlsh/pi-ask-user) by Enzo Lucchesi, MIT License (see `LICENSE`), copied into Fang so that the `ask_user` tool works out of the box. Fang ships it as the extension `fang-ask-user`.

- Upstream version: 0.17.2, tag `v0.17.2`, commit `3d6a424ea65f0295ffe402eead3073c001a29468`.

## What differs from upstream

Everything below is a deliberate Fang change. Keep each one when folding in a release, unless upstream made it unnecessary.

### In this folder

1. `index.ts` no longer reads its version from `package.json`:
   - upstream: `import { createRequire } from "node:module";` / `const _require = createRequire(import.meta.url);` / `const ASK_USER_VERSION: string = (_require("./package.json") as { version: string }).version;`
   - Fang: `const ASK_USER_VERSION = "0.17.2";`
   - Why: `host/build.ts` bundles the whole extension into one asset, which no `package.json` sits beside, so upstream's lookup throws while Pi loads it.
2. `index.ts` names a choice's description in the dialogs it falls back to in RPC mode (Fang's mode): `askViaDialogs` appends `" — <description>"` to each choice's title and maps the answer back to the plain title. Upstream shows the bare titles there, so a host without Pi's terminal UI loses the descriptions. Fang's question card shows one line per choice, which is why the description shares the line.
3. Only upstream's top-level `*.ts` files and `LICENSE` are copied. Its `package.json`, tests, scripts, docs, media and `skills/ask-user` are left out: Fang installs extensions as source files, not as packages, so it has nothing to install a package's skills into.

### Outside this folder

- `host/build.ts` bundles `index.ts` into the APK asset `fang-extensions/fang-ask-user.js`, leaving `@earendil-works/*` and `@sinclair/typebox` out because Pi's loader hands an extension its own copies of them. The bundle starts with upstream's MIT notice, and the build writes `licenses/pi-ask-user.txt` for the app's license list.
- `BundledExtension.AskUser` in `app/src/main/kotlin/io/leloush/fang/data/BundledExtension.kt` installs it as `<root>/extensions/fang-ask-user/index.js`, which Settings can turn off and on like every other Fang extension.
- `host/src/modes/rpc.ts` leaves `fang-ask-user` out when Pi's global settings install `npm:pi-ask-user` or the repository from git, because Pi exits when two extensions register the same tools.
- `host/tsconfig.json` and `host/.oxlintrc.json` leave this folder out, because upstream's compiler settings are looser than the host's. Only esbuild checks it, when it bundles the extension.

## Updating to a new upstream release

Fold upstream's changes into this folder rather than copying the release over it, so Fang's changes stay. In the commands, `OLD` is the version above and `NEW` is the release to take.

1. Read upstream's `CHANGELOG.md` between the two versions, and check whether each change above is still needed. For example, upstream may have dropped its `package.json` version read itself.
2. Make upstream's diff for the copied files, and apply it from the repository root:

   ```sh
   git clone https://github.com/edlsh/pi-ask-user /tmp/pi-ask-user
   git -C /tmp/pi-ask-user diff vOLD vNEW -- '*.ts' ':(exclude)*/**' LICENSE > /tmp/pi-ask-user.patch
   patch -p1 -d host/src/pi-extensions/fang-ask-user < /tmp/pi-ask-user.patch
   ```

   `patch` tolerates the one-line headers. Resolve any hunk it rejects by hand (it leaves a `.rej` file next to the file), then delete the `.rej` and `.orig` files.
3. Give every new file the header, and change the version in every header, at the top of this file, in `ASK_USER_VERSION`, and in the `pi-ask-user` license text in `host/build.ts`.
4. Read upstream's `package.json` for new dependencies. A new `@earendil-works/*` or `@sinclair/typebox` import is fine as it is; any other package has to be bundled, so add it to the extension's build in `host/build.ts` and to `licenses/`, after checking its license.
5. Update the version at the top of this file and the list above, then build and test as for any host change, including a chat on the emulator where the agent calls `ask_user` and the card's answer reaches it.
