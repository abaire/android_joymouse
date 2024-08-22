package work.bearbrains.joymouse.impl

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.RectF
import android.view.Display
import android.view.ViewConfiguration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import work.bearbrains.joymouse.DisplayInfo
import work.bearbrains.joymouse.input.GestureUtil
import work.bearbrains.joymouse.input.impl.GestureBuilderImpl
import work.bearbrains.joymouse.input.impl.GestureDescriptionBuilderProvider
import work.bearbrains.joymouse.test.FakeClock
import work.bearbrains.joymouse.test.FakeJoystickCursorState

@RunWith(AndroidJUnit4::class)
internal class GestureBuilderImplTest {
  private val context = InstrumentationRegistry.getInstrumentation().targetContext
  private val gestureUtil =
    GestureUtil(ViewConfiguration.get(context), GestureDescription.getMaxGestureDuration())
  private val clock = FakeClock()
  private val displayInfo =
    DisplayInfo(
      Display.DEFAULT_DISPLAY,
      context,
      windowWidth = 1024f,
      windowHeight = 768f,
    )

  @Test
  fun gesture_withNoMotion_andShortDelay_isTouch() {
    val sut =
      GestureBuilderImpl(
        FakeJoystickCursorState(displayInfo),
        gestureUtil,
        clock,
        GestureDescriptionBuilderProvider
      )
    sut.endGesture(FakeJoystickCursorState(displayInfo))

    val result = sut.build()

    assertThat(result.strokeCount).isEqualTo(1)

    val strokeDescription = result.getStroke(0)
    assertThat(strokeDescription.startTime).isEqualTo(0L)
    assertThat(strokeDescription.duration).isLessThan(gestureUtil.longTouchThresholdMilliseconds)
    assertThat(strokeDescription.willContinue()).isFalse()

    assertThat(strokeDescription.path.isSinglePoint()).isTrue()
  }

  @Test
  fun gesture_withNoMotion_andLongDelay_passesThroughDelay() {
    val sut =
      GestureBuilderImpl(
        FakeJoystickCursorState(displayInfo),
        gestureUtil,
        clock,
        GestureDescriptionBuilderProvider
      )
    val durationMilliseconds = GestureDescription.getMaxGestureDuration() - 1
    clock.advanceMilliseconds(durationMilliseconds)
    sut.endGesture(FakeJoystickCursorState(displayInfo))

    val result = sut.build()

    assertThat(result.strokeCount).isEqualTo(1)

    val strokeDescription = result.getStroke(0)
    assertThat(strokeDescription.startTime).isEqualTo(0L)
    assertThat(strokeDescription.duration).isEqualTo(durationMilliseconds)
    assertThat(strokeDescription.willContinue()).isFalse()

    assertThat(strokeDescription.path.isSinglePoint()).isTrue()
  }

  @Test
  fun gesture_withNoMotion_hasDelayCappedToMaximum() {
    val sut =
      GestureBuilderImpl(
        FakeJoystickCursorState(displayInfo),
        gestureUtil,
        clock,
        GestureDescriptionBuilderProvider
      )
    val durationMilliseconds = GestureDescription.getMaxGestureDuration() + 1
    clock.advanceMilliseconds(durationMilliseconds)
    sut.endGesture(FakeJoystickCursorState(displayInfo))

    val result = sut.build()

    assertThat(result.strokeCount).isEqualTo(1)

    val strokeDescription = result.getStroke(0)
    assertThat(strokeDescription.startTime).isEqualTo(0L)
    assertThat(strokeDescription.duration).isEqualTo(GestureDescription.getMaxGestureDuration())
    assertThat(strokeDescription.willContinue()).isFalse()

    assertThat(strokeDescription.path.isSinglePoint()).isTrue()
  }

  private companion object {
    fun Path.isSinglePoint(): Boolean {
      val pathBounds = RectF()
      computeBounds(pathBounds, true)
      return pathBounds.isEmpty
    }
  }
}
