package work.bearbrains.joymouse.impl

import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import work.bearbrains.joymouse.CursorView

/** Handles rendering a mouse cursor. */
class CursorViewImpl(private val view: View, private val windowManager: WindowManager) :
  CursorView {

  private var isShown = view.parent != null

  private var xPosition: Float = 0f
  private var yPosition: Float = 0f

  override fun show() {
    if (isShown) {
      return
    }

    val params =
      WindowManager.LayoutParams().apply {
        type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        format = PixelFormat.TRANSLUCENT
        flags =
          WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
        gravity = Gravity.TOP or Gravity.START
        x = xPosition.toInt()
        y = yPosition.toInt()
      }

    windowManager.addView(view, params)
    isShown = true
  }

  override fun updatePosition(x: Float, y: Float) {
    xPosition = x
    yPosition = y

    if (isShown) {
      val params = view.layoutParams as WindowManager.LayoutParams
      params.x = xPosition.toInt()
      params.y = yPosition.toInt()

      windowManager.updateViewLayout(view, params)
    }
  }

  override fun hideCursor() {
    if (!isShown) {
      return
    }

    isShown = false
    view.let { windowManager.removeView(it) }
  }

  override var cursorState: CursorView.State
    get() {
      var value = if (view.isEnabled) 0 else 1
      value += if (view.isPressed) 2 else 0
      value += if (view.isSelected) 4 else 0

      return CursorView.State.entries[value]
    }
    set(value) {
      val bits = CursorView.State.entries.indexOf(value)
      view.isEnabled = (bits and 1) == 0
      view.isPressed = (bits and 2) != 0
      view.isSelected = (bits and 4) != 0
    }
}
