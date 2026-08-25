package work.bearbrains.joymouse

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDisplayManager
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [MouseAccessibilityServiceTest.ShadowGestureDescription::class])
class MouseAccessibilityServiceTest {

  @Implements(android.accessibilityservice.GestureDescription::class)
  class ShadowGestureDescription {
    companion object {
      @Implementation
      @JvmStatic
      fun getMaxGestureDuration(): Long = 60_000L
    }
  }

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

  @Test
  fun testCycleDisplay_withSingleDisplay_showsToast() {
    val serviceController = Robolectric.buildService(MouseAccessibilityService::class.java)
    val service = serviceController.create().get()

    service.onServiceConnected()

    val joystickDevice = mock<InputDevice>()
    org.mockito.kotlin.whenever(joystickDevice.id).thenReturn(1)
    service.addJoystickDevice(joystickDevice)

    // L2 Down (Left Trigger)
    service.onKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_L2, 0, 0, 1, 0))
    // L1 Down (Left Shoulder)
    service.onKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_L1, 0, 0, 1, 0))
    // L1 Up
    service.onKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_L1, 0, 0, 1, 0))

    val toastText = ShadowToast.getTextOfLatestToast()
    assertThat(toastText).isEqualTo(service.getString(R.string.toast_single_display))
  }

  @Test
  fun testCycleDisplay_withMultipleDisplays_switchesDisplayAndShowsToast() {
    // Add secondary display (e.g. DeX display)
    val displayId = ShadowDisplayManager.addDisplay("1920x1080")

    val serviceController = Robolectric.buildService(MouseAccessibilityService::class.java)
    val service = serviceController.create().get()
    service.onServiceConnected()

    val joystickDevice = mock<InputDevice>()
    org.mockito.kotlin.whenever(joystickDevice.id).thenReturn(1)
    service.addJoystickDevice(joystickDevice)

    // L2 Down (Left Trigger)
    service.onKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_L2, 0, 0, 1, 0))
    // L1 Down (Left Shoulder)
    service.onKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_L1, 0, 0, 1, 0))
    // L1 Up -> Cycle display backward
    service.onKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_L1, 0, 0, 1, 0))

    val toastText = ShadowToast.getTextOfLatestToast()
    assertThat(toastText).isEqualTo(service.getString(R.string.toast_active_display, displayId))

    // Now cycle forward with L2 + R1
    service.onKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_R1, 0, 0, 1, 0))
    service.onKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_R1, 0, 0, 1, 0))

    val forwardToastText = ShadowToast.getTextOfLatestToast()
    assertThat(forwardToastText).isEqualTo(service.getString(R.string.toast_active_display, Display.DEFAULT_DISPLAY))
  }

  @Test
  fun testCycleDisplay_whenSecondaryDisplayRemoved_handlesGracefully() {
    val displayId = ShadowDisplayManager.addDisplay("1920x1080")

    val serviceController = Robolectric.buildService(MouseAccessibilityService::class.java)
    val service = serviceController.create().get()
    service.onServiceConnected()

    val joystickDevice = mock<InputDevice>()
    org.mockito.kotlin.whenever(joystickDevice.id).thenReturn(1)
    service.addJoystickDevice(joystickDevice)

    // Cycle to secondary display
    service.onKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_L2, 0, 0, 1, 0))
    service.onKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_L1, 0, 0, 1, 0))
    service.onKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_L1, 0, 0, 1, 0))

    assertThat(ShadowToast.getTextOfLatestToast())
      .isEqualTo(service.getString(R.string.toast_active_display, displayId))

    // Remove secondary display
    ShadowDisplayManager.removeDisplay(displayId)
    service.onDisplayRemoved(displayId)

    // Now cycling displays should indicate only 1 display is available
    service.onKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_L1, 0, 0, 1, 0))
    service.onKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_L1, 0, 0, 1, 0))

    assertThat(ShadowToast.getTextOfLatestToast())
      .isEqualTo(service.getString(R.string.toast_single_display))
  }

  @Test
  fun testPrimaryDeviceSelection_firstDeviceBecomesPrimary_otherDevicesIgnored() {
    val serviceController = Robolectric.buildService(MouseAccessibilityService::class.java)
    val service = serviceController.create().get()
    service.onServiceConnected()

    val device1 = mock<InputDevice>()
    org.mockito.kotlin.whenever(device1.id).thenReturn(1)
    service.addJoystickDevice(device1)

    val device2 = mock<InputDevice>()
    org.mockito.kotlin.whenever(device2.id).thenReturn(2)
    service.addJoystickDevice(device2)

    assertThat(service.primaryDeviceId).isNull()

    // Send key event from device 1
    val consumed1 =
      service.onKeyEvent(
        KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_A, 0, 0, 1, 0)
      )
    assertThat(consumed1).isTrue()
    assertThat(service.primaryDeviceId).isEqualTo(1)

    // Key event from device 2 should not be consumed by JoyMouse service
    val consumed2 =
      service.onKeyEvent(
        KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_A, 0, 0, 2, 0)
      )
    assertThat(consumed2).isFalse()
  }

  @Test
  fun testSelectPrimaryDevice_withSingleDevice_showsToast() {
    val serviceController = Robolectric.buildService(MouseAccessibilityService::class.java)
    val service = serviceController.create().get()
    service.onServiceConnected()

    val device1 = mock<InputDevice>()
    org.mockito.kotlin.whenever(device1.id).thenReturn(1)
    service.addJoystickDevice(device1)

    // L2 Down (Left Trigger) + SELECT Down + SELECT Up
    service.onKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_L2, 0, 0, 1, 0))
    service.onKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT, 0, 0, 1, 0))
    service.onKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_SELECT, 0, 0, 1, 0))

    val toastText = ShadowToast.getTextOfLatestToast()
    assertThat(toastText).isEqualTo(service.getString(R.string.toast_single_device))
    assertThat(service.primaryDeviceId).isEqualTo(1)
  }

  @Test
  fun testSelectPrimaryDevice_withMultipleDevices_launchesSelectionActivity() {
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

    // Initially device 1 interacts and becomes primary
    service.onKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_A, 0, 0, 1, 0))
    service.onKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_A, 0, 0, 1, 0))
    assertThat(service.primaryDeviceId).isEqualTo(1)

    // L2 Down + SELECT Down + SELECT Up from device 1 to open controller picker
    service.onKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_L2, 0, 0, 1, 0))
    service.onKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT, 0, 0, 1, 0))
    service.onKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_SELECT, 0, 0, 1, 0))

    val shadowService = shadowOf(service)
    val nextIntent = shadowService.nextStartedActivity
    assertThat(nextIntent).isNotNull()
    assertThat(nextIntent.component?.className)
      .isEqualTo(work.bearbrains.joymouse.ui.ControllerSelectionActivity::class.java.name)
  }

  @Test
  fun testGetConnectedControllers_returnsAllDevicesWithPrimaryStatus() {
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

    service.primaryDeviceId = 1

    val controllers = service.getConnectedControllers()
    assertThat(controllers).hasSize(2)
    assertThat(controllers[0].id).isEqualTo(1)
    assertThat(controllers[0].isPrimary).isTrue()
    assertThat(controllers[1].id).isEqualTo(2)
    assertThat(controllers[1].isPrimary).isFalse()
  }

  @Test
  fun testSetPrimaryDevice_switchesPrimaryAndShowsToast() {
    val serviceController = Robolectric.buildService(MouseAccessibilityService::class.java)
    val service = serviceController.create().get()
    service.onServiceConnected()

    val device1 = mock<InputDevice>()
    org.mockito.kotlin.whenever(device1.id).thenReturn(1)
    service.addJoystickDevice(device1)

    val device2 = mock<InputDevice>()
    org.mockito.kotlin.whenever(device2.id).thenReturn(2)
    service.addJoystickDevice(device2)

    service.primaryDeviceId = 1

    service.setPrimaryDevice(2)
    assertThat(service.primaryDeviceId).isEqualTo(2)

    val toastText = ShadowToast.getTextOfLatestToast()
    assertThat(toastText).isEqualTo(service.getString(R.string.toast_active_device, service.getString(R.string.controller_fallback_name, 2)))

    // Now device 2 events are consumed, device 1 events are ignored
    val consumed2 =
      service.onKeyEvent(
        KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_A, 0, 0, 2, 0)
      )
    assertThat(consumed2).isTrue()

    val consumed1 =
      service.onKeyEvent(
        KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_A, 0, 0, 1, 0)
      )
    assertThat(consumed1).isFalse()
  }

  @Test
  fun testPrimaryDevice_failoverOnDisconnect() {
    val serviceController = Robolectric.buildService(MouseAccessibilityService::class.java)
    val service = serviceController.create().get()
    service.onServiceConnected()

    val device1 = mock<InputDevice>()
    org.mockito.kotlin.whenever(device1.id).thenReturn(1)
    service.addJoystickDevice(device1)

    val device2 = mock<InputDevice>()
    org.mockito.kotlin.whenever(device2.id).thenReturn(2)
    service.addJoystickDevice(device2)

    // Device 1 becomes primary
    service.onKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_A, 0, 0, 1, 0))
    service.onKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_A, 0, 0, 1, 0))
    assertThat(service.primaryDeviceId).isEqualTo(1)

    // Disconnect device 1 -> failover to device 2
    service.onInputDeviceRemoved(1)
    assertThat(service.primaryDeviceId).isEqualTo(2)

    // Disconnect device 2 -> primary becomes null
    service.onInputDeviceRemoved(2)
    assertThat(service.primaryDeviceId).isNull()
  }
}
