package work.bearbrains.joymouse.input.impl

import android.view.InputDevice
import android.view.MotionEvent
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import work.bearbrains.joymouse.RangedAxis

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

    assertThat(sut.update(makeEvent(FLAT))).isTrue()
  }

  @Test
  fun update_fromDeadzone_withDeflection_setsDeflection() {
    val sut = RangedAxis(AXIS, makeRange())

    sut.update(makeEvent(FLAT))

    assertThat(sut.deflection).isEqualTo(FLAT)
  }

  @Test
  fun update_fromDeflected_withTinyDeflection_returnsFalse() {
    val sut = RangedAxis(AXIS, makeRange())
    sut.update(makeEvent(FLAT))

    assertThat(sut.update(makeEvent(FLAT + FUZZ - EPSILON))).isFalse()
  }

  @Test
  fun update_fromDeflected_withTinyDeflection_retainsOldDeflection() {
    val sut = RangedAxis(AXIS, makeRange())
    sut.update(makeEvent(FLAT))

    sut.update(makeEvent(FLAT + FUZZ - EPSILON))

    assertThat(sut.deflection).isEqualTo(FLAT)
  }

  @Test
  fun update_fromDeflected_withFurtherDeflection_returnsTrue() {
    val sut = RangedAxis(AXIS, makeRange())
    sut.update(makeEvent(FLAT))

    assertThat(sut.update(makeEvent(FLAT + FUZZ))).isTrue()
  }

  @Test
  fun update_fromDeflected_withFurtherDeflection_setsDeflection() {
    val sut = RangedAxis(AXIS, makeRange())
    sut.update(makeEvent(FLAT))

    sut.update(makeEvent(FLAT + FUZZ))

    assertThat(sut.deflection).isEqualTo(FLAT + FUZZ)
  }

  @Test
  fun update_fromDeflected_withNoiseReturnToDeadzone_returnsFalse() {
    val sut = RangedAxis(AXIS, makeRange())
    sut.update(makeEvent(FLAT))

    assertThat(sut.update(makeEvent(FLAT - EPSILON))).isFalse()
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
