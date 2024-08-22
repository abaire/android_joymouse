package work.bearbrains.joymouse.input.impl

import android.view.KeyEvent
import work.bearbrains.joymouse.input.JoystickAction
import work.bearbrains.joymouse.input.JoystickButtonProcessor

/**
 * Default [JoystickButtonProcessor.Factory] implementation that produces
 * [JoystickButtonProcessorImpl] instances.
 */
object JoystickButtonProcessorFactoryImpl : JoystickButtonProcessor.Factory {

  override fun create(
    onAction: (JoystickButtonProcessor, JoystickAction) -> Unit
  ): JoystickButtonProcessor {

    // Chord used to toggle the joystick mouse on/off.
    val toggleChord =
      VirtualButton(
        setOf(
          KeyEvent.KEYCODE_BUTTON_L1,
          KeyEvent.KEYCODE_BUTTON_L2,
          KeyEvent.KEYCODE_BUTTON_R1,
          KeyEvent.KEYCODE_BUTTON_R2,
        ),
        isChord = true,
      )

    val basicActions =
      listOf(
        buttonMapping(basicButton(KeyEvent.KEYCODE_BUTTON_MODE), onRelease = JoystickAction.HOME),
        buttonMapping(
          basicButton(KeyEvent.KEYCODE_BUTTON_START),
          onRelease = JoystickAction.RECENTS
        ),
        buttonMapping(basicButton(KeyEvent.KEYCODE_DPAD_UP), onRelease = JoystickAction.DPAD_UP),
        buttonMapping(
          basicButton(KeyEvent.KEYCODE_DPAD_DOWN),
          onRelease = JoystickAction.DPAD_DOWN
        ),
        buttonMapping(
          basicButton(KeyEvent.KEYCODE_DPAD_LEFT),
          onRelease = JoystickAction.DPAD_LEFT
        ),
        buttonMapping(
          basicButton(KeyEvent.KEYCODE_DPAD_RIGHT),
          onRelease = JoystickAction.DPAD_RIGHT
        ),
        buttonMapping(basicButton(KeyEvent.KEYCODE_BUTTON_A), onRelease = JoystickAction.ACTIVATE),
        buttonMapping(
          multiplexedButton(KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_B),
          onRelease = JoystickAction.BACK
        ),
        buttonMapping(
          basicButton(KeyEvent.KEYCODE_BUTTON_R2),
          onPress = JoystickAction.PRIMARY_PRESS,
          onRelease = JoystickAction.PRIMARY_RELEASE
        ),
        buttonMapping(
          basicButton(KeyEvent.KEYCODE_BUTTON_L1),
          onPress = JoystickAction.FAST_CURSOR_PRESS,
          onRelease = JoystickAction.FAST_CURSOR_RELEASE
        ),
      )

    val shiftedActions =
      listOf(
        buttonMapping(
          chordButton(
            KeyEvent.KEYCODE_BUTTON_L1,
            KeyEvent.KEYCODE_BUTTON_R1,
            KeyEvent.KEYCODE_BUTTON_R2,
          ),
          onPress = JoystickAction.TOGGLE_ENABLED
        ),
        buttonMapping(
          chordButton(
            KeyEvent.KEYCODE_BUTTON_A,
          ),
          onRelease = JoystickAction.TOGGLE_GESTURE
        ),
        buttonMapping(
          chordButton(KeyEvent.KEYCODE_BUTTON_L1),
          onRelease = JoystickAction.CYCLE_DISPLAY_BACKWARD
        ),
        buttonMapping(
          chordButton(KeyEvent.KEYCODE_BUTTON_R1),
          onRelease = JoystickAction.CYCLE_DISPLAY_FORWARD
        ),
        buttonMapping(basicButton(KeyEvent.KEYCODE_DPAD_UP), onRelease = JoystickAction.SWIPE_UP),
        buttonMapping(
          basicButton(KeyEvent.KEYCODE_DPAD_DOWN),
          onRelease = JoystickAction.SWIPE_DOWN
        ),
        buttonMapping(
          basicButton(KeyEvent.KEYCODE_DPAD_LEFT),
          onRelease = JoystickAction.SWIPE_LEFT
        ),
        buttonMapping(
          basicButton(KeyEvent.KEYCODE_DPAD_RIGHT),
          onRelease = JoystickAction.SWIPE_RIGHT
        ),
        buttonMapping(
          basicButton(KeyEvent.KEYCODE_BUTTON_R2),
          onPress = JoystickAction.PRIMARY_PRESS,
          onRelease = JoystickAction.PRIMARY_RELEASE
        ),
      )

    return JoystickButtonProcessorImpl(
      basicButtons = basicActions,
      shiftButtons = shiftedActions,
      onAction = onAction
    )
  }

  private fun basicButton(buttonId: Int): VirtualButton =
    VirtualButton(setOf(buttonId), isChord = false)

  private fun multiplexedButton(vararg buttonIds: Int): VirtualButton =
    VirtualButton(buttonIds.toSet(), isChord = false)

  private fun chordButton(vararg buttonIds: Int): VirtualButton =
    VirtualButton(buttonIds.toSet(), isChord = true)
}
