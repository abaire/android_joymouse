package work.bearbrains.joymouse

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/** Handles conversion of joystick input events to motion eventsevents. */
class MouseAccessibilityService : AccessibilityService() {

  private val handler = Handler(Looper.getMainLooper())

  override fun onKeyEvent(event: KeyEvent?): Boolean {
    println("!!! onKeyEvent $event")
    if (event?.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && event.action == KeyEvent.ACTION_DOWN) {
      injectMouseMove(5f, 0f) // Move 5 pixels to the right
      return true // Consume the event
    }
    return super.onKeyEvent(event)
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    // ... (Handle joystick input events here)

    println("!!! onAccessibilityEvent $event")

    // Calculate new mouse cursor position (relativeX, relativeY)
    val relativeX = 0.5f
    val relativeY = 0.5f

    // Inject mouse move event
//    injectMouseMove(relativeX, relativeY)
  }

  private fun injectMouseMove(relativeX: Float, relativeY: Float) {
    println("!!! injectMouseMove: $relativeX $relativeY")
    val path = Path()
    path.moveTo(0f, 0f) // Start at current position
    path.lineTo(relativeX, relativeY) // Move relative to current position

    val stroke = GestureDescription.StrokeDescription(path, 0, 50) // 50ms duration

    val gestureBuilder = GestureDescription.Builder()
    gestureBuilder.addStroke(stroke)

    dispatchGesture(
        gestureBuilder.build(),
        object : GestureResultCallback() {
          override fun onCompleted(gestureDescription: GestureDescription) {
            // Mouse move completed successfully
          }

          override fun onCancelled(gestureDescription: GestureDescription) {
            // Mouse move was cancelled
          }
        },
        handler)
  }

  override fun onInterrupt() {}
}
