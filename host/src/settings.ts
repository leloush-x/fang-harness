import { readFile } from "node:fs/promises";
import { join } from "node:path";
import type { SettingsManager } from "@earendil-works/pi-coding-agent";
import { HostError, type PiSdk } from "./env.js";
import { isObject } from "./framing.js";
import type { ModeContext } from "./mode.js";

export const SETTINGS_INVALID = "settings.json is not valid. Ask the agent to fix it.";

/** Pi's global settings, read and written through Pi's own lock-and-merge `SettingsManager`. */
export interface PiSettings {
  manager: SettingsManager;
  /** `enabledModels` patterns; empty means every model is in scope. */
  enabledModels: string[];
  /** `defaultProvider/defaultModel`, when both are set. */
  defaultRef: string | undefined;
}

/** Throws `settings_invalid` when settings.json cannot be parsed or `enabledModels` is not a list of strings. */
export function openSettings(ctx: ModeContext, pi: PiSdk): PiSettings {
  const manager = pi.SettingsManager.create(ctx.env.home, ctx.env.configDir);
  if (manager.drainErrors().some((error) => error.scope === "global")) {
    throw new HostError("settings_invalid", SETTINGS_INVALID, { key: "settings_invalid" });
  }
  const enabledModels: unknown = manager.getEnabledModels() ?? [];
  if (!Array.isArray(enabledModels) || !enabledModels.every((pattern) => typeof pattern === "string")) {
    throw new HostError("settings_invalid", SETTINGS_INVALID, { key: "settings_invalid" });
  }
  const provider = manager.getDefaultProvider();
  const model = manager.getDefaultModel();
  return { manager, enabledModels, defaultRef: provider && model ? `${provider}/${model}` : undefined };
}

/**
 * The settings, or undefined when settings.json is invalid: Pi then ignores it, so there is no scope
 * for a provider change to maintain.
 */
export function openSettingsIfValid(ctx: ModeContext, pi: PiSdk): PiSettings | undefined {
  try {
    return openSettings(ctx, pi);
  } catch (error) {
    if (error instanceof HostError && error.code === "settings_invalid") return undefined;
    throw error;
  }
}

/** Writes `enabledModels` (removing it when empty) and waits for Pi's locked write. */
export async function saveEnabledModels(settings: PiSettings, patterns: readonly string[]): Promise<void> {
  settings.manager.setEnabledModels(patterns.length > 0 ? [...patterns] : undefined);
  await settings.manager.flush();
  const [failure] = settings.manager.drainErrors();
  if (failure !== undefined) throw failure.error;
}

/** The user's global Pi settings as parsed JSON; null when the file is missing or malformed, which Pi reports itself. */
export async function readGlobalSettings(configDir: string): Promise<unknown> {
  return readFile(join(configDir, "settings.json"), "utf8")
    .then((text) => JSON.parse(text.replace(/^\uFEFF/, "")) as unknown)
    .catch(() => null);
}

/**
 * The packages the user's global Pi settings install, keyed by how Pi identifies them: npm ones by package name, git
 * ones by `host/path` of the repository. Project settings are left out: Pi loads them only after a trust decision that
 * extensions, `trust.json` and prompts can make, which the host cannot predict.
 */
export interface UserPackages {
  npm: Set<string>;
  git: Set<string>;
}

/** [UserPackages] of the user's global Pi settings. */
export function userPackages(settings: unknown): UserPackages {
  const packages: UserPackages = { npm: new Set(), git: new Set() };
  if (!isObject(settings) || !Array.isArray(settings.packages)) return packages;
  for (const entry of settings.packages as unknown[]) {
    const source = isObject(entry) ? entry.source : entry;
    if (typeof source !== "string") continue;
    if (source.startsWith("npm:")) {
      const spec = source.slice("npm:".length).trim();
      packages.npm.add(/^(@?[^@]+(?:\/[^@]+)?)(?:@.+)?$/.exec(spec)?.[1] ?? spec);
      continue;
    }
    const repository = gitRepository(source);
    if (repository !== null) packages.git.add(repository);
  }
  return packages;
}

/**
 * npm package names the user's global Pi settings install. Pi parses `npm:<name>[@version]` sources with this same
 * pattern and identifies npm packages by name.
 */
export function userNpmPackages(settings: unknown): Set<string> {
  return userPackages(settings).npm;
}

/**
 * The `host/path` of the git repository [source] installs, as Pi identifies it, or null when the source installs
 * something else. Pi reads `git:github.com/owner/repo`, `git:https://github.com/owner/repo`, `git@github.com:owner/repo`
 * and the same with a `#ref` or `@ref`; every form ends up at the host and path, without `.git`.
 */
export function gitRepository(source: string): string | null {
  let value = source.trim();
  if (!value.startsWith("git:") && !/^(https?|ssh|git):\/\//i.test(value) && !value.startsWith("git@")) return null;
  value = value.replace(/^git:/, "").trim();
  const scp = /^git@([^:]+):(.+)$/.exec(value);
  if (scp !== null) value = `${scp[1]}/${scp[2]}`;
  value = value
    .replace(/^(https?|ssh|git):\/\//i, "")
    .replace(/^git@/, "")
    .replace(/\.git$/, "")
    .replace(/[#@][^/]*$/, "")
    .replace(/\/+$/, "")
    .toLowerCase();
  // Pi takes a generic source's first segment as the host only when it looks like one.
  const slash = value.indexOf("/");
  if (slash <= 0) return null;
  const host = value.slice(0, slash);
  return host.includes(".") || host === "localhost" ? value : null;
}
