import { HostError } from "../env.js";
import { isObject, type JsonObject } from "../framing.js";
import type { ModeContext } from "../mode.js";
import { authStatus } from "./auth-status.js";
import { defaultModel } from "./default-model.js";
import { logout } from "./logout.js";
import { modelDefaults } from "./model-defaults.js";
import { providerModels } from "./provider-models.js";
import { providerRemove } from "./provider-remove.js";
import { providerSave } from "./provider-save.js";
import { sessionMessages } from "./session-messages.js";
import { sessions } from "./sessions.js";
import { subscriptionSave } from "./subscription-save.js";

/** The operations this host answers, and the function that answers each. */
const OPERATIONS: Record<string, (ctx: ModeContext) => Promise<JsonObject>> = {
  auth_status: authStatus,
  default_model: defaultModel,
  logout,
  model_defaults: modelDefaults,
  provider_models: providerModels,
  provider_remove: providerRemove,
  provider_save: providerSave,
  session_messages: sessionMessages,
  sessions,
  subscription_save: subscriptionSave,
};

/**
 * Long-lived access to the questions the app asks the runtime most: the session list, the sign-in state, the models
 * an endpoint offers, a model's defaults, a session's messages.
 *
 * Each of them was a mode of its own, and each started a process that loaded Pi's own software: twenty megabytes of
 * modules, every file of them opened through PRoot, which takes about ten seconds. Asking a host that stays up means
 * that cost is paid once instead of once per question.
 *
 * The host sends `fang_utility_ready`, then answers each `fang_utility_request` with a `fang_utility_result` that
 * echoes the request's `id`. A failure is in the result, with its `code` and its `i18n`, so the app can say why.
 */
export async function utility(ctx: ModeContext): Promise<JsonObject> {
  ctx.conn.send({ type: "fang_utility_ready" });
  // Returning exits the process, so a hang-up first waits for the answers still on their way.
  const inFlight = new Set<Promise<void>>();
  ctx.conn.listen((message) => {
    const { id, op } = message;
    if (typeof id !== "number" || typeof op !== "string") return;
    const run = OPERATIONS[op];
    if (run === undefined) {
      ctx.conn.send({ type: "fang_utility_result", id, error: `Unknown operation ${op}`, code: "unknown_operation" });
      return;
    }
    const params = isObject(message.params) ? message.params : {};
    const sent = answer(ctx, run, params, id).catch(() => undefined);
    inFlight.add(sent);
    void sent.finally(() => inFlight.delete(sent));
  });
  if (!ctx.signal.aborted) {
    const { promise, resolve: hungUp } = Promise.withResolvers<void>();
    ctx.signal.addEventListener("abort", () => hungUp(), { once: true });
    await promise;
  }
  await Promise.allSettled(inFlight);
  return {};
}

/** Runs [run] for the request's [params] and answers with the result, or with why it failed. */
async function answer(
  ctx: ModeContext,
  run: (ctx: ModeContext) => Promise<JsonObject>,
  params: JsonObject,
  id: number,
): Promise<void> {
  try {
    const data = await run({ ...ctx, params });
    ctx.conn.send({ type: "fang_utility_result", id, data });
  } catch (error) {
    if (error instanceof HostError) {
      ctx.conn.send({ type: "fang_utility_result", id, error: error.message, code: error.code, i18n: error.i18n });
      return;
    }
    ctx.conn.send({
      type: "fang_utility_result",
      id,
      error: error instanceof Error ? error.message : String(error),
      code: "failed",
    });
  }
}
