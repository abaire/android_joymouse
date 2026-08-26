package work.bearbrains.joymouse.input.impl

import android.view.InputDevice
import android.view.MotionEvent
import kotlin.math.absoluteValue

/** Encapsulates a [MotionEvent] axis and associated [MotionRange]. */
internal data class RangedAxis(
  val axis: Int,
  private val range: InputDevice.MotionRange?,
  private val deadzone: Float? = null,
) {
  /** The modified deflection of this axis, between -1 and 1. */
  var deflection = 0f
    private set

  private var rawDeflection = 0f

  /**
   * Updates the [deflection] value for this axis. Returns true if the value was substantively
   * modified.
   */
  fun update(event: MotionEvent): Boolean {
    val newDeflection = event.getAxisValue(axis)

    val fuzz = range?.fuzz ?: 0f
    val flat = deadzone ?: range?.flat ?: 0f

    if ((newDeflection - rawDeflection).absoluteValue <= fuzz) {
      return false
    }
    rawDeflection = newDeflection

    val absRaw = rawDeflection.absoluteValue
    val newValue =
      if (absRaw <= flat) {
        0f
      } else if (flat < 1f) {
        val normalized = ((absRaw - flat) / (1f - flat)).coerceIn(0f, 1f)
        if (rawDeflection < 0f) -normalized else normalized
      } else {
        if (rawDeflection < 0f) -1f else 1f
      }
    if (newValue == deflection) {
      return false
    }

    deflection = newValue
    return true
  }
}
