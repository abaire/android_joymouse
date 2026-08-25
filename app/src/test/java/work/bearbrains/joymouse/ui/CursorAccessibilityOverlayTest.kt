package work.bearbrains.joymouse.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import work.bearbrains.joymouse.DisplayInfo
import work.bearbrains.joymouse.input.CursorColors
import work.bearbrains.joymouse.input.CursorConfig
import work.bearbrains.joymouse.input.CursorPalette

@RunWith(RobolectricTestRunner::class)
class CursorAccessibilityOverlayTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val displayInfo =
    DisplayInfo(displayId = 0, context = context, windowWidth = 1920f, windowHeight = 1080f)

  @Test
  fun overlay_initializesWithDefaultConfigAndState() {
    val overlay = CursorAccessibilityOverlay(displayInfo)
    assertThat(overlay.cursorConfig).isEqualTo(CursorConfig.DEFAULT)
    assertThat(overlay.cursorState).isEqualTo(CursorAccessibilityOverlay.State.STATE_RELEASED)
    overlay.close()
  }

  @Test
  fun overlay_stateChanges_updateProperly() {
    val overlay = CursorAccessibilityOverlay(displayInfo)

    overlay.cursorState = CursorAccessibilityOverlay.State.STATE_PRESSED_TAP
    assertThat(overlay.cursorState).isEqualTo(CursorAccessibilityOverlay.State.STATE_PRESSED_TAP)

    overlay.cursorState = CursorAccessibilityOverlay.State.STATE_PRESSED_SLOW_DRAG
    assertThat(overlay.cursorState).isEqualTo(CursorAccessibilityOverlay.State.STATE_PRESSED_SLOW_DRAG)

    overlay.cursorState = CursorAccessibilityOverlay.State.STATE_PRESSED_FLING
    assertThat(overlay.cursorState).isEqualTo(CursorAccessibilityOverlay.State.STATE_PRESSED_FLING)

    overlay.close()
  }

  @Test
  fun overlay_customConfig_updatesProperly() {
    val overlay = CursorAccessibilityOverlay(displayInfo)
    val customConfig =
      CursorConfig(
        palette = CursorPalette.COLORBLIND_FRIENDLY,
        changeShapeForMode = true,
      )

    overlay.cursorConfig = customConfig
    assertThat(overlay.cursorConfig.palette).isEqualTo(CursorPalette.COLORBLIND_FRIENDLY)
    assertThat(overlay.cursorConfig.changeShapeForMode).isTrue()

    overlay.close()
  }
}
