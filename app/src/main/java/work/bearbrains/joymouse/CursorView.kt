package work.bearbrains.joymouse

import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/** Handles rendering a mouse cursor. */
class CursorView(private val view: View, private val windowManager: WindowManager) {
  private var isShown = view.parent != null

  private var xPosition: Float = 0f
  private var yPosition: Float = 0f

  /** Shows the cursor view. */
  fun show() {
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

  /** Updates the position of the cursor. */
  fun updatePosition(x: Float, y: Float) {
    xPosition = x
    yPosition = y

    if (isShown) {
      val params = view.layoutParams as WindowManager.LayoutParams
      params.x = xPosition.toInt()
      params.y = yPosition.toInt()

      windowManager.updateViewLayout(view, params)
    }
  }

  /** Hides the cursor. */
  fun hideCursor() {
    if (isShown) {
      return
    }

    isShown = false
    view.let { (it.parent as? WindowManager)?.removeView(it) }
  }
}
