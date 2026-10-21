/**
 * Bundles the host into `$OUT/fang-host.mjs`, and each Pi extension Fang Harness ships into
 * `$OUT/fang-extensions/<name>.js`. Both ship as APK assets and are copied into the app's Wolfi storage at launch. The
 * extensions stay out of the host: Fang Harness sends their source over the host's socket to `ensure`, which installs
 * them, and each chat sends only their SHA-256 to check the installed copy.
 *
 * The licences of the third-party code in the bundles go to `$OUT/licenses/`, next to the app's own in
 * `app/src/main/assets/licenses/`, for Settings › Open-source licenses.
 */
import { mkdir, readFile, writeFile } from "node:fs/promises";
import { build } from "esbuild";

const runtimeManifest: unknown = JSON.parse(await readFile("../app/src/main/assets/pi/package.json", "utf8"));
if (typeof runtimeManifest !== "object" || runtimeManifest === null || !("dependencies" in runtimeManifest)) {
  throw new Error("The Pi runtime's package.json has no dependencies");
}
/** The packages `npm ci` installs in Fang Harness's Pi runtime, which the extensions saved there import at run time. */
const runtimePackages = Object.keys(Object(runtimeManifest.dependencies));

const out = process.env.OUT;
if (!out) throw new Error("Set OUT to the directory the bundles go in");

const common = { bundle: true, platform: "node", format: "esm", target: "node22", logLevel: "warning" } as const;

// The MIT and BSD licences ask every copy to carry its notice, and esbuild drops the notices of the code it bundles,
// so each bundle with third-party code starts with them.
const [diffLicense, typeboxLicense, webAccessLicense, todoLicense] = await Promise.all([
  readFile("node_modules/diff/LICENSE", "utf8"),
  readFile("node_modules/typebox/license", "utf8"),
  readFile("src/pi-extensions/fang-web-access/LICENSE", "utf8"),
  readFile("src/pi-extensions/fang-todo/LICENSE", "utf8"),
]);
const [diffVersion, typeboxVersion] = await Promise.all(["diff", "typebox"].map(packageVersion));
const paseoNotice =
  "Adapts Paseo's folder search (https://github.com/getpaseo/paseo, commit b26b923),\n" +
  "Copyright (c) 2025-present Mohamed Boudra, licensed under the Apache License, Version 2.0\n" +
  "(http://www.apache.org/licenses/LICENSE-2.0), with changes for Fang Harness.\n";
const hostNotices = `/*\nBundles jsdiff ${diffVersion}:\n\n${diffLicense}\n${paseoNotice}*/`;
const todoNotices = `/*\nThe todo tool's texts come from @juicesharp/rpiv-todo 2.13.8:\n\n${todoLicense}*/`;
const webAccessNotices =
  `/*\nVendored from pi-web-access:\n\n${webAccessLicense}\nBundles typebox:\n\n${typeboxLicense}*/`;

/** The version of the installed package [name]. */
async function packageVersion(name: string): Promise<string> {
  const manifest: unknown = JSON.parse(await readFile(`node_modules/${name}/package.json`, "utf8"));
  if (typeof manifest !== "object" || manifest === null || !("version" in manifest)) {
    throw new Error(`${name}'s package.json has no version`);
  }
  return String(manifest.version);
}

/** Each bundled component's licence file for the app's licence list: its source and use, then its licence. */
const licenses: Record<string, string> = {
  jsdiff:
    `jsdiff ${diffVersion} (https://github.com/kpdecker/jsdiff), BSD 3-Clause License. ` +
    `Parses diffs in Fang Harness's Wolfi host.\n\n${diffLicense}`,
  typebox:
    `TypeBox ${typeboxVersion} (https://github.com/sinclairzx81/typebox), MIT License. ` +
    `Bundled into the Fang Harness web access extension.\n\n${typeboxLicense}`,
  "pi-web-access":
    "pi-web-access 0.39.0 (https://github.com/nicobailon/pi-web-access), MIT License. " +
    `Fang Harness's web access extension is a modified copy.\n\n${webAccessLicense}`,
  "rpiv-todo":
    "@juicesharp/rpiv-todo 2.13.8 (https://github.com/juicesharp/rpiv-mono), MIT License. " +
    `The Fang Harness todo extension's tool texts come from it.\n\n${todoLicense}`,
};

await mkdir(`${out}/licenses`, { recursive: true });
await Promise.all([
  build({ ...common, entryPoints: ["src/main.ts"], outfile: `${out}/fang-host.mjs`, banner: { js: hostNotices } }),
  build({
    ...common,
    entryPoints: ["src/pi-extensions/fang-todo/index.ts"],
    outfile: `${out}/fang-extensions/fang-todo.js`,
    banner: { js: todoNotices },
    // Pi resolves these to its own copies when it loads an extension.
    external: ["@earendil-works/*", "typebox"],
  }),
  build({
    ...common,
    entryPoints: ["src/pi-extensions/fang-background/index.ts"],
    outfile: `${out}/fang-extensions/fang-background.js`,
    // Pi resolves these to its own copies when it loads an extension.
    external: ["@earendil-works/*", "typebox"],
  }),
  build({
    ...common,
    entryPoints: ["src/pi-extensions/fang-web-access/index.ts"],
    outfile: `${out}/fang-extensions/fang-web-access.js`,
    banner: { js: webAccessNotices },
    // Node, not Pi, resolves this extension's imports, since the host saves it inside the runtime. Anything left
    // external loads its whole module graph, so typebox, which builds the tool schemas at startup, is bundled.
    external: [...runtimePackages, "@earendil-works/*"],
  }),
  ...Object.entries(licenses).map(([name, text]) => writeFile(`${out}/licenses/${name}.txt`, text)),
]);
