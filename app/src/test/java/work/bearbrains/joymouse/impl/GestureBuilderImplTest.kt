package work.bearbrains.joymouse

import android.content.Context
import android.view.ViewConfiguration
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import work.bearbrains.joymouse.impl.GestureBuilderImpl
import work.bearbrains.joymouse.test.FakeClock
import work.bearbrains.joymouse.test.FakeJoystickCursorState

@RunWith(RobolectricTestRunner::class)
internal class GestureBuilderImplTest {
  private val context: Context = ApplicationProvider.getApplicationContext()
  private val gestureUtil =
    GestureUtil(ViewConfiguration.get(context), MOCK_MAX_GESTURE_DURATION_MILLISECONDS)
  private val clock = FakeClock()

  @Test
  fun action_forUnfinishedGesture_withNoMotion_andShortDelay_isTouch() {
    val sut = GestureBuilderImpl(FakeJoystickCursorState(), gestureUtil, clock)

    assertThat(sut.action).isEqualTo(GestureBuilder.Action.TOUCH)
  }

  @Test
  fun action_forUnfinishedGesture_withNoMotion_andLongDelay_isLongPress() {
    val sut = GestureBuilderImpl(FakeJoystickCursorState(), gestureUtil, clock)
    clock.advanceMilliseconds(gestureUtil.longTouchThresholdMilliseconds)

    assertThat(sut.action).isEqualTo(GestureBuilder.Action.LONG_TOUCH)
  }

  @Test
  fun action_forUnfinishedGesture_withVerySmallMotion_andShortDelay_isDrag() {
    val sut = GestureBuilderImpl(FakeJoystickCursorState(), gestureUtil, clock)
    sut.cursorMove(
      FakeJoystickCursorState(pointerX = 0f, pointerY = GestureBuilder.MIN_DRAG_DISTANCE - 1f)
    )

    assertThat(sut.action).isEqualTo(GestureBuilder.Action.TOUCH)
  }

  @Test
  fun action_forUnfinishedGesture_withVerySmallMotion_andLongDelay_isDrag() {
    val sut = GestureBuilderImpl(FakeJoystickCursorState(), gestureUtil, clock)
    sut.cursorMove(
      FakeJoystickCursorState(pointerX = 0f, pointerY = GestureBuilder.MIN_DRAG_DISTANCE - 1f)
    )
    clock.advanceMilliseconds(gestureUtil.longTouchThresholdMilliseconds)

    assertThat(sut.action).isEqualTo(GestureBuilder.Action.LONG_TOUCH)
  }

  @Test
  fun action_forUnfinishedGesture_withSmallMotion_isDrag() {
    val sut = GestureBuilderImpl(FakeJoystickCursorState(), gestureUtil, clock)
    sut.cursorMove(
      FakeJoystickCursorState(pointerX = 0f, pointerY = GestureBuilder.MIN_DRAG_DISTANCE)
    )

    assertThat(sut.action).isEqualTo(GestureBuilder.Action.DRAG)
  }

  @Test
  fun action_forUnfinishedGesture_withLargeMotion_isFling() {
    val sut = GestureBuilderImpl(FakeJoystickCursorState(), gestureUtil, clock)
    sut.cursorMove(
      FakeJoystickCursorState(pointerX = 0f, pointerY = GestureBuilder.MIN_FLING_DISTANCE)
    )

    assertThat(sut.action).isEqualTo(GestureBuilder.Action.FLING)
  }

  @Test
  fun action_forUnfinishedGesture_withSmallMotion_followedByReturnToStart_isTouch() {
    val sut = GestureBuilderImpl(FakeJoystickCursorState(), gestureUtil, clock)
    sut.cursorMove(
      FakeJoystickCursorState(pointerX = 0f, pointerY = GestureBuilder.MIN_DRAG_DISTANCE)
    )
    sut.cursorMove(FakeJoystickCursorState(pointerX = 0f, pointerY = 0f))

    assertThat(sut.action).isEqualTo(GestureBuilder.Action.TOUCH)
  }

  @Test
  fun action_forUnfinishedGesture_withLargeMotion_followedByReturnToStart_isTouch() {
    val sut = GestureBuilderImpl(FakeJoystickCursorState(), gestureUtil, clock)
    sut.cursorMove(
      FakeJoystickCursorState(pointerX = 0f, pointerY = GestureBuilder.MIN_FLING_DISTANCE)
    )
    sut.cursorMove(FakeJoystickCursorState(pointerX = 0f, pointerY = 0f))

    assertThat(sut.action).isEqualTo(GestureBuilder.Action.TOUCH)
  }

  private companion object {
    const val MOCK_MAX_GESTURE_DURATION_MILLISECONDS = 2000L
  }
}
