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
import android.hardware.display.DisplayManager
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
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
import work.bearbrains.joymouse.impl.CursorViewImpl
import work.bearbrains.joymouse.impl.GestureBuilderImpl
import work.bearbrains.joymouse.impl.NanoClockImpl
import work.bearbrains.joymouse.ui.SwipeVisualization

/** Handles conversion of joystick input events to motion eventsevents. */
class MouseAccessibilityService :
  AccessibilityService(), InputManager.InputDeviceListener, DisplayManager.DisplayListener {
  private var joystickDeviceIdsToState = mutableMapOf<Int, JoystickCursorState>()
  private val handler = Handler(Looper.getMainLooper())

  private val displayInfos = mutableMapOf<Int, DisplayInfo>()

  private lateinit var gestureUtil: GestureUtil

  // TODO: Associate with a joystick state to allow multiple controllers.
  var cursorView: CursorView? = null
  val cursorDisplayTimeoutMilliseconds = 1500L

  // TODO: activeGestureBuilder should be associated with a joystick state
  // This would allow multiple cursors to be controlled independently.
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

  private lateinit var cursorIcon: ImageView

  override fun onServiceConnected() {
    cursorIcon = createCursorIcon()
    val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    displayManager.registerDisplayListener(this, handler)

    gestureUtil =
      GestureUtil(ViewConfiguration.get(this), GestureDescription.getMaxGestureDuration())

    cursorView = CursorViewImpl(cursorIcon, getSystemService(WINDOW_SERVICE) as WindowManager)

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
    (getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).unregisterDisplayListener(this)

    val inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
    inputManager.unregisterInputDeviceListener(this)
    return super.onUnbind(intent)
  }

  override fun onDisplayAdded(displayId: Int) {
    Log.d(TAG, "onDisplayAdded")
    rebuildDisplays()
  }

  override fun onDisplayRemoved(displayId: Int) {
    Log.d(TAG, "onDisplayRemoved")
    rebuildDisplays()
  }

  override fun onDisplayChanged(displayId: Int) {
    Log.d(TAG, "onDisplayChanged")
    rebuildDisplays()
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    rebuildDisplays()
    super.onConfigurationChanged(newConfig)
  }

  private fun rebuildDisplays() {
    measureDisplays()
    detectJoystickDevices(getSystemService(Context.INPUT_SERVICE) as InputManager)
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

  private fun updateCursorPosition(state: JoystickCursorState) {
    cursorHider.cancel()

    activeGestureBuilder?.cursorMove(state)

    cursorView?.apply {
      updatePosition(state.pointerX, state.pointerY)
      if (!state.isPrimaryButtonPressed) {
        cursorHider.restart()
      } else {
        updateCursorViewState()
      }
      show()
    }
  }

  /** Update the visual state of the cursor based on the under-construction gesture action. */
  private fun updateCursorViewState() {
    cursorView?.apply {
      activeGestureBuilder?.action?.let { action -> cursorState = action.toCursorState() }
    }
  }

  private fun onUpdatePrimaryButton(state: JoystickCursorState) {
    updateCursorPosition(state)

    if (state.isPrimaryButtonPressed) {
      activeGestureBuilder = GestureBuilderImpl(state, gestureUtil, NanoClockImpl())
      cursorView?.cursorState = CursorView.State.STATE_PRESSED_TAP

      handler.postDelayed(::updateCursorViewState, gestureUtil.longTouchThresholdMilliseconds)
    } else {
      cursorView?.cursorState = CursorView.State.STATE_RELEASED
      activeGestureBuilder?.endGesture(state)
      dispatchPendingGesture()
      cursorHider.restart()
    }
  }

  private fun dispatchGesture(
    gesture: GestureDescription,
    numRetries: Int = 0,
    onCompleted: ((GestureDescription) -> Unit)? = null,
  ): Boolean {
    Log.d(TAG, "Dispatching gesture ${gesture} to display ${gesture.displayId}")
    return dispatchGesture(
      gesture,
      object : GestureResultCallback() {
        override fun onCompleted(gestureDescription: GestureDescription) {
          onCompleted?.invoke(gestureDescription)
        }

        override fun onCancelled(gestureDescription: GestureDescription) {
          // Gestures are cancelled by arbitrary MotionEvents. This means that an axis button
          // could pass the activation threshold, triggering this method, then emit further events
          // and cancel the gesture.
          if (numRetries > MAX_GESTURE_DISPATCH_RETRIES) {
            Log.w(TAG, "Gesture cancelled after ${numRetries} retries: ${gestureDescription}")
            return
          }

          val delay = (numRetries * GESTURE_DISPATCH_RETRY_BACKOFF_MILLIS).toLong()
          handler.postDelayed({ dispatchGesture(gesture, numRetries + 1, onCompleted) }, delay)
        }
      },
      handler,
    )
  }

  /** Dispatches the gesture(s) built up by the [activeGestureBuilder] and resets it. */
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

  private fun dispatchFling(state: JoystickCursorState, dX: Float, dY: Float) {
    val endX = (state.pointerX + dX).coerceIn(0f, state.displayInfo.windowWidth)
    val endY = (state.pointerY + dY).coerceIn(0f, state.displayInfo.windowHeight)
    if (
      (endX - state.pointerX).absoluteValue < GestureBuilder.MIN_DRAG_DISTANCE &&
        (endY - state.pointerX).absoluteValue < GestureBuilder.MIN_DRAG_DISTANCE
    ) {
      Log.d(TAG, "Ignoring short swipe: ${state.pointerX}, ${state.pointerY} -> $endX, $endY")
      return
    }

    val builder =
      GestureDescription.Builder().apply {
        setDisplayId(state.displayInfo.displayId)
        addStroke(
          GestureDescription.StrokeDescription(
            Path().apply {
              moveTo(state.pointerX, state.pointerY)
              lineTo(endX, endY)
            },
            1L,
            gestureUtil.flingTimeBetween(state.pointerX, state.pointerY, endX, endY),
            false,
          )
        )
      }
    val wasDispatched =
      dispatchGesture(builder.build()) { gestureDescription ->
        SwipeVisualization(gestureDescription, state.displayInfo.context)
      }
    if (!wasDispatched) {
      Log.e(TAG, "dispatchFling failed for ${state.pointerX}, ${state.pointerY} -> $endX, $endY")
    }
  }

  private fun cycleDisplay(state: JoystickCursorState, forward: Boolean) {
    val currentDisplayId = state.displayInfo.displayId

    val displayIds = displayInfos.keys.sorted()
    val currentIndex = displayIds.indexOf(currentDisplayId)
    var newIndex =
      currentIndex +
        if (forward) {
          1
        } else {
          -1
        }
    if (newIndex < 0) {
      newIndex = displayIds.size - 1
    } else if (newIndex >= displayIds.size) {
      newIndex = 0
    }

    val newDisplayId = displayIds[newIndex]

    if (newDisplayId == currentDisplayId) {
      return
    }

    val inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
    val device = inputManager.getInputDevice(state.deviceId)
    if (device == null) {
      Log.e(
        TAG,
        "Unexpectedly failed to retrieve device ${state.deviceId} associated with existing state"
      )
      return
    }

    val newDisplayInfo = displayInfos[newDisplayId]
    if (newDisplayInfo == null) {
      Log.e(TAG, "Unexpectedly lost displayInfo $newDisplayId")
      return
    }

    destroyCursorState(state)
    cursorView?.hideCursor()
    cursorView =
      CursorViewImpl(
        cursorIcon,
        newDisplayInfo.context.getSystemService(WINDOW_SERVICE) as WindowManager
      )
    activeGestureBuilder = null
    val newState = addJoystickDevice(device, newDisplayInfo)
    updateCursorPosition(newState)
  }

  private fun onAction(state: JoystickCursorState, action: JoystickCursorState.Action) {
    when (action) {
      JoystickCursorState.Action.CYCLE_DISPLAY_FORWARD -> {
        cycleDisplay(state, true)
      }
      JoystickCursorState.Action.CYCLE_DISPLAY_BACKWARD -> {
        cycleDisplay(state, false)
      }
      JoystickCursorState.Action.SWIPE_UP -> {
        dispatchFling(state, 0f, -SWIPE_DISTANCE)
      }
      JoystickCursorState.Action.SWIPE_DOWN -> {
        dispatchFling(state, 0f, SWIPE_DISTANCE)
      }
      JoystickCursorState.Action.SWIPE_LEFT -> {
        dispatchFling(state, -SWIPE_DISTANCE, 0f)
      }
      JoystickCursorState.Action.SWIPE_RIGHT -> {
        dispatchFling(state, SWIPE_DISTANCE, 0f)
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

  private fun getDisplayContext(): Context {
    displayInfos.get(Display.DEFAULT_DISPLAY)?.let { info ->
      return info.context
    }

    val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    val defaultDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
    return createDisplayContext(defaultDisplay)
  }

  private fun measureDisplays() {
    val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    val defaultDisplayContext = getDisplayContext()

    val detectedDisplayIds = mutableSetOf<Int>()

    for (display in displayManager.displays) {
      val context =
        if (display.displayId == Display.DEFAULT_DISPLAY) {
          defaultDisplayContext
        } else {
          displayInfos.get(display.displayId)?.context
            ?: defaultDisplayContext.createWindowContext(
              display,
              WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
              null
            )
        }

      val windowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
      val windowWidth = windowManager.maximumWindowMetrics.bounds.width().toFloat()
      val windowHeight = windowManager.maximumWindowMetrics.bounds.height().toFloat()

      displayInfos[display.displayId] =
        DisplayInfo(display.displayId, context, windowWidth, windowHeight)
      detectedDisplayIds.add(display.displayId)
      Log.d(TAG, "Display ${display.displayId} ${windowWidth} x ${windowHeight}")
    }

    for (displayId in displayInfos.keys) {
      if (!detectedDisplayIds.contains(displayId)) {
        displayInfos.remove(displayId)
      }
    }
  }

  private fun destroyCursorState(state: JoystickCursorState) {
    state.cancelRepeater()
  }

  private fun destroyCursors() {
    joystickDeviceIdsToState.forEach { (_, state) -> destroyCursorState(state) }
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

  private fun addJoystickDevice(
    device: InputDevice,
    displayInfo: DisplayInfo? = null
  ): JoystickCursorState {
    val newDevice =
      JoystickCursorStateImpl.create(
        device = device,
        displayInfo = displayInfo ?: displayInfos[Display.DEFAULT_DISPLAY]!!,
        handler = handler,
        xAxis = X_AXIS,
        yAxis = Y_AXIS,
        nanoClock = NanoClockImpl(),
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
    joystickDeviceIdsToState[device.id] = newDevice

    return newDevice
  }

  private fun JoystickCursorState.Action.toGlobalAction(): Int? {
    return when (this) {
      JoystickCursorState.Action.BACK -> GLOBAL_ACTION_BACK
      JoystickCursorState.Action.HOME -> GLOBAL_ACTION_HOME
      JoystickCursorState.Action.RECENTS -> GLOBAL_ACTION_RECENTS
      JoystickCursorState.Action.DPAD_UP -> GLOBAL_ACTION_DPAD_UP
      JoystickCursorState.Action.DPAD_DOWN -> GLOBAL_ACTION_DPAD_DOWN
      JoystickCursorState.Action.DPAD_LEFT -> GLOBAL_ACTION_DPAD_LEFT
      JoystickCursorState.Action.DPAD_RIGHT -> GLOBAL_ACTION_DPAD_RIGHT
      JoystickCursorState.Action.ACTIVATE -> GLOBAL_ACTION_DPAD_CENTER
      else -> null
    }
  }

  private fun createCursorIcon(): ImageView =
    ImageView(this).apply {
      val drawable =
        ContextCompat.getDrawable(this@MouseAccessibilityService, R.drawable.mouse_cursor)
      setImageDrawable(drawable)

      fitsSystemWindows = false

      imageTintMode = PorterDuff.Mode.MULTIPLY
      imageTintList =
        ColorStateList(
          arrayOf(
            // STATE_RELEASED
            intArrayOf(
              android.R.attr.state_enabled,
              -android.R.attr.state_pressed,
              -android.R.attr.state_selected
            ),
            // STATE_PRESSED_TAP
            intArrayOf(
              -android.R.attr.state_enabled,
              -android.R.attr.state_pressed,
              -android.R.attr.state_selected
            ),
            // STATE_PRESSED_LONG_TOUCH
            intArrayOf(
              android.R.attr.state_enabled,
              android.R.attr.state_pressed,
              -android.R.attr.state_selected
            ),
            // STATE_PRESSED_SLOW_DRAG
            intArrayOf(
              -android.R.attr.state_enabled,
              android.R.attr.state_pressed,
              -android.R.attr.state_selected
            ),
            // STATE_PRESSED_FLING
            intArrayOf(
              android.R.attr.state_enabled,
              -android.R.attr.state_pressed,
              android.R.attr.state_selected
            ),
          ),
          intArrayOf(
            Color.WHITE,
            Color.argb(0.65f, 1.0f, 1.0f, 1.0f),
            Color.argb(0.65f, 0.5f, 1.0f, 0.5f),
            Color.argb(0.65f, 1.0f, 0.8f, 0.5f),
            Color.argb(0.65f, 1.0f, 0.4f, 0.6f),
          ),
        )
    }

  private companion object {
    const val TAG = "MouseAccessibilityService"

    val X_AXIS = MotionEvent.AXIS_Z
    val Y_AXIS = MotionEvent.AXIS_RZ

    const val MAX_GESTURE_DISPATCH_RETRIES = 15
    // How many milliseconds to delay before retrying gestures. This value is multiplied by the
    // retry count so that retries are progressively further apart.
    const val GESTURE_DISPATCH_RETRY_BACKOFF_MILLIS = 0.5f

    const val SWIPE_DISTANCE = 150f
  }
}

private val InputDevice.isJoystick: Boolean
  get() = isExternal && isEnabled && supportsSource(SOURCE_JOYSTICK)

private fun GestureBuilder.Action.toCursorState(): CursorView.State =
  when (this) {
    GestureBuilder.Action.TOUCH -> CursorView.State.STATE_PRESSED_TAP
    GestureBuilder.Action.LONG_TOUCH -> CursorView.State.STATE_PRESSED_LONG_TOUCH
    GestureBuilder.Action.DRAG -> CursorView.State.STATE_PRESSED_SLOW_DRAG
    GestureBuilder.Action.FLING -> CursorView.State.STATE_PRESSED_FLING
  }
