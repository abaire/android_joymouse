package work.bearbrains.joymouse

import android.accessibilityservice.AccessibilityService.WINDOW_SERVICE
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PixelFormat
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import kotlin.math.atan2

/** Renders a visualization of a [GestureDescription]. */
class SwipeVisualization(
  gestureDescription: GestureDescription,
  context: Context,
  displayForMillis: Long = DISPLAY_FOR_MILLIS
) {
  init {
    val paths = collectPaths(gestureDescription)
    if (paths.isNotEmpty()) {
      addArrowhead(paths)
      val view = createView(paths, context)
      addView(view, context, displayForMillis)
    }
  }

  private companion object {
    const val DISPLAY_FOR_MILLIS = 500L
    const val OUTLINE_WIDTH = 20f
    const val FILL_LINE_WIDTH = OUTLINE_WIDTH * 0.75f

    fun collectPaths(gestureDescription: GestureDescription): MutableList<Path> {
      val paths = mutableListOf<Path>()
      for (i in 0 ..< gestureDescription.strokeCount) {
        val stroke = gestureDescription.getStroke(i)
        paths.add(stroke.path)
      }
      return paths
    }

    fun addArrowhead(paths: MutableList<Path>) {
      val lastPath = paths.last()

      val pathMeasure = PathMeasure(lastPath, false)
      val lastPathLength = pathMeasure.length
      val pos = FloatArray(2)
      val tan = FloatArray(2)
      pathMeasure.getPosTan(pathMeasure.length, pos, tan)

      val endX = pos[0]
      val endY = pos[1]
      val angle = Math.toDegrees(atan2(tan[1].toDouble(), tan[0].toDouble())).toFloat()

      val arrowHeadPath = Path()
      arrowHeadPath.moveTo(endX, endY)
      arrowHeadPath.lineTo(endX - 10, endY - 5)
      arrowHeadPath.lineTo(endX - 10, endY + 5)
      arrowHeadPath.close()

      val matrix = Matrix()
      matrix.postTranslate(-10f, 0f)
      matrix.postRotate(angle, endX, endY)
      arrowHeadPath.transform(matrix)

      paths.add(arrowHeadPath)

      lastPath.rewind()
      val shortenLength = 15f
      pathMeasure.getSegment(0f, lastPathLength - shortenLength, lastPath, true)
    }

    fun createView(paths: List<Path>, context: Context): View {
      val outlinePaint =
        Paint().apply {
          color = Color.BLACK
          style = Paint.Style.STROKE
          strokeWidth = OUTLINE_WIDTH
        }

      val insidePaint =
        Paint().apply {
          color = Color.WHITE
          style = Paint.Style.STROKE
          strokeWidth = FILL_LINE_WIDTH
        }

      val pathMeasure = PathMeasure(paths.first(), false)
      val pos = FloatArray(2)
      pathMeasure.getPosTan(0f, pos, null)

      return object : View(context) {
        override fun onDraw(canvas: Canvas) {
          super.onDraw(canvas)
          for (path in paths) {
            canvas.drawPath(path, outlinePaint)
          }
          for (path in paths) {
            canvas.drawPath(path, insidePaint)
          }

          canvas.drawCircle(pos[0], pos[1], 4f, outlinePaint)
          canvas.drawCircle(pos[0], pos[1], 4f, insidePaint)
        }
      }
    }

    fun addView(view: View, context: Context, hideAfterMillis: Long) {
      val windowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
      val params =
        WindowManager.LayoutParams().apply {
          type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
          format = PixelFormat.TRANSLUCENT
          flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
              WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
              WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
              WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
          width = WindowManager.LayoutParams.WRAP_CONTENT
          height = WindowManager.LayoutParams.WRAP_CONTENT
          gravity = Gravity.TOP or Gravity.START
        }

      windowManager.addView(view, params)

      Handler(context.mainLooper).postDelayed({ windowManager.removeView(view) }, hideAfterMillis)
    }
  }
}
