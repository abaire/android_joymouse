package work.bearbrains.joymouse.input.impl

import android.view.KeyEvent
import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class VirtualButtonTest {

  @Test
  fun isPressed_defaultsToFalse() {
    val sut = VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A), false)

    assertThat(sut.isPressed).isFalse()
  }

  @Test
  fun update_inMultiplexerMode_whenNotPressed_withNoButtonsPressed_returnsFalse() {
    val sut = VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A), false)

    assertThat(sut.update(emptyMap())).isFalse()
    assertThat(sut.isPressed).isFalse()
  }

  @Test
  fun update_inMultiplexerMode_whenNotPressed_withIrrelevantButtonsPressed_returnsFalse() {
    val sut = VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A), false)

    assertThat(sut.update(mapOf(KeyEvent.KEYCODE_BUTTON_B to true))).isFalse()
    assertThat(sut.isPressed).isFalse()
  }

  @Test
  fun update_inMultiplexerMode_whenNotPressed_withButtonPressed_returnsTrue() {
    val sut = VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A), false)

    assertThat(sut.update(mapOf(KeyEvent.KEYCODE_BUTTON_A to true))).isTrue()
    assertThat(sut.isPressed).isTrue()
  }

  @Test
  fun update_inMultiplexerMode_whenNotPressed_withAllButtonPressed_returnsTrue() {
    val sut = VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B), false)

    assertThat(
        sut.update(mapOf(KeyEvent.KEYCODE_BUTTON_A to true, KeyEvent.KEYCODE_BUTTON_B to true))
      )
      .isTrue()
    assertThat(sut.isPressed).isTrue()
  }

  @Test
  fun update_inMultiplexerMode_whenPressed_withAllRelevantButtonReleased_returnsTrue() {
    val sut = VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B), false)
    sut.update(mapOf(KeyEvent.KEYCODE_BUTTON_A to true))

    assertThat(sut.update(mapOf(KeyEvent.KEYCODE_BUTTON_C to true))).isTrue()
    assertThat(sut.isPressed).isFalse()
  }

  @Test
  fun update_inMultiplexerMode_whenPressed_withSomeRelevantButtonReleased_returnsFalse() {
    val sut = VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B), false)
    sut.update(mapOf(KeyEvent.KEYCODE_BUTTON_A to true, KeyEvent.KEYCODE_BUTTON_B to true))

    assertThat(sut.update(mapOf(KeyEvent.KEYCODE_BUTTON_B to true))).isFalse()
    assertThat(sut.isPressed).isTrue()
  }

  @Test
  fun update_inMultiplexerMode_whenNotPressed_withAllRelevantButtonReleased_invokesOnFullyReleased() {
    val sut = VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B), false)
    var wasReleased = false
    sut.onFullyReleased = { wasReleased = true }

    sut.update(mapOf(KeyEvent.KEYCODE_BUTTON_C to true))

    assertThat(wasReleased).isTrue()
  }

  @Test
  fun update_inMultiplexerMode_whenPressed_withAllRelevantButtonReleased_invokesOnFullyReleased() {
    val sut = VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B), false)
    var wasReleased = false
    sut.onFullyReleased = { wasReleased = true }
    sut.update(mapOf(KeyEvent.KEYCODE_BUTTON_A to true, KeyEvent.KEYCODE_BUTTON_B to true))

    sut.update(mapOf(KeyEvent.KEYCODE_BUTTON_C to true))

    assertThat(wasReleased).isTrue()
  }

  @Test
  fun update_inChordMode_whenNotPressed_withNoButtonsPressed_returnsFalse() {
    val sut =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_L1), isChord = true)

    assertThat(sut.update(emptyMap())).isFalse()
    assertThat(sut.isPressed).isFalse()
  }

  @Test
  fun update_inChordMode_whenNotPressed_withIrrelevantButtonsPressed_returnsFalse() {
    val sut =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_L1), isChord = true)

    assertThat(sut.update(mapOf(KeyEvent.KEYCODE_BUTTON_B to true))).isFalse()
    assertThat(sut.isPressed).isFalse()
  }

  @Test
  fun update_inChordMode_whenNotPressed_withSomeButtonsPressed_returnsFalse() {
    val sut =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_L1), isChord = true)

    assertThat(sut.update(mapOf(KeyEvent.KEYCODE_BUTTON_A to true))).isFalse()
    assertThat(sut.isPressed).isFalse()
  }

  @Test
  fun update_inChordMode_whenNotPressed_withAllButtonPressed_returnsTrue() {
    val sut =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_L1), isChord = true)

    assertThat(
        sut.update(mapOf(KeyEvent.KEYCODE_BUTTON_A to true, KeyEvent.KEYCODE_BUTTON_L1 to true))
      )
      .isTrue()
    assertThat(sut.isPressed).isTrue()
  }

  @Test
  fun update_inChordMode_whenPressed_withAllRelevantButtonReleased_returnsTrue() {
    val sut =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_L1), isChord = true)
    sut.update(mapOf(KeyEvent.KEYCODE_BUTTON_A to true, KeyEvent.KEYCODE_BUTTON_L1 to true))

    assertThat(sut.update(mapOf(KeyEvent.KEYCODE_BUTTON_C to true))).isTrue()
    assertThat(sut.isPressed).isFalse()
  }

  @Test
  fun update_inChordMode_whenPressed_withSomeRelevantButtonReleased_returnsTrue() {
    val sut =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_L1), isChord = true)
    sut.update(mapOf(KeyEvent.KEYCODE_BUTTON_A to true, KeyEvent.KEYCODE_BUTTON_L1 to true))

    assertThat(sut.update(mapOf(KeyEvent.KEYCODE_BUTTON_A to true))).isTrue()
    assertThat(sut.isPressed).isFalse()
  }

  @Test
  fun update_inChordMode_whenNotPressed_withAllRelevantButtonReleased_invokesOnFullyReleased() {
    val sut =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_L1), isChord = true)
    var wasReleased = false
    sut.onFullyReleased = { wasReleased = true }

    sut.update(mapOf(KeyEvent.KEYCODE_BUTTON_C to true))

    assertThat(wasReleased).isTrue()
  }

  @Test
  fun update_inChordMode_whenPressed_withAllRelevantButtonReleased_invokesOnFullyReleased() {
    val sut =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_L1), isChord = true)
    var wasReleased = false
    sut.onFullyReleased = { wasReleased = true }
    sut.update(mapOf(KeyEvent.KEYCODE_BUTTON_A to true, KeyEvent.KEYCODE_BUTTON_L1 to true))

    sut.update(mapOf(KeyEvent.KEYCODE_BUTTON_C to true))

    assertThat(wasReleased).isTrue()
  }

  @Test
  fun isLockedOut_multiplexed_againstMultiplexed_nonOverlapping_returnsFalse() {
    val sut =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_L1), isChord = false)
    val latchHolder =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_C), isChord = false)

    assertThat(sut.isLockedOutBy(latchHolder)).isFalse()
  }

  @Test
  fun isLockedOut_multiplexed_againstChorded_nonOverlapping_returnsFalse() {
    val sut =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_L1), isChord = false)
    val latchHolder =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_C), isChord = true)

    assertThat(sut.isLockedOutBy(latchHolder)).isFalse()
  }

  @Test
  fun isLockedOut_chorded_againstMultiplexed_nonOverlapping_returnsFalse() {
    val sut =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_L1), isChord = true)
    val latchHolder =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_C), isChord = false)

    assertThat(sut.isLockedOutBy(latchHolder)).isFalse()
  }

  @Test
  fun isLockedOut_chorded_againstChorded_nonOverlapping_returnsFalse() {
    val sut =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_L1), isChord = true)
    val latchHolder =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_C), isChord = true)

    assertThat(sut.isLockedOutBy(latchHolder)).isFalse()
  }

  @Test
  fun isLockedOut_multiplexed_againstMultiplexed_returnsTrue() {
    val sut =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_L1), isChord = false)
    val latchHolder =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B), isChord = false)

    assertThat(sut.isLockedOutBy(latchHolder)).isTrue()
  }

  @Test
  fun isLockedOut_multiplexed_againstChorded_returnsTrue() {
    val sut =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_L1), isChord = false)
    val latchHolder =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B), isChord = true)

    assertThat(sut.isLockedOutBy(latchHolder)).isTrue()
  }

  @Test
  fun isLockedOut_chorded_againstMultiplexed_returnsFalse() {
    val sut =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_L1), isChord = true)
    val latchHolder =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B), isChord = false)

    assertThat(sut.isLockedOutBy(latchHolder)).isFalse()
  }

  @Test
  fun isLockedOut_chorded_againstChordedWithExtraButtons_returnsTrue() {
    val sut =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_L1), isChord = true)
    val latchHolder =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B), isChord = true)

    assertThat(sut.isLockedOutBy(latchHolder)).isTrue()
  }

  @Test
  fun isLockedOut_chorded_againstChordedWithSubsetButtons_returnsFalse() {
    val sut =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_L1), isChord = true)
    val latchHolder = VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_L1), isChord = true)

    assertThat(sut.isLockedOutBy(latchHolder)).isFalse()
  }

  @Test
  fun isLockedOut_multiplexed_againstSelf_returnsFalse() {
    val sut =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_L1), isChord = false)

    assertThat(sut.isLockedOutBy(sut)).isFalse()
  }

  @Test
  fun isLockedOut_chorded_againstSelf_returnsFalse() {
    val sut =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_L1), isChord = true)

    assertThat(sut.isLockedOutBy(sut)).isFalse()
  }

  @Test
  fun isLockedOut_chorded_againstChordedWithIdenticalButtons_returnsTrue() {
    val sut =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_L1), isChord = true)
    val latchHolder =
      VirtualButton(setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_L1), isChord = true)

    assertThat(sut.isLockedOutBy(latchHolder)).isTrue()
  }
}
