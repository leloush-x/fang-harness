package io.leloush.fang.data

import androidx.annotation.StringRes
import io.leloush.fang.R

/**
 * The Pi extensions Fang Harness bundles, which Settings can turn off. Each one's source is the APK asset [asset]
 * (`host/build.ts` bundles it), which the host's `ensure` installs under [id]. [usesRuntimePackages] marks one that
 * imports packages the bundled `pi/package.json` installs, which `ensure` installs inside that runtime so Node finds them.
 */
internal enum class BundledExtension(
  val id: String,
  @StringRes val title: Int,
  @StringRes val summary: Int,
  val asset: String,
  val usesRuntimePackages: Boolean = false,
) {
  Todo(
    "fang-todo",
    R.string.settings_extension_todo,
    R.string.settings_extension_todo_summary,
    asset = "fang-extensions/fang-todo.js",
  ),

  /** pi-ask-user's `ask_user` tool: the agent asks a question with choices and the chat shows it as a card. */
  AskUser(
    "fang-ask-user",
    R.string.settings_extension_ask_user,
    R.string.settings_extension_ask_user_summary,
    asset = "fang-extensions/fang-ask-user.js",
  ),

  /** Runs shell commands past the agent's turn; it tells the app over Fang Harness's channel while they run. */
  Background(
    "fang-background",
    R.string.settings_extension_background,
    R.string.settings_extension_background_summary,
    asset = "fang-extensions/fang-background.js",
  ),

  /** pi-web-access, vendored in `host/src/pi-extensions/fang-web-access`. */
  WebAccess(
    "fang-web-access",
    R.string.settings_extension_web_access,
    R.string.settings_extension_web_access_summary,
    asset = "fang-extensions/fang-web-access.js",
    usesRuntimePackages = true,
  ),
}
