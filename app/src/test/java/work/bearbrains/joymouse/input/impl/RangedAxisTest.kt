package work.bearbrains.joymouse.input.impl

import android.view.InputDevice
import android.view.MotionEvent
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

internal class RangedAxisTest {

  @Test
  fun update_fromDeadzone_withDeadzoneDeflection_returnsFalse() {
    val sut = RangedAxis(AXIS, makeRange())

    assertThat(sut.update(makeEvent(FLAT - EPSILON))).isFalse()
  }

  @Test
  fun update_fromDeadzone_withDeadzoneDeflection_setsDeflectionToZero() {
    val sut = RangedAxis(AXIS, makeRange())

    sut.update(makeEvent(FLAT - EPSILON))

    assertThat(sut.deflection).isEqualTo(0f)
  }

  @Test
  fun update_fromDeadzone_withDeflection_returnsTrue() {
    val sut = RangedAxis(AXIS, makeRange())

    assertThat(sut.update(makeEvent(0.55f))).isTrue()
  }

  @Test
  fun update_fromDeadzone_withDeflection_setsDeflection() {
    val sut = RangedAxis(AXIS, makeRange())

    sut.update(makeEvent(0.55f))

    // With FLAT = 0.1f and raw = 0.55f, normalized deflection = (0.55 - 0.1) / (1 - 0.1) = 0.5f
    assertThat(sut.deflection).isWithin(0.0001f).of(0.5f)
  }

  @Test
  fun update_fromDeflected_withTinyDeflection_returnsFalse() {
    val sut = RangedAxis(AXIS, makeRange())
    sut.update(makeEvent(0.55f))

    assertThat(sut.update(makeEvent(0.55f + FUZZ - EPSILON))).isFalse()
  }

  @Test
  fun update_fromDeflected_withTinyDeflection_retainsOldDeflection() {
    val sut = RangedAxis(AXIS, makeRange())
    sut.update(makeEvent(0.55f))

    sut.update(makeEvent(0.55f + FUZZ - EPSILON))

    assertThat(sut.deflection).isWithin(0.0001f).of(0.5f)
  }

  @Test
  fun update_fromDeflected_withFurtherDeflection_returnsTrue() {
    val sut = RangedAxis(AXIS, makeRange())
    sut.update(makeEvent(0.55f))

    assertThat(sut.update(makeEvent(0.55f + FUZZ + EPSILON))).isTrue()
  }

  @Test
  fun update_fromDeflected_withFurtherDeflection_setsDeflection() {
    val sut = RangedAxis(AXIS, makeRange())
    sut.update(makeEvent(0.55f))

    sut.update(makeEvent(1.0f))

    assertThat(sut.deflection).isEqualTo(1.0f)
  }

  @Test
  fun update_fromDeflected_withNoiseReturnToDeadzone_returnsFalse() {
    val sut = RangedAxis(AXIS, makeRange())
    sut.update(makeEvent(0.55f))
    sut.update(makeEvent(FLAT - EPSILON))

    assertThat(sut.update(makeEvent(FLAT - EPSILON * 2))).isFalse()
  }

  @Test
  fun update_withExplicitDeadzone_overridesMotionRangeFlat() {
    // Range has flat = 0.05f, but deadzone is set to 0.2f
    val sut = RangedAxis(AXIS, makeRange(flatValue = 0.05f, fuzzValue = 0.01f), deadzone = 0.2f)

    // Deflection of 0.15f is above range.flat (0.05f) but below custom deadzone (0.2f)
    assertThat(sut.update(makeEvent(0.15f))).isFalse()
    assertThat(sut.deflection).isEqualTo(0f)

    // Deflection of 0.60f reaches custom deadzone and is normalized: (0.6 - 0.2) / (1 - 0.2) = 0.5f
    assertThat(sut.update(makeEvent(0.60f))).isTrue()
    assertThat(sut.deflection).isWithin(0.0001f).of(0.5f)
  }

  @Test
  fun update_withZeroDeadzone_allowsDeflectionAboveFuzz() {
    // Range has flat = 0.1f, but deadzone is explicitly 0f
    val sut = RangedAxis(AXIS, makeRange(flatValue = 0.1f, fuzzValue = 0.02f), deadzone = 0f)

    // Deflection of 0.05f is below range.flat (0.1f) but above deadzone (0f) and fuzz (0.02f)
    assertThat(sut.update(makeEvent(0.05f))).isTrue()
    assertThat(sut.deflection).isWithin(0.0001f).of(0.05f)
  }

  @Test
  fun update_scalesDeflectionSmoothlyAcrossDeadzoneRange() {
    val sut = RangedAxis(AXIS, makeRange(flatValue = 0f, fuzzValue = 0.001f), deadzone = 0.30f)

    // Deflection exactly at or below 0.30f is deadzone
    assertThat(sut.update(makeEvent(0.30f))).isFalse()
    assertThat(sut.deflection).isEqualTo(0f)

    // Deflection at 0.31f (barely past deadzone) starts smoothly at ~1.4%
    assertThat(sut.update(makeEvent(0.31f))).isTrue()
    assertThat(sut.deflection).isWithin(0.0001f).of((0.31f - 0.30f) / (1.0f - 0.30f))

    // Deflection at 0.65f (halfway through active range) is 50%
    assertThat(sut.update(makeEvent(0.65f))).isTrue()
    assertThat(sut.deflection).isWithin(0.0001f).of(0.50f)

    // Deflection at 1.00f reaches full 100%
    assertThat(sut.update(makeEvent(1.00f))).isTrue()
    assertThat(sut.deflection).isEqualTo(1.0f)

    // Negative deflection at -0.65f is -50%
    assertThat(sut.update(makeEvent(-0.65f))).isTrue()
    assertThat(sut.deflection).isWithin(0.0001f).of(-0.50f)
  }

  private companion object {
    // Deadzone.
    const val FLAT = 0.1f

    // Minimum deflection necessary to consider the event a meaningful change.
    const val FUZZ = 0.05f

    const val EPSILON = 0.001f

    const val AXIS = MotionEvent.AXIS_LTRIGGER

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
