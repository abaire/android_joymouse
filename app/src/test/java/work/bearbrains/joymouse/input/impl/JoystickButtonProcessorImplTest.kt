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
        basicButtons = listOf(ButtonMapping(multiplexedButton(), ACTION_ON_PRESS_ONLY)),
        shiftButtons = listOf(ButtonMapping(multiplexedButton(), ACTION_ON_BOTH)),
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
        basicButtons = listOf(ButtonMapping(multiplexedButton(), ACTION_ON_PRESS_ONLY)),
        shiftButtons = listOf(ButtonMapping(multiplexedButton(), ACTION_ON_BOTH)),
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
        basicButtons = listOf(ButtonMapping(multiplexedButton(), ACTION_ON_PRESS_ONLY)),
        shiftButtons = listOf(ButtonMapping(multiplexedButton(), ACTION_ON_BOTH)),
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
        basicButtons = listOf(ButtonMapping(multiplexedButton(), ACTION_ON_PRESS_ONLY)),
        shiftButtons = listOf(ButtonMapping(multiplexedButton(), ACTION_ON_BOTH)),
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
        basicButtons = listOf(ButtonMapping(multiplexedButton(), ACTION_ON_RELEASE_ONLY)),
        shiftButtons = listOf(ButtonMapping(multiplexedButton(), ACTION_ON_BOTH)),
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
        basicButtons = listOf(ButtonMapping(multiplexedButton(), ACTION_ON_RELEASE_ONLY)),
        shiftButtons = listOf(ButtonMapping(multiplexedButton(), ACTION_ON_BOTH)),
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
        basicButtons = listOf(ButtonMapping(multiplexedButton(), ACTION_ON_RELEASE_ONLY)),
        shiftButtons = listOf(ButtonMapping(multiplexedButton(), ACTION_ON_BOTH)),
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
        basicButtons = listOf(ButtonMapping(multiplexedButton(), ACTION_ON_BOTH)),
        shiftButtons = listOf(ButtonMapping(multiplexedButton(), ALT_ACTION_ON_BOTH)),
        onAction = captor.capture(),
      )
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    sut.handleButtonEvent(sut.shiftButton, true)
    captor.reset()

    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, false)

    assertThat(captor.size).isEqualTo(1)
    assertThat(captor.events.last().first).isEqualTo(sut)
    assertThat(captor.events.last().second).isEqualTo(ACTION_RELEASE)
  }

  @Test
  fun shifted_multiplexed_withShiftHeld_emitsActionOnPress() {
    val captor = EventCaptor()
    val sut =
      JoystickButtonProcessorImpl(
        basicButtons = listOf(ButtonMapping(multiplexedButton(), ALT_ACTION_ON_BOTH)),
        shiftButtons = listOf(ButtonMapping(multiplexedButton(), ACTION_ON_BOTH)),
        onAction = captor.capture(),
      )

    sut.handleButtonEvent(sut.shiftButton, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)

    assertThat(captor.size).isEqualTo(1)
    assertThat(captor.events.first().first).isEqualTo(sut)
    assertThat(captor.events.first().second).isEqualTo(JoystickAction.PRIMARY_PRESS)
  }

  @Test
  fun shifted_multiplexed_withShiftHeld_emitsActionOnRelease() {
    val captor = EventCaptor()
    val sut =
      JoystickButtonProcessorImpl(
        basicButtons = listOf(ButtonMapping(multiplexedButton(), ALT_ACTION_ON_BOTH)),
        shiftButtons = listOf(ButtonMapping(multiplexedButton(), ACTION_ON_BOTH)),
        onAction = captor.capture(),
      )
    sut.handleButtonEvent(sut.shiftButton, true)
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
        basicButtons = listOf(ButtonMapping(multiplexedButton(), ALT_ACTION_ON_BOTH)),
        shiftButtons = listOf(ButtonMapping(multiplexedButton(), ACTION_ON_BOTH)),
        onAction = captor.capture(),
      )
    sut.handleButtonEvent(sut.shiftButton, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    captor.reset()

    sut.handleButtonEvent(sut.shiftButton, false)

    assertThat(captor.size).isEqualTo(0)
  }

  @Test
  fun shifted_multiplexed_withShiftReleasedAfterPress_emitsShiftedReleaseWhenButtonReleased() {
    val captor = EventCaptor()
    val sut =
      JoystickButtonProcessorImpl(
        basicButtons = listOf(ButtonMapping(multiplexedButton(), ALT_ACTION_ON_BOTH)),
        shiftButtons = listOf(ButtonMapping(multiplexedButton(), ACTION_ON_BOTH)),
        onAction = captor.capture(),
      )
    sut.handleButtonEvent(sut.shiftButton, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    sut.handleButtonEvent(sut.shiftButton, false)
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
        basicButtons =
          listOf(
            ButtonMapping(multiplexedButton(), ACTION_ON_BOTH),
            ButtonMapping(chordButton(), CHORDED_ACTION_ON_BOTH)
          ),
        shiftButtons = listOf(ButtonMapping(multiplexedButton(), ALT_ACTION_ON_BOTH)),
        onAction = captor.capture(),
      )

    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_R2, true)

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
        basicButtons =
          listOf(
            ButtonMapping(chordButton(), CHORDED_ACTION_ON_BOTH),
            ButtonMapping(subchordButton(), ACTION_ON_BOTH)
          ),
        shiftButtons = listOf(ButtonMapping(multiplexedButton(), ALT_ACTION_ON_BOTH)),
        onAction = captor.capture(),
      )

    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_R2, true)

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
        basicButtons =
          listOf(
            ButtonMapping(multiplexedButton(), ACTION_ON_BOTH),
            ButtonMapping(chordButton(), CHORDED_ACTION_ON_BOTH)
          ),
        shiftButtons = listOf(ButtonMapping(multiplexedButton(), ALT_ACTION_ON_BOTH)),
        onAction = captor.capture(),
      )
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_R2, true)
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
        basicButtons =
          listOf(
            ButtonMapping(chordButton(), CHORDED_ACTION_ON_BOTH),
            ButtonMapping(subchordButton(), ACTION_ON_BOTH)
          ),
        shiftButtons = listOf(ButtonMapping(multiplexedButton(), ALT_ACTION_ON_BOTH)),
        onAction = captor.capture(),
      )
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_R2, true)
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
        basicButtons =
          listOf(
            ButtonMapping(chordButton(), CHORDED_ACTION_ON_BOTH),
            ButtonMapping(overlappingChordButton(), ACTION_ON_BOTH)
          ),
        shiftButtons = listOf(ButtonMapping(multiplexedButton(), ALT_ACTION_ON_BOTH)),
        onAction = captor.capture(),
      )
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_R2, true)
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
        basicButtons =
          listOf(
            ButtonMapping(chordButton(), CHORDED_ACTION_ON_BOTH),
            ButtonMapping(overlappingChordButton(), ACTION_ON_BOTH)
          ),
        shiftButtons = listOf(ButtonMapping(multiplexedButton(), ALT_ACTION_ON_BOTH)),
        onAction = captor.capture(),
      )
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_R2, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_R2, false)
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
        basicButtons =
          listOf(
            ButtonMapping(chordButton(), ACTION_ON_BOTH),
            ButtonMapping(overlappingChordButton(), CHORDED_ACTION_ON_BOTH)
          ),
        shiftButtons = listOf(ButtonMapping(multiplexedButton(), ALT_ACTION_ON_BOTH)),
        onAction = captor.capture(),
      )
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_R2, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, false)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_R2, false)
    captor.reset()

    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_A, true)
    sut.handleButtonEvent(KeyEvent.KEYCODE_BUTTON_B, true)

    assertThat(captor.size).isEqualTo(1)
    assertThat(captor.events.first().first).isEqualTo(sut)
    assertThat(captor.events.first().second).isEqualTo(CHORD_PRESS)
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
    fun chordButton() =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_R2), isChord = true)

    fun subchordButton() = VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A), isChord = true)

    fun overlappingChordButton() =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B), isChord = true)

    fun multiplexedButton() =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B), isChord = false)

    val ACTION_PRESS = JoystickAction.PRIMARY_PRESS
    val ACTION_RELEASE = JoystickAction.PRIMARY_RELEASE
    val ACTION_ON_PRESS_ONLY =
      JoystickButtonProcessorImpl.ActionSet(onPress = ACTION_PRESS, onRelease = null)
    val ACTION_ON_RELEASE_ONLY =
      JoystickButtonProcessorImpl.ActionSet(onRelease = ACTION_RELEASE, onPress = null)

    val ACTION_ON_BOTH =
      JoystickButtonProcessorImpl.ActionSet(onPress = ACTION_PRESS, onRelease = ACTION_RELEASE)

    val CHORD_PRESS = JoystickAction.SWIPE_RIGHT
    val CHORD_RELEASE = JoystickAction.SWIPE_LEFT
    val CHORDED_ACTION_ON_BOTH =
      JoystickButtonProcessorImpl.ActionSet(onPress = CHORD_PRESS, onRelease = CHORD_RELEASE)

    val ALT_PRESS = JoystickAction.CYCLE_DISPLAY_FORWARD
    val ALT_RELEASE = JoystickAction.CYCLE_DISPLAY_BACKWARD
    val ALT_ACTION_ON_BOTH =
      JoystickButtonProcessorImpl.ActionSet(onPress = ALT_PRESS, onRelease = ALT_RELEASE)
  }
}
