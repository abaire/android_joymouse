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

    fun mapping(
      button: VirtualButton,
      onPress: JoystickAction? = null,
      onRelease: JoystickAction? = null,
    ): JoystickButtonProcessorImpl.ButtonMapping =
      JoystickButtonProcessorImpl.ButtonMapping(button, onPress = onPress, onRelease = onRelease)

    val unshifted =
      setOf(
        mapping(basicButton(KeyEvent.KEYCODE_BUTTON_MODE), onRelease = JoystickAction.HOME),
        mapping(basicButton(KeyEvent.KEYCODE_BUTTON_START), onRelease = JoystickAction.RECENTS),
        mapping(basicButton(KeyEvent.KEYCODE_DPAD_UP), onRelease = JoystickAction.DPAD_UP),
        mapping(basicButton(KeyEvent.KEYCODE_DPAD_DOWN), onRelease = JoystickAction.DPAD_DOWN),
        mapping(basicButton(KeyEvent.KEYCODE_DPAD_LEFT), onRelease = JoystickAction.DPAD_LEFT),
        mapping(basicButton(KeyEvent.KEYCODE_DPAD_RIGHT), onRelease = JoystickAction.DPAD_RIGHT),
        mapping(basicButton(KeyEvent.KEYCODE_BUTTON_A), onRelease = JoystickAction.ACTIVATE),
        mapping(
          multiplexedButton(KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_B),
          onRelease = JoystickAction.BACK
        ),
        mapping(
          basicButton(KeyEvent.KEYCODE_BUTTON_R2),
          onPress = JoystickAction.PRIMARY_PRESS,
          onRelease = JoystickAction.PRIMARY_RELEASE
        ),
        mapping(
          basicButton(KeyEvent.KEYCODE_BUTTON_L2),
          onPress = JoystickAction.FAST_CURSOR_PRESS,
          onRelease = JoystickAction.FAST_CURSOR_RELEASE
        ),
      )

    // Left shift buttons
    val leftShifted =
      setOf(
        mapping(
          chordButton(KeyEvent.KEYCODE_BUTTON_L1),
          onRelease = JoystickAction.CYCLE_DISPLAY_BACKWARD
        ),
        mapping(
          chordButton(KeyEvent.KEYCODE_BUTTON_R1),
          onRelease = JoystickAction.CYCLE_DISPLAY_FORWARD
        ),
      )

    val rightShifted =
      setOf(
        // Right shift buttons
        mapping(
          chordButton(
            KeyEvent.KEYCODE_BUTTON_A,
          ),
          onRelease = JoystickAction.TOGGLE_GESTURE
        ),
        mapping(basicButton(KeyEvent.KEYCODE_DPAD_UP), onRelease = JoystickAction.SWIPE_UP),
        mapping(basicButton(KeyEvent.KEYCODE_DPAD_DOWN), onRelease = JoystickAction.SWIPE_DOWN),
        mapping(basicButton(KeyEvent.KEYCODE_DPAD_LEFT), onRelease = JoystickAction.SWIPE_LEFT),
        mapping(basicButton(KeyEvent.KEYCODE_DPAD_RIGHT), onRelease = JoystickAction.SWIPE_RIGHT),
      )

    return JoystickButtonProcessorImpl(
      toggleChord =
        setOf(
          KeyEvent.KEYCODE_BUTTON_L1,
          KeyEvent.KEYCODE_BUTTON_R1,
          KeyEvent.KEYCODE_BUTTON_X,
        ),
      unshiftedButtons = unshifted,
      leftShiftButtons = leftShifted,
      rightShiftButtons = rightShifted,
      rawButtons =
        setOf(
          JoystickButtonProcessorImpl.RawButtonMapping(
            basicButton(KeyEvent.KEYCODE_BUTTON_L2),
            onPress = JoystickAction.FAST_CURSOR_PRESS,
            onRelease = JoystickAction.FAST_CURSOR_RELEASE,
          ),
          JoystickButtonProcessorImpl.RawButtonMapping(
            basicButton(KeyEvent.KEYCODE_BUTTON_R2),
            onPress = JoystickAction.PRIMARY_PRESS,
            onRelease = JoystickAction.PRIMARY_RELEASE,
          ),
        ),
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
