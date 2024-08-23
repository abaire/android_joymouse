package work.bearbrains.joymouse.ui

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PathIterator
import android.graphics.PointF
import android.util.Log

/** Collects the [Path]s that make up the gesture into a [MutableList]. */
fun GestureDescription.collectPathsMutable(): MutableList<Path> {
  val paths = mutableListOf<Path>()
  for (i in 0 ..< strokeCount) {
    val stroke = getStroke(i)
    paths.add(stroke.path)
  }
  return paths
}

/** Returns the first point in the gesture. */
fun GestureDescription.firstPoint(point: PointF): Boolean {
  if (strokeCount == 0) {
    return false
  }

  val firstPath = getStroke(0).path
  if (!firstPath.pathIterator.hasNext()) {
    return false
  }

  val firstSegment = firstPath.pathIterator.next()
  val points = firstSegment.points
  if (points.isEmpty()) {
    return false
  }

  point.x = points[0]
  point.y = points[1]

  return true
}

/** Returns the last point in the gesture. */
fun GestureDescription.lastPoint(point: PointF): Boolean {
  if (strokeCount == 0) {
    return false
  }

  firstPoint(point)

  val lastPath = getStroke(strokeCount - 1).path
  val points = FloatArray(8)
  val verbs = lastPath.pathIterator
  while (verbs.hasNext()) {
    val verb = verbs.next(points, 0)
    when (verb) {
      PathIterator.VERB_MOVE -> {
        point.x = points[0]
        point.y = points[1]
      }
      PathIterator.VERB_LINE -> {
        point.x = points[2]
        point.y = points[3]
      }
      PathIterator.VERB_CLOSE -> {
        // Intentionally ignored.
        // TODO: Pick the centroid of the enclosed path?
      }
      else -> {
        Log.e("GestureDescriptionExt", "Ignoring unknown verb $verb")
      }
    }
  }

  return true
}
