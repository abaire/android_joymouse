package work.bearbrains.joymouse.impl

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import work.bearbrains.joymouse.GestureBuilder
import work.bearbrains.joymouse.GestureUtil
import work.bearbrains.joymouse.JoystickCursorState
import work.bearbrains.joymouse.NanoClock

/** Provides functionality to compile a gesture over the course of some number of events. */
internal class GestureBuilderImpl(
  initialState: JoystickCursorState,
  private val gestureUtil: GestureUtil,
  private val clock: NanoClock,
) : GestureBuilder {

  private var gestureStartTimestamp = clock.nanoTime()
  private var lastEventTimestamp = gestureStartTimestamp
  private val initialX = initialState.pointerX
  private val initialY = initialState.pointerY
  private var lastEventX = initialX
  private var lastEventY = initialY

  private val builder =
    GestureDescription.Builder().setDisplayId(initialState.displayInfo.displayId)

  private var _action = GestureBuilder.Action.TOUCH
  /** The logical action represented by this gesture. */
  override val action: GestureBuilder.Action
    get() {
      if (_action == GestureBuilder.Action.TOUCH) {
        val elapsedMilliseconds = clock.nanoTime().toGestureTimeMillis()
        if (elapsedMilliseconds >= gestureUtil.longTouchThresholdMilliseconds) {
          _action = GestureBuilder.Action.LONG_TOUCH
        }
      }

      return _action
    }

  /** Constructs a [GestureDescription] from the compiled state events. */
  override fun build(): GestureDescription = builder.build()

  /** Mark the gesture as completed. */
  override fun endGesture(state: JoystickCursorState) {
    val now = clock.nanoTime()

    val startTime = 0L
    val endTime =
      when (toMotionAction(state)) {
        GestureBuilder.Action.DRAG ->
          gestureUtil.dragTimeBetween(initialX, initialY, state.pointerX, state.pointerY)
        GestureBuilder.Action.FLING ->
          gestureUtil.flingTimeBetween(initialX, initialY, state.pointerX, state.pointerY)
        else -> now.toGestureTimeMillis().coerceIn(1, gestureUtil.maxGestureDuration)
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
  override fun cursorMove(state: JoystickCursorState) {
    _action = toMotionAction(state)
  }

  private fun toMotionAction(state: JoystickCursorState): GestureBuilder.Action {
    val distanceSquared =
      GestureUtil.distanceSquared(lastEventX, lastEventY, state.pointerX, state.pointerY)
    if (distanceSquared >= MIN_FLING_DISTANCE_SQUARED) {
      return GestureBuilder.Action.FLING
    }

    if (distanceSquared >= MIN_DRAG_DISTANCE_SQUARED) {
      return GestureBuilder.Action.DRAG
    }

    return GestureBuilder.Action.TOUCH
  }

  /** Converts a nanosecond timestamp to a millisecond offset from the start of the gesture. */
  private fun Long.toGestureTimeMillis(): Long {
    val deltaNanos = this - gestureStartTimestamp
    return deltaNanos / 1_000_000
  }

  private fun JoystickCursorState.pathTo(): Path =
    Path().apply {
      moveTo(lastEventX, lastEventY)
      lineTo(pointerX, pointerY)
    }

  companion object {
    private const val TAG = "GestureBuilder"

    private const val MIN_DRAG_DISTANCE_SQUARED =
      GestureBuilder.MIN_DRAG_DISTANCE * GestureBuilder.MIN_DRAG_DISTANCE
    private const val MIN_FLING_DISTANCE_SQUARED =
      GestureBuilder.MIN_FLING_DISTANCE * GestureBuilder.MIN_FLING_DISTANCE
  }
}
