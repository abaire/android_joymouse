package work.bearbrains.joymouse

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
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
import android.widget.Toast
import androidx.core.util.keyIterator
import java.io.Closeable
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import work.bearbrains.joymouse.impl.NanoClockImpl
import work.bearbrains.joymouse.input.ActionConfig
import work.bearbrains.joymouse.input.ActionConfigRepository
import work.bearbrains.joymouse.input.GestureBuilder
import work.bearbrains.joymouse.input.GestureUtil
import work.bearbrains.joymouse.input.JoystickAction
import work.bearbrains.joymouse.input.JoystickCursorState
import work.bearbrains.joymouse.input.impl.GestureBuilderImpl
import work.bearbrains.joymouse.input.impl.GestureDescriptionBuilderProvider
import work.bearbrains.joymouse.input.impl.JoystickButtonProcessorFactoryImpl
import work.bearbrains.joymouse.input.impl.JoystickCursorStateImpl
import work.bearbrains.joymouse.model.ControllerInfo
import work.bearbrains.joymouse.ui.ControllerSelectionActivity
import work.bearbrains.joymouse.ui.CursorAccessibilityOverlay
import work.bearbrains.joymouse.ui.SwipeVisualization
import work.bearbrains.joymouse.ui.lastPoint

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

  @androidx.annotation.VisibleForTesting
  internal var primaryDeviceId: Int? = null

  private var activeGestureBuilder: GestureBuilder? = null
  private var pendingLongTouchRunnable: Runnable? = null

  private fun cancelPendingLongTouch() {
    pendingLongTouchRunnable?.let {
      handler.removeCallbacks(it)
      pendingLongTouchRunnable = null
    }
  }

  private fun resetGestureState() {
    cancelPendingLongTouch()
    activeGestureBuilder = null
  }

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

      val info = serviceInfo ?: AccessibilityServiceInfo()
      if (value) {
        info.motionEventSources = SOURCE_JOYSTICK
      } else {
        info.motionEventSources = 0
      }
      serviceInfo = info
    }

  private lateinit var actionConfigRepository: ActionConfigRepository
  private var actionConfigCloseable: AutoCloseable? = null

  public override fun onServiceConnected() {
    instance = this

    actionConfigRepository = ActionConfigRepository(this)
    actionConfigCloseable = actionConfigRepository.registerListener { newConfig ->
      joystickDeviceIdsToState.values.forEach { it.updateActionConfig(newConfig) }
    }

    val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    displayManager.registerDisplayListener(this, handler)

    gestureUtil =
      GestureUtil(
        ViewConfiguration.get(this),
        GestureDescription.getMaxGestureDuration().milliseconds
      )

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
    cleanup()
    return super.onUnbind(intent)
  }

  override fun onDestroy() {
    cleanup()
    super.onDestroy()
  }

  private fun cleanup() {
    (getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).unregisterDisplayListener(this)

    val inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
    inputManager.unregisterInputDeviceListener(this)

    joystickDeviceIdsToState.forEach { (_, state) -> state.close() }
    joystickDeviceIdsToState.clear()

    displayIdToCursorDisplayState.forEach { (_, state) -> state.close() }
    displayIdToCursorDisplayState.clear()

    actionConfigCloseable?.close()
    actionConfigCloseable = null

    closeableOverlays.forEach { it.close() }
    closeableOverlays.clear()

    resetGestureState()
    primaryDeviceId = null
    if (instance === this) {
      instance = null
    }

    handler.removeCallbacksAndMessages(null)
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
    resetGestureState()
    measureDisplays()

    for (joystickId in joystickDeviceIdsToState.keys.toSet()) {
      val state = joystickDeviceIdsToState[joystickId]!!
      val currentDisplayId = state.displayInfo.displayId

      val newDisplayInfo =
        if (displayInfos.containsKey(currentDisplayId)) {
          displayInfos[currentDisplayId]!!
        } else {
          displayInfos.getOrDefault(Display.DEFAULT_DISPLAY, displayInfos.values.firstOrNull())
        }

      if (newDisplayInfo != null) {
        state.updateDisplayInfo(newDisplayInfo)
        if (primaryDeviceId == null || primaryDeviceId == joystickId) {
          updateCursorPosition(state)
        }
      }
    }
  }


  public override fun onKeyEvent(event: KeyEvent?): Boolean {
    if (event == null) {
      return super.onKeyEvent(event)
    }

    Log.d(TAG, "onKeyEvent: ${event}")

    if (primaryDeviceId == null) {
      primaryDeviceId = event.deviceId
    } else if (event.deviceId != primaryDeviceId) {
      return super.onKeyEvent(event)
    }

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

  public override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

  public override fun onMotionEvent(event: MotionEvent) {
    if (primaryDeviceId == null) {
      primaryDeviceId = event.deviceId
    } else if (event.deviceId != primaryDeviceId) {
      return
    }

    val state = joystickDeviceIdsToState.get(event.deviceId)
    if (state == null) {
      return
    }

    state.update(event)

    super.onMotionEvent(event)
  }

  private fun updateCursorPosition(state: JoystickCursorState) {
    if (!state.isEnabled) {
      return
    }

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
      cancelPendingLongTouch()
      activeGestureBuilder =
        GestureBuilderImpl(
          state,
          gestureUtil,
          NanoClockImpl(),
          gestureDescriptionBuilderProvider = GestureDescriptionBuilderProvider,
        )
      cursorState.currentState = CursorDisplayState.State.STATE_PRESSED_TAP

      val runnable = Runnable {
        pendingLongTouchRunnable = null
        updateCursorDisplayState(cursorState)
      }
      pendingLongTouchRunnable = runnable
      handler.postDelayed(
        runnable,
        gestureUtil.longTouchThreshold.inWholeMilliseconds
      )
    } else {
      cancelPendingLongTouch()
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
    cancelPendingLongTouch()
    activeGestureBuilder?.let { builder ->
      activeGestureBuilder = null

      val gesture = builder.build()

      // Attempt an accessibility click
      if (builder.action == GestureBuilder.Action.TOUCH) {
        val point = PointF()
        if (gesture.lastPoint(point)) {
          if (
            performAccessibilityClick(
              builder.displayInfo,
              point.x.roundToInt(),
              point.y.roundToInt()
            )
          ) {
            return@let
          }
        }
      }

      val wasDispatched = dispatchGesture(gesture)
      if (!wasDispatched) {
        Log.e(TAG, "dispatchGesture failed for ${gesture}!")
      }
    }
  }

  private fun dispatchFling(state: JoystickCursorState, dX: Float, dY: Float) {
    val endX = (state.pointerX + dX).coerceIn(0f, state.displayInfo.windowWidth)
    val endY = (state.pointerY + dY).coerceIn(0f, state.displayInfo.windowHeight)
    if (
      (endX - state.pointerX).absoluteValue < GestureBuilder.MIN_DRAG_DISTANCE &&
        (endY - state.pointerY).absoluteValue < GestureBuilder.MIN_DRAG_DISTANCE
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
            gestureUtil
              .flingTimeBetween(state.pointerX, state.pointerY, endX, endY)
              .inWholeMilliseconds,
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
    if (displayIds.size <= 1) {
      Toast.makeText(state.displayInfo.context, R.string.toast_single_display, Toast.LENGTH_SHORT)
        .show()
      return
    }

    val currentIndex = displayIds.indexOf(currentDisplayId)
    var newIndex =
      if (currentIndex == -1) {
        0
      } else {
        currentIndex +
          if (forward) {
            1
          } else {
            -1
          }
      }
    if (newIndex < 0) {
      newIndex = displayIds.size - 1
    } else if (newIndex >= displayIds.size) {
      newIndex = 0
    }

    val newDisplayId = displayIds[newIndex]

    if (newDisplayId == currentDisplayId) {
      Toast.makeText(state.displayInfo.context, R.string.toast_single_display, Toast.LENGTH_SHORT)
        .show()
      return
    }

    val newDisplayInfo = displayInfos[newDisplayId]
    if (newDisplayInfo == null) {
      Log.e(TAG, "Unexpectedly lost displayInfo $newDisplayId")
      return
    }

    resetGestureState()
    displayIdToCursorDisplayState.get(currentDisplayId)?.hide()
    state.updateDisplayInfo(newDisplayInfo)
    updateCursorPosition(state)
    selectDisplayRootWindow(newDisplayId)
    Toast.makeText(
      newDisplayInfo.context,
      getString(R.string.toast_active_display, newDisplayId),
      Toast.LENGTH_SHORT
    ).show()
  }

  fun getConnectedControllers(): List<ControllerInfo> {
    val inputManager = getSystemService(Context.INPUT_SERVICE) as? InputManager
    return joystickDeviceIdsToState.keys.sorted().map { deviceId ->
      val deviceName =
        inputManager?.getInputDevice(deviceId)?.name
          ?: getString(R.string.controller_fallback_name, deviceId)
      ControllerInfo(
        id = deviceId,
        name = deviceName,
        isPrimary = (deviceId == primaryDeviceId),
      )
    }
  }

  fun setPrimaryDevice(deviceId: Int) {
    if (!joystickDeviceIdsToState.containsKey(deviceId)) {
      return
    }
    if (deviceId == primaryDeviceId) {
      return
    }

    resetGestureState()
    primaryDeviceId = deviceId

    val nextState = joystickDeviceIdsToState[deviceId]
    if (nextState != null) {
      updateCursorPosition(nextState)
      val inputManager = getSystemService(Context.INPUT_SERVICE) as? InputManager
      val deviceName =
        inputManager?.getInputDevice(deviceId)?.name
          ?: getString(R.string.controller_fallback_name, deviceId)
      Toast.makeText(
        nextState.displayInfo.context,
        getString(R.string.toast_active_device, deviceName),
        Toast.LENGTH_SHORT
      ).show()
    }
  }

  private fun showControllerPicker(state: JoystickCursorState) {
    val deviceIds = joystickDeviceIdsToState.keys
    if (deviceIds.size <= 1) {
      Toast.makeText(state.displayInfo.context, R.string.toast_single_device, Toast.LENGTH_SHORT)
        .show()
      return
    }

    val intent =
      Intent(this, ControllerSelectionActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
      }
    val options = ActivityOptions.makeBasic()
    options.setLaunchDisplayId(state.displayInfo.displayId)
    startActivity(intent, options.toBundle())
  }


  private fun onAction(state: JoystickCursorState, action: JoystickAction) {
    Log.d(TAG, "onAction ${action} for state ${state}")
    when (action) {
      JoystickAction.PRIMARY_PRESS,
      JoystickAction.PRIMARY_RELEASE -> {
        onUpdatePrimaryButton(state)
      }
      JoystickAction.FAST_CURSOR_PRESS,
      JoystickAction.FAST_CURSOR_RELEASE -> {
        // Intentionally ignored
      }
      JoystickAction.TOGGLE_GESTURE -> {
        activeGestureBuilder?.let {
          it.dragIsFling = !it.dragIsFling
          updateCursorPosition(state)
        }
      }
      JoystickAction.SELECT_PRIMARY_DEVICE -> {
        resetGestureState()
        showControllerPicker(state)
      }
      JoystickAction.CYCLE_DISPLAY_FORWARD -> {
        resetGestureState()
        cycleDisplay(state, true)
      }
      JoystickAction.CYCLE_DISPLAY_BACKWARD -> {
        resetGestureState()
        cycleDisplay(state, false)
      }
      JoystickAction.SWIPE_UP -> {
        resetGestureState()
        dispatchFling(state, 0f, -SWIPE_DISTANCE)
      }
      JoystickAction.SWIPE_DOWN -> {
        resetGestureState()
        dispatchFling(state, 0f, SWIPE_DISTANCE)
      }
      JoystickAction.SWIPE_LEFT -> {
        resetGestureState()
        dispatchFling(state, -SWIPE_DISTANCE, 0f)
      }
      JoystickAction.SWIPE_RIGHT -> {
        resetGestureState()
        dispatchFling(state, SWIPE_DISTANCE, 0f)
      }
      JoystickAction.TOGGLE_ENABLED -> {
        resetGestureState()
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
      else -> {
        resetGestureState()
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

    if (deviceId == primaryDeviceId) {
      resetGestureState()
      primaryDeviceId = joystickDeviceIdsToState.keys.firstOrNull()
      primaryDeviceId?.let { newPrimaryId ->
        joystickDeviceIdsToState[newPrimaryId]?.let { newState ->
          updateCursorPosition(newState)
          val inputManager = getSystemService(Context.INPUT_SERVICE) as? InputManager
          val deviceName = inputManager?.getInputDevice(newPrimaryId)?.name ?: newPrimaryId.toString()
          Toast.makeText(
            newState.displayInfo.context,
            getString(R.string.toast_active_device, deviceName),
            Toast.LENGTH_SHORT
          ).show()
        }
      }
    }
  }

  override fun onInputDeviceChanged(deviceId: Int) {
    Log.d(TAG, "Ignoring onInputDeviceChanged for device ID ${deviceId}")
  }

  private fun getDefaultDisplayContext(): Context {
    val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    val defaultDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY) ?: return this
    return createDisplayContext(defaultDisplay)
  }


  /**
   * Certain elements in Samsung DeX do not respond to accessibility gestures but do respond to
   * actions on AccessibilityNodeInfos. This method attempts to determine if the cursor is over such
   * an element and performs a click action.
   */
  private fun performAccessibilityClick(
    displayInfo: DisplayInfo,
    pointerX: Int,
    pointerY: Int
  ): Boolean {
    val displayId = displayInfo.displayId
    val displayWindows = windowsOnAllDisplays[displayId] ?: return false
    for (window in displayWindows) {
      val root = window.root
      if (root == null) {
        continue
      }

      val clickableChild = root.getClickableChildAt(pointerX, pointerY)
      if (clickableChild == null) {
        continue
      }

      Log.d(
        TAG,
        "Performing click on clickable accessibility info node at $pointerX $pointerY [ ${clickableChild} ]"
      )
      return clickableChild.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    return false
  }

  // Temporary element used while determining whether a cursor is contained by an
  // AccessibilityNodeInfo. Must not be accessed off the main thread.
  private val accessibilityNodeInfoScreenBounds = Rect()

  private fun AccessibilityNodeInfo.getClickableChildAt(
    pointerX: Int,
    pointerY: Int
  ): AccessibilityNodeInfo? {
    getBoundsInScreen(accessibilityNodeInfoScreenBounds)
    if (!accessibilityNodeInfoScreenBounds.contains(pointerX, pointerY)) {
      return null
    }

    for (i in 0 ..< childCount) {
      getChild(i)?.let { child ->
        val clickableChild = child.getClickableChildAt(pointerX, pointerY)
        if (clickableChild != null) {
          return clickableChild
        }
      }
    }

    return if (isClickable) this else null
  }

  /**
   * Discovers all available displays from both [DisplayManager] and the accessibility window list.
   * Samsung DeX may expose displays through physical enumeration or virtual display IDs in the
   * window list.
   */
  private fun extractDisplays(): List<Display> {
    val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    val displays = mutableMapOf<Int, Display>()

    for (display in displayManager.displays) {
      Log.d(TAG, "  DisplayManager display ${display.displayId}: $display")
      displays[display.displayId] = display
    }

    val displayToWindows = windowsOnAllDisplays
    val numDisplays = displayToWindows.size()
    Log.d(TAG, "extractDisplays: windowsOnAllDisplays.size = $numDisplays")

    for (key in displayToWindows.keyIterator()) {
      if (!displays.containsKey(key)) {
        displayManager.getDisplay(key)?.let {
          Log.d(TAG, "  Virtual display from windows $key: $it")
          displays[key] = it
        }
      }
    }

    return displays.values.toList()
  }

  /** Sends a SELECT action to the root node of the given display (which may be virtual). */
  private fun selectDisplayRootWindow(displayId: Int): Boolean {
    val windows = windowsOnAllDisplays[displayId]
    if (windows.isNullOrEmpty()) {
      Log.i(TAG, "No windows to select on displayId $displayId")
      return false
    }

    for (window in windows.reversed()) {
      val rootNode = window.root ?: continue
      for (actionToPerform in SELECT_DISPLAY_ACTIONS) {
        if (rootNode.performAction(actionToPerform)) {
          return true
        }
      }
    }
    Log.w(TAG, "Failed to select display $displayId with any window root node")
    return false
  }

  private fun measureDisplays() {
    val defaultDisplayContext = getDefaultDisplayContext()
    val detectedDisplayIds = mutableSetOf<Int>()

    for (display in extractDisplays()) {
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

      if (!displayIdToCursorDisplayState.containsKey(display.displayId)) {
        val cursorOverlay = CursorAccessibilityOverlay(info)
        displayIdToCursorDisplayState[display.displayId] =
          CursorDisplayState(cursorOverlay, handler, onShow = ::attachAccessibilityOverlayToDisplay)
      }
    }

    val removedDisplayIds = displayIdToCursorDisplayState.keys - detectedDisplayIds
    for (displayId in removedDisplayIds) {
      displayIdToCursorDisplayState[displayId]?.close()
      displayIdToCursorDisplayState.remove(displayId)
      displayInfos.remove(displayId)
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
      onInputDeviceRemoved(deviceId)
    }
  }

  @androidx.annotation.VisibleForTesting
  internal fun addJoystickDevice(
    device: InputDevice,
    displayInfo: DisplayInfo? = null
  ): JoystickCursorState {

    fun getValidDisplayInfo(): DisplayInfo {
      if (displayInfo != null) {
        return displayInfo
      }

      // If the primary display has gone to sleep, it is possible that it is no longer accessible.
      return displayInfos.getOrDefault(Display.DEFAULT_DISPLAY, displayInfos.values.firstOrNull())
        ?: DisplayInfo(Display.DEFAULT_DISPLAY, getDefaultDisplayContext(), 1920f, 1080f)
    }

    val config =
      if (::actionConfigRepository.isInitialized) {
        actionConfigRepository.getConfig()
      } else {
        ActionConfig.DEFAULT
      }

    val newDevice =
      JoystickCursorStateImpl.create(
        device = device,
        displayInfo = getValidDisplayInfo(),
        handler = handler,
        xAxis = config.mouseStick.xAxis,
        yAxis = config.mouseStick.yAxis,
        nanoClock = NanoClockImpl(),
        buttonProcessorFactory = JoystickButtonProcessorFactoryImpl,
        onUpdatePosition = ::updateCursorPosition,
        onAction = ::onAction,
        config = config,
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

  companion object {
    private const val TAG = "MouseAccessibilityService"

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

    var instance: MouseAccessibilityService? = null
      internal set
  }
}

private val InputDevice.isJoystick: Boolean
  get() = isExternal && isEnabled && supportsSource(SOURCE_JOYSTICK)

private class CursorDisplayState(
  val overlay: CursorAccessibilityOverlay,
  private val handler: Handler,
  val cursorDisplayTimeoutMilliseconds: Long = 1500L,
  private val onShow: (Int, SurfaceControl) -> Unit,
) : Closeable {
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

    SurfaceControl.Transaction()
      .setVisibility(overlay.surfaceControl, true)
      .apply()
    onShow(overlay.displayInfo.displayId, overlay.surfaceControl)
    isShown = true
  }

  /** Hides the [overlay] managed by this state. */
  fun hide() {
    cancelHider()
    if (!isShown) {
      return
    }

    SurfaceControl.Transaction()
      .setVisibility(overlay.surfaceControl, false)
      .reparent(overlay.surfaceControl, null)
      .apply()
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
        State.STATE_PRESSED_TAP to Color.rgb(200, 225, 255),
        State.STATE_PRESSED_LONG_TOUCH to Color.rgb(76, 217, 100),
        State.STATE_PRESSED_SLOW_DRAG to Color.rgb(33, 150, 243),
        State.STATE_PRESSED_FLING to Color.rgb(255, 64, 129),
      )
  }

  override fun close() {
    hide()
    overlay.close()
  }
}

private fun GestureBuilder.Action.toCursorState(): CursorDisplayState.State =
  when (this) {
    GestureBuilder.Action.TOUCH -> CursorDisplayState.State.STATE_PRESSED_TAP
    GestureBuilder.Action.LONG_TOUCH -> CursorDisplayState.State.STATE_PRESSED_LONG_TOUCH
    GestureBuilder.Action.DRAG -> CursorDisplayState.State.STATE_PRESSED_SLOW_DRAG
    GestureBuilder.Action.FLING -> CursorDisplayState.State.STATE_PRESSED_FLING
  }
