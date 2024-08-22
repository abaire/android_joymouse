package work.bearbrains.joymouse.input.impl

import android.accessibilityservice.GestureDescription
import android.content.Context
import android.view.Display
import android.view.ViewConfiguration
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
import javax.inject.Provider
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.anyInt
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import work.bearbrains.joymouse.DisplayInfo
import work.bearbrains.joymouse.input.GestureBuilder
import work.bearbrains.joymouse.input.GestureUtil
import work.bearbrains.joymouse.test.FakeClock
import work.bearbrains.joymouse.test.FakeJoystickCursorState

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
internal class GestureBuilderImplTest {
  private val context: Context = ApplicationProvider.getApplicationContext()
  private val gestureUtil =
    GestureUtil(ViewConfiguration.get(context), MOCK_MAX_GESTURE_DURATION_MILLISECONDS)
  private val clock = FakeClock()

  private val gestureDescriptionBuilderProvider =
    object : Provider<GestureDescription.Builder> {
      override fun get(): GestureDescription.Builder = mock {
        on { setDisplayId(anyInt()) } doAnswer Mockito.RETURNS_SELF
      }
    }

  private val displayInfo =
    DisplayInfo(Display.DEFAULT_DISPLAY, context, windowWidth = 640f, windowHeight = 480f)

  @Test
  fun action_forUnfinishedGesture_withNoMotion_andShortDelay_isTouch() {
    val sut =
      GestureBuilderImpl(
        FakeJoystickCursorState(displayInfo),
        gestureUtil,
        clock,
        gestureDescriptionBuilderProvider,
      )

    Truth.assertThat(sut.action).isEqualTo(GestureBuilder.Action.TOUCH)
  }

  @Test
  fun action_forUnfinishedGesture_withNoMotion_andLongDelay_isLongPress() {
    val sut =
      GestureBuilderImpl(
        FakeJoystickCursorState(displayInfo),
        gestureUtil,
        clock,
        gestureDescriptionBuilderProvider,
      )
    clock.advanceMilliseconds(gestureUtil.longTouchThresholdMilliseconds)

    Truth.assertThat(sut.action).isEqualTo(GestureBuilder.Action.LONG_TOUCH)
  }

  @Test
  fun action_forUnfinishedGesture_withVerySmallMotion_andShortDelay_isDrag() {
    val sut =
      GestureBuilderImpl(
        FakeJoystickCursorState(displayInfo),
        gestureUtil,
        clock,
        gestureDescriptionBuilderProvider,
      )
    sut.cursorMove(
      FakeJoystickCursorState(
        displayInfo,
        pointerX = 0f,
        pointerY = GestureBuilder.MIN_DRAG_DISTANCE - 1f
      )
    )

    Truth.assertThat(sut.action).isEqualTo(GestureBuilder.Action.TOUCH)
  }

  @Test
  fun action_forUnfinishedGesture_withVerySmallMotion_andLongDelay_isDrag() {
    val sut =
      GestureBuilderImpl(
        FakeJoystickCursorState(displayInfo),
        gestureUtil,
        clock,
        gestureDescriptionBuilderProvider,
      )
    sut.cursorMove(
      FakeJoystickCursorState(
        displayInfo,
        pointerX = 0f,
        pointerY = GestureBuilder.MIN_DRAG_DISTANCE - 1f
      )
    )
    clock.advanceMilliseconds(gestureUtil.longTouchThresholdMilliseconds)

    Truth.assertThat(sut.action).isEqualTo(GestureBuilder.Action.LONG_TOUCH)
  }

  @Test
  fun action_forUnfinishedGesture_withSmallMotion_isDrag() {
    val sut =
      GestureBuilderImpl(
        FakeJoystickCursorState(displayInfo),
        gestureUtil,
        clock,
        gestureDescriptionBuilderProvider,
      )
    sut.cursorMove(
      FakeJoystickCursorState(
        displayInfo,
        pointerX = 0f,
        pointerY = GestureBuilder.MIN_DRAG_DISTANCE
      )
    )

    Truth.assertThat(sut.action).isEqualTo(GestureBuilder.Action.DRAG)
  }

  @Test
  fun action_forUnfinishedGesture_withDistanceFlingStrategy_withLargeMotion_isFling() {
    val sut =
      GestureBuilderImpl(
        FakeJoystickCursorState(displayInfo),
        gestureUtil,
        clock,
        gestureDescriptionBuilderProvider,
        useDistanceBasedFlingStrategy = true,
      )
    sut.cursorMove(
      FakeJoystickCursorState(
        displayInfo,
        pointerX = 0f,
        pointerY = GestureBuilder.MIN_FLING_DISTANCE
      )
    )

    Truth.assertThat(sut.action).isEqualTo(GestureBuilder.Action.FLING)
  }

  @Test
  fun action_forUnfinishedGesture_withoutDistanceFlingStrategy_withLargeMotion_isDrag() {
    val sut =
      GestureBuilderImpl(
        FakeJoystickCursorState(displayInfo),
        gestureUtil,
        clock,
        gestureDescriptionBuilderProvider,
        useDistanceBasedFlingStrategy = false,
      )
    sut.cursorMove(
      FakeJoystickCursorState(
        displayInfo,
        pointerX = 0f,
        pointerY = GestureBuilder.MIN_FLING_DISTANCE
      )
    )

    Truth.assertThat(sut.action).isEqualTo(GestureBuilder.Action.DRAG)
  }

  @Test
  fun action_forUnfinishedGesture_withSmallMotion_followedByReturnToStart_isTouch() {
    val sut =
      GestureBuilderImpl(
        FakeJoystickCursorState(displayInfo),
        gestureUtil,
        clock,
        gestureDescriptionBuilderProvider,
      )
    sut.cursorMove(
      FakeJoystickCursorState(
        displayInfo,
        pointerX = 0f,
        pointerY = GestureBuilder.MIN_DRAG_DISTANCE
      )
    )
    sut.cursorMove(FakeJoystickCursorState(displayInfo, pointerX = 0f, pointerY = 0f))

    Truth.assertThat(sut.action).isEqualTo(GestureBuilder.Action.TOUCH)
  }

  @Test
  fun action_forUnfinishedGesture_withLargeMotion_followedByReturnToStart_isTouch() {
    val sut =
      GestureBuilderImpl(
        FakeJoystickCursorState(displayInfo),
        gestureUtil,
        clock,
        gestureDescriptionBuilderProvider,
      )
    sut.cursorMove(
      FakeJoystickCursorState(
        displayInfo,
        pointerX = 0f,
        pointerY = GestureBuilder.MIN_FLING_DISTANCE
      )
    )
    sut.cursorMove(FakeJoystickCursorState(displayInfo, pointerX = 0f, pointerY = 0f))

    Truth.assertThat(sut.action).isEqualTo(GestureBuilder.Action.TOUCH)
  }

  private companion object {
    const val MOCK_MAX_GESTURE_DURATION_MILLISECONDS = 2000L
  }
}
