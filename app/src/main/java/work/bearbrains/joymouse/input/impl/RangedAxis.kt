package work.bearbrains.joymouse

import android.view.InputDevice
import android.view.MotionEvent
import kotlin.math.absoluteValue

/** Encapsulates a [MotionEvent] axis and associated [MotionRange]. */
internal data class RangedAxis(val axis: Int, private val range: InputDevice.MotionRange) {
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

    if ((newDeflection - rawDeflection).absoluteValue <= range.fuzz) {
      return false
    }
    rawDeflection = newDeflection

    val newValue = if (rawDeflection.absoluteValue < range.flat) 0f else rawDeflection
    if (newValue == deflection) {
      return false
    }

    deflection = newValue
    return true
  }
}
