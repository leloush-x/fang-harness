import { chmod, readFile, rename, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { HostError } from "../env.js";
import { isObject, type JsonObject } from "../framing.js";
import type { ModeContext } from "../mode.js";
import { stringParam } from "../params.js";
import { withConfigLock } from "../config-lock.js";
import { createRuntime } from "../runtime.js";

const ACCOUNTS_FILE = "fang-accounts.json";
const VERSION = 1;

type AccountKind = "api_key" | "oauth";

interface Account {
  id: string;
  label: string;
  kind: AccountKind;
  key?: string | undefined;
  credential?: { access: string; refresh: string; expires: number } | undefined;
  enabled: boolean;
  state: "active" | "disabled" | "cooldown";
  failures: number;
  cooldownUntil: number;
  lastUsedAt: number;
  needsSignIn: boolean;
}

interface ProviderAccounts {
  strategy: "round_robin" | "sticky" | "manual";
  accounts: Account[];
}

interface AccountsFile {
  version: number;
  providers: Record<string, ProviderAccounts>;
}

function accountsPath(configDir: string): string {
  return join(configDir, ACCOUNTS_FILE);
}

async function readAccounts(configDir: string): Promise<AccountsFile> {
  let raw: string;
  try {
    raw = await readFile(accountsPath(configDir), "utf8");
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code === "ENOENT") return { version: VERSION, providers: {} };
    throw new HostError("accounts_store_broken", "The account list can't be read", { key: "accounts_store_broken" });
  }
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch {
    throw new HostError("accounts_store_broken", "The account list can't be read", { key: "accounts_store_broken" });
  }
  if (parsed === null || typeof parsed !== "object" || !("providers" in parsed) || typeof (parsed as { providers: unknown }).providers !== "object") {
    throw new HostError("accounts_store_broken", "The account list can't be read", { key: "accounts_store_broken" });
  }
  const providers = (parsed as { providers: Record<string, unknown> }).providers;
  const out: Record<string, ProviderAccounts> = {};
  for (const [id, value] of Object.entries(providers)) {
    if (value === null || typeof value !== "object") continue;
    const v = value as { strategy?: unknown; accounts?: unknown };
    const strategy = v.strategy === "sticky" || v.strategy === "manual" ? v.strategy : "round_robin";
    const accounts: Account[] = Array.isArray(v.accounts)
      ? v.accounts.filter((a): a is Account => a !== null && typeof a === "object" && typeof (a as { id?: unknown }).id === "string").map((a) => {
          const acc = a as Account & { kind?: unknown };
          return {
            id: acc.id,
            label: typeof acc.label === "string" ? acc.label : acc.id,
            kind: acc.kind === "oauth" ? "oauth" : "api_key",
            key: typeof acc.key === "string" ? acc.key : undefined,
            credential: acc.kind === "oauth" && acc.credential && typeof acc.credential === "object" ? (acc.credential as Account["credential"]) : undefined,
            enabled: acc.enabled !== false,
            state: acc.state === "disabled" || acc.state === "cooldown" ? acc.state : "active",
            failures: typeof acc.failures === "number" ? acc.failures : 0,
            cooldownUntil: typeof acc.cooldownUntil === "number" ? acc.cooldownUntil : 0,
            lastUsedAt: typeof acc.lastUsedAt === "number" ? acc.lastUsedAt : 0,
            needsSignIn: acc.needsSignIn === true,
          };
        })
      : [];
    out[id] = { strategy, accounts };
  }
  return { version: VERSION, providers: out };
}

async function writeAccounts(configDir: string, data: AccountsFile): Promise<void> {
  const path = accountsPath(configDir);
  const temporary = `${path}.${process.pid}.tmp`;
  await writeFile(temporary, JSON.stringify(data, null, 2), { encoding: "utf8", mode: 0o600 });
  await chmod(temporary, 0o600);
  await rename(temporary, path);
}

function maskKey(key: string): string {
  const trimmed = key.trim();
  if (trimmed.length <= 8) return "••••";
  return `${trimmed.slice(0, 4)}…${trimmed.slice(-4)}`;
}

function toInfo(account: Account): JsonObject {
  return {
    id: account.id,
    label: account.label,
    kind: account.kind,
    masked: account.kind === "api_key" && account.key ? maskKey(account.key) : account.label,
    enabled: account.enabled,
    state: account.state,
    failures: account.failures,
    cooldownUntil: account.cooldownUntil,
    lastUsedAt: account.lastUsedAt,
    needsSignIn: account.needsSignIn,
  };
}

function toProviderInfo(id: string, pa: ProviderAccounts, name: string): JsonObject {
  return {
    providerId: id,
    name,
    strategy: pa.strategy,
    accounts: pa.accounts.map(toInfo),
  };
}

/** The pool account as the spawn-time rotation and the auth.json write need it. */
function toPicked(account: Account): PickedAccount {
  return { accountId: account.id, label: account.label, kind: account.kind, key: account.key, credential: account.credential };
}

function newAccountId(): string {
  return `acct_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
}

/** `Account 1`, `Account 2`, … — the first label the pool does not hold. */
function nextLabel(pa: ProviderAccounts): string {
  for (let n = 1; ; n++) {
    const label = `Account ${n}`;
    if (!pa.accounts.some((a) => a.label === label)) return label;
  }
}

async function builtinName(ctx: ModeContext, id: string): Promise<string> {
  const runtime = await createRuntime(ctx, { builtinOnly: true });
  return runtime.getProvider(id)?.name ?? id;
}

export async function accountsList(ctx: ModeContext): Promise<JsonObject> {
  const data = await readAccounts(ctx.env.configDir);
  const providers: JsonObject[] = [];
  for (const [id, pa] of Object.entries(data.providers)) {
    providers.push(toProviderInfo(id, pa, await builtinName(ctx, id).catch(() => id)));
  }
  return { providers };
}

export async function accountsSave(ctx: ModeContext): Promise<JsonObject> {
  const providerId = stringParam(ctx.params, "providerId");
  const label = stringParam(ctx.params, "label");
  const kind = ctx.params.kind === "oauth" ? "oauth" : "api_key";
  if (providerId === "") throw new HostError("accounts_bad_provider", "This provider cannot hold accounts", { key: "accounts_bad_provider" });
  if (label === "") throw new HostError("accounts_bad_provider", "This provider cannot hold accounts", { key: "accounts_bad_provider" });

  return withConfigLock(ctx.env, async () => {
    const data = await readAccounts(ctx.env.configDir);
    const pa = data.providers[providerId] ?? { strategy: "round_robin", accounts: [] };
    const id = typeof ctx.params.id === "string" && ctx.params.id !== "" ? ctx.params.id : newAccountId();
    const existingIndex = pa.accounts.findIndex((a) => a.id === id);
    const duplicate = pa.accounts.some((a, i) => a.label === label && i !== existingIndex);
    if (duplicate) throw new HostError("accounts_duplicate", "This provider already has an account with that label", { key: "accounts_duplicate" });

    let account: Account;
    if (kind === "api_key") {
      const apiKey = typeof ctx.params.apiKey === "string" ? ctx.params.apiKey : "";
      if (apiKey.trim() === "") throw new HostError("accounts_no_key", "The account needs an API key", { key: "accounts_no_key" });
      account = {
        id,
        label,
        kind: "api_key",
        key: apiKey.trim(),
        enabled: ctx.params.enabled !== false,
        state: "active",
        failures: 0,
        cooldownUntil: 0,
        lastUsedAt: 0,
        needsSignIn: false,
      };
    } else {
      const cred = ctx.params.credential;
      if (!cred || typeof cred !== "object") throw new HostError("accounts_bad_kind", "This provider signs in instead of taking an API key", { key: "accounts_bad_kind" });
      const c = cred as { access?: unknown; refresh?: unknown; expires?: unknown };
      if (typeof c.access !== "string") throw new HostError("accounts_bad_kind", "This provider signs in instead of taking an API key", { key: "accounts_bad_kind" });
      account = {
        id,
        label,
        kind: "oauth",
        credential: { access: c.access, refresh: typeof c.refresh === "string" ? c.refresh : "", expires: typeof c.expires === "number" ? c.expires : 0 },
        enabled: ctx.params.enabled !== false,
        state: "active",
        failures: 0,
        cooldownUntil: 0,
        lastUsedAt: 0,
        needsSignIn: false,
      };
    }

    if (existingIndex >= 0) pa.accounts[existingIndex] = account;
    else pa.accounts.push(account);
    data.providers[providerId] = pa;
    await writeAccounts(ctx.env.configDir, data);
    // Pi's slot follows the newest account, so adding a key makes the provider configured straight away.
    await writeStoredCredential(ctx.env.configDir, providerId, toPicked(account));
    return { account: toInfo(account) };
  });
}

export async function accountsRemove(ctx: ModeContext): Promise<JsonObject> {
  const id = stringParam(ctx.params, "id");
  return withConfigLock(ctx.env, async () => {
    const data = await readAccounts(ctx.env.configDir);
    for (const [providerId, pa] of Object.entries(data.providers)) {
      const index = pa.accounts.findIndex((a) => a.id === id);
      if (index >= 0) {
        pa.accounts.splice(index, 1);
        await writeAccounts(ctx.env.configDir, data);
        // Deleting the whole pool out from under Pi must not leave the deleted key working.
        if (pa.accounts.length === 0) await removeStoredCredential(ctx.env.configDir, providerId);
        return {};
      }
    }
    return {};
  });
}

/** Drops the provider's entry from Pi's auth.json, keeping every other provider's. */
async function removeStoredCredential(configDir: string, providerId: string): Promise<void> {
  const path = join(configDir, "auth.json");
  const entries = await readAuthFile(configDir);
  if (!(providerId in entries)) return;
  delete entries[providerId];
  const temporary = `${path}.${process.pid}.tmp`;
  await writeFile(temporary, JSON.stringify(entries, null, 2), { encoding: "utf8", mode: 0o600 });
  await chmod(temporary, 0o600);
  await rename(temporary, path);
}

export async function accountsActivate(ctx: ModeContext): Promise<JsonObject> {
  const id = stringParam(ctx.params, "id");
  return withConfigLock(ctx.env, async () => {
    const data = await readAccounts(ctx.env.configDir);
    for (const [providerId, pa] of Object.entries(data.providers)) {
      const account = pa.accounts.find((a) => a.id === id);
      if (account) {
        pa.strategy = "manual";
        for (const a of pa.accounts) a.state = a.id === id ? "active" : "disabled";
        await writeAccounts(ctx.env.configDir, data);
        // Switching the pool must switch Pi: the picked account becomes the stored credential.
        await writeStoredCredential(ctx.env.configDir, providerId, toPicked(account));
        return { account: toInfo(account) };
      }
    }
    throw new HostError("accounts_not_found", "This account no longer exists", { key: "accounts_not_found" });
  });
}

export async function accountsStrategy(ctx: ModeContext): Promise<JsonObject> {
  const providerId = stringParam(ctx.params, "providerId");
  const strategy = ctx.params.strategy === "sticky" || ctx.params.strategy === "manual" ? ctx.params.strategy : "round_robin";
  return withConfigLock(ctx.env, async () => {
    const data = await readAccounts(ctx.env.configDir);
    const pa = data.providers[providerId];
    if (!pa) throw new HostError("accounts_bad_provider", "This provider cannot hold accounts", { key: "accounts_bad_provider" });
    pa.strategy = strategy;
    await writeAccounts(ctx.env.configDir, data);
    return {};
  });
}

/**
 * Copies the stored sign-in of [providerId] into the pool; every sign-in joins the pool this way, keyed for rotation
 * without a re-add. The stored credential stays Pi's, so signing in never signs the provider out. Returns whether a
 * new account was added.
 */
export async function accountsCapture(ctx: ModeContext): Promise<JsonObject> {
  const providerId = stringParam(ctx.params, "providerId");
  if (providerId === "") throw new HostError("accounts_bad_provider", "This provider cannot hold accounts", { key: "accounts_bad_provider" });
  return withConfigLock(ctx.env, async () => {
    const stored = (await readAuthFile(ctx.env.configDir))[providerId];
    if (!stored) return { added: false };
    const data = await readAccounts(ctx.env.configDir);
    const pa = data.providers[providerId] ?? { strategy: "round_robin", accounts: [] };
    let account: Account;
    if (stored.type === "api_key") {
      const key = typeof stored.key === "string" ? stored.key.trim() : "";
      if (key === "" || pa.accounts.some((a) => a.kind === "api_key" && a.key === key)) return { added: false };
      account = { id: newAccountId(), label: nextLabel(pa), kind: "api_key", key, enabled: true, state: "active", failures: 0, cooldownUntil: 0, lastUsedAt: 0, needsSignIn: false };
    } else {
      const access = typeof stored.access === "string" ? stored.access : "";
      if (access === "") return { added: false };
      const refresh = typeof stored.refresh === "string" ? stored.refresh : "";
      const expires = typeof stored.expires === "number" ? stored.expires : 0;
      if (pa.accounts.some((a) => a.kind === "oauth" && a.credential?.access === access && a.credential.refresh === refresh)) return { added: false };
      account = { id: newAccountId(), label: nextLabel(pa), kind: "oauth", credential: { access, refresh, expires }, enabled: true, state: "active", failures: 0, cooldownUntil: 0, lastUsedAt: 0, needsSignIn: false };
    }
    pa.accounts.push(account);
    data.providers[providerId] = pa;
    await writeAccounts(ctx.env.configDir, data);
    return { added: true };
  });
}

/** One pooled account the host can run, as the picker needs it. */
export interface PickedAccount {
  accountId: string;
  label: string;
  kind: "api_key" | "oauth";
  key?: string | undefined;
  credential?: { access: string; refresh: string; expires: number } | undefined;
}

/**
 * Reads Pi's `auth.json`, a `providerId → credential` map; empty when it is missing or does not hold one. Every field of a
 * credential is kept, so writing it back never drops what Pi stores that Fang does not model (e.g. an API key's `env`).
 */
async function readAuthFile(configDir: string): Promise<Record<string, JsonObject>> {
  let raw: string;
  try {
    raw = await readFile(join(configDir, "auth.json"), "utf8");
  } catch {
    return {};
  }
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch {
    // Pi could not read its own file either; replacing it would throw away every provider's credential, so the caller refuses instead.
    throw new HostError("auth_unreadable", "Pi's auth.json is not valid JSON, so the pool leaves it alone", { key: "auth_unreadable" });
  }
  if (!isObject(parsed)) throw new HostError("auth_unreadable", "Pi's auth.json is not an object, so the pool leaves it alone", { key: "auth_unreadable" });
  const entries: Record<string, JsonObject> = {};
  for (const [providerId, value] of Object.entries(parsed)) {
    if (isObject(value) && (value.type === "api_key" || value.type === "oauth")) entries[providerId] = value;
  }
  return entries;
}

/** Writes one account as the stored credential for its provider, keeping every other provider's. The file stays Pi's: only the entry changes. */
export async function writeStoredCredential(configDir: string, providerId: string, account: PickedAccount): Promise<void> {
  const path = join(configDir, "auth.json");
  const entries = await readAuthFile(configDir);
  const existing = entries[providerId] ?? {};
  if (account.kind === "api_key") {
    if (!account.key) return;
    const env = isObject(existing.env) ? { env: existing.env } : {};
    entries[providerId] = { type: "api_key", key: account.key, ...env };
  } else {
    if (!account.credential?.access) return;
    entries[providerId] = { ...existing, type: "oauth", access: account.credential.access, refresh: account.credential.refresh, expires: account.credential.expires };
  }
  const temporary = `${path}.${process.pid}.tmp`;
  await writeFile(temporary, JSON.stringify(entries, null, 2), { encoding: "utf8", mode: 0o600 });
  await chmod(temporary, 0o600);
  await rename(temporary, path);
}

/**
 * Picks and records the next API-key account of one provider's pool: round-robin takes the least recently used and
 * every chat that starts moves it, so keys rotate by themselves; sticky and manual take the active one. OAuth accounts
 * stay out of automatic rotation: their refresh tokens rotate with use, and a stale pooled copy cannot stand in for
 * the live one, so OAuth switches through `accounts_activate`, which writes the picked account instead.
 */
export async function pickAccount(ctx: ModeContext, providerId: string): Promise<PickedAccount | undefined> {
  try {
    return await withConfigLock(ctx.env, async () => {
      const data = await readAccounts(ctx.env.configDir);
      const pa = data.providers[providerId];
      const usable = (pa?.accounts ?? []).filter((a) => a.enabled && a.state !== "disabled" && a.state !== "cooldown" && a.kind === "api_key" && (a.key ?? "") !== "");
      if (usable.length === 0) return undefined;
      const now = Date.now();
      const strategy = pa?.strategy ?? "round_robin";
      const picked =
        strategy === "sticky" || strategy === "manual"
          ? usable.find((a) => a.state === "active") ?? usable[0]
          : usable.toSorted((a, b) => (a.lastUsedAt || 0) - (b.lastUsedAt || 0))[0];
      if (picked === undefined) return undefined;
      picked.lastUsedAt = now;
      await writeAccounts(ctx.env.configDir, data);
      return { accountId: picked.id, label: picked.label, kind: picked.kind, key: picked.key, credential: picked.credential };
    });
  } catch (error) {
    if (error instanceof HostError && error.code === "config_lock_busy") return undefined;
    throw error;
  }
}
