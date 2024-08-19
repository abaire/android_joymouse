package work.bearbrains.joymouse

import android.os.Handler
import android.view.InputDevice
import android.view.InputDevice.MotionRange
import android.view.MotionEvent
import kotlin.math.absoluteValue

/** Encapsulates state for a virtual cursor that is controlled by axes from [MotionEvent]s. */
data class CursorState(
    /** The ID of the physical device that this repeater is associated with. */
    val deviceId: Int,
    val handler: Handler,
    private val xAxis: Int,
    private val yAxis: Int,
    private val xRange: MotionRange,
    private val yRange: MotionRange,
    private val windowWidth: Float,
    private val windowHeight: Float,
    private val onUpdate: (CursorState) -> Unit,
) {

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

  private var xDeflection: Float = 0f
  private var yDeflection: Float = 0f

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

  /** The velocity of the pointer. */
  var velocityPixelsPerNanosecond: Float =
      calculateDefaultVelocityForWindow(windowWidth, windowHeight)

  private var lastEventTimeMilliseconds: Long? = null

  /** Indicates whether any measurable deflection has been applied. */
  val hasDeflection: Boolean
    get() = xDeflection != 0f || yDeflection != 0f

  /** Stops repeating events for this state. */
  fun cancelRepeater() {
    eventRepeater.cancel()
  }

  /**
   * Updates the stored xDeflection and yDeflection with the contents of the given [MotionEvent].
   */
  fun update(event: MotionEvent) {
    val rawX = event.getAxisValue(xAxis)
    val rawY = event.getAxisValue(yAxis)

    xDeflection = if (rawX.absoluteValue < xRange.flat) 0f else rawX
    yDeflection = if (rawY.absoluteValue < yRange.flat) 0f else rawY

    applyDeflection()

    if (hasDeflection) {
      eventRepeater.restart()
    } else {
      eventRepeater.cancel()
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

    val dX = xDeflection * timeDelta * velocityPixelsPerNanosecond
    val dY = yDeflection * timeDelta * velocityPixelsPerNanosecond

    pointerX = (pointerX + dX).coerceIn(0f, windowWidth)
    pointerY = (pointerY + dY).coerceIn(0f, windowHeight)

    onUpdate(this)
  }

  companion object {
    private const val INPUT_GAP_NANOSECONDS = 150_000_000L

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
        onUpdate: (CursorState) -> Unit,
    ): CursorState {
      return CursorState(
          device.id,
          handler,
          xAxis,
          yAxis,
          device.getMotionRange(xAxis),
          device.getMotionRange(yAxis),
          windowWidth,
          windowHeight,
          onUpdate,
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
