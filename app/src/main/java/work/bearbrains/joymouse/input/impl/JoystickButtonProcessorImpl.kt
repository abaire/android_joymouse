package work.bearbrains.joymouse.input.impl

import android.view.KeyEvent
import java.lang.IllegalArgumentException
import work.bearbrains.joymouse.input.JoystickAction
import work.bearbrains.joymouse.input.JoystickButtonProcessor

/** Concrete implementation of [JoystickButtonProcessor]. */
class JoystickButtonProcessorImpl(
  private val unshiftedButtons: Set<ButtonMapping> = emptySet(),
  private val leftShiftButtons: Set<ButtonMapping> = emptySet(),
  private val rightShiftButtons: Set<ButtonMapping> = emptySet(),
  private val dualShiftButtons: Set<ButtonMapping> = emptySet(),
  private val rawButtons: Set<RawButtonMapping> = emptySet(),
  private val onAction: (JoystickButtonProcessor, JoystickAction) -> Unit,
) : JoystickButtonProcessor {
  private interface ButtonHolder {
    val button: VirtualButton
  }

  /** Describes actions that should be executed for a given [VirtualButton]. */
  data class ButtonMapping(
    override val button: VirtualButton,
    val actionSet: ActionSet,
  ) : ButtonHolder {
    constructor(
      button: VirtualButton,
      onPress: JoystickAction? = null,
      onRelease: JoystickAction? = null,
    ) : this(button, ActionSet(onPress = onPress, onRelease = onRelease))
  }

  /**
   * Describes actions for a button that should be sent independent of shift states or keycodes
   * being part of other button mappings. The given [VirtualButton] must have exactly one component.
   */
  data class RawButtonMapping(
    override val button: VirtualButton,
    val actionSet: ActionSet,
  ) : ButtonHolder {
    constructor(
      button: VirtualButton,
      onPress: JoystickAction? = null,
      onRelease: JoystickAction? = null,
    ) : this(button, ActionSet(onPress = onPress, onRelease = onRelease))

    init {
      if (button.components.size != 1) {
        throw IllegalArgumentException("`button` must have exactly one component.")
      }
    }
  }

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

  override fun handleButtonEvent(buttonId: Int, isPressed: Boolean) {
    val oldState = buttonStates.getOrDefault(buttonId, false)
    if (oldState == isPressed) {
      return
    }

    buttonStates[buttonId] = isPressed

    processRawButtonEvents(buttonId)

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

    fun clearVirtualButtons(mappings: Iterable<ButtonHolder>) {
      mappings.forEach { it.button.reset() }
    }

    clearVirtualButtons(unshiftedButtons)
    clearVirtualButtons(leftShiftButtons)
    clearVirtualButtons(rightShiftButtons)
    clearVirtualButtons(dualShiftButtons)
    clearVirtualButtons(rawButtons)
  }

  /** Process events for single buttons that ignore shift modes. */
  private fun processRawButtonEvents(buttonId: Int) {
    for (mapping in rawButtons) {
      val (button, actionEvent) = mapping
      if (button.isInterestedIn(buttonId)) {
        val stateChanged = button.update(buttonStates)
        if (!stateChanged) {
          continue
        }

        if (button.isPressed) {
          actionEvent.onPress?.let { action -> onAction(this, action) }
        } else {
          actionEvent.onRelease?.let { action -> onAction(this, action) }
        }
      }
    }
  }

  private fun handleButtonPressEvent(buttonId: Int) {
    val leftShift = buttonStates.getOrDefault(LEFT_SHIFT, false)
    val rightShift = buttonStates.getOrDefault(RIGHT_SHIFT, false)

    val buttonList =
      if (leftShift && rightShift) {
        dualShiftButtons
      } else if (rightShift) {
        rightShiftButtons
      } else if (leftShift) {
        leftShiftButtons
      } else {
        unshiftedButtons
      }

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
        buttonLatches[it]?.button?.onFullyReleased = null
        buttonLatches[it] = mapping
      }
      button.onFullyReleased = { button.components.forEach { buttonLatches.remove(it) } }

      actionEvent.onPress?.let { action -> onAction(this, action) }
    }
  }

  private fun handleButtonReleaseEvent(buttonId: Int) {
    buttonLatches.get(buttonId)?.let { mapping ->
      val stateChanged = mapping.button.update(buttonStates)
      if (!stateChanged) {
        return
      }

      mapping.actionSet.onRelease?.let { action -> onAction(this, action) }
    }
  }

  /**
   * Checks to see if any of the components of this [VirtualButton] are being held by another
   * [VirtualButton].
   */
  private fun VirtualButton.isLockedOut(): Boolean {
    val latchedComponents = buttonLatches.keys.intersect(components)
    return latchedComponents.any { buttonLatches[it]?.button?.let { isLockedOutBy(it) } ?: false }
  }

  companion object {
    const val LEFT_SHIFT = KeyEvent.KEYCODE_BUTTON_L2
    const val RIGHT_SHIFT = KeyEvent.KEYCODE_BUTTON_R2
  }
}

private fun VirtualButton.isInterestedIn(buttonId: Int): Boolean {
  return components.contains(buttonId)
}
