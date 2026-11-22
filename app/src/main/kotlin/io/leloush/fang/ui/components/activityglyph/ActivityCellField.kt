package io.leloush.fang.ui.components.activityglyph

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** One frame's shader cells plus the two globals the renderer packs with them. */
internal data class CellFrame(
  val cells: FloatArray,
  val glow: Float,
  val accent: ActivityAccent?,
  val tint: Float,
  val heat: Float,
)

/**
 * A small, analytic reimplementation of upstream's spring-driven ActivityGyro. The stroke geometry,
 * per-state constants and LED-field formula are ported; angles are driven directly by time instead of
 * critically damped springs, and a short exponential phosphor stands in for the per-cell afterglow.
 */
internal class ActivityCellField {
  private val coords = OrbCells.seven
  private val cells = FloatArray(49 * 4)
  private val light = DoubleArray(49)
  private val fresh = DoubleArray(49)
  private val lit = DoubleArray(49)
  private val heat = DoubleArray(49)
  private var current: AgentActivity? = null
  private var last: Double? = null
  private var age = 0.0
  private var clock = 0.0

  var frame = CellFrame(cells, 0f, null, 0f, 0f)
    private set

  /** Nothing moves or cools further in a final or reduced-motion state. */
  var isResting = false
    private set

  fun advance(now: Double, activity: AgentActivity, reduceMotion: Boolean): CellFrame {
    val design = designOf(activity)
    val immediate = current != activity
    if (immediate) {
      current = activity
      age = 0.0
      light.fill(0.0)
      heat.fill(0.0)
    }
    val dt = GlyphMath.clamp(now - (last ?: now), 0.0, 1.0 / 30.0)
    last = now
    age += dt
    if (!reduceMotion) clock += dt
    val t = if (reduceMotion) 0.0 else clock
    var maxFresh = 1e-3
    for (i in coords.indices) {
      val cell = coords[i]
      val p = Vec3(cell.nx, cell.ny, cell.nz)
      var value = 0.0
      var fr = 0.0
      for (stroke in design.strokes) {
        val g = strokeLight(cell, p, stroke, design, t, reduceMotion)
        value = 1.0 - (1.0 - value) * (1.0 - GlyphMath.clamp(g, 0.0, 1.0))
        fr = maxOf(fr, g)
      }
      lit[i] = value
      fresh[i] = fr
      maxFresh = maxOf(maxFresh, fr)
    }
    return finish(design, maxFresh, t, dt, immediate, reduceMotion)
  }

  private fun finish(
    design: Design,
    maxFresh: Double,
    t: Double,
    dt: Double,
    immediate: Boolean,
    reduce: Boolean,
  ): CellFrame {
    val tint = if (design.accent == null) 0.0 else 1.0
    val ringN = if (design.ring > 0) GlyphMath.basis(0.0, GlyphMath.HALF, design.ringA, 0.0).n else null
    var hottest = 0.0
    for (i in coords.indices) {
      val cell = coords[i]
      val shape = fresh[i] / maxFresh
      var target =
        (0.14 + 0.4 * cell.shade.pow(1.2)) * design.body + 0.95 * lit[i] + patternLight(i, design, shape, t, reduce)
      if (ringN != null) {
        val d = Vec3(cell.nx, cell.ny, cell.nz) dot ringN
        target += design.ring * exp(-d * d / 0.045)
      }
      val drive = flashDrive(cell, design, shape, reduce)
      val h = heat[i]
      val tau = if (drive > h) 0.03 else design.flashDecay
      val nextHeat = if (reduce || design.flash == 0.0) 0.0 else h + (drive - h) * (1 - exp(-dt / tau))
      heat[i] = nextHeat
      val a = design.flash * nextHeat
      hottest = maxOf(hottest, a)
      target += 0.7 * a
      var fr = maxOf(fresh[i], a)
      if (design.flash > 0) fr = maxOf(fr * (0.3 + 0.7 * a), a)
      val fall = if (design.decay == 0.0) 0.35 else design.decay
      val tauLight = if (target > light[i]) design.attack else fall
      val shown = if (immediate || reduce ||
        tauLight < 1e-3
      ) {
        target
      } else {
        light[i] + (target - light[i]) * (1 - exp(-dt / tauLight))
      }
      light[i] = shown
      val base = (cell.row * 7 + cell.column) * 4
      cells[base] = shown.toFloat()
      cells[base + 1] = fr.toFloat()
      cells[base + 2] = (tint * GlyphMath.sstep(0.3, 0.65, shown) * (1 - a)).toFloat()
      cells[base + 3] = maxOf(1e-3, cell.size * (1 + design.flashBloom * nextHeat)).toFloat()
    }
    isResting = reduce || (design.flash > 0 && age > 1.2)
    frame = CellFrame(cells, (design.glow + 0.9 * hottest).toFloat(), design.accent, tint.toFloat(), hottest.toFloat())
    return frame
  }

  /** One band's Gaussian contribution at one cell, with dash clusters and comet taps where the design has them. */
  private fun strokeLight(cell: OrbCell, p: Vec3, stroke: Stroke, design: Design, t: Double, reduce: Boolean): Double {
    var k = stroke.k
    if (k <= 0.0) return 0.0
    if (design.motion == Motion.CONDENSE && !reduce) k = 0.95 * GlyphMath.sstep(0.2, 1.1, age)
    val s = GlyphMath.latitude(stroke.phi + stroke.offset + if (reduce) 0.0 else stroke.phiSpin * t)
    val pitch = if (design.motion == Motion.HELIX && !reduce) 0.5 + 0.25 * sin(age * 0.7) else stroke.pitch
    val fw = minOf(0.6, 0.06 + 2.2 * stroke.w + 0.5 * pitch)
    k *= GlyphMath.sstep(1.1, 1.1 - fw, abs(s)) * (0.65 + 0.35 * cell.nz)
    if (k <= 1e-3) return 0.0
    val theta = stroke.theta + if (reduce) 0.0 else stroke.thetaSpin * t
    val b = GlyphMath.basis(theta, stroke.e, stroke.a, stroke.r)
    val pu = p dot b.u
    val pv = p dot b.v
    val needLam = pitch > 1e-3 || stroke.dash > 1e-3
    val lam = if (needLam) atan2(pv, pu) else 0.0
    val on = if (needLam) GlyphMath.sstep(0.04, 0.3, sqrt(pu * pu + pv * pv)) else 1.0
    var d = (p dot b.n) - s
    if (pitch > 1e-3) d += 0.5 * on * pitch * cos(lam)
    var g = GlyphMath.gauss(d, stroke.w) * k
    if (stroke.dash > 1e-3) {
      val ds = GlyphMath.wrap(lam - stroke.psi - if (reduce) 0.0 else stroke.psiSpin * t)
      val cluster = 0.5 + 0.5 * cos(stroke.m * ds)
      g *= 1 - on * stroke.dash * (1 - cluster.pow(if (stroke.m <= 4) 1.5 else 4.0))
    }
    if (stroke.taps && !reduce) {
      for (j in 1..3) {
        val lag = j * 0.05
        val tap = GlyphMath.basis(theta - stroke.thetaSpin * lag, stroke.e, stroke.a, stroke.r).n
        g = maxOf(g, GlyphMath.gauss((p dot tap) - s, stroke.w) * k * 0.5.pow(j + 1) * 0.9)
      }
    }
    return g
  }

  private fun patternLight(i: Int, design: Design, shape: Double, t: Double, reduce: Boolean): Double =
    when (design.pattern) {
      Pattern.SHIMMER -> {
        val f = 0.14 + 0.1 * GlyphMath.hash(i + 17)
        val v = 0.5 + 0.5 * sin(GlyphMath.TAU * (t * f + GlyphMath.hash(i)))
        0.22 * v.pow(10) * if (reduce) 1.0 else GlyphMath.sstep(0.0, 0.6, age)
      }

      Pattern.CONDENSE -> {
        val phase = t * 3 + GlyphMath.hash(i)
        GlyphMath.gauss(phase - floor(phase) - 0.5, 0.06) * (1 - GlyphMath.sstep(0.1, 1.0, age) * (1 - shape))
      }

      Pattern.NONE -> 0.0
    }

  private fun flashDrive(cell: OrbCell, design: Design, shape: Double, reduce: Boolean): Double {
    if (reduce || design.flash == 0.0) return 0.0
    val delay = if (design.flashSweep) {
      val a = atan2(cell.ny, cell.nx) + GlyphMath.HALF
      (a - GlyphMath.TAU * floor(a / GlyphMath.TAU)) / GlyphMath.TAU * 0.36
    } else {
      0.0
    }
    // shortcut: no tool-event stream reaches this API, so KICK gets a short recurring flash instead.
    val x = if (design.motion == Motion.KICK) age % 0.7 - 0.2 else age - 0.2 - delay
    return if (x >= 0.0 && x < 0.1) 0.15 + 0.85 * shape else 0.0
  }
}
