package work.bearbrains.joymouse.input.impl

/** Represents a virtual button controlled by one or more logical buttons. */
class VirtualButton(
  /** The [KeyEvent] `KEYCODE_BUTTON_` constants that contribute to this [VirtualButton]. */
  val components: Set<Int>,
  /**
   * Whether this [VirtualButton] is a chord (requiring all components to be pressed to be
   * considered pressed) or a multiplexer (pressed if any component is pressed).
   */
  private val isChord: Boolean,
) {

  var isPressed: Boolean = false
    private set

  /** One-shot callback to be invoked when all components of this [VirtualButton] are released. */
  var onFullyReleased: (() -> Unit)? = null

  /**
   * Processes a change in button states. Returns `true` if this change resulted in a state change
   * for this virtual button.
   */
  fun update(buttonStates: Map<Int, Boolean>): Boolean {
    val oldState = isPressed

    if (isChord) {
      // Press: All buttons must be pressed
      // Release: Any button must be released
      val allPressed = components.all { buttonStates.getOrDefault(it, false) }
      isPressed = allPressed
    } else {
      // Press: Any button must be down
      // Release: All buttons must be up
      val anyPressed = components.any { buttonStates.getOrDefault(it, false) }
      isPressed = anyPressed
    }

    // Notify any registered listener once all components are released.
    if (!isPressed) {
      onFullyReleased?.let { callback ->
        if (components.none { buttonStates.getOrDefault(it, false) }) {
          callback()
          onFullyReleased = null
        }
      }
    }

    return isPressed != oldState
  }

  /**
   * Checks to see whether this [VirtualButton] should be prevented from being pressed/released by
   * another [VirtualButton].
   *
   * Specifically:
   * - No button may lock out a button with non-overlapping components.
   * - No button may lock out itself.
   * - A multiplexed button does not supersede any button with overlapping components.
   * - A chorded button supersedes any multiplexed button with overlapping components.
   * - A chorded button supersedes any chorded button whose components are a strict subset of its
   *   components.
   */
  fun isLockedOutBy(other: VirtualButton): Boolean {
    if (other == this) {
      return false
    }

    val commonButtons = other.components.intersect(components)
    if (commonButtons.isEmpty()) {
      return false
    }

    if (!isChord) {
      return true
    }

    if (!other.isChord) {
      return false
    }

    if (!commonButtons.containsAll(other.components)) {
      return true
    }

    return components.size <= other.components.size
  }

  /**
   * Resets the internal state of this button. Note that this means [onFullyReleased] will never be
   * called!
   */
  fun reset() {
    isPressed = false
    onFullyReleased = null
  }
}
