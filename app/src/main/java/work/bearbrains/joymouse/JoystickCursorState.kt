package work.bearbrains.joymouse

import android.view.MotionEvent

/** Encapsulates state for a virtual cursor that is controlled by axes from [MotionEvent]s. */
interface JoystickCursorState {

  /** Logical actions that may be emitted by a JoystickCursor */
  enum class Action {
    BACK,
    HOME,
    RECENTS,
    DPAD_UP,
    DPAD_DOWN,
    DPAD_LEFT,
    DPAD_RIGHT,
    ACTIVATE,
    SWIPE_UP,
    SWIPE_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT,
  }

  /** Whether or not this virtual cursor is enabled or has been toggled off via a button chord. */
  val isEnabled: Boolean

  /** The current X coordinate of the pointer (in pixels) */
  val pointerX: Float

  /** The current Y coordinate of the pointer (in pixels) */
  val pointerY: Float

  /** Indicates that the primary virtual mouse button is currently pressed. */
  val isPrimaryButtonPressed: Boolean

  /** Indicates that the cursor speed should be set to "fast". */
  val isFastCursorEnabled: Boolean

  /** Stops repeating events for this state. */
  fun cancelRepeater()

  /**
   * Updates the stored xDeflection and yDeflection with the contents of the given [MotionEvent].
   */
  fun update(event: MotionEvent)

  /** Applies the last calculated deflection values. */
  fun applyDeflection()

  /** Processes a press/release event. Returns true if the event was consumed. */
  fun handleButtonEvent(isDown: Boolean, keyCode: Int): Boolean
}
