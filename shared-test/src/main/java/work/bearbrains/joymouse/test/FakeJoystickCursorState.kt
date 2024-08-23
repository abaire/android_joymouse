package work.bearbrains.joymouse.test

import android.view.MotionEvent
import work.bearbrains.joymouse.DisplayInfo
import work.bearbrains.joymouse.input.JoystickCursorState

class FakeJoystickCursorState(
  override val displayInfo: DisplayInfo,
  override val deviceId: Int = 0,
  override var isEnabled: Boolean = true,
  override var pointerX: Float = 0f,
  override var pointerY: Float = 0f,
  override var isPrimaryButtonPressed: Boolean = false,
  override var isFastCursorEnabled: Boolean = false,
) : JoystickCursorState {

  override fun close() {}

  override fun cancelRepeater() {}

  override fun update(event: MotionEvent) {}

  override fun handleButtonEvent(isDown: Boolean, keyCode: Int) {}
}
