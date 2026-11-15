import { chmod, readFile, rename, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { HostError } from "../env.js";
import type { JsonObject } from "../framing.js";
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
    const id = typeof ctx.params.id === "string" && ctx.params.id !== "" ? ctx.params.id : `acct_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
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
    return { account: toInfo(account) };
  });
}

export async function accountsRemove(ctx: ModeContext): Promise<JsonObject> {
  const id = stringParam(ctx.params, "id");
  return withConfigLock(ctx.env, async () => {
    const data = await readAccounts(ctx.env.configDir);
    for (const pa of Object.values(data.providers)) {
      const index = pa.accounts.findIndex((a) => a.id === id);
      if (index >= 0) {
        pa.accounts.splice(index, 1);
        await writeAccounts(ctx.env.configDir, data);
        return {};
      }
    }
    return {};
  });
}

export async function accountsActivate(ctx: ModeContext): Promise<JsonObject> {
  const id = stringParam(ctx.params, "id");
  return withConfigLock(ctx.env, async () => {
    const data = await readAccounts(ctx.env.configDir);
    for (const pa of Object.values(data.providers)) {
      const account = pa.accounts.find((a) => a.id === id);
      if (account) {
        pa.strategy = "manual";
        for (const a of pa.accounts) a.state = a.id === id ? "active" : "disabled";
        await writeAccounts(ctx.env.configDir, data);
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
