package work.bearbrains.joymouse.input

import android.view.KeyEvent
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActionConfigTest {

  @Test
  fun defaultActionConfig_containsAllExpectedMappings() {
    val config = ActionConfig.DEFAULT

    assertThat(config.actionBindings[JoystickAction.BACK]?.keyCodes)
      .containsExactly(KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_B)
    assertThat(config.actionBindings[JoystickAction.BACK]?.modifier).isEqualTo(ShiftModifier.NONE)

    assertThat(config.actionBindings[JoystickAction.HOME]?.keyCodes)
      .containsExactly(KeyEvent.KEYCODE_BUTTON_MODE)
    assertThat(config.actionBindings[JoystickAction.HOME]?.modifier).isEqualTo(ShiftModifier.NONE)

    assertThat(config.actionBindings[JoystickAction.RECENTS]?.keyCodes)
      .containsExactly(KeyEvent.KEYCODE_BUTTON_START)
    assertThat(config.actionBindings[JoystickAction.RECENTS]?.modifier).isEqualTo(ShiftModifier.NONE)

    assertThat(config.actionBindings[JoystickAction.ACTIVATE]?.keyCodes)
      .containsExactly(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_R2)
    assertThat(config.actionBindings[JoystickAction.ACTIVATE]?.modifier).isEqualTo(ShiftModifier.NONE)

    assertThat(config.actionBindings[JoystickAction.FAST_CURSOR]?.keyCodes)
      .containsExactly(KeyEvent.KEYCODE_BUTTON_L2)
    assertThat(config.actionBindings[JoystickAction.FAST_CURSOR]?.modifier).isEqualTo(ShiftModifier.NONE)

    assertThat(config.actionBindings[JoystickAction.TOGGLE_GESTURE]?.keyCodes)
      .containsExactly(KeyEvent.KEYCODE_BUTTON_THUMBR)
    assertThat(config.actionBindings[JoystickAction.TOGGLE_GESTURE]?.modifier)
      .isEqualTo(ShiftModifier.NONE)

    assertThat(config.actionBindings[JoystickAction.CYCLE_DISPLAY_BACKWARD]?.keyCodes)
      .containsExactly(KeyEvent.KEYCODE_BUTTON_L1)
    assertThat(config.actionBindings[JoystickAction.CYCLE_DISPLAY_BACKWARD]?.modifier)
      .isEqualTo(ShiftModifier.SHIFT)

    assertThat(config.actionBindings[JoystickAction.CYCLE_DISPLAY_FORWARD]?.keyCodes)
      .containsExactly(KeyEvent.KEYCODE_BUTTON_R1)
    assertThat(config.actionBindings[JoystickAction.CYCLE_DISPLAY_FORWARD]?.modifier)
      .isEqualTo(ShiftModifier.SHIFT)

    assertThat(config.actionBindings[JoystickAction.SELECT_PRIMARY_DEVICE]?.keyCodes)
      .containsExactly(KeyEvent.KEYCODE_BUTTON_SELECT)
    assertThat(config.actionBindings[JoystickAction.SELECT_PRIMARY_DEVICE]?.modifier)
      .isEqualTo(ShiftModifier.SHIFT)

    assertThat(config.actionBindings[JoystickAction.SWIPE_UP]?.keyCodes)
      .containsExactly(KeyEvent.KEYCODE_DPAD_UP)
    assertThat(config.actionBindings[JoystickAction.SWIPE_UP]?.modifier).isEqualTo(ShiftModifier.ALT)

    assertThat(config.actionBindings[JoystickAction.SWIPE_DOWN]?.keyCodes)
      .containsExactly(KeyEvent.KEYCODE_DPAD_DOWN)
    assertThat(config.actionBindings[JoystickAction.SWIPE_DOWN]?.modifier).isEqualTo(ShiftModifier.ALT)

    assertThat(config.actionBindings[JoystickAction.SWIPE_LEFT]?.keyCodes)
      .containsExactly(KeyEvent.KEYCODE_DPAD_LEFT)
    assertThat(config.actionBindings[JoystickAction.SWIPE_LEFT]?.modifier).isEqualTo(ShiftModifier.ALT)

    assertThat(config.actionBindings[JoystickAction.SWIPE_RIGHT]?.keyCodes)
      .containsExactly(KeyEvent.KEYCODE_DPAD_RIGHT)
    assertThat(config.actionBindings[JoystickAction.SWIPE_RIGHT]?.modifier).isEqualTo(ShiftModifier.ALT)

    assertThat(config.toggleChord)
      .containsExactly(
        KeyEvent.KEYCODE_BUTTON_L1,
        KeyEvent.KEYCODE_BUTTON_R1,
        KeyEvent.KEYCODE_BUTTON_X,
      )

    assertThat(config.shiftButton).isEqualTo(KeyEvent.KEYCODE_BUTTON_L2)
    assertThat(config.altButton).isEqualTo(KeyEvent.KEYCODE_BUTTON_R2)
    assertThat(config.mouseStick).isEqualTo(MouseStick.RIGHT_STICK)
    assertThat(config.invertX).isFalse()
    assertThat(config.invertY).isFalse()
  }

  @Test
  fun remappableActions_containsActivateAndSpecialActions() {
    assertThat(ActionConfig.REMAPPABLE_ACTIONS).contains(JoystickAction.ACTIVATE)
    assertThat(ActionConfig.REMAPPABLE_ACTIONS).contains(JoystickAction.FAST_CURSOR)
    assertThat(ActionConfig.REMAPPABLE_ACTIONS).contains(JoystickAction.TOGGLE_GESTURE)
    assertThat(ActionConfig.REMAPPABLE_ACTIONS).doesNotContain(JoystickAction.DPAD_UP)
    assertThat(ActionConfig.REMAPPABLE_ACTIONS).doesNotContain(JoystickAction.DPAD_DOWN)
    assertThat(ActionConfig.REMAPPABLE_ACTIONS).doesNotContain(JoystickAction.DPAD_LEFT)
    assertThat(ActionConfig.REMAPPABLE_ACTIONS).doesNotContain(JoystickAction.DPAD_RIGHT)
  }

  @Test
  fun actionConfig_sameShiftAndAltButton_throwsException() {
    assertThrows(IllegalArgumentException::class.java) {
      ActionConfig(
        shiftButton = KeyEvent.KEYCODE_BUTTON_L1,
        altButton = KeyEvent.KEYCODE_BUTTON_L1,
      )
    }
  }

  @Test
  fun jsonSerialization_roundtrip_preservesConfig() {
    val customBindings =
      mapOf(
        JoystickAction.BACK to
          ActionBinding(ShiftModifier.SHIFT, setOf(KeyEvent.KEYCODE_BUTTON_Y)),
        JoystickAction.HOME to
          ActionBinding(
            ShiftModifier.ALTSHIFT,
            setOf(KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1),
            isChord = true,
          ),
      )
    val customChord = setOf(KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_START)
    val config =
      ActionConfig(
        actionBindings = customBindings,
        toggleChord = customChord,
        shiftButton = KeyEvent.KEYCODE_BUTTON_L1,
        altButton = KeyEvent.KEYCODE_BUTTON_R1,
        mouseStick = MouseStick.LEFT_STICK,
        invertX = true,
        invertY = true,
      )

    val jsonString = config.toJson()
    val restored = ActionConfig.fromJson(jsonString)

    assertThat(restored.actionBindings[JoystickAction.BACK]?.modifier).isEqualTo(ShiftModifier.SHIFT)
    assertThat(restored.actionBindings[JoystickAction.BACK]?.keyCodes)
      .containsExactly(KeyEvent.KEYCODE_BUTTON_Y)

    assertThat(restored.actionBindings[JoystickAction.HOME]?.modifier).isEqualTo(ShiftModifier.ALTSHIFT)
    assertThat(restored.actionBindings[JoystickAction.HOME]?.keyCodes)
      .containsExactly(KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1)
    assertThat(restored.actionBindings[JoystickAction.HOME]?.isChord).isTrue()

    assertThat(restored.toggleChord).containsExactly(KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_START)
    assertThat(restored.shiftButton).isEqualTo(KeyEvent.KEYCODE_BUTTON_L1)
    assertThat(restored.altButton).isEqualTo(KeyEvent.KEYCODE_BUTTON_R1)
    assertThat(restored.mouseStick).isEqualTo(MouseStick.LEFT_STICK)
    assertThat(restored.invertX).isTrue()
    assertThat(restored.invertY).isTrue()

    // Verify missing actions in JSON fallback to defaults
    assertThat(restored.actionBindings[JoystickAction.CYCLE_DISPLAY_FORWARD]?.keyCodes)
      .containsExactly(KeyEvent.KEYCODE_BUTTON_R1)
  }

  @Test
  fun formatBinding_multiplexedAndChord_formatsCorrectly() {
    val multiplexed =
      ActionBinding(
        ShiftModifier.NONE,
        setOf(KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_B),
        isChord = false,
      )
    assertThat(ActionConfig.formatBinding(multiplexed)).isEqualTo("`Select` or `B`")

    val single = ActionBinding(ShiftModifier.SHIFT, setOf(KeyEvent.KEYCODE_BUTTON_L1))
    assertThat(ActionConfig.formatBinding(single)).isEqualTo("`Left bumper`")

    val chord =
      ActionBinding(
        ShiftModifier.ALT,
        setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_X),
        isChord = true,
      )
    assertThat(ActionConfig.formatBinding(chord)).isEqualTo("`A` + `X`")

    val empty = ActionBinding(ShiftModifier.NONE, emptySet())
    assertThat(ActionConfig.formatBinding(empty)).isEqualTo("Unassigned")
  }

  @Test
  fun formatChord_formatsCorrectly() {
    val chord = setOf(KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_X)
    assertThat(ActionConfig.formatChord(chord)).isEqualTo("`Left bumper` + `Right bumper` + `X`")
  }

  @Test
  fun shiftModifier_displayNames_useShiftAndAlt() {
    assertThat(ShiftModifier.NONE.getDisplayName()).isEqualTo("None")
    assertThat(ShiftModifier.SHIFT.getDisplayName()).isEqualTo("Shift")
    assertThat(ShiftModifier.ALT.getDisplayName()).isEqualTo("Alt")
    assertThat(ShiftModifier.ALTSHIFT.getDisplayName()).isEqualTo("Shift + Alt")
  }

  @Test
  fun formatBindingWithModifier_formatsWithActiveModifiers() {
    val config = ActionConfig.DEFAULT // shift = L2 (Left trigger), alt = R2 (Right trigger)

    val unshifted =
      ActionBinding(
        ShiftModifier.NONE,
        setOf(KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_B),
      )
    assertThat(ActionConfig.formatBindingWithModifier(unshifted, config))
      .isEqualTo("Select or B")

    val shiftAction = ActionBinding(ShiftModifier.SHIFT, setOf(KeyEvent.KEYCODE_BUTTON_R1))
    assertThat(ActionConfig.formatBindingWithModifier(shiftAction, config))
      .isEqualTo("Left trigger + Right bumper")

    val altAction = ActionBinding(ShiftModifier.ALT, setOf(KeyEvent.KEYCODE_DPAD_UP))
    assertThat(ActionConfig.formatBindingWithModifier(altAction, config))
      .isEqualTo("Right trigger + D-pad up")

    val dualShift =
      ActionBinding(ShiftModifier.ALTSHIFT, setOf(KeyEvent.KEYCODE_BUTTON_X))
    assertThat(ActionConfig.formatBindingWithModifier(dualShift, config))
      .isEqualTo("Left trigger + Right trigger + X")

    val chord =
      ActionBinding(
        ShiftModifier.SHIFT,
        setOf(KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1),
        isChord = true,
      )
    assertThat(ActionConfig.formatBindingWithModifier(chord, config))
      .isEqualTo("Left trigger + Left bumper + Right bumper")

    val unassigned = ActionBinding(ShiftModifier.NONE, emptySet())
    assertThat(ActionConfig.formatBindingWithModifier(unassigned, config)).isEqualTo("Unassigned")
  }

  @Test
  fun getBindingButtonGroups_returnsStructuredButtonGroups() {
    val config = ActionConfig.DEFAULT

    val unshifted =
      ActionBinding(
        ShiftModifier.NONE,
        setOf(KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_B),
      )
    assertThat(ActionConfig.getBindingButtonGroups(unshifted, config))
      .containsExactly(listOf("Select"), listOf("B"))

    val shiftAction = ActionBinding(ShiftModifier.SHIFT, setOf(KeyEvent.KEYCODE_BUTTON_R1))
    assertThat(ActionConfig.getBindingButtonGroups(shiftAction, config))
      .containsExactly(listOf("Left trigger", "Right bumper"))

    val chord =
      ActionBinding(
        ShiftModifier.SHIFT,
        setOf(KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1),
        isChord = true,
      )
    assertThat(ActionConfig.getBindingButtonGroups(chord, config))
      .containsExactly(listOf("Left trigger", "Left bumper", "Right bumper"))
  }

  @Test
  fun actionDisplayName_usesGoogleStyleTitleCasing() {
    assertThat(ActionConfig.getActionDisplayName(JoystickAction.SWIPE_UP)).isEqualTo("Fling up")
    assertThat(ActionConfig.getActionDisplayName(JoystickAction.SWIPE_DOWN)).isEqualTo("Fling down")
    assertThat(ActionConfig.getActionDisplayName(JoystickAction.CYCLE_DISPLAY_FORWARD))
      .isEqualTo("Move to next display")
    assertThat(ActionConfig.getActionDisplayName(JoystickAction.CYCLE_DISPLAY_BACKWARD))
      .isEqualTo("Move to previous display")
    assertThat(ActionConfig.getActionDisplayName(JoystickAction.SELECT_PRIMARY_DEVICE))
      .isEqualTo("Select active controller")
    assertThat(ActionConfig.getActionDisplayName(JoystickAction.TOGGLE_GESTURE))
      .isEqualTo("Toggle drag/fling")
    assertThat(ActionConfig.getActionDisplayName(JoystickAction.RECENTS)).isEqualTo("Recent")
    assertThat(ActionConfig.getActionDisplayName(JoystickAction.ACTIVATE)).isEqualTo("Activate at cursor")
    assertThat(ActionConfig.getActionDisplayName(JoystickAction.FAST_CURSOR)).isEqualTo("Move cursor faster")
  }

  @Test
  fun friendlyButtonName_formatsCorrectly() {
    assertThat(ActionConfig.getFriendlyButtonName(KeyEvent.KEYCODE_BUTTON_L2)).isEqualTo("left trigger")
    assertThat(ActionConfig.getFriendlyButtonName(KeyEvent.KEYCODE_BUTTON_R2)).isEqualTo("right trigger")
    assertThat(ActionConfig.getFriendlyButtonName(KeyEvent.KEYCODE_BUTTON_L1)).isEqualTo("left bumper")
  }

  @Test
  fun mouseStick_names_formatCorrectly() {
    assertThat(MouseStick.RIGHT_STICK.getDisplayName()).isEqualTo("Right thumbstick")
    assertThat(MouseStick.LEFT_STICK.getDisplayName()).isEqualTo("Left thumbstick")
    assertThat(MouseStick.RIGHT_STICK.getFriendlyName()).isEqualTo("right thumbstick")
    assertThat(MouseStick.LEFT_STICK.getFriendlyName()).isEqualTo("left thumbstick")
  }

  @Test
  fun mouseControlDisplayName_formatsInvertState() {
    val normal = ActionConfig(mouseStick = MouseStick.RIGHT_STICK, invertX = false, invertY = false)
    assertThat(ActionConfig.getMouseControlDisplayName(normal)).isEqualTo("Right thumbstick")

    val invertX = ActionConfig(mouseStick = MouseStick.LEFT_STICK, invertX = true, invertY = false)
    assertThat(ActionConfig.getMouseControlDisplayName(invertX)).isEqualTo("Left thumbstick (Invert X)")

    val invertBoth = ActionConfig(mouseStick = MouseStick.RIGHT_STICK, invertX = true, invertY = true)
    assertThat(ActionConfig.getMouseControlDisplayName(invertBoth)).isEqualTo("Right thumbstick (Invert X, Invert Y)")
  }

  @Test
  fun defaultCursorConfig_usesDefaultPaletteAndNormalShapes() {
    val config = ActionConfig.DEFAULT
    assertThat(config.cursorConfig.palette).isEqualTo(CursorPalette.DEFAULT)
    assertThat(config.cursorConfig.changeShapeForMode).isFalse()
    assertThat(config.cursorConfig.getColors()).isEqualTo(CursorColors.DEFAULT)
  }

  @Test
  fun cursorConfig_colorblindFriendlyPalette_returnsAccessibleColors() {
    val config = CursorConfig(palette = CursorPalette.COLORBLIND_FRIENDLY)
    assertThat(config.getColors()).isEqualTo(CursorColors.COLORBLIND_FRIENDLY)
  }

  @Test
  fun cursorConfig_customColors_roundtripThroughJson() {
    val customColors =
      CursorColors(
        released = android.graphics.Color.YELLOW,
        tap = android.graphics.Color.CYAN,
        longTouch = android.graphics.Color.GREEN,
        drag = android.graphics.Color.MAGENTA,
        fling = android.graphics.Color.RED,
      )
    val customCursorConfig =
      CursorConfig(
        palette = CursorPalette.CUSTOM,
        customColors = customColors,
        changeShapeForMode = true,
      )
    val actionConfig = ActionConfig(cursorConfig = customCursorConfig)

    val jsonString = actionConfig.toJson()
    val restored = ActionConfig.fromJson(jsonString)

    assertThat(restored.cursorConfig.palette).isEqualTo(CursorPalette.CUSTOM)
    assertThat(restored.cursorConfig.changeShapeForMode).isTrue()
    assertThat(restored.cursorConfig.getColors()).isEqualTo(customColors)
  }
}
