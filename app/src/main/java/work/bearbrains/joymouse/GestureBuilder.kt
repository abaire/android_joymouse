package work.bearbrains.joymouse

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log

/** Provides functionality to compile a gesture over the course of some number of events. */
internal class GestureBuilder(state: CursorState) {
  private var gestureStartTimestamp = System.nanoTime()
  private var lastEventTimestamp = gestureStartTimestamp
  private var lastEventX = state.pointerX
  private var lastEventY = state.pointerY

  private val builder = GestureDescription.Builder()

  /** Constructs a [GestureDescription] from the compiled state events. */
  fun build(): GestureDescription = builder.build()

  /** Mark the gesture as completed. */
  fun endGesture(state: CursorState) {
    val now = System.nanoTime()

    val startTime = 0L
    val endTime =
      if (isDrag(state)) {
        DRAG_GESTURE_DURATION_MILLISECONDS
      } else {
        now.toGestureTimeMillis().coerceIn(1, MAX_GESTURE_DURATION_MILLISECONDS)
      }

    Log.d(
      TAG,
      "Ending gesture with segment ${lastEventX}, ${lastEventY} to ${state.pointerX}, ${state.pointerY} from ${startTime} ms to ${endTime} ms"
    )

    builder.addStroke(
      GestureDescription.StrokeDescription(state.pathTo(), startTime, endTime - startTime)
    )

    lastEventTimestamp = now
    lastEventX = state.pointerX
    lastEventY = state.pointerY
  }

  /** Report a cursor move event. */
  fun cursorMove(state: CursorState) {}

  private fun isDrag(state: CursorState): Boolean {
    val dX = state.pointerX - lastEventX
    val dY = state.pointerY - lastEventY
    val distanceSquared = dX * dX + dY * dY

    return distanceSquared >= MIN_DRAG_DISTANCE_SQUARED
  }

  /** Converts a nanosecond timestamp to a millisecond offset from the start of the gesture. */
  private fun Long.toGestureTimeMillis(): Long {
    val deltaNanos = this - gestureStartTimestamp
    return deltaNanos / 1_000_000
  }

  private fun CursorState.pathTo(): Path =
    Path().apply {
      moveTo(lastEventX, lastEventY)
      lineTo(pointerX, pointerY)
    }

  companion object {
    private const val TAG = "GestureBuilder"

    const val MIN_DRAG_DISTANCE = 20f
    private const val MIN_DRAG_DISTANCE_SQUARED = MIN_DRAG_DISTANCE * MIN_DRAG_DISTANCE
    const val DRAG_GESTURE_DURATION_MILLISECONDS = 30L

    val MAX_GESTURE_DURATION_MILLISECONDS = GestureDescription.getMaxGestureDuration()
  }
}
