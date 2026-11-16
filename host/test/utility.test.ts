import { readFile } from "node:fs/promises";
import path from "node:path";
import { afterEach, describe, expect, it } from "vitest";
import type { JsonObject } from "../src/framing.js";
import { type Dirs, type HostRun, makeDirs, removeDirs, startHost } from "./harness.js";

describe("utility mode", () => {
  let dirs: Dirs;
  let run: HostRun;
  let nextId = 0;

  /**
   * Starts the host. The operations it answers need Pi, which the runtime here does not have: what is being tested
   * is the host itself — that it stays up and answers every question — and the operations' own answers are covered
   * by the tests of the modes they come from.
   */
  async function start(): Promise<void> {
    dirs = await makeDirs();
    ({ run } = await startHost("utility", dirs));
    // The host answers its hello, then waits for a request before the mode starts, as every mode does.
    await run.next();
    run.send({ type: "fang_request", params: {} });
  }

  afterEach(async () => {
    run.socket.destroy();
    await removeDirs(dirs);
  });

  /** Sends the operation [op] and returns the answer carrying its id. */
  async function ask(op: string, params: JsonObject = {}): Promise<JsonObject | undefined> {
    const id = ++nextId;
    run.send({ type: "fang_utility_request", id, op, params });
    for (;;) {
      const message = await run.next();
      if (message === undefined || message.id === id) return message;
    }
  }

  it("says it is ready", async () => {
    await start();

    expect(await run.next()).toEqual({ type: "fang_utility_ready" });
  });

  it("answers more than one question from the same host", async () => {
    await start();
    await run.next();

    const first = await ask("sessions");
    const second = await ask("sessions");

    expect(first?.type).toBe("fang_result");
    expect(first?.id).toBe(1);
    expect(second?.type).toBe("fang_result");
    expect(second?.id).toBe(2);
  });

  /**
   * The app sends every one-off operation here. The accounts operations went missing from the registry once and every
   * call returned unknown_op, so a new op registered only in main.ts must fail this test.
   */
  it("registers every operation the app sends to the utility host", async () => {
    const client = await readFile(
      path.join(import.meta.dirname, "../../app/src/main/kotlin/io/leloush/fang/host/HostClient.kt"),
      "utf8",
    );
    const ops = [...new Set([...client.matchAll(/utilityData\(\s*"([a-z_]+)"/g)].map((match) => match[1] as string))];
    expect(ops.length).toBeGreaterThan(15);
    await start();
    await run.next();
    for (const op of ops) {
      const answer = await ask(op);
      expect(answer).toBeDefined();
      // Most operations need a real Pi install and fail with their own code; that is fine. Being unregistered is not.
      expect({ op, code: answer?.code }).not.toHaveProperty("code", "unknown_op");
    }
  });

  it("says so for an operation it does not have", async () => {
    await start();
    await run.next();

    const answer = await ask("nonsense");

    expect(answer?.type).toBe("fang_result");
    expect(answer?.ok).toBe(false);
    expect(answer?.code).toBe("unknown_op");
  });

  it("reports a failure with its code, and keeps answering", async () => {
    // A runtime without Pi: the operation cannot run, and the host says so instead of ending.
    await start();
    await run.next();

    const failed = await ask("sessions");
    const after = await ask("sessions");

    expect(failed?.type).toBe("fang_result");
    expect(failed?.ok).toBe(false);
    expect(typeof failed?.code).toBe("string");
    expect(after?.type).toBe("fang_result");
  });
});
