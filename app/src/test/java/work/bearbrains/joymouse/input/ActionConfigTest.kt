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

    assertThat(config.actionBindings[JoystickAction.TOGGLE_GESTURE]?.keyCodes)
      .containsExactly(KeyEvent.KEYCODE_BUTTON_A)
    assertThat(config.actionBindings[JoystickAction.TOGGLE_GESTURE]?.modifier)
      .isEqualTo(ShiftModifier.ALT)

    assertThat(config.toggleChord)
      .containsExactly(
        KeyEvent.KEYCODE_BUTTON_L1,
        KeyEvent.KEYCODE_BUTTON_R1,
        KeyEvent.KEYCODE_BUTTON_X,
      )

    assertThat(config.shiftButton).isEqualTo(KeyEvent.KEYCODE_BUTTON_L2)
    assertThat(config.altButton).isEqualTo(KeyEvent.KEYCODE_BUTTON_R2)
  }

  @Test
  fun remappableActions_containsActivateAndSpecialActions() {
    assertThat(ActionConfig.REMAPPABLE_ACTIONS).contains(JoystickAction.ACTIVATE)
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
    assertThat(ActionConfig.formatBinding(single)).isEqualTo("`Left shoulder`")

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
    assertThat(ActionConfig.formatChord(chord)).isEqualTo("`Left shoulder` + `Right shoulder` + `X`")
  }

  @Test
  fun shiftModifier_displayNames_useShiftAndAlt() {
    assertThat(ShiftModifier.NONE.getDisplayName()).isEqualTo("None")
    assertThat(ShiftModifier.SHIFT.getDisplayName()).isEqualTo("Shift")
    assertThat(ShiftModifier.ALT.getDisplayName()).isEqualTo("Alt")
    assertThat(ShiftModifier.ALTSHIFT.getDisplayName()).isEqualTo("Shift + Alt")
  }

  @Test
  fun actionDisplayName_usesGoogleStyleTitleCasing() {
    assertThat(ActionConfig.getActionDisplayName(JoystickAction.SWIPE_UP)).isEqualTo("Fling up")
    assertThat(ActionConfig.getActionDisplayName(JoystickAction.SWIPE_DOWN)).isEqualTo("Fling down")
    assertThat(ActionConfig.getActionDisplayName(JoystickAction.CYCLE_DISPLAY_FORWARD)).isEqualTo("Switch to next display")
    assertThat(ActionConfig.getActionDisplayName(JoystickAction.CYCLE_DISPLAY_BACKWARD)).isEqualTo("Switch to previous display")
    assertThat(ActionConfig.getActionDisplayName(JoystickAction.SELECT_PRIMARY_DEVICE)).isEqualTo("Choose active controller")
    assertThat(ActionConfig.getActionDisplayName(JoystickAction.TOGGLE_GESTURE)).isEqualTo("Toggle drag or fling gesture")
    assertThat(ActionConfig.getActionDisplayName(JoystickAction.RECENTS)).isEqualTo("Recent apps")
  }

  @Test
  fun friendlyButtonName_formatsCorrectly() {
    assertThat(ActionConfig.getFriendlyButtonName(KeyEvent.KEYCODE_BUTTON_L2)).isEqualTo("left trigger")
    assertThat(ActionConfig.getFriendlyButtonName(KeyEvent.KEYCODE_BUTTON_R2)).isEqualTo("right trigger")
    assertThat(ActionConfig.getFriendlyButtonName(KeyEvent.KEYCODE_BUTTON_L1)).isEqualTo("left shoulder")
  }
}
