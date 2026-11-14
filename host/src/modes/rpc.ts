import { spawn } from "node:child_process";
import { readFile, stat, writeFile } from "node:fs/promises";
import type { Socket } from "node:net";
import { join } from "node:path";
import type { Readable } from "node:stream";
import {
  AppInputRouter,
  CHANNEL_FD,
  ChannelReader,
  encodeChannelMessage,
  HELLO_TOPIC,
  OutputMerger,
} from "../channel.js";
import { folderNotFound, HostError, installedPiVersion, loadPi, piEntry } from "../env.js";
import { isObject } from "../framing.js";
import type { ModeContext } from "../mode.js";
import { nullableStringParam, stringParam } from "../params.js";
import { installedFangExtensionFiles, fangExtensionFingerprintsParam } from "../fang-extensions.js";
import { SYSTEM_APPEND } from "../prompt.js";
import { openSettingsIfValid, readGlobalSettings, userPackages, type UserPackages } from "../settings.js";
import { sessionEnv } from "../session.js";
import { configBusy, isConfigLocked } from "../config-lock.js";

const STDERR_TAIL = 16 * 1024;
const TERM_AFTER_MS = 5000;
const KILL_AFTER_MS = 5000;
/**
 * Pi output the host holds while Fang Harness is not reading, for example while the phone has frozen Fang Harness with the screen
 * off. Below this Pi never waits on Fang Harness; above it Pi waits until Fang Harness reads again, so the host's memory stays
 * bounded.
 */
const OUTPUT_BACKLOG_LIMIT = 32 * 1024 * 1024;
const WEB_ACCESS_EXTENSION = "fang-web-access";
/**
 * The packages whose tools a Fang Harness extension also registers, by extension name. Pi exits when two extensions
 * register one tool, so a user who installed the package gets Pi's extension and Fang Harness leaves its copy out.
 */
const REPLACED_BY_PACKAGES: Record<string, { npm?: string; git?: string }> = {
  [WEB_ACCESS_EXTENSION]: { npm: "pi-web-access" },
  "fang-ask-user": { npm: "pi-ask-user", git: "github.com/edlsh/pi-ask-user" },
};

/** Keeps only the newest bytes of Pi's stderr for the exit report. */
class TailBuffer {
  private bytes: Buffer = Buffer.alloc(0);

  push(chunk: Buffer): void {
    const joined = Buffer.concat([this.bytes, chunk]);
    this.bytes = joined.length > STDERR_TAIL ? Buffer.from(joined.subarray(joined.length - STDERR_TAIL)) : joined;
  }

  text(): string {
    return this.bytes.toString("utf8");
  }
}

/**
 * One timestamped line on the host's stderr. The app logs it once the host exits, so a
 * dropped connection can be traced to its side.
 */
function note(message: string): void {
  process.stderr.write(`fang-host rpc ${new Date().toISOString()}: ${message}\n`);
}

/** Notes a channel line from [from] that the host dropped, which only a bug in Fang Harness sends. */
function droppedChannelLine(from: string): (reason: string) => void {
  return (reason) => note(`dropped a channel line from ${from}: ${reason}`);
}

/** Whether the user's global settings install the package that [extension] duplicates, in npm or from git. */
function replacedByUserPackage(extension: string, installed: UserPackages): boolean {
  const packages = REPLACED_BY_PACKAGES[extension];
  if (packages === undefined) return false;
  const fromNpm = packages.npm !== undefined && installed.npm.has(packages.npm);
  return fromNpm || (packages.git !== undefined && installed.git.has(packages.git));
}

/**
 * Copies Pi's stdout through [merger] to the socket like `pipe`, but lets up to `OUTPUT_BACKLOG_LIMIT` bytes queue in
 * the socket instead of pipe's 16 KiB, so Pi keeps working while Fang Harness is not reading. Never ends the socket: the exit
 * line follows Pi's output. Once the socket is gone the output is dropped, so a Pi waiting to write gets going again
 * and sees its stdin close instead of waiting for SIGTERM.
 */
function forwardOutput(output: Readable, socket: Socket, merger: OutputMerger): void {
  output.on("data", (chunk: Buffer) => {
    if (!socket.writable) return;
    merger.output(chunk);
    if (socket.writableLength >= OUTPUT_BACKLOG_LIMIT) output.pause();
  });
  socket.on("drain", () => output.resume());
  socket.on("close", () => output.resume());
}

/**
 * Sets fang-web-access's `toolActivation` to `eager` in its `web-search.json` unless the user chose one, so every
 * chat has the web tools from its first request instead of behind a `web_enable` call some models only make
 * sometimes. A file that is not a JSON object is left alone; the extension reports it itself.
 */
async function defaultWebToolsToEager(configDir: string): Promise<void> {
  const path = join(configDir, "web-search.json");
  let config: unknown;
  try {
    config = JSON.parse(await readFile(path, "utf8"));
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code !== "ENOENT") return;
    config = {};
  }
  if (!isObject(config) || "toolActivation" in config) return;
  await writeFile(path, `${JSON.stringify({ ...config, toolActivation: "eager" }, null, 2)}\n`);
}

/**
 * Sets Pi's `steeringMode` to `all` unless the user chose one, so every steer sent during a turn reaches the model
 * together, as Fang Harness's single steering box shows them. Writes through Pi's locked `SettingsManager`, since another
 * chat's Pi may be saving settings too; Pi itself reports settings it cannot read. A failed write only leaves Pi's
 * one-at-a-time default, which the steering box follows as well.
 */
async function defaultSteeringToAll(ctx: ModeContext, settings: unknown): Promise<void> {
  if (isObject(settings) && "steeringMode" in settings) return;
  const current = openSettingsIfValid(ctx, await loadPi(ctx.env.root));
  if (current === undefined || current.manager.getGlobalSettings().steeringMode !== undefined) return;
  current.manager.setSteeringMode("all");
  await current.manager.flush();
}

/** Error results for rpc; success has no result line, only `fang_exit`. */
function fail(ctx: ModeContext, refusal: HostError): void {
  ctx.conn.send({ type: "fang_result", ok: false, error: refusal.message, code: refusal.code, i18n: refusal.i18n });
  ctx.conn.finish();
}

/**
 * Bridges the socket to `pi --mode rpc` byte-for-byte. Pi exits on stdin EOF, so closing the
 * socket stops the agent; signals only cover a Pi that ignores EOF.
 */
export async function rpc(ctx: ModeContext): Promise<void> {
  const cwd = stringParam(ctx.params, "cwd");
  const sessionPath = nullableStringParam(ctx.params, "sessionPath");
  const expectedVersion = stringParam(ctx.params, "expectedVersion");
  const packageJson = stringParam(ctx.params, "packageJson");
  const fangExtensions = fangExtensionFingerprintsParam(ctx.params);
  const installed = await installedPiVersion(ctx.env.root);
  if (installed !== expectedVersion) {
    const refusal =
      installed === null
        ? new HostError("pi_version", `The agent is not installed; Fang Harness needs ${expectedVersion}`, {
            key: "agent_not_installed_needs",
            args: { expected: expectedVersion },
          })
        : new HostError("pi_version", `Agent ${installed} is installed; Fang Harness needs ${expectedVersion}`, {
            key: "agent_version_mismatch",
            args: { installed, expected: expectedVersion },
          });
    fail(ctx, refusal);
    return;
  }
  // The runtime's packages and Fang Harness's extensions can change while Pi's version stays put; ensure installs both.
  const installedPackageJson = await readFile(join(ctx.env.root, "pi", "package.json"), "utf8").catch(() => null);
  if (installedPackageJson !== packageJson) {
    fail(ctx, new HostError("pi_version", "The agent's packages are out of date", { key: "agent_packages_outdated" }));
    return;
  }
  const extensionFiles = await installedFangExtensionFiles(ctx.env.root, fangExtensions);
  if (extensionFiles === null) {
    fail(ctx, new HostError("pi_version", "The agent's bundled extensions are out of date", { key: "agent_extensions_outdated" }));
    return;
  }
  const isDirectory = await stat(cwd).then(
    (stats) => stats.isDirectory(),
    () => false,
  );
  if (!isDirectory) {
    fail(ctx, folderNotFound("cwd_missing", cwd));
    return;
  }
  // A restore swaps the configuration while it holds the lock, and an extension update rewrites the packages Pi loads;
  // Pi must start on one side of the change or the other.
  if (await isConfigLocked(ctx.env)) {
    fail(ctx, configBusy());
    return;
  }

  const args = [await piEntry(ctx.env.root), "--mode", "rpc"];
  if (sessionPath !== null) args.push("--session", sessionPath);
  if (fangExtensions.some(({ name }) => name === WEB_ACCESS_EXTENSION)) await defaultWebToolsToEager(ctx.env.configDir);
  const settings = await readGlobalSettings(ctx.env.configDir);
  await defaultSteeringToAll(ctx, settings);
  const userInstalled = userPackages(settings);
  for (const { name, path } of extensionFiles) {
    if (!replacedByUserPackage(name, userInstalled)) args.push("--extension", path);
  }
  args.push("--append-system-prompt", SYSTEM_APPEND);
  const env = await sessionEnv(ctx.env);
  // The channel socket goes last, at CHANNEL_FD; Pi hands none of its fds past stderr to the commands it runs.
  const child = spawn(process.execPath, args, { cwd, env, stdio: ["pipe", "pipe", "pipe", "pipe"] });
  const channel = child.stdio[CHANNEL_FD] as Socket;
  const { socket } = ctx.conn;
  const stderr = new TailBuffer();
  child.stderr.on("data", (chunk: Buffer) => {
    stderr.push(chunk);
  });
  child.stdin.on("error", () => undefined);
  channel.on("error", (error) => note(`channel error: ${error.message}`));
  const merger = new OutputMerger((data) => {
    if (socket.writable) socket.write(data);
  });
  const fromPi = new ChannelReader((message) => merger.channel(message), droppedChannelLine("pi"));
  channel.on("data", (chunk: Buffer) => fromPi.push(chunk));
  channel.write(encodeChannelMessage({ topic: HELLO_TOPIC, data: null }));

  let reported = false;
  const report = (code: number | null, signal: string | null): void => {
    if (reported) return;
    reported = true;
    note(`pi exited: code=${code} signal=${signal} socketWritable=${socket.writable}`);
    merger.end();
    ctx.conn.send({ type: "fang_exit", code, signal, stderr: stderr.text() });
    ctx.conn.finish();
  };
  // `close` fires only after stdout and the channel ended, so the exit line follows everything Pi sent.
  child.on("close", report);
  child.on("error", (error) => {
    stderr.push(Buffer.from(`\n${error.message}\n`));
    report(null, null);
  });

  const toPi = new AppInputRouter(
    (data) => {
      if (!child.stdin.write(data)) socket.pause();
    },
    (message) => {
      if (!channel.destroyed) channel.write(encodeChannelMessage(message));
    },
    droppedChannelLine("the app"),
  );
  child.stdin.on("drain", () => socket.resume());
  const { leftover } = ctx.conn;
  if (leftover.length > 0) toPi.push(leftover);
  socket.on("data", (chunk: Buffer) => toPi.push(chunk));
  socket.on("end", () => {
    toPi.end();
    child.stdin.end();
  });
  socket.resume();
  forwardOutput(child.stdout, socket, merger);
  socket.on("end", () => {
    note("Fang Harness ended the connection");
  });
  socket.on("error", (error) => {
    note(`socket error: ${error.message}`);
  });

  const stop = (): void => {
    note("stopping pi");
    if (!child.stdin.writableEnded) child.stdin.end();
    const running = (): boolean => child.exitCode === null && child.signalCode === null;
    setTimeout(() => {
      if (!running()) return;
      note("pi ignored stdin EOF; sending SIGTERM");
      child.kill("SIGTERM");
      setTimeout(() => {
        if (!running()) return;
        note("pi ignored SIGTERM; sending SIGKILL");
        child.kill("SIGKILL");
      }, KILL_AFTER_MS).unref();
    }, TERM_AFTER_MS).unref();
  };
  if (ctx.signal.aborted) stop();
  else ctx.signal.addEventListener("abort", stop, { once: true });
}
