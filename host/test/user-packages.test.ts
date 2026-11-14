import { describe, expect, it } from "vitest";
import { gitRepository, userNpmPackages, userPackages } from "../src/settings.js";

describe("userPackages", () => {
  it("reads npm names without versions, from strings and { source } entries", () => {
    const settings = {
      packages: ["npm:pi-web-access", "npm:@gotgenes/pi-anthropic-auth@3.1.0", { source: "npm:pi-ask-user@0.17.2" }],
    };
    expect(userPackages(settings).npm).toEqual(
      new Set(["pi-web-access", "@gotgenes/pi-anthropic-auth", "pi-ask-user"]),
    );
  });

  it("reads the repository of every git source form, without a ref", () => {
    const settings = {
      packages: [
        "git:github.com/edlsh/pi-ask-user",
        "git:github.com/edlsh/pi-ask-user#v0.17.2",
        { source: "git:https://github.com/edlsh/pi-ask-user.git" },
        "git:ssh://git@github.com/edlsh/pi-ask-user@main",
        "git@github.com:edlsh/pi-ask-user.git",
        "https://github.com/edlsh/pi-ask-user",
      ],
    };
    expect(userPackages(settings).git).toEqual(new Set(["github.com/edlsh/pi-ask-user"]));
  });

  it("leaves out local paths and packages it cannot identify", () => {
    const settings = { packages: ["./plugin", "/abs/plugin", "git:~/repo", 42, { source: 7 }] };
    expect(userPackages(settings)).toEqual({ npm: new Set(), git: new Set() });
  });

  it("reads no packages from settings without a package list", () => {
    expect(userPackages(undefined)).toEqual({ npm: new Set(), git: new Set() });
    expect(userPackages({})).toEqual({ npm: new Set(), git: new Set() });
    expect(userPackages({ packages: "npm:pi-web-access" })).toEqual({ npm: new Set(), git: new Set() });
  });

  it("keeps userNpmPackages to the npm names", () => {
    const settings = { packages: ["npm:pi-web-access", "git:github.com/edlsh/pi-ask-user"] };
    expect(userNpmPackages(settings)).toEqual(new Set(["pi-web-access"]));
  });
});

describe("gitRepository", () => {
  it("normalizes the forms Pi installs to host/path", () => {
    for (const source of [
      "git:github.com/edlsh/pi-ask-user",
      "git:github.com/edlsh/pi-ask-user/",
      "git:github.com/edlsh/pi-ask-user.git",
      "git:github.com/edlsh/pi-ask-user#v0.17.2",
      "git:https://github.com/edlsh/pi-ask-user.git",
      "git:ssh://git@github.com/edlsh/pi-ask-user",
      "git@github.com:edlsh/pi-ask-user.git",
      "https://github.com/edlsh/pi-ask-user",
      "ssh://git@github.com/edlsh/pi-ask-user#main",
    ]) {
      expect(gitRepository(source)).toBe("github.com/edlsh/pi-ask-user");
    }
  });

  it("ignores the case of a host and path", () => {
    expect(gitRepository("git:GitHub.com/Edlsh/Pi-Ask-User")).toBe("github.com/edlsh/pi-ask-user");
  });

  it("returns null for a local path or a source with no repository", () => {
    expect(gitRepository("./plugin")).toBeNull();
    expect(gitRepository("/abs/plugin")).toBeNull();
    expect(gitRepository("npm:pi-web-access")).toBeNull();
    expect(gitRepository("git:github.com")).toBeNull();
  });
});
