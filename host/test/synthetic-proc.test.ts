import { describe, expect, it } from "vitest";
import { loadavgText, statText, uptimeText, vmstatText } from "../src/synthetic-proc.js";

/** The fields `/proc/stat`'s `cpu` line holds: the ten counters, after the label. */
const CPU_COUNTERS = 10;

function cpulines(text: string): string[] {
  return text.split("\n").filter((line) => line.startsWith("cpu"));
}

describe("synthetic /proc", () => {
  describe("statText", () => {
    it("writes one line per CPU, each with the ten counters a reader expects", () => {
      const lines = cpulines(statText(1_700_000_000_000, 100_000));

      // The aggregate line, then one per CPU; os.availableParallelism() is at least 1 wherever this runs.
      expect(lines.length).toBeGreaterThanOrEqual(2);
      expect(lines[0]?.startsWith("cpu  ")).toBe(true);
      expect(lines[1]?.startsWith("cpu0 ")).toBe(true);
      for (const line of lines) {
        expect(line.trim().split(/\s+/)).toHaveLength(CPU_COUNTERS + 1);
      }
    });

    it("names the CPUs in order, without a gap", () => {
      const cpus = cpulines(statText()).slice(1).map((line) => line.split(/\s+/)[0]);
      expect(cpus).toEqual(cpus.map((_, index) => `cpu${index}`));
    });

    it("gives the boot time the uptime implies, rather than a stale one", () => {
      const text = statText(1_700_000_000_000, 1_000);
      expect(text).toContain(`btime ${1_700_000_000 - 1_000}`);
    });

    it("reports no CPU time, since the kernel's counts are in the file Android hides", () => {
      const [aggregate] = cpulines(statText()).slice(0, 1);
      expect(aggregate?.trim().split(/\s+/).slice(1)).toEqual(Array.from({ length: CPU_COUNTERS }, () => "0"));
    });
  });

  describe("uptimeText", () => {
    it("writes the uptime and an idle time", () => {
      expect(uptimeText(1234.5)).toBe("1234.50 0.00\n");
    });

    it("gives a real uptime by default, which libuv reads through sysinfo(2)", () => {
      expect(Number.parseFloat(uptimeText())).toBeGreaterThan(0);
    });
  });

  describe("loadavgText", () => {
    it("writes the three loads, the runnable and total processes, and a last pid", () => {
      const fields = loadavgText({ running: 2, total: 7 }, 4242).trim().split(" ");
      expect(fields).toHaveLength(5);
      expect(fields[3]).toBe("2/7");
      expect(fields[4]).toBe("4242");
      for (const load of fields.slice(0, 3)) expect(load).toMatch(/^[0-9]+\.[0-9]{2}$/);
    });

    it("gives real loads by default, which libuv reads through sysinfo(2)", () => {
      const loads = loadavgText({ running: 1, total: 1 })
        .trim()
        .split(" ")
        .slice(0, 3)
        .map(Number.parseFloat);
      expect(loads.every((load) => Number.isFinite(load))).toBe(true);
    });
  });

  describe("vmstatText", () => {
    it("writes the free page count", () => {
      expect(vmstatText()).toMatch(/^nr_free_pages [0-9]+\n$/);
    });
  });
});