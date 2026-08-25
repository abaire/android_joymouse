package work.bearbrains.joymouse.ui

import android.graphics.Canvas
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

/**
 * Manages a [SurfaceControl] into which a cursor image is rendered.
 *
 * Samsung DEX does not appear to render TYPE_ACCESSIBILITY_OVERLAY views, so a workaround via
 * [attachAccessibilityOverlayToDisplay] is provided through this class.
 */
class CursorAccessibilityOverlay(val displayInfo: DisplayInfo) : Closeable {
  private var lastX = 0f
  private var lastY = 0f

  private var activeSurface: Surface? = null
  private val transaction = SurfaceControl.Transaction()

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


  /** The tint that should be applied to the cursor image. */
  @ColorInt
  var tintColor = Color.WHITE
    set(value) {
      if (field == value) {
        return
      }
      field = value
      activeSurface?.release()
      activeSurface = buildSurface(displayInfo.context, surfaceControl, tintColor)

      draw(lastX, lastY)
    }

  private val vectorDrawable =
    ContextCompat.getDrawable(displayInfo.context, R.drawable.mouse_cursor) as VectorDrawable

  /** The [SurfaceControl] into which the cursor will be rendered. */
  val surfaceControl =
    SurfaceControl.Builder()
      .apply {
        setName("CursorAccessibilityOverlay")
        setBufferSize(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        setHidden(false)
        setFormat(PixelFormat.TRANSLUCENT)
      }
      .build()
      .also { surfaceControl ->
        activeSurface?.release()
        activeSurface = buildSurface(displayInfo.context, surfaceControl, tintColor)

        transaction
          .setFrameRate(surfaceControl, 60f, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT)
          .apply()
      }

  /** Draws the cursor at the given position. */
  fun draw(x: Float, y: Float) {
    lastX = x
    lastY = y
    transaction.setPosition(surfaceControl, x, y).apply()
  }

  private companion object {
    fun buildSurface(
      context: android.content.Context,
      surfaceControl: SurfaceControl,
      @ColorInt tintColor: Int
    ): Surface {
      val drawable =
        ContextCompat.getDrawable(context, R.drawable.mouse_cursor)!!.mutate() as VectorDrawable
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
