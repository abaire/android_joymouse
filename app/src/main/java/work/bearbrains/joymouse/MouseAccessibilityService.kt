package work.bearbrains.joymouse

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.InputDevice.SOURCE_JOYSTICK
import android.view.InputDevice.SOURCE_UNKNOWN
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.MotionEvent.PointerCoords
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/** Handles conversion of joystick input events to motion eventsevents. */
class MouseAccessibilityService : AccessibilityService(), InputManager.InputDeviceListener {

  private var joystickDeviceIds = mutableSetOf<Int>()
  private val handler = Handler(Looper.getMainLooper())

  private var pointerX = 0f
  private var pointerY = 0f
  private var windowWidth = 0
  private var windowHeight = 0

  private val motionEventData = PointerCoords()

  private val tapTimeout = ViewConfiguration.getTapTimeout()

  override fun onServiceConnected() {
    val info = serviceInfo
    info.motionEventSources = SOURCE_JOYSTICK or SOURCE_UNKNOWN
    serviceInfo = info

    measureDisplays()

    val inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
    inputManager.registerInputDeviceListener(this, null)
    detectJoystickDevices(inputManager)

    pointerX = windowWidth * 0.5f
    pointerY = windowHeight * 0.5f
  }

  override fun onUnbind(intent: Intent?): Boolean {
    val inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
    inputManager.unregisterInputDeviceListener(this)
    return super.onUnbind(intent)
  }

  override fun onKeyEvent(event: KeyEvent?): Boolean {
    if (event == null || !joystickDeviceIds.contains(event.deviceId)) {
      return super.onKeyEvent(event)
    }

    // TODO: Look for the enable/disable chord.
    println("!!! onKeyEvent $event")
    return super.onKeyEvent(event)
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

  override fun onMotionEvent(event: MotionEvent) {
    if (!joystickDeviceIds.contains(event.deviceId)) {
      return
    }

    val xAxis = MotionEvent.AXIS_Z
    val yAxis = MotionEvent.AXIS_RZ

    val xDeflection = event.getAxisValue(xAxis)
    val yDeflection = event.getAxisValue(yAxis)

    println("!!! Joystick ${xDeflection}, ${yDeflection}")

    // TODO: Apply velocity/acceleration
    val dX = xDeflection
    val dY = yDeflection

    // TODO: Simulate repeat events

    // TODO: Only inject a gesture if the trigger is currently held down
    injectMouseMove(xDeflection, yDeflection)

    pointerX += dX
    pointerY += dY

    super.onMotionEvent(event)
  }

  private fun injectMouseMove(relativeX: Float, relativeY: Float) {
    println("!!! injectMouseMove: $relativeX $relativeY")
    val path = Path()

    path.moveTo(pointerX, pointerY)
    path.lineTo(pointerX + relativeX, pointerY + relativeY)

    // TODO: Have the duration be the time since the last motion event?
    val durationMilliseconds = 50L
    // TODO: Mark the gesture as willContinue if the trigger is still held
    val willContinue = true
    val stroke = GestureDescription.StrokeDescription(path, 0, durationMilliseconds, willContinue) // 50ms duration

    val gestureBuilder = GestureDescription.Builder()
    gestureBuilder.addStroke(stroke)

    println("Dispatch gesture $gestureBuilder")
    val wasDispatched =
        dispatchGesture(
            gestureBuilder.build(),
            object : GestureResultCallback() {
              override fun onCompleted(gestureDescription: GestureDescription) {}

              override fun onCancelled(gestureDescription: GestureDescription) {
                println("!!! Gesture cancelled! ${gestureDescription}")
              }
            },
            handler)

    if (!wasDispatched) {
      println("!!! Error: dispatchGesture failed!")
    }

    // private void tap(PointF point) {
    //          StrokeDescription tap =  new StrokeDescription(path(point), 0,
    //          ViewConfiguration.getTapTimeout());
    //          GestureDescription.Builder builder = new GestureDescription.Builder();
    //          builder.addStroke(tap);
    //          dispatchGesture(builder.build(), null, null);
    //      }
  }

  override fun onInterrupt() {}

  override fun onInputDeviceAdded(deviceId: Int) {
    val inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
    inputManager.getInputDevice(deviceId)?.let { device ->
      if (!device.isJoystick) {
        return@let
      }
      joystickDeviceIds.add(deviceId)
    }
  }

  override fun onInputDeviceRemoved(deviceId: Int) {
    joystickDeviceIds.remove(deviceId)
  }

  override fun onInputDeviceChanged(deviceId: Int) {
    println("!!! onInputDeviceChanged ${deviceId}")
  }

  private fun measureDisplays() {
    val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    windowWidth = windowManager.maximumWindowMetrics.bounds.width()
    windowHeight = windowManager.maximumWindowMetrics.bounds.height()

    //    val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    //
    //    for (display in displayManager.displays) {
    //      // Do something with each display
    //      val displayId = display.displayId
    //      val displayName = display.name
    //      display.getRealSize()
    //      // ...
    //    }
  }

  private fun detectJoystickDevices(inputManager: InputManager) {
    joystickDeviceIds.clear()

    for (deviceId in inputManager.inputDeviceIds) {
      inputManager.getInputDevice(deviceId)?.let { device ->
        if (!device.isJoystick) {
          return@let
        }
        joystickDeviceIds.add(deviceId)
      }
    }
  }
}

private val InputDevice.isJoystick: Boolean
  get() = isExternal && isEnabled && supportsSource(SOURCE_JOYSTICK)
