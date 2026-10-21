import { describe, expect, it } from "vitest";
import { isSearchablePath } from "../src/folder-search.js";

/**
 * A search root can be the runtime's own root, which holds the virtual filesystems and Android's bound-in system
 * images. Walking one costs a syscall storm, and PRoot intercepts every one, so the search would spend its whole
 * budget there instead of on the folders it was asked for.
 */
describe("isSearchablePath", () => {
  it("refuses the virtual filesystems and Android's system images", () => {
    const refused = ["/proc", "/sys", "/dev", "/apex", "/odm", "/product", "/system", "/system_ext", "/vendor"];
    for (const path of refused) expect(isSearchablePath(path)).toBe(false);
  });

  it("walks a folder of those names that is not the system one", () => {
    // A project may keep a folder called `dev` or `vendor`; only the system folder itself is refused.
    expect(isSearchablePath("/sdcard/project/dev")).toBe(true);
    expect(isSearchablePath("/sdcard/project/vendor")).toBe(true);
    expect(isSearchablePath("/proc/1/task")).toBe(true);
  });

  it("walks the runtime's own root and the folders under it", () => {
    for (const path of ["/", "/usr", "/usr/src", "/var", "/root", "/sdcard/projects"]) {
      expect(isSearchablePath(path)).toBe(true);
    }
  });
});