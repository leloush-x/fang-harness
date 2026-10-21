package io.leloush.fang.agent

import io.leloush.fang.agent.TimelineItem.AssistantText
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * The state the chat screen renders.
 *
 * Two states are the same only when they are the same object, so the [kotlinx.coroutines.flow.StateFlow] that holds
 * one does not walk the whole chat, and the streamed text with it, to tell two states apart on every delta.
 */
class TimelineStateTest {
  private val items = listOf(AssistantText("m0.0", "Done", streaming = false, timestamp = 1))

  @Test
  fun twoStatesWithTheSameItemsAreNotTheSame() {
    val first = TimelineState(items = items)
    val second = TimelineState(items = items)

    assertNotSame(first, second)
    assertNotEquals(first, second)
  }

  @Test
  fun aStateIsTheSameAsItself() {
    val state = TimelineState(items = items)

    assertSame(state, state)
    assertEquals(state, state)
  }

  @Test
  fun aStreamedDeltaPublishesANewState() {
    val timeline = ChatTimeline()
    timeline.apply(JSONObject("""{"type":"message_start","message":{"role":"assistant","content":[]}}"""))
    val before = timeline.state.value

    timeline.apply(
      JSONObject(
        """{"type":"message_update","assistantMessageEvent":{"type":"text_delta","contentIndex":0,"delta":"Hi"}}""",
      ),
    )
    val after = timeline.state.value

    assertNotSame(before, after)
    assertEquals("Hi", (after.items.last() as AssistantText).markdown)
  }
}
