import type { JsonObject } from "./framing.js";
import type { ModeContext } from "./mode.js";
import { type ModelDef, parseModelDef } from "./model-def.js";
import { createRuntime } from "./runtime.js";

/** Providers whose copy of a shared model id is preferred, best first. */
const PREFERRED_PROVIDERS = ["openai", "openai-codex", "anthropic", "google"];

export type Catalogue = (id: string) => ModelDef | undefined;

/**
 * The catalogue per api, kept for as long as the process lives. It is Pi's built-in catalogue, which never changes,
 * and loading it means starting Pi's model runtime, which takes a while under PRoot.
 */
const CACHE = new Map<string, Promise<Catalogue>>();

/**
 * Looks model ids up in Pi's built-in catalogue (never `models.json`). When several built-in
 * providers list an id, the copy with the requested `api` wins, then `PREFERRED_PROVIDERS` order,
 * then catalogue order.
 */
export async function loadCatalogue(ctx: ModeContext, api: string): Promise<Catalogue> {
  const cached = CACHE.get(api);
  if (cached !== undefined) return cached;
  const loading = build(ctx, api);
  CACHE.set(api, loading);
  // A failure is not kept, so the next call tries again.
  void loading.catch(() => CACHE.delete(api));
  return loading;
}

async function build(ctx: ModeContext, api: string): Promise<Catalogue> {
  const runtime = await createRuntime(ctx, { builtinOnly: true });
  const best = new Map<string, { rank: number; def: ModelDef }>();
  for (const model of runtime.getModels()) {
    const preferred = PREFERRED_PROVIDERS.indexOf(model.provider);
    const rank = (model.api === api ? 0 : 100) + (preferred === -1 ? PREFERRED_PROVIDERS.length : preferred);
    const current = best.get(model.id);
    if (current === undefined || rank < current.rank) {
      best.set(model.id, { rank, def: parseModelDef(model.id, model as unknown as JsonObject) });
    }
  }
  return (id) => best.get(id)?.def;
}
