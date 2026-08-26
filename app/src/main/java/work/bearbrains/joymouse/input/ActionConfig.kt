package work.bearbrains.joymouse.input

import android.graphics.Color
import android.view.KeyEvent
import android.view.MotionEvent
import kotlin.math.roundToInt
import org.json.JSONArray
import org.json.JSONObject

/**
 * Modifiers that can be combined with buttons to trigger actions.
 */
enum class ShiftModifier {
  NONE,
  SHIFT,
  ALT,
  ALTSHIFT;

  fun getDisplayName(): String {
    return when (this) {
      NONE -> "None"
      SHIFT -> "Shift"
      ALT -> "Alt"
      ALTSHIFT -> "Shift + Alt"
    }
  }
}

/** Configurable thumbstick used for controlling the mouse cursor. */
enum class MouseStick(val xAxis: Int, val yAxis: Int) {
  RIGHT_STICK(MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ),
  LEFT_STICK(MotionEvent.AXIS_X, MotionEvent.AXIS_Y);

  fun getDisplayName(): String =
    when (this) {
      RIGHT_STICK -> "Right thumbstick"
      LEFT_STICK -> "Left thumbstick"
    }

  fun getFriendlyName(): String =
    when (this) {
      RIGHT_STICK -> "right thumbstick"
      LEFT_STICK -> "left thumbstick"
    }
}

/** Palette options for cursor display states. */
enum class CursorPalette {
  DEFAULT,
  COLORBLIND_FRIENDLY,
  CUSTOM;

  fun getDisplayName(): String =
    when (this) {
      DEFAULT -> "Default"
      COLORBLIND_FRIENDLY -> "Colorblind friendly"
      CUSTOM -> "Custom"
    }
}

/** Specific colors associated with each cursor display state. */
data class CursorColors(
  val released: Int = COLOR_WHITE,
  val tap: Int = COLOR_TAP,
  val longTouch: Int = COLOR_LONG_TOUCH,
  val drag: Int = COLOR_DRAG,
  val fling: Int = COLOR_FLING,
) {
  fun toJson(): JSONObject {
    val json = JSONObject()
    json.put("released", released)
    json.put("tap", tap)
    json.put("longTouch", longTouch)
    json.put("drag", drag)
    json.put("fling", fling)
    return json
  }

  companion object {
    const val COLOR_WHITE = 0xFFFFFFFF.toInt()
    const val COLOR_TAP = 0xFFC8E1FF.toInt()
    const val COLOR_LONG_TOUCH = 0xFF4CD964.toInt()
    const val COLOR_DRAG = 0xFF2196F3.toInt()
    const val COLOR_FLING = 0xFFFF4081.toInt()

    const val COLOR_CB_YELLOW = 0xFFF0E442.toInt()
    const val COLOR_CB_GREEN = 0xFF009E73.toInt()
    const val COLOR_CB_BLUE = 0xFF0072B2.toInt()
    const val COLOR_CB_VERMILLION = 0xFFD55E00.toInt()

    val DEFAULT =
      CursorColors(
        released = COLOR_WHITE,
        tap = COLOR_TAP,
        longTouch = COLOR_LONG_TOUCH,
        drag = COLOR_DRAG,
        fling = COLOR_FLING,
      )

    val COLORBLIND_FRIENDLY =
      CursorColors(
        released = COLOR_WHITE,
        tap = COLOR_CB_YELLOW,
        longTouch = COLOR_CB_GREEN,
        drag = COLOR_CB_BLUE,
        fling = COLOR_CB_VERMILLION,
      )

    fun fromJson(json: JSONObject): CursorColors {
      return CursorColors(
        released = json.optInt("released", COLOR_WHITE),
        tap = json.optInt("tap", COLOR_TAP),
        longTouch = json.optInt("longTouch", COLOR_LONG_TOUCH),
        drag = json.optInt("drag", COLOR_DRAG),
        fling = json.optInt("fling", COLOR_FLING),
      )
    }
  }
}

/** Configuration for mouse cursor appearance, color palette, and mode shape indicator. */
data class CursorConfig(
  val palette: CursorPalette = CursorPalette.DEFAULT,
  val customColors: CursorColors = CursorColors.DEFAULT,
  val changeShapeForMode: Boolean = false,
) {
  fun getColors(): CursorColors =
    when (palette) {
      CursorPalette.DEFAULT -> CursorColors.DEFAULT
      CursorPalette.COLORBLIND_FRIENDLY -> CursorColors.COLORBLIND_FRIENDLY
      CursorPalette.CUSTOM -> customColors
    }

  fun toJson(): JSONObject {
    val json = JSONObject()
    json.put("palette", palette.name)
    json.put("customColors", customColors.toJson())
    json.put("changeShapeForMode", changeShapeForMode)
    return json
  }

  companion object {
    val DEFAULT = CursorConfig()

    fun fromJson(json: JSONObject): CursorConfig {
      val palette =
        try {
          CursorPalette.valueOf(json.optString("palette", CursorPalette.DEFAULT.name))
        } catch (_: Exception) {
          CursorPalette.DEFAULT
        }
      val customColors =
        json.optJSONObject("customColors")?.let { CursorColors.fromJson(it) } ?: CursorColors.DEFAULT
      val changeShapeForMode = json.optBoolean("changeShapeForMode", false)
      return CursorConfig(
        palette = palette,
        customColors = customColors,
        changeShapeForMode = changeShapeForMode,
      )
    }
  }
}

/** Describes how a [JoystickAction] is bound to a physical button or button combination. */
data class ActionBinding(
  val modifier: ShiftModifier = ShiftModifier.NONE,
  val keyCodes: Set<Int> = emptySet(),
  val isChord: Boolean = false,
) {
  fun toJson(): JSONObject {
    val json = JSONObject()
    json.put("modifier", modifier.name)
    val keyCodesArray = JSONArray()
    keyCodes.forEach { keyCodesArray.put(it) }
    json.put("keyCodes", keyCodesArray)
    json.put("isChord", isChord)
    return json
  }

  companion object {
    fun fromJson(json: JSONObject): ActionBinding {
      val modifier =
        try {
          ShiftModifier.valueOf(json.getString("modifier"))
        } catch (_: Exception) {
          ShiftModifier.NONE
        }
      val keyCodesSet = mutableSetOf<Int>()
      val keyCodesArray = json.optJSONArray("keyCodes")
      if (keyCodesArray != null) {
        for (i in 0 until keyCodesArray.length()) {
          keyCodesSet.add(keyCodesArray.getInt(i))
        }
      }
      val isChord = json.optBoolean("isChord", false)
      return ActionBinding(modifier = modifier, keyCodes = keyCodesSet, isChord = isChord)
    }
  }
}

/** Configuration mapping special [JoystickAction]s. */
data class ActionConfig(
  val actionBindings: Map<JoystickAction, ActionBinding> = DEFAULT_ACTION_BINDINGS,
  val toggleChord: Set<Int> = DEFAULT_TOGGLE_CHORD,
  val shiftButton: Int = DEFAULT_SHIFT_BUTTON,
  val altButton: Int = DEFAULT_ALT_BUTTON,
  val mouseStick: MouseStick = DEFAULT_MOUSE_STICK,
  val invertX: Boolean = false,
  val invertY: Boolean = false,
  val cursorConfig: CursorConfig = CursorConfig.DEFAULT,
  val cursorSpeed: Float = DEFAULT_CURSOR_SPEED,
  val fastCursorSpeed: Float = DEFAULT_FAST_CURSOR_SPEED,
  val deadzone: Float = DEFAULT_DEADZONE,
) {

  init {
    if (shiftButton == altButton) {
      throw IllegalArgumentException("Shift and Alt cannot be mapped to the same button.")
    }
  }

  fun toJson(): String {
    val json = JSONObject()
    val bindingsJson = JSONObject()
    for ((action, binding) in actionBindings) {
      bindingsJson.put(action.name, binding.toJson())
    }
    json.put("actionBindings", bindingsJson)

    val chordArray = JSONArray()
    toggleChord.forEach { chordArray.put(it) }
    json.put("toggleChord", chordArray)

    json.put("shiftButton", shiftButton)
    json.put("altButton", altButton)
    json.put("mouseStick", mouseStick.name)
    json.put("invertX", invertX)
    json.put("invertY", invertY)
    json.put("cursorConfig", cursorConfig.toJson())
    json.put("cursorSpeed", cursorSpeed.toDouble())
    json.put("fastCursorSpeed", fastCursorSpeed.toDouble())
    json.put("deadzone", deadzone.toDouble())

    return json.toString()
  }

  companion object {
    const val DEFAULT_SHIFT_BUTTON = KeyEvent.KEYCODE_BUTTON_L2
    const val DEFAULT_ALT_BUTTON = KeyEvent.KEYCODE_BUTTON_R2
    val DEFAULT_MOUSE_STICK = MouseStick.RIGHT_STICK
    const val DEFAULT_CURSOR_SPEED = 1.0f
    const val DEFAULT_FAST_CURSOR_SPEED = 2.0f
    const val DEFAULT_DEADZONE = 0.10f

    /** Special actions that can be remapped to buttons. */
    val REMAPPABLE_ACTIONS: List<JoystickAction> =
      listOf(
        JoystickAction.BACK,
        JoystickAction.HOME,
        JoystickAction.RECENTS,
        JoystickAction.ACTIVATE,
        JoystickAction.FAST_CURSOR,
        JoystickAction.TOGGLE_GESTURE,
        JoystickAction.CYCLE_DISPLAY_BACKWARD,
        JoystickAction.CYCLE_DISPLAY_FORWARD,
        JoystickAction.SELECT_PRIMARY_DEVICE,
        JoystickAction.SWIPE_UP,
        JoystickAction.SWIPE_DOWN,
        JoystickAction.SWIPE_LEFT,
        JoystickAction.SWIPE_RIGHT,
      )

    val AVAILABLE_BUTTONS: List<Int> =
      listOf(
        KeyEvent.KEYCODE_BUTTON_A,
        KeyEvent.KEYCODE_BUTTON_B,
        KeyEvent.KEYCODE_BUTTON_X,
        KeyEvent.KEYCODE_BUTTON_Y,
        KeyEvent.KEYCODE_BUTTON_L1,
        KeyEvent.KEYCODE_BUTTON_R1,
        KeyEvent.KEYCODE_BUTTON_L2,
        KeyEvent.KEYCODE_BUTTON_R2,
        KeyEvent.KEYCODE_BUTTON_SELECT,
        KeyEvent.KEYCODE_BUTTON_START,
        KeyEvent.KEYCODE_BUTTON_THUMBL,
        KeyEvent.KEYCODE_BUTTON_THUMBR,
        KeyEvent.KEYCODE_BUTTON_MODE,
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT,
      )

    val DEFAULT_TOGGLE_CHORD: Set<Int> =
      setOf(
        KeyEvent.KEYCODE_BUTTON_L1,
        KeyEvent.KEYCODE_BUTTON_R1,
        KeyEvent.KEYCODE_BUTTON_X,
      )

    val DEFAULT_ACTION_BINDINGS: Map<JoystickAction, ActionBinding> =
      mapOf(
        JoystickAction.BACK to
          ActionBinding(
            ShiftModifier.NONE,
            setOf(KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_B),
            isChord = false,
          ),
        JoystickAction.HOME to
          ActionBinding(ShiftModifier.NONE, setOf(KeyEvent.KEYCODE_BUTTON_MODE)),
        JoystickAction.RECENTS to
          ActionBinding(ShiftModifier.NONE, setOf(KeyEvent.KEYCODE_BUTTON_START)),
        JoystickAction.ACTIVATE to
          ActionBinding(
            ShiftModifier.NONE,
            setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_R2),
            isChord = false,
          ),
        JoystickAction.FAST_CURSOR to
          ActionBinding(ShiftModifier.NONE, setOf(KeyEvent.KEYCODE_BUTTON_L2)),
        JoystickAction.TOGGLE_GESTURE to
          ActionBinding(ShiftModifier.NONE, setOf(KeyEvent.KEYCODE_BUTTON_THUMBR)),
        JoystickAction.CYCLE_DISPLAY_BACKWARD to
          ActionBinding(ShiftModifier.SHIFT, setOf(KeyEvent.KEYCODE_BUTTON_L1)),
        JoystickAction.CYCLE_DISPLAY_FORWARD to
          ActionBinding(ShiftModifier.SHIFT, setOf(KeyEvent.KEYCODE_BUTTON_R1)),
        JoystickAction.SELECT_PRIMARY_DEVICE to
          ActionBinding(ShiftModifier.SHIFT, setOf(KeyEvent.KEYCODE_BUTTON_SELECT)),
        JoystickAction.SWIPE_UP to
          ActionBinding(ShiftModifier.ALT, setOf(KeyEvent.KEYCODE_DPAD_UP)),
        JoystickAction.SWIPE_DOWN to
          ActionBinding(ShiftModifier.ALT, setOf(KeyEvent.KEYCODE_DPAD_DOWN)),
        JoystickAction.SWIPE_LEFT to
          ActionBinding(ShiftModifier.ALT, setOf(KeyEvent.KEYCODE_DPAD_LEFT)),
        JoystickAction.SWIPE_RIGHT to
          ActionBinding(ShiftModifier.ALT, setOf(KeyEvent.KEYCODE_DPAD_RIGHT)),
      )

    val DEFAULT = ActionConfig()

    fun fromJson(jsonString: String): ActionConfig {
      return try {
        val json = JSONObject(jsonString)
        val bindingsMap = mutableMapOf<JoystickAction, ActionBinding>()
        val bindingsJson = json.optJSONObject("actionBindings")
        if (bindingsJson != null) {
          for (key in bindingsJson.keys()) {
            try {
              val action = JoystickAction.valueOf(key)
              val bindingJson = bindingsJson.getJSONObject(key)
              bindingsMap[action] = ActionBinding.fromJson(bindingJson)
            } catch (_: Exception) {}
          }
        }

        // Fill in any missing default actions
        for ((action, defaultBinding) in DEFAULT_ACTION_BINDINGS) {
          if (!bindingsMap.containsKey(action)) {
            bindingsMap[action] = defaultBinding
          }
        }

        val toggleChordSet = mutableSetOf<Int>()
        val chordArray = json.optJSONArray("toggleChord")
        if (chordArray != null) {
          for (i in 0 until chordArray.length()) {
            toggleChordSet.add(chordArray.getInt(i))
          }
        }
        val toggleChord = if (toggleChordSet.isNotEmpty()) toggleChordSet else DEFAULT_TOGGLE_CHORD

        val shiftButton = json.optInt("shiftButton", DEFAULT_SHIFT_BUTTON)
        var altButton = json.optInt("altButton", DEFAULT_ALT_BUTTON)
        if (shiftButton == altButton) {
          altButton = if (shiftButton == DEFAULT_ALT_BUTTON) DEFAULT_SHIFT_BUTTON else DEFAULT_ALT_BUTTON
        }

        val mouseStick =
          try {
            MouseStick.valueOf(json.optString("mouseStick", DEFAULT_MOUSE_STICK.name))
          } catch (_: Exception) {
            DEFAULT_MOUSE_STICK
          }

        val invertX = json.optBoolean("invertX", false)
        val invertY = json.optBoolean("invertY", false)

        val cursorConfig =
          json.optJSONObject("cursorConfig")?.let { CursorConfig.fromJson(it) } ?: CursorConfig.DEFAULT

        val cursorSpeed =
          json.optDouble("cursorSpeed", DEFAULT_CURSOR_SPEED.toDouble()).toFloat()
        val fastCursorSpeed =
          json.optDouble("fastCursorSpeed", DEFAULT_FAST_CURSOR_SPEED.toDouble()).toFloat()
        val deadzone =
          json.optDouble("deadzone", DEFAULT_DEADZONE.toDouble()).toFloat()

        ActionConfig(
          actionBindings = bindingsMap,
          toggleChord = toggleChord,
          shiftButton = shiftButton,
          altButton = altButton,
          mouseStick = mouseStick,
          invertX = invertX,
          invertY = invertY,
          cursorConfig = cursorConfig,
          cursorSpeed = cursorSpeed,
          fastCursorSpeed = fastCursorSpeed,
          deadzone = deadzone,
        )
      } catch (_: Exception) {
        DEFAULT
      }
    }

    fun getButtonDisplayName(keyCode: Int): String {
      return when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_A -> "A"
        KeyEvent.KEYCODE_BUTTON_B -> "B"
        KeyEvent.KEYCODE_BUTTON_X -> "X"
        KeyEvent.KEYCODE_BUTTON_Y -> "Y"
        KeyEvent.KEYCODE_BUTTON_L1 -> "Left bumper"
        KeyEvent.KEYCODE_BUTTON_R1 -> "Right bumper"
        KeyEvent.KEYCODE_BUTTON_L2 -> "Left trigger"
        KeyEvent.KEYCODE_BUTTON_R2 -> "Right trigger"
        KeyEvent.KEYCODE_BUTTON_SELECT -> "Select"
        KeyEvent.KEYCODE_BUTTON_START -> "Start"
        KeyEvent.KEYCODE_BUTTON_THUMBL -> "Left thumbstick"
        KeyEvent.KEYCODE_BUTTON_THUMBR -> "Right thumbstick"
        KeyEvent.KEYCODE_BUTTON_MODE -> "Mode"
        KeyEvent.KEYCODE_DPAD_UP -> "D-pad up"
        KeyEvent.KEYCODE_DPAD_DOWN -> "D-pad down"
        KeyEvent.KEYCODE_DPAD_LEFT -> "D-pad left"
        KeyEvent.KEYCODE_DPAD_RIGHT -> "D-pad right"
        else -> KeyEvent.keyCodeToString(keyCode)
      }
    }

    fun getFriendlyButtonName(keyCode: Int): String {
      return when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_A -> "A button"
        KeyEvent.KEYCODE_BUTTON_B -> "B button"
        KeyEvent.KEYCODE_BUTTON_X -> "X button"
        KeyEvent.KEYCODE_BUTTON_Y -> "Y button"
        KeyEvent.KEYCODE_BUTTON_L1 -> "left bumper"
        KeyEvent.KEYCODE_BUTTON_R1 -> "right bumper"
        KeyEvent.KEYCODE_BUTTON_L2 -> "left trigger"
        KeyEvent.KEYCODE_BUTTON_R2 -> "right trigger"
        KeyEvent.KEYCODE_BUTTON_SELECT -> "Select button"
        KeyEvent.KEYCODE_BUTTON_START -> "Start button"
        KeyEvent.KEYCODE_BUTTON_THUMBL -> "left thumbstick"
        KeyEvent.KEYCODE_BUTTON_THUMBR -> "right thumbstick"
        KeyEvent.KEYCODE_BUTTON_MODE -> "Mode button"
        KeyEvent.KEYCODE_DPAD_UP -> "D-pad up"
        KeyEvent.KEYCODE_DPAD_DOWN -> "D-pad down"
        KeyEvent.KEYCODE_DPAD_LEFT -> "D-pad left"
        KeyEvent.KEYCODE_DPAD_RIGHT -> "D-pad right"
        else -> getButtonDisplayName(keyCode).lowercase()
      }
    }

    fun getMouseControlDisplayName(config: ActionConfig): String {
      val stickName = config.mouseStick.getDisplayName()
      val inverts = mutableListOf<String>()
      if (config.invertX) inverts.add("Invert X")
      if (config.invertY) inverts.add("Invert Y")
      return if (inverts.isNotEmpty()) {
        "$stickName (${inverts.joinToString(", ")})"
      } else {
        stickName
      }
    }

    fun formatDeadzone(deadzone: Float): String = "${(deadzone * 100).roundToInt()}%"

    fun formatBinding(binding: ActionBinding): String {
      if (binding.keyCodes.isEmpty()) {
        return "Unassigned"
      }
      val buttonNames = binding.keyCodes.map { getButtonDisplayName(it) }
      return if (binding.isChord) {
        buttonNames.joinToString(" + ") { "`$it`" }
      } else {
        buttonNames.joinToString(" or ") { "`$it`" }
      }
    }

    fun formatChord(keyCodes: Set<Int>): String {
      if (keyCodes.isEmpty()) {
        return "Unassigned"
      }
      return keyCodes.map { getButtonDisplayName(it) }.joinToString(" + ") { "`$it`" }
    }

    fun formatBindingWithModifier(binding: ActionBinding, config: ActionConfig): String {
      if (binding.keyCodes.isEmpty()) {
        return "Unassigned"
      }
      val modifierPrefix =
        when (binding.modifier) {
          ShiftModifier.NONE -> ""
          ShiftModifier.SHIFT -> getButtonDisplayName(config.shiftButton) + " + "
          ShiftModifier.ALT -> getButtonDisplayName(config.altButton) + " + "
          ShiftModifier.ALTSHIFT ->
            "${getButtonDisplayName(config.shiftButton)} + ${getButtonDisplayName(config.altButton)} + "
        }
      val buttonNames = binding.keyCodes.map { getButtonDisplayName(it) }
      return if (binding.isChord) {
        modifierPrefix + buttonNames.joinToString(" + ")
      } else {
        buttonNames.joinToString(" or ") { modifierPrefix + it }
      }
    }

    fun getBindingButtonGroups(binding: ActionBinding, config: ActionConfig): List<List<String>> {
      if (binding.keyCodes.isEmpty()) {
        return emptyList()
      }
      val modifierButtons =
        when (binding.modifier) {
          ShiftModifier.NONE -> emptyList()
          ShiftModifier.SHIFT -> listOf(getButtonDisplayName(config.shiftButton))
          ShiftModifier.ALT -> listOf(getButtonDisplayName(config.altButton))
          ShiftModifier.ALTSHIFT ->
            listOf(
              getButtonDisplayName(config.shiftButton),
              getButtonDisplayName(config.altButton),
            )
        }
      val buttonNames = binding.keyCodes.map { getButtonDisplayName(it) }
      return if (binding.isChord) {
        listOf(modifierButtons + buttonNames)
      } else {
        buttonNames.map { modifierButtons + listOf(it) }
      }
    }

    fun getActionDisplayName(action: JoystickAction): String {
      return when (action) {
        JoystickAction.BACK -> "Back"
        JoystickAction.HOME -> "Home"
        JoystickAction.RECENTS -> "Recent"
        JoystickAction.ACTIVATE -> "Activate at cursor"
        JoystickAction.DPAD_UP -> "D-pad up"
        JoystickAction.DPAD_DOWN -> "D-pad down"
        JoystickAction.DPAD_LEFT -> "D-pad left"
        JoystickAction.DPAD_RIGHT -> "D-pad right"
        JoystickAction.CYCLE_DISPLAY_BACKWARD -> "Move to previous display"
        JoystickAction.CYCLE_DISPLAY_FORWARD -> "Move to next display"
        JoystickAction.SELECT_PRIMARY_DEVICE -> "Select active controller"
        JoystickAction.SWIPE_UP -> "Fling up"
        JoystickAction.SWIPE_DOWN -> "Fling down"
        JoystickAction.SWIPE_LEFT -> "Fling left"
        JoystickAction.SWIPE_RIGHT -> "Fling right"
        JoystickAction.TOGGLE_GESTURE -> "Toggle drag/fling"
        JoystickAction.TOGGLE_ENABLED -> "Toggle JoyMouse"
        JoystickAction.PRIMARY_PRESS,
        JoystickAction.PRIMARY_RELEASE -> "Primary click"
        JoystickAction.FAST_CURSOR,
        JoystickAction.FAST_CURSOR_PRESS,
        JoystickAction.FAST_CURSOR_RELEASE -> "Move cursor faster"
      }
    }

    fun getActionHelpDescription(action: JoystickAction): String {
      return when (action) {
        JoystickAction.BACK -> "`BACK`"
        JoystickAction.HOME -> "`HOME`"
        JoystickAction.RECENTS -> "`START`"
        JoystickAction.ACTIVATE -> "activate at cursor"
        JoystickAction.FAST_CURSOR -> "move cursor faster"
        JoystickAction.DPAD_UP -> "`DPAD_UP`"
        JoystickAction.DPAD_DOWN -> "`DPAD_DOWN`"
        JoystickAction.DPAD_LEFT -> "`DPAD_LEFT`"
        JoystickAction.DPAD_RIGHT -> "`DPAD_RIGHT`"
        JoystickAction.CYCLE_DISPLAY_BACKWARD -> "switch to previous display device"
        JoystickAction.CYCLE_DISPLAY_FORWARD -> "switch to next display device"
        JoystickAction.SELECT_PRIMARY_DEVICE -> "choose active controller"
        JoystickAction.SWIPE_UP -> "`fling up`"
        JoystickAction.SWIPE_DOWN -> "`fling down`"
        JoystickAction.SWIPE_LEFT -> "`fling left`"
        JoystickAction.SWIPE_RIGHT -> "`fling right`"
        JoystickAction.TOGGLE_GESTURE -> "toggle between drag and fling gestures"
        JoystickAction.TOGGLE_ENABLED -> "toggle JoyMouse"
        JoystickAction.PRIMARY_PRESS,
        JoystickAction.PRIMARY_RELEASE -> "primary action"
        JoystickAction.FAST_CURSOR_PRESS,
        JoystickAction.FAST_CURSOR_RELEASE -> "fast cursor"
      }
    }
  }
}
