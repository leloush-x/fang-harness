import { describe, expect, it } from "vitest";
import { detectModel } from "../src/detect.js";

/** The app calls a model free when the id says so or all four prices are zero. */
describe("free-model detection", () => {
  it("reads OpenRouter's per-token price strings", () => {
    const model = detectModel("deepseek/deepseek-r1:free", {
      pricing: { prompt: "0", completion: "0", input_cache_read: "0", input_cache_write: "0" },
    });
    expect(model.cost).toEqual({ input: 0, output: 0, cacheRead: 0, cacheWrite: 0 });
  });

  it("accepts an explicit free flag when the list gives no prices", () => {
    for (const entry of [
      { id: "some/model", is_free: true },
      { id: "some/model", free: true },
      { id: "some/model", metadata: { is_free: true } },
    ]) {
      expect(detectModel(entry.id as string, entry).cost).toEqual({ input: 0, output: 0, cacheRead: 0, cacheWrite: 0 });
    }
  });

  it("accepts a free id suffix when the list gives no prices", () => {
    expect(detectModel("vendor/model:free", {}).cost).toEqual({ input: 0, output: 0, cacheRead: 0, cacheWrite: 0 });
  });

  it("does not call a model with no price information free", () => {
    expect(detectModel("vendor/model", {}).cost).toBeUndefined();
  });
});
