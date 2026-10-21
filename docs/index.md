---
title: Fang Harness guide
description: What Fang Harness is, how it runs on your phone, and where to start.
---

Fang Harness is an AI coding agent that runs on your Android phone. Tell it what to build or fix, and it reads your code, runs commands, edits files, and checks its own work. There's no laptop or cloud server doing the work somewhere else: the agent, your files, and every command stay on the phone.

## How it fits together

Fang Harness has three parts. You install the app and Termux, and setup installs Pi for you:

- **The Fang Harness app** is what you see and tap: chats, files, code changes, terminals, and settings.
- **[Termux](https://termux.dev)** is a Linux-like environment for Android. The agent works there, so it can install and run real tools with `pkg`, the same way it would on a computer.
- **[Pi](https://pi.dev)** is the open-source agent that does the work. Fang Harness installs it in Termux, starts one Pi process for each chat, and shows what it does in the app.

The thinking happens at the AI model you connect, such as Claude or OpenAI, so you need a network connection while a chat is working.

## Where to go next

- **[Get started](/docs/get-started/)**: install Fang Harness and Termux, walk through setup, and start your first chat.
- **[Connect a model](/docs/providers/)**: sign in with a Claude or OpenAI subscription, add a Gemini, Z.ai or Kimi API key, or add any OpenAI-compatible API.
- **[Configure the agent](/docs/configure/)**: find Pi's configuration in Termux and change how the agent behaves.
- **[Extensions](/docs/extensions/)**: add Pi extensions, skills, and prompt templates, and see which ones Fang Harness bundles.
- **[Run in the background](/docs/background/)**: how Fang Harness keeps chats working while you're away, and what Android can still stop.

Fang Harness is in early development, so screens and settings may change between versions.
