package work.bearbrains.joymouse.input

import android.view.KeyEvent
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

    return json.toString()
  }

  companion object {
    const val DEFAULT_SHIFT_BUTTON = KeyEvent.KEYCODE_BUTTON_L2
    const val DEFAULT_ALT_BUTTON = KeyEvent.KEYCODE_BUTTON_R2

    /** Special actions that can be remapped to buttons. */
    val REMAPPABLE_ACTIONS: List<JoystickAction> =
      listOf(
        JoystickAction.BACK,
        JoystickAction.HOME,
        JoystickAction.RECENTS,
        JoystickAction.ACTIVATE,
        JoystickAction.CYCLE_DISPLAY_BACKWARD,
        JoystickAction.CYCLE_DISPLAY_FORWARD,
        JoystickAction.SELECT_PRIMARY_DEVICE,
        JoystickAction.SWIPE_UP,
        JoystickAction.SWIPE_DOWN,
        JoystickAction.SWIPE_LEFT,
        JoystickAction.SWIPE_RIGHT,
        JoystickAction.TOGGLE_GESTURE,
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
          ActionBinding(ShiftModifier.NONE, emptySet()),
        JoystickAction.CYCLE_DISPLAY_BACKWARD to
          ActionBinding(ShiftModifier.SHIFT, setOf(KeyEvent.KEYCODE_BUTTON_L1), isChord = true),
        JoystickAction.CYCLE_DISPLAY_FORWARD to
          ActionBinding(ShiftModifier.SHIFT, setOf(KeyEvent.KEYCODE_BUTTON_R1), isChord = true),
        JoystickAction.SELECT_PRIMARY_DEVICE to
          ActionBinding(ShiftModifier.SHIFT, setOf(KeyEvent.KEYCODE_BUTTON_SELECT), isChord = true),
        JoystickAction.SWIPE_UP to
          ActionBinding(ShiftModifier.ALT, setOf(KeyEvent.KEYCODE_DPAD_UP)),
        JoystickAction.SWIPE_DOWN to
          ActionBinding(ShiftModifier.ALT, setOf(KeyEvent.KEYCODE_DPAD_DOWN)),
        JoystickAction.SWIPE_LEFT to
          ActionBinding(ShiftModifier.ALT, setOf(KeyEvent.KEYCODE_DPAD_LEFT)),
        JoystickAction.SWIPE_RIGHT to
          ActionBinding(ShiftModifier.ALT, setOf(KeyEvent.KEYCODE_DPAD_RIGHT)),
        JoystickAction.TOGGLE_GESTURE to
          ActionBinding(ShiftModifier.ALT, setOf(KeyEvent.KEYCODE_BUTTON_A), isChord = true),
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

        ActionConfig(
          actionBindings = bindingsMap,
          toggleChord = toggleChord,
          shiftButton = shiftButton,
          altButton = altButton,
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
        KeyEvent.KEYCODE_BUTTON_L1 -> "Left shoulder"
        KeyEvent.KEYCODE_BUTTON_R1 -> "Right shoulder"
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
        KeyEvent.KEYCODE_BUTTON_L1 -> "left shoulder"
        KeyEvent.KEYCODE_BUTTON_R1 -> "right shoulder"
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

    fun getActionDisplayName(action: JoystickAction): String {
      return when (action) {
        JoystickAction.BACK -> "Back"
        JoystickAction.HOME -> "Home"
        JoystickAction.RECENTS -> "Recent apps"
        JoystickAction.ACTIVATE -> "Activate"
        JoystickAction.DPAD_UP -> "D-pad up"
        JoystickAction.DPAD_DOWN -> "D-pad down"
        JoystickAction.DPAD_LEFT -> "D-pad left"
        JoystickAction.DPAD_RIGHT -> "D-pad right"
        JoystickAction.CYCLE_DISPLAY_BACKWARD -> "Switch to previous display"
        JoystickAction.CYCLE_DISPLAY_FORWARD -> "Switch to next display"
        JoystickAction.SELECT_PRIMARY_DEVICE -> "Choose active controller"
        JoystickAction.SWIPE_UP -> "Fling up"
        JoystickAction.SWIPE_DOWN -> "Fling down"
        JoystickAction.SWIPE_LEFT -> "Fling left"
        JoystickAction.SWIPE_RIGHT -> "Fling right"
        JoystickAction.TOGGLE_GESTURE -> "Toggle drag or fling gesture"
        JoystickAction.TOGGLE_ENABLED -> "Toggle JoyMouse shortcut"
        JoystickAction.PRIMARY_PRESS,
        JoystickAction.PRIMARY_RELEASE -> "Primary click"
        JoystickAction.FAST_CURSOR_PRESS,
        JoystickAction.FAST_CURSOR_RELEASE -> "Fast cursor"
      }
    }

    fun getActionHelpDescription(action: JoystickAction): String {
      return when (action) {
        JoystickAction.BACK -> "`BACK`"
        JoystickAction.HOME -> "`HOME`"
        JoystickAction.RECENTS -> "`START`"
        JoystickAction.ACTIVATE -> "`ACTIVATE`"
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
