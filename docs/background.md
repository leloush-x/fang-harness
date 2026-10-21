---
title: Run in the background
description: How Fang Harness keeps chats and terminal commands running when you leave the app or lock the screen, and what Android can still stop.
---

The agent runs in Wolfi OS, the Linux runtime inside Fang Harness. Android pauses and stops apps you aren't looking at to save battery and memory, so Fang Harness takes a few steps to keep work going while you're away, and lets go of them as soon as nothing is running.

## While something is working

Fang Harness counts as working while a chat is running, while the agent waits on a [background task](#background-tasks), while a command runs in a Fang Harness terminal, and during sign-in, installing the agent, Wolfi OS setup, backups, and restores. For as long as any of these lasts, Fang Harness:

- **Runs as a foreground service.** Android shows Fang Harness's notification: **Fang Harness agent is working**, **A background task is running** when only background tasks are, or **A terminal command is running** when only a terminal is busy. Android stops a foreground service far less readily than an app in the background.
- **Holds a wake lock.** This keeps the phone's processor running while the screen is off, so the agent keeps working. The screen still turns off and locks as usual.

The notification's **Stop** button stops every working chat and every background task. The terminal notice has no Stop button, since stopping chats wouldn't stop a terminal's command. To stop the command, end it in the terminal or close the terminal.

A command that keeps running in a Fang Harness terminal, such as a development server, counts as work for as long as it runs. That keeps the notification and the wake lock up the whole time, so close the terminal when you're done with it.

## Background tasks

The agent can run a command in the background, such as a development server, a long build, or a test suite, and stop working while it runs. When the command exits, its result comes back to the chat and the agent picks up where it left off, even if you've left the app. Background tasks are experimental.

- **The tasks show on the message box** as a **Background** tab, with each task's state and command.
- **A chat with background tasks shows as working.** The simmering pot stays on its tab and at the end of the chat, with how many tasks are running. You can still send messages, and **Stop** on the message box stops the tasks when the agent isn't replying.
- **A chat with background tasks keeps its agent** and counts toward the running chats limit.
- **You're told once the chat is done.** If you're away, the **Agent finished** notification comes when the agent's last reply is over and no task is left, not each time a reply ends while tasks still run.
- **Tasks end with their chat's agent.** Reloading or archiving the chat, switching it to another session, or Android stopping Fang Harness ends them; Fang Harness asks before a reload or archive does.
- **The chat learns what became of tasks its agent lost.** If the agent stops without passing on a task's result, for example because Android stopped Fang Harness, the next agent in the chat reports what Fang Harness recorded: the task's result if it finished, that Fang Harness stopped it if it was still running with nothing tracking it, or that how it ended is unknown, with its last output. The report arrives as a **fang-background-report** message, which the agent reads with your next message; it doesn't start the agent replying on its own.
- **Each task's output is kept** in a log file under `files/wolfi/background` in Fang Harness's private storage, until a day after the task last wrote to it. The result names the file, so you or the agent can read all of it.
- **At most four tasks run at once in each chat.** Android stops apps that start too many processes, so each task runs as a single command.

The agent is told to use background tasks for anything that should keep running. It can still start processes another way, such as with `&`, `nohup`, `tmux`, or `screen`, but Fang Harness can't see those or keep the phone awake for them, so Android may stop them. To turn background tasks off, turn off **Fang Harness background** in **Settings → Fang Harness extensions**.

### Why Fang Harness has its own

Pi's package list already has background-task extensions, such as [pi-background-tasks](https://github.com/Jawfish/pi-background-tasks). They work well in Pi on a computer, but not in Fang Harness:

- **Fang Harness can't see their tasks.** To save battery and memory, Fang Harness stops the agent of a chat that isn't working and lets the phone sleep once no chat is working. An agent waiting on another extension's task looks idle, so Fang Harness stops it, and its tasks with it, or the phone sleeps and Android stops Fang Harness. Fang Harness background comes with the app and tells it how many tasks each chat is waiting on, so the chat keeps its agent and the phone stays awake until they end.
- **Some run their tasks in `tmux`**, outside the agent, where Fang Harness can't track or stop them. A `tmux` server and its sessions also add processes toward Android's [32-process limit](#the-32-process-limit). Fang Harness background runs tasks without one and allows four at a time in each chat.
- **Some add several tools.** The model reads every tool's description with each request, so more tools cost more input tokens. Fang Harness background is one tool with one short guideline.

## When nothing is working

Once every chat is idle, no background task is running, and no terminal is running a command, Fang Harness removes its notification and releases the wake lock, so the phone can sleep normally. Fang Harness doesn't hold a wake lock while idle, so leaving it open costs no extra battery.

Idle chats also give back their memory. A chat that isn't working, waiting on background tasks, or on screen stops its agent after the time set in **Settings → Advanced → Stop idle chats after** (5 minutes by default). Its history stays, and the agent starts again when you open the chat.

The agent is part of Fang Harness: there is no separate runtime app to open or keep running, and opening a chat starts its agent again if it had stopped.

## Allow background activity

Battery optimization can still stop Fang Harness, or cut off the agent's network, after a while in the background. Setup asks you to exempt Fang Harness; you can do it later from **Settings → Permissions**:

- **Run in the background** exempts Fang Harness. Tap **Allow** when Android asks.

Phone makers often name the setting differently, and many add battery managers of their own that can stop apps even with the exemption. If chats or tasks still stop while you're away, see [Troubleshooting](#troubleshooting) for the settings on your phone.

Also turn on **Notifications** in **Settings → Permissions**. Without them, Fang Harness can't tell you when a chat finishes, stops, or [asks you something](/docs/extensions/#how-extensions-look-in-fang) while you're away.

## What Android can still stop

None of this makes Fang Harness impossible to stop. Android can still end it in these cases:

- **The phone runs low on memory.** When memory gets tight, Android stops apps to free it, and it can stop Fang Harness, with every chat and terminal in it, even while its notification is showing. Background exemptions and the foreground service make this less likely, but nothing an app can do rules it out. Closing other heavy apps and running fewer chats at once help.
- **Too many processes are running.** On Android 12 and later, Android stops child processes once more than 32 are running across all apps. See [The 32-process limit](#the-32-process-limit) below.
- **Your phone maker's battery manager steps in.** Samsung, Xiaomi, Huawei, OnePlus and other makers add their own rules for stopping apps in the background. See [Troubleshooting](#troubleshooting).
- **You force stop Fang Harness.** Force stopping it from Android's app info stops every chat and terminal in it, including the agent. Leave Fang Harness running while chats are working.

### The 32-process limit

Android 12 added a limit on child processes: programs an app starts itself, rather than the app's own process. Everything the agent runs is one, because Fang Harness starts it. Once more than 32 are running across all apps combined, Android stops the extra ones, oldest first, even with plenty of memory free. Android also stops child processes that use a lot of CPU while their app is in the background.

In Fang Harness, these count toward the limit:

- **Each running chat**: a few processes for the agent, plus every command and tool it runs while it works.
- **Each open terminal**: its shell, plus whatever runs in it.
- **Anything else running in Fang Harness**, such as commands you start in a Fang Harness terminal, servers left running in the background, and other apps' child processes.

When a chat's agent is stopped this way, the chat shows **Agent stopped** (see [When a chat stops](#when-a-chat-stops)).

#### Stay under the limit

These work on any Android version and change nothing outside Fang Harness:

- Lower **Settings → Advanced → Running chats**, which caps how many chats keep their agent running at once (6 by default, up to 16).
- Shorten **Settings → Advanced → Stop idle chats after**, so chats you've left stop their agent sooner. Their history stays.
- Close terminals you aren't using, and stop servers or watchers you started once you're done with them.

#### Turn the limit off

How to turn it off depends on the Android version. Each change below affects every app on the phone, not just Fang Harness.

- **Android 14 and later**: in **Settings → System → Developer options**, turn on **Disable child process restrictions**. This also stops Android killing child processes for heavy CPU use. To show Developer options, tap **Build number** seven times; it's usually in **Settings → About phone**. If you turn Developer options off again, the restriction comes back.
- **Android 12L and 13**: there's no switch, but one `adb` command turns off both the limit and the CPU check, and the change persists across restarts:

  ```sh
  adb shell settings put global settings_enable_monitor_phantom_procs false
  ```

  To undo it, run `adb shell settings delete global settings_enable_monitor_phantom_procs`. This command also works on Android 14 and later, and keeps working if you later turn Developer options off.
- **Android 12**: only the 32-process cap can be raised, through `device_config`. On phones with Google Play services this also means turning off syncing of Android's device configuration, which can stop Android recovering from bad settings. [This guide](https://github.com/agnostic-apollo/Android-Docs/blob/master/en/docs/apps/processes/phantom-cached-and-empty-processes.md#commands-for-android-12) explains the commands and the risks.

#### Run adb on the phone itself

You don't need a computer for the `adb` commands, but Fang Harness's built-in terminal, a Wolfi Bash shell, doesn't ship `adb`. Use a separate terminal app with an `adb` client, or a computer. On Android 11 and later, the phone can connect to its own wireless debugging:

1. Connect to Wi-Fi. In **Developer options**, turn on **Wireless debugging**.
2. In **Wireless debugging**, tap **Pair device with pairing code**. Use split screen or a floating window so the code stays visible. In the terminal with `adb`, run `adb pair localhost:<port>` with the port shown with the code, then enter the code.
3. Run `adb connect localhost:<port>` with the port on the main **Wireless debugging** screen, which differs from the pairing port.
4. Run the command for your Android version as shown above.

You can turn wireless debugging off again afterwards; the setting stays.

## When a chat stops

If the agent stops while a chat is working, the reply in progress ends there. Its history up to that point is saved.

- **The connection dropped.** The chat shows **Lost the connection to Wolfi OS** where it happened. Fang Harness reconnects on its own and reloads the chat's history. If it drops again within a minute, Fang Harness stops trying and shows **Agent stopped** instead.
- **The agent stopped.** The chat shows **Agent stopped** with a **Restart** button, which starts the agent again with the same chat. Then ask it to continue where it left off.

If you were away when it happened, Fang Harness sends a notification: **Agent finished**, **Agent failed**, or **Pi exited**, with the chat's name. Tap it to open the chat.

## Troubleshooting

If Fang Harness still stops while you're away, the cause is usually a battery setting. Setup only asks Android to exempt Fang Harness from battery optimization, and many phones add battery managers that ignore that exemption. Change the settings below for **Fang Harness**: first the ones for all phones, then the ones for your phone's brand. Menu names change between versions, so if a path doesn't match, search Settings for the setting's name.

### Find out what stopped Fang Harness

Android records why each app's process ended. With [adb](#run-adb-on-the-phone-itself), run:

```sh
adb shell dumpsys activity exit-info io.leloush.fang
```

For Fang Harness Dev, use `io.leloush.fang.debug` instead. Each entry has a `timestamp` and a `description`, newest first:

- **`o-kill` or `nirvana`**: ColorOS's battery manager, on OnePlus, Oppo and Realme phones. See [OnePlus, Oppo, and Realme](#oneplus-oppo-and-realme).
- **A reason that mentions low memory**: the phone ran out of memory. Close heavy apps and run fewer chats at once.

These records cover Fang Harness's processes. If a chat's agent stops while Fang Harness keeps running, the [32-process limit](#the-32-process-limit) is one possible cause; the agent may also have crashed.

### All phones

- **Allow background usage.** In **Settings → Apps → Fang Harness → App battery usage**, tap **Allow background usage** and choose **Unrestricted**. Setup sets this, but some phones switch Fang Harness back to **Optimized** later, so check it again if things start stopping.
- **Pause app activity if unused.** Turn this off on Fang Harness's info page. On Android 12 it's called **Remove permissions and free up space**. A paused app loses its permissions and can't run in the background.
- **Battery Saver.** Battery Saver limits background activity, so keep it off in **Settings → Battery → Battery Saver** while chats or tasks run.
- **Data Saver.** If Data Saver is on, turn on Fang Harness in **Settings → Network & internet → Data Saver → Unrestricted data**. Otherwise the agent loses its network in the background.
- **Child processes.** Turn off the [32-process limit](#turn-the-limit-off) if you run several chats, terminals, or servers at once.

### Samsung

- In **Settings → Battery → Background usage limits** (**Settings → Battery and device care → Battery → Background usage limits** on One UI 6):
  - Turn off **Put unused apps to sleep**.
  - Add Fang Harness to **Never sleeping apps**, and make sure it isn't in **Sleeping apps** or **Deep sleeping apps**. Samsung limits what sleeping apps can do in the background, even while their notification is showing.
- In Fang Harness's battery settings, choose **Unrestricted**.
- Turn off **Adaptive battery** and **Auto optimize** if your phone has them.
- In Recents, tap Fang Harness's icon on its card and choose **Keep open**.

### Google Pixel

The settings for [all phones](#all-phones) are all a Pixel needs. Pixels have been reported to switch Fang Harness from **Unrestricted** back to **Optimized**, so check it again if chats start stopping.

### Huawei and Honor

- In **Settings → Apps & services → App launch** (**Phone Manager → App launch** on older versions), turn off **Manage automatically** for Fang Harness, then turn on **Auto-launch**, **Secondary launch**, and **Run in background**. On Honor phones, search Settings for **App launch**.
- Search Settings for **Battery optimization**, show **All apps**, and set Fang Harness to **Don't allow**.
- In Recents, swipe Fang Harness's card down to lock it.
- Keep **Power saving mode** off while chats or tasks run.

Some EMUI versions stop background apps that aren't on Huawei's own list, whatever these settings say.

### Xiaomi, Redmi, and POCO

- In **Settings → Apps → Permissions → Background autostart**, turn on Fang Harness. On MIUI, it's **Autostart** on Fang Harness's page.
- On Fang Harness's page, set **Battery saver** (called **Power** on some HyperOS versions) to **No restrictions**.
- Keep **Battery saver** and **Ultra battery saver** off while chats or tasks run.
- In Recents, long-press Fang Harness's card and tap the padlock.

### OnePlus, Oppo, and Realme

These phones run ColorOS, OxygenOS, or realme UI.

- In **Settings → Apps → App management → Fang Harness → Battery usage**, turn on **Allow background activity**, and **Allow foreground activity** if it's there. On OxygenOS 14, it's in **Settings → Battery → More settings → App battery management**. On realme UI, it's in **Settings → Battery → Power saving settings → App battery management**, where you also turn on **Allow auto launch** and turn off **Optimize battery use**.
- In **Settings → Apps → Auto launch**, turn on Fang Harness.
- In **Settings → Battery → More settings**, turn off **Sleep standby optimization**.
- In Recents, open Fang Harness's card menu and choose **Lock**. OnePlus phones have been reported to reset the battery settings of apps that aren't locked.
- Keep the phone updated. Some Android 15 builds from late 2024 stopped Fang Harness's commands, until updates from November 2024 fixed it.

Even with all of these, ColorOS can stop Fang Harness while its notification is showing, often a few seconds after the screen turns on. It shows as `nirvana` or `o-kill` in [Fang Harness's exit records](#find-out-what-stopped-fang-harness). No setting is known to turn it off.

### Vivo

- In **Settings → Apps → Autostart**, turn on Fang Harness.
- In **Settings → Battery → Background power consumption management**, allow Fang Harness.
- Lock Fang Harness in Recents.

### Motorola

- Set Fang Harness to **Unrestricted**, as for [all phones](#all-phones).
- Turn off **Adaptive battery** in **Settings → Battery**.
