<p align="center">
  <img src="site/public/favicon.svg" alt="Fang Harness logo" width="128" height="128">
</p>

<h1 align="center">Fang Harness</h1>

<p align="center">
  <a href="https://github.com/leloush-x/fang-harness/actions/workflows/release.yml"><img src="https://github.com/leloush-x/fang-harness/actions/workflows/release.yml/badge.svg" alt="Release"></a>
  <a href="https://github.com/leloush-x/fang-harness/releases/latest"><img src="https://img.shields.io/github/v/release/leloush-x/fang-harness" alt="Latest release"></a>
  <a href="LICENSE"><img src="https://img.shields.io/github/license/leloush-x/fang-harness" alt="License"></a>
</p>

**Fang Harness** is a native Android app for agentic coding. Tell it what to build or fix, and it reads your code, runs commands, edits files, and checks its own work — all on your phone. It runs on the [Pi](https://pi.dev) agent harness inside its own [Wolfi OS](https://wolfi.dev) runtime, so no laptop or cloud server does the work for you.

---

## Contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)
- [Development](#development)
- [License](#license)

## Features

- **Runs on your phone.** The agent, your code, git, and terminals all live in Wolfi OS on your device. Install compilers, runtimes, and databases with `apk add` — no root required.
- **Built on Pi.** Compatible with Pi's skills, tools, prompt templates, and extensions. Install extensions the usual Pi way.
- **Native Jetpack Compose UI.** No web view — smooth scrolling, typing, and streaming replies. Split panes on foldables and tablets.
- **Fix a message you sent.** Edit what you wrote and send it again, resend it as it was, or delete it and everything after it.
- **Real workbench.** Review changes in the built-in git diff viewer, browse files with tree-sitter syntax highlighting, look through commits, and open terminals inside the app.
- **Your model, your plan.** Connect Claude, OpenAI Codex, Gemini, Z.ai Coding Plan, Kimi Code, or any OpenAI-compatible API. Switch models per chat.
- **Runs in the background.** Lock your phone or switch apps; a notification tells you when a chat is done or needs your answer.
- **Guided setup.** Setup downloads and unpacks Wolfi OS, installs Pi, and walks you through permissions.

## Requirements

- Android 10 or newer on a 64-bit device (arm64 or x86_64).
- Network for first setup (downloads Wolfi OS, ~46–49 MB) and while chats are working.
- Setup is automatic — no separate terminal app or root needed.

## Installation

### Download a release

[Download the latest APK](https://github.com/leloush-x/fang-harness/releases/latest) and open it on your phone to install. Each release attaches one file, `fang-v<VERSION>.apk`, and its notes give the file's size and SHA-256. Android asks you to allow installs from your browser or file manager the first time.

Open the app and tap **Get started**: setup downloads and unpacks Wolfi OS, installs its tools, and installs the agent for you.

If Android says **"the package appears to be invalid"**, it refused the install without saying why. Compare the file's size with the release notes and download it again if it differs; a browser may have saved it as `.apk.txt` or `.zip`; and a copy signed with another key cannot be installed over an existing app, so uninstall **Fang Harness** (after **Settings → Backups**) first. [Get started](docs/get-started.md#if-android-says-the-package-appears-to-be-invalid) has the full list and the `adb install` command that prints the real error.

### Build from source

```sh
git clone https://github.com/leloush-x/fang-harness.git
cd fang-harness
./gradlew :app:installDebug
```

`installDebug` installs on the device or emulator that `adb` is connected to. To build an APK without installing, run `./gradlew :app:assembleDebug`.

Builds need JDK 17+, Android SDK (API 37, NDK `30.0.16248370`, CMake `4.1.2`), Node.js 22.19+, and pnpm. Set `ANDROID_HOME` or `sdk.dir` in `local.properties`.

## Usage

Open the app, walk through setup, and start your first chat. From the settings you can connect a model provider (Claude, OpenAI, Gemini, or any OpenAI-compatible API), configure the agent, manage extensions, and run backups.

## Development

Run every check:

```sh
./gradlew check
```

Build a debug APK:

```sh
./gradlew :app:assembleDebug
```

Host checks:

```sh
cd host && pnpm install --frozen-lockfile && pnpm run check
```

## License

Fang Harness is released under the [MIT License](LICENSE). Third-party code, fonts, and icons keep their own licenses — see **Settings → About → Open-source licenses** in the app.
