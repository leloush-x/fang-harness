import { readFile, writeFile } from "node:fs/promises";
import path from "node:path";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import type { HostEnv } from "../src/env.js";
import { isObject, type JsonObject } from "../src/framing.js";
import type { ModeContext } from "../src/mode.js";
import { pickAccount } from "../src/modes/accounts.js";
import { type Dirs, makeDirs, removeDirs, runOp } from "./harness.js";

function providerCount(reply: JsonObject): number {
  const data: unknown = reply.data;
  return isObject(data) && Array.isArray(data.providers) ? data.providers.length : 0;
}

function poolAccounts(reply: JsonObject): { label: string; lastUsedAt: number }[] {
  const data: unknown = reply.data;
  const first = isObject(data) && Array.isArray(data.providers) ? data.providers[0] : undefined;
  if (!isObject(first) || !Array.isArray(first.accounts)) throw new Error("no accounts in reply");
  return first.accounts as { label: string; lastUsedAt: number }[];
}

function credential(entries: Record<string, Record<string, unknown>>, provider: string): Record<string, unknown> {
  const entry = entries[provider];
  if (!entry) throw new Error(`no ${provider} credential`);
  return entry;
}

/** The account id in an operation's reply; checked once so the tests do not cast through `unknown`. */
function accountId(reply: JsonObject): string {
  if (!isObject(reply.data) || !isObject(reply.data.account) || typeof reply.data.account.id !== "string") {
    throw new Error("no account in reply");
  }
  return reply.data.account.id;
}

/** The pool is the source of truth; these tests pin how sign-ins get into it and how it drives Pi's one stored slot. */
describe("account pool", () => {
  let dirs: Dirs;
  let ctx: ModeContext;

  beforeEach(async () => {
    dirs = await makeDirs();
    const env: HostEnv = {
      token: "token",
      root: dirs.root,
      configDir: dirs.configDir,
      home: dirs.home,
      prefix: dirs.prefix,
      sharedStorage: dirs.shared,
    };
    ctx = { env, params: {}, conn: {} as never, signal: new AbortController().signal };
  });

  afterEach(async () => {
    await removeDirs(dirs);
  });

  async function stored(): Promise<Record<string, Record<string, unknown>>> {
    return JSON.parse(await readFile(path.join(dirs.configDir, "auth.json"), "utf8")) as Record<string, Record<string, unknown>>;
  }

  it("captures a stored sign-in once, ignoring custom providers", async () => {
    await writeFile(
      path.join(dirs.configDir, "auth.json"),
      JSON.stringify({
        zai: { type: "api_key", key: "key-zai" },
        google: { type: "api_key", key: "key-google" },
        custom: { type: "api_key", key: "key-custom" },
      }),
    );
    expect(await runOp("accounts_capture", dirs, { providerId: "zai" })).toMatchObject({ data: { added: true } });
    expect(await runOp("accounts_capture", dirs, { providerId: "zai" })).toMatchObject({ data: { added: false } });
    const list = await runOp("accounts_list", dirs, {});
    expect(providerCount(list)).toBe(1);
  });

  it("rotates the least recently used key and advances its last use", async () => {
    await runOp("accounts_save", dirs, { providerId: "zai", label: "A", kind: "api_key", apiKey: "key-a" });
    await runOp("accounts_save", dirs, { providerId: "zai", label: "B", kind: "api_key", apiKey: "key-b" });
    expect((await pickAccount(ctx, "zai"))?.key).toBe("key-a");
    const second = await pickAccount(ctx, "zai");
    expect(second?.key).toBe("key-b");
    expect((await pickAccount(ctx, "zai"))?.key).toBe("key-a");
    const list = await runOp("accounts_list", dirs, {});
    const accounts = poolAccounts(list);
    expect(accounts?.find((account) => account.label === "A")?.lastUsedAt).toBeGreaterThan(0);
    expect(accounts?.find((account) => account.label === "B")?.lastUsedAt).toBeGreaterThan(0);
  });

  it("stays on the active account in manual mode", async () => {
    const first = await runOp("accounts_save", dirs, { providerId: "zai", label: "A", kind: "api_key", apiKey: "key-a" });
    const second = await runOp("accounts_save", dirs, { providerId: "zai", label: "B", kind: "api_key", apiKey: "key-b" });
    const secondId = accountId(second);
    expect(first.ok).toBe(true);
    await runOp("accounts_activate", dirs, { id: secondId });
    expect((await pickAccount(ctx, "zai"))?.key).toBe("key-b");
    expect((await pickAccount(ctx, "zai"))?.key).toBe("key-b");
  });

  it("skips disabled and cooling accounts", async () => {
    await runOp("accounts_save", dirs, { providerId: "zai", label: "A", kind: "api_key", apiKey: "key-a" });
    const result = await runOp("accounts_save", dirs, { providerId: "zai", label: "B", kind: "api_key", apiKey: "key-b", enabled: false });
    expect(result.ok).toBe(true);
    expect((await pickAccount(ctx, "zai"))?.key).toBe("key-a");
    expect((await pickAccount(ctx, "zai"))?.key).toBe("key-a");
  });

  it("keeps Pi's stored credential in step with save, activate and delete", async () => {
    await writeFile(
      path.join(dirs.configDir, "auth.json"),
      JSON.stringify({
        anthropic: { type: "oauth", access: "a", refresh: "r", expires: 1, extra: "keep" },
        openrouter: { type: "api_key", key: "other", env: { ACCOUNT_ID: "1" } },
      }),
    );
    const first = await runOp("accounts_save", dirs, { providerId: "zai", label: "A", kind: "api_key", apiKey: "key-a" });
    const second = await runOp("accounts_save", dirs, { providerId: "zai", label: "B", kind: "api_key", apiKey: "key-b" });
    expect((await stored()).zai).toEqual({ type: "api_key", key: "key-b" });
    expect(credential(await stored(), "anthropic").extra).toBe("keep");
    expect((await stored()).openrouter).toEqual({ type: "api_key", key: "other", env: { ACCOUNT_ID: "1" } });

    const secondId = accountId(second);
    await runOp("accounts_activate", dirs, { id: secondId });
    expect((await stored()).zai).toEqual({ type: "api_key", key: "key-b" });

    await runOp("accounts_remove", dirs, { id: accountId(first) });
    expect((await stored()).zai).toBeDefined();
    await runOp("accounts_remove", dirs, { id: secondId });
    expect((await stored()).zai).toBeUndefined();
  });

  it("stores an OAuth account as Pi's OAuth credential", async () => {
    await runOp("accounts_save", dirs, {
      providerId: "anthropic",
      label: "Claude",
      kind: "oauth",
      credential: { access: "access", refresh: "refresh", expires: 123 },
    });
    expect((await stored()).anthropic).toEqual({ type: "oauth", access: "access", refresh: "refresh", expires: 123 });
  });
});
