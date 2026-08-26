package work.bearbrains.joymouse.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import work.bearbrains.joymouse.MainActivity
import work.bearbrains.joymouse.ShadowGestureDescriptionCustom

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [ShadowGestureDescriptionCustom::class])
class MainActivityRobolectricTest {

  @Test
  fun testCreate_launchesSuccessfully() {
    val activityController = Robolectric.buildActivity(MainActivity::class.java).setup()
    val activity = activityController.get()
    assertThat(activity.isFinishing).isFalse()
  }
}
