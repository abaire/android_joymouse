package work.bearbrains.joymouse

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.InputDevice.SOURCE_JOYSTICK
import android.view.InputDevice.SOURCE_UNKNOWN
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.MotionEvent.PointerCoords
import android.view.accessibility.AccessibilityEvent

/** Handles conversion of joystick input events to motion eventsevents. */
class MouseAccessibilityService : AccessibilityService(), InputManager.InputDeviceListener {

  private var joystickDeviceIds = mutableSetOf<Int>()
  private val handler = Handler(Looper.getMainLooper())

  private val pointerCoords = PointerCoords()

  override fun onServiceConnected() {
    val inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
    inputManager.registerInputDeviceListener(this, null)

    val info = serviceInfo
    info.motionEventSources =  SOURCE_JOYSTICK
    serviceInfo = info

    for (deviceId in inputManager.inputDeviceIds) {
      inputManager.getInputDevice(deviceId)?.let { device ->
        if (!device.isExternal || !device.isEnabled || !device.supportsSource(SOURCE_JOYSTICK)) {
          return@let
        }
        joystickDeviceIds.add(deviceId)

//        device.motionRanges
        //AXIS_HAT_Y: source=0x1000010 min=-1.0 max=1.0 flat=0.0 fuzz=0.0 resolution=0.0
        //AXIS_LTRIGGER: source=0x1000010 min=0.0 max=1.0 flat=0.06158358 fuzz=0.0029325513 resolution=0.0
        //AXIS_BRAKE: source=0x1000010 min=0.0 max=1.0 flat=0.06158358 fuzz=0.0029325513 resolution=0.0
        //AXIS_RTRIGGER: source=0x1000010 min=0.0 max=1.0 flat=0.06158358 fuzz=0.0029325513 resolution=0.0
        //AXIS_GAS: source=0x1000010 min=0.0 max=1.0 flat=0.06158358 fuzz=0.0029325513 resolution=0.0
        //AXIS_Z: source=0x1000010 min=-1.0 max=1.0 flat=0.12497139 fuzz=0.007782101 resolution=0.0
        //AXIS_Y: source=0x1000010 min=-1.0 max=1.0 flat=0.12497139 fuzz=0.007782101 resolution=0.0
        //AXIS_HAT_X: source=0x1000010 min=-1.0 max=1.0 flat=0.0 fuzz=0.0 resolution=0.0
        //AXIS_RZ: source=0x1000010 min=-1.0 max=1.0 flat=0.12497139 fuzz=0.007782101 resolution=0.0
        //AXIS_X: source=0x1000010 min=-1.0 max=1.0 flat=0.12497139 fuzz=0.007782101 resolution=0.0
//        println("!!! DeviceID: ${deviceId} => ${device}")
      }
    }
  }

  override fun onUnbind(intent: Intent?): Boolean {
    val inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
    inputManager.unregisterInputDeviceListener(this)
    return super.onUnbind(intent)
  }

  override fun onKeyEvent(event: KeyEvent?): Boolean {
    if (event == null || !joystickDeviceIds.contains(event.deviceId)) {
      return super.onKeyEvent(event)
    }

    // TODO: Look for the enable/disable chord.
    println("!!! onKeyEvent $event")
    return super.onKeyEvent(event)
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
  }

  override fun onMotionEvent(event: MotionEvent) {
    if (!joystickDeviceIds.contains(event.deviceId)) {
      return
    }

    println("!!! onMotionEvent $event\n\tPointerCount: ${event.pointerCount}")
    event.getPointerCoords(0, pointerCoords)
    event.device.motionRanges.forEach { range ->
      println("!!! Motion range ${range.axis} ${MotionEvent.axisToString(range.axis)} ${range.source} ${range.min} - ${range.max} = ${range.range} Flat: ${range.flat} Resolution: ${range.resolution} Fuzz: ${range.fuzz}")
    }
// Motion range 16 AXIS_HAT_Y 16777232 -1.0 - 1.0 = 2.0 Flat: 0.0 Resolution: 0.0 Fuzz: 0.0
// Motion range 17 AXIS_LTRIGGER 16777232 0.0 - 1.0 = 1.0 Flat: 0.06158358 Resolution: 0.0 Fuzz: 0.0029325513
// Motion range 23 AXIS_BRAKE 16777232 0.0 - 1.0 = 1.0 Flat: 0.06158358 Resolution: 0.0 Fuzz: 0.0029325513
// Motion range 18 AXIS_RTRIGGER 16777232 0.0 - 1.0 = 1.0 Flat: 0.06158358 Resolution: 0.0 Fuzz: 0.0029325513
// Motion range 22 AXIS_GAS 16777232 0.0 - 1.0 = 1.0 Flat: 0.06158358 Resolution: 0.0 Fuzz: 0.0029325513
// Motion range 11 AXIS_Z 16777232 -1.0 - 1.0 = 2.0 Flat: 0.12497139 Resolution: 0.0 Fuzz: 0.007782101
// Motion range 1 AXIS_Y 16777232 -1.0 - 1.0 = 2.0 Flat: 0.12497139 Resolution: 0.0 Fuzz: 0.007782101
// Motion range 15 AXIS_HAT_X 16777232 -1.0 - 1.0 = 2.0 Flat: 0.0 Resolution: 0.0 Fuzz: 0.0
// Motion range 14 AXIS_RZ 16777232 -1.0 - 1.0 = 2.0 Flat: 0.12497139 Resolution: 0.0 Fuzz: 0.007782101
// Motion range 0 AXIS_X 16777232 -1.0 - 1.0 = 2.0 Flat: 0.12497139 Resolution: 0.0 Fuzz: 0.007782101



    // The pointer for an Xbox controller is only the left stick (AXIS_X, AXIS_Y), but the right
    // stick (AXIS_Z, AXIS_RZ) can be queried manually via getAxisValue
    println("!!! Pointer coords:\n\t${pointerCoords.x}, ${pointerCoords.y}\n\tSize: ${pointerCoords.size}\n\torientation: ${pointerCoords.orientation}\n\tpressure: ${pointerCoords.pressure}\n\tTool: ${pointerCoords.toolMajor} . ${pointerCoords.toolMinor}\n\tTouch ${pointerCoords.touchMajor} . ${pointerCoords.touchMinor}")
    event.device.motionRanges.forEach { range ->
      println("\tCurrent axis value? ${MotionEvent.axisToString(range.axis)} = ${event.getAxisValue(range.axis)}")

      // Z is right stick horizontal
      // RZ is right stick vertical
    }


//    getHistoricalPointerCoords(int pointerIndex, int pos, MotionEvent.PointerCoords outPointerCoords)
//    2024-08-17 17:59:24.342 15511-15511 System.out              work.bearbrains.joymouse             I  !!! onMotionEvent MotionEvent { action=ACTION_MOVE, actionButton=0, id[0]=0, x[0]=0.010971189, y[0]=-0.04203862, toolType[0]=TOOL_TYPE_UNKNOWN, buttonState=0, classification=NONE, metaState=0, flags=0x0, edgeFlags=0x0, pointerCount=1, historySize=0, eventTime=5590955, downTime=0, deviceId=6, source=0x1000010, displayId=-1, eventId=29118201 }

    // Right stick
//    2024-08-17 17:59:24.372 15511-15511 System.out              work.bearbrains.joymouse             I  !!! onMotionEvent MotionEvent { action=ACTION_MOVE, actionButton=0, id[0]=0, x[0]=0.010971189, y[0]=-0.04203862, toolType[0]=TOOL_TYPE_UNKNOWN, buttonState=0, classification=NONE, metaState=0, flags=0x0, edgeFlags=0x0, pointerCount=1, historySize=0, eventTime=5590984, downTime=0, deviceId=6, source=0x1000010, displayId=-1, eventId=637324597 }


    // Left stick
    // 2024-08-17 21:32:38.141 22961-22961 System.out              work.bearbrains.joymouse             I  !!! onMotionEvent MotionEvent { action=ACTION_MOVE, actionButton=0, id[0]=0, x[0]=1.0, y[0]=-0.060959816, toolType[0]=TOOL_TYPE_UNKNOWN, buttonState=0, classification=NONE, metaState=0, flags=0x0, edgeFlags=0x0, pointerCount=1, historySize=0, eventTime=8122528, downTime=0, deviceId=7, source=0x1000010, displayId=-1, eventId=1044213735 }



    injectMouseMove(150f, 0.0f)

    super.onMotionEvent(event)
  }

  private fun injectMouseMove(relativeX: Float, relativeY: Float) {
    println("!!! injectMouseMove: $relativeX $relativeY")
    val path = Path()
    val startX = 700f
    val startY = 600f
    path.moveTo(startX, startY)
    path.lineTo(startX + relativeX, startY + relativeY)

    val stroke = GestureDescription.StrokeDescription(path, 0, 50) // 50ms duration

    val gestureBuilder = GestureDescription.Builder()
    gestureBuilder.addStroke(stroke)

    println("Dispatch gesture $gestureBuilder")
    val wasDispatched = dispatchGesture(
        gestureBuilder.build(),
        object : GestureResultCallback() {
          override fun onCompleted(gestureDescription: GestureDescription) {
            println("!!! Gesture completed: ${gestureDescription}")
            // Mouse move completed successfully
          }

          override fun onCancelled(gestureDescription: GestureDescription) {
            // Mouse move was cancelled
            println("!!! Gesture cancelled!! ${gestureDescription}")
          }
        },
        handler)

    if (!wasDispatched) {
      println("!!! Error: dispatchGesture failed!")
    }


    // private void tap(PointF point) {
    //          StrokeDescription tap =  new StrokeDescription(path(point), 0,
    //          ViewConfiguration.getTapTimeout());
    //          GestureDescription.Builder builder = new GestureDescription.Builder();
    //          builder.addStroke(tap);
    //          dispatchGesture(builder.build(), null, null);
    //      }
  }

  override fun onInterrupt() {}

  override fun onInputDeviceAdded(deviceId: Int) {
    val inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
    val device = inputManager.getInputDevice(deviceId)
    println("!!! onInputDeviceAdded ${deviceId} ${device}")

//    // Check if the device is an Xbox controller
//    if (isXboxController(device)) {
//      xboxController = device
//      xboxController?.registerOnGenericMotionListener(this, null)
//    }
  }

  override fun onInputDeviceRemoved(deviceId: Int) {
    println("!!! onInputDeviceRemoved ${deviceId}")
  }

  override fun onInputDeviceChanged(deviceId: Int) {
    println("!!! onInputDeviceChanged ${deviceId}")
  }

//  override fun onGenericMotion(event: MotionEvent): Boolean {
//    if (event.source == SOURCE_JOYSTICK && event.device == xboxController) {
//      // Process joystick events here
//      val axisX = event.getAxisValue(MotionEvent.AXIS_X)
//      val axisY = event.getAxisValue(MotionEvent.AXIS_Y)
//      // ... handle other axes and buttons
//    }
//    return true // Indicate that the event was handled
//  }

}
