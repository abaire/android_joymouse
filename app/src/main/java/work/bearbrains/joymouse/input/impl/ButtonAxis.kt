package work.bearbrains.joymouse

import android.view.MotionEvent
import androidx.annotation.VisibleForTesting

/**
 * Encapsulates a [MotionEvent] axis and associated [MotionRange] that should be mapped to binary
 * [KeyEvent] keycodes.
 */
internal data class ButtonAxis(
  private val axis: RangedAxis,
  val positiveKeycode: Int,
  val negativeKeycode: Int?,

  /**
   * [MotionEvent]s appear to cancel [GestureDescription]s, so virtual buttons may be set to refrain
   * from emitting an "up" event until they return to zero.
   */
  private val latchUntilZero: Boolean = false,
) {
  var isPositivePressed = false
    private set

  var isNegativePressed = false
    private set

  /**
   * Updates whether or not this axis represents a pressed button.
   *
   * Returns true if the positive or negative press states were modified.
   */
  fun update(event: MotionEvent): Boolean {
    if (!axis.update(event)) {
      return false
    }

    var ret = false

    val pastPositiveThreshold = axis.deflection >= TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD
    if (!isPositivePressed) {
      if (pastPositiveThreshold) {
        isPositivePressed = true
        ret = true
      }
    } else {
      if (latchUntilZero) {
        if (axis.deflection <= 0f) {
          isPositivePressed = false
          ret = true
        }
      } else if (!pastPositiveThreshold) {
        isPositivePressed = false
        ret = true
      }
    }

    if (negativeKeycode != null) {
      val pastNegativeThreshold = axis.deflection <= -TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD
      if (!isNegativePressed) {
        if (pastNegativeThreshold) {
          isNegativePressed = true
          ret = true
        }
      } else {
        if (latchUntilZero) {
          if (axis.deflection >= 0f) {
            isNegativePressed = false
            ret = true
          }
        } else if (!pastNegativeThreshold) {
          isNegativePressed = false
          ret = true
        }
      }
    }

    return ret
  }

  internal companion object {
    private const val TAG = "ButtonAxis"

    @VisibleForTesting internal const val TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD = 0.8f
  }
}
