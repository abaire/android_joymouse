package work.bearbrains.joymouse

import android.os.Handler
import android.view.InputDevice
import android.view.InputDevice.MotionRange
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.annotation.VisibleForTesting
import kotlin.math.absoluteValue

/** Concrete implementation of [JoystickCursorState]. */
class JoystickCursorStateImpl
private constructor(
  /** The ID of the physical device that this repeater is associated with. */
  val deviceId: Int,
  override val displayInfo: DisplayInfo,
  private val handler: Handler,
  private val xAxis: RangedAxis,
  private val yAxis: RangedAxis,
  private val buttonAxes: List<ButtonAxis>,
  private val toggleButton: VirtualButton,
  private val primaryButton: VirtualButton,
  private val actionTriggers: List<Pair<VirtualButton, JoystickCursorState.Action>>,
  private val onUpdatePosition: (JoystickCursorState) -> Unit,
  private val onUpdatePrimaryButton: (JoystickCursorState) -> Unit,
  private val onAction: (JoystickCursorState, JoystickCursorState.Action) -> Unit,
  private val onEnableChanged: (JoystickCursorState) -> Unit,
  private val nanoClock: NanoClock,
) : JoystickCursorState {

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

  @VisibleForTesting
  internal val buttonStates =
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

  override var isEnabled: Boolean = true
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

  override var pointerX = displayInfo.windowWidth * 0.5f
    private set

  override var pointerY = displayInfo.windowHeight * 0.5f
    private set

  private val defaultVelocityPixelsPerNanosecond =
    calculateDefaultVelocityForWindow(displayInfo.windowWidth, displayInfo.windowHeight)

  private var lastEventTimeNanoseconds: Long? = null

  override val isPrimaryButtonPressed: Boolean
    get() = primaryButton.isPressed

  override val isFastCursorEnabled: Boolean
    get() = buttonStates.getOrDefault(KeyEvent.KEYCODE_BUTTON_L2, false)

  /** Indicates whether any measurable deflection has been applied. */
  private val hasDeflection: Boolean
    get() = xAxis.deflection != 0f || yAxis.deflection != 0f

  override fun cancelRepeater() {
    eventRepeater.cancel()
  }

  override fun update(event: MotionEvent) {
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
  private fun applyDeflection() {
    val now = nanoClock.nanoTime()
    val timeDelta = lastEventTimeNanoseconds?.let { now - it } ?: 0L
    lastEventTimeNanoseconds = now

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

    pointerX = (pointerX + dX).coerceIn(0f, displayInfo.windowWidth)
    pointerY = (pointerY + dY).coerceIn(0f, displayInfo.windowHeight)

    onUpdatePosition(this)
  }

  override fun handleButtonEvent(isDown: Boolean, keyCode: Int): Boolean {
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
    const val TAG = "JoystickCursorStateImpl"
    private const val INPUT_GAP_NANOSECONDS = 150_000_000L

    private const val FAST_CURSOR_MODIFIER = 2f

    /**
     * Creates a new [JoystickState] instance for the given device using the given axes. The axes
     * must be `MotionEvent.AXIS_*` constants (e.g., `MotionEvent.AXIS_Z`).
     */
    fun create(
      device: InputDevice,
      displayInfo: DisplayInfo,
      handler: Handler,
      xAxis: Int,
      yAxis: Int,
      nanoClock: NanoClock,
      onUpdatePosition: (JoystickCursorState) -> Unit,
      onUpdatePrimaryButton: (JoystickCursorState) -> Unit,
      onAction: (JoystickCursorState, JoystickCursorState.Action) -> Unit,
      onEnableChanged: (JoystickCursorState) -> Unit,
    ): JoystickCursorState {
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
            JoystickCursorState.Action.SWIPE_UP,
          ),
          Pair(
            ButtonChord(
              setOf(KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_DPAD_DOWN),
              setOf(KeyEvent.KEYCODE_DPAD_DOWN),
            ),
            JoystickCursorState.Action.SWIPE_DOWN,
          ),
          Pair(
            ButtonChord(
              setOf(KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_DPAD_LEFT),
              setOf(KeyEvent.KEYCODE_DPAD_LEFT),
            ),
            JoystickCursorState.Action.SWIPE_LEFT,
          ),
          Pair(
            ButtonChord(
              setOf(KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_DPAD_RIGHT),
              setOf(KeyEvent.KEYCODE_DPAD_RIGHT),
            ),
            JoystickCursorState.Action.SWIPE_RIGHT,
          ),
          Pair(
            ButtonMultiplexer(setOf(KeyEvent.KEYCODE_BUTTON_MODE)),
            JoystickCursorState.Action.HOME
          ),
          Pair(
            ButtonMultiplexer(
              setOf(
                KeyEvent.KEYCODE_BUTTON_SELECT,
                KeyEvent.KEYCODE_BUTTON_B,
              )
            ),
            JoystickCursorState.Action.BACK,
          ),
          Pair(
            ButtonMultiplexer(setOf(KeyEvent.KEYCODE_BUTTON_START)),
            JoystickCursorState.Action.RECENTS
          ),
          Pair(
            ButtonMultiplexer(setOf(KeyEvent.KEYCODE_DPAD_UP)),
            JoystickCursorState.Action.DPAD_UP
          ),
          Pair(
            ButtonMultiplexer(setOf(KeyEvent.KEYCODE_DPAD_DOWN)),
            JoystickCursorState.Action.DPAD_DOWN
          ),
          Pair(
            ButtonMultiplexer(setOf(KeyEvent.KEYCODE_DPAD_LEFT)),
            JoystickCursorState.Action.DPAD_LEFT
          ),
          Pair(
            ButtonMultiplexer(setOf(KeyEvent.KEYCODE_DPAD_RIGHT)),
            JoystickCursorState.Action.DPAD_RIGHT
          ),
          Pair(
            ButtonMultiplexer(setOf(KeyEvent.KEYCODE_BUTTON_A)),
            JoystickCursorState.Action.ACTIVATE
          ),
        )

      return JoystickCursorStateImpl(
        device.id,
        displayInfo,
        handler,
        xAxis = RangedAxis(xAxis, device.getMotionRange(xAxis)),
        yAxis = RangedAxis(yAxis, device.getMotionRange(yAxis)),
        buttonAxes,
        toggleChord,
        primaryButton = ButtonMultiplexer(setOf(KeyEvent.KEYCODE_BUTTON_R2)),
        actionTriggers = actionTriggers,
        onUpdatePosition,
        onUpdatePrimaryButton,
        onAction,
        onEnableChanged,
        nanoClock,
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
internal data class RangedAxis(val axis: Int, private val range: MotionRange) {
  /** The modified deflection of this axis, between -1 and 1. */
  var deflection = 0f
    private set

  /**
   * Updates the [deflection] value for this axis. Returns true if the value was substantively
   * modified.
   */
  fun update(event: MotionEvent): Boolean {
    val rawDeflection = event.getAxisValue(axis)
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
internal data class ButtonAxis(
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

  internal companion object {
    private const val TAG = "ButtonAxis"

    @VisibleForTesting internal const val TRIGGER_AXIS_AS_BUTTON_DEFLECTION_THRESHOLD = 0.8f
  }
}

internal interface VirtualButton {
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
internal data class ButtonChord(val keycodes: Set<Int>, override val exclusionKeycodes: Set<Int>) :
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
internal data class ButtonMultiplexer(private val keycodes: Set<Int>) : VirtualButton {
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
