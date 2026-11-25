import { detectModel } from "./detect.js";
import { HostError } from "./env.js";
import { isObject, type JsonObject } from "./framing.js";
import type { ModeContext } from "./mode.js";
import type { ModelDef } from "./model-def.js";

const TIMEOUT_MS = 15_000;
const BODY_PREVIEW = 300;
/** A few pages is enough for a large catalogue; a broken cursor must not keep the host fetching. */
const MAX_PAGES = 10;
/** Error pages and wrong base URLs answer with HTML, which says nothing useful as text. */
const HTML_BODY = /^\s*<(!doctype|html|head|body)/i;

/**
 * Fetches an OpenAI-style `/models` list at [url] and reads each entry's capabilities, keyed by id in
 * the server's order. Later pages are followed when the server supplies a next URL or cursor, so an endpoint that
 * paginates doesn't silently lose models. `baseUrl` names the server in messages. Failures carry a code per cause
 * (`network`, `timeout`, `unauthorized`, `not_found`, `rate_limited`, `server_error`, `http_error`,
 * `bad_response`) and a message with what the server said, for Fang to explain.
 */
export async function fetchModelList(
  ctx: ModeContext,
  baseUrl: string,
  url: string,
  apiKey: string,
): Promise<Map<string, ModelDef>> {
  const detected = new Map<string, ModelDef>();
  const visited = new Set<string>();
  let page: string | null = url;
  for (let count = 0; count < MAX_PAGES && page !== null && !visited.has(page); count++) {
    visited.add(page);
    const current = page;
    const { list, next } = await fetchPage(ctx, baseUrl, current, apiKey);
    for (const entry of list) {
      if (!isObject(entry)) continue;
      // Ollama names its models with `name` instead of `id`; numeric or empty ids say nothing useful.
      const id = typeof entry.id === "string" ? entry.id.trim() : typeof entry.name === "string" ? entry.name.trim() : "";
      if (id !== "" && !detected.has(id)) detected.set(id, detectModel(id, entry));
    }
    page = next;
  }
  return detected;
}

/** One page and the URL of the next one, or null when the server says there is none. */
async function fetchPage(
  ctx: ModeContext,
  baseUrl: string,
  url: string,
  apiKey: string,
): Promise<{ list: unknown[]; next: string | null }> {
  let response: Response;
  let body: string;
  // The body read shares the timeout and failure handling: a server can send headers and then stall or hang up.
  try {
    response = await fetch(url, {
      headers: { Authorization: `Bearer ${apiKey}` },
      signal: AbortSignal.any([ctx.signal, AbortSignal.timeout(TIMEOUT_MS)]),
    });
    body = await response.text();
  } catch (error) {
    if (error instanceof DOMException && error.name === "TimeoutError") {
      throw new HostError("timeout", `No response from ${baseUrl} within 15 s`);
    }
    if (ctx.signal.aborted) throw error;
    if (!(error instanceof Error && error.cause instanceof Error)) {
      throw new HostError("network", `Could not reach ${baseUrl}`, { key: "provider_unreachable", args: { url: baseUrl } });
    }
    const cause = error.cause.message;
    throw new HostError("network", `Could not reach ${baseUrl}: ${cause}`, {
      key: "provider_unreachable_because",
      args: { url: baseUrl, cause },
    });
  }
  if (!response.ok) {
    const code = httpErrorCode(response.status);
    const said = serverMessage(body);
    if (said === "") {
      throw new HostError(code, `HTTP ${response.status}`, { key: "provider_http_status", args: { status: response.status } });
    }
    throw new HostError(code, `HTTP ${response.status}: ${said}`, {
      key: "provider_http_status_said",
      args: { status: response.status, message: said },
    });
  }
  let parsed: unknown;
  try {
    parsed = JSON.parse(body);
  } catch {
    if (HTML_BODY.test(body)) {
      throw new HostError("bad_response", `${url} returned a web page, not JSON`, { key: "provider_web_page", args: { url } });
    }
    const preview = body.slice(0, BODY_PREVIEW);
    throw new HostError("bad_response", `The server did not return JSON: ${preview}`, {
      key: "provider_not_json",
      args: { body: preview },
    });
  }
  const envelope = isObject(parsed) ? parsed : {};
  // An OpenAI list with nothing in it can come as `data: null`, as Ollama sends before any model is downloaded.
  const data = envelope.object === "list" && envelope.data === null ? [] : envelope.data;
  const list = Array.isArray(data) ? data : envelope.models;
  if (!Array.isArray(list)) {
    throw new HostError("bad_response", "The server's /models response has no data list", { key: "provider_no_data_list" });
  }
  return { list, next: nextPage(parsed, url) };
}

/** A next URL the server named, or a cursor it said to continue from; null at the last page. */
function nextPage(envelope: JsonObject, current: string): string | null {
  const named = [envelope.next, envelope.next_url, envelope.nextLink].find(
    (value): value is string => typeof value === "string" && /^https?:\/\//.test(value),
  );
  if (named !== undefined) return named;
  if (envelope.has_more !== true) return null;
  const cursor = [envelope.next_cursor, envelope.last_id, envelope.after].find(
    (value): value is string => typeof value === "string" && value.length > 0,
  );
  if (cursor === undefined) return null;
  try {
    const url = new URL(current);
    url.searchParams.set("after", cursor);
    return url.toString();
  } catch {
    return null;
  }
}

function httpErrorCode(status: number): string {
  if (status === 401 || status === 403) return "unauthorized";
  if (status === 404) return "not_found";
  if (status === 429) return "rate_limited";
  if (status >= 500) return "server_error";
  return "http_error";
}

/** The message in an error body: OpenAI's `{error: {message}}`, a bare `{error}` string or FastAPI's `{detail}`. */
function serverMessage(body: string): string {
  if (HTML_BODY.test(body)) return "";
  try {
    const parsed: unknown = JSON.parse(body);
    if (isObject(parsed)) {
      const error = parsed.error;
      const candidates = [isObject(error) ? error.message : error, parsed.message, parsed.detail];
      const message = candidates.find((value): value is string => typeof value === "string" && value.trim() !== "");
      if (message !== undefined) return message.trim().slice(0, BODY_PREVIEW);
    }
  } catch {
    // Not JSON: the body itself is the message.
  }
  return body.trim().slice(0, BODY_PREVIEW);
}
