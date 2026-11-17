package io.leloush.fang.agent

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** A malformed channel message must never reach the chat or throw on Pi's reader thread. */
class AskRequestTest {
  @Test
  fun parses_a_request_with_options() {
    val option = JSONObject().put("title", "Approve").put("description", "Merge the change")
    val question = JSONObject()
      .put("question", "What now?")
      .put("context", "The build is green")
      .put("options", JSONArray().put(option))
      .put("allowMultiple", true)
      .put("allowFreeform", false)
    val data = JSONObject()
      .put("id", "ask-1")
      .put("heading", "Release")
      .put("questions", JSONArray().put(question))
      .put("allowComment", true)
      .put("timeout", 5000)

    val request = AskRequest.from(data)!!

    assertEquals("ask-1", request.id)
    assertEquals("Release", request.heading)
    assertEquals(5000L, request.timeout)
    assertEquals(1, request.questions.size)
    assertEquals("Approve", request.questions[0].options[0].title)
    assertEquals("Merge the change", request.questions[0].options[0].description)
    assertEquals(true, request.questions[0].allowMultiple)
    assertEquals(false, request.questions[0].allowFreeform)
    assertEquals(true, request.allowComment)
  }

  @Test
  fun ignores_malformed_requests() {
    assertNull(AskRequest.from(JSONObject()))
    assertNull(AskRequest.from(JSONObject().put("id", "ask-2")))
    assertNull(AskRequest.from(JSONObject().put("id", "ask-3").put("questions", JSONArray())))
    val bad = JSONObject().put("id", "ask-4").put("questions", JSONArray().put("not an object"))
    assertNull(AskRequest.from(bad))
  }

  @Test
  fun keeps_valid_questions_when_one_is_malformed() {
    val good = JSONObject().put("question", "Pick one").put("options", JSONArray().put(JSONObject().put("title", "A")))
    val data = JSONObject()
      .put("id", "ask-5")
      .put("questions", JSONArray().put("bad").put(good))

    assertEquals(1, AskRequest.from(data)!!.questions.size)
  }
}
