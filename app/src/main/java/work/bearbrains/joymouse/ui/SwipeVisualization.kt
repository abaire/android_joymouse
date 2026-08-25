package work.bearbrains.joymouse.ui

import android.accessibilityservice.GestureDescription
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PixelFormat
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import android.view.Surface
import android.view.SurfaceControl
import java.io.Closeable
import kotlin.math.atan2
import kotlin.math.ceil
import work.bearbrains.joymouse.DisplayInfo

/** Renders a visualization of a [GestureDescription]. */
class SwipeVisualization(
  val displayInfo: DisplayInfo,
  gestureDescription: GestureDescription,
) : Closeable {
  /** The [SurfaceControl] into which the visualization will be rendered. */
  val surfaceControl: SurfaceControl

  private val width: Int
  private val height: Int

  init {
    val density = displayInfo.context.resources.displayMetrics.density
    val paths = gestureDescription.collectPathsMutable()
    if (paths.isNotEmpty()) {
      addArrowhead(paths, density)
    }

    val outlineStroke = OUTLINE_STROKE_DP * density
    val insideStroke = INSIDE_STROKE_DP * density

    val totalBounds = measurePaths(paths, outlineStroke)

    val origin = PointF()
    gestureDescription.firstPoint(origin)

    // Translate the paths to the origin.
    val dX = -totalBounds.left
    val dY = -totalBounds.top
    for (path in paths) {
      path.offset(dX, dY)
    }

    width = ceil(totalBounds.width()).toInt()
    height = ceil(totalBounds.height()).toInt()

    surfaceControl = buildSurfaceControl(width, height)
    val surface =
      buildSurface(
        surfaceControl,
        paths,
        originX = origin.x + dX,
        originY = origin.y + dY,
        width,
        height,
        outlineStroke,
        insideStroke,
        originRadius = ORIGIN_RADIUS_DP * density,
      )

    SurfaceControl.Transaction()
      .setPosition(surfaceControl, totalBounds.left, totalBounds.top)
      .apply()

    surface.release()
  }

  /** Releases the resources used by this visualization. */
  override fun close() {
    SurfaceControl.Transaction().reparent(surfaceControl, null).apply()
    surfaceControl.release()
  }

  private companion object {
    const val OUTLINE_STROKE_DP = 20f
    const val INSIDE_STROKE_DP = OUTLINE_STROKE_DP * 0.75f
    const val ORIGIN_RADIUS_DP = 4f
    const val ARROWHEAD_LENGTH_DP = 10f
    const val ARROWHEAD_HALF_WIDTH_DP = 5f
    const val ARROWHEAD_OFFSET_DP = 10f
    const val ARROWHEAD_SHORTEN_LENGTH_DP = 15f

    fun measurePaths(paths: List<Path>, outlineStroke: Float): RectF {
      val pathBounds = RectF()
      val totalBounds = RectF()
      for (path in paths) {
        path.computeBounds(pathBounds, false)
        if (pathBounds.width() == 0f) {
          pathBounds.right += 1f
        }
        if (pathBounds.height() == 0f) {
          pathBounds.bottom += 1f
        }
        pathBounds.inset(-outlineStroke, -outlineStroke)
        totalBounds.union(pathBounds)
      }

      return totalBounds
    }

    fun buildSurfaceControl(width: Int, height: Int): SurfaceControl {
      return SurfaceControl.Builder()
        .apply {
          setName("SwipeVisualizationAccessibilityOverlay")
          setBufferSize(width, height)
          setHidden(false)
          setFormat(PixelFormat.TRANSLUCENT)
        }
        .build()
        .also { surfaceControl ->
          SurfaceControl.Transaction()
            .setFrameRate(surfaceControl, 60f, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT)
            .apply()
        }
    }

    fun buildSurface(
      surfaceControl: SurfaceControl,
      paths: List<Path>,
      originX: Float,
      originY: Float,
      width: Int,
      height: Int,
      outlineStroke: Float,
      insideStroke: Float,
      originRadius: Float,
    ): Surface {
      val outlinePaint =
        Paint().apply {
          color = Color.BLACK
          style = Paint.Style.STROKE
          strokeWidth = outlineStroke
        }

      val insidePaint =
        Paint().apply {
          color = Color.WHITE
          style = Paint.Style.STROKE
          strokeWidth = insideStroke
        }

      return Surface(surfaceControl).apply {
        val canvas = lockHardwareCanvas()

        val dirtyRect = Rect(0, 0, width, height)
        canvas.clipRect(dirtyRect)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        for (path in paths) {
          canvas.drawPath(path, outlinePaint)
        }
        for (path in paths) {
          canvas.drawPath(path, insidePaint)
        }

        canvas.drawCircle(originX, originY, originRadius, outlinePaint)
        canvas.drawCircle(originX, originY, originRadius, insidePaint)

        unlockCanvasAndPost(canvas)
      }
    }

    fun addArrowhead(paths: MutableList<Path>, density: Float) {
      val lastPath = paths.last()

      val pathMeasure = PathMeasure(lastPath, false)
      val lastPathLength = pathMeasure.length
      val pos = FloatArray(2)
      val tan = FloatArray(2)
      pathMeasure.getPosTan(pathMeasure.length, pos, tan)

      val endX = pos[0]
      val endY = pos[1]
      val angle = Math.toDegrees(atan2(tan[1].toDouble(), tan[0].toDouble())).toFloat()

      val arrowLength = ARROWHEAD_LENGTH_DP * density
      val arrowHalfWidth = ARROWHEAD_HALF_WIDTH_DP * density
      val arrowOffset = ARROWHEAD_OFFSET_DP * density
      val shortenLength = ARROWHEAD_SHORTEN_LENGTH_DP * density

      val arrowHeadPath = Path()
      arrowHeadPath.moveTo(endX, endY)
      arrowHeadPath.lineTo(endX - arrowLength, endY - arrowHalfWidth)
      arrowHeadPath.lineTo(endX - arrowLength, endY + arrowHalfWidth)
      arrowHeadPath.close()

      val matrix = Matrix()
      matrix.postTranslate(-arrowOffset, 0f)
      matrix.postRotate(angle, endX, endY)
      arrowHeadPath.transform(matrix)

      paths.add(arrowHeadPath)

      lastPath.rewind()
      pathMeasure.getSegment(0f, lastPathLength - shortenLength, lastPath, true)
    }
  }
}
