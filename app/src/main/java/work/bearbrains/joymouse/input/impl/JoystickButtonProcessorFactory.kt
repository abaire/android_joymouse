package work.bearbrains.joymouse.input.impl

import android.view.KeyEvent
import work.bearbrains.joymouse.input.ActionConfig
import work.bearbrains.joymouse.input.JoystickAction
import work.bearbrains.joymouse.input.JoystickButtonProcessor
import work.bearbrains.joymouse.input.ShiftModifier

/**
 * Default [JoystickButtonProcessor.Factory] implementation that produces
 * [JoystickButtonProcessorImpl] instances based on an [ActionConfig].
 */
object JoystickButtonProcessorFactoryImpl : JoystickButtonProcessor.Factory {

  override fun create(
    config: ActionConfig,
    onAction: (JoystickButtonProcessor, JoystickAction) -> Unit
  ): JoystickButtonProcessor {

    fun mapping(
      button: VirtualButton,
      onPress: JoystickAction? = null,
      onRelease: JoystickAction? = null,
    ): JoystickButtonProcessorImpl.ButtonMapping =
      JoystickButtonProcessorImpl.ButtonMapping(button, onPress = onPress, onRelease = onRelease)

    val unshifted = mutableSetOf<JoystickButtonProcessorImpl.ButtonMapping>()
    val leftShifted = mutableSetOf<JoystickButtonProcessorImpl.ButtonMapping>()
    val rightShifted = mutableSetOf<JoystickButtonProcessorImpl.ButtonMapping>()
    val dualShifted = mutableSetOf<JoystickButtonProcessorImpl.ButtonMapping>()

    // Standard physical buttons
    unshifted.add(mapping(basicButton(KeyEvent.KEYCODE_DPAD_UP), onRelease = JoystickAction.DPAD_UP))
    unshifted.add(mapping(basicButton(KeyEvent.KEYCODE_DPAD_DOWN), onRelease = JoystickAction.DPAD_DOWN))
    unshifted.add(mapping(basicButton(KeyEvent.KEYCODE_DPAD_LEFT), onRelease = JoystickAction.DPAD_LEFT))
    unshifted.add(mapping(basicButton(KeyEvent.KEYCODE_DPAD_RIGHT), onRelease = JoystickAction.DPAD_RIGHT))

    // Standard primary button and fast cursor actions
    unshifted.add(
      mapping(
        basicButton(KeyEvent.KEYCODE_BUTTON_A),
        onPress = JoystickAction.PRIMARY_PRESS,
        onRelease = JoystickAction.PRIMARY_RELEASE,
      )
    )
    unshifted.add(
      mapping(
        basicButton(config.altButton),
        onPress = JoystickAction.PRIMARY_PRESS,
        onRelease = JoystickAction.PRIMARY_RELEASE,
      )
    )
    unshifted.add(
      mapping(
        basicButton(config.shiftButton),
        onPress = JoystickAction.FAST_CURSOR_PRESS,
        onRelease = JoystickAction.FAST_CURSOR_RELEASE,
      )
    )

    for ((action, binding) in config.actionBindings) {
      if (binding.keyCodes.isEmpty()) {
        continue
      }
      val button = VirtualButton(binding.keyCodes, isChord = binding.isChord)
      val buttonMapping = mapping(button, onRelease = action)
      when (binding.modifier) {
        ShiftModifier.NONE -> unshifted.add(buttonMapping)
        ShiftModifier.SHIFT -> leftShifted.add(buttonMapping)
        ShiftModifier.ALT -> rightShifted.add(buttonMapping)
        ShiftModifier.ALTSHIFT -> dualShifted.add(buttonMapping)
      }
    }

    return JoystickButtonProcessorImpl(
      toggleChord = config.toggleChord,
      unshiftedButtons = unshifted,
      leftShiftButtons = leftShifted,
      rightShiftButtons = rightShifted,
      dualShiftButtons = dualShifted,
      rawButtons =
        setOf(
          JoystickButtonProcessorImpl.RawButtonMapping(
            basicButton(config.shiftButton),
            onPress = JoystickAction.FAST_CURSOR_PRESS,
            onRelease = JoystickAction.FAST_CURSOR_RELEASE,
          ),
        ),
      leftShiftKey = config.shiftButton,
      rightShiftKey = config.altButton,
      onAction = onAction,
    )
  }

  private fun basicButton(buttonId: Int): VirtualButton =
    VirtualButton(setOf(buttonId), isChord = false)
}
