package work.bearbrains.joymouse.input.impl

import android.view.KeyEvent
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import work.bearbrains.joymouse.input.JoystickAction
import work.bearbrains.joymouse.input.JoystickButtonProcessor

internal class JoystickButtonProcessorFactoryImplTest {

  @Test
  fun unshifted_buttonA_emitsPrimaryPressAndRelease() {
    val events = mutableListOf<JoystickAction>()
    val sut = JoystickButtonProcessorFactoryImpl.create { _, action -> events.add(action) }

    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, false)

    assertThat(events).containsExactly(
      JoystickAction.PRIMARY_PRESS,
      JoystickAction.PRIMARY_RELEASE
    ).inOrder()
  }

  @Test
  fun unshifted_buttonR2_emitsPrimaryPressAndRelease() {
    val events = mutableListOf<JoystickAction>()
    val sut = JoystickButtonProcessorFactoryImpl.create { _, action -> events.add(action) }

    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_R2, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_R2, false)

    assertThat(events).containsExactly(
      JoystickAction.PRIMARY_PRESS,
      JoystickAction.PRIMARY_RELEASE
    ).inOrder()
  }

  @Test
  fun shifted_r2WithDpadUp_emitsSwipeUpWithoutPrimaryPressOrRelease() {
    val events = mutableListOf<JoystickAction>()
    val sut = JoystickButtonProcessorFactoryImpl.create { _, action -> events.add(action) }

    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_R2, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_DPAD_UP, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_DPAD_UP, false)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_R2, false)

    assertThat(events).containsExactly(
      JoystickAction.SWIPE_UP
    )
  }

  @Test
  fun shifted_r2WithButtonA_emitsToggleGestureWithoutPrimaryPressOrRelease() {
    val events = mutableListOf<JoystickAction>()
    val sut = JoystickButtonProcessorFactoryImpl.create { _, action -> events.add(action) }

    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_R2, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, false)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_R2, false)

    assertThat(events).containsExactly(
      JoystickAction.TOGGLE_GESTURE
    )
  }

  @Test
  fun unshifted_dpadUp_emitsDpadUp() {
    val events = mutableListOf<JoystickAction>()
    val sut = JoystickButtonProcessorFactoryImpl.create { _, action -> events.add(action) }

    sut.handleButtonEvent(KeyEvent.KEYCODE_DPAD_UP, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_DPAD_UP, false)

    assertThat(events).containsExactly(
      JoystickAction.DPAD_UP
    )
  }
}
