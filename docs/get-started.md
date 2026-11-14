---
title: Get started
description: Install Fang Harness, walk through automatic setup, and start your first chat.
---

Setup takes a few minutes, and Fang Harness does most of it for you. It checks off each step as it finishes and explains every permission it asks for.

## What you need

- An Android phone or tablet running **Android 10 or newer**, with a 64-bit arm64 or x86_64 processor (almost every phone from the last few years).
- **An AI model to talk to**: a Claude Pro or Max subscription, an OpenAI Codex subscription, a Gemini, Z.ai Coding Plan or Kimi Code API key, or an API key for any OpenAI-compatible service. See [Connect a model](/docs/providers/).
- **A network connection** for the first setup, which downloads Wolfi OS (about 46–49 MB), and while a chat is working.
- **No separate terminal app or root.** Fang Harness downloads and sets up Wolfi OS itself.

## Install Fang Harness

[Download the latest APK](https://github.com/leloush-x/fang-harness/releases/latest) from the releases page on your phone and open it to install. Each release attaches exactly one file, `fang-v<VERSION>.apk`, and its notes give that file's size and SHA-256. Android asks you to allow installs from your browser or file manager the first time. Later releases install over it as updates and keep your chats and settings.

### If Android says "the package appears to be invalid"

That is Android's one line for every install it refuses, so it does not say what is wrong. In order:

1. **Compare the file's size with the release notes.** A download that lost its connection is short by a few bytes or a few megabytes, and the installer rejects it: download it again, on Wi-Fi if you can.
2. **Make sure it is the APK.** A browser or file manager that cannot open a `.apk` sometimes saves it as `fang-v<VERSION>.apk.txt`, `fang-v<VERSION>.apk.zip`, or `.bin`. Rename it back to end in `.apk`, or copy the file from a computer.
3. **Uninstall an older copy signed with another key.** An app cannot be updated over one signed with a different key, and the installer reports that refusal with the same sentence. Export a backup from **Settings → Backups** if the old app still opens, then uninstall **Fang Harness** and install again. Your chats live in the app's storage and go with it, so back them up first as well.
4. **Check the phone.** Fang Harness needs Android 10 or newer and a 64-bit processor, and does not install on a 32-bit-only device.
5. **Ask `adb` for the real error.** With the phone connected over USB:

   ```sh
   adb install -r fang-v<VERSION>.apk
   ```

   The `INSTALL_FAILED_*` code it prints names the cause; the message on the phone does not.

Every release is signed with the same key, carries the v1, v2 and v3 signature schemes, and is aligned for 16 KB memory pages. If a phone still refuses one, include the `adb` output when you report it.

### Build from source

With the Android SDK set up and your phone connected over `adb`, run:

```sh
git clone https://github.com/leloush-x/fang-harness.git
cd fang
./gradlew :app:installDebug
```

The repository's README lists the full toolchain.

A build from source is named **Fang Harness Dev** and installs beside a release. Each build has its own app ID and its own app-private Wolfi runtime, so setup, sign-ins, chats, and settings stay separate.

## Walk through setup

Open Fang Harness and tap **Get started**. Each step shows **Done** once it's complete, and you can go back to any of them later from **Settings → Permissions**.

1. **Set up Wolfi OS.** Tap **Set up Wolfi OS**. Fang Harness downloads Wolfi OS, unpacks it into app-private storage, and installs Bash, Git, Node.js, and the other tools it needs. It takes a few minutes, and you can leave the screen while it runs.
2. **Notifications** (optional but recommended). Fang Harness lets you know when a chat finishes or needs your answer while you're away.
3. **Run in the background** (optional but recommended). Keeps a chat going while the screen is off or you're in another app.
4. **Use phone storage** (optional but recommended). Lets the agent work on files outside its own storage, such as in Downloads or Documents.
5. **Install the agent.** Fang Harness installs Pi and its bundled extensions in Wolfi OS automatically once the runtime is ready. It takes a few minutes, and you can leave the screen while it runs.
6. **Connect a model** (optional here). Sign in with **Claude** or **OpenAI**, add a **Gemini**, **Z.ai** or **Kimi** key, or add an **OpenAI-compatible API**. You can also do this later in **Settings → Providers**.

When you see **You're all set**, tap **Start using Fang Harness**.

## Start your first chat

Fang Harness organizes chats by **workspace**: a folder the agent works in, usually a project.

1. Open the sidebar and tap **Add workspace**.
2. Pick a folder under **Home** or **Phone storage**, or type a path in the search box, then tap **Add as workspace**. To start fresh, create a folder for your project first.
3. Tap **New chat** in the workspace, type what you want, and tap **Send**.

A few things to know while it works:

- **Pick a model** with the model chip under the message box, and how hard it should think with the **Reasoning effort** chip. Your choice applies to the chat and becomes the default for new chats.
- **Steer it.** Type a message while the agent is busy and it waits in the **Steering** box until the agent is ready for it. Tap **Send now** to interrupt, or **Edit** to change it.
- **Stop it** at any time with **Stop**.
- **Attach images** with the **+** button when the model can see them.
- **Type `/`** to see commands, such as `/model`, `/compact` to shrink a long chat, or `/fork` to branch from an earlier message.
- **Fix a message you sent.** Tap the **⋯** under one of your own messages to edit it and send it again, resend it as it was, or delete it together with everything after it. Editing and deleting start a new branch of the chat, so the earlier part of the conversation stays as it was.

## Check the agent's work

- **Files**: open the file explorer from a chat to browse and read files, with syntax highlighting for more than twenty languages. The viewer is read-only; to change a file, ask the agent or use a terminal.
- **Changes**: in a git repository, see exactly what the agent changed, file by file. Fang Harness asks you to trust a repository before it runs git there, because a repository's own settings can run programs.
- **Terminal**: from **Chat actions → New terminal**, open a Wolfi Bash shell right next to your chat.
- **Chat actions** also lets you rename a chat, reload the agent after changing its configuration, copy the session's file path, or archive the chat.

On a foldable or tablet, open chats, files, changes, and terminals side by side with **Open in split screen**.

## If something goes wrong

**Wolfi OS setup stops partway.** Open setup again from **Settings → Permissions** and tap **Set up Wolfi OS** again; it continues where it left off. If it reports a download problem, check your internet connection and retry.

**Fang Harness can't use phone storage after setup.** In **Settings → Permissions → Use phone storage**, tap **Allow**.

**Chats stop when the phone is locked.** Allow background activity for Fang Harness in **Settings → Permissions**. Some phones name this setting differently, and the step shows where to find it. [Run in the background](/docs/background/) explains what else can stop a chat.

**Fang Harness says Wolfi OS isn't ready.** Open setup from **Settings → Permissions** and finish or retry the Wolfi OS step. The runtime is part of the app, so there's no other app to open.

**You reinstalled Fang Harness.** Reinstalling removes Wolfi OS, the agent, and its sign-ins from app-private storage. Fang Harness walks you through setup again. A [backup](/docs/configure/#back-up-and-restore) brings back your settings, sign-ins, and models.
