package work.bearbrains.joymouse.ui

import android.graphics.Bitmap
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

  override fun close() {
    activeSurface?.release()
    activeSurface = null
  }

  /** The tint that should be applied to the cursor image. */
  @ColorInt
  var tintColor = Color.WHITE
    set(value) {
      field = value
      activeSurface?.release()
      activeSurface = buildSurface(surfaceControl, vectorDrawable, tintColor)

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
        activeSurface = buildSurface(surfaceControl, vectorDrawable, tintColor)

        SurfaceControl.Transaction()
          .setFrameRate(surfaceControl, 60f, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT)
          .apply()
      }

  /** Draws the cursor at the given position. */
  fun draw(x: Float, y: Float) {
    lastX = x
    lastY = y
    SurfaceControl.Transaction().setPosition(surfaceControl, x, y).apply()
  }

  private companion object {
    fun buildSurface(
      surfaceControl: SurfaceControl,
      vectorDrawable: VectorDrawable,
      @ColorInt tintColor: Int
    ): Surface {
      return Surface(surfaceControl).apply {
        val canvas = lockHardwareCanvas()

        val dirtyRect = Rect(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        canvas.clipRect(dirtyRect)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val cursorBitmap = createCursorBitmap(vectorDrawable, tintColor)
        canvas.drawBitmap(cursorBitmap, 0f, 0f, null)

        unlockCanvasAndPost(canvas)
      }
    }

    private fun createCursorBitmap(
      vectorDrawable: VectorDrawable,
      @ColorInt tintColor: Int
    ): Bitmap {
      val bitmap =
        Bitmap.createBitmap(
          vectorDrawable.intrinsicWidth,
          vectorDrawable.intrinsicHeight,
          Bitmap.Config.ARGB_8888
        )

      val canvas = Canvas(bitmap)
      DrawableCompat.setTint(vectorDrawable, tintColor)
      DrawableCompat.setTintMode(vectorDrawable, PorterDuff.Mode.MULTIPLY)
      vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
      vectorDrawable.draw(canvas)

      return bitmap
    }
  }
}
