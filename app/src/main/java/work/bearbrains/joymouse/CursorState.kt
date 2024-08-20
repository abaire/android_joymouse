package work.bearbrains.joymouse

import android.os.Handler
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
  private val actionTriggers: List<Pair<VirtualButton, Action>>,
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
    SWIPE_UP,
    SWIPE_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT,
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
            buttonStates[negativeKeycode] = button.isNegativePressed
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
    if (toggleButton.update(buttonStates) && toggleButton.isPressed) {
      isEnabled = !isEnabled
      return true
    }

    if (primaryButton.update(buttonStates)) {
      onUpdatePrimaryButton(this)
    }

    val consumedButtons = mutableSetOf<Int>()
    actionTriggers.forEach { (virtualButton, action) ->
      // Verify that the buttons needed for this virtual button haven't already been assigned to
      // some other virtual button.
      if (consumedButtons.intersect(virtualButton.exclusionKeycodes).isNotEmpty()) {
        if (virtualButton.isPressed) {
          virtualButton.reset()
        }
        return@forEach
      }

      if (virtualButton.update(buttonStates) && !virtualButton.isPressed) {
        onAction(this, action)
        consumedButtons.addAll(virtualButton.exclusionKeycodes)
      }
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
      fun makeButtonAxis(
        axis: Int,
        keycode: Int,
        opposingKeycode: Int? = null,
        latchUntilZero: Boolean = false,
      ) =
        ButtonAxis(
          RangedAxis(axis, device.getMotionRange(axis)),
          keycode,
          opposingKeycode,
          latchUntilZero,
        )

      val buttonAxes =
        listOf(
          makeButtonAxis(
            MotionEvent.AXIS_LTRIGGER,
            KeyEvent.KEYCODE_BUTTON_L2,
            latchUntilZero = true
          ),
          makeButtonAxis(
            MotionEvent.AXIS_RTRIGGER,
            KeyEvent.KEYCODE_BUTTON_R2,
            latchUntilZero = true
          ),
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
          ),
          emptySet(),
        )

      val actionTriggers =
        listOf(
          Pair(
            ButtonChord(
              setOf(KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_DPAD_UP),
              setOf(KeyEvent.KEYCODE_DPAD_UP),
            ),
            Action.SWIPE_UP,
          ),
          Pair(
            ButtonChord(
              setOf(KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_DPAD_DOWN),
              setOf(KeyEvent.KEYCODE_DPAD_DOWN),
            ),
            Action.SWIPE_DOWN,
          ),
          Pair(
            ButtonChord(
              setOf(KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_DPAD_LEFT),
              setOf(KeyEvent.KEYCODE_DPAD_LEFT),
            ),
            Action.SWIPE_LEFT,
          ),
          Pair(
            ButtonChord(
              setOf(KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_DPAD_RIGHT),
              setOf(KeyEvent.KEYCODE_DPAD_RIGHT),
            ),
            Action.SWIPE_RIGHT,
          ),
          Pair(ButtonMultiplexer(setOf(KeyEvent.KEYCODE_BUTTON_MODE)), Action.HOME),
          Pair(
            ButtonMultiplexer(
              setOf(
                KeyEvent.KEYCODE_BUTTON_SELECT,
                KeyEvent.KEYCODE_BUTTON_B,
              )
            ),
            Action.BACK,
          ),
          Pair(ButtonMultiplexer(setOf(KeyEvent.KEYCODE_BUTTON_START)), Action.RECENTS),
          Pair(ButtonMultiplexer(setOf(KeyEvent.KEYCODE_DPAD_UP)), Action.DPAD_UP),
          Pair(ButtonMultiplexer(setOf(KeyEvent.KEYCODE_DPAD_DOWN)), Action.DPAD_DOWN),
          Pair(ButtonMultiplexer(setOf(KeyEvent.KEYCODE_DPAD_LEFT)), Action.DPAD_LEFT),
          Pair(ButtonMultiplexer(setOf(KeyEvent.KEYCODE_DPAD_RIGHT)), Action.DPAD_RIGHT),
          Pair(ButtonMultiplexer(setOf(KeyEvent.KEYCODE_BUTTON_A)), Action.ACTIVATE),
        )

      return CursorState(
        device.id,
        handler,
        xAxis = RangedAxis(xAxis, device.getMotionRange(xAxis)),
        yAxis = RangedAxis(yAxis, device.getMotionRange(yAxis)),
        buttonAxes,
        toggleChord,
        primaryButton = ButtonMultiplexer(setOf(KeyEvent.KEYCODE_BUTTON_R2)),
        actionTriggers = actionTriggers,
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

  // TODO: REMOVEME
  var rawDeflection = 0f
    private set

  /**
   * Updates the [deflection] value for this axis. Returns true if the value was substantively
   * modified.
   */
  fun update(event: MotionEvent): Boolean {
    rawDeflection = event.getAxisValue(axis)
    val newValue = if (rawDeflection.absoluteValue < range.flat) 0f else rawDeflection

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

  /**
   * [MotionEvent]s appear to cancel [GestureDescription]s, so virtual buttons may be set to refrain
   * from emitting an "up" event until they return to zero.
   */
  private val latchUntilZero: Boolean = false,
) {
  var isPositivePressed = false
    private set

  var isNegativePressed = false
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

    var ret = false

    val pastPositiveThreshold = axis.deflection >= TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD
    if (!isPositivePressed) {
      if (pastPositiveThreshold) {
        isPositivePressed = true
        ret = true
      }
    } else {
      if (latchUntilZero) {
        if (axis.deflection <= 0f) {
          isPositivePressed = false
          ret = true
        }
      } else if (!pastPositiveThreshold) {
        isPositivePressed = false
        ret = true
      }
    }

    if (negativeKeycode != null) {
      val pastNegativeThreshold = axis.deflection <= -TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD
      if (!isNegativePressed) {
        if (pastNegativeThreshold) {
          isNegativePressed = true
          ret = true
        }
      } else {
        if (latchUntilZero) {
          if (axis.deflection >= 0f) {
            isNegativePressed = false
            ret = true
          }
        } else if (!pastNegativeThreshold) {
          isNegativePressed = false
          ret = true
        }
      }
    }

    return ret
  }

  private companion object {
    private const val TAG = "ButtonAxis"
    private const val TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD = 0.8f
  }
}

private interface VirtualButton {
  /** Whether the virtual button is considered pressed. */
  val isPressed: Boolean

  /**
   * Set of button IDs that must not have been consumed by some other virtual button for this button
   * to be considered pressed.
   */
  val exclusionKeycodes: Set<Int>

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
private data class ButtonChord(val keycodes: Set<Int>, override val exclusionKeycodes: Set<Int>) :
  VirtualButton {
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
private data class ButtonMultiplexer(private val keycodes: Set<Int>) : VirtualButton {
  override var isPressed = false
    private set

  override val exclusionKeycodes: Set<Int>
    get() = keycodes

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
