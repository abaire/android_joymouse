package work.bearbrains.joymouse.ui

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.VectorDrawable
import android.view.Surface
import android.view.SurfaceControl
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import java.io.Closeable
import work.bearbrains.joymouse.DisplayInfo
import work.bearbrains.joymouse.R
import work.bearbrains.joymouse.input.CursorConfig

/**
 * Manages a [SurfaceControl] into which a cursor image is rendered.
 *
 * Samsung DEX does not appear to render TYPE_ACCESSIBILITY_OVERLAY views, so a workaround via
 * [attachAccessibilityOverlayToDisplay] is provided through this class.
 */
class CursorAccessibilityOverlay(
  val displayInfo: DisplayInfo,
  initialConfig: CursorConfig = CursorConfig.DEFAULT,
) : Closeable {
  enum class State {
    STATE_RELEASED,
    STATE_PRESSED_TAP,
    STATE_PRESSED_LONG_TOUCH,
    STATE_PRESSED_SLOW_DRAG,
    STATE_PRESSED_FLING,
  }

  private var lastX = 0f
  private var lastY = 0f

  private var activeSurface: Surface? = null
  private val transaction = SurfaceControl.Transaction()

  var cursorConfig: CursorConfig = initialConfig
    set(value) {
      if (field == value) {
        return
      }
      field = value
      updateSurface()
    }

  var cursorState: State = State.STATE_RELEASED
    set(value) {
      if (field == value) {
        return
      }
      field = value
      updateSurface()
    }

  override fun close() {
    transaction
      .setVisibility(surfaceControl, false)
      .reparent(surfaceControl, null)
      .apply()
    transaction.close()
    activeSurface?.release()
    activeSurface = null
    surfaceControl.release()
  }

  private fun updateSurface() {
    activeSurface?.release()
    activeSurface = buildSurface(displayInfo.context, surfaceControl, cursorState, cursorConfig)
    draw(lastX, lastY)
  }

  private val baseDrawable =
    ContextCompat.getDrawable(displayInfo.context, R.drawable.mouse_cursor) as VectorDrawable

  /** The [SurfaceControl] into which the cursor will be rendered. */
  val surfaceControl =
    SurfaceControl.Builder()
      .apply {
        setName("CursorAccessibilityOverlay")
        setBufferSize(baseDrawable.intrinsicWidth, baseDrawable.intrinsicHeight)
        setHidden(false)
        setFormat(PixelFormat.TRANSLUCENT)
      }
      .build()
      .also { surfaceControl ->
        activeSurface?.release()
        activeSurface = buildSurface(displayInfo.context, surfaceControl, cursorState, cursorConfig)

        transaction
          .setFrameRate(surfaceControl, 60f, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT)
          .apply()
      }

  /** Draws the cursor at the given position. */
  fun draw(x: Float, y: Float) {
    lastX = x
    lastY = y
    val isCenteredShape =
      cursorConfig.changeShapeForMode &&
        (cursorState == State.STATE_PRESSED_LONG_TOUCH ||
          cursorState == State.STATE_PRESSED_SLOW_DRAG)
    val offsetX = if (isCenteredShape) baseDrawable.intrinsicWidth / 2f else 0f
    val offsetY = if (isCenteredShape) baseDrawable.intrinsicHeight / 2f else 0f
    transaction.setPosition(surfaceControl, x - offsetX, y - offsetY).apply()
  }

  private companion object {
    fun buildSurface(
      context: Context,
      surfaceControl: SurfaceControl,
      state: State,
      config: CursorConfig,
    ): Surface {
      val colors = config.getColors()
      val tintColor =
        when (state) {
          State.STATE_RELEASED -> colors.released
          State.STATE_PRESSED_TAP -> colors.tap
          State.STATE_PRESSED_LONG_TOUCH -> colors.longTouch
          State.STATE_PRESSED_SLOW_DRAG -> colors.drag
          State.STATE_PRESSED_FLING -> colors.fling
        }

      val drawableRes =
        if (config.changeShapeForMode) {
          when (state) {
            State.STATE_RELEASED,
            State.STATE_PRESSED_TAP -> R.drawable.mouse_cursor
            State.STATE_PRESSED_LONG_TOUCH -> R.drawable.mouse_cursor_target
            State.STATE_PRESSED_SLOW_DRAG -> R.drawable.mouse_cursor_drag
            State.STATE_PRESSED_FLING -> R.drawable.mouse_cursor_fling
          }
        } else {
          R.drawable.mouse_cursor
        }

      val drawable =
        ContextCompat.getDrawable(context, drawableRes)!!.mutate() as VectorDrawable
      return Surface(surfaceControl).apply {
        val canvas = lockHardwareCanvas()

        val dirtyRect = Rect(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        canvas.clipRect(dirtyRect)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        DrawableCompat.setTint(drawable, tintColor)
        DrawableCompat.setTintMode(drawable, PorterDuff.Mode.MULTIPLY)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)

        unlockCanvasAndPost(canvas)
      }
    }
  }
}
