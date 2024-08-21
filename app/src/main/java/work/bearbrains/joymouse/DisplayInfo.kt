package work.bearbrains.joymouse

import android.content.Context

/** Encapsulates information about an attached [Display]. */
data class DisplayInfo(
  val displayId: Int,
  val context: Context,
  val windowWidth: Float,
  val windowHeight: Float,
)
