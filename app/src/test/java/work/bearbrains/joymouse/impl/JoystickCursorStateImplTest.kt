import android.content.Context
import android.os.Handler
import android.view.Display
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import work.bearbrains.joymouse.ButtonAxis
import work.bearbrains.joymouse.DisplayInfo
import work.bearbrains.joymouse.JoystickCursorState
import work.bearbrains.joymouse.JoystickCursorStateImpl
import work.bearbrains.joymouse.test.FakeClock

@RunWith(MockitoJUnitRunner::class)
@Config(manifest = Config.NONE)
internal class JoystickCursorStateImplTest {

  private val context: Context = mock()
  private val handler: Handler = mock()
  private val motionEvent: MotionEvent = mock()
  private val inputDevice: InputDevice = mock()
  private val motionRange: InputDevice.MotionRange = mock()

  private val nanoClock = FakeClock()

  @Before
  fun setUp() {
    whenever(inputDevice.getMotionRange(anyInt())).thenReturn(motionRange)
    whenever(motionRange.flat).thenReturn(AXIS_DEADZONE)
    whenever(motionRange.fuzz).thenReturn(0.05f)
  }

  @Test
  fun update_withLeftTrigger_setsFastCursorEnabled() {
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_LTRIGGER)).thenReturn(1f)
    val sut = create()

    sut.update(motionEvent)

    assertThat(sut.isFastCursorEnabled).isTrue()
  }

  @Test
  fun update_withNegativeHatX_doesNotEmitAction() {
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_HAT_X)).thenReturn(-1f)
    var receivedState: JoystickCursorState? = null
    var receivedAction: JoystickCursorState.Action? = null
    val sut =
      create(
        onAction = { state, action ->
          receivedState = state
          receivedAction = action
        }
      )

    sut.update(motionEvent)

    assertThat(receivedState).isNull()
    assertThat(receivedAction).isNull()
  }

  @Test
  fun update_withNegativeHatX_followedByIncreasePastThreshold_emitsAction() {
    var receivedState: JoystickCursorState? = null
    var receivedAction: JoystickCursorState.Action? = null
    val sut =
      create(
        onAction = { state, action ->
          receivedState = state
          receivedAction = action
        }
      )

    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_HAT_X)).thenReturn(-1f)
    sut.update(motionEvent)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_HAT_X)).thenReturn(0f)
    sut.update(motionEvent)

    assertThat(receivedState).isEqualTo(sut)
    assertThat(receivedAction).isEqualTo(JoystickCursorState.Action.DPAD_LEFT)
  }

  @Test
  fun update_withPositiveHatX_followedByDecreasePastThreshold_emitsAction() {
    var receivedState: JoystickCursorState? = null
    var receivedAction: JoystickCursorState.Action? = null
    val sut =
      create(
        onAction = { state, action ->
          receivedState = state
          receivedAction = action
        }
      )

    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_HAT_X)).thenReturn(1f)
    sut.update(motionEvent)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_HAT_X))
      .thenReturn(ButtonAxis.TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD - EPSILON)
    sut.update(motionEvent)

    assertThat(receivedState).isEqualTo(sut)
    assertThat(receivedAction).isEqualTo(JoystickCursorState.Action.DPAD_RIGHT)
  }

  @Test
  fun sendingToggleChord_clearsIsEnabled() {
    val sut = create()

    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_LTRIGGER)).thenReturn(1f)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_RTRIGGER)).thenReturn(1f)
    sut.update(motionEvent)
    sut.handleButtonEvent(true, KeyEvent.KEYCODE_BUTTON_L1)
    sut.handleButtonEvent(true, KeyEvent.KEYCODE_BUTTON_R1)

    assertThat(sut.isEnabled).isFalse()
  }

  @Test
  fun sendingToggleChord_aSecondTime_resetsIsEnabled() {
    val sut = create()
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_LTRIGGER)).thenReturn(1f)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_RTRIGGER)).thenReturn(1f)
    sut.update(motionEvent)
    sut.handleButtonEvent(true, KeyEvent.KEYCODE_BUTTON_L1)
    sut.handleButtonEvent(true, KeyEvent.KEYCODE_BUTTON_R1)

    sut.handleButtonEvent(false, KeyEvent.KEYCODE_BUTTON_L1)
    sut.handleButtonEvent(true, KeyEvent.KEYCODE_BUTTON_L1)

    assertThat(sut.isEnabled).isTrue()
  }

  @Test
  fun buttonAxis_withLatchUntilZero_isPressed_whenRaisingAboveThreshold() {
    val sut = create()
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_LTRIGGER))
      .thenReturn(ButtonAxis.TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD)
    sut.update(motionEvent)
    assertThat(sut.buttonStates[KeyEvent.KEYCODE_BUTTON_L2]).isTrue()
  }

  @Test
  fun buttonAxis_withLatchUntilZero_remainsPressed_whenFallingBelowPressThreshold() {
    val sut = create()
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_LTRIGGER)).thenReturn(1f)
    sut.update(motionEvent)

    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_LTRIGGER)).thenReturn(AXIS_DEADZONE)
    sut.update(motionEvent)

    assertThat(sut.buttonStates[KeyEvent.KEYCODE_BUTTON_L2]).isTrue()
  }

  @Test
  fun buttonAxis_withLatchUntilZero_isReleased_whenReturningToDeadzone() {
    val sut = create()
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_LTRIGGER)).thenReturn(1f)
    sut.update(motionEvent)

    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_LTRIGGER)).thenReturn(WITHIN_AXIS_DEADZONE)
    sut.update(motionEvent)

    assertThat(sut.buttonStates[KeyEvent.KEYCODE_BUTTON_L2]).isFalse()
  }

  @Test
  fun chordEvents_preventUnchordEvents() {
    // L2 + UP should emit SWIPE_UP, but should not emit DPAD_UP.
    var timesCalled = 0
    var receivedState: JoystickCursorState? = null
    var receivedAction: JoystickCursorState.Action? = null
    val sut =
      create(
        onAction = { state, action ->
          timesCalled += 1
          receivedState = state
          receivedAction = action
        }
      )

    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_LTRIGGER)).thenReturn(1f)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_HAT_Y)).thenReturn(-1f)
    sut.update(motionEvent)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_HAT_Y)).thenReturn(0f)
    sut.update(motionEvent)

    assertThat(timesCalled).isEqualTo(1)
    assertThat(receivedState).isEqualTo(sut)
    assertThat(receivedAction).isEqualTo(JoystickCursorState.Action.SWIPE_UP)
  }

  @Test
  fun rightTrigger_callsOnUpdatePrimaryButton() {
    var timesCalled = 0
    var receivedState: JoystickCursorState? = null
    val sut =
      create(
        onUpdatePrimaryButton = { state ->
          timesCalled += 1
          receivedState = state
        }
      )

    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_RTRIGGER)).thenReturn(1f)
    sut.update(motionEvent)

    assertThat(timesCalled).isEqualTo(1)
    assertThat(receivedState).isEqualTo(sut)
    assertThat(receivedState!!.isPrimaryButtonPressed).isTrue()
  }

  @Test
  fun rightTrigger_whenReturningToDeadzone_callsOnUpdatePrimaryButton() {
    var timesCalled = 0
    var receivedState: JoystickCursorState? = null
    val sut =
      create(
        onUpdatePrimaryButton = { state ->
          timesCalled += 1
          receivedState = state
        }
      )

    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_RTRIGGER)).thenReturn(1f)
    sut.update(motionEvent)

    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_RTRIGGER)).thenReturn(WITHIN_AXIS_DEADZONE)
    sut.update(motionEvent)

    assertThat(timesCalled).isEqualTo(2)
    assertThat(receivedState).isEqualTo(sut)
    assertThat(receivedState!!.isPrimaryButtonPressed).isFalse()
  }

  @Test
  fun rightStickDeflection_callsOnUpdatePosition() {
    var timesCalled = 0
    var receivedState: JoystickCursorState? = null
    val sut =
      create(
        onUpdatePosition = { state ->
          timesCalled += 1
          receivedState = state
        }
      )

    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_Z)).thenReturn(0.5f)
    sut.update(motionEvent)
    nanoClock.advanceMilliseconds(1)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_Z)).thenReturn(1f)
    sut.update(motionEvent)

    assertThat(timesCalled).isEqualTo(1)
    assertThat(receivedState).isEqualTo(sut)
    assertThat(receivedState!!.pointerX).isEqualTo(500.5f)
    assertThat(receivedState!!.pointerY).isEqualTo(WINDOW_HEIGHT * 0.5f)
  }

  @Test
  fun rightStickDeflection_withFastCursorEnabled_movesCursorFurther() {
    var timesCalled = 0
    var receivedState: JoystickCursorState? = null
    val sut =
      create(
        onUpdatePosition = { state ->
          timesCalled += 1
          receivedState = state
        }
      )

    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_LTRIGGER)).thenReturn(1f)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_Z)).thenReturn(0.5f)
    sut.update(motionEvent)
    nanoClock.advanceMilliseconds(1)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_Z)).thenReturn(1f)
    sut.update(motionEvent)

    assertThat(timesCalled).isEqualTo(1)
    assertThat(receivedState).isEqualTo(sut)
    assertThat(receivedState!!.pointerX).isEqualTo(501.0f)
    assertThat(receivedState!!.pointerY).isEqualTo(WINDOW_HEIGHT * 0.5f)
  }

  @Test
  fun rightStickDeflection_registersRepeatRunnable() {
    val runnableCaptor = argumentCaptor<Runnable>()
    whenever(handler.postDelayed(any(), anyLong())).thenReturn(true)
    val sut = create()

    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_Z)).thenReturn(0.5f)
    sut.update(motionEvent)

    verify(handler).postDelayed(runnableCaptor.capture(), anyLong())
  }

  @Test
  fun rightStickDeflection_removesRepeatRunnable_whenReturningToDeadzone() {
    val runnableCaptor = argumentCaptor<Runnable>()
    whenever(handler.postDelayed(any(), anyLong())).thenReturn(true)
    val sut = create()

    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_Z)).thenReturn(1f)
    sut.update(motionEvent)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_Z)).thenReturn(WITHIN_AXIS_DEADZONE)
    sut.update(motionEvent)

    verify(handler).postDelayed(runnableCaptor.capture(), anyLong())
    verify(handler).removeCallbacks(runnableCaptor.capture())
    assertThat(runnableCaptor.firstValue).isEqualTo(runnableCaptor.lastValue)
  }

  @Test
  fun repeatRunnable_movesCursor() {
    val runnableCaptor = argumentCaptor<Runnable>()
    whenever(handler.postDelayed(any(), anyLong())).thenReturn(true)
    var timesCalled = 0
    var receivedState: JoystickCursorState? = null
    val sut =
      create(
        onUpdatePosition = { state ->
          timesCalled += 1
          receivedState = state
        }
      )

    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_Z)).thenReturn(1f)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_RZ)).thenReturn(-1f)
    sut.update(motionEvent)

    verify(handler).postDelayed(runnableCaptor.capture(), anyLong())
    assertThat(timesCalled).isEqualTo(0)
    assertThat(receivedState).isNull()
    assertThat(sut.pointerX).isEqualTo(WINDOW_WIDTH * 0.5f)
    assertThat(sut.pointerY).isEqualTo(WINDOW_HEIGHT * 0.5f)

    nanoClock.advanceMilliseconds(1)
    runnableCaptor.firstValue.run()
    assertThat(timesCalled).isEqualTo(1)
    assertThat(receivedState).isEqualTo(sut)
    assertThat(sut.pointerX).isEqualTo((WINDOW_WIDTH * 0.5f) + 0.5f)
    assertThat(sut.pointerY).isEqualTo((WINDOW_HEIGHT * 0.5f) - 0.5f)
  }

  @Test
  fun cancelRepeater_removesRepeatRunnable() {
    val runnableCaptor = argumentCaptor<Runnable>()
    whenever(handler.postDelayed(any(), anyLong())).thenReturn(true)
    val sut = create()

    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_Z)).thenReturn(1f)
    sut.update(motionEvent)
    sut.cancelRepeater()

    verify(handler).postDelayed(runnableCaptor.capture(), anyLong())
    verify(handler).removeCallbacks(runnableCaptor.capture())
    assertThat(runnableCaptor.firstValue).isEqualTo(runnableCaptor.lastValue)
  }

  private fun create(
    displayInfo: DisplayInfo =
      DisplayInfo(
        Display.DEFAULT_DISPLAY,
        context,
        windowWidth = WINDOW_WIDTH,
        windowHeight = WINDOW_HEIGHT,
      ),
    onUpdatePosition: (JoystickCursorState) -> Unit = {},
    onUpdatePrimaryButton: (JoystickCursorState) -> Unit = {},
    onAction: (JoystickCursorState, JoystickCursorState.Action) -> Unit = { _, _ -> },
    onEnableChanged: (JoystickCursorState) -> Unit = {},
  ): JoystickCursorStateImpl {
    return JoystickCursorStateImpl.create(
      inputDevice,
      displayInfo,
      handler,
      xAxis = MotionEvent.AXIS_Z,
      yAxis = MotionEvent.AXIS_RZ,
      nanoClock = nanoClock,
      onUpdatePosition = onUpdatePosition,
      onUpdatePrimaryButton = onUpdatePrimaryButton,
      onAction = onAction,
      onEnableChanged = onEnableChanged,
    ) as JoystickCursorStateImpl
  }

  private companion object {
    const val WINDOW_WIDTH = 1000f
    const val WINDOW_HEIGHT = 500f

    const val AXIS_DEADZONE = 0.1f
    const val EPSILON = 0.001f
    const val WITHIN_AXIS_DEADZONE = AXIS_DEADZONE - EPSILON
  }
}
