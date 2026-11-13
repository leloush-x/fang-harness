package io.leloush.fang.ui.theme

import androidx.compose.ui.text.font.FontFamily

/** Font helpers for Fang UI, compatible with ported subagent components. */
object FangFonts {
  /** Ligatures and contextual alternates off, tabular figures on for ticking counters. */
  const val TABULAR = "liga 0, calt 0, tnum"
  const val FEATURES = "liga 0, calt 0"

  val mono: FontFamily = FontFamily.Monospace
  val ui: FontFamily = FontFamily.Default
}
