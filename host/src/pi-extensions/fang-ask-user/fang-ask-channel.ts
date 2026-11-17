/**
 * Fang Harness: the extension's side of the app's rich `ask_user` card (channel topic "ask"). Upstream pi-ask-user has
 * no such channel and falls back to plain select/input dialogs when ctx.ui.custom() is unavailable in RPC mode.
 */
import { fangChannel, type FangChannel } from "../fang-channel.js";

const TOPIC = "ask";

export interface ChannelOption {
  title: string;
  description?: string;
}

export interface ChannelQuestion {
  question: string;
  context?: string;
  options: ChannelOption[];
  allowMultiple: boolean;
  allowFreeform: boolean;
}

/** Extension -> app. The transport assigns `id`; everything else comes from the caller. */
export interface AskRequest {
  id: string;
  heading?: string;
  questions: ChannelQuestion[];
  allowComment: boolean;
  timeout?: number;
}

export type AskRequestData = Omit<AskRequest, "id">;

/** App -> extension. One entry per question, index-aligned with the request. */
export type ChannelAnswer =
  | { status: "answered"; response:
      | { kind: "selection"; selections: string[]; comment?: string }
      | { kind: "freeform"; text: string } }
  | { status: "skipped" };

/** One decoded app message, or undefined when it is not a response to our request. */
export type DecodedAskResponse = { kind: "answers"; answers: ChannelAnswer[] } | { kind: "cancelled" };

let nextId = 0;

/** Build the wire payload, giving the request an id so a late answer from an older card is ignored. */
export function encodeAskRequest(id: string, data: AskRequestData): AskRequest {
  return {
    id,
    ...(data.heading !== undefined ? { heading: data.heading } : {}),
    questions: data.questions,
    allowComment: data.allowComment,
    ...(data.timeout !== undefined ? { timeout: data.timeout } : {}),
  };
}

function isChannelAnswer(value: unknown): value is ChannelAnswer {
  if (!value || typeof value !== "object") return false;
  const answer = value as Record<string, unknown>;
  if (answer.status === "skipped") return true;
  if (answer.status !== "answered") return false;
  const response = answer.response;
  if (!response || typeof response !== "object") return false;
  const body = response as Record<string, unknown>;
  if (body.kind === "freeform") return typeof body.text === "string";
  if (body.kind === "selection") {
    return Array.isArray(body.selections)
      && body.selections.every((title) => typeof title === "string")
      && (body.comment === undefined || typeof body.comment === "string");
  }
  return false;
}

/** Decode one app message. Wrong id or a malformed body is ignored; a cancel resolves the ask with null. */
export function decodeAskResponse(data: unknown, id: string): DecodedAskResponse | undefined {
  if (!data || typeof data !== "object") return undefined;
  const message = data as Record<string, unknown>;
  if (message.id !== id) return undefined;
  if (message.cancelled === true) return { kind: "cancelled" };
  const answers = message.answers;
  if (Array.isArray(answers) && answers.every(isChannelAnswer)) return { kind: "answers", answers };
  return undefined;
}

/**
 * Show the app's card and resolve with its answers, or null when it is cancelled, aborted, or the deadline passes. The
 * listener is removed however the call ends, so a late answer from an earlier card cannot be mistaken for this one.
 */
export function askViaChannel(
  data: AskRequestData,
  signal?: AbortSignal,
  deadline?: number,
  channel: FangChannel = fangChannel(),
): Promise<ChannelAnswer[] | null> {
  const id = `${Date.now()}-${++nextId}`;
  if (signal?.aborted || (deadline !== undefined && deadline <= Date.now())) return Promise.resolve(null);
  return new Promise((resolve) => {
    let settled = false;
    let timer: ReturnType<typeof setTimeout> | undefined;
    let onAbort: (() => void) | undefined;
    let off: (() => void) | undefined;

    const finish = (value: ChannelAnswer[] | null) => {
      if (settled) return;
      settled = true;
      off?.();
      if (timer !== undefined) clearTimeout(timer);
      if (onAbort) signal?.removeEventListener("abort", onAbort);
      resolve(value);
    };

    off = channel.on(TOPIC, (incoming) => {
      const decoded = decodeAskResponse(incoming, id);
      if (decoded === undefined) return;
      finish(decoded.kind === "cancelled" ? null : decoded.answers);
    });
    channel.send(TOPIC, encodeAskRequest(id, data));

    if (signal?.aborted) {
      finish(null);
    } else if (signal) {
      onAbort = () => finish(null);
      signal.addEventListener("abort", onAbort, { once: true });
    }
    if (!settled && deadline !== undefined) {
      const remaining = deadline - Date.now();
      if (remaining <= 0) finish(null);
      else timer = setTimeout(() => finish(null), remaining);
    }
  });
}
