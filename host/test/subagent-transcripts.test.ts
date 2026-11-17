import { mkdtemp, rm, stat, writeFile } from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { describe as describeTranscript } from "../src/modes/subagent-sessions.js";

const HEADER = { type: "session", timestamp: "2026-01-01T00:00:00.000Z" };
const CREATED = Date.parse(HEADER.timestamp);

function link(toolCallId = "tc1"): string {
  return JSON.stringify({
    type: "custom",
    customType: "fang-subagent",
    data: { toolCallId, agent: "Explore", description: "read hello", task: "CHILD read" },
  });
}

function message(n: number): string {
  return JSON.stringify({ type: "message", message: { role: "user", content: `message ${n}` } });
}

function transcript(lines: string[]): string {
  return `${lines.join("\n")}\n`;
}

describe("subagent transcript describe()", () => {
  let dir: string;

  beforeEach(async () => {
    dir = await mkdtemp(path.join(os.tmpdir(), "fang-subagent-transcripts-"));
  });

  afterEach(async () => {
    await rm(dir, { recursive: true, force: true });
  });

  it("streams a large transcript with bounded memory and counts every message", async () => {
    const before = 3000;
    const after = 3000; // messages after the link still count
    const file = path.join(dir, "large.jsonl");
    await writeFile(
      file,
      transcript([
        JSON.stringify(HEADER),
        ...Array.from({ length: before }, (_, i) => message(i)),
        link(),
        ...Array.from({ length: after }, (_, i) => message(before + i)),
      ]),
    );

    const result = await describeTranscript(file);
    expect(result).toEqual({
      path: file,
      toolCallId: "tc1",
      agent: "Explore",
      description: "read hello",
      created: CREATED,
      modified: Math.round((await stat(file)).mtimeMs),
      messageCount: before + after,
    });
  });

  it("returns undefined when there is no subagent link, even after a full read", async () => {
    const file = path.join(dir, "no-link.jsonl");
    await writeFile(file, transcript([JSON.stringify(HEADER), message(0), message(1)]));
    expect(await describeTranscript(file)).toBeUndefined();
  });

  it("skips malformed lines and still counts the valid messages", async () => {
    const file = path.join(dir, "malformed.jsonl");
    await writeFile(
      file,
      transcript([JSON.stringify(HEADER), message(0), "not valid json", "{broken", message(1), link("tc2")]),
    );

    const result = await describeTranscript(file);
    expect(result).toEqual({
      path: file,
      toolCallId: "tc2",
      agent: "Explore",
      description: "read hello",
      created: CREATED,
      modified: Math.round((await stat(file)).mtimeMs),
      messageCount: 2,
    });
  });

  it("returns undefined when the first valid entry is not a session header", async () => {
    const file = path.join(dir, "no-header.jsonl");
    await writeFile(file, transcript([message(0), JSON.stringify(HEADER), link()]));
    expect(await describeTranscript(file)).toBeUndefined();
  });
});
