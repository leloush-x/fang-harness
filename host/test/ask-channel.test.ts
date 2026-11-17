import { describe, expect, it } from "vitest";
import type { FangChannel } from "../src/pi-extensions/fang-channel.js";
import {
  askViaChannel,
  decodeAskResponse,
  encodeAskRequest,
  type ChannelAnswer,
} from "../src/pi-extensions/fang-ask-user/fang-ask-channel.js";

const ID = "ask-1";

const answered: ChannelAnswer = {
  status: "answered",
  response: { kind: "selection", selections: ["One", "Two"], comment: "because" },
};

describe("ask channel wire format", () => {
  it("encodes the request with its id and optional fields", () => {
    expect(encodeAskRequest(ID, {
      heading: "Pick",
      questions: [{ question: "Which?", options: [], allowMultiple: false, allowFreeform: true }],
      allowComment: false,
      timeout: 5000,
    })).toEqual({
      id: ID,
      heading: "Pick",
      questions: [{ question: "Which?", options: [], allowMultiple: false, allowFreeform: true }],
      allowComment: false,
      timeout: 5000,
    });
    expect(encodeAskRequest(ID, { questions: [], allowComment: true })).toEqual({
      id: ID,
      questions: [],
      allowComment: true,
    });
  });

  it("decodes a valid answered message", () => {
    expect(decodeAskResponse({ id: ID, answers: [answered] }, ID)).toEqual({ kind: "answers", answers: [answered] });
  });

  it("decodes a skipped answer", () => {
    expect(decodeAskResponse({ id: ID, answers: [{ status: "skipped" }] }, ID)).toEqual({
      kind: "answers",
      answers: [{ status: "skipped" }],
    });
  });

  it("ignores a cancelled message with the wrong id", () => {
    expect(decodeAskResponse({ id: "other", answers: [], cancelled: true }, ID)).toBeUndefined();
  });

  it("decodes a cancellation with the right id", () => {
    expect(decodeAskResponse({ id: ID, answers: [], cancelled: true }, ID)).toEqual({ kind: "cancelled" });
  });
});

/** A channel that records what the extension sent and lets the test answer it. */
function fakeChannel() {
  const handlers = new Map<string, (data: unknown) => void>();
  const sent: unknown[] = [];
  const channel: FangChannel = {
    send(topic, data) {
      if (topic === "ask") sent.push(data);
    },
    on(topic, handler) {
      handlers.set(topic, handler);
      return () => handlers.delete(topic);
    },
  };
  return { channel, sent, answer: (data: unknown) => handlers.get("ask")?.(data), handlers };
}

describe("ask channel round trip", () => {
  it("sends a request and resolves with the matching answer", async () => {
    const { channel, sent, answer, handlers } = fakeChannel();
    const pending = askViaChannel(
      { questions: [{ question: "Which?", options: [], allowMultiple: false, allowFreeform: true }], allowComment: false },
      undefined,
      undefined,
      channel,
    );
    const request = sent[0] as { id: string };
    expect(request.id).toBeTruthy();
    answer({ id: "other", answers: [answered] });
    answer({ id: request.id, answers: [answered] });
    expect(await pending).toEqual([answered]);
    expect(handlers.has("ask")).toBe(false);
  });

  it("resolves null on abort and removes its listener", async () => {
    const { channel, handlers } = fakeChannel();
    const controller = new AbortController();
    const pending = askViaChannel(
      { questions: [{ question: "Which?", options: [], allowMultiple: false, allowFreeform: true }], allowComment: false },
      controller.signal,
      undefined,
      channel,
    );
    controller.abort();
    expect(await pending).toBeNull();
    expect(handlers.has("ask")).toBe(false);
  });
});
