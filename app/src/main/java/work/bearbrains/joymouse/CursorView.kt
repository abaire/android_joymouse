package work.bearbrains.joymouse

/** Handles rendering a mouse cursor. */
interface CursorView {

  /** The visual state of this cursor. */
  enum class State {
    STATE_RELEASED,
    STATE_PRESSED_TAP,
    STATE_PRESSED_LONG_TOUCH,
    STATE_PRESSED_SLOW_DRAG,
    STATE_PRESSED_FLING,
  }

  /** Shows the cursor view. */
  fun show()

  /** Updates the position of the cursor. */
  fun updatePosition(x: Float, y: Float)

  /** Hides the cursor. */
  fun hideCursor()

  /**
   * Used to request a visual indication that the primary button associated with this cursor is
   * down.
   */
  var cursorState: State
}
