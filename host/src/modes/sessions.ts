import { readdir, stat } from "node:fs/promises";
import { dirname } from "node:path";
import { loadPi } from "../env.js";
import type { JsonObject } from "../framing.js";
import type { ModeContext } from "../mode.js";
import { subagentFolder } from "../subagent-folders.js";

const FIRST_MESSAGE_CHARS = 200;

async function isDirectory(path: string): Promise<boolean> {
  try {
    return (await stat(path)).isDirectory();
  } catch {
    return false;
  }
}

/** Runs [map] with at most [limit] calls at once: hundreds of chats must not open hundreds of file descriptors. */
async function mapLimited<T, R>(items: readonly T[], limit: number, map: (item: T) => Promise<R>): Promise<R[]> {
  const result = Array.from({ length: items.length }, () => undefined as R);
  let next = 0;
  await Promise.all(Array.from({ length: Math.min(limit, items.length) }, async () => {
    while (next < items.length) {
      const index = next++;
      result[index] = await map(items[index] as T);
    }
  }));
  return result;
}

/** Transcript counts per subagent folder, so one recursive readdir serves every session in that folder. */
function countsIn(folder: string, cache: Map<string, Promise<Map<string, number>>>): Promise<Map<string, number>> {
  let pending = cache.get(folder);
  if (pending === undefined) {
    pending = readdir(folder, { recursive: true }).then((entries) => {
      const counts = new Map<string, number>();
      for (const entry of entries) {
        if (typeof entry !== "string" || !entry.endsWith(".jsonl")) continue;
        const id = entry.split("/")[0];
        if (id !== undefined) counts.set(id, (counts.get(id) ?? 0) + 1);
      }
      return counts;
    }).catch(() => new Map<string, number>());
    cache.set(folder, pending);
  }
  return pending;
}

/**
 * Every session, newest first; `cwdExists` tells whether its workspace folder is still there, `subagents` how many
 * subagent transcripts it has.
 */
export async function sessions(ctx: ModeContext): Promise<JsonObject> {
  const { SessionManager } = await loadPi(ctx.env.root);
  const seen = new Set<string>();
  const all = (await SessionManager.listAll(undefined, ctx.signal)).filter((session) => {
    if (seen.has(session.path)) return false;
    seen.add(session.path);
    return true;
  });
  all.sort((a, b) => b.modified.getTime() - a.modified.getTime());
  const cwds = [...new Set(all.map((session) => session.cwd))];
  const exists = await mapLimited(cwds, 16, isDirectory);
  // Per call: a long-lived utility host must see subagent transcripts added since the previous refresh.
  const counts = new Map<string, Promise<Map<string, number>>>();
  for (const session of all) {
    const folder = subagentFolder(session.path, session.id);
    if (folder) {
      const parent = dirname(folder);
      if (!counts.has(parent)) counts.set(parent, countsIn(parent, counts));
    }
  }
  const subagents = await mapLimited(all, 16, async (session) => {
    const folder = subagentFolder(session.path, session.id);
    if (!folder) return 0;
    return (await counts.get(dirname(folder)) ?? new Map<string, number>()).get(session.id) ?? 0;
  });
  const existing = new Set(cwds.filter((_, index) => exists[index]));
  return {
    sessions: all.map((session, index) => ({
      path: session.path,
      id: session.id,
      cwd: session.cwd,
      cwdExists: existing.has(session.cwd),
      name: session.name,
      created: session.created.toISOString(),
      modified: session.modified.toISOString(),
      messageCount: session.messageCount,
      // Code points, so the cut never leaves a lone surrogate.
      firstMessage: Array.from(session.firstMessage).slice(0, FIRST_MESSAGE_CHARS).join(""),
      subagents: subagents[index] ?? 0,
    })),
  };
}
