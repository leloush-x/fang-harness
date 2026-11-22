package io.leloush.fang.ui.components.activityglyph

import kotlin.math.max
import kotlin.math.sqrt

/** One dot of the 7x7 grid: column/row in the shader's cell array, sphere normal, key light and size. */
internal data class OrbCell(
  val column: Int,
  val row: Int,
  val nx: Double,
  val ny: Double,
  val nz: Double,
  val shade: Double,
  val size: Double,
)

/** Upstream's `GyroCell.orb(7)`: a disc mask with a top-left key light and dots shrinking toward the limb. */
internal object OrbCells {
  val seven: List<OrbCell> by lazy { orb(7) }

  private fun orb(n: Int): List<OrbCell> {
    val half = (n - 1) / 2.0
    val r = n / 2.0 + 0.1
    val lx = -0.42
    val ly = -0.52
    val lz = 0.74
    val ln = sqrt(lx * lx + ly * ly + lz * lz)
    val cells = mutableListOf<OrbCell>()
    for (row in 0 until n) {
      for (column in 0 until n) {
        val x = column - half
        val y = row - half
        if (sqrt(x * x + y * y) > n / 2.0) continue
        val nx = x / r
        val ny = y / r
        val nz = sqrt(max(0.0, 1 - nx * nx - ny * ny))
        val shade = max(0.0, (nx * lx + ny * ly + nz * lz) / ln)
        cells.add(OrbCell(column, row, nx, ny, nz, shade, 0.7 + 0.3 * nz))
      }
    }
    return cells
  }
}
