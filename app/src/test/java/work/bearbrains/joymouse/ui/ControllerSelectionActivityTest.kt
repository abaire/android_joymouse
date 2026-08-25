package work.bearbrains.joymouse.ui

import android.view.InputDevice
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import work.bearbrains.joymouse.MouseAccessibilityService
import work.bearbrains.joymouse.ShadowGestureDescriptionCustom

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [ShadowGestureDescriptionCustom::class])
class ControllerSelectionActivityTest {

  @Test
  fun testCreate_withoutService_finishesImmediately() {
    MouseAccessibilityService.instance = null
    val controller = Robolectric.buildActivity(ControllerSelectionActivity::class.java).setup()
    assertThat(controller.get().isFinishing).isTrue()
  }

  @Test
  fun testCreate_withSingleDevice_finishesImmediately() {
    val serviceController = Robolectric.buildService(MouseAccessibilityService::class.java)
    val service = serviceController.create().get()
    service.onServiceConnected()

    val device1 = mock<InputDevice>()
    org.mockito.kotlin.whenever(device1.id).thenReturn(1)
    service.addJoystickDevice(device1)

    val activityController = Robolectric.buildActivity(ControllerSelectionActivity::class.java).setup()
    assertThat(activityController.get().isFinishing).isTrue()
  }

  @Test
  fun testCreate_withMultipleDevices_launchesAndDisplays() {
    val serviceController = Robolectric.buildService(MouseAccessibilityService::class.java)
    val service = serviceController.create().get()
    service.onServiceConnected()

    val device1 = mock<InputDevice>()
    org.mockito.kotlin.whenever(device1.id).thenReturn(1)
    org.mockito.kotlin.whenever(device1.name).thenReturn("Controller 1")
    service.addJoystickDevice(device1)

    val device2 = mock<InputDevice>()
    org.mockito.kotlin.whenever(device2.id).thenReturn(2)
    org.mockito.kotlin.whenever(device2.name).thenReturn("Controller 2")
    service.addJoystickDevice(device2)

    val activityController = Robolectric.buildActivity(ControllerSelectionActivity::class.java).setup()
    val activity = activityController.get()
    assertThat(activity.isFinishing).isFalse()
  }
}
