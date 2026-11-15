import { describe, expect, it } from "vitest";
import { ASK_USER_DESCRIPTION, ASK_USER_GUIDELINES, ASK_USER_SNIPPET } from "../src/pi-extensions/fang-ask-user/fang-prompt.js";

describe("fang-ask-user prompt", () => {
  it("is a non-empty description with a trigger sentence", () => {
    expect(ASK_USER_DESCRIPTION.length).toBeGreaterThan(40);
    expect(ASK_USER_DESCRIPTION).toContain("Ask the user");
    expect(ASK_USER_DESCRIPTION).toContain("context");
  });

  it("has a short snippet", () => {
    expect(ASK_USER_SNIPPET.length).toBeGreaterThan(10);
    expect(ASK_USER_SNIPPET.length).toBeLessThan(120);
  });

  it("carries the guidance the model needs", () => {
    expect(ASK_USER_GUIDELINES.length).toBeGreaterThanOrEqual(6);
    const joined = ASK_USER_GUIDELINES.join("\n");
    expect(joined).toContain("context");
    expect(joined).toContain("questions");
    expect(joined).toContain("allowFreeform");
    expect(joined).toContain("blocking");
    expect(joined).toContain("dismiss");
  });

  it("never tells the model to combine several questions into one", () => {
    const joined = ASK_USER_GUIDELINES.join("\n");
    expect(joined).toContain("one focused question per call");
    expect(joined).toContain("must not contain");
  });
});
