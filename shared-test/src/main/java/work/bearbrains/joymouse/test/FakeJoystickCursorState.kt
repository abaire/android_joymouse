package work.bearbrains.joymouse.test

import android.view.MotionEvent
import work.bearbrains.joymouse.DisplayInfo
import work.bearbrains.joymouse.input.JoystickCursorState

class FakeJoystickCursorState(
  override var displayInfo: DisplayInfo,
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

  override fun updateDisplayInfo(newDisplayInfo: DisplayInfo) {
    val oldWidth = displayInfo.windowWidth
    val oldHeight = displayInfo.windowHeight

    displayInfo = newDisplayInfo

    val relX = if (oldWidth > 0f) pointerX / oldWidth else 0.5f
    val relY = if (oldHeight > 0f) pointerY / oldHeight else 0.5f

    pointerX = (relX * newDisplayInfo.windowWidth).coerceIn(0f, newDisplayInfo.windowWidth)
    pointerY = (relY * newDisplayInfo.windowHeight).coerceIn(0f, newDisplayInfo.windowHeight)
  }

  override fun updateActionConfig(config: work.bearbrains.joymouse.input.ActionConfig) {}
}
