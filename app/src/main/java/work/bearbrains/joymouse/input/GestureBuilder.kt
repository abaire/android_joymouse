package work.bearbrains.joymouse.input

import android.accessibilityservice.GestureDescription

/** Provides functionality to compile a gesture over the course of some number of events. */
interface GestureBuilder {

  /** Logical gesture actions. */
  enum class Action {
    TOUCH,
    LONG_TOUCH,
    DRAG,
    FLING,
  }

  /** The logical action represented by this gesture. */
  val action: Action

  /** Constructs a [GestureDescription] from the compiled state events. */
  fun build(): GestureDescription

  /** Mark the gesture as completed. */
  fun endGesture(state: JoystickCursorState)

  /** Report a cursor move event. */
  fun cursorMove(state: JoystickCursorState)

  /** Forces a drag gesture to be treated as a fling. */
  var dragIsFling: Boolean

  companion object {
    /**
     * The minimum displacement from the start of a virtual click before the joystick cursor
     * movement will be treated as a drag action.
     */
    const val MIN_DRAG_DISTANCE = 20f

    /**
     * The minimum displacement from the start of a virtual click before the joystick cursor
     * movement will be treated as a fling action.
     */
    const val MIN_FLING_DISTANCE = 150f
  }
}
