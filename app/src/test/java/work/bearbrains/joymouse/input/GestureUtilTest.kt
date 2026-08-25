package work.bearbrains.joymouse.input

import android.content.Context
import android.view.ViewConfiguration
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
import kotlin.time.Duration.Companion.milliseconds
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
internal class GestureUtilTest {
  val context: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun sanityCheckViewConfiguration() {
    val config = ViewConfiguration.get(context)

    Truth.assertThat(config.scaledMinimumFlingVelocity).isEqualTo(50)
  }

  @Test
  fun testFlingTimeBetween() {
    val config = ViewConfiguration.get(context)
    val sut = GestureUtil(config, MOCK_MAX_GESTURE_DURATION)

    Truth.assertThat(sut.flingTimeBetween(0f, 0f, 50f, 0f)).isEqualTo(12.milliseconds)
  }

  @Test
  fun testDragTimeBetween() {
    val config = ViewConfiguration.get(context)
    val sut = GestureUtil(config, MOCK_MAX_GESTURE_DURATION)

    // For 50 pixels and scaledMinimumFlingVelocity of 50 px/s (1000 ms),
    // a drag gesture must take longer than 1000 ms (e.g. 1010 ms) so that
    // velocity < 50 px/s (drag instead of fling).
    Truth.assertThat(sut.dragTimeBetween(0f, 0f, 50f, 0f)).isEqualTo(1010.milliseconds)
  }

  @Test
  fun testDragTimeBetween_clampedToMaxGestureDuration() {
    val config = ViewConfiguration.get(context)
    val sut = GestureUtil(config, MOCK_MAX_GESTURE_DURATION)

    // For a very large distance, drag duration should be clamped to maxGestureDuration
    Truth.assertThat(sut.dragTimeBetween(0f, 0f, 100000f, 0f)).isEqualTo(MOCK_MAX_GESTURE_DURATION)
  }

  @Test
  fun testFlingTimeBetween_clampedToMaxGestureDuration() {
    val config = ViewConfiguration.get(context)
    val sut = GestureUtil(config, MOCK_MAX_GESTURE_DURATION)

    // For a very large distance, fling duration should be clamped to maxGestureDuration
    Truth.assertThat(sut.flingTimeBetween(0f, 0f, 1000000f, 0f))
      .isEqualTo(MOCK_MAX_GESTURE_DURATION)
  }

  private companion object {
    val MOCK_MAX_GESTURE_DURATION = 5000.milliseconds
  }
}
