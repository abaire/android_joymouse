package work.bearbrains.joymouse

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PathIterator
import android.graphics.PointF
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.util.SparseArray
import android.view.Display
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceControl
import android.view.WindowMetrics
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.Closeable
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowAccessibilityService
import org.robolectric.shadows.ShadowDisplayManager
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowToast
import work.bearbrains.joymouse.input.GestureBuilder
import work.bearbrains.joymouse.input.JoystickAction
import work.bearbrains.joymouse.input.JoystickCursorState
import work.bearbrains.joymouse.ui.lastPoint

@Implements(android.accessibilityservice.GestureDescription::class)
class ShadowGestureDescriptionCustom {
  companion object {
    @Implementation
    @JvmStatic
    fun getMaxGestureDuration(): Long = 60_000L
  }
}

@Implements(AccessibilityService::class)
class ShadowAccessibilityServiceCustom : ShadowAccessibilityService() {
  var customServiceInfo: AccessibilityServiceInfo? = AccessibilityServiceInfo()
  var customWindowsOnAllDisplays: SparseArray<List<AccessibilityWindowInfo>> = SparseArray()
  val customAttachedOverlays = mutableListOf<Pair<Int, SurfaceControl>>()
  val customDispatchedGestures = mutableListOf<GestureDescription>()
  var shouldCancelGestures = false
  var gestureCallback: AccessibilityService.GestureResultCallback? = null

  @Implementation
  fun getServiceInfo(): AccessibilityServiceInfo? {
    return customServiceInfo
  }

  @Implementation
  fun setServiceInfo(info: AccessibilityServiceInfo?) {
    customServiceInfo = info
  }

  @Implementation
  override fun getWindowsOnAllDisplays(): SparseArray<List<AccessibilityWindowInfo>> {
    return customWindowsOnAllDisplays
  }

  @Implementation
  fun attachAccessibilityOverlayToDisplay(displayId: Int, sc: SurfaceControl) {
    customAttachedOverlays.add(displayId to sc)
  }

  @Implementation
  override fun dispatchGesture(
    gesture: GestureDescription,
    callback: AccessibilityService.GestureResultCallback?,
    handler: Handler?
  ): Boolean {
    customDispatchedGestures.add(gesture)
    gestureCallback = callback
    if (shouldCancelGestures) {
      callback?.onCancelled(gesture)
    } else {
      callback?.onCompleted(gesture)
    }
    return true
  }
}

@Implements(InputManager::class)
class ShadowInputManagerCustom {
  companion object {
    private val devices = mutableMapOf<Int, InputDevice>()
    val registeredListeners = mutableListOf<InputManager.InputDeviceListener>()

    fun addDevice(device: InputDevice) {
      devices[device.id] = device
    }

    fun removeDevice(deviceId: Int) {
      devices.remove(deviceId)
    }

    fun clear() {
      devices.clear()
      registeredListeners.clear()
    }
  }

  @Implementation
  fun getInputDeviceIds(): IntArray {
    return devices.keys.toIntArray()
  }

  @Implementation
  fun getInputDevice(id: Int): InputDevice? {
    return devices[id]
  }

  @Implementation
  fun registerInputDeviceListener(
    listener: InputManager.InputDeviceListener,
    handler: Handler?
  ) {
    registeredListeners.add(listener)
  }

  @Implementation
  fun unregisterInputDeviceListener(listener: InputManager.InputDeviceListener) {
    registeredListeners.remove(listener)
  }
}

@Implements(PathIterator::class)
class ShadowPathIteratorCustom {
  private var index = 0

  @Implementation
  fun hasNext(): Boolean = index < 2

  @Implementation
  fun next(points: FloatArray, offset: Int): Int {
    if (index == 0) {
      points[offset] = 100f
      points[offset + 1] = 100f
      index++
      return PathIterator.VERB_MOVE
    } else {
      points[offset] = 100f
      points[offset + 1] = 100f
      points[offset + 2] = 200f
      points[offset + 3] = 200f
      index++
      return PathIterator.VERB_LINE
    }
  }

  @Implementation
  fun next(): PathIterator.Segment {
    try {
      val constructor = PathIterator.Segment::class.java.getDeclaredConstructor()
      constructor.isAccessible = true
      val segment = constructor.newInstance()
      val pointsField = PathIterator.Segment::class.java.getDeclaredField("mPoints")
      pointsField.isAccessible = true
      val points = FloatArray(8)
      val verb = next(points, 0)
      pointsField.set(segment, points)
      val verbField = PathIterator.Segment::class.java.getDeclaredField("mVerb")
      verbField.isAccessible = true
      verbField.setInt(segment, verb)
      return segment
    } catch (_: Exception) {
      throw RuntimeException("Failed to construct Segment")
    }
  }
}

@Implements(WindowMetrics::class)
class ShadowWindowMetricsCustom {
  @Implementation
  fun getBounds(): Rect = Rect(0, 0, 1080, 1920)
}

@RunWith(RobolectricTestRunner::class)
@Config(
  sdk = [34],
  shadows = [
    ShadowGestureDescriptionCustom::class,
    ShadowAccessibilityServiceCustom::class,
    ShadowInputManagerCustom::class,
    ShadowPathIteratorCustom::class,
    ShadowWindowMetricsCustom::class
  ]
)
class MouseAccessibilityServiceTest {

  private lateinit var context: Context
  private lateinit var serviceController: ServiceController<MouseAccessibilityService>
  private lateinit var service: MouseAccessibilityService
  private lateinit var shadowAccessibilityService: ShadowAccessibilityServiceCustom

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    ShadowInputManagerCustom.clear()
    ShadowDisplayManager.changeDisplay(Display.DEFAULT_DISPLAY, "w1080dp-h1920dp")

    serviceController = Robolectric.buildService(MouseAccessibilityService::class.java)
    service = serviceController.create().get()
    shadowAccessibilityService = shadowOf(service) as ShadowAccessibilityServiceCustom

    // Setup default window on default display
    val defaultWindow = createMockWindow()
    val sparseArray = SparseArray<List<AccessibilityWindowInfo>>()
    sparseArray.put(Display.DEFAULT_DISPLAY, listOf(defaultWindow))
    shadowAccessibilityService.customWindowsOnAllDisplays = sparseArray
  }

  @After
  fun tearDown() {
    ShadowInputManagerCustom.clear()
  }

  // =========================================================================
  // 1. Service Lifecycle & Cleanup Tests
  // =========================================================================
  @Test
  fun testLifecycle_unbindAndDestroy_cleansUpGracefully() {
    val joystick = createMockJoystick(1)
    ShadowInputManagerCustom.addDevice(joystick)
    service.callOnServiceConnected()

    assertThat(service.getJoystickDeviceIdsToState()).containsKey(1)
    assertThat(service.getDisplayIdToCursorDisplayState()).containsKey(Display.DEFAULT_DISPLAY)

    // Test unbind
    val unbindResult = service.onUnbind(Intent())
    assertThat(unbindResult).isFalse()

    // All states and overlays should be cleaned up
    assertThat(service.getJoystickDeviceIdsToState()).isEmpty()
    assertThat(service.getDisplayIdToCursorDisplayState()).isEmpty()
    assertThat(service.getCloseableOverlays()).isEmpty()

    // Test destroy after unbind (idempotent cleanup)
    serviceController.destroy()
  }

  @Test
  fun testLifecycle_destroyWithoutUnbind_cleansUpGracefully() {
    val joystick = createMockJoystick(1)
    ShadowInputManagerCustom.addDevice(joystick)
    service.callOnServiceConnected()

    assertThat(service.getJoystickDeviceIdsToState()).isNotEmpty()

    // Destroy directly
    serviceController.destroy()

    assertThat(service.getJoystickDeviceIdsToState()).isEmpty()
    assertThat(service.getDisplayIdToCursorDisplayState()).isEmpty()
  }

  @Test
  fun testOnServiceConnected_initializesDisplaysAndDetectsJoysticks() {
    val joystick = createMockJoystick(10)
    val keyboard = createMockNonJoystick(20)
    ShadowInputManagerCustom.addDevice(joystick)
    ShadowInputManagerCustom.addDevice(keyboard)

    service.callOnServiceConnected()

    assertThat(service.getIsEnabled()).isTrue()
    assertThat(service.serviceInfo?.motionEventSources)
      .isEqualTo(InputDevice.SOURCE_JOYSTICK)

    // Joystick is detected and tracked; non-joystick is ignored
    assertThat(service.getJoystickDeviceIdsToState()).containsKey(10)
    assertThat(service.getJoystickDeviceIdsToState()).doesNotContainKey(20)

    // Display overlay is created for default display
    assertThat(service.getDisplayInfos()).containsKey(Display.DEFAULT_DISPLAY)
    assertThat(service.getDisplayIdToCursorDisplayState()).containsKey(Display.DEFAULT_DISPLAY)

    // Initial cursor position update triggers overlay attachment
    assertThat(shadowAccessibilityService.customAttachedOverlays).isNotEmpty()
    assertThat(shadowAccessibilityService.customAttachedOverlays.first().first)
      .isEqualTo(Display.DEFAULT_DISPLAY)
  }

  // =========================================================================
  // 2. Device Attach / Detach & Input Event Tests
  // =========================================================================

  @Test
  fun testOnInputDeviceAdded_withJoystick_createsStateAndUpdatesCursor() {
    service.callOnServiceConnected()
    assertThat(service.getJoystickDeviceIdsToState()).isEmpty()

    val newJoystick = createMockJoystick(42)
    ShadowInputManagerCustom.addDevice(newJoystick)

    service.onInputDeviceAdded(42)

    assertThat(service.getJoystickDeviceIdsToState()).containsKey(42)
    val joystickState = service.getJoystickDeviceIdsToState()[42]!!
    assertThat(joystickState.deviceId).isEqualTo(42)
    assertThat(joystickState.displayInfo.displayId).isEqualTo(Display.DEFAULT_DISPLAY)
  }

  @Test
  fun testOnInputDeviceAdded_withNonJoystick_isIgnored() {
    service.callOnServiceConnected()

    val keyboard = createMockNonJoystick(99)
    ShadowInputManagerCustom.addDevice(keyboard)

    service.onInputDeviceAdded(99)

    assertThat(service.getJoystickDeviceIdsToState()).doesNotContainKey(99)
  }

  @Test
  fun testOnInputDeviceAdded_withDisabledOrInternalDevice_isIgnored() {
    service.callOnServiceConnected()

    val internalJoystick = createMockJoystick(101, isExternal = false)
    val disabledJoystick = createMockJoystick(102, isEnabled = false)
    ShadowInputManagerCustom.addDevice(internalJoystick)
    ShadowInputManagerCustom.addDevice(disabledJoystick)

    service.onInputDeviceAdded(101)
    service.onInputDeviceAdded(102)

    assertThat(service.getJoystickDeviceIdsToState()).doesNotContainKey(101)
    assertThat(service.getJoystickDeviceIdsToState()).doesNotContainKey(102)
  }

  @Test
  fun testOnInputDeviceRemoved_closesAndRemovesJoystickState() {
    val joystick = createMockJoystick(5)
    ShadowInputManagerCustom.addDevice(joystick)
    service.callOnServiceConnected()

    assertThat(service.getJoystickDeviceIdsToState()).containsKey(5)

    ShadowInputManagerCustom.removeDevice(5)
    service.onInputDeviceRemoved(5)

    assertThat(service.getJoystickDeviceIdsToState()).doesNotContainKey(5)
  }

  @Test
  fun testOnInputDeviceChanged_isIgnoredGracefully() {
    val joystick = createMockJoystick(7)
    ShadowInputManagerCustom.addDevice(joystick)
    service.callOnServiceConnected()

    // Calling onInputDeviceChanged should not throw or alter state
    service.onInputDeviceChanged(7)
    assertThat(service.getJoystickDeviceIdsToState()).containsKey(7)
  }

  @Test
  fun testOnMotionEvent_withKnownJoystick_updatesCursorPosition() {
    val joystick = createMockJoystick(1)
    ShadowInputManagerCustom.addDevice(joystick)
    service.callOnServiceConnected()

    val motionEvent = createMockMotionEvent(deviceId = 1, axisZ = 0.5f, axisRz = 0.5f)
    service.onMotionEvent(motionEvent)

    // State update was processed (no exception, state remains valid)
    assertThat(service.getJoystickDeviceIdsToState()).containsKey(1)
  }

  @Test
  fun testOnMotionEvent_withUnknownDevice_isIgnored() {
    service.callOnServiceConnected()

    val motionEvent = createMockMotionEvent(deviceId = 999, axisZ = 0.5f)
    service.onMotionEvent(motionEvent)
    // No error, no state created
    assertThat(service.getJoystickDeviceIdsToState()).isEmpty()
  }

  @Test
  fun testOnKeyEvent_withKnownJoystick_consumesEventWhenEnabled() {
    val joystick = createMockJoystick(1)
    ShadowInputManagerCustom.addDevice(joystick)
    service.callOnServiceConnected()

    val keyEvent = createMockKeyEvent(deviceId = 1, action = KeyEvent.ACTION_DOWN, keyCode = KeyEvent.KEYCODE_BUTTON_A)
    val consumed = service.callOnKeyEvent(keyEvent)

    assertThat(consumed).isTrue()
  }

  @Test
  fun testOnKeyEvent_withUnknownDeviceOrNull_returnsFalse() {
    service.callOnServiceConnected()

    val nullEventResult = service.callOnKeyEvent(null)
    assertThat(nullEventResult).isFalse()

    val unknownDeviceEvent = createMockKeyEvent(deviceId = 888, action = KeyEvent.ACTION_DOWN, keyCode = KeyEvent.KEYCODE_BUTTON_A)
    val unknownResult = service.callOnKeyEvent(unknownDeviceEvent)
    assertThat(unknownResult).isFalse()
  }

  @Test
  fun testOnKeyEvent_toggleChord_togglesEnabledAndHidesOverlay() {
    val joystick = createMockJoystick(1)
    ShadowInputManagerCustom.addDevice(joystick)
    service.callOnServiceConnected()

    val cursorDisplayState = service.getDisplayIdToCursorDisplayState()[Display.DEFAULT_DISPLAY]!!
    assertThat(cursorDisplayState.isCursorShown()).isTrue()

    // Send toggle chord (L1 + R1 + X)
    service.callOnKeyEvent(createMockKeyEvent(deviceId = 1, action = KeyEvent.ACTION_DOWN, keyCode = KeyEvent.KEYCODE_BUTTON_L1))
    service.callOnKeyEvent(createMockKeyEvent(deviceId = 1, action = KeyEvent.ACTION_DOWN, keyCode = KeyEvent.KEYCODE_BUTTON_R1))
    val toggleResult = service.callOnKeyEvent(createMockKeyEvent(deviceId = 1, action = KeyEvent.ACTION_DOWN, keyCode = KeyEvent.KEYCODE_BUTTON_X))

    // Toggle event is consumed
    assertThat(toggleResult).isTrue()

    val joystickState = service.getJoystickDeviceIdsToState()[1]!!
    assertThat(joystickState.isEnabled).isFalse()
    assertThat(service.getIsEnabled()).isFalse()
    assertThat(cursorDisplayState.isCursorShown()).isFalse()
  }

  // =========================================================================
  // 3. Multi-Display Handling Tests
  // =========================================================================

  @Test
  fun testMeasureDisplays_discoversDisplaysWithWindows() {
    val secondaryDisplayId = ShadowDisplayManager.addDisplay("w1920dp-h1080dp")

    val window0 = createMockWindow()
    val window1 = createMockWindow()
    val sparseArray = SparseArray<List<AccessibilityWindowInfo>>()
    sparseArray.put(Display.DEFAULT_DISPLAY, listOf(window0))
    sparseArray.put(secondaryDisplayId, listOf(window1))
    shadowAccessibilityService.customWindowsOnAllDisplays = sparseArray

    service.callOnServiceConnected()

    assertThat(service.getDisplayInfos()).containsKey(Display.DEFAULT_DISPLAY)
    assertThat(service.getDisplayInfos()).containsKey(secondaryDisplayId)
    assertThat(service.getDisplayIdToCursorDisplayState()).containsKey(Display.DEFAULT_DISPLAY)
    assertThat(service.getDisplayIdToCursorDisplayState()).containsKey(secondaryDisplayId)
  }

  @Test
  fun testMeasureDisplays_ignoresDisplaysNotInDisplayManager() {
    val invalidDisplayId = 999

    val window0 = createMockWindow()
    val windowInvalid = createMockWindow()
    val sparseArray = SparseArray<List<AccessibilityWindowInfo>>()
    sparseArray.put(Display.DEFAULT_DISPLAY, listOf(window0))
    sparseArray.put(invalidDisplayId, listOf(windowInvalid))
    shadowAccessibilityService.customWindowsOnAllDisplays = sparseArray

    service.callOnServiceConnected()

    assertThat(service.getDisplayInfos()).containsKey(Display.DEFAULT_DISPLAY)
    assertThat(service.getDisplayInfos()).doesNotContainKey(invalidDisplayId)
    assertThat(service.getDisplayIdToCursorDisplayState()).doesNotContainKey(invalidDisplayId)
  }

  @Test
  fun testOnDisplayAdded_remeasuresAndAddsOverlay() {
    service.callOnServiceConnected()

    val newDisplayId = ShadowDisplayManager.addDisplay("w1920dp-h1080dp")

    val window0 = createMockWindow()
    val window1 = createMockWindow()
    val sparseArray = SparseArray<List<AccessibilityWindowInfo>>()
    sparseArray.put(Display.DEFAULT_DISPLAY, listOf(window0))
    sparseArray.put(newDisplayId, listOf(window1))
    shadowAccessibilityService.customWindowsOnAllDisplays = sparseArray

    service.onDisplayAdded(newDisplayId)

    assertThat(service.getDisplayInfos()).containsKey(newDisplayId)
    assertThat(service.getDisplayIdToCursorDisplayState()).containsKey(newDisplayId)
  }

  @Test
  fun testOnDisplayRemoved_closesDisplayOverlay_andReassignsJoysticksToDefaultDisplay() {
    val secondaryDisplayId = ShadowDisplayManager.addDisplay("w1920dp-h1080dp")

    val window0 = createMockWindow()
    val window1 = createMockWindow()
    val sparseArray = SparseArray<List<AccessibilityWindowInfo>>()
    sparseArray.put(Display.DEFAULT_DISPLAY, listOf(window0))
    sparseArray.put(secondaryDisplayId, listOf(window1))
    shadowAccessibilityService.customWindowsOnAllDisplays = sparseArray

    val joystick = createMockJoystick(1)
    ShadowInputManagerCustom.addDevice(joystick)

    service.callOnServiceConnected()

    // Assign joystick to secondary display
    val joystickState = service.getJoystickDeviceIdsToState()[1]!!
    joystickState.updateDisplayInfo(service.getDisplayInfos()[secondaryDisplayId]!!)
    assertThat(joystickState.displayInfo.displayId).isEqualTo(secondaryDisplayId)

    // Remove secondary display
    ShadowDisplayManager.removeDisplay(secondaryDisplayId)
    val updatedSparseArray = SparseArray<List<AccessibilityWindowInfo>>()
    updatedSparseArray.put(Display.DEFAULT_DISPLAY, listOf(window0))
    shadowAccessibilityService.customWindowsOnAllDisplays = updatedSparseArray

    service.onDisplayRemoved(secondaryDisplayId)

    assertThat(service.getDisplayInfos()).doesNotContainKey(secondaryDisplayId)
    assertThat(service.getDisplayIdToCursorDisplayState()).doesNotContainKey(secondaryDisplayId)
    // Joystick should fall back to DEFAULT_DISPLAY
    assertThat(joystickState.displayInfo.displayId).isEqualTo(Display.DEFAULT_DISPLAY)
  }

  @Test
  fun testOnDisplayChanged_and_onConfigurationChanged_updatesDisplayMetrics() {
    val joystick = createMockJoystick(1)
    ShadowInputManagerCustom.addDevice(joystick)
    service.callOnServiceConnected()

    val joystickState = service.getJoystickDeviceIdsToState()[1]!!
    assertThat(joystickState.displayInfo.displayId).isEqualTo(Display.DEFAULT_DISPLAY)

    service.onDisplayChanged(Display.DEFAULT_DISPLAY)
    assertThat(service.getDisplayInfos()).containsKey(Display.DEFAULT_DISPLAY)

    service.onConfigurationChanged(Configuration())
    assertThat(service.getDisplayInfos()).containsKey(Display.DEFAULT_DISPLAY)
  }

  @Test
  fun testCycleDisplay_cyclesThroughDisplays_andSelectsRootWindow() {
    val secondaryDisplayId = ShadowDisplayManager.addDisplay("w1920dp-h1080dp")

    val rootNode0 = mock<AccessibilityNodeInfo>()
    val rootNode1 = mock<AccessibilityNodeInfo>()
    whenever(rootNode1.performAction(anyInt())).thenReturn(true)
    whenever(rootNode0.performAction(anyInt())).thenReturn(true)

    val window0 = createMockWindow(rootNode0)
    val window1 = createMockWindow(rootNode1)

    val sparseArray = SparseArray<List<AccessibilityWindowInfo>>()
    sparseArray.put(Display.DEFAULT_DISPLAY, listOf(window0))
    sparseArray.put(secondaryDisplayId, listOf(window1))
    shadowAccessibilityService.customWindowsOnAllDisplays = sparseArray

    val joystick = createMockJoystick(1)
    ShadowInputManagerCustom.addDevice(joystick)

    service.callOnServiceConnected()

    val joystickState = service.getJoystickDeviceIdsToState()[1]!!
    assertThat(joystickState.displayInfo.displayId).isEqualTo(Display.DEFAULT_DISPLAY)

    // Call onAction with CYCLE_DISPLAY_FORWARD via reflection helper
    service.callOnAction(joystickState, JoystickAction.CYCLE_DISPLAY_FORWARD)

    // Joystick is now on secondary display
    assertThat(joystickState.displayInfo.displayId).isEqualTo(secondaryDisplayId)
    // Root window on secondary display received focus/select action
    verify(rootNode1).performAction(AccessibilityNodeInfo.ACTION_FOCUS)

    // Cycle backward -> back to DEFAULT_DISPLAY
    service.callOnAction(joystickState, JoystickAction.CYCLE_DISPLAY_BACKWARD)
    assertThat(joystickState.displayInfo.displayId).isEqualTo(Display.DEFAULT_DISPLAY)
    verify(rootNode0).performAction(AccessibilityNodeInfo.ACTION_FOCUS)
  }

  // =========================================================================
  // 4. Overlay Creation & Lifecycle Tests
  // =========================================================================

  @Test
  fun testCursorOverlay_autoHidesAfterInactivityTimeout() {
    val joystick = createMockJoystick(1)
    ShadowInputManagerCustom.addDevice(joystick)
    service.callOnServiceConnected()

    val cursorState = service.getDisplayIdToCursorDisplayState()[Display.DEFAULT_DISPLAY]!!
    assertThat(cursorState.isCursorShown()).isTrue()

    // Advance looper past inactivity timeout (1500ms)
    ShadowLooper.idleMainLooper(1500, TimeUnit.MILLISECONDS)

    assertThat(cursorState.isCursorShown()).isFalse()
  }

  @Test
  fun testCursorOverlay_movingCursor_reshowsCursorAndRestartsHider() {
    val joystick = createMockJoystick(1)
    ShadowInputManagerCustom.addDevice(joystick)
    service.callOnServiceConnected()

    val cursorState = service.getDisplayIdToCursorDisplayState()[Display.DEFAULT_DISPLAY]!!
    ShadowLooper.idleMainLooper(1500, TimeUnit.MILLISECONDS)
    assertThat(cursorState.isCursorShown()).isFalse()

    // Move cursor / trigger updateCursorPosition
    val joystickState = service.getJoystickDeviceIdsToState()[1]!!
    service.callUpdateCursorPosition(joystickState)

    assertThat(cursorState.isCursorShown()).isTrue()
  }

  @Test
  fun testCursorOverlay_primaryButtonPress_cancelsHider_andUpdatesVisualState() {
    val joystick = createMockJoystick(1)
    ShadowInputManagerCustom.addDevice(joystick)
    service.callOnServiceConnected()

    val cursorState = service.getDisplayIdToCursorDisplayState()[Display.DEFAULT_DISPLAY]!!

    // Press primary button via Button A
    service.callOnKeyEvent(
      createMockKeyEvent(
        deviceId = 1,
        action = KeyEvent.ACTION_DOWN,
        keyCode = KeyEvent.KEYCODE_BUTTON_A
      )
    )

    // Visual state should be pressed tap
    assertThat(cursorState.getCursorCurrentState().toString()).isEqualTo("STATE_PRESSED_TAP")

    // Advance past inactivity timeout - cursor should NOT hide because button is held
    ShadowLooper.idleMainLooper(1500, TimeUnit.MILLISECONDS)
    assertThat(cursorState.isCursorShown()).isTrue()

    // Release primary button via Button A release
    service.callOnKeyEvent(
      createMockKeyEvent(
        deviceId = 1,
        action = KeyEvent.ACTION_UP,
        keyCode = KeyEvent.KEYCODE_BUTTON_A
      )
    )

    assertThat(cursorState.getCursorCurrentState().toString()).isEqualTo("STATE_RELEASED")

    // Now advance past inactivity timeout - cursor should hide
    ShadowLooper.idleMainLooper(1500, TimeUnit.MILLISECONDS)
    assertThat(cursorState.isCursorShown()).isFalse()
  }

  @Test
  fun testDispatchFling_createsAndCleansUpSwipeVisualizationOverlay() {
    val joystick = createMockJoystick(1)
    ShadowInputManagerCustom.addDevice(joystick)
    service.callOnServiceConnected()

    val joystickState = service.getJoystickDeviceIdsToState()[1]!!

    // Trigger SWIPE_UP action
    service.callOnAction(joystickState, JoystickAction.SWIPE_UP)

    // Gesture was dispatched
    assertThat(shadowAccessibilityService.customDispatchedGestures).isNotEmpty()

    // SwipeVisualization overlay was attached
    assertThat(service.getCloseableOverlays()).isNotEmpty()

    // Advance looper by 500ms (visualization display duration)
    ShadowLooper.idleMainLooper(500, TimeUnit.MILLISECONDS)

    // Overlay is cleaned up and removed
    assertThat(service.getCloseableOverlays()).isEmpty()
  }

  @Test
  fun testGlobalActions_delegatesToPerformGlobalAction() {
    val joystick = createMockJoystick(1)
    ShadowInputManagerCustom.addDevice(joystick)
    service.callOnServiceConnected()

    val joystickState = service.getJoystickDeviceIdsToState()[1]!!

    service.callOnAction(joystickState, JoystickAction.BACK)
    assertThat(shadowAccessibilityService.globalActionsPerformed)
      .contains(AccessibilityService.GLOBAL_ACTION_BACK)

    service.callOnAction(joystickState, JoystickAction.HOME)
    assertThat(shadowAccessibilityService.globalActionsPerformed)
      .contains(AccessibilityService.GLOBAL_ACTION_HOME)

    service.callOnAction(joystickState, JoystickAction.RECENTS)
    assertThat(shadowAccessibilityService.globalActionsPerformed)
      .contains(AccessibilityService.GLOBAL_ACTION_RECENTS)
  }

  @Test
  fun testOnAccessibilityEvent_doesNotThrow() {
    service.callOnServiceConnected()
    val event = AccessibilityEvent()
    service.onAccessibilityEvent(event)
  }

  @Test
  fun testOnInterrupt_doesNotThrow() {
    service.callOnServiceConnected()
    service.onInterrupt()
  }

  // =========================================================================
  // Test Helpers & Reflection Utilities
  // =========================================================================

  private fun createMockJoystick(
    deviceId: Int,
    isExternal: Boolean = true,
    isEnabled: Boolean = true,
    supportsJoystickSource: Boolean = true,
  ): InputDevice {
    val device = mock<InputDevice>()
    whenever(device.id).thenReturn(deviceId)
    whenever(device.isExternal).thenReturn(isExternal)
    whenever(device.isEnabled).thenReturn(isEnabled)
    whenever(device.supportsSource(InputDevice.SOURCE_JOYSTICK)).thenReturn(supportsJoystickSource)

    val range = createMotionRange(axis = MotionEvent.AXIS_Z)
    whenever(device.getMotionRange(anyInt())).thenReturn(range)

    return device
  }

  private fun createMotionRange(
    axis: Int = MotionEvent.AXIS_Z,
    source: Int = InputDevice.SOURCE_JOYSTICK,
    min: Float = -1f,
    max: Float = 1f,
    flat: Float = 0.1f,
    fuzz: Float = 0.05f,
    resolution: Float = 0f
  ): InputDevice.MotionRange {
    val constructor = InputDevice.MotionRange::class.java.declaredConstructors.first()
    constructor.isAccessible = true
    return constructor.newInstance(axis, source, min, max, flat, fuzz, resolution) as InputDevice.MotionRange
  }

  private fun createMockNonJoystick(
    deviceId: Int,
    isExternal: Boolean = true,
    isEnabled: Boolean = true,
  ): InputDevice {
    val device = mock<InputDevice>()
    whenever(device.id).thenReturn(deviceId)
    whenever(device.isExternal).thenReturn(isExternal)
    whenever(device.isEnabled).thenReturn(isEnabled)
    whenever(device.supportsSource(InputDevice.SOURCE_JOYSTICK)).thenReturn(false)
    return device
  }

  private fun createMockMotionEvent(
    deviceId: Int = 1,
    axisZ: Float = 0f,
    axisRz: Float = 0f,
    axisLTrigger: Float = 0f,
    axisRTrigger: Float = 0f,
    axisHatX: Float = 0f,
    axisHatY: Float = 0f,
  ): MotionEvent {
    val event = mock<MotionEvent>()
    whenever(event.deviceId).thenReturn(deviceId)
    whenever(event.getAxisValue(MotionEvent.AXIS_Z)).thenReturn(axisZ)
    whenever(event.getAxisValue(MotionEvent.AXIS_RZ)).thenReturn(axisRz)
    whenever(event.getAxisValue(MotionEvent.AXIS_LTRIGGER)).thenReturn(axisLTrigger)
    whenever(event.getAxisValue(MotionEvent.AXIS_RTRIGGER)).thenReturn(axisRTrigger)
    whenever(event.getAxisValue(MotionEvent.AXIS_HAT_X)).thenReturn(axisHatX)
    whenever(event.getAxisValue(MotionEvent.AXIS_HAT_Y)).thenReturn(axisHatY)
    return event
  }

  private fun createMockKeyEvent(
    deviceId: Int = 1,
    action: Int = KeyEvent.ACTION_DOWN,
    keyCode: Int = KeyEvent.KEYCODE_BUTTON_A,
  ): KeyEvent {
    val event = mock<KeyEvent>()
    whenever(event.deviceId).thenReturn(deviceId)
    whenever(event.action).thenReturn(action)
    whenever(event.keyCode).thenReturn(keyCode)
    return event
  }

  private fun createMockWindow(
    rootNode: AccessibilityNodeInfo? = null,
  ): AccessibilityWindowInfo {
    val window = mock<AccessibilityWindowInfo>()
    whenever(window.root).thenReturn(rootNode)
    return window
  }

  private fun MouseAccessibilityService.callOnServiceConnected() {
    val method = MouseAccessibilityService::class.java.getDeclaredMethod("onServiceConnected")
    method.isAccessible = true
    method.invoke(this)
  }

  private fun MouseAccessibilityService.callOnKeyEvent(event: KeyEvent?): Boolean {
    val method =
      MouseAccessibilityService::class.java.getDeclaredMethod("onKeyEvent", KeyEvent::class.java)
    method.isAccessible = true
    return method.invoke(this, event) as Boolean
  }

  private fun MouseAccessibilityService.callOnAction(
    state: JoystickCursorState,
    action: JoystickAction
  ) {
    val method =
      MouseAccessibilityService::class.java.getDeclaredMethod(
        "onAction",
        JoystickCursorState::class.java,
        JoystickAction::class.java
      )
    method.isAccessible = true
    method.invoke(this, state, action)
  }

  private fun MouseAccessibilityService.callUpdateCursorPosition(state: JoystickCursorState) {
    val method =
      MouseAccessibilityService::class.java.getDeclaredMethod(
        "updateCursorPosition",
        JoystickCursorState::class.java
      )
    method.isAccessible = true
    method.invoke(this, state)
  }

  private fun MouseAccessibilityService.getJoystickDeviceIdsToState(): Map<Int, JoystickCursorState> {
    val field = MouseAccessibilityService::class.java.getDeclaredField("joystickDeviceIdsToState")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    return field.get(this) as Map<Int, JoystickCursorState>
  }

  private fun MouseAccessibilityService.getDisplayIdToCursorDisplayState(): Map<Int, Any> {
    val field =
      MouseAccessibilityService::class.java.getDeclaredField("displayIdToCursorDisplayState")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    return field.get(this) as Map<Int, Any>
  }

  private fun MouseAccessibilityService.getDisplayInfos(): Map<Int, DisplayInfo> {
    val field = MouseAccessibilityService::class.java.getDeclaredField("displayInfos")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    return field.get(this) as Map<Int, DisplayInfo>
  }

  private fun MouseAccessibilityService.getCloseableOverlays(): Set<Closeable> {
    val field = MouseAccessibilityService::class.java.getDeclaredField("closeableOverlays")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    return field.get(this) as Set<Closeable>
  }

  private fun MouseAccessibilityService.getIsEnabled(): Boolean {
    val field = MouseAccessibilityService::class.java.getDeclaredField("isEnabled")
    field.isAccessible = true
    return field.getBoolean(this)
  }

  private fun Any.isCursorShown(): Boolean {
    val field = this.javaClass.getDeclaredField("isShown")
    field.isAccessible = true
    return field.getBoolean(this)
  }

  private fun Any.getCursorCurrentState(): Any {
    val field = this.javaClass.getDeclaredField("currentState")
    field.isAccessible = true
    return field.get(this)!!
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
