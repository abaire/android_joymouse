package work.bearbrains.joymouse.input.impl

import android.view.KeyEvent
import work.bearbrains.joymouse.input.JoystickAction
import work.bearbrains.joymouse.input.JoystickButtonProcessor

typealias ButtonMapping = Pair<VirtualButton, JoystickButtonProcessorImpl.ActionSet>

/** Constructs a new [ButtonMapping] instance. */
fun buttonMapping(
  button: VirtualButton,
  onPress: JoystickAction? = null,
  onRelease: JoystickAction? = null,
): ButtonMapping =
  ButtonMapping(
    button,
    JoystickButtonProcessorImpl.ActionSet(onPress = onPress, onRelease = onRelease)
  )

/** Concrete implementation of [JoystickButtonProcessor]. */
class JoystickButtonProcessorImpl(
  private val basicButtons: List<ButtonMapping> = emptyList(),
  private val shiftButtons: List<ButtonMapping> = emptyList(),
  override var shiftButton: Int = KeyEvent.KEYCODE_BUTTON_L2,
  private val onAction: (JoystickButtonProcessor, JoystickAction) -> Unit,
) : JoystickButtonProcessor {

  class ActionSet(val onPress: JoystickAction?, val onRelease: JoystickAction?)

  private val buttonStates =
    mutableMapOf(
      KeyEvent.KEYCODE_BUTTON_A to false,
      KeyEvent.KEYCODE_BUTTON_B to false,
      KeyEvent.KEYCODE_BUTTON_X to false,
      KeyEvent.KEYCODE_BUTTON_Y to false,
      KeyEvent.KEYCODE_BUTTON_L1 to false,
      KeyEvent.KEYCODE_BUTTON_L2 to false,
      KeyEvent.KEYCODE_BUTTON_R1 to false,
      KeyEvent.KEYCODE_BUTTON_R2 to false,
      KeyEvent.KEYCODE_BUTTON_SELECT to false,
      KeyEvent.KEYCODE_BUTTON_START to false,
      KeyEvent.KEYCODE_BUTTON_THUMBL to false,
      KeyEvent.KEYCODE_BUTTON_THUMBR to false,
      KeyEvent.KEYCODE_MEDIA_RECORD to false,
      KeyEvent.KEYCODE_BUTTON_MODE to false, // Xbox button
      KeyEvent.KEYCODE_DPAD_LEFT to false,
      KeyEvent.KEYCODE_DPAD_RIGHT to false,
      KeyEvent.KEYCODE_DPAD_UP to false,
      KeyEvent.KEYCODE_DPAD_DOWN to false,
    )

  /** Map of button IDs to [ButtonMapping] instances that were triggered when they were pressed. */
  private val buttonLatches = mutableMapOf<Int, ButtonMapping>()

  override val isShifted: Boolean
    get() = buttonStates.getOrDefault(shiftButton, false)

  override fun handleButtonEvent(buttonId: Int, isPressed: Boolean) {
    val oldState = buttonStates.getOrDefault(buttonId, false)
    if (oldState == isPressed) {
      return
    }

    buttonStates[buttonId] = isPressed

    if (isPressed) {
      handleButtonPressEvent(buttonId)
    } else {
      handleButtonReleaseEvent(buttonId)
    }
  }

  override fun reset() {
    for (key in buttonStates.keys) {
      buttonStates[key] = false
    }

    buttonLatches.clear()

    fun clearVirtualButtons(mappings: List<ButtonMapping>) {
      mappings.forEach { (button, _) -> button.reset() }
    }

    clearVirtualButtons(basicButtons)
    clearVirtualButtons(shiftButtons)
  }

  private fun handleButtonPressEvent(buttonId: Int) {
    val buttonList = if (isShifted) shiftButtons else basicButtons

    for (mapping in buttonList) {
      val (button, actionEvent) = mapping

      // Changes to uninteresting buttons are ignored.
      if (!button.isInterestedIn(buttonId)) {
        continue
      }

      // Check to see if any of the components of this button are latched by another virtual button.
      if (button.isLockedOut()) {
        continue
      }

      val stateChanged = button.update(buttonStates)
      if (!stateChanged) {
        continue
      }

      // Latch all of the components of this [VirtualButton] and add a callback to clean them up.
      button.components.forEach {
        // Cancel any subset latches.
        buttonLatches[it]?.first?.onFullyReleased = null
        buttonLatches[it] = mapping
      }
      button.onFullyReleased = { button.components.forEach { buttonLatches.remove(it) } }

      actionEvent.onPress?.let { action -> onAction(this, action) }
    }
  }

  private fun handleButtonReleaseEvent(buttonId: Int) {
    buttonLatches.get(buttonId)?.let { (button, actionSet) ->
      val stateChanged = button.update(buttonStates)
      if (!stateChanged) {
        return
      }

      actionSet.onRelease?.let { action -> onAction(this, action) }
    }
  }

  /**
   * Checks to see if any of the components of this [VirtualButton] are being held by another
   * [VirtualButton].
   */
  private fun VirtualButton.isLockedOut(): Boolean {
    val latchedComponents = buttonLatches.keys.intersect(components)
    return latchedComponents.any { buttonLatches[it]?.first?.let { isLockedOutBy(it) } ?: false }
  }
}

private fun VirtualButton.isInterestedIn(buttonId: Int): Boolean {
  return components.contains(buttonId)
}
