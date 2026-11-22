package io.leloush.fang.ui.components.activityglyph

import kotlin.math.PI

internal enum class ActivityAccent { ORANGE, RED, GREEN, BLUE }

/** Time-driven tweaks on top of the static stroke specs (springs are replaced by analytic motion). */
internal enum class Motion { NONE, CONDENSE, HELIX, SPIRAL, KICK }

internal enum class Pattern { NONE, SHIMMER, CONDENSE }

/** One band on the LED sphere. Angles are radians; phi is the latitude angle upstream's `s` rolls through. */
internal data class Stroke(
  val k: Double = 0.0,
  val w: Double = 0.2,
  val theta: Double = 0.0,
  val thetaSpin: Double = 0.0,
  val e: Double = 0.0,
  val a: Double = 0.0,
  val r: Double = 0.0,
  val phi: Double = 0.0,
  val phiSpin: Double = 0.0,
  val offset: Double = 0.0,
  val pitch: Double = 0.0,
  val dash: Double = 0.0,
  val m: Double = 11.0,
  val psi: Double = 0.0,
  val psiSpin: Double = 0.0,
  val taps: Boolean = false,
)

/** The per-state constants from upstream's `ActivityGyro.designs` (`kit-set.json`). */
internal data class Design(
  val strokes: List<Stroke> = emptyList(),
  val body: Double = 1.0,
  val glow: Double = 0.0,
  val ring: Double = 0.0,
  val ringA: Double = 0.0,
  val accent: ActivityAccent? = null,
  val pattern: Pattern = Pattern.NONE,
  val motion: Motion = Motion.NONE,
  val attack: Double = 0.0,
  val decay: Double = 0.0,
  val flash: Double = 0.0,
  val flashDecay: Double = 1.0,
  val flashBloom: Double = 0.0,
  val flashSweep: Boolean = false,
)

private val meridian = Stroke(k = 0.95, w = 0.2, thetaSpin = GlyphMath.TAU / 1.8, phi = PI)

private val idleDesign =
  Design(strokes = listOf(Stroke()), body = 0.68, glow = 0.14, pattern = Pattern.SHIMMER, attack = 0.15, decay = 0.5)

/** latitude(PI) = 0: the band condenses onto Thinking's meridian over 1.1 s. */
private val startupDesign = Design(
  strokes = listOf(meridian.copy(k = 0.02)),
  body = 0.8,
  glow = 0.5,
  pattern = Pattern.CONDENSE,
  motion = Motion.CONDENSE,
  attack = 0.05,
  decay = 0.35,
)

private val thinkingDesign =
  Design(strokes = listOf(meridian.copy(taps = true)), body = 1.0, glow = 0.5, ring = 0.1, ringA = 0.45)

private val longWorkDesign = Design(
  strokes = List(3) { i ->
    Stroke(
      k = 0.8, w = 0.13, theta = GlyphMath.HALF, e = GlyphMath.HALF, a = 0.4, r = 0.2,
      phiSpin =
        GlyphMath.TAU / 3,
      offset = i * GlyphMath.TAU / 3, pitch = 0.5,
    )
  },
  body = 0.9,
  glow = 0.55,
  motion = Motion.HELIX,
)

private val toolDesign = Design(
  strokes = listOf(meridian.copy(thetaSpin = GlyphMath.TAU / 1.15)),
  body = 1.0,
  glow = 0.55,
  ring = 0.2,
  ringA = 0.45,
  motion = Motion.KICK,
)

private val streamingDesign = Design(
  strokes = List(3) { i ->
    Stroke(
      k = 0.85,
      w = 0.12,
      theta = GlyphMath.HALF,
      e = GlyphMath.HALF,
      phiSpin =
        GlyphMath.TAU / 3,
      offset = i * GlyphMath.TAU / 3,
      pitch = 0.55,
    )
  },
  body = 0.85,
  glow = 0.45,
  motion = Motion.SPIRAL,
)

/** ringS(0.8) = 0.6: a face-on band broken into three rim-chasing dash clusters (m = 3, dash = 0.8). */
private val waitingDesign = Design(
  strokes = listOf(
    Stroke(
      k = 1.0,
      w = 0.24,
      theta = GlyphMath.HALF,
      phi = GlyphMath.latitudeAngle(0.6),
      dash = 0.8,
      m = 3.0,
      psi = -GlyphMath.HALF,
      psiSpin =
        GlyphMath.TAU / 4.5,
    ),
  ),
  body = 0.9,
  glow = 0.45,
  accent = ActivityAccent.ORANGE,
)

private val handoffDesign = Design(
  strokes = List(2) { i ->
    Stroke(
      k = 0.9,
      w = 0.17,
      theta = PI,
      r = 0.16,
      phiSpin = GlyphMath.TAU / 1.15,
      offset =
        i * PI,
    )
  },
  body = 0.9,
  glow = 0.45,
  accent = ActivityAccent.BLUE,
)

private val doneDesign = Design(
  strokes = listOf(Stroke(k = 1.0, w = 0.2, theta = GlyphMath.HALF, phi = GlyphMath.latitudeAngle(0.5))),
  body = 0.75,
  glow = 0.6,
  accent = ActivityAccent.GREEN,
  flash = 0.8,
  flashDecay = 0.9,
  flashBloom = 0.1,
  flashSweep = true,
)

private val failedDesign = Design(
  strokes = listOf(
    Stroke(k = 0.95, w = 0.11, theta = GlyphMath.HALF, e = GlyphMath.HALF, r = PI / 4, phi = PI),
    Stroke(k = 0.95, w = 0.11, theta = GlyphMath.HALF, e = GlyphMath.HALF, r = -PI / 4, phi = PI),
  ),
  body = 0.8,
  glow = 0.45,
  accent = ActivityAccent.RED,
  flash = 0.6,
  flashDecay = 1.2,
  flashBloom = 0.06,
)

internal fun designOf(activity: AgentActivity): Design = when (activity) {
  AgentActivity.IDLE -> idleDesign
  AgentActivity.STARTUP -> startupDesign
  AgentActivity.THINKING -> thinkingDesign
  AgentActivity.LONG_WORK -> longWorkDesign
  AgentActivity.TOOL -> toolDesign
  AgentActivity.STREAMING -> streamingDesign
  AgentActivity.WAITING -> waitingDesign
  AgentActivity.HANDOFF -> handoffDesign
  AgentActivity.DONE -> doneDesign
  AgentActivity.FAILED -> failedDesign
}
