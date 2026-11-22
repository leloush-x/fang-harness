package io.leloush.fang.ui.components.activityglyph

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

/** A point on the unit sphere: a cell's normal, or the axis/span of a stroke's great circle. */
internal class Vec3(val x: Double, val y: Double, val z: Double) {
  infix fun dot(other: Vec3): Double = x * other.x + y * other.y + z * other.z
}

/** The orthonormal frame (n, u, v) spanning one stroke's circle. */
internal data class Basis(val n: Vec3, val u: Vec3, val v: Vec3)

/** The small numeric kit from upstream's ActivityGyro, kept separate from the per-state designs. */
internal object GlyphMath {
  const val TAU = 6.283185307179586
  const val HALF = 1.5707963267948966

  fun clamp(x: Double, a: Double, b: Double): Double = if (x < a) {
    a
  } else if (x > b) {
    b
  } else {
    x
  }

  fun sstep(a: Double, b: Double, x: Double): Double {
    val t = clamp((x - a) / (b - a), 0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)
  }

  fun wrap(a: Double): Double = a - TAU * floor((a + PI) / TAU)

  fun gauss(x: Double, s: Double): Double = exp(-(x * x) / (2 * s * s))

  /** Latitude falls from 1.1 to -1.1 over each turn, so a rolling stroke reappears at the other pole. */
  fun latitude(phi: Double): Double = 1.1 - 2.2 * (phi / TAU - floor(phi / TAU))

  fun latitudeAngle(s: Double): Double = TAU * (1.1 - s) / 2.2

  /** A fixed pseudo-random per cell: the prototype's `hash(row * 7 + column)`. */
  fun hash(i: Int): Double {
    val x = sin(i * 12.9898 + 4.1) * 43758.5453
    return x - floor(x)
  }

  /** n, u, v for angles (theta, e, a, r): n = Rz(r)·Rx(a)·(cos e cos theta, sin e, cos e sin theta). */
  fun basis(th: Double, e: Double, a: Double, r: Double): Basis {
    val ce = cos(e)
    val se = sin(e)
    val ct = cos(th)
    val st = sin(th)
    return Basis(
      rotate(ce * ct, se, ce * st, a, r),
      rotate(-st, 0.0, ct, a, r),
      rotate(-se * ct, ce, -se * st, a, r),
    )
  }
}

/** Rx(a) then Rz(r), as one step so the basis helper stays small. */
internal fun rotate(x: Double, y: Double, z: Double, a: Double, r: Double): Vec3 {
  val y1 = y * cos(a) - z * sin(a)
  val z1 = y * sin(a) + z * cos(a)
  return Vec3(x * cos(r) - y1 * sin(r), x * sin(r) + y1 * cos(r), z1)
}
