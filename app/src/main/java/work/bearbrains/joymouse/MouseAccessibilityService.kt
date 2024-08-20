package work.bearbrains.joymouse

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Path
import android.graphics.PorterDuff
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
import kotlin.math.absoluteValue

/** Handles conversion of joystick input events to motion eventsevents. */
class MouseAccessibilityService : AccessibilityService(), InputManager.InputDeviceListener {
  private var joystickDeviceIdsToState = mutableMapOf<Int, CursorState>()
  private val handler = Handler(Looper.getMainLooper())

  private var windowWidth = 0f
  private var windowHeight = 0f

  private val tapTimeout = ViewConfiguration.getTapTimeout()

  var cursorView: CursorView? = null
  val cursorDisplayTimeoutMilliseconds = 1500L

  private var activeGestureBuilder: GestureBuilder? = null

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
    val cursorIcon =
      ImageView(this).apply {
        val drawable =
          ContextCompat.getDrawable(this@MouseAccessibilityService, R.drawable.mouse_cursor)
        setImageDrawable(drawable)

        fitsSystemWindows = false

        imageTintMode = PorterDuff.Mode.MULTIPLY
        imageTintList =
          ColorStateList(
            arrayOf(
              intArrayOf(android.R.attr.state_pressed),
              intArrayOf(),
            ),
            intArrayOf(
              Color.argb(0.65f, 1.0f, 0.4f, 0.6f),
              Color.WHITE,
            ),
          )
      }

    cursorView = CursorView(cursorIcon, getSystemService(Context.WINDOW_SERVICE) as WindowManager)

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

  override fun onConfigurationChanged(newConfig: Configuration) {
    measureDisplays()
    detectJoystickDevices(getSystemService(Context.INPUT_SERVICE) as InputManager)

    super.onConfigurationChanged(newConfig)
  }

  override fun onKeyEvent(event: KeyEvent?): Boolean {
    if (event == null) {
      return super.onKeyEvent(event)
    }

    val state = joystickDeviceIdsToState.get(event.deviceId)
    if (state == null) {
      return super.onKeyEvent(event)
    }

    return state.handleButtonEvent(event.action == KeyEvent.ACTION_DOWN, event.keyCode)
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
    cursorHider.cancel()

    cursorView?.apply {
      updatePosition(state.pointerX, state.pointerY)
      if (!state.isPrimaryButtonPressed) {
        cursorHider.restart()
      }
      show()
    }

    activeGestureBuilder?.cursorMove(state)
  }

  private fun onUpdatePrimaryButton(state: CursorState) {
    updateCursorPosition(state)

    if (state.isPrimaryButtonPressed) {
      activeGestureBuilder = GestureBuilder(state)
      cursorView?.onCursorPressed(true)
    } else {
      cursorView?.onCursorPressed(false)
      activeGestureBuilder?.endGesture(state)
      dispatchPendingGesture()
      cursorHider.restart()
    }
  }

  private fun dispatchGesture(
    gesture: GestureDescription,
    onCompleted: (() -> Unit)? = null,
    numRetries: Int = 0,
  ): Boolean {
    return dispatchGesture(
      gesture,
      object : GestureResultCallback() {
        override fun onCompleted(gestureDescription: GestureDescription) {
          onCompleted?.invoke()
        }

        override fun onCancelled(gestureDescription: GestureDescription) {
          // Gestures are cancelled by arbitrary MotionEvents. This means that an axis button
          // could pass the activation threshold, triggering this method, then emit further events
          // and cancel the gesture.
          if (numRetries > MAX_GESTURE_DISPATCH_RETRIES) {
            Log.d(
              TAG,
              "!!!!\n Gesture cancelled after ${numRetries} retries: ${gestureDescription}\n\n"
            )
            return
          }

          handler.post { dispatchGesture(gesture, onCompleted, numRetries + 1) }
        }
      },
      handler,
    )
  }

  private fun dispatchPendingGesture() {
    activeGestureBuilder?.let {
      val gesture = it.build()

      val wasDispatched = dispatchGesture(gesture)
      if (!wasDispatched) {
        Log.e(TAG, "dispatchGesture failed for ${gesture}!")
      }
    }

    activeGestureBuilder = null
  }

  private fun dispatchSwipe(state: CursorState, dX: Float, dY: Float) {
    val endX = (state.pointerX + dX).coerceIn(0f, windowWidth)
    val endY = (state.pointerY + dY).coerceIn(0f, windowHeight)
    if (
      (endX - state.pointerX).absoluteValue < GestureBuilder.MIN_DRAG_DISTANCE &&
        (endY - state.pointerX).absoluteValue < GestureBuilder.MIN_DRAG_DISTANCE
    ) {
      Log.d(TAG, "Ignoring short swipe: ${state.pointerX}, ${state.pointerY} -> $endX, $endY")
      return
    }

    val builder =
      GestureDescription.Builder().apply {
        addStroke(
          GestureDescription.StrokeDescription(
            Path().apply {
              moveTo(state.pointerX, state.pointerY)
              lineTo(endX, endY)
            },
            1L,
            GestureBuilder.DRAG_GESTURE_DURATION_MILLISECONDS,
            false,
          )
        )
      }
    val wasDispatched = dispatchGesture(builder.build())
    if (!wasDispatched) {
      Log.e(TAG, "dispatchSwipe failed for ${state.pointerX}, ${state.pointerY} -> $endX, $endY")
    }
  }

  private fun onAction(state: CursorState, action: CursorState.Action) {
    when (action) {
      CursorState.Action.SWIPE_UP -> {
        dispatchSwipe(state, 0f, -SWIPE_DISTANCE)
      }
      CursorState.Action.SWIPE_DOWN -> {
        dispatchSwipe(state, 0f, SWIPE_DISTANCE)
      }
      CursorState.Action.SWIPE_LEFT -> {
        dispatchSwipe(state, -SWIPE_DISTANCE, 0f)
      }
      CursorState.Action.SWIPE_RIGHT -> {
        dispatchSwipe(state, SWIPE_DISTANCE, 0f)
      }
      else -> {
        val globalAction = action.toGlobalAction()
        if (globalAction != null) {
          performGlobalAction(globalAction)
        } else {
          Log.e(TAG, "Unexpected Action ${action}")
        }
      }
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
    Log.d(TAG, "Ignoring onInputDeviceChanged for device ID ${deviceId}")
  }

  private fun measureDisplays() {
    val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    windowWidth = windowManager.maximumWindowMetrics.bounds.width().toFloat()
    windowHeight = windowManager.maximumWindowMetrics.bounds.height().toFloat()
  }

  private fun destroyCursors() {
    joystickDeviceIdsToState.forEach { (_, state) -> state.cancelRepeater() }
    joystickDeviceIdsToState.clear()
  }

  private fun detectJoystickDevices(inputManager: InputManager) {
    destroyCursors()

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
        onUpdatePosition = ::updateCursorPosition,
        onUpdatePrimaryButton = ::onUpdatePrimaryButton,
        onAction = ::onAction,
      ) { state ->
        if (!state.isEnabled) {
          cursorView?.hideCursor()
          cursorHider.cancel()
        } else {
          updateCursorPosition(state)
        }
      }
  }

  private fun CursorState.Action.toGlobalAction(): Int? {
    return when (this) {
      CursorState.Action.BACK -> GLOBAL_ACTION_BACK
      CursorState.Action.HOME -> GLOBAL_ACTION_HOME
      CursorState.Action.RECENTS -> GLOBAL_ACTION_RECENTS
      CursorState.Action.DPAD_UP -> GLOBAL_ACTION_DPAD_UP
      CursorState.Action.DPAD_DOWN -> GLOBAL_ACTION_DPAD_DOWN
      CursorState.Action.DPAD_LEFT -> GLOBAL_ACTION_DPAD_LEFT
      CursorState.Action.DPAD_RIGHT -> GLOBAL_ACTION_DPAD_RIGHT
      CursorState.Action.ACTIVATE -> GLOBAL_ACTION_DPAD_CENTER
      else -> null
    }
  }

  private companion object {
    const val TAG = "MouseAccessibilityService"

    val X_AXIS = MotionEvent.AXIS_Z
    val Y_AXIS = MotionEvent.AXIS_RZ

    val MAX_GESTURE_DISPATCH_RETRIES = 15

    const val SWIPE_DISTANCE = 150f
  }
}

private val InputDevice.isJoystick: Boolean
  get() = isExternal && isEnabled && supportsSource(SOURCE_JOYSTICK)
