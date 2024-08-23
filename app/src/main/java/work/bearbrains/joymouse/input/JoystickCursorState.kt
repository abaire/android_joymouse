package work.bearbrains.joymouse.input

import android.view.MotionEvent
import java.io.Closeable
import work.bearbrains.joymouse.DisplayInfo

/** Encapsulates state for a virtual cursor that is controlled by axes from [MotionEvent]s. */
interface JoystickCursorState : Closeable {
  /** The ID of the physical device that this repeater is associated with. */
  val deviceId: Int

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

  /** The display on which this cursor is currently attached. */
  val displayInfo: DisplayInfo

  /** Stops repeating events for this state. */
  fun cancelRepeater()

  /**
   * Updates the stored xDeflection and yDeflection with the contents of the given [MotionEvent].
   */
  fun update(event: MotionEvent)

  /** Processes a press/release event. Returns true if the event was consumed. */
  fun handleButtonEvent(isDown: Boolean, keyCode: Int)
}
