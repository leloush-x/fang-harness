---
title: Privacy policy
description: What data Fang Harness keeps on your phone, what leaves it, and where it goes.
---

_Last updated: November 16, 2026_

Fang Harness is an Android app that runs the [Pi](https://pi.dev) coding agent inside a bundled Wolfi OS runtime, entirely on your phone. It is open source: [the code](https://github.com/leloush-x/fang-harness) is the authority for everything on this page.

## In short

- Fang Harness has no accounts and no servers of its own. It does not send your chats, files, sign-ins, or settings to the developer.
- There is no analytics, crash reporting, advertising, or tracking in the app.
- Your messages and the files the agent reads go to the AI provider you connect, and to any other service you or the agent use. Those services' privacy policies cover what they do with that data.

## What stays on your phone

Fang Harness keeps its settings and the agent's state in its own app-private storage: preferences, chats and subagent transcripts, sign-ins and API keys, installed extensions, skills, prompts, and themes. No other Android app can read that storage.

If you grant **All files access**, Fang Harness can reach shared storage so the agent can work on files in Downloads, Documents and other folders. The app does not copy those files into its own storage. You can revoke the permission in Android's settings at any time.

Backups you create in **Settings → Backups** are written to `Download/fang-harness/backup` on shared storage when that access is granted, and to app-private storage otherwise. A backup contains Pi's configuration and sign-ins; chats are merged back on restore. Backups are never uploaded.

## What leaves your phone

Fang Harness sends data off the phone only to do what you ask:

- **Your AI model provider.** The agent sends your messages, attached images, and the parts of files and command output it reads to the provider you connected. Sign-in happens on the provider's own website. Fang Harness also asks a provider you add for its model list, using the key you gave it.
- **Web search and page fetching.** The bundled web access extension sends search queries to the search service you chose (Exa by default) and downloads pages, PDFs or videos you ask the agent to read. Depending on your extension settings, a page or link may also be sent to an extraction service such as Gemini or Datalab. You can turn the extension off in Settings.
- **Downloads during setup and updates.** Setup downloads Node.js and the Wolfi packages from their public mirrors, and Pi from the [npm registry](https://www.npmjs.com). Installing or updating extensions downloads them from where they are published.
- **Commands and extensions.** The agent runs commands inside Wolfi OS; those commands can reach the network when you ask them to, for example `git push`. Installed extensions run with the same access. What they send is up to the command or extension.

Like any network request, these services see your phone's IP address.

## Permissions

The app asks for:

- **Network access**, to reach the model provider, mirrors and services above.
- **Notifications**, to tell you when a chat finishes or needs your answer.
- **Foreground service and wake lock**, so a chat keeps working while the screen is off or you use other apps.
- **All files access** (optional), so the agent can work on files in shared storage.
- **Ignore battery optimisations** (optional), for long-running chats.

Attaching a photo uses Android's picker, which gives the app only the photos you choose. Fang Harness does not ask for contacts, location, camera, or microphone access.

## Deleting your data

Uninstalling Fang Harness deletes its app-private storage: settings, chats, sign-ins and any backups kept there. If you granted shared-storage access, also delete `Download/fang-harness/backup` to remove backups kept there. Data held by a model provider or another service is deleted through that service's own settings.

## Children

Fang Harness is a developer tool that is not directed at children under 13.

## Changes

If the app's handling of data changes, this page is updated with a new date at the top.

## Contact

Questions and bug reports belong in the repository's [issues](https://github.com/leloush-x/fang-harness/issues).
