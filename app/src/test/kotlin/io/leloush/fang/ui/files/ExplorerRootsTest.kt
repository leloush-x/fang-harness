package io.leloush.fang.ui.files

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The folders an explorer pane has shown, and which of them it shows now. Back and forward walk them, as a browser
 * does, and opening a place drops what forward would have shown.
 */
class ExplorerRootsTest {
  @Test
  fun thePaneStartsOnTheFolderItOpenedOn() {
    val roots = ExplorerRoots("/w")

    assertEquals("/w", roots.shown)
    assertFalse(roots.canGoBack)
    assertFalse(roots.canGoForward)
  }

  @Test
  fun backAndForwardWalkThePlaces() {
    val roots = ExplorerRoots("/w")
    roots.open("/sdcard")

    assertEquals("/sdcard", roots.shown)
    assertTrue(roots.canGoBack)
    assertFalse(roots.canGoForward)

    roots.goBack()
    assertEquals("/w", roots.shown)
    assertTrue(roots.canGoForward)

    roots.goForward()
    assertEquals("/sdcard", roots.shown)
  }

  @Test
  fun goingBackAtTheStartDoesNothing() {
    val roots = ExplorerRoots("/w")

    roots.goBack()

    assertEquals("/w", roots.shown)
  }

  @Test
  fun goingForwardAtTheEndDoesNothing() {
    val roots = ExplorerRoots("/w")
    roots.open("/sdcard")

    roots.goForward()

    assertEquals("/sdcard", roots.shown)
  }

  @Test
  fun openingAPlaceDropsWhatForwardWouldHaveShown() {
    val roots = ExplorerRoots("/w")
    roots.open("/sdcard")
    roots.goBack()
    assertTrue(roots.canGoForward)

    roots.open("/usr")

    assertEquals("/usr", roots.shown)
    assertFalse(roots.canGoForward)
  }

  @Test
  fun openingTheFolderShownChangesNothing() {
    val roots = ExplorerRoots("/w")
    roots.open("/w")

    assertEquals("/w", roots.shown)
    assertFalse(roots.canGoBack)
    assertFalse(roots.canGoForward)
  }
}
