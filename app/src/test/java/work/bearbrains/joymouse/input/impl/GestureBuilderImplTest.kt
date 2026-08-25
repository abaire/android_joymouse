package work.bearbrains.joymouse.input.impl

import android.accessibilityservice.GestureDescription
import android.content.Context
import android.view.Display
import android.view.ViewConfiguration
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
import javax.inject.Provider
import kotlin.time.Duration.Companion.milliseconds
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.anyInt
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import work.bearbrains.joymouse.DisplayInfo
import work.bearbrains.joymouse.input.GestureBuilder
import work.bearbrains.joymouse.input.GestureUtil
import work.bearbrains.joymouse.test.FakeClock
import work.bearbrains.joymouse.test.FakeJoystickCursorState

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
internal class GestureBuilderImplTest {
  private val context: Context = ApplicationProvider.getApplicationContext()
  private val gestureUtil = GestureUtil(ViewConfiguration.get(context), MOCK_MAX_GESTURE_DURATION)
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
    clock.advanceMilliseconds(gestureUtil.longTouchThreshold.inWholeMilliseconds)

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
    clock.advanceMilliseconds(gestureUtil.longTouchThreshold.inWholeMilliseconds)

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

  @Test
  fun endGesture_forDragWithLargeDistance_clampsStrokeDurationToMaxGestureDuration() {
    val builderMock =
      mock<GestureDescription.Builder> {
        on { setDisplayId(anyInt()) } doAnswer Mockito.RETURNS_SELF
      }
    val sut =
      GestureBuilderImpl(
        FakeJoystickCursorState(displayInfo),
        gestureUtil,
        clock,
        { builderMock },
      )
    sut.cursorMove(
      FakeJoystickCursorState(
        displayInfo,
        pointerX = 0f,
        pointerY = 100_000f,
      )
    )
    sut.endGesture(
      FakeJoystickCursorState(
        displayInfo,
        pointerX = 0f,
        pointerY = 100_000f,
      )
    )

    val strokeCaptor = argumentCaptor<GestureDescription.StrokeDescription>()
    verify(builderMock).addStroke(strokeCaptor.capture())
    Truth.assertThat(strokeCaptor.firstValue.duration)
      .isAtMost(MOCK_MAX_GESTURE_DURATION.inWholeMilliseconds)
  }

  @Test
  fun endGesture_forFlingWithLargeDistance_clampsStrokeDurationToMaxGestureDuration() {
    val builderMock =
      mock<GestureDescription.Builder> {
        on { setDisplayId(anyInt()) } doAnswer Mockito.RETURNS_SELF
      }
    val sut =
      GestureBuilderImpl(
        FakeJoystickCursorState(displayInfo),
        gestureUtil,
        clock,
        { builderMock },
      )
    sut.dragIsFling = true
    sut.cursorMove(
      FakeJoystickCursorState(
        displayInfo,
        pointerX = 0f,
        pointerY = 100_000f,
      )
    )
    sut.endGesture(
      FakeJoystickCursorState(
        displayInfo,
        pointerX = 0f,
        pointerY = 100_000f,
      )
    )

    val strokeCaptor = argumentCaptor<GestureDescription.StrokeDescription>()
    verify(builderMock).addStroke(strokeCaptor.capture())
    Truth.assertThat(strokeCaptor.firstValue.duration)
      .isAtMost(MOCK_MAX_GESTURE_DURATION.inWholeMilliseconds)
  }

  @Test
  fun dragIsFling_whenSetToTrue_immediatelyChangesActionToFling() {
    val sut =
      GestureBuilderImpl(
        FakeJoystickCursorState(displayInfo),
        gestureUtil,
        clock,
        gestureDescriptionBuilderProvider,
      )

    Truth.assertThat(sut.action).isEqualTo(GestureBuilder.Action.TOUCH)
    sut.dragIsFling = true
    Truth.assertThat(sut.action).isEqualTo(GestureBuilder.Action.FLING)

    sut.dragIsFling = false
    Truth.assertThat(sut.action).isEqualTo(GestureBuilder.Action.TOUCH)
  }

  @Test
  fun dragIsFling_duringDragMotion_togglesBetweenDragAndFling() {
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
        pointerY = GestureBuilder.MIN_DRAG_DISTANCE,
      )
    )

    Truth.assertThat(sut.action).isEqualTo(GestureBuilder.Action.DRAG)

    sut.dragIsFling = true
    Truth.assertThat(sut.action).isEqualTo(GestureBuilder.Action.FLING)

    sut.dragIsFling = false
    sut.cursorMove(
      FakeJoystickCursorState(
        displayInfo,
        pointerX = 0f,
        pointerY = GestureBuilder.MIN_DRAG_DISTANCE + 10f,
      )
    )
    Truth.assertThat(sut.action).isEqualTo(GestureBuilder.Action.DRAG)
  }

  private companion object {
    val MOCK_MAX_GESTURE_DURATION = 2000.milliseconds
  }
}
