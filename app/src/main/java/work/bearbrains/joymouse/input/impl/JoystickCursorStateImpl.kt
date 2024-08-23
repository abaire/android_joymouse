package work.bearbrains.joymouse

import android.os.Handler
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import work.bearbrains.joymouse.input.JoystickAction
import work.bearbrains.joymouse.input.JoystickButtonProcessor
import work.bearbrains.joymouse.input.JoystickCursorState

/** Concrete implementation of [JoystickCursorState]. */
class JoystickCursorStateImpl
private constructor(
  override val deviceId: Int,
  override val displayInfo: DisplayInfo,
  private val handler: Handler,
  private val xAxis: RangedAxis,
  private val yAxis: RangedAxis,
  private val buttonAxes: List<ButtonAxis>,
  private val nanoClock: NanoClock,
  buttonProcessorFactory: JoystickButtonProcessor.Factory,
  private val onUpdatePosition: (JoystickCursorState) -> Unit,
  onAction: (JoystickCursorState, JoystickAction) -> Unit,
) : JoystickCursorState {

  private val buttonProcessor =
    buttonProcessorFactory.create() { _, action ->
      when (action) {
        JoystickAction.TOGGLE_ENABLED -> {
          isEnabled = !isEnabled
        }
        JoystickAction.PRIMARY_PRESS -> {
          isPrimaryButtonPressed = true
        }
        JoystickAction.PRIMARY_RELEASE -> {
          isPrimaryButtonPressed = false
        }
        JoystickAction.FAST_CURSOR_PRESS -> {
          isFastCursorEnabled = true
        }
        JoystickAction.FAST_CURSOR_RELEASE -> {
          isFastCursorEnabled = false
        }
        else -> {
          // Just through to the onAction handler.
        }
      }
      if (isEnabled || action == JoystickAction.TOGGLE_ENABLED) {
        onAction(this, action)
      }
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

  override var isEnabled: Boolean = true
    private set(value) {
      if (!value) {
        eventRepeater.cancel()
        buttonProcessor.reset()
      }
      field = value
    }

  override var pointerX = displayInfo.windowWidth * 0.5f
    private set

  override var pointerY = displayInfo.windowHeight * 0.5f
    private set

  private val defaultVelocityPixelsPerNanosecond =
    calculateDefaultVelocityForWindow(displayInfo.windowWidth, displayInfo.windowHeight)

  private var lastEventTimeNanoseconds: Long? = null

  override var isPrimaryButtonPressed: Boolean = false
    private set

  override var isFastCursorEnabled: Boolean = false
    private set

  /** Indicates whether any measurable deflection has been applied. */
  private val hasDeflection: Boolean
    get() = xAxis.deflection != 0f || yAxis.deflection != 0f

  override fun close() {
    isEnabled = false
  }

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
    for (button in buttonAxes) {
      button.update(event).let { wasModified ->
        if (wasModified) {
          buttonProcessor.handleButtonEvent(button.positiveKeycode, button.isPositivePressed)
          button.negativeKeycode?.let { negativeKeycode ->
            buttonProcessor.handleButtonEvent(negativeKeycode, button.isNegativePressed)
          }
        }
      }
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

  override fun handleButtonEvent(isDown: Boolean, keyCode: Int) {
    buttonProcessor.handleButtonEvent(keyCode, isDown)
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
      buttonProcessorFactory: JoystickButtonProcessor.Factory,
      onUpdatePosition: (JoystickCursorState) -> Unit,
      onAction: (JoystickCursorState, JoystickAction) -> Unit,
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

      return JoystickCursorStateImpl(
        device.id,
        displayInfo,
        handler,
        xAxis = RangedAxis(xAxis, device.getMotionRange(xAxis)),
        yAxis = RangedAxis(yAxis, device.getMotionRange(yAxis)),
        buttonAxes,
        nanoClock,
        buttonProcessorFactory,
        onUpdatePosition,
        onAction,
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
