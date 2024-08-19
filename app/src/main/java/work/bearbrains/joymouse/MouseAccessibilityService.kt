package work.bearbrains.joymouse

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.InputDevice
import android.view.InputDevice.SOURCE_JOYSTICK
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.ImageView
import androidx.core.content.ContextCompat

/** Handles conversion of joystick input events to motion eventsevents. */
class MouseAccessibilityService : AccessibilityService(), InputManager.InputDeviceListener {
  private var joystickDeviceIdsToState = mutableMapOf<Int, CursorState>()
  private val handler = Handler(Looper.getMainLooper())

  private var windowWidth = 0f
  private var windowHeight = 0f

  private val tapTimeout = ViewConfiguration.getTapTimeout()

  var cursorView: CursorView? = null
  val cursorDisplayTimeoutMilliseconds = 1500L

  private val cursorHider =
    object : Runnable {
      override fun run() {
        Log.d(TAG, "Hiding cursor due to inactivity")
        cursorView?.hideCursor()
      }

      /** Queues this repeater for future processing. */
      fun restart() {
        cancel()
        handler.postDelayed(this, cursorDisplayTimeoutMilliseconds)
      }

      /** Cancels any pending runs for this repeater. */
      fun cancel() {
        handler.removeCallbacks(this)
      }
    }

  override fun onServiceConnected() {
    cursorView =
      CursorView(
        ImageView(this).apply {
          val drawable =
            ContextCompat.getDrawable(this@MouseAccessibilityService, R.drawable.mouse_cursor)
          setImageDrawable(drawable)

          fitsSystemWindows = false
        },
        getSystemService(Context.WINDOW_SERVICE) as WindowManager
      )

    val info = serviceInfo
    info.motionEventSources = SOURCE_JOYSTICK
    serviceInfo = info

    measureDisplays()

    val inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
    inputManager.registerInputDeviceListener(this, null)
    detectJoystickDevices(inputManager)

    if (joystickDeviceIdsToState.isNotEmpty()) {
      updateCursorPosition(joystickDeviceIdsToState.values.first())
    }
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

    val isPress = event.action == KeyEvent.ACTION_DOWN
    return state.handleButtonEvent(isPress, event.keyCode)
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

  override fun onMotionEvent(event: MotionEvent) {
    val state = joystickDeviceIdsToState.get(event.deviceId)
    if (state == null) {
      return
    }

    state.update(event)

    super.onMotionEvent(event)
  }

  private fun updateCursorPosition(state: CursorState) {
    cursorView?.apply {
      updatePosition(state.pointerX, state.pointerY)
      cursorHider.restart()
      show()
    }

    injectMoveGesture(state)
  }

  private fun onUpdatePrimaryButton(state: CursorState) {
    injectMoveGesture(state, isButtonUpdate = true)
  }

  private fun injectMoveGesture(state: CursorState, isButtonUpdate: Boolean = false) {
    if (!state.isPrimaryButtonPressed && !isButtonUpdate) {
      return
    }

    val path = state.createGesturePath()

    // TODO: Have the duration utilize the time since the last motion event?
    val durationMilliseconds = 10L
    val stroke =
      GestureDescription.StrokeDescription(
        path,
        0,
        durationMilliseconds,
        state.isPrimaryButtonPressed,
      )

    val gestureBuilder = GestureDescription.Builder()
    gestureBuilder.addStroke(stroke)

    val wasDispatched =
      dispatchGesture(
        gestureBuilder.build(),
        object : GestureResultCallback() {
          override fun onCompleted(gestureDescription: GestureDescription) {}

          override fun onCancelled(gestureDescription: GestureDescription) {}
        },
        handler
      )

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
      addJoystickDevice(device)
    }
  }

  override fun onInputDeviceRemoved(deviceId: Int) {
    joystickDeviceIdsToState.get(deviceId)?.cancelRepeater()
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
        addJoystickDevice(device)
      }
    }
  }

  private fun addJoystickDevice(device: InputDevice) {
    joystickDeviceIdsToState[device.id] =
      CursorState.create(
        device,
        handler,
        X_AXIS,
        Y_AXIS,
        windowWidth,
        windowHeight,
        ::updateCursorPosition,
        ::onUpdatePrimaryButton,
      ) { state ->
        if (!state.isEnabled) {
          cursorView?.hideCursor()
          cursorHider.cancel()
        } else {
          updateCursorPosition(state)
        }
      }
  }

  private companion object {
    const val TAG = "MouseAccessibilityService"

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
