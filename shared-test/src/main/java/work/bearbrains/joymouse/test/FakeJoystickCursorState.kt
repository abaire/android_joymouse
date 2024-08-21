package work.bearbrains.joymouse.test

import android.view.MotionEvent
import work.bearbrains.joymouse.JoystickCursorState

class FakeJoystickCursorState(
  override var isEnabled: Boolean = true,
  override var pointerX: Float = 0f,
  override var pointerY: Float = 0f,
  override var isPrimaryButtonPressed: Boolean = false,
  override var isFastCursorEnabled: Boolean = false,
) : JoystickCursorState {

  override fun cancelRepeater() {}

  override fun update(event: MotionEvent) {}

  override fun applyDeflection() {}

  override fun handleButtonEvent(isDown: Boolean, keyCode: Int): Boolean {
    return false
  }
}
