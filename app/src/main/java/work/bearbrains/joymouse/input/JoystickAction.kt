package work.bearbrains.joymouse.input

/** Logical actions that may be emitted by a JoystickCursor */
enum class JoystickAction {
  /** Android "BACK" global action. */
  BACK,
  /** Android "HOME" global action. */
  HOME,
  /** Android "RECENTS" global action. */
  RECENTS,
  /** Android "DPAD_CENTER" global action. */
  ACTIVATE,
  DPAD_UP,
  DPAD_DOWN,
  DPAD_LEFT,
  DPAD_RIGHT,

  /** Requests that the JoystickMouse be enabled/disabled. */
  TOGGLE_ENABLED,

  /** Requests that a drag gesture be toggled to a fling or vice-versa. */
  TOGGLE_GESTURE,

  /** Requests that a swipe or fling up be applied from the current cursor position. */
  SWIPE_UP,
  /** Requests that a swipe or fling down be applied from the current cursor position. */
  SWIPE_DOWN,
  /** Requests that a swipe or fling left be applied from the current cursor position. */
  SWIPE_LEFT,
  /** Requests that a swipe or fling right be applied from the current cursor position. */
  SWIPE_RIGHT,

  /** Requests that the joystick cursor be moved to the next display. */
  CYCLE_DISPLAY_FORWARD,

  /** Requests that the joystick cursor be moved to the previous display. */
  CYCLE_DISPLAY_BACKWARD,

  /** The "primary button" has been fully depressed. */
  PRIMARY_PRESS,

  /** The "primary button" has been fully released. */
  PRIMARY_RELEASE,

  /** The "fast cursor button" has been fully depressed. */
  FAST_CURSOR_PRESS,

  /** The "fast cursor button" has been fully released. */
  FAST_CURSOR_RELEASE,
}
