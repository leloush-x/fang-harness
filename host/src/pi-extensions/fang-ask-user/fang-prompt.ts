/**
 * Fang's own prompt text for the `ask_user` tool, in Fang's house style (see `host/src/pi-extensions/fang-todo/todo.ts`
 * for the convention: a trigger sentence, a rule, a concrete example, a warning). Vendored from pi-ask-user 0.17.2
 * (see `UPSTREAM.md`), but the description, snippet and guidelines are Fang's, because the upstream text is terse and
 * assumes the terminal UI, which Fang shows as a card.
 *
 * Keep this file free of Pi imports: it is a pure data module so it can be imported by both the bundled extension and
 * the host tests.
 */
export const ASK_USER_DESCRIPTION =
  "Ask the user a focused question with optional multiple-choice answers, to settle a decision or gather information " +
  "interactively. Ask one question per call by default, or 2-4 independent questions together through `questions`. " +
  "Gather context with tools (read, web, ...) first and pass a short summary via `context`.";

export const ASK_USER_SNIPPET =
  "Ask the user one focused question (or 2-4 independent ones together) with optional choices";

export const ASK_USER_GUIDELINES: string[] = [
  "Before calling ask_user, gather context with tools (read, web, ...) and pass a short summary of what you found via `context`.",
  "Use ask_user when the user's intent is ambiguous, when a decision needs explicit user input, or when several valid options exist. Do not use it for purely informational messages, for a single obvious path, or when you can answer the question yourself.",
  "Ask one focused question per call by default. A question's text must not contain numbered, multipart or unrelated sub-questions.",
  "Use `questions` (2-4 entries) only for independent decisions whose prerequisites are already settled; anything that depends on another answer goes in a later ask_user call. Put every actual question inside `questions`; a top-level `question` is only a heading for the batch and is never asked or answered.",
  "Offer 2-5 options with short titles (a few words) and a description for each where the difference matters. Leave `allowFreeform` on (the default) so the user can answer in their own words.",
  "ask_user is blocking: do not batch it with other tools in the same turn, and do not start work that depends on the answer until it comes back. The user can dismiss the question; treat that as \"no answer\" and either pick a sensible default or ask again.",
  "Set `allowMultiple` only when several choices genuinely apply; set `allowComment` when you want an optional note alongside the selection; set `timeout` when the question should auto-dismiss.",
];
