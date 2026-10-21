package io.leloush.fang.host

/**
 * The release's names in Termux, apart from Fang Harness Dev's (src/debug) so both builds can share one Termux: Pi's config
 * and the helper under `~/.fang`, the host under `~/.local/share/fang`, `fang-start`.
 */
internal object TermuxBuild {
  const val NAME = "fang"

  /** The first of the five loopback ports the Play-Termux helper may listen on. */
  const val HELPER_FIRST_PORT = 47310
}
