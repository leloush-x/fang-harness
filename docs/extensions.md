---
title: Extensions, skills, and prompts
description: Install Pi packages in Fang Harness, write your own extensions, add skills and prompt templates, and see how extensions appear in Fang Harness's interface.
---

Fang Harness supports Pi's extensions, skills, and prompt templates. They go in Fang Harness's config folder, `files/wolfi/pi`. See [Configure the agent](/docs/configure/) for editing files there.

## Bundled extensions

Fang Harness bundles extensions for ease of use, or where the experience it wants isn't possible through Pi's RPC protocol. The current bundled extensions are:

- **Fang Harness todo** keeps a to-do list for the agent's current task, shown as a tab on the message box. The list is saved with the chat, and `/todos` shows it. It showcases Fang Harness's [widgets](#how-extensions-look-in-fang). Its tool matches [rpiv-todo](https://github.com/juicesharp/rpiv-mono)'s.
- **Fang Harness subagents** gives the agent a `subagent` tool: it hands a self-contained task to another agent with a fresh context, and several calls in one reply run in parallel. The chat follows each run, and you can open, steer or stop it. The bundled **Explore** agent only reads and searches; **General** has the same tools as the chat. Add your own agents as Markdown files in `files/wolfi/pi/agents/`, with frontmatter for `name`, `description`, `tools`, `model` and `thinking`.
- **Fang Harness questions** gives the agent [pi-ask-user](https://github.com/edlsh/pi-ask-user)'s `ask_user` tool, so it can ask you a question with choices when it needs a decision. The chat shows the question as a [card](#how-extensions-look-in-fang) with its answer choices, an optional comment, or a text field for your own answer.
- **Fang Harness background** (experimental) lets the agent run commands in the background, such as servers and long builds, and picks the chat back up when each one exits. Fang Harness keeps the chat's agent and the phone awake while they run. See [Background tasks](/docs/background/#background-tasks).
- **Fang Harness web access** lets the agent search the web and read pages, docs, and PDFs. `/websearch` opens its search curator. It's a copy of [pi-web-access](https://github.com/nicobailon/pi-web-access), changed to start faster on Android. Its settings, such as search API keys, go in `files/wolfi/pi/web-search.json`; the [pi-web-access README](https://github.com/nicobailon/pi-web-access#readme) lists them.

Turn them off in **Settings → Fang Harness extensions**. Idle chats reload to apply the change; working chats apply it when their agent next starts.

If you install `npm:pi-web-access` or `pi-ask-user` yourself, or the pi-ask-user repository from git, Fang Harness drops its bundled copy so the two don't register the same tools.

## Install a Pi package

Pi packages bundle extensions, skills, prompts, and themes from npm or git. Install one in either of two ways.

**List it in `settings.json`** under `packages`:

```json
{
  "packages": ["npm:@example/pi-tools"]
}
```

Then reload the chat with **Chat actions → Reload agent** or `/reload`. Pi installs missing packages when the agent starts.

**Run Pi's install command** in a terminal tab. Fang Harness's Pi isn't on the terminal's `PATH`, so give its path and config folder:

```sh
APP=/data/data/io.leloush.fang/files/wolfi
PI_CODING_AGENT_DIR="$APP/pi" \
  node "$APP/runtime/pi/node_modules/@earendil-works/pi-coding-agent/dist/bundle/cli.js" \
  install npm:@example/pi-tools
```

For Fang Harness Dev, use `/data/data/io.leloush.fang.debug/files/wolfi`. Use `remove` to uninstall or `list` to list packages, then reload the chat. Without `PI_CODING_AGENT_DIR`, Pi installs into `~/.pi/agent`, which Fang Harness ignores.

Pi's [packages documentation](https://github.com/earendil-works/pi/blob/main/packages/coding-agent/docs/packages.md) covers git sources, version pinning, and partial loading. Packages run code with the agent's permissions; review them before installing.

### Update installed packages

**Settings → Agent → Update Pi extensions** runs Pi's `pi update --extensions` with Fang Harness's config folder. After you confirm, Pi updates every package listed in `files/wolfi/pi/settings.json` to its latest version, except npm packages pinned to a version, such as `npm:@example/pi-tools@1.2.0`. The row shows Pi's progress, and a message says when the update finished or why it failed. Pi itself stays at the version Fang Harness ships, and Fang Harness's bundled extensions update with the app.

Chats can't start while the update runs. Afterwards, chats that aren't working reload with the new versions; a working chat picks them up the next time its agent starts.

## Write your own extension

Put a TypeScript file in `files/wolfi/pi/extensions/`, such as `hello.ts`, and reload the chat. Pi's [extensions documentation](https://github.com/earendil-works/pi/blob/main/packages/coding-agent/docs/extensions.md) covers the API. Extension commands appear when you type `/`.

## How extensions look in Fang Harness

Fang Harness talks to Pi over Pi's RPC mode and draws extension UI with native controls:

| An extension calls | Fang Harness shows |
| --- | --- |
| `select`, `confirm`, `input`, `editor` | A card above the message box with choices, Yes and No, or a text field |
| `notify` | A notice in the chat, styled as info, a warning, or an error |
| `setStatus` | A status line under the chat's title |
| `setWidget` with lines of text | A tab on the message box that opens the widget |
| `setTitle` | The chat's title |
| `sendMessage` | A message in the chat |

If a `select`, `confirm`, `input`, or `editor` card appears in a chat that isn't on screen, Fang Harness sends an **Agent needs your answer** notification with the chat's name and the question. Tap it to open the chat. It clears once the chat is on screen or the question is answered or expires.

RPC mode can't show custom components, headers, footers, editors, or component widgets; the rest of the extension works. See Pi's [RPC extension UI notes](https://github.com/earendil-works/pi/blob/main/packages/coding-agent/docs/rpc-extension-ui.md).

## Skills

A skill is a folder with a `SKILL.md` that teaches the agent a task. Put skills in `files/wolfi/pi/skills/`. Skills in a project's `.agents/skills/` or `.pi/skills/` load only from [trusted projects](/docs/configure/#project-settings). The agent uses skills on its own; type `/skill:` and a name to run one.

## Prompt templates

A prompt template is a Markdown file run as a command. Save `files/wolfi/pi/prompts/review.md`:

```markdown
---
description: Review the uncommitted changes
---
Review the uncommitted changes in this repository. Point out bugs first, then style.
```

Reload the chat, then type `/review`. Pi's [prompt template documentation](https://github.com/earendil-works/pi/blob/main/packages/coding-agent/docs/prompt-templates.md) covers arguments such as `$1` and `$@`.

Reload the chat after adding or changing skills or prompts.
