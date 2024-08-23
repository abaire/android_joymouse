package work.bearbrains.joymouse

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Path
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
import android.view.SurfaceControl
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.util.keyIterator
import java.io.Closeable
import kotlin.math.absoluteValue
import work.bearbrains.joymouse.impl.NanoClockImpl
import work.bearbrains.joymouse.input.GestureBuilder
import work.bearbrains.joymouse.input.GestureUtil
import work.bearbrains.joymouse.input.JoystickAction
import work.bearbrains.joymouse.input.JoystickCursorState
import work.bearbrains.joymouse.input.impl.GestureBuilderImpl
import work.bearbrains.joymouse.input.impl.GestureDescriptionBuilderProvider
import work.bearbrains.joymouse.input.impl.JoystickButtonProcessorFactoryImpl
import work.bearbrains.joymouse.ui.CursorAccessibilityOverlay
import work.bearbrains.joymouse.ui.SwipeVisualization

/** Handles conversion of joystick input events to motion eventsevents. */
class MouseAccessibilityService :
  AccessibilityService(), InputManager.InputDeviceListener, DisplayManager.DisplayListener {

  // Maps a joystick device ID to a [JoystickCursorState] responsible for tracking the virtual mouse
  // state.
  private var joystickDeviceIdsToState = mutableMapOf<Int, JoystickCursorState>()

  // Maps the ID of a [Display] to a state object encapsulating the ability to auto-hide the cursor
  // as well as an overlay surface into which the cursor will be drawn.
  private val displayIdToCursorDisplayState = mutableMapOf<Int, CursorDisplayState>()

  private val handler = Handler(Looper.getMainLooper())

  private val displayInfos = mutableMapOf<Int, DisplayInfo>()

  private val closeableOverlays = mutableSetOf<Closeable>()

  private lateinit var gestureUtil: GestureUtil

  private val cursorDisplayTimeoutMilliseconds = 1500L

  // TODO: activeGestureBuilder should be associated with a joystick state
  // This would allow multiple cursors to be controlled independently.
  private var activeGestureBuilder: GestureBuilder? = null

  private var isEnabled = false
    set(value) {
      field = value

      Log.i(TAG, "JoyMouse enabled: ${value}")
      captureJoystickMotionEvents = value
    }

  private var captureJoystickMotionEvents: Boolean = false
    set(value) {
      if (field == value) {
        return
      }

      field = value

      val info = serviceInfo
      if (value) {
        info.motionEventSources = SOURCE_JOYSTICK
      } else {
        info.motionEventSources = 0
      }
      serviceInfo = info
    }

  override fun onServiceConnected() {
    val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    displayManager.registerDisplayListener(this, handler)

    gestureUtil =
      GestureUtil(ViewConfiguration.get(this), GestureDescription.getMaxGestureDuration())

    isEnabled = true

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

    joystickDeviceIdsToState.forEach { (_, state) -> state.close() }
    joystickDeviceIdsToState.clear()

    displayIdToCursorDisplayState.forEach { (_, state) -> state.hide() }

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

    for (joystickId in joystickDeviceIdsToState.keys.toSet()) {
      val state = joystickDeviceIdsToState[joystickId]!!
      if (state.displayInfo.isValid) {
        continue
      }

      val displayInfo =
        displayInfos.getOrDefault(Display.DEFAULT_DISPLAY, displayInfos.values.first())

      moveJoystickCursorToDisplay(state, displayInfo)?.let { newState ->
        joystickDeviceIdsToState[joystickId] = newState
      }
    }
  }

  private val DisplayInfo.isValid: Boolean
    get() = displayIdToCursorDisplayState.containsKey(displayId)

  override fun onKeyEvent(event: KeyEvent?): Boolean {
    if (event == null) {
      return super.onKeyEvent(event)
    }

    Log.d(TAG, "onKeyEvent: ${event}")

    val state = joystickDeviceIdsToState.get(event.deviceId)
    if (state == null) {
      return super.onKeyEvent(event)
    }

    val wasEnabled = state.isEnabled
    state.handleButtonEvent(event.action == KeyEvent.ACTION_DOWN, event.keyCode)

    // While the state is enabled, all joystick keys will be consumed. If this key resulted in
    // toggling the state to off, it should also be consumed.
    return state.isEnabled || state.isEnabled != wasEnabled
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
    val displayInfo = state.displayInfo
    val cursorState = displayIdToCursorDisplayState.get(displayInfo.displayId)
    if (cursorState == null) {
      Log.e(
        TAG,
        "Ignoring cursor position update for missing display ID ${state.displayInfo.displayId}"
      )
      return
    }
    cursorState.cancelHider()

    activeGestureBuilder?.cursorMove(state)
    cursorState.overlay.draw(state.pointerX, state.pointerY)
    if (!state.isPrimaryButtonPressed) {
      cursorState.restartHider()
    } else {
      updateCursorDisplayState(cursorState)
    }
    cursorState.show()
  }

  /** Update the visual state of the cursor based on the under-construction gesture action. */
  private fun updateCursorDisplayState(displayState: CursorDisplayState) {
    activeGestureBuilder?.action?.let { action ->
      displayState.currentState = action.toCursorState()
    }
  }

  private fun onUpdatePrimaryButton(state: JoystickCursorState) {
    updateCursorPosition(state)
    val displayInfo = state.displayInfo
    val cursorState = displayIdToCursorDisplayState.get(displayInfo.displayId)
    if (cursorState == null) {
      Log.e(
        TAG,
        "Ignoring cursor view state update for missing display ID ${displayInfo.displayId}"
      )
      return
    }

    if (state.isPrimaryButtonPressed) {
      activeGestureBuilder =
        GestureBuilderImpl(
          state,
          gestureUtil,
          NanoClockImpl(),
          gestureDescriptionBuilderProvider = GestureDescriptionBuilderProvider,
        )
      cursorState.currentState = CursorDisplayState.State.STATE_PRESSED_TAP

      handler.postDelayed(
        { updateCursorDisplayState(cursorState) },
        gestureUtil.longTouchThresholdMilliseconds
      )
    } else {
      cursorState.currentState = CursorDisplayState.State.STATE_RELEASED
      activeGestureBuilder?.endGesture(state)
      dispatchPendingGesture()
      cursorState.restartHider()
    }
  }

  private fun dispatchGesture(
    gesture: GestureDescription,
    numRetries: Int = 0,
    onCompleted: ((GestureDescription) -> Unit)? = null,
  ): Boolean {
    Log.d(TAG, "Dispatching gesture ${gesture} to display ${gesture.displayId} [$numRetries]")
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

    // TODO: Consider adding a pause on the initial press
    // It seems as though these gestures are sometimes ignored by the system, likely discarded as
    // accidental brushes of the screen. Try breaking this up into an initial stroke that holds
    // for some number of milliseconds with `willContinue`, followed by the actual fling gesture.
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
        val visualization = SwipeVisualization(state.displayInfo, gestureDescription)
        closeableOverlays.add(visualization)
        attachAccessibilityOverlayToDisplay(
          state.displayInfo.displayId,
          visualization.surfaceControl
        )

        handler.postDelayed(
          {
            SurfaceControl.Transaction().reparent(visualization.surfaceControl, null).apply()
            visualization.close()
            closeableOverlays.remove(visualization)
          },
          500L
        )
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

    val newDisplayInfo = displayInfos[newDisplayId]
    if (newDisplayInfo == null) {
      Log.e(TAG, "Unexpectedly lost displayInfo $newDisplayId")
      return
    }

    moveJoystickCursorToDisplay(state, newDisplayInfo)
    selectDisplayRootWindow(newDisplayId)
  }

  private fun moveJoystickCursorToDisplay(
    state: JoystickCursorState,
    newDisplayInfo: DisplayInfo
  ): JoystickCursorState? {
    if (newDisplayInfo == state.displayInfo) {
      return state
    }

    val inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
    val device = inputManager.getInputDevice(state.deviceId)
    if (device == null) {
      Log.e(
        TAG,
        "Unexpectedly failed to retrieve device ${state.deviceId} associated with existing joystick state"
      )
      return null
    }

    state.close()
    displayIdToCursorDisplayState.get(state.displayInfo.displayId)?.hide()

    activeGestureBuilder = null

    return addJoystickDevice(device, newDisplayInfo).also { updateCursorPosition(it) }
  }

  private fun onAction(state: JoystickCursorState, action: JoystickAction) {
    Log.d(TAG, "onAction ${action} for state ${state}")
    when (action) {
      JoystickAction.CYCLE_DISPLAY_FORWARD -> {
        cycleDisplay(state, true)
      }
      JoystickAction.CYCLE_DISPLAY_BACKWARD -> {
        cycleDisplay(state, false)
      }
      JoystickAction.SWIPE_UP -> {
        dispatchFling(state, 0f, -SWIPE_DISTANCE)
      }
      JoystickAction.SWIPE_DOWN -> {
        dispatchFling(state, 0f, SWIPE_DISTANCE)
      }
      JoystickAction.SWIPE_LEFT -> {
        dispatchFling(state, -SWIPE_DISTANCE, 0f)
      }
      JoystickAction.SWIPE_RIGHT -> {
        dispatchFling(state, SWIPE_DISTANCE, 0f)
      }
      JoystickAction.TOGGLE_ENABLED -> {
        isEnabled = state.isEnabled
        if (!state.isEnabled) {
          val cursorState = displayIdToCursorDisplayState.get(state.displayInfo.displayId)
          if (cursorState == null) {
            Log.e(TAG, "Ignoring onEnabledChange display ID ${state.displayInfo.displayId}")
          } else {
            cursorState.hide()
          }
        } else {
          updateCursorPosition(state)
        }
      }
      JoystickAction.TOGGLE_GESTURE -> {
        activeGestureBuilder?.let {
          it.dragIsFling = !it.dragIsFling
          updateCursorPosition(state)
        }
      }
      JoystickAction.PRIMARY_PRESS,
      JoystickAction.PRIMARY_RELEASE -> {
        onUpdatePrimaryButton(state)
      }
      JoystickAction.FAST_CURSOR_PRESS,
      JoystickAction.FAST_CURSOR_RELEASE -> {
        // Intentionally ignored
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
    joystickDeviceIdsToState.get(deviceId)?.close()
    joystickDeviceIdsToState.remove(deviceId)
  }

  override fun onInputDeviceChanged(deviceId: Int) {
    Log.d(TAG, "Ignoring onInputDeviceChanged for device ID ${deviceId}")
  }

  private fun getDefaultDisplayContext(): Context {
    displayInfos.get(Display.DEFAULT_DISPLAY)?.let { info ->
      return info.context
    }

    val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    val defaultDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
    return createDisplayContext(defaultDisplay)
  }

  /**
   * [DisplayManager] enumerates physical displays but in the case of Samsung DEX, the alternate
   * display appears to be a virtual display whose ID does not appear in the [DisplayManager]
   * enumeration. This method uses the accessibility window list to discover all displays with at
   * least one window.
   */
  private fun extractDisplaysFromWindows(): List<Display> {
    val ret = mutableListOf<Display>()

    val displayToWindows = windowsOnAllDisplays
    val numDisplays = displayToWindows.size()
    Log.d(TAG, "extractDisplaysFromWindows: detected ${numDisplays} with accessibility window info")

    val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    for (key in displayToWindows.keyIterator()) {
      displayManager.getDisplay(key)?.let {
        Log.d(TAG, "  Display ${key}: ${it}")

        if (displayToWindows[key].isEmpty()) {
          Log.i(TAG, "Ignoring display ${key} with no accessibility window info ($it})")
          return@let
        }

        ret.add(it)
      }
    }

    return ret
  }

  /** Sends a SELECT action to the root node of the given display (which may be virtual). */
  private fun selectDisplayRootWindow(displayId: Int): Boolean {
    val windows = windowsOnAllDisplays[displayId]
    if (windows == null) {
      Log.e(TAG, "Ignoring attempt to select root node on unknown displayId ${displayId}")
      return false
    }
    if (windows.size == 0) {
      Log.e(TAG, "Ignoring attempt to select root node on displayId ${displayId} with no windows")
      return false
    }

    val rootWindow = windows.last()
    val rootNode = rootWindow.root

    for (actionToPerform in SELECT_DISPLAY_ACTIONS) {
      if (rootNode.performAction(actionToPerform)) {
        return true
      }
    }
    Log.w(TAG, "Failed to select display ${displayId} with root node ${rootNode}")
    return false
  }

  private fun measureDisplays() {
    val defaultDisplayContext = getDefaultDisplayContext()
    val detectedDisplayIds = mutableSetOf<Int>()

    for (display in extractDisplaysFromWindows()) {
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

      val info = DisplayInfo(display.displayId, context, windowWidth, windowHeight)
      displayInfos[display.displayId] = info
      detectedDisplayIds.add(display.displayId)

      val cursorOverlay = CursorAccessibilityOverlay(info)

      displayIdToCursorDisplayState[display.displayId] =
        CursorDisplayState(cursorOverlay, handler, onShow = ::attachAccessibilityOverlayToDisplay)
    }

    val displayInfoKeys = displayInfos.keys.toSet()
    for (displayId in displayInfoKeys) {
      if (!detectedDisplayIds.contains(displayId)) {
        displayIdToCursorDisplayState[displayId]?.hide()
        displayInfos.remove(displayId)
      }
    }
  }

  private fun detectJoystickDevices(inputManager: InputManager) {
    val visitedDevices = mutableSetOf<Int>()

    for (deviceId in inputManager.inputDeviceIds) {
      inputManager.getInputDevice(deviceId)?.let { device ->
        if (!device.isJoystick) {
          return@let
        }

        visitedDevices.add(deviceId)
        if (joystickDeviceIdsToState.containsKey(deviceId)) {
          return@let
        }

        addJoystickDevice(device)
      }
    }

    val removedDevices = joystickDeviceIdsToState.keys.toMutableSet()
    removedDevices.removeAll(visitedDevices)
    for (deviceId in removedDevices) {
      joystickDeviceIdsToState[deviceId]?.close()
      joystickDeviceIdsToState.remove(deviceId)
    }
  }

  private fun addJoystickDevice(
    device: InputDevice,
    displayInfo: DisplayInfo? = null
  ): JoystickCursorState {

    fun getValidDisplayInfo(): DisplayInfo {
      if (displayInfo != null) {
        return displayInfo
      }

      // If the primary display has gone to sleep, it is possible that it is no longer accessible.
      return displayInfos.getOrDefault(Display.DEFAULT_DISPLAY, displayInfos.values.first())
    }

    val newDevice =
      JoystickCursorStateImpl.create(
        device = device,
        displayInfo = getValidDisplayInfo(),
        handler = handler,
        xAxis = X_AXIS,
        yAxis = Y_AXIS,
        nanoClock = NanoClockImpl(),
        JoystickButtonProcessorFactoryImpl,
        onUpdatePosition = ::updateCursorPosition,
        onAction = ::onAction,
      )
    joystickDeviceIdsToState[device.id] = newDevice

    return newDevice
  }

  private fun JoystickAction.toGlobalAction(): Int? {
    return when (this) {
      JoystickAction.BACK -> GLOBAL_ACTION_BACK
      JoystickAction.HOME -> GLOBAL_ACTION_HOME
      JoystickAction.RECENTS -> GLOBAL_ACTION_RECENTS
      JoystickAction.DPAD_UP -> GLOBAL_ACTION_DPAD_UP
      JoystickAction.DPAD_DOWN -> GLOBAL_ACTION_DPAD_DOWN
      JoystickAction.DPAD_LEFT -> GLOBAL_ACTION_DPAD_LEFT
      JoystickAction.DPAD_RIGHT -> GLOBAL_ACTION_DPAD_RIGHT
      JoystickAction.ACTIVATE -> GLOBAL_ACTION_DPAD_CENTER
      else -> null
    }
  }

  private companion object {
    const val TAG = "MouseAccessibilityService"

    val X_AXIS = MotionEvent.AXIS_Z
    val Y_AXIS = MotionEvent.AXIS_RZ

    const val MAX_GESTURE_DISPATCH_RETRIES = 15
    // How many milliseconds to delay before retrying gestures. This value is multiplied by the
    // retry count so that retries are progressively further apart.
    const val GESTURE_DISPATCH_RETRY_BACKOFF_MILLIS = 0.5f

    const val SWIPE_DISTANCE = 250f

    // Ordered list of [AccessibilityNodeInfo] actions that will be performed when attempting to set
    // the active [Display].
    val SELECT_DISPLAY_ACTIONS =
      listOf(
        AccessibilityNodeInfo.ACTION_FOCUS,
        AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS,
        AccessibilityNodeInfo.ACTION_SELECT,
        AccessibilityNodeInfo.ACTION_CLICK,
      )
  }
}

private val InputDevice.isJoystick: Boolean
  get() = isExternal && isEnabled && supportsSource(SOURCE_JOYSTICK)

private class CursorDisplayState(
  val overlay: CursorAccessibilityOverlay,
  private val handler: Handler,
  val cursorDisplayTimeoutMilliseconds: Long = 1500L,
  private val onShow: (Int, SurfaceControl) -> Unit,
) {
  /** The visual state of this cursor. */
  enum class State {
    STATE_RELEASED,
    STATE_PRESSED_TAP,
    STATE_PRESSED_LONG_TOUCH,
    STATE_PRESSED_SLOW_DRAG,
    STATE_PRESSED_FLING,
  }

  /** Whether or not the cursor overlay is currently displayed. */
  var isShown: Boolean = false
    private set

  /** Tracks the current display state of this cursor. */
  var currentState: State = State.STATE_RELEASED
    set(value) {
      if (value == field) {
        return
      }

      field = value
      // Safe as long as TINT_MAP is always kept in sync with the [State] enumeration.
      overlay.tintColor = TINT_MAP[value]!!
    }

  private val hider =
    object : Runnable {
      override fun run() {
        Log.d(TAG, "Hiding cursor on display ${overlay.displayInfo} due to inactivity")
        hide()
      }
    }

  /** Shows the [overlay] managed by this state. */
  fun show() {
    if (isShown) {
      return
    }

    onShow(overlay.displayInfo.displayId, overlay.surfaceControl)
    isShown = true
  }

  /** Hides the [overlay] managed by this state. */
  fun hide() {
    cancelHider()
    if (!isShown) {
      return
    }

    SurfaceControl.Transaction().reparent(overlay.surfaceControl, null).apply()
    isShown = false
  }

  /** Queues a future action to invoke the `onHide` callback. */
  fun restartHider() {
    cancelHider()
    handler.postDelayed(hider, cursorDisplayTimeoutMilliseconds)
  }

  /** Cancels any pending runs for the hide timer. */
  fun cancelHider() {
    handler.removeCallbacks(hider)
  }

  private companion object {
    const val TAG = "CursorDisplayState"

    val TINT_MAP =
      mapOf(
        State.STATE_RELEASED to Color.WHITE,
        State.STATE_PRESSED_TAP to Color.argb(0.65f, 1.0f, 1.0f, 1.0f),
        State.STATE_PRESSED_LONG_TOUCH to Color.argb(0.65f, 0.5f, 1.0f, 0.5f),
        State.STATE_PRESSED_SLOW_DRAG to Color.argb(0.65f, 1.0f, 0.8f, 0.5f),
        State.STATE_PRESSED_FLING to Color.argb(0.65f, 1.0f, 0.4f, 0.6f),
      )
  }
}

private fun GestureBuilder.Action.toCursorState(): CursorDisplayState.State =
  when (this) {
    GestureBuilder.Action.TOUCH -> CursorDisplayState.State.STATE_PRESSED_TAP
    GestureBuilder.Action.LONG_TOUCH -> CursorDisplayState.State.STATE_PRESSED_LONG_TOUCH
    GestureBuilder.Action.DRAG -> CursorDisplayState.State.STATE_PRESSED_SLOW_DRAG
    GestureBuilder.Action.FLING -> CursorDisplayState.State.STATE_PRESSED_FLING
  }
