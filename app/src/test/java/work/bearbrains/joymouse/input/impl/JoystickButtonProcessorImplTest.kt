package work.bearbrains.joymouse.input.impl

import android.view.KeyEvent
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import work.bearbrains.joymouse.input.JoystickAction
import work.bearbrains.joymouse.input.JoystickButtonProcessor

internal class JoystickButtonProcessorImplTest {
  @Test
  fun basic_multiplexed_actionOnPress_emitsWhenPressed() {
    val captor = EventCaptor()
    val sut =
      JoystickButtonProcessorImpl(
        toggleChord = emptySet(),
        unshiftedButtons = setOf(mapping(multiplexedButton(), ACTION_PRESS)),
        leftShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        rightShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        onAction = captor.capture(),
      )

    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)

    assertThat(captor.size).isEqualTo(1)
    assertThat(captor.events.first().first).isEqualTo(sut)
    assertThat(captor.events.first().second).isEqualTo(ACTION_PRESS)
  }

  @Test
  fun basic_multiplexed_actionOnPress_doesNotEmitWhenReleased() {
    val captor = EventCaptor()
    val sut =
      JoystickButtonProcessorImpl(
        toggleChord = emptySet(),
        unshiftedButtons = setOf(mapping(multiplexedButton(), ACTION_PRESS)),
        leftShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        rightShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        onAction = captor.capture(),
      )
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    captor.reset()

    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, false)

    assertThat(captor.size).isEqualTo(0)
  }

  @Test
  fun basic_multiplexed_actionOnPress_doesNotEmitIfPressedWhilePressed() {
    val captor = EventCaptor()
    val sut =
      JoystickButtonProcessorImpl(
        toggleChord = emptySet(),
        unshiftedButtons = setOf(mapping(multiplexedButton(), ACTION_PRESS)),
        leftShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        rightShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        onAction = captor.capture(),
      )
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    captor.reset()

    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)

    assertThat(captor.size).isEqualTo(0)
  }

  @Test
  fun basic_multiplexed_actionOnPress_emitsIfPressedAfterRelease() {
    val captor = EventCaptor()
    val sut =
      JoystickButtonProcessorImpl(
        toggleChord = emptySet(),
        unshiftedButtons = setOf(mapping(multiplexedButton(), ACTION_PRESS)),
        leftShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        rightShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        onAction = captor.capture(),
      )
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, false)
    captor.reset()

    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)

    assertThat(captor.size).isEqualTo(1)
    assertThat(captor.events.first().first).isEqualTo(sut)
    assertThat(captor.events.first().second).isEqualTo(ACTION_PRESS)
  }

  @Test
  fun basic_multiplexed_actionOnRelease_doesNotEmitWhenPressed() {
    val captor = EventCaptor()
    val sut =
      JoystickButtonProcessorImpl(
        toggleChord = emptySet(),
        unshiftedButtons = setOf(mapping(multiplexedButton(), onRelease = ACTION_RELEASE)),
        leftShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        rightShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        onAction = captor.capture(),
      )

    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)

    assertThat(captor.size).isEqualTo(0)
  }

  @Test
  fun basic_multiplexed_actionOnRelease_emitsWhenReleased() {
    val captor = EventCaptor()
    val sut =
      JoystickButtonProcessorImpl(
        toggleChord = emptySet(),
        unshiftedButtons = setOf(mapping(multiplexedButton(), onRelease = ACTION_RELEASE)),
        leftShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        rightShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        onAction = captor.capture(),
      )

    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, false)

    assertThat(captor.size).isEqualTo(1)
    assertThat(captor.events.first().first).isEqualTo(sut)
    assertThat(captor.events.first().second).isEqualTo(ACTION_RELEASE)
  }

  @Test
  fun basic_multiplexed_actionOnRelease_doesNotEmitIfReleasedWhileReleased() {
    val captor = EventCaptor()
    val sut =
      JoystickButtonProcessorImpl(
        toggleChord = emptySet(),
        unshiftedButtons = setOf(mapping(multiplexedButton(), onRelease = ACTION_RELEASE)),
        leftShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        rightShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        onAction = captor.capture(),
      )

    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, false)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, false)

    assertThat(captor.size).isEqualTo(0)
  }

  @Test
  fun basic_multiplexed_withShiftPressedAfterPress_emitsBasicReleaseWhenButtonReleased() {
    val captor = EventCaptor()
    val sut =
      JoystickButtonProcessorImpl(
        toggleChord = emptySet(),
        unshiftedButtons = setOf(mapping(multiplexedButton(), onRelease = ACTION_RELEASE)),
        leftShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        rightShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        onAction = captor.capture(),
      )
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    sut.handleButtonEvent(JoystickButtonProcessorImpl.LEFT_SHIFT, true)
    captor.reset()

    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, false)

    assertThat(captor.size).isEqualTo(1)
    assertThat(captor.events.last().first).isEqualTo(sut)
    assertThat(captor.events.last().second).isEqualTo(ACTION_RELEASE)
  }

  @Test
  fun shifted_multiplexed_withLeftShiftHeld_emitsShiftedActionOnPress() {
    val captor = EventCaptor()
    val sut =
      JoystickButtonProcessorImpl(
        toggleChord = emptySet(),
        unshiftedButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        leftShiftButtons = setOf(mapping(multiplexedButton(), ACTION_PRESS, ACTION_RELEASE)),
        rightShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        onAction = captor.capture(),
      )

    sut.handleButtonEvent(JoystickButtonProcessorImpl.LEFT_SHIFT, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)

    assertThat(captor.size).isEqualTo(1)
    assertThat(captor.events.first().first).isEqualTo(sut)
    assertThat(captor.events.first().second).isEqualTo(JoystickAction.PRIMARY_PRESS)
  }

  @Test
  fun shifted_multiplexed_withDoubleShiftHeld_doesNotEmitAction() {
    val captor = EventCaptor()
    val sut =
      JoystickButtonProcessorImpl(
        toggleChord = emptySet(),
        unshiftedButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        leftShiftButtons = setOf(mapping(multiplexedButton(), ACTION_PRESS, ACTION_RELEASE)),
        rightShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        onAction = captor.capture(),
      )

    sut.handleButtonEvent(JoystickButtonProcessorImpl.LEFT_SHIFT, true)
    sut.handleButtonEvent(JoystickButtonProcessorImpl.RIGHT_SHIFT, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)

    assertThat(captor.size).isEqualTo(0)
  }

  @Test
  fun shifted_multiplexed_withShiftHeld_emitsActionOnRelease() {
    val captor = EventCaptor()
    val sut =
      JoystickButtonProcessorImpl(
        toggleChord = emptySet(),
        unshiftedButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        leftShiftButtons = setOf(mapping(multiplexedButton(), ACTION_PRESS, ACTION_RELEASE)),
        rightShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        onAction = captor.capture(),
      )
    sut.handleButtonEvent(JoystickButtonProcessorImpl.LEFT_SHIFT, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    captor.reset()

    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, false)

    assertThat(captor.size).isEqualTo(1)
    assertThat(captor.events.first().first).isEqualTo(sut)
    assertThat(captor.events.first().second).isEqualTo(ACTION_RELEASE)
  }

  @Test
  fun shifted_multiplexed_withShiftReleasedAfterPress_doesNotEmitEvent() {
    val captor = EventCaptor()
    val sut =
      JoystickButtonProcessorImpl(
        toggleChord = emptySet(),
        unshiftedButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        leftShiftButtons = setOf(mapping(multiplexedButton(), ACTION_PRESS, ACTION_RELEASE)),
        rightShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        onAction = captor.capture(),
      )
    sut.handleButtonEvent(JoystickButtonProcessorImpl.LEFT_SHIFT, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    captor.reset()

    sut.handleButtonEvent(JoystickButtonProcessorImpl.LEFT_SHIFT, false)

    assertThat(captor.size).isEqualTo(0)
  }

  @Test
  fun shifted_multiplexed_withShiftReleasedAfterPress_emitsShiftedReleaseWhenButtonReleased() {
    val captor = EventCaptor()
    val sut =
      JoystickButtonProcessorImpl(
        toggleChord = emptySet(),
        unshiftedButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        leftShiftButtons = setOf(mapping(multiplexedButton(), ACTION_PRESS, ACTION_RELEASE)),
        rightShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        onAction = captor.capture(),
      )
    sut.handleButtonEvent(JoystickButtonProcessorImpl.LEFT_SHIFT, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    sut.handleButtonEvent(JoystickButtonProcessorImpl.LEFT_SHIFT, false)
    captor.reset()

    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, false)

    assertThat(captor.size).isEqualTo(1)
    assertThat(captor.events.last().first).isEqualTo(sut)
    assertThat(captor.events.last().second).isEqualTo(ACTION_RELEASE)
  }

  @Test
  fun basic_chorded_emitsMultiplexedPressFollowedByChordPress() {
    val captor = EventCaptor()
    val sut =
      JoystickButtonProcessorImpl(
        toggleChord = emptySet(),
        unshiftedButtons =
          setOf(
            mapping(multiplexedButton(), ACTION_PRESS, ACTION_RELEASE),
            mapping(chordButton(), CHORD_PRESS, CHORD_RELEASE)
          ),
        leftShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        rightShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        onAction = captor.capture(),
      )

    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_R1, true)

    assertThat(captor.size).isEqualTo(2)
    assertThat(captor.events.first().first).isEqualTo(sut)
    assertThat(captor.events.first().second).isEqualTo(ACTION_PRESS)
    assertThat(captor.events.last().first).isEqualTo(sut)
    assertThat(captor.events.last().second).isEqualTo(CHORD_PRESS)
  }

  @Test
  fun basic_chorded_emitsSubchordPressFollowedByChordPress() {
    val captor = EventCaptor()
    val sut =
      JoystickButtonProcessorImpl(
        toggleChord = emptySet(),
        unshiftedButtons =
          setOf(
            mapping(multiplexedButton(), ACTION_PRESS, ACTION_RELEASE),
            mapping(chordButton(), CHORD_PRESS, CHORD_RELEASE)
          ),
        leftShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        rightShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        onAction = captor.capture(),
      )

    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_R1, true)

    assertThat(captor.size).isEqualTo(2)
    assertThat(captor.events.first().first).isEqualTo(sut)
    assertThat(captor.events.first().second).isEqualTo(ACTION_PRESS)
    assertThat(captor.events.last().first).isEqualTo(sut)
    assertThat(captor.events.last().second).isEqualTo(CHORD_PRESS)
  }

  @Test
  fun basic_chorded_overlappingMultiplexed_emitsOnlyChordedRelease() {
    val captor = EventCaptor()
    val sut =
      JoystickButtonProcessorImpl(
        toggleChord = emptySet(),
        unshiftedButtons =
          setOf(
            mapping(multiplexedButton(), ACTION_PRESS, ACTION_RELEASE),
            mapping(chordButton(), CHORD_PRESS, CHORD_RELEASE)
          ),
        leftShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        rightShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        onAction = captor.capture(),
      )
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_R1, true)
    captor.reset()

    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, false)

    assertThat(captor.size).isEqualTo(1)
    assertThat(captor.events.first().first).isEqualTo(sut)
    assertThat(captor.events.first().second).isEqualTo(CHORD_RELEASE)
  }

  @Test
  fun basic_chorded_supersedingChorded_emitsOnlyChordedRelease() {
    val captor = EventCaptor()
    val sut =
      JoystickButtonProcessorImpl(
        toggleChord = emptySet(),
        unshiftedButtons =
          setOf(
            mapping(subchordButton(), ACTION_PRESS, ACTION_RELEASE),
            mapping(chordButton(), CHORD_PRESS, CHORD_RELEASE)
          ),
        leftShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        rightShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        onAction = captor.capture(),
      )
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_R1, true)
    captor.reset()

    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, false)

    assertThat(captor.size).isEqualTo(1)
    assertThat(captor.events.first().first).isEqualTo(sut)
    assertThat(captor.events.first().second).isEqualTo(CHORD_RELEASE)
  }

  @Test
  fun basic_chorded_overlappingChord_doesNotEmitPressOrRelease() {
    val captor = EventCaptor()
    val sut =
      JoystickButtonProcessorImpl(
        toggleChord = emptySet(),
        unshiftedButtons =
          setOf(
            mapping(multiplexedButton(), ACTION_PRESS, ACTION_RELEASE),
            mapping(chordButton(), CHORD_PRESS, CHORD_RELEASE)
          ),
        leftShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        rightShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        onAction = captor.capture(),
      )
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_R1, true)
    captor.reset()

    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_B, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_B, false)

    assertThat(captor.size).isEqualTo(0)
  }

  @Test
  fun basic_chorded_afterOverlappingChord_partialRelease_doesNotEmitPressOrRelease() {
    val captor = EventCaptor()
    val sut =
      JoystickButtonProcessorImpl(
        toggleChord = emptySet(),
        unshiftedButtons =
          setOf(
            mapping(multiplexedButton(), ACTION_PRESS, ACTION_RELEASE),
            mapping(chordButton(), CHORD_PRESS, CHORD_RELEASE)
          ),
        leftShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        rightShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        onAction = captor.capture(),
      )
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_R1, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_R1, false)
    captor.reset()

    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_B, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_B, false)

    assertThat(captor.size).isEqualTo(0)
  }

  @Test
  fun basic_chorded_afterOverlappingChord_fullRelease_emitsPress() {
    val captor = EventCaptor()
    val sut =
      JoystickButtonProcessorImpl(
        toggleChord = emptySet(),
        unshiftedButtons =
          setOf(
            mapping(chordButton(), ACTION_PRESS, ACTION_RELEASE),
            mapping(overlappingChordButton(), CHORD_PRESS, CHORD_RELEASE)
          ),
        leftShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        rightShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        onAction = captor.capture(),
      )
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_R1, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, false)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_R1, false)
    captor.reset()

    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_B, true)

    assertThat(captor.size).isEqualTo(1)
    assertThat(captor.events.first().first).isEqualTo(sut)
    assertThat(captor.events.first().second).isEqualTo(CHORD_PRESS)
  }

  @Test
  fun toggleChord_isProcessedEventIfElementsAreLatched() {
    val captor = EventCaptor()
    val sut =
      JoystickButtonProcessorImpl(
        toggleChord = setOf(KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_B),
        unshiftedButtons =
          setOf(
            mapping(chordButton(), ACTION_PRESS, ACTION_RELEASE),
            mapping(overlappingChordButton(), CHORD_PRESS, CHORD_RELEASE)
          ),
        leftShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        rightShiftButtons = setOf(mapping(multiplexedButton(), ALT_PRESS, ALT_RELEASE)),
        onAction = captor.capture(),
      )
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_R1, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_B, true)

    assertThat(captor.size).isEqualTo(2)
    assertThat(captor.events.last().first).isEqualTo(sut)
    assertThat(captor.events.last().second).isEqualTo(JoystickAction.TOGGLE_ENABLED)
  }

  private class EventCaptor {
    val events = mutableListOf<Pair<JoystickButtonProcessor, JoystickAction>>()

    val size: Int
      get() = events.size

    fun capture(): (JoystickButtonProcessor, JoystickAction) -> Unit = { processor, action ->
      events.add(Pair(processor, action))
    }

    fun reset() {
      events.clear()
    }
  }

  private companion object {
    fun mapping(
      button: VirtualButton,
      onPress: JoystickAction? = null,
      onRelease: JoystickAction? = null,
    ): JoystickButtonProcessorImpl.ButtonMapping =
      JoystickButtonProcessorImpl.ButtonMapping(button, onPress = onPress, onRelease = onRelease)

    fun chordButton() =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_R1), isChord = true)

    fun subchordButton() = VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A), isChord = true)

    fun overlappingChordButton() =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B), isChord = true)

    fun multiplexedButton() =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B), isChord = false)

    // Arbitrary action pairings.
    val ACTION_PRESS = JoystickAction.PRIMARY_PRESS
    val ACTION_RELEASE = JoystickAction.PRIMARY_RELEASE
    val CHORD_PRESS = JoystickAction.CYCLE_DISPLAY_FORWARD
    val CHORD_RELEASE = JoystickAction.CYCLE_DISPLAY_BACKWARD
    val ALT_PRESS = JoystickAction.FAST_CURSOR_PRESS
    val ALT_RELEASE = JoystickAction.FAST_CURSOR_RELEASE
  }
}
