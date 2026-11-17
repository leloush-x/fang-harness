import { readFile } from "node:fs/promises";
import path from "node:path";
import { setImmediate } from "node:timers/promises";
import { describe, expect, it } from "vitest";
import type { HostEnv } from "../src/env.js";
import { withConfigLock } from "../src/config-lock.js";
import { type Dirs, makeDirs, removeDirs } from "./harness.js";

describe("config lock", () => {
  it("queues callers inside one host instead of reporting a false busy", async () => {
    const dirs: Dirs = await makeDirs();
    const env: HostEnv = {
      token: "token",
      root: dirs.root,
      configDir: dirs.configDir,
      home: dirs.home,
      prefix: dirs.prefix,
      sharedStorage: dirs.shared,
    };
    const order: string[] = [];
    let release!: () => void;
    const gate = new Promise<void>((resolve) => {
      release = resolve;
    });
    try {
      const first = withConfigLock(env, async () => {
        order.push("first-start");
        await gate;
        order.push("first-end");
      });
      // Let the first caller take the local queue before the second one arrives.
      await setImmediate();
      const second = withConfigLock(env, async () => {
        order.push("second");
      });
      release();
      await Promise.all([first, second]);
      expect(order).toEqual(["first-start", "first-end", "second"]);
      // The symlink is gone once both callers finish.
      await expect(readFile(path.join(dirs.root, "config.lock"))).rejects.toMatchObject({ code: "ENOENT" });
    } finally {
      await removeDirs(dirs);
    }
  });
});
