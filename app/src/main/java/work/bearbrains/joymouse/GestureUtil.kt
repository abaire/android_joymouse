package work.bearbrains.joymouse

import android.accessibilityservice.GestureDescription
import android.view.ViewConfiguration
import kotlin.math.max
import kotlin.math.sqrt

/** Provides utilities for the construction of [GestureDescription]s. */
class GestureUtil(
  viewConfiguration: ViewConfiguration,
  /**
   * The maximum duration of a gesture, in milliseconds. Generally reported by
   * [GestureDescription.getMaxGestureDuration()].
   */
  val maxGestureDuration: Long,
) {
  // Fling velocity in pixels / second
  private val scaledMinimumFlingVelocity = viewConfiguration.scaledMinimumFlingVelocity
  private val minFlingVelocityMillisecondsPerPixel = (1f / scaledMinimumFlingVelocity) * 1000f

  private val scaledMaximumFlingVelocity = viewConfiguration.scaledMaximumFlingVelocity
  private val maxFlingVelocityMillisecondsPerPixel = (1f / scaledMaximumFlingVelocity) * 1000f

  /**
   * The maximum duration, in milliseconds, above which a tap gesture is considered a long press.
   */
  val longTouchThresholdMilliseconds = ViewConfiguration.getLongPressTimeout().toLong()

  /**
   * Returns the time in milliseconds for a gesture between the given points to be considered a drag
   * action and not a fling.
   */
  fun dragTimeBetween(startX: Float, startY: Float, endX: Float, endY: Float): Long {
    val dist = distance(startX, startY, endX, endY)
    val minFlingTime = (dist * minFlingVelocityMillisecondsPerPixel).toLong()
    return max(minFlingTime - 10, 1)
  }

  /**
   * Returns the time in milliseconds for a gesture between the given points to be considered a
   * fling action.
   */
  fun flingTimeBetween(startX: Float, startY: Float, endX: Float, endY: Float): Long {
    val dist = distance(startX, startY, endX, endY)
    return max((dist * maxFlingVelocityMillisecondsPerPixel).toLong(), 1L)
  }

  companion object {
    /** Provides the distance between two 2D points. */
    fun distance(startX: Float, startY: Float, endX: Float, endY: Float): Float {
      return sqrt(distanceSquared(startX, startY, endX, endY))
    }

    /** Provides the squared distance between two 2D points. */
    fun distanceSquared(startX: Float, startY: Float, endX: Float, endY: Float): Float {
      val dX = startX - endX
      val dY = startY - endY
      return dX * dX + dY * dY
    }
  }
}
