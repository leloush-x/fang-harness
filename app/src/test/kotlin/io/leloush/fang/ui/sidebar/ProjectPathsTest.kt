package io.leloush.fang.ui.sidebar

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectPathsTest {
  private val places = listOf("/root", "/storage/emulated/0", "/storage/emulated/0/sd")

  @Test
  fun aPlaceAndItsSubfoldersSelectIt() {
    assertEquals(0, placeIndexOf(places, "/root"))
    assertEquals(1, placeIndexOf(places, "/storage/emulated/0/Download"))
  }

  @Test
  fun theDeepestPlaceWins() {
    assertEquals(2, placeIndexOf(places, "/storage/emulated/0/sd/Music"))
  }

  @Test
  fun aSiblingWithTheSamePrefixOrAFolderAboveSelectsNothing() {
    assertEquals(-1, placeIndexOf(places, "/storage/emulated/01"))
    assertEquals(-1, placeIndexOf(places, "/storage/emulated"))
    assertEquals(-1, placeIndexOf(places, "/"))
  }
}
