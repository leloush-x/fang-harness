package io.leloush.fang.ui.components.activityglyph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CellFieldTest {
  private val masked = OrbCells.seven.map { (it.row * 7 + it.column) * 4 }.toSet()

  private fun settled(activity: AgentActivity, reduceMotion: Boolean = false): CellFrame {
    val field = ActivityCellField()
    var frame = field.frame
    for (step in 1..40) frame = field.advance(step / 60.0, activity, reduceMotion)
    return frame
  }

  @Test
  fun everyStateProducesFiniteCellsWithAMaskedDotDisc() {
    assertEquals(37, masked.size)
    for (activity in states) {
      val frame = settled(activity)
      assertTrue(activity.name, frame.cells.all { it.isFinite() })
      assertTrue(activity.name, masked.all { frame.cells[it + 3] > 0f })
      assertTrue(activity.name, frame.cells.indices.all { i -> (i / 4) * 4 in masked || frame.cells[i] == 0f })
      assertTrue(activity.name, frame.glow.isFinite() && frame.heat.isFinite())
    }
  }

  @Test
  fun reduceMotionProducesFiniteStillCellsForEveryState() {
    for (activity in states) {
      val frame = settled(activity, reduceMotion = true)
      assertTrue(activity.name, frame.cells.all { it.isFinite() })
      assertTrue(activity.name, frame.cells.any { it > 0.05f })
    }
  }

  @Test
  fun accentsMatchTheFourColouredStates() {
    val expected = mapOf(
      AgentActivity.WAITING to ActivityAccent.ORANGE,
      AgentActivity.FAILED to ActivityAccent.RED,
      AgentActivity.DONE to ActivityAccent.GREEN,
      AgentActivity.HANDOFF to ActivityAccent.BLUE,
    )
    for (activity in states) {
      val frame = settled(activity)
      assertEquals(activity.name, expected[activity], frame.accent)
      assertEquals(activity.name, if (activity in expected) 1f else 0f, frame.tint, 0f)
    }
  }

  private val states = AgentActivity.entries
}
