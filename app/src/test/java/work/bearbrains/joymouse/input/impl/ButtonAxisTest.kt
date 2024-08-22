package work.bearbrains.joymouse.input.impl

import android.view.InputDevice
import android.view.MotionEvent
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import work.bearbrains.joymouse.ButtonAxis
import work.bearbrains.joymouse.RangedAxis

internal class ButtonAxisTest {

  @Test
  fun update_withDeflectionWithinDeadzone_returnsFalse() {
    val sut = ButtonAxis(createRangedAxis(), POSITIVE, NEGATIVE)

    assertThat(sut.update(makeEvent(FLAT - EPSILON))).isFalse()
    assertThat(sut.isPositivePressed).isFalse()
    assertThat(sut.isNegativePressed).isFalse()
  }

  @Test
  fun update_withDeflectionWithinNoise_returnsFalse() {
    val sut = ButtonAxis(createRangedAxis(), POSITIVE, NEGATIVE)
    sut.update(makeEvent(FLAT - FUZZ))

    assertThat(sut.update(makeEvent(FLAT))).isFalse()
    assertThat(sut.isPositivePressed).isFalse()
    assertThat(sut.isNegativePressed).isFalse()
  }

  @Test
  fun update_withDeflectionBeyondThreshold_isPressed() {
    val sut = ButtonAxis(createRangedAxis(), POSITIVE, NEGATIVE)

    assertThat(sut.update(makeEvent(ButtonAxis.TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD)))
      .isTrue()
    assertThat(sut.isPositivePressed).isTrue()
    assertThat(sut.isNegativePressed).isFalse()
  }

  @Test
  fun update_whilePressed_withDeflectionBelowThreshold_isReleased() {
    val sut = ButtonAxis(createRangedAxis(), POSITIVE, NEGATIVE)
    sut.update(makeEvent(ButtonAxis.TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD))

    assertThat(sut.update(makeEvent(ButtonAxis.TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD - FUZZ)))
      .isTrue()
    assertThat(sut.isPositivePressed).isFalse()
    assertThat(sut.isNegativePressed).isFalse()
  }

  @Test
  fun update_withNegativeDeflectionWithinDeadzone_returnsFalse() {
    val sut = ButtonAxis(createRangedAxis(), POSITIVE, NEGATIVE)

    assertThat(sut.update(makeEvent(-(FLAT - EPSILON)))).isFalse()
    assertThat(sut.isPositivePressed).isFalse()
    assertThat(sut.isNegativePressed).isFalse()
  }

  @Test
  fun update_withNegativeDeflectionWithinNoise_returnsFalse() {
    val sut = ButtonAxis(createRangedAxis(), POSITIVE, NEGATIVE)
    sut.update(makeEvent(-(FLAT - FUZZ)))

    assertThat(sut.update(makeEvent(-FLAT))).isFalse()
    assertThat(sut.isPositivePressed).isFalse()
    assertThat(sut.isNegativePressed).isFalse()
  }

  @Test
  fun update_withNegativeDeflectionBeyondThreshold_isPressed() {
    val sut = ButtonAxis(createRangedAxis(), POSITIVE, NEGATIVE)

    assertThat(sut.update(makeEvent(-ButtonAxis.TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD)))
      .isTrue()
    assertThat(sut.isPositivePressed).isFalse()
    assertThat(sut.isNegativePressed).isTrue()
  }

  @Test
  fun update_whilePressed_withNegativeDeflectionBelowThreshold_isReleased() {
    val sut = ButtonAxis(createRangedAxis(), POSITIVE, NEGATIVE)
    sut.update(makeEvent(-ButtonAxis.TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD))

    assertThat(
        sut.update(makeEvent(-ButtonAxis.TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD + FUZZ))
      )
      .isTrue()
    assertThat(sut.isPositivePressed).isFalse()
    assertThat(sut.isNegativePressed).isFalse()
  }

  @Test
  fun update_withLatchUntilZero_whenRaisingAboveThreshold_returnsTrue() {
    val sut = ButtonAxis(createRangedAxis(), POSITIVE, NEGATIVE, latchUntilZero = true)

    assertThat(sut.update(makeEvent(ButtonAxis.TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD)))
      .isTrue()
  }

  @Test
  fun update_withLatchUntilZero_whenRaisingAboveThreshold_setsPositivePressed() {
    val sut = ButtonAxis(createRangedAxis(), POSITIVE, NEGATIVE, latchUntilZero = true)

    sut.update(makeEvent(ButtonAxis.TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD))

    assertThat(sut.isPositivePressed).isTrue()
    assertThat(sut.isNegativePressed).isFalse()
  }

  @Test
  fun update_withLatchUntilZero_whenFallingBelowThreshold_returnsFalse() {
    val sut = ButtonAxis(createRangedAxis(), POSITIVE, NEGATIVE, latchUntilZero = true)
    sut.update(makeEvent(ButtonAxis.TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD))

    assertThat(sut.update(makeEvent(FLAT))).isFalse()
  }

  @Test
  fun update_withLatchUntilZero_whenFallingBelowThreshold_remainsPressed() {
    val sut = ButtonAxis(createRangedAxis(), POSITIVE, NEGATIVE, latchUntilZero = true)
    sut.update(makeEvent(ButtonAxis.TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD))

    sut.update(makeEvent(FLAT))

    assertThat(sut.isPositivePressed).isTrue()
    assertThat(sut.isNegativePressed).isFalse()
  }

  @Test
  fun update_withLatchUntilZero_whenFallingIntoDeadzone_returnsTrue() {
    val sut = ButtonAxis(createRangedAxis(), POSITIVE, NEGATIVE, latchUntilZero = true)
    sut.update(makeEvent(ButtonAxis.TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD))

    assertThat(sut.update(makeEvent(FLAT - EPSILON))).isTrue()
  }

  @Test
  fun update_withLatchUntilZero_whenFallingIntoDeadzone_clearsPressed() {
    val sut = ButtonAxis(createRangedAxis(), POSITIVE, NEGATIVE, latchUntilZero = true)
    sut.update(makeEvent(ButtonAxis.TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD))

    sut.update(makeEvent(FLAT - EPSILON))

    assertThat(sut.isPositivePressed).isFalse()
    assertThat(sut.isNegativePressed).isFalse()
  }

  @Test
  fun update_withLatchUntilZero_whenSwingingNegative_returnsTrue() {
    val sut = ButtonAxis(createRangedAxis(), POSITIVE, NEGATIVE, latchUntilZero = true)
    sut.update(makeEvent(ButtonAxis.TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD))

    assertThat(sut.update(makeEvent(-1f))).isTrue()
  }

  @Test
  fun update_withLatchUntilZero_whenSwingingNegative_swapsPressed() {
    val sut = ButtonAxis(createRangedAxis(), POSITIVE, NEGATIVE, latchUntilZero = true)
    sut.update(makeEvent(ButtonAxis.TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD))

    sut.update(makeEvent(-1f))

    assertThat(sut.isPositivePressed).isFalse()
    assertThat(sut.isNegativePressed).isTrue()
  }

  @Test
  fun update_withLatchUntilZero_whenFallingBelowNegativeThreshold_returnsFalse() {
    val sut = ButtonAxis(createRangedAxis(), POSITIVE, NEGATIVE, latchUntilZero = true)
    sut.update(makeEvent(-ButtonAxis.TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD))

    assertThat(sut.update(makeEvent(-FLAT))).isFalse()
  }

  @Test
  fun update_withLatchUntilZero_whenFallingBelowNegativeThreshold_remainsPressed() {
    val sut = ButtonAxis(createRangedAxis(), POSITIVE, NEGATIVE, latchUntilZero = true)
    sut.update(makeEvent(-ButtonAxis.TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD))

    sut.update(makeEvent(-FLAT))

    assertThat(sut.isPositivePressed).isFalse()
    assertThat(sut.isNegativePressed).isTrue()
  }

  @Test
  fun update_withLatchUntilZero_whenFallingIntoNegativeDeadzone_returnsTrue() {
    val sut = ButtonAxis(createRangedAxis(), POSITIVE, NEGATIVE, latchUntilZero = true)
    sut.update(makeEvent(-ButtonAxis.TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD))

    assertThat(sut.update(makeEvent(-(FLAT - EPSILON)))).isTrue()
  }

  @Test
  fun update_withLatchUntilZero_whenFallingIntoNegativeDeadzone_clearsPressed() {
    val sut = ButtonAxis(createRangedAxis(), POSITIVE, NEGATIVE, latchUntilZero = true)
    sut.update(makeEvent(-ButtonAxis.TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD))

    sut.update(makeEvent(-(FLAT - EPSILON)))

    assertThat(sut.isPositivePressed).isFalse()
    assertThat(sut.isNegativePressed).isFalse()
  }

  @Test
  fun update_withLatchUntilZero_whenSwingingPositive_returnsTrue() {
    val sut = ButtonAxis(createRangedAxis(), POSITIVE, NEGATIVE, latchUntilZero = true)
    sut.update(makeEvent(-ButtonAxis.TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD))

    assertThat(sut.update(makeEvent(1f))).isTrue()
  }

  @Test
  fun update_withLatchUntilZero_whenSwingingPositive_swapsPressed() {
    val sut = ButtonAxis(createRangedAxis(), POSITIVE, NEGATIVE, latchUntilZero = true)
    sut.update(makeEvent(-ButtonAxis.TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD))

    sut.update(makeEvent(1f))

    assertThat(sut.isPositivePressed).isTrue()
    assertThat(sut.isNegativePressed).isFalse()
  }

  private companion object {
    const val AXIS = MotionEvent.AXIS_LTRIGGER

    const val POSITIVE = 1
    const val NEGATIVE = -1

    // Deadzone.
    const val FLAT = 0.1f

    // Minimum deflection necessary to consider the event a meaningful change.
    const val FUZZ = 0.05f

    const val EPSILON = 0.001f

    fun createRangedAxis(): RangedAxis = RangedAxis(AXIS, makeRange())

    fun makeEvent(axisValue: Float): MotionEvent {
      return mock { on { getAxisValue(anyInt()) } doReturn axisValue }
    }

    fun makeRange(flatValue: Float = FLAT, fuzzValue: Float = FUZZ): InputDevice.MotionRange {
      return mock {
        on { fuzz } doReturn fuzzValue
        on { flat } doReturn flatValue
      }
    }
  }
}
