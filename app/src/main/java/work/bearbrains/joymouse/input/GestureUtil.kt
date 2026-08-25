package work.bearbrains.joymouse.input

import android.accessibilityservice.GestureDescription
import android.view.ViewConfiguration
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** Provides utilities for the construction of [GestureDescription]s. */
class GestureUtil(
  viewConfiguration: ViewConfiguration,
  /**
   * The maximum duration of a gesture. Generally reported by
   * [GestureDescription.getMaxGestureDuration()].
   */
  val maxGestureDuration: Duration,
) {
  // Fling velocity in pixels / second
  private val scaledMinimumFlingVelocity = viewConfiguration.scaledMinimumFlingVelocity
  private val minFlingVelocityMillisecondsPerPixel = (1f / scaledMinimumFlingVelocity) * 1000f

  private val scaledMaximumFlingVelocity = viewConfiguration.scaledMaximumFlingVelocity
  private val maxFlingVelocityMillisecondsPerPixel = (1f / scaledMaximumFlingVelocity) * 1000f

  /** The maximum duration above which a tap gesture is considered a long press. */
  val longTouchThreshold: Duration = ViewConfiguration.getLongPressTimeout().milliseconds

  /**
   * Returns the duration for a gesture between the given points to be considered a drag action and
   * not a fling.
   */
  fun dragTimeBetween(startX: Float, startY: Float, endX: Float, endY: Float): Duration {
    val dist = distance(startX, startY, endX, endY)
    val minFlingTimeMillis = (dist * minFlingVelocityMillisecondsPerPixel).toLong()
    return (minFlingTimeMillis + 10L).milliseconds.coerceIn(1.milliseconds, maxGestureDuration)
  }

  /**
   * Returns the duration for a gesture between the given points to be considered a fling action.
   */
  fun flingTimeBetween(startX: Float, startY: Float, endX: Float, endY: Float): Duration {
    val dist = distance(startX, startY, endX, endY)
    val flingTimeMillis = (dist * maxFlingVelocityMillisecondsPerPixel).toLong()
    return flingTimeMillis.milliseconds.coerceIn(1.milliseconds, maxGestureDuration)
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
