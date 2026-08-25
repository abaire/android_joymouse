package work.bearbrains.joymouse

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MouseAccessibilityServiceTest {

  @Test
  fun testLifecycle_unbindAndDestroy_cleansUpGracefully() {
    val serviceController = Robolectric.buildService(MouseAccessibilityService::class.java)
    val service = serviceController.create().get()

    // Test unbind
    val unbindResult = service.onUnbind(Intent())
    assertThat(unbindResult).isFalse()

    // Test destroy after unbind (idempotent cleanup)
    serviceController.destroy()
  }

  @Test
  fun testLifecycle_destroyWithoutUnbind_cleansUpGracefully() {
    val serviceController = Robolectric.buildService(MouseAccessibilityService::class.java)
    serviceController.create().destroy()
  }
}
