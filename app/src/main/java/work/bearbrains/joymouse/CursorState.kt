package work.bearbrains.joymouse

import android.os.Handler
import android.util.Log
import android.view.InputDevice
import android.view.InputDevice.MotionRange
import android.view.KeyEvent
import android.view.MotionEvent
import kotlin.math.absoluteValue

/** Encapsulates state for a virtual cursor that is controlled by axes from [MotionEvent]s. */
class CursorState
private constructor(
  /** The ID of the physical device that this repeater is associated with. */
  val deviceId: Int,
  private val handler: Handler,
  private val xAxis: RangedAxis,
  private val yAxis: RangedAxis,
  private val buttonAxes: List<ButtonAxis>,
  private val toggleButton: VirtualButton,
  private val primaryButton: VirtualButton,
  private val homeButton: VirtualButton,
  private val backButton: VirtualButton,
  private val recentsButton: VirtualButton,
  private val dpadUpButton: VirtualButton,
  private val dpadDownButton: VirtualButton,
  private val dpadLeftButton: VirtualButton,
  private val dpadRightButton: VirtualButton,
  private val activateButton: VirtualButton,
  private val windowWidth: Float,
  private val windowHeight: Float,
  private val onUpdatePosition: (CursorState) -> Unit,
  private val onUpdatePrimaryButton: (CursorState) -> Unit,
  private val onAction: (CursorState, Action) -> Unit,
  private val onEnableChanged: (CursorState) -> Unit,
) {

  enum class Action {
    BACK,
    HOME,
    RECENTS,
    DPAD_UP,
    DPAD_DOWN,
    DPAD_LEFT,
    DPAD_RIGHT,
    ACTIVATE,
  }

  private val eventRepeater =
    object : Runnable {
      private val REPEAT_DELAY_MILLISECONDS = 10L

      override fun run() {
        applyDeflection()
        restart()
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

  private val buttonStates =
    mutableMapOf(
      KeyEvent.KEYCODE_BUTTON_A to false,
      KeyEvent.KEYCODE_BUTTON_B to false,
      KeyEvent.KEYCODE_BUTTON_X to false,
      KeyEvent.KEYCODE_BUTTON_Y to false,
      KeyEvent.KEYCODE_BUTTON_L1 to false,
      KeyEvent.KEYCODE_BUTTON_L2 to false,
      KeyEvent.KEYCODE_BUTTON_R1 to false,
      KeyEvent.KEYCODE_BUTTON_R2 to false,
      KeyEvent.KEYCODE_BUTTON_SELECT to false,
      KeyEvent.KEYCODE_BUTTON_START to false,
      KeyEvent.KEYCODE_BUTTON_THUMBL to false,
      KeyEvent.KEYCODE_BUTTON_THUMBR to false,
      KeyEvent.KEYCODE_MEDIA_RECORD to false,
      KeyEvent.KEYCODE_BUTTON_MODE to false, // Xbox button
      KeyEvent.KEYCODE_DPAD_LEFT to false,
      KeyEvent.KEYCODE_DPAD_RIGHT to false,
      KeyEvent.KEYCODE_DPAD_UP to false,
      KeyEvent.KEYCODE_DPAD_DOWN to false,
    )

  /** Whether or not this virtual cursor is enabled or has been toggled off via a button chord. */
  var isEnabled: Boolean = true
    private set(value) {
      if (!value) {
        eventRepeater.cancel()
        if (primaryButton.isPressed) {
          primaryButton.reset()
          onUpdatePrimaryButton(this)
        }
      }
      field = value
      onEnableChanged(this)
    }

  /** The current X coordinate of the pointer (in pixels) */
  var pointerX = windowWidth * 0.5f
    private set

  /** The current Y coordinate of the pointer (in pixels) */
  var pointerY = windowHeight * 0.5f
    private set

  /** The X coordinate (in pixels) of the pointer prior to the most recent move. */
  var lastPointerX = pointerX
    private set

  /** The Y coordinate (in pixels) of the pointer prior to the most recent move. */
  var lastPointerY = pointerY
    private set

  /** The default velocity of the pointer. */
  private val defaultVelocityPixelsPerNanosecond =
    calculateDefaultVelocityForWindow(windowWidth, windowHeight)

  private var lastEventTimeMilliseconds: Long? = null

  /** Indicates that the primary virtual mouse button is currently pressed. */
  val isPrimaryButtonPressed: Boolean
    get() = primaryButton.isPressed

  /** Indicates whether any measurable deflection has been applied. */
  val hasDeflection: Boolean
    get() = xAxis.deflection != 0f || yAxis.deflection != 0f

  /** Indicates that the cursor speed should be set to "fast". */
  val isFastCursorEnabled: Boolean
    get() = buttonStates.getOrDefault(KeyEvent.KEYCODE_BUTTON_L2, false)

  /** Stops repeating events for this state. */
  fun cancelRepeater() {
    eventRepeater.cancel()
  }

  /**
   * Updates the stored xDeflection and yDeflection with the contents of the given [MotionEvent].
   */
  fun update(event: MotionEvent) {
    processAxesAsButtons(event)

    if (!isEnabled) {
      return
    }

    val xMoved = xAxis.update(event)
    val yMoved = yAxis.update(event)
    if (!xMoved && !yMoved) {
      return
    }

    applyDeflection()

    if (hasDeflection) {
      eventRepeater.restart()
    } else {
      eventRepeater.cancel()
    }
  }

  private fun processAxesAsButtons(event: MotionEvent) {
    var buttonStatesChanged = false

    for (button in buttonAxes) {
      button.update(event).let { wasModified ->
        if (wasModified) {
          buttonStates[button.positiveKeycode] = button.isPositivePressed
          button.negativeKeycode?.let { negativeKeycode ->
            buttonStates[negativeKeycode] = button.isNegativeePressed
          }
          buttonStatesChanged = true
        }
      }
    }

    if (buttonStatesChanged) {
      onButtonStatesChanged()
    }
  }

  /** Applies the last calculated deflection values. */
  fun applyDeflection() {
    val timeDelta = lastEventTimeMilliseconds?.let { (System.nanoTime() - it) } ?: 0L
    lastEventTimeMilliseconds = System.nanoTime()

    lastPointerX = pointerX
    lastPointerY = pointerY

    // If timeDelta is very large there was probably an intentional gap in user input and this is
    // the start of a new movement.
    if (timeDelta > INPUT_GAP_NANOSECONDS || timeDelta == 0L) {
      return
    }

    val velocity =
      if (isFastCursorEnabled) {
        defaultVelocityPixelsPerNanosecond * FAST_CURSOR_MODIFIER
      } else {
        defaultVelocityPixelsPerNanosecond
      }

    val dX = xAxis.deflection * timeDelta * velocity
    val dY = yAxis.deflection * timeDelta * velocity

    pointerX = (pointerX + dX).coerceIn(0f, windowWidth)
    pointerY = (pointerY + dY).coerceIn(0f, windowHeight)

    onUpdatePosition(this)
  }

  /** Processes a press/release event. Returns true if the event was consumed. */
  fun handleButtonEvent(isDown: Boolean, keyCode: Int): Boolean {
    buttonStates[keyCode] = isDown

    return onButtonStatesChanged()
  }

  /**
   * Processes the current state of all buttons. Returns "true" if the event that triggered this
   * call should be consumed and not passed to further handlers.
   */
  private fun onButtonStatesChanged(): Boolean {
    Log.d(TAG, "Axis as button updated press state")
    buttonStates.forEach { (k, v) ->
      if (v) {
        Log.d(TAG, "\t${KeyEvent.keyCodeToString(k)}")
      }
    }

    if (toggleButton.update(buttonStates) && toggleButton.isPressed) {
      isEnabled = !isEnabled
      return true
    }

    if (primaryButton.update(buttonStates)) {
      onUpdatePrimaryButton(this)
    }

    fun justReleased(button: VirtualButton): Boolean =
      button.update(buttonStates) && !button.isPressed

    if (justReleased(homeButton)) {
      onAction(this, Action.HOME)
    }

    if (justReleased(backButton)) {
      onAction(this, Action.BACK)
    }

    if (justReleased(recentsButton)) {
      onAction(this, Action.RECENTS)
    }

    if (justReleased(dpadUpButton)) {
      onAction(this, Action.DPAD_UP)
    }
    if (justReleased(dpadDownButton)) {
      onAction(this, Action.DPAD_DOWN)
    }
    if (justReleased(dpadLeftButton)) {
      onAction(this, Action.DPAD_LEFT)
    }
    if (justReleased(dpadRightButton)) {
      onAction(this, Action.DPAD_RIGHT)
    }
    if (justReleased(activateButton)) {
      onAction(this, Action.ACTIVATE)
    }

    return isEnabled
  }

  companion object {
    const val TAG = "CursorState"
    private const val INPUT_GAP_NANOSECONDS = 150_000_000L

    private const val FAST_CURSOR_MODIFIER = 2f

    /**
     * Creates a new [JoystickState] instance for the given device using the given axes. The axes
     * must be `MotionEvent.AXIS_*` constants (e.g., `MotionEvent.AXIS_Z`).
     */
    fun create(
      device: InputDevice,
      handler: Handler,
      xAxis: Int,
      yAxis: Int,
      windowWidth: Float,
      windowHeight: Float,
      onUpdatePosition: (CursorState) -> Unit,
      onUpdatePrimaryButton: (CursorState) -> Unit,
      onAction: (CursorState, Action) -> Unit,
      onEnableChanged: (CursorState) -> Unit,
    ): CursorState {
      fun makeButtonAxis(axis: Int, keycode: Int, opposingKeycode: Int? = null) =
        ButtonAxis(RangedAxis(axis, device.getMotionRange(axis)), keycode, opposingKeycode)

      val buttonAxes =
        listOf(
          makeButtonAxis(MotionEvent.AXIS_LTRIGGER, KeyEvent.KEYCODE_BUTTON_L2),
          makeButtonAxis(MotionEvent.AXIS_RTRIGGER, KeyEvent.KEYCODE_BUTTON_R2),
          makeButtonAxis(
            MotionEvent.AXIS_HAT_X,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_LEFT
          ),
          makeButtonAxis(
            MotionEvent.AXIS_HAT_Y,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_UP
          ),
        )

      val toggleChord =
        ButtonChord(
          setOf(
            KeyEvent.KEYCODE_BUTTON_L1,
            KeyEvent.KEYCODE_BUTTON_L2,
            KeyEvent.KEYCODE_BUTTON_R1,
            KeyEvent.KEYCODE_BUTTON_R2,
          )
        )

      return CursorState(
        device.id,
        handler,
        xAxis = RangedAxis(xAxis, device.getMotionRange(xAxis)),
        yAxis = RangedAxis(yAxis, device.getMotionRange(yAxis)),
        buttonAxes,
        toggleChord,
        primaryButton = ButtonMultiplexer(setOf(KeyEvent.KEYCODE_BUTTON_R2)),
        homeButton = ButtonMultiplexer(setOf(KeyEvent.KEYCODE_BUTTON_MODE)),
        backButton =
          ButtonMultiplexer(
            setOf(
              KeyEvent.KEYCODE_BUTTON_SELECT,
              KeyEvent.KEYCODE_BUTTON_B,
            )
          ),
        recentsButton = ButtonMultiplexer(setOf(KeyEvent.KEYCODE_BUTTON_START)),
        dpadUpButton = ButtonMultiplexer(setOf(KeyEvent.KEYCODE_DPAD_UP)),
        dpadDownButton = ButtonMultiplexer(setOf(KeyEvent.KEYCODE_DPAD_DOWN)),
        dpadLeftButton = ButtonMultiplexer(setOf(KeyEvent.KEYCODE_DPAD_LEFT)),
        dpadRightButton = ButtonMultiplexer(setOf(KeyEvent.KEYCODE_DPAD_RIGHT)),
        activateButton = ButtonMultiplexer(setOf(KeyEvent.KEYCODE_BUTTON_A)),
        windowWidth,
        windowHeight,
        onUpdatePosition,
        onUpdatePrimaryButton,
        onAction,
        onEnableChanged,
      )
    }

    /**
     * The number of seconds it should take to move the cursor from the minimum width/height to the
     * maximum for whichever of the two axes is shorter.
     */
    private const val DEFAULT_SECONDS_PER_SHORT_AXIS_TRAVERSAL = 1f

    private fun calculateDefaultVelocityForWindow(windowWidth: Float, windowHeight: Float): Float {
      val shortAxis = if (windowWidth < windowHeight) windowWidth else windowHeight

      val pixelsPerSecond = shortAxis / DEFAULT_SECONDS_PER_SHORT_AXIS_TRAVERSAL

      return pixelsPerSecond / 1_000_000_000
    }
  }
}

/** Encapsulates a [MotionEvent] axis and associated [MotionRange]. */
private data class RangedAxis(val axis: Int, private val range: MotionRange) {
  var deflection = 0f
    private set

  /**
   * Updates the [deflection] value for this axis. Returns true if the value was substantively
   * modified.
   */
  fun update(event: MotionEvent): Boolean {
    val raw = event.getAxisValue(axis)
    val newValue = if (raw.absoluteValue < range.flat) 0f else raw

    if ((newValue - deflection).absoluteValue <= range.fuzz) {
      return false
    }

    deflection = newValue

    return true
  }
}

/**
 * Encapsulates a [MotionEvent] axis and associated [MotionRange] that should be mapped to binary
 * [KeyEvent] keycodes.
 */
private data class ButtonAxis(
  private val axis: RangedAxis,
  val positiveKeycode: Int,
  val negativeKeycode: Int?,
) {
  var isPositivePressed = false
    private set

  var isNegativeePressed = false
    private set

  /**
   * Updates whether or not this axis represents a pressed button.
   *
   * Returns true if the positive or negative press states were modified.
   */
  fun update(event: MotionEvent): Boolean {
    if (!axis.update(event)) {
      return false
    }

    val newPositivePressState = axis.deflection >= TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD

    var ret = false
    if (newPositivePressState != isPositivePressed) {
      isPositivePressed = newPositivePressState
      ret = true
    }

    val newNegativePressState = axis.deflection <= -TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD
    if (negativeKeycode != null && newNegativePressState != isNegativeePressed) {
      isNegativeePressed = newNegativePressState
      ret = true
    }

    return ret
  }

  private companion object {
    private const val TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD = 0.8f
  }
}

private interface VirtualButton {
  val isPressed: Boolean

  /**
   * Updates the state of this [VirtualButton] based on the given physical button states.
   *
   * Returns true if the state has changed.
   */
  fun update(buttonStates: Map<Int, Boolean>): Boolean

  /** Forcibly resets the virtual button state to unpressed. */
  fun reset()
}

/**
 * Defines a set of buttons that will be used to control a virtual button state (if all physical
 * buttons are pressed, the virtual button is considered pressed).
 */
private data class ButtonChord(val keycodes: Set<Int>) : VirtualButton {
  override var isPressed = false
    private set

  override fun update(buttonStates: Map<Int, Boolean>): Boolean {
    val currentlyPressed = keycodes.all { buttonStates.getOrDefault(it, false) }

    if (currentlyPressed == isPressed) {
      return false
    }

    isPressed = currentlyPressed
    return true
  }

  override fun reset() {
    isPressed = false
  }
}

/**
 * Defines a set of buttons that will be used to control a virtual button state (if any given
 * physical button is pressed, the virtual button is considered pressed).
 */
private data class ButtonMultiplexer(val keycodes: Set<Int>) : VirtualButton {
  override var isPressed = false
    private set

  override fun update(buttonStates: Map<Int, Boolean>): Boolean {
    val currentlyPressed = keycodes.any { buttonStates.getOrDefault(it, false) }

    if (currentlyPressed == isPressed) {
      return false
    }

    isPressed = currentlyPressed
    return true
  }

  override fun reset() {
    isPressed = false
  }
}
