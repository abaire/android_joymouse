package work.bearbrains.joymouse.input

import android.content.Context
import android.view.ViewConfiguration
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
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
    val sut = GestureUtil(config, MOCK_MAX_GESTURE_DURATION_MILLISECONDS)

    Truth.assertThat(sut.flingTimeBetween(0f, 0f, 50f, 0f)).isEqualTo(12L)
  }

  @Test
  fun testDragTimeBetween() {
    val config = ViewConfiguration.get(context)
    val sut = GestureUtil(config, MOCK_MAX_GESTURE_DURATION_MILLISECONDS)

    // Expect < 50 pixels / 1000 ms
    Truth.assertThat(sut.dragTimeBetween(0f, 0f, 50f, 0f)).isEqualTo(990L)
  }

  private companion object {
    const val MOCK_MAX_GESTURE_DURATION_MILLISECONDS = 5000L
  }
}
