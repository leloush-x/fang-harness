package io.leloush.fang.host

/**
 * Fang Harness Dev's names in Termux, apart from the release's (src/release) so both builds can share one Termux: Pi's
 * config and the helper under `~/.fang-dev`, the host under `~/.local/share/fang-dev`, `fang-dev-start`.
 */
internal object TermuxBuild {
  const val NAME = "fang-dev"

  /** The first of the five loopback ports the Play-Termux helper may listen on. */
  const val HELPER_FIRST_PORT = 47320
}
