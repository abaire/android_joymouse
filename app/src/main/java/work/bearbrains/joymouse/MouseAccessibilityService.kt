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
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/** Handles conversion of joystick input events to motion eventsevents. */
class MouseAccessibilityService : AccessibilityService(), InputManager.InputDeviceListener {
  private var joystickDeviceIdsToState = mutableMapOf<Int, CursorState>()
  private val handler = Handler(Looper.getMainLooper())

  private var windowWidth = 0f
  private var windowHeight = 0f

  private val tapTimeout = ViewConfiguration.getTapTimeout()

  private val eventRepeater =
      object : Runnable {
        private val REPEAT_DELAY_MILLISECONDS = 10L

        /** The [CursorState] that should be reapplied by this repeater. */
        var state: CursorState? = null

        /** The ID of the physical device that this repeater is associated with. */
        var deviceId: Int? = null

        override fun run() {
          state?.let {
            it.applyDeflection()
            updateCursor(it)
            restart()
          }
        }

        /** Queues this repeater for future processing. */
        fun restart() {
          handler.postDelayed(this, REPEAT_DELAY_MILLISECONDS)
        }

        /** Cancels any pending runs for this repeater. */
        fun cancel() {
          handler.removeCallbacks(this)
        }
      }

  override fun onServiceConnected() {
    val info = serviceInfo
    info.motionEventSources = SOURCE_JOYSTICK
    serviceInfo = info

    measureDisplays()

    val inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
    inputManager.registerInputDeviceListener(this, null)
    detectJoystickDevices(inputManager)
  }

  override fun onUnbind(intent: Intent?): Boolean {
    val inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
    inputManager.unregisterInputDeviceListener(this)
    return super.onUnbind(intent)
  }

  override fun onKeyEvent(event: KeyEvent?): Boolean {
    if (event == null) {
      return super.onKeyEvent(event)
    }

    val state = joystickDeviceIdsToState.get(event.deviceId)
    if (state == null) {
      return super.onKeyEvent(event)
    }

    // TODO: Look for the enable/disable chord.
    println("!!! onKeyEvent $event")

    // private void tap(PointF point) {
    //          StrokeDescription tap =  new StrokeDescription(path(point), 0,
    //          ViewConfiguration.getTapTimeout());
    //          GestureDescription.Builder builder = new GestureDescription.Builder();
    //          builder.addStroke(tap);
    //          dispatchGesture(builder.build(), null, null);
    //      }

    return super.onKeyEvent(event)
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

  override fun onMotionEvent(event: MotionEvent) {
    val state = joystickDeviceIdsToState.get(event.deviceId)
    if (state == null) {
      return
    }

    state.update(event)
    updateCursor(state)

    if (state.hasDeflection) {
      eventRepeater.state = state
      // TODO: Properly support multiple devices.
      eventRepeater.deviceId = event.deviceId
      eventRepeater.restart()
    } else {
      eventRepeater.cancel()
    }

    super.onMotionEvent(event)
  }

  private fun updateCursor(state: CursorState) {

    // TODO: Only inject a gesture if the trigger is currently held down
    injectMoveGesture(state)
  }

  private fun injectMoveGesture(state: CursorState) {
    val path = state.createGesturePath()

    // TODO: Have the duration utilize the time since the last motion event?
    val durationMilliseconds = 10L

    // TODO: Only mark the gesture as willContinue if the trigger is still held
    val willContinue = true
    val stroke = GestureDescription.StrokeDescription(path, 0, durationMilliseconds, willContinue)

    val gestureBuilder = GestureDescription.Builder()
    gestureBuilder.addStroke(stroke)

    val wasDispatched =
        dispatchGesture(
            gestureBuilder.build(),
            object : GestureResultCallback() {
              override fun onCompleted(gestureDescription: GestureDescription) {}

              override fun onCancelled(gestureDescription: GestureDescription) {}
            },
            handler)

    if (!wasDispatched) {
      println("!!! Error: dispatchGesture failed!")
    }
  }

  override fun onInterrupt() {}

  override fun onInputDeviceAdded(deviceId: Int) {
    val inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
    inputManager.getInputDevice(deviceId)?.let { device ->
      if (!device.isJoystick) {
        return@let
      }
      joystickDeviceIdsToState[deviceId] =
          CursorState.create(device, X_AXIS, Y_AXIS, windowWidth, windowHeight)
    }
  }

  override fun onInputDeviceRemoved(deviceId: Int) {
    if (eventRepeater.deviceId == deviceId) {
      eventRepeater.cancel()
    }

    joystickDeviceIdsToState.remove(deviceId)
  }

  override fun onInputDeviceChanged(deviceId: Int) {
    println("!!! onInputDeviceChanged ${deviceId}")
  }

  private fun measureDisplays() {
    val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    windowWidth = windowManager.maximumWindowMetrics.bounds.width().toFloat()
    windowHeight = windowManager.maximumWindowMetrics.bounds.height().toFloat()

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
    joystickDeviceIdsToState.clear()

    for (deviceId in inputManager.inputDeviceIds) {
      inputManager.getInputDevice(deviceId)?.let { device ->
        if (!device.isJoystick) {
          return@let
        }
        joystickDeviceIdsToState[deviceId] =
            CursorState.create(device, X_AXIS, Y_AXIS, windowWidth, windowHeight)
      }
    }
  }

  private companion object {
    val X_AXIS = MotionEvent.AXIS_Z
    val Y_AXIS = MotionEvent.AXIS_RZ
  }
}

private fun CursorState.createGesturePath(): Path {
  return Path().apply {
    moveTo(lastPointerX, lastPointerY)
    lineTo(pointerX, pointerY)
  }
}

private val InputDevice.isJoystick: Boolean
  get() = isExternal && isEnabled && supportsSource(SOURCE_JOYSTICK)
