/**
 * The `/proc` entries Android denies an app, written by the runtime so the tools that read them work.
 *
 * Android's kernel refuses an app `/proc/stat`, `/proc/uptime`, `/proc/loadavg` and `/proc/vmstat`, so inside the
 * runtime — where the process is root — `htop` still answers `Cannot open /proc/stat: Permission denied`. The
 * launcher binds the files this module writes over those entries.
 *
 * Whatever can still be had is real: libuv reads the uptime and the load average through `sysinfo(2)`, which Android
 * leaves alone, and `/proc/meminfo` is readable. The rest is a well-formed stand-in, because a tool that reads the
 * file cannot be given the kernel's numbers. `/proc/stat`'s per-CPU times are the one real loss: they live only in
 * the file Android hides, so its counters are zero and report no CPU time rather than a stale snapshot's.
 */
import { readdir, readFile, writeFile } from "node:fs/promises";
import os from "node:os";
import { join } from "node:path";

/** The entries bound over, and the file each keeps in the synthetic `/proc`. */
export const SYNTHETIC_PROC = ["stat", "vmstat", "loadavg", "uptime"] as const;

/** How often the files are rewritten: a tool reading one twice should see time pass. */
const REFRESH_MS = 2_000;

const PAGE_BYTES = 4096;

/** The `cpu` line's ten counters: user, nice, system, idle, iowait, irq, softirq, steal, guest, guest_nice. */
const CPU_COUNTERS = 10;

/**
 * `/proc/stat`, with a line per CPU and the boot time `sysinfo(2)` gives. The counters are zero: Android's
 * `/proc/stat` is the only place the kernel's per-CPU times were, and it denies the app that would read it.
 */
export function statText(now: number = Date.now(), uptimeSeconds: number = os.uptime()): string {
  const cpus = Math.max(1, os.availableParallelism());
  const zeros = Array.from({ length: CPU_COUNTERS }, () => "0").join(" ");
  const lines = [`cpu  ${zeros}`];
  for (let cpu = 0; cpu < cpus; cpu++) lines.push(`cpu${cpu} ${zeros}`);
  const boot = Math.floor(now / 1000 - uptimeSeconds);
  lines.push(
    // An empty intr line: the kernel's per-irq counts are in the hidden /proc/stat, and a count here would be a lie.
    "intr 0",
    "ctxt 0",
    `btime ${boot}`,
    "processes 0",
    "procs_running 1",
    "procs_blocked 0",
    `softirq ${zeros}`,
  );
  return `${lines.join("\n")}\n`;
}

/** `/proc/vmstat` with the free page count `/proc/meminfo` still tells us. */
export function vmstatText(): string {
  return `nr_free_pages ${Math.floor(os.freemem() / PAGE_BYTES)}\n`;
}

/**
 * `/proc/loadavg`: the load average `sysinfo(2)` gives, then the runnable and total processes and the last pid. The
 * two counts are the task count a tool shows, and Android hides other apps' processes, so they count this app's.
 */
export function loadavgText(processes: { running: number; total: number }, lastPid: number = process.pid): string {
  // Defaults, because the array's length is not part of its type: a kernel always gives three, a test one may not.
  const [one = 0, five = 0, fifteen = 0] = os.loadavg();
  return `${one.toFixed(2)} ${five.toFixed(2)} ${fifteen.toFixed(2)} ${processes.running}/${processes.total} ${lastPid}\n`;
}

/**
 * `/proc/uptime`: the seconds since boot `sysinfo(2)` gives, then the idle seconds. The idle time is the kernel's own
 * and lives in the hidden `/proc/stat`, so this reports none.
 */
export function uptimeText(seconds: number = os.uptime()): string {
  return `${seconds.toFixed(2)} 0.00\n`;
}

/** How many of this app's processes are running, from the `/proc/<pid>/stat` files Android lets an app read. */
async function countProcesses(): Promise<{ running: number; total: number }> {
  try {
    const names = await readdir("/proc");
    let running = 0;
    let total = 0;
    for (const name of names) {
      if (!/^[0-9]+$/.test(name)) continue;
      total += 1;
      const stat = await readFile(`/proc/${name}/stat`, "utf8").catch(() => null);
      if (stat === null) continue;
      // The state is the field after the command in parentheses, which may itself hold spaces and parentheses.
      const state = stat.slice(stat.lastIndexOf(")") + 1).trim().split(/\s+/)[0];
      if (state === "R") running += 1;
    }
    return { running, total };
  } catch {
    return { running: 1, total: 1 };
  }
}

/** Writes every synthetic `/proc` file into [procDir], which the launcher binds over the real entries. */
export async function writeProcFiles(procDir: string): Promise<void> {
  const processes = await countProcesses();
  const uptime = os.uptime();
  const files: [string, string][] = [
    ["stat", statText(Date.now(), uptime)],
    ["vmstat", vmstatText()],
    ["loadavg", loadavgText(processes)],
    ["uptime", uptimeText(uptime)],
  ];
  // A file the runtime cannot write only leaves the entry as it was; the rest are still worth writing.
  await Promise.all(files.map(([name, text]) => writeFile(join(procDir, name), text).catch(() => undefined)));
}

/**
 * Keeps [procDir]'s files current for as long as the host runs, and returns a function that stops it. The timer is
 * unref'd, so a host that would otherwise exit still does.
 */
export function startSyntheticProc(procDir: string): () => void {
  const write = (): void => {
    void writeProcFiles(procDir);
  };
  write();
  const timer = setInterval(write, REFRESH_MS);
  timer.unref();
  return () => clearInterval(timer);
}