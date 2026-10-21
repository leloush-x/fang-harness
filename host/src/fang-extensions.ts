import { createHash } from "node:crypto";
import { mkdir, readFile, rename, writeFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { HostError } from "./env.js";
import { isObject, type JsonObject } from "./framing.js";

/** Where a Pi extension Fang Harness ships lives, as `ensure` installs it and `rpc` finds it. */
interface ExtensionLocation {
  name: string;
  /** Whether the extension imports packages that `ensure` installs in Fang Harness's Pi runtime. */
  usesRuntimePackages: boolean;
}

/** An extension's source, which `ensure` installs (`host/build.ts` bundles each). */
export interface FangExtension extends ExtensionLocation {
  source: string;
}

/** An extension a chat loads, by the SHA-256 (lowercase hex) of the source Fang Harness ships. */
export interface FangExtensionFingerprint extends ExtensionLocation {
  sha256: string;
}

function isLocation(item: unknown): item is ExtensionLocation & JsonObject {
  return (
    isObject(item) &&
    typeof item.name === "string" &&
    /^[a-z0-9][a-z0-9-]*$/.test(item.name) &&
    typeof item.usesRuntimePackages === "boolean"
  );
}

/** The `ensure` parameter `fangExtensions`: every extension Fang Harness ships, enabled or not, with a plain folder name. */
export function fangExtensionsParam(params: JsonObject): FangExtension[] {
  const value = params.fangExtensions;
  if (!Array.isArray(value) || !value.every((item): item is FangExtension => isLocation(item) && typeof item.source === "string")) {
    throw new HostError(
      "bad_params",
      'Parameter "fangExtensions" must be an array of { name, source, usesRuntimePackages } with plain names',
    );
  }
  return value;
}

/** The `rpc` parameter `fangExtensions`: the extensions the user left on, each with a plain folder name. */
export function fangExtensionFingerprintsParam(params: JsonObject): FangExtensionFingerprint[] {
  const value = params.fangExtensions;
  const valid = Array.isArray(value) && value.every((item): item is FangExtensionFingerprint => isLocation(item) && typeof item.sha256 === "string");
  if (!valid) {
    throw new HostError(
      "bad_params",
      'Parameter "fangExtensions" must be an array of { name, sha256, usesRuntimePackages } with plain names',
    );
  }
  return value;
}

/**
 * An extension's `index.js`. One that uses runtime packages goes inside the runtime, where Node finds them and imports
 * the file natively. Any other stays outside, where Pi's loader transpiles it and hands its `@earendil-works/*` and
 * `typebox` imports Pi's own loaded copies instead of loading second ones.
 */
function extensionPath(root: string, { name, usesRuntimePackages }: ExtensionLocation): string {
  return usesRuntimePackages ? join(root, "pi", "extensions", name, "index.js") : join(root, "extensions", name, "index.js");
}

/**
 * Writes each extension to its `index.js`. A file is rewritten only when its source changed, and through a rename, so
 * a chat's Pi starting at the same time never loads half a file.
 */
export async function installFangExtensions(root: string, extensions: FangExtension[]): Promise<void> {
  for (const extension of extensions) {
    const path = extensionPath(root, extension);
    if ((await readFile(path, "utf8").catch(() => null)) === extension.source) continue;
    await mkdir(dirname(path), { recursive: true });
    const temporary = `${path}.${process.pid}.tmp`;
    await writeFile(temporary, extension.source);
    await rename(temporary, path);
  }
}

/**
 * The installed `index.js` of each extension, for Pi's `--extension`, or null when one is missing or differs from the
 * source Fang Harness ships, which `ensure` then installs.
 */
export async function installedFangExtensionFiles(
  root: string,
  fingerprints: FangExtensionFingerprint[],
): Promise<{ name: string; path: string }[] | null> {
  const files: { name: string; path: string }[] = [];
  for (const fingerprint of fingerprints) {
    const path = extensionPath(root, fingerprint);
    const installed = await readFile(path).catch(() => null);
    if (installed === null || createHash("sha256").update(installed).digest("hex") !== fingerprint.sha256) return null;
    files.push({ name: fingerprint.name, path });
  }
  return files;
}
