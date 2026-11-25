import { spawn } from "node:child_process";
import { withConfigLock } from "../config-lock.js";
import { piEntry } from "../env.js";
import type { JsonObject } from "../framing.js";
import type { ModeContext } from "../mode.js";
import { stringParam } from "../params.js";
import { packageSources, readGlobalSettings } from "../settings.js";
import { sessionEnv } from "../session.js";
import { commandFailed, forwardLines } from "./ensure.js";

/** How long Pi's group gets to end after SIGTERM before SIGKILL. */
const KILL_AFTER_MS = 2000;

/** The raw sources of the Pi packages in Fang Harness's config, for the extension manager. */
export async function extensionPackages(ctx: ModeContext): Promise<JsonObject> {
  return { packages: packageSources(await readGlobalSettings(ctx.env.configDir)) };
}

/**
 * Runs `pi remove <source>` in Fang Harness's Pi. The config is held while Pi edits its settings, so no backup,
 * restore or starting chat sees a package half-removed. Pi runs in its own process group, so hanging up stops npm or
 * git too when they keep the output open.
 */
export async function packageRemove(ctx: ModeContext): Promise<JsonObject> {
  const source = stringParam(ctx.params, "source");
  return withConfigLock(ctx.env, async () => {
    const child = spawn(process.execPath, [await piEntry(ctx.env.root), "remove", source], {
      cwd: ctx.env.home,
      env: await sessionEnv(ctx.env),
      detached: true,
      stdio: ["ignore", "pipe", "pipe"],
    });
    // Drain both pipes, or Pi blocks once its output fills them.
    forwardLines(child.stdout, () => undefined);
    forwardLines(child.stderr, () => undefined);
    const signalGroup = (signal: NodeJS.Signals): void => {
      try {
        if (child.pid !== undefined) process.kill(-child.pid, signal);
      } catch {
        // The group has already exited.
      }
    };
    const stop = (): void => {
      signalGroup("SIGTERM");
      setTimeout(() => signalGroup("SIGKILL"), KILL_AFTER_MS).unref();
    };
    if (ctx.signal.aborted) stop();
    else ctx.signal.addEventListener("abort", stop, { once: true });
    const { promise, resolve, reject } = Promise.withResolvers<number | null>();
    child.on("error", reject);
    child.on("close", resolve);
    const code = await promise;
    ctx.signal.removeEventListener("abort", stop);
    if (code === 0) return {};
    throw commandFailed("remove_failed", "pi remove", code);
  });
}
