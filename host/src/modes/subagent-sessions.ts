import { createReadStream } from "node:fs";
import { stat } from "node:fs/promises";
import { isObject, LineSplitter, type JsonObject } from "../framing.js";
import type { ModeContext } from "../mode.js";
import { stringParam } from "../params.js";
import { sessionFile } from "../session-file.js";
import { containedFolder, sessionId, subagentFolder, transcriptFiles } from "../subagent-folders.js";

/** The custom entry the Fang subagents extension writes into each transcript, linking it to the parent's call. */
const ENTRY_TYPE = "fang-subagent";

/** What one transcript says about its run, or undefined when it isn't a subagent's. */
export async function describe(path: string): Promise<JsonObject | undefined> {
  let header: JsonObject | undefined;
  let link: JsonObject | undefined;
  let messageCount = 0;
  let invalid = false;

  // Stream the transcript with a bounded splitter: no whole-file buffer, but every message is still counted.
  const lines = new LineSplitter((line) => {
    let entry: unknown;
    try {
      entry = JSON.parse(line);
    } catch {
      return;
    }
    if (!isObject(entry)) return;
    if (!header) {
      if (entry.type !== "session") {
        invalid = true;
        lines.stop();
        return;
      }
      header = entry;
    } else if (entry.type === "message") messageCount++;
    else if (entry.type === "custom" && entry.customType === ENTRY_TYPE && isObject(entry.data)) link ??= entry.data;
  });

  try {
    for await (const chunk of createReadStream(path)) {
      lines.push(chunk as Buffer);
      if (invalid) break;
    }
    // A final line without a newline is still an entry, as it was with split("\n").
    if (!invalid && lines.leftover.length > 0) lines.push(Buffer.from([0x0a]));
  } catch {
    return undefined;
  }
  if (invalid || !header || !link) return undefined;
  const created = typeof header.timestamp === "string" ? Date.parse(header.timestamp) : Number.NaN;
  return {
    path,
    toolCallId: typeof link.toolCallId === "string" ? link.toolCallId : null,
    agent: typeof link.agent === "string" ? link.agent : null,
    description: typeof link.description === "string" ? link.description : null,
    created: Number.isNaN(created) ? null : created,
    modified: Math.round((await stat(path)).mtimeMs),
    messageCount,
  };
}

/** The subagent transcripts of the session file `path`, oldest first; none when it has no session header. */
export async function subagentSessions(ctx: ModeContext): Promise<JsonObject> {
  const parent = await sessionFile(ctx.env.configDir, stringParam(ctx.params, "path"));
  const id = await sessionId(parent);
  const folder = id === undefined ? undefined : subagentFolder(parent, id);
  const contained = folder && (await containedFolder(ctx.env.configDir, folder));
  const found = contained ? await Promise.all((await transcriptFiles(contained)).map(describe)) : [];
  const sessions = found.filter((session): session is JsonObject => session !== undefined);
  sessions.sort((a, b) => Number(a.created ?? 0) - Number(b.created ?? 0));
  return { sessions };
}
