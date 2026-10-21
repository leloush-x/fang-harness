---
title: Configure the agent
description: Where Fang Harness keeps Pi's configuration, how to edit it, how to give the agent standing instructions, and how backups work.
---

Fang Harness runs [Pi](https://pi.dev), so the agent is configured the way Pi is: with files in a config folder. Fang Harness gives Pi its own folder, separate from any copy of Pi you run yourself.

## Where the configuration lives

Fang Harness's config folder is `files/wolfi/pi` in the app's private storage. Inside Wolfi it's `/data/data/<package>/files/wolfi/pi` (`io.leloush.fang` for the release, `io.leloush.fang.debug` for Fang Harness Dev), and **Settings → Agent → Config folder** has a button that copies the exact path.

A plain Pi install uses `~/.pi/agent` instead. Fang Harness doesn't read that folder, so settings, sign-ins, and extensions you set up for your own `pi` don't carry over, and the reverse is true too.

| File or folder | What it holds |
| --- | --- |
| `settings.json` | Pi's settings, including the default model and installed packages |
| `auth.json` | Subscription sign-ins and API keys |
| `models.json` | OpenAI-compatible providers and their models (see [Connect a model](/docs/providers/)) |
| `AGENTS.md` | Instructions the agent follows in every chat |
| `APPEND_SYSTEM.md` | Text added to the end of the system prompt |
| `SYSTEM.md` | A replacement for Pi's system prompt |
| `extensions/`, `skills/`, `prompts/`, `themes/` | Your own extensions, skills, prompt templates, and themes |
| `web-search.json` | Settings for the bundled web search extension |
| `sessions/` | Chat history |

Pi's [configuration documentation](https://github.com/earendil-works/pi/blob/main/packages/coding-agent/docs/configuration.md) describes every file and setting. Wherever it says `~/.pi/agent`, use `files/wolfi/pi`, or the full path Settings copies.

## Edit the files

Fang Harness's file viewer is read-only, so there are two ways to change these files:

- **Ask the agent.** In any chat, say what you want, for example: "Add a rule to Pi's `AGENTS.md` that tests run with `pytest -q`."
- **Use a terminal.** Open one with **Chat actions → New terminal** and edit with a terminal editor, such as `nano` (`apk add nano`).

To browse the folder, add the config folder (the path Settings copies) as a workspace, or open the file explorer and choose **Show hidden files**.

After you change a file, reload so the agent picks it up:

- **Chat actions → Reload agent**, or type `/reload`, reloads the current chat. If the agent is working, Fang Harness asks before stopping it.
- **Settings → Agent → Reload all agents** reloads every chat that isn't working.

A reload restarts the agent in the same folder and keeps the conversation.

## Give the agent standing instructions

Put instructions you want in every chat in Pi's `AGENTS.md` in that config folder: your preferred tools, coding style, or notes about your phone. For a single project, put an `AGENTS.md` in the project's folder instead. Pi reads both, plus any `AGENTS.md` in the folders above the project.

Fang Harness already tells the agent it's running on Android in Wolfi OS, so you don't need to explain that. To add to the system prompt instead of the conversation's instructions, use `APPEND_SYSTEM.md`.

## Project settings

A project can carry its own Pi settings, extensions, skills, and prompts in a `.pi` folder. Because those can run code, Pi loads them only from folders you trust. Fang Harness runs Pi without its interactive trust prompt, so record the decision in `trust.json` in the config folder, using the folder's full path:

```json
{
  "/sdcard/projects/my-app": true
}
```

Trusting a folder also trusts the folders inside it. `AGENTS.md` files don't need trust; they always load.

## Back up and restore

Open **Settings → Agent → Backups** and tap **Back up now**. A backup holds Pi's configuration: settings, sign-ins, models, extensions, skills, prompts, and themes. Chats aren't part of it.

Backups are saved in Fang Harness's app-private storage at `files/wolfi/backups`. Because they live there, uninstalling Fang Harness or clearing its data deletes them too.

To restore, tap **Restore** next to a backup. The backup replaces the current configuration, and anything changed since is lost. Your chats stay as they are. Fang Harness stops running chats first, so a reply in progress ends. If you started `pi` yourself in a terminal with Fang Harness's config folder, quit it before restoring.
