package work.bearbrains.joymouse.model

/**
 * Encapsulates display information for a connected joystick / gamepad controller.
 */
data class ControllerInfo(
  val id: Int,
  val name: String,
  val isPrimary: Boolean,
)
