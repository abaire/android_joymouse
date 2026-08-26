package work.bearbrains.joymouse.input.impl

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
import work.bearbrains.joymouse.DisplayInfo
import work.bearbrains.joymouse.input.JoystickAction
import work.bearbrains.joymouse.input.JoystickCursorState
import work.bearbrains.joymouse.test.FakeClock

@RunWith(MockitoJUnitRunner.Silent::class)
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
    lenient().whenever(inputDevice.getMotionRange(anyInt())).thenReturn(motionRange)
    lenient().whenever(motionRange.flat).thenReturn(AXIS_DEADZONE)
    lenient().whenever(motionRange.fuzz).thenReturn(0.05f)
  }

  @Test
  fun update_withLeftTrigger_andNoShift_setsFastCursorEnabled() {
    val sut = create()
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_LTRIGGER)).thenReturn(1f)
    sut.update(motionEvent)

    assertThat(sut.isFastCursorEnabled).isTrue()
  }

  @Test
  fun update_withLeftTriggerReleased_andNoShift_clearsFastCursorEnabled() {
    val sut = create()
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_LTRIGGER)).thenReturn(1f)
    sut.update(motionEvent)

    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_LTRIGGER)).thenReturn(0f)
    sut.update(motionEvent)

    assertThat(sut.isFastCursorEnabled).isFalse()
  }

  @Test
  fun update_withLeftTrigger_andRightShift_setsFastCursorEnabled() {
    val sut = create()
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_LTRIGGER)).thenReturn(1f)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_RTRIGGER)).thenReturn(1f)
    sut.update(motionEvent)

    assertThat(sut.isFastCursorEnabled).isTrue()
  }

  @Test
  fun update_withNegativeHatX_doesNotEmitAction() {
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_HAT_X)).thenReturn(-1f)
    var receivedState: JoystickCursorState? = null
    var receivedAction: JoystickAction? = null
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
    var receivedAction: JoystickAction? = null
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
    assertThat(receivedAction).isEqualTo(JoystickAction.DPAD_LEFT)
  }

  @Test
  fun update_withPositiveHatX_followedByDecreasePastThreshold_emitsAction() {
    var receivedState: JoystickCursorState? = null
    var receivedAction: JoystickAction? = null
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
    assertThat(receivedAction).isEqualTo(JoystickAction.DPAD_RIGHT)
  }

  @Test
  fun sendingToggleChord_clearsIsEnabled() {
    val sut = create()

    sut.handleButtonEvent(true, KeyEvent.KEYCODE_BUTTON_L1)
    sut.handleButtonEvent(true, KeyEvent.KEYCODE_BUTTON_R1)
    sut.handleButtonEvent(true, KeyEvent.KEYCODE_BUTTON_X)

    assertThat(sut.isEnabled).isFalse()
  }

  @Test
  fun releasingToggleChord_remainsDisabled() {
    val sut = create()
    fun changeChord(isPressed: Boolean) {
      sut.handleButtonEvent(isPressed, KeyEvent.KEYCODE_BUTTON_L1)
      sut.handleButtonEvent(isPressed, KeyEvent.KEYCODE_BUTTON_R1)
      sut.handleButtonEvent(isPressed, KeyEvent.KEYCODE_BUTTON_X)
    }
    changeChord(true)

    changeChord(false)

    assertThat(sut.isEnabled).isFalse()
  }

  @Test
  fun sendingToggleChord_aSecondTime_resetsIsEnabled() {
    val sut = create()
    fun changeChord(isPressed: Boolean) {
      println("Setting chord ${isPressed}")
      val axisValue = if (isPressed) 1f else 0f

      whenever(motionEvent.getAxisValue(MotionEvent.AXIS_LTRIGGER)).thenReturn(axisValue)
      whenever(motionEvent.getAxisValue(MotionEvent.AXIS_RTRIGGER)).thenReturn(axisValue)
      sut.update(motionEvent)
      sut.handleButtonEvent(isPressed, KeyEvent.KEYCODE_BUTTON_L1)
      sut.handleButtonEvent(isPressed, KeyEvent.KEYCODE_BUTTON_R1)
    }
    changeChord(true)
    changeChord(false)

    changeChord(true)

    assertThat(sut.isEnabled).isTrue()
  }

  @Test
  fun chordEvents_preventUnchordEvents() {
    // R2 + UP should emit SWIPE_UP, but should not emit DPAD_UP or PRIMARY_PRESS.
    val captor = EventCaptor()
    val sut = create(onAction = captor.capture())

    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_RTRIGGER)).thenReturn(1f)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_HAT_Y)).thenReturn(-1f)
    sut.update(motionEvent)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_HAT_Y)).thenReturn(0f)
    sut.update(motionEvent)

    assertThat(captor.events).contains(
      Pair(sut, JoystickAction.SWIPE_UP)
    )
    assertThat(captor.events).doesNotContain(
      Pair(sut, JoystickAction.DPAD_UP)
    )
  }

  @Test
  fun leftTrigger_andLeftShoulder_emitsCycleDisplayBackward() {
    val captor = EventCaptor()
    val sut = create(onAction = captor.capture())

    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_LTRIGGER)).thenReturn(1f)
    sut.update(motionEvent)

    sut.handleButtonEvent(true, KeyEvent.KEYCODE_BUTTON_L1)
    sut.handleButtonEvent(false, KeyEvent.KEYCODE_BUTTON_L1)

    assertThat(captor.events.map { it.second })
      .containsExactly(
        JoystickAction.FAST_CURSOR_PRESS,
        JoystickAction.CYCLE_DISPLAY_BACKWARD,
      )
  }

  @Test
  fun leftTrigger_andRightShoulder_emitsCycleDisplayForward() {
    val captor = EventCaptor()
    val sut = create(onAction = captor.capture())

    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_LTRIGGER)).thenReturn(1f)
    sut.update(motionEvent)

    sut.handleButtonEvent(true, KeyEvent.KEYCODE_BUTTON_R1)
    sut.handleButtonEvent(false, KeyEvent.KEYCODE_BUTTON_R1)

    assertThat(captor.events.map { it.second })
      .containsExactly(
        JoystickAction.FAST_CURSOR_PRESS,
        JoystickAction.CYCLE_DISPLAY_FORWARD,
      )
  }

  @Test
  fun rightTrigger_callsOnUpdatePrimaryButton() {
    var timesCalled = 0
    var receivedState: JoystickCursorState? = null
    val sut =
      create() { state, action ->
        if (action == JoystickAction.PRIMARY_PRESS) {
          timesCalled += 1
          receivedState = state
        }
      }

    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_RTRIGGER)).thenReturn(1f)
    sut.update(motionEvent)

    assertThat(timesCalled).isEqualTo(1)
    assertThat(receivedState).isEqualTo(sut)
    assertThat(receivedState!!.isPrimaryButtonPressed).isTrue()

    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_RTRIGGER)).thenReturn(WITHIN_AXIS_DEADZONE)
    sut.update(motionEvent)
    assertThat(receivedState!!.isPrimaryButtonPressed).isFalse()
  }

  @Test
  fun rightTrigger_whenReturningToDeadzone_callsOnUpdatePrimaryButton() {
    var timesCalled = 0
    var receivedState: JoystickCursorState? = null
    val sut =
      create() { state, action ->
        if (action == JoystickAction.PRIMARY_RELEASE) {
          timesCalled += 1
          receivedState = state
        }
      }

    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_RTRIGGER)).thenReturn(1f)
    sut.update(motionEvent)

    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_RTRIGGER)).thenReturn(WITHIN_AXIS_DEADZONE)
    sut.update(motionEvent)

    assertThat(timesCalled).isEqualTo(1)
    assertThat(receivedState).isEqualTo(sut)
    assertThat(receivedState!!.isPrimaryButtonPressed).isFalse()
  }

  @Test
  fun buttonA_callsOnUpdatePrimaryButton() {
    var timesCalled = 0
    var receivedState: JoystickCursorState? = null
    val sut =
      create() { state, action ->
        if (action == JoystickAction.PRIMARY_PRESS) {
          timesCalled += 1
          receivedState = state
        }
      }

    sut.handleButtonEvent(true, KeyEvent.KEYCODE_BUTTON_A)

    assertThat(timesCalled).isEqualTo(1)
    assertThat(receivedState).isEqualTo(sut)
    assertThat(receivedState!!.isPrimaryButtonPressed).isTrue()

    sut.handleButtonEvent(false, KeyEvent.KEYCODE_BUTTON_A)
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

    sut.handleButtonEvent(true, KeyEvent.KEYCODE_BUTTON_L2)
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
    verify(handler, times(2)).removeCallbacks(runnableCaptor.capture())
    assertThat(runnableCaptor.allValues.distinct()).hasSize(1)
  }

  @Test
  fun rightStickDeflection_cancelsPriorPendingRepeatRunnable_beforeSchedulingNewOne() {
    val runnableCaptor = argumentCaptor<Runnable>()
    whenever(handler.postDelayed(any(), anyLong())).thenReturn(true)
    val sut = create()

    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_Z)).thenReturn(0.5f)
    sut.update(motionEvent)

    val inOrder = inOrder(handler)
    inOrder.verify(handler).removeCallbacks(runnableCaptor.capture())
    inOrder.verify(handler).postDelayed(runnableCaptor.capture(), anyLong())

    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_Z)).thenReturn(0.8f)
    sut.update(motionEvent)

    inOrder.verify(handler).removeCallbacks(runnableCaptor.capture())
    inOrder.verify(handler).postDelayed(runnableCaptor.capture(), anyLong())

    assertThat(runnableCaptor.allValues.distinct()).hasSize(1)
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
    verify(handler, times(2)).removeCallbacks(runnableCaptor.capture())
    assertThat(runnableCaptor.allValues.distinct()).hasSize(1)
  }

  @Test
  fun create_withMissingMotionRanges_doesNotThrowNpe() {
    val incompleteDevice: InputDevice = mock()
    whenever(incompleteDevice.getMotionRange(anyInt())).thenReturn(null)

    val sut =
      JoystickCursorStateImpl.create(
        incompleteDevice,
        DisplayInfo(
          Display.DEFAULT_DISPLAY,
          context,
          windowWidth = WINDOW_WIDTH,
          windowHeight = WINDOW_HEIGHT,
        ),
        handler,
        xAxis = MotionEvent.AXIS_Z,
        yAxis = MotionEvent.AXIS_RZ,
        nanoClock = nanoClock,
        JoystickButtonProcessorFactoryImpl,
        onUpdatePosition = {},
        onAction = { _, _ -> },
      )

    assertThat(sut).isNotNull()
    assertThat(sut.isEnabled).isTrue()
  }

  @Test
  fun updateDisplayInfo_adjustsCursorPositionRelatively() {
    var updatePositionCalled = 0

    val sut =
      create(
        onUpdatePosition = {
          updatePositionCalled++
        }
      )
    // Initially centered at 500, 250 in 1000x500
    assertThat(sut.pointerX).isEqualTo(500f)
    assertThat(sut.pointerY).isEqualTo(250f)
    assertThat(updatePositionCalled).isEqualTo(0)

    val portraitDisplayInfo =
      DisplayInfo(
        Display.DEFAULT_DISPLAY,
        context,
        windowWidth = 500f,
        windowHeight = 1000f,
      )
    sut.updateDisplayInfo(portraitDisplayInfo)

    // Should now be at relative 0.5, 0.5 in 500x1000 -> 250, 500
    assertThat(sut.pointerX).isEqualTo(250f)
    assertThat(sut.pointerY).isEqualTo(500f)
    assertThat(sut.displayInfo).isEqualTo(portraitDisplayInfo)
    assertThat(updatePositionCalled).isEqualTo(1)
  }

  @Test
  fun updateDisplayInfo_adjustsCursorPositionRelatively_whenMoved() {
    val sut = create()
    // Move cursor on X and Y
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_Z)).thenReturn(0.5f)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_RZ)).thenReturn(0.55f)
    sut.update(motionEvent)
    nanoClock.advanceMilliseconds(100)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_Z)).thenReturn(1f)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_RZ)).thenReturn(0.55f)
    sut.update(motionEvent)

    // Initial position was (500, 250) on (1000x500).
    // Over 100 ms with 1.0 X deflection and 0.5 Y deflection (velocity = 0.5 px/ms):
    // dX = 1.0 * 100 * 0.5 = +50 px -> pointerX = 550f (55%)
    // dY = 0.5 * 100 * 0.5 = +25 px -> pointerY = 275f (55%)
    assertThat(sut.pointerX).isEqualTo(550f)
    assertThat(sut.pointerY).isEqualTo(275f)

    val portraitDisplayInfo =
      DisplayInfo(
        Display.DEFAULT_DISPLAY,
        context,
        windowWidth = 500f,
        windowHeight = 1000f,
      )
    sut.updateDisplayInfo(portraitDisplayInfo)

    // 55% of 500 is 275, 55% of 1000 is 550
    assertThat(sut.pointerX).isEqualTo(275f)
    assertThat(sut.pointerY).isEqualTo(550f)
  }

  @Test
  fun updateDisplayInfo_updatesBoundsForSubsequentMotion() {
    val sut = create()

    val portraitDisplayInfo =
      DisplayInfo(
        Display.DEFAULT_DISPLAY,
        context,
        windowWidth = 500f,
        windowHeight = 1000f,
      )
    sut.updateDisplayInfo(portraitDisplayInfo)

    // Deflect downwards in portrait mode (velocity is 500px/s = 0.5px/ms)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_RZ)).thenReturn(0.5f)
    sut.update(motionEvent)
    // Advance 100ms with 1.0 deflection: dY = 1.0 * 100 * 0.5 = +50px
    nanoClock.advanceMilliseconds(100)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_RZ)).thenReturn(1f)
    sut.update(motionEvent)

    // pointerY started at 500f in portrait (500x1000), moving downwards moves it to 550f (> 500f landscape height)
    assertThat(sut.pointerY).isEqualTo(550f)
    assertThat(sut.pointerY).isGreaterThan(500f)
    assertThat(sut.pointerY).isAtMost(1000f)
  }




  @Test
  fun updateDisplayInfo_withSameDisplayInfo_doesNotEmitUpdate() {
    var updatePositionCalled = 0
    val displayInfo =
      DisplayInfo(
        Display.DEFAULT_DISPLAY,
        context,
        windowWidth = WINDOW_WIDTH,
        windowHeight = WINDOW_HEIGHT,
      )
    val sut =
      create(
        displayInfo = displayInfo,
        onUpdatePosition = {
          updatePositionCalled++
        }
      )

    sut.updateDisplayInfo(displayInfo)

    assertThat(updatePositionCalled).isEqualTo(0)
  }

  @Test
  fun updateActionConfig_withLeftStick_rebindsAxesToLeftStick() {
    val sut = create()

    // Initially, right stick (AXIS_Z) moves cursor
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_Z)).thenReturn(0.5f)
    sut.update(motionEvent)
    nanoClock.advanceMilliseconds(100)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_Z)).thenReturn(1f)
    sut.update(motionEvent)
    val afterRightStickX = sut.pointerX
    assertThat(afterRightStickX).isGreaterThan(WINDOW_WIDTH * 0.5f)

    // Reconfigure to LEFT_STICK (AXIS_X, AXIS_Y)
    sut.updateActionConfig(work.bearbrains.joymouse.input.ActionConfig(mouseStick = work.bearbrains.joymouse.input.MouseStick.LEFT_STICK))

    // Right stick (AXIS_Z) should now do nothing
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_Z)).thenReturn(1f)
    sut.update(motionEvent)
    nanoClock.advanceMilliseconds(100)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_Z)).thenReturn(1f)
    sut.update(motionEvent)
    assertThat(sut.pointerX).isEqualTo(afterRightStickX)

    // Left stick (AXIS_X) should now move cursor
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_X)).thenReturn(0.5f)
    sut.update(motionEvent)
    nanoClock.advanceMilliseconds(100)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_X)).thenReturn(1f)
    sut.update(motionEvent)
    assertThat(sut.pointerX).isGreaterThan(afterRightStickX)
  }

  @Test
  fun updateActionConfig_withInvertedAxes_invertsMovement() {
    val sut = create()
    val initialX = sut.pointerX
    val initialY = sut.pointerY

    sut.updateActionConfig(work.bearbrains.joymouse.input.ActionConfig(invertX = true, invertY = true))

    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_Z)).thenReturn(0.5f)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_RZ)).thenReturn(0.5f)
    sut.update(motionEvent)
    nanoClock.advanceMilliseconds(100)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_Z)).thenReturn(1f)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_RZ)).thenReturn(1f)
    sut.update(motionEvent)

    assertThat(sut.pointerX).isLessThan(initialX)
    assertThat(sut.pointerY).isLessThan(initialY)
  }

  @Test
  fun updateActionConfig_withCustomCursorSpeed_scalesMovementVelocity() {
    val sut = create()
    val initialX = sut.pointerX

    // 2.0x normal speed
    sut.updateActionConfig(work.bearbrains.joymouse.input.ActionConfig(cursorSpeed = 2.0f))

    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_Z)).thenReturn(0.5f)
    sut.update(motionEvent)
    // 100ms with 1.0 deflection at 2.0x velocity (0.5 px/ms * 2.0 = 1.0 px/ms -> +100px)
    nanoClock.advanceMilliseconds(100)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_Z)).thenReturn(1f)
    sut.update(motionEvent)

    assertThat(sut.pointerX).isEqualTo(initialX + 100f)
  }

  @Test
  fun update_withFastCursor_andCustomFastCursorSpeed_scalesMovementVelocity() {
    val sut = create()
    val initialX = sut.pointerX

    // 4.0x fast cursor speed
    sut.updateActionConfig(work.bearbrains.joymouse.input.ActionConfig(fastCursorSpeed = 4.0f))

    // Enable fast cursor via LTRIGGER
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_LTRIGGER)).thenReturn(1f)
    sut.update(motionEvent)

    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_Z)).thenReturn(0.5f)
    sut.update(motionEvent)
    // 100ms with 1.0 deflection at 4.0x velocity (0.5 px/ms * 4.0 = 2.0 px/ms -> +200px)
    nanoClock.advanceMilliseconds(100)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_Z)).thenReturn(1f)
    sut.update(motionEvent)

    assertThat(sut.pointerX).isEqualTo(initialX + 200f)
  }

  @Test
  fun updateActionConfig_withCustomDeadzone_updatesDeadzone() {
    val sut = create()
    val initialX = sut.pointerX

    // 0.3f deadzone
    sut.updateActionConfig(work.bearbrains.joymouse.input.ActionConfig(deadzone = 0.3f))

    // 0.15f and 0.25f are below the 0.3f deadzone -> no movement
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_Z)).thenReturn(0.15f)
    sut.update(motionEvent)
    nanoClock.advanceMilliseconds(100)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_Z)).thenReturn(0.25f)
    sut.update(motionEvent)
    assertThat(sut.pointerX).isEqualTo(initialX)

    // 0.5f and 1.0f are above the 0.3f deadzone -> moves
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_Z)).thenReturn(0.5f)
    sut.update(motionEvent)
    nanoClock.advanceMilliseconds(100)
    whenever(motionEvent.getAxisValue(MotionEvent.AXIS_Z)).thenReturn(1.0f)
    sut.update(motionEvent)
    assertThat(sut.pointerX).isGreaterThan(initialX)
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
    onAction: (JoystickCursorState, JoystickAction) -> Unit = { _, _ -> },
  ): JoystickCursorStateImpl {
    return JoystickCursorStateImpl.create(
      inputDevice,
      displayInfo,
      handler,
      xAxis = MotionEvent.AXIS_Z,
      yAxis = MotionEvent.AXIS_RZ,
      nanoClock = nanoClock,
      JoystickButtonProcessorFactoryImpl,
      onUpdatePosition = onUpdatePosition,
      onAction = onAction,
    ) as JoystickCursorStateImpl
  }

  private class EventCaptor {
    val events = mutableListOf<Pair<JoystickCursorState, JoystickAction>>()

    val size: Int
      get() = events.size

    fun capture(): (JoystickCursorState, JoystickAction) -> Unit = { processor, action ->
      events.add(Pair(processor, action))
    }

    fun containsExactly(vararg events: Pair<JoystickCursorState, JoystickAction>): Boolean {
      return this.events == events
    }

    fun reset() {
      events.clear()
    }
  }

  private companion object {
    const val WINDOW_WIDTH = 1000f
    const val WINDOW_HEIGHT = 500f

    const val AXIS_DEADZONE = 0.1f
    const val EPSILON = 0.001f
    const val WITHIN_AXIS_DEADZONE = AXIS_DEADZONE - EPSILON
  }
}
