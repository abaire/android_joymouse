package work.bearbrains.joymouse.ui

import android.graphics.Path
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SwipeVisualizationTest {

  @Test
  fun testMeasurePaths_withEmptyPaths_returnsPositiveBounds() {
    val bounds = SwipeVisualization.measurePaths(emptyList(), outlineStroke = 20f)
    assertThat(bounds.width()).isAtLeast(1f)
    assertThat(bounds.height()).isAtLeast(1f)
  }

  @Test
  fun testMeasurePaths_withZeroLengthPath_returnsPositiveBounds() {
    val path =
      Path().apply {
        moveTo(50f, 50f)
        lineTo(50f, 50f)
      }
    val bounds = SwipeVisualization.measurePaths(listOf(path), outlineStroke = 0f)
    assertThat(bounds.width()).isAtLeast(1f)
    assertThat(bounds.height()).isAtLeast(1f)
  }

  @Test
  fun testMeasurePaths_withValidPath_includesOutlineStroke() {
    val path =
      Path().apply {
        moveTo(10f, 10f)
        lineTo(100f, 100f)
      }
    val outlineStroke = 10f
    val bounds = SwipeVisualization.measurePaths(listOf(path), outlineStroke = outlineStroke)
    assertThat(bounds.left).isEqualTo(10f - outlineStroke)
    assertThat(bounds.top).isEqualTo(10f - outlineStroke)
    assertThat(bounds.right).isEqualTo(100f + outlineStroke)
    assertThat(bounds.bottom).isEqualTo(100f + outlineStroke)
  }

  @Test
  fun testBuildSurfaceControl_withZeroDimensions_doesNotThrow() {
    val surfaceControl = SwipeVisualization.buildSurfaceControl(0, 0)
    assertThat(surfaceControl).isNotNull()
    surfaceControl.release()
  }

  @Test
  fun testBuildSurfaceControl_withNegativeDimensions_doesNotThrow() {
    val surfaceControl = SwipeVisualization.buildSurfaceControl(-10, -20)
    assertThat(surfaceControl).isNotNull()
    surfaceControl.release()
  }

  @Test
  fun testBuildSurfaceControl_withPositiveDimensions_buildsSuccessfully() {
    val surfaceControl = SwipeVisualization.buildSurfaceControl(100, 200)
    assertThat(surfaceControl).isNotNull()
    surfaceControl.release()
  }
}
