package work.bearbrains.joymouse.input

/** Converts button press/release events into logical actions. */
interface JoystickButtonProcessor {

  /**
   * [KeyEvent] `KEYCODE_BUTTON_` constant indicating a button that should be used to cause other
   * buttons to send alternate actions.
   */
  var shiftButton: Int

  /** Whether the shift button is currently depressed. */
  val isShifted: Boolean

  /**
   * Reports a state change in a physical or logical button.
   *
   * [buttonId] should be a `KEYCODE_BUTTON_*` constant from [KeyEvent].
   */
  fun handleButtonEvent(buttonId: Int, isPressed: Boolean)

  /** Clears all button states without triggering any release actions. */
  fun reset()

  /** Factory interface that may be used to construct a JoystickButtonProcessor. */
  interface Factory {
    fun create(onAction: (JoystickButtonProcessor, JoystickAction) -> Unit): JoystickButtonProcessor
  }
}
