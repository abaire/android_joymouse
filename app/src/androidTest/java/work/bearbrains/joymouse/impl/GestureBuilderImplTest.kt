package work.bearbrains.joymouse.impl

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.RectF
import android.view.ViewConfiguration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import work.bearbrains.joymouse.GestureUtil
import work.bearbrains.joymouse.test.FakeClock
import work.bearbrains.joymouse.test.FakeJoystickCursorState

@RunWith(AndroidJUnit4::class)
internal class GestureBuilderImplTest {
  private val context = InstrumentationRegistry.getInstrumentation().targetContext
  private val gestureUtil =
    GestureUtil(ViewConfiguration.get(context), GestureDescription.getMaxGestureDuration())
  private val clock = FakeClock()

  @Test
  fun gesture_withNoMotion_andShortDelay_isTouch() {
    val sut = GestureBuilderImpl(FakeJoystickCursorState(), gestureUtil, clock)
    sut.endGesture(FakeJoystickCursorState())

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
    val sut = GestureBuilderImpl(FakeJoystickCursorState(), gestureUtil, clock)
    val durationMilliseconds = GestureDescription.getMaxGestureDuration() - 1
    clock.advanceMilliseconds(durationMilliseconds)
    sut.endGesture(FakeJoystickCursorState())

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
    val sut = GestureBuilderImpl(FakeJoystickCursorState(), gestureUtil, clock)
    val durationMilliseconds = GestureDescription.getMaxGestureDuration() + 1
    clock.advanceMilliseconds(durationMilliseconds)
    sut.endGesture(FakeJoystickCursorState())

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
