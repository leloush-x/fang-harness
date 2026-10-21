package io.leloush.fang.host

/** Wire constants shared with `host/src` (see `host.mjs` modes). */
internal object HostProtocol {
  const val VERSION = 1

  /** Images arrive base64-encoded inside `prompt`, so lines can be large. */
  const val MAX_LINE_BYTES = 16 * 1024 * 1024

  /** The hello comes from an unauthenticated peer; bound it far below [MAX_LINE_BYTES]. */
  const val MAX_HELLO_BYTES = 64 * 1024

  const val HELLO = "fang_hello"
  const val REQUEST = "fang_request"
  const val RESULT = "fang_result"
  const val PROGRESS = "fang_progress"
  const val EXIT = "fang_exit"

  /**
   * A line on Fang Harness's channel between the app and the extensions it bundles, `{"type":…,"topic":…,"data":…}`. The
   * host routes the app's lines by their first bytes, so [type] must come first (see `host/src/channel.ts`).
   */
  const val CHANNEL = "fang_channel"
  const val AUTH_EVENT = "fang_auth_event"
  const val AUTH_PROMPT = "fang_auth_prompt"
  const val AUTH_RESPONSE = "fang_auth_response"
  const val AUTH_CANCEL = "fang_auth_cancel"
  const val AUTH_PROMPT_DISMISS = "fang_auth_prompt_dismiss"
}
